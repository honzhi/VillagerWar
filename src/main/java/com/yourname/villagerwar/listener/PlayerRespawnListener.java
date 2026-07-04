package com.yourname.villagerwar.listener;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.GameState;
import com.yourname.villagerwar.VillagerWar;
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

        // 设置复活点为己方队伍出生点
        if (game.getGameWorld() != null && game.getGameWorld().getBukkitWorld() != null) {
            Location spawnLoc = game.getGameWorld().getTeamSpawnLocation(gp.getTeam(), game);
            if (spawnLoc != null) {
                event.setRespawnLocation(spawnLoc);
                return;
            }
        }

        // 降级：传送到游戏世界出生点
        event.setRespawnLocation(game.getGameWorld().getBukkitWorld().getSpawnLocation());
    }
}
