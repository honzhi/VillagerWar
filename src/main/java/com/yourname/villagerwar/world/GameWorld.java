package com.yourname.villagerwar.world;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.config.holder.MapConfig;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

/**
 * 单局游戏世界封装
 * 负责模板地图复制、加载、卸载以及玩家传送
 */
public class GameWorld {

    private final String templateName;
    private final String worldName;
    private final VillagerWar plugin;
    private final UUID worldUid;

    private World bukkitWorld;
    private boolean loaded;
    private MapConfig mapConfig;

    /**
     * 创建游戏世界实例
     * @param templateName 模板名称，对应 maps/<templateName>/ 目录
     * @param plugin       插件主类实例
     */
    public GameWorld(String templateName, VillagerWar plugin) {
        this.templateName = templateName;
        this.plugin = plugin;
        this.worldUid = UUID.randomUUID();
        // 世界名使用 game_ + UUID 短格式，保证 Minecraft 合法 [a-z0-9/._-]
        this.worldName = "game_" + worldUid.toString().substring(0, 8);
        this.loaded = false;
    }

      /**
     * 使用已有的 Bukkit World 创建 GameWorld（不复制模板）
     */
    public GameWorld(String templateName, VillagerWar plugin, World bukkitWorld) {
        this.templateName = templateName;
        this.plugin = plugin;
        this.worldUid = UUID.randomUUID();
        this.worldName = bukkitWorld.getName();
        this.bukkitWorld = bukkitWorld;
        this.loaded = true;
        // 尝试加载地图配置
        File mapConfigFile = new File(plugin.getDataFolder(), "maps/" + templateName + "/map.yml");
        if (mapConfigFile.exists()) {
            this.mapConfig = new MapConfig(mapConfigFile);
        }
    }
  /**
     * 加载世界：复制模板地图 → 创建并加载 World
     */
    public void load() {
        if (loaded) {
            plugin.getLogger().warning("世界 " + worldName + " 已加载，跳过");
            return;
        }

        File mapDir = new File(plugin.getDataFolder(), "maps/" + templateName);
        if (!mapDir.isDirectory()) {
            plugin.getLogger().severe("模板地图不存在: " + mapDir.getAbsolutePath());
            return;
        }
        File templateFolder = findMapWorldFolder(mapDir);
        if (templateFolder == null) {
            plugin.getLogger().severe("未找到地图世界文件: " + mapDir.getAbsolutePath()
                + "（请放入含 level.dat 的文件夹）");
            return;
        }

        File targetFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (targetFolder.exists()) {
            plugin.getLogger().warning("目标世界文件夹已存在，将被覆盖: " + worldName);
            deleteFolder(targetFolder);
        }

        // 复制模板文件夹到目标世界
        try {
            copyFolder(templateFolder, targetFolder);
                        File uidFile = new File(targetFolder, "uid.dat");
            if (uidFile.exists()) {
                uidFile.delete();
                plugin.getLogger().info("已删除 uid.dat，避免世界重复冲突");
            }
            File paperWorldFile = new File(targetFolder, "paper-world.yml");
            if (paperWorldFile.exists()) {
                paperWorldFile.delete();
            }
            File sessionLockFile = new File(targetFolder, "session.lock");
            if (sessionLockFile.exists()) {
                sessionLockFile.delete();
            }
            plugin.getLogger().info("模板 " + templateName + " 已复制到 " + worldName);
        } catch (IOException e) {
            plugin.getLogger().severe("复制模板地图失败: " + e.getMessage());
            return;
        }

        // 加载世界
        WorldCreator creator = new WorldCreator(worldName);
        plugin.getLogger().info("[Debug] Creating world: " + worldName + " (template: " + templateName + ")");
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);

        this.bukkitWorld = Bukkit.createWorld(creator);
        if (this.bukkitWorld == null) {
            plugin.getLogger().severe("无法加载世界: " + worldName);
            return;
        }

        // 设置世界规则
        this.bukkitWorld.setAutoSave(false);
        this.bukkitWorld.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
        this.bukkitWorld.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
        this.bukkitWorld.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
        this.bukkitWorld.setGameRule(org.bukkit.GameRule.ANNOUNCE_ADVANCEMENTS, false);
        this.bukkitWorld.setGameRule(org.bukkit.GameRule.DO_FIRE_TICK, false);
        this.bukkitWorld.setTime(1000);

        this.loaded = true;

        // 加载地图配置
        File mapConfigFile = new File(plugin.getDataFolder(), "maps/" + templateName + "/map.yml");
        if (mapConfigFile.exists()) {
            this.mapConfig = new MapConfig(mapConfigFile);
        } else {
            plugin.getLogger().warning("地图配置文件不存在: " + mapConfigFile.getAbsolutePath());
        }

        plugin.getLogger().info("游戏世界 " + worldName + " 加载完成");
    }

    /**
     * 卸载世界
     */
    public void unload() {
        if (!loaded || bukkitWorld == null) return;

        // 先将所有玩家踢出世界
        List<Player> playersInWorld = bukkitWorld.getPlayers();
        for (Player player : playersInWorld) {
            Location lobbyLoc = getLobbyLocation();
            if (lobbyLoc != null) {
                player.teleport(lobbyLoc);
            }
        }

        // 卸载世界
        boolean unloaded = Bukkit.unloadWorld(bukkitWorld, false);
        if (unloaded) {
            plugin.getLogger().info("世界 " + worldName + " 已卸载");
        } else {
            plugin.getLogger().warning("世界 " + worldName + " 卸载失败");
        }

        this.bukkitWorld = null;
        this.loaded = false;
    }

    /**
     * 传送所有玩家到各自队伍的出生点
     * @param game 游戏实例
     */
        public void teleportPlayers(Game game) {
        plugin.getLogger().info("[Debug] teleportPlayers: world=" + (bukkitWorld != null ? bukkitWorld.getName() : "null") + " loaded=" + loaded);
        if (bukkitWorld == null || !loaded) return;

        plugin.getLogger().info("[Debug] teleportPlayers: " + game.getPlayers().size() + " players");
        for (GamePlayer gp : game.getPlayers()) {
            GamePlayer.Team team = gp.getTeam();
            if (team == null) {
                plugin.getLogger().warning("[Debug] " + gp.getUuid() + " no team");
                continue;
            }

            Player player = gp.getPlayer();
            if (player == null) {
                plugin.getLogger().warning("[Debug] GamePlayer has null Bukkit player");
                continue;
            }

            Location spawnLoc = getTeamSpawnLocation(team, game);
            plugin.getLogger().info("[Debug] teleport " + player.getName() + " team=" + team + " loc=" + (spawnLoc != null ? spawnLoc.getWorld().getName() + "," + spawnLoc.getBlockX() + "," + spawnLoc.getBlockY() + "," + spawnLoc.getBlockZ() : "null"));
            if (spawnLoc != null) {
                player.teleport(spawnLoc);
                player.setFallDistance(0);
            }
        }
    }

    public void returnToLobby(Game game) {
        Location lobbyLoc = getLobbyLocation();
        if (lobbyLoc == null) {
            lobbyLoc = Bukkit.getWorlds().get(0).getSpawnLocation();
        }

        for (GamePlayer gp : game.getPlayers()) {
            Player player = gp.getPlayer();
            if (player == null) continue;
            player.teleport(lobbyLoc);
            player.setFallDistance(0);
        }
    }

    /**
     * 获取指定队伍的一个出生点位置
     */
    public Location getTeamSpawnLocation(GamePlayer.Team team, Game game) {
        if (mapConfig == null) {
            return bukkitWorld.getSpawnLocation();
        }

        String teamId = team == GamePlayer.Team.RED ? "red" : "blue";
        org.bukkit.configuration.ConfigurationSection teamSection = mapConfig.getTeamSection(teamId);
        if (teamSection == null) return bukkitWorld.getSpawnLocation();

        org.bukkit.configuration.ConfigurationSection spawnSection = teamSection.getConfigurationSection("player.spawn");
        if (spawnSection == null) return bukkitWorld.getSpawnLocation();

        // 获取 yaw/pitch
        float yaw = (float) spawnSection.getDouble("yaw", 0);
        float pitch = (float) spawnSection.getDouble("pitch", 0);

        // 单点固定生成 point: {x, y, z}
        org.bukkit.configuration.ConfigurationSection singlePoint = spawnSection.getConfigurationSection("point");
        if (singlePoint != null) {
            double x = singlePoint.getDouble("x", 0);
            double y = singlePoint.getDouble("y", 64);
            double z = singlePoint.getDouble("z", 0);
            return new Location(bukkitWorld, x, y, z, yaw, pitch);
        }

        // 兼容写法: points: {x, y, z}
        org.bukkit.configuration.ConfigurationSection pointsSection = spawnSection.getConfigurationSection("points");
        if (pointsSection != null) {
            double x = pointsSection.getDouble("x", 0);
            double y = pointsSection.getDouble("y", 64);
            double z = pointsSection.getDouble("z", 0);
            return new Location(bukkitWorld, x, y, z, yaw, pitch);
        }

        // 多点随机生成 points: [{x, y, z}, ...]
        List<?> points = spawnSection.getList("points");
        if (points != null && !points.isEmpty()) {
            int index = (int) (Math.random() * points.size());
            if (points.get(index) instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> point = (java.util.Map<String, Object>) points.get(index);
                return locationFromMap(point, yaw, pitch);
            }
        }

        return bukkitWorld.getSpawnLocation();
    }

    /**
     * 获取大厅传送点位置
     */
    private Location getLobbyLocation() {
        // 从 config.yml 的 post_game.teleport_to 读取
        org.bukkit.configuration.file.FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("post_game.teleport_enabled", true)) {
            return null;
        }

        String worldName = config.getString("post_game.teleport_to.world", "lobby");
        World lobbyWorld = Bukkit.getWorld(worldName);
        if (lobbyWorld == null) {
            lobbyWorld = Bukkit.getWorlds().get(0);
        }

        double x = config.getDouble("post_game.teleport_to.x", 0.5);
        double y = config.getDouble("post_game.teleport_to.y", 64.0);
        double z = config.getDouble("post_game.teleport_to.z", 0.5);
        float yaw = (float) config.getDouble("post_game.teleport_to.yaw", 0.0);
        float pitch = (float) config.getDouble("post_game.teleport_to.pitch", 0.0);

        return new Location(lobbyWorld, x, y, z, yaw, pitch);
    }

    /**
     * 从 Map 创建 Location
     */
    private Location locationFromMap(java.util.Map<String, Object> map, float yaw, float pitch) {
        double x = getDouble(map, "x", 0);
        double y = getDouble(map, "y", 64);
        double z = getDouble(map, "z", 0);
        return new Location(bukkitWorld, x, y, z, yaw, pitch);
    }

    private double getDouble(java.util.Map<String, Object> map, String key, double def) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        return def;
    }

    /**
     * 复制文件夹（递归）
     */
    private void copyFolder(File source, File target) throws IOException {
        if (source.isDirectory()) {
            if (!target.exists() && !target.mkdirs()) {
                throw new IOException("无法创建目录: " + target);
            }
            File[] children = source.listFiles();
            if (children != null) {
                for (File child : children) {
                    copyFolder(child, new File(target, child.getName()));
                }
            }
        } else {
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * 递归删除文件夹
     */
    private void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                } else {
                    file.delete();
                }
            }
        }
        folder.delete();
    }

    /**
     * 在地图文件夹下找到含 level.dat 的子文件夹
     * 不再限定 world/ 文件夹名称，只认 level.dat
     */
    private File findMapWorldFolder(File mapDir) {
        if (!mapDir.isDirectory()) return null;
        File[] subDirs = mapDir.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File subDir : subDirs) {
                if (new File(subDir, "level.dat").exists()) {
                    return subDir;
                }
            }
        }
        return null;
    }

    // ─── Getters ───

    public String getTemplateName() { return templateName; }
    public String getWorldName() { return worldName; }
    public World getBukkitWorld() { return bukkitWorld; }
    public boolean isLoaded() { return loaded; }
    public MapConfig getMapConfig() { return mapConfig; }
    public UUID getWorldUid() { return worldUid; }
}


