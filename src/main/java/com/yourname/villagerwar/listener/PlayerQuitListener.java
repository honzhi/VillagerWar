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

        // 濡傛灉鍦ㄥ尮閰嶉槦鍒椾腑 鈫?娓呯悊 + 褰掕繕鑳屽寘
        GUIUtils.removePlayer(player.getName());

        // 褰掕繕鑳屽寘蹇収锛堥槦鍒楀拰娓告垙涓兘鏈夊揩鐓ч渶瑕佸綊杩橈級
        if (plugin.getInventoryManager().hasSnapshot(player)) {
            plugin.getInventoryManager().restore(player);
        }

        // 濡傛灉鍦ㄤ竴灞€娓告垙涓?鈫?绂诲紑娓告垙
        Optional<Game> gameOpt = plugin.getGameManager().getGame(player);
        gameOpt.ifPresent(game -> {
            plugin.getGameManager().leaveGame(player);
            player.resetTitle();
            game.getUiManager().getMessageManager().broadcastMessage("game.leave",
                    "player", player.getName(),
                    "game", game.getGameName());
        });
    }
}