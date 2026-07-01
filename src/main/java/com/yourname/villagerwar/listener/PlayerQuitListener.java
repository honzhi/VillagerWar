package com.yourname.villagerwar.listener;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.VillagerWar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;

public class PlayerQuitListener implements Listener {
    private final VillagerWar plugin;

    public PlayerQuitListener(VillagerWar plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Optional<Game> gameOpt = plugin.getGameManager().getGame(player);

        gameOpt.ifPresent(game -> {
            plugin.getGameManager().leaveGame(player);
            game.getUiManager().getMessageManager().broadcastMessage("game.leave",
                    "player", player.getName(),
                    "game", game.getGameName());
        });
    }
}