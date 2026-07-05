package com.yourname.villagerwar.listener;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.GameState;
import com.yourname.villagerwar.VillagerWar;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Optional;

public class PlayerRespawnListener implements Listener {
    private final VillagerWar plugin;

    public PlayerRespawnListener(VillagerWar plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Optional<Game> gameOpt = plugin.getGameManager().getGame(player);
        if (gameOpt.isEmpty()) return;

        Game game = gameOpt.get();
        if (game.getState() != GameState.PLAYING) return;

        GamePlayer gp = game.getPlayer(player.getUniqueId());
        if (gp == null) return;

        // 倒计时未结束 → 在游戏世界作为观察者重生
        if (game.getRespawnManager().isInQueue(gp)) {
            if (game.getGameWorld() != null && game.getGameWorld().getBukkitWorld() != null) {
                event.setRespawnLocation(game.getGameWorld().getBukkitWorld().getSpawnLocation());
            }
            // 下一tick设置为观察者模式
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.setGameMode(GameMode.SPECTATOR);
            }, 1L);
            return;
        }

        // 倒计时已结束 → 在队伍出生点以生存模式重生
        if (game.getGameWorld() != null && game.getGameWorld().getBukkitWorld() != null) {
            Location spawnLoc = game.getGameWorld().getTeamSpawnLocation(gp.getTeam(), game);
            if (spawnLoc != null) {
                event.setRespawnLocation(spawnLoc);
            } else {
                event.setRespawnLocation(game.getGameWorld().getBukkitWorld().getSpawnLocation());
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(player.getMaxHealth());
                player.setFoodLevel(20);
                player.setFireTicks(0);
            }, 1L);
            return;
        }

        // 降级：传送到游戏世界出生点
        event.setRespawnLocation(game.getGameWorld().getBukkitWorld().getSpawnLocation());
    }
}
