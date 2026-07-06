package com.yourname.villagerwar.manager;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.GameState;
import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.bridge.MMBridge;
import com.yourname.villagerwar.config.holder.MapConfig;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.*;

/**
 * 管理基地生成追踪 + 小兵波次生成 + 路径导航
 */
public class SpawnManager {

    private final Game game;
    private int lastWaveTime = 0;
    private int waveIndex = 0;

    // 追踪所有基地实体 <Entity, Team>
    private final Map<Entity, GamePlayer.Team> bases = new HashMap<>();
    // 追踪所有小兵实体 <Entity, MinionData>
    private final Map<Entity, MinionData> minions = new HashMap<>();
    // 波次内小兵生成索引
    private int spawnTickOffset = 0;

    public SpawnManager(Game game) {
        this.game = game;
    }

    /**
     * 初始化：在就队伍出生点生成基地
     */
    public void spawnBases() {
        if (game.getGameWorld() == null || game.getGameWorld().getBukkitWorld() == null) return;

        org.bukkit.World world = game.getGameWorld().getBukkitWorld();
        MapConfig mapConfig = VillagerWar.getInstance().getConfigManager().getMapConfig(game.getMapId());
        if (mapConfig == null) return;

        for (GamePlayer.Team team : GamePlayer.Team.values()) {
            String teamId = team.name().toLowerCase();
            String mobId = mapConfig.getTeamBaseMobId(teamId);
            Map<String, Object> baseConfig = mapConfig.getTeamBaseConfig(teamId);
            if (mobId == null || mobId.isEmpty()) continue;

            // 获取基地生成位置
            Location spawnLoc = null;
            if (baseConfig != null) {
                Object spawnObj = baseConfig.get("spawn");
                if (spawnObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> spawnMap = (Map<String, Object>) spawnObj;
                    spawnLoc = MapConfig.extractLocation(spawnMap, world);
                }
            }
            if (spawnLoc == null) {
                VillagerWar.getInstance().getLogger().warning("无法找到 " + teamId + " 队的基地生成位置");
                continue;
            }

            // 通过 MM 召唤基地
            Entity baseEntity = (Entity) com.yourname.villagerwar.bridge.MMBridge.spawnMob(mobId, spawnLoc);
            if (baseEntity == null) {
                VillagerWar.getInstance().getLogger().warning("基地实体生成失败: " + mobId);
                continue;
            }

            // PDC 标记
            baseEntity.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(VillagerWar.getInstance(), "vw_team"),
                org.bukkit.persistence.PersistentDataType.STRING, team.name()
            );
            baseEntity.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(VillagerWar.getInstance(), "vw_type"),
                org.bukkit.persistence.PersistentDataType.STRING, "base"
            );

            // 设置 MM 阵营
            MMBridge.setFaction(baseEntity, team == GamePlayer.Team.RED ? "red_team" : "blue_team");

            bases.put(baseEntity, team);
            VillagerWar.getInstance().getLogger().info("[Spawn] " + teamId + " 基地已生成: " + mobId + " @ " + spawnLoc);
        }
    }

    /**
     * 每 tick 处理小兵生成 + 路径导航
     */
    public void tick(int gameTime) {
        if (game.getState() != GameState.PLAYING) return;

        // 检查基地存活
        checkBases();

        // 小兵路径导航
        navigateMinions();

        // 出兵波次
        int waveInterval = game.getGameRule().getWaveInterval();
        if (gameTime - lastWaveTime >= waveInterval) {
            startWave();
            lastWaveTime = gameTime;
        }
    }

    private void checkBases() {
        List<Entity> deadBases = new ArrayList<>();
        for (java.util.Map.Entry<Entity, GamePlayer.Team> entry : bases.entrySet()) {
            Entity e = entry.getKey();
            if (e.isDead() || !e.isValid()) {
                deadBases.add(e);
            }
        }
        for (Entity e : deadBases) {
            GamePlayer.Team deadTeam = bases.remove(e);
            if (deadTeam != null) {
                GamePlayer.Team winner = deadTeam == GamePlayer.Team.RED ? GamePlayer.Team.BLUE : GamePlayer.Team.RED;
                VillagerWar.getInstance().getLogger().info("[Spawn] " + deadTeam + " 基地已死亡! " + winner + " 获胜!");
                game.getVictoryManager().declareWinner(winner);
            }
        }
    }

    private void navigateMinions() {
        for (Map.Entry<Entity, MinionData> entry : minions.entrySet()) {
            Entity e = entry.getKey();
            if (e.isDead() || !e.isValid()) {
                continue;
            }
            MinionData data = entry.getValue();
            if (!(e instanceof Mob)) continue;
            Mob mob = (Mob) e;

            // 到达当前路径点？
            if (data.currentPathIndex < data.path.size()) {
                Location target = data.path.get(data.currentPathIndex);
                if (mob.getLocation().distance(target) < 2.0) {
                    // 到达，切到下一个点
                    data.currentPathIndex++;
                }
                // 还没到，继续走向当前点
                if (data.currentPathIndex < data.path.size()) {
                    Location next = data.path.get(data.currentPathIndex);
                    mob.getPathfinder().moveTo(next, 1.0);
                }
            }

            // 所有路径点走完 → 寻找敌方基地攻击
            if (data.currentPathIndex >= data.path.size()) {
                Entity targetBase = findEnemyBase(data.team);
                if (targetBase != null && targetBase.isValid()) {
                    mob.getPathfinder().moveTo(targetBase.getLocation(), 1.0);
                }
            }
        }
    }

    private Entity findEnemyBase(GamePlayer.Team team) {
        GamePlayer.Team enemy = team == GamePlayer.Team.RED ? GamePlayer.Team.BLUE : GamePlayer.Team.RED;
        for (java.util.Map.Entry<Entity, GamePlayer.Team> entry : bases.entrySet()) {
            if (entry.getValue() == enemy && !entry.getKey().isDead()) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 开始一波出兵
     */
    private void startWave() {
        VillagerWar.getInstance().getLogger().info("[Spawn] 第 " + (++waveIndex) + " 波出兵");
        MapConfig mapConfig = VillagerWar.getInstance().getConfigManager().getMapConfig(game.getMapId());
        if (mapConfig == null) return;
        if (game.getGameWorld() == null || game.getGameWorld().getBukkitWorld() == null) return;
        org.bukkit.World world = game.getGameWorld().getBukkitWorld();

        spawnTickOffset = 0;

        for (GamePlayer.Team team : GamePlayer.Team.values()) {
            String teamId = team.name().toLowerCase();
            List<String> loop = mapConfig.getTeamSoldierLoop(teamId);
            if (loop.isEmpty()) continue;

            Map<String, Object> soldierConfig = mapConfig.getTeamSoldierConfig(teamId);
            Location spawnLoc = null;
            if (soldierConfig != null) {
                Object spawnObj = soldierConfig.get("spawn");
                if (spawnObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> spawnMap = (Map<String, Object>) spawnObj;
                    spawnLoc = MapConfig.extractLocation(spawnMap, world);
                }
            }
            if (spawnLoc == null) continue;

            // 路径点
            List<Map<String, Object>> rawPath = mapConfig.getTeamSoldierPath(teamId);
            List<Location> path = new ArrayList<>();
            for (Map<String, Object> pt : rawPath) {
                Location loc = MapConfig.pathPointToLocation(pt, world);
                if (loc != null) path.add(loc);
            }

            int loopInterval = mapConfig.getSoldierLoopInterval(teamId);

            // 延迟生成循环中的小兵
            for (int i = 0; i < loop.size(); i++) {
                final String mobId = loop.get(i);
                final Location finalSpawn = spawnLoc.clone();
                final List<Location> finalPath = new ArrayList<>(path);
                final GamePlayer.Team finalTeam = team;
                final int delay = i * loopInterval;

                Bukkit.getScheduler().runTaskLater(VillagerWar.getInstance(), () -> {
                    spawnMinion(mobId, finalSpawn, finalPath, finalTeam);
                }, delay);
            }
        }
    }

    private void spawnMinion(String mobId, Location loc, List<Location> path, GamePlayer.Team team) {
        Entity entity = (Entity) MMBridge.spawnMob(mobId, loc);
        if (entity == null) {
            VillagerWar.getInstance().getLogger().warning("小兵生成失败: " + mobId);
            return;
        }

        // PDC 标记
        entity.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(VillagerWar.getInstance(), "vw_team"),
            org.bukkit.persistence.PersistentDataType.STRING, team.name()
        );
        entity.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(VillagerWar.getInstance(), "vw_type"),
            org.bukkit.persistence.PersistentDataType.STRING, "minion"
        );

        // MM 阵营
        MMBridge.setFaction(entity, team == GamePlayer.Team.RED ? "red_team" : "blue_team");

        MinionData data = new MinionData(team, path);
        minions.put(entity, data);

        // 开始走向第一个路径点
        if (!path.isEmpty() && entity instanceof Mob) {
            ((Mob) entity).getPathfinder().moveTo(path.get(0), 1.0);
        }
    }

    /**
     * 清理所有基地和小兵实体
     */
    public void clearAll() {
        for (Entity e : bases.keySet()) {
            if (e != null && e.isValid()) e.remove();
        }
        bases.clear();
        for (Entity e : minions.keySet()) {
            if (e != null && e.isValid()) e.remove();
        }
        minions.clear();
        waveIndex = 0;
        lastWaveTime = 0;
    }

    private static class MinionData {
        final GamePlayer.Team team;
        final java.util.List<Location> path;
        int currentPathIndex = 0;

        MinionData(GamePlayer.Team team, java.util.List<Location> path) {
            this.team = team;
            this.path = path;
        }
    }

}