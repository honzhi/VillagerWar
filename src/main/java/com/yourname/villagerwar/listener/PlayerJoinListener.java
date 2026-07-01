package com.yourname.villagerwar.listener;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.VillagerWar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Optional;

public class PlayerJoinListener implements Listener {
    private final VillagerWar plugin;

    public PlayerJoinListener(VillagerWar plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPlayedBefore()) {
            player.sendMessage("§7[§6村民战争§7] §a欢迎来到服务器！输入 §e/vw list §a查看活跃的游戏。");
        } else {
            Optional<Game> activeGame = plugin.getGameManager().getGame(player);
            if (activeGame.isPresent()) {
                player.sendMessage("§7[§6村民战争§7] §a你有一局进行中的游戏：§e" + activeGame.get().getGameName());
            }
        }
    }
}
