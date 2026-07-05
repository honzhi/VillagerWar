package com.yourname.villagerwar.manager;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.GameState;
import com.yourname.villagerwar.VillagerWar;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RespawnManager {

    private final Game game;
    private final Map<UUID, Integer> respawnQueue = new HashMap<>();

    public RespawnManager(Game game) {
        this.game = game;
    }

    /**
     * 每 tick 检查复活队列中是否有到时的玩家。
     * @param gameTime 当前游戏刻数
     */
        public void tick(int gameTime) {
        if (game.getState() != GameState.PLAYING) return;

        List<UUID> ready = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : respawnQueue.entrySet()) {
            if (gameTime >= entry.getValue()) {
                ready.add(entry.getKey());
            }
        }

        for (UUID uuid : ready) {
            respawnQueue.remove(uuid);
            GamePlayer gp = game.getPlayer(uuid);
            if (gp != null) {
                Player player = gp.getPlayer();
                if (player != null && player.isOnline() && player.getGameMode() == GameMode.SPECTATOR) {
                    // 倒计时已到，自动复活玩家
                    Location spawnLoc = game.getGameWorld() != null ?
                        game.getGameWorld().getTeamSpawnLocation(gp.getTeam(), game) : null;
                    if (spawnLoc != null) {
                        player.teleport(spawnLoc);
                    } else if (game.getGameWorld() != null && game.getGameWorld().getBukkitWorld() != null) {
                        player.teleport(game.getGameWorld().getBukkitWorld().getSpawnLocation());
                    }
                    player.setGameMode(GameMode.SURVIVAL);
                    player.setHealth(player.getMaxHealth());
                    player.setFoodLevel(20);
                    player.setFireTicks(0);
                    player.setFallDistance(0);
                    player.resetTitle();
                }
            }
        }

        // 给观察者模式的玩家显示剩余复活时间
        for (Map.Entry<UUID, Integer> entry : respawnQueue.entrySet()) {
            GamePlayer gp = game.getPlayer(entry.getKey());
            if (gp == null) continue;
            Player p = gp.getPlayer();
            if (p == null || !p.isOnline() || p.getGameMode() != GameMode.SPECTATOR) continue;
            int remainTicks = entry.getValue() - gameTime;
            int remainSec = Math.max(0, (remainTicks + 19) / 20);
            p.sendActionBar("§c复活倒计时: §e" + remainSec + "§c秒");
        }
    }
    public void addToRespawnQueue(GamePlayer player, int delayTicks) {
        respawnQueue.put(player.getUuid(), game.getGameTime() + delayTicks);
    }

    /**
     * 获取玩家剩余复活时间（秒）
     */
    public int getRemainingTime(GamePlayer player) {
        if (player == null) return 0;
        Integer expireTick = respawnQueue.get(player.getUuid());
        if (expireTick == null) return 0;
        int remainTicks = expireTick - game.getGameTime();
        return Math.max(0, (remainTicks + 19) / 20); // 向上取整到秒
    }

    /**
     * 立即复活玩家到己方基地。
     */
    public void respawnPlayer(GamePlayer player) {
        if (player == null) return;
        Player bukkitPlayer = player.getPlayer();
        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) return;

        // 传送到己方队伍出生点
        if (game.getGameWorld() != null && game.getGameWorld().getBukkitWorld() != null) {
            Location spawnLoc = game.getGameWorld().getTeamSpawnLocation(player.getTeam(), game);
            if (spawnLoc != null) {
                bukkitPlayer.teleport(spawnLoc);
            } else {
                bukkitPlayer.teleport(game.getGameWorld().getBukkitWorld().getSpawnLocation());
            }
        }

        // 恢复状态
        bukkitPlayer.setGameMode(GameMode.SURVIVAL);
        bukkitPlayer.setHealth(bukkitPlayer.getMaxHealth());
        bukkitPlayer.setFoodLevel(20);
        bukkitPlayer.setFireTicks(0);
        bukkitPlayer.setFallDistance(0);

        // 清除标题
        bukkitPlayer.resetTitle();
    }

    /**
     * 检查玩家是否在复活队列中。
     */
    public boolean isInQueue(GamePlayer player) {
        if (player == null) return false;
        return respawnQueue.containsKey(player.getUuid());
    }

    public Game getGame() {
        return game;
    }
}
