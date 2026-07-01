package com.yourname.villagerwar.ui;

import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.config.holder.MessagesConfig;
import org.bukkit.entity.Player;

public class MessageManager {
    private final Game game;
    private final MessagesConfig messagesConfig;

    public MessageManager(Game game) {
        this.game = game;
        this.messagesConfig = VillagerWar.getInstance().getConfigManager().getMessagesConfig();
    }

    public void clearAll() {
    }

    public void sendMessage(Player player, String key, Object... args) {
        String msg = messagesConfig.getMessage(key);
        if (msg != null) {
            player.sendMessage(com.yourname.villagerwar.util.MessageUtil.format(msg, args));
        }
    }

    public void broadcastMessage(String key, Object... args) {
        String msg = messagesConfig.getMessage(key);
        if (msg != null) {
            String formatted = com.yourname.villagerwar.util.MessageUtil.format(msg, args);
            game.getPlayers().forEach(p -> {
                Player player = p.getPlayer();
                if (player != null) player.sendMessage(formatted);
            });
        }
    }
}