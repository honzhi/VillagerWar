package com.yourname.villagerwar.listener;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.gui.GUIUtils;
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

        // 如果在匹配队列中 → 清理 + 归还背包
        GUIUtils.removePlayer(player.getName());

        // 归还背包快照（队列和游戏中都有快照需要归还）
        if (plugin.getInventoryManager().hasSnapshot(player)) {
            plugin.getInventoryManager().restore(player);
        }

        // 如果在一局游戏中 → 离开游戏
        Optional<Game> gameOpt = plugin.getGameManager().getGame(player);
        gameOpt.ifPresent(game -> {
            plugin.getGameManager().leaveGame(player);
            game.getUiManager().getMessageManager().broadcastMessage("game.leave",
                    "player", player.getName(),
                    "game", game.getGameName());
        });
    }
}