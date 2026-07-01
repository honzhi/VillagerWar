package com.yourname.villagerwar.listener;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GameState;
import com.yourname.villagerwar.VillagerWar;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Optional;

public class PlayerMoveListener implements Listener {
    private final VillagerWar plugin;

    public PlayerMoveListener(VillagerWar plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Optional<Game> gameOpt = plugin.getGameManager().getGame(player);
        if (gameOpt.isEmpty()) return;

        Game game = gameOpt.get();
        GameState state = game.getState();

        // Only check actual position changes (not just looking around)
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        switch (state) {
            case SKILL_SELECT:
            case SKILL_SHOW:
            case READY:
                event.setCancelled(true);
                break;
            case WAITING:
                // Allow free movement in lobby
                break;
            case PREPARING:
            case TELEPORT:
                event.setCancelled(true);
                break;
            case PLAYING:
                // Allow movement - border check could be added here
                break;
            case ENDING:
            case REWARD:
            case RETURNING:
                event.setCancelled(true);
                break;
        }
    }
}
