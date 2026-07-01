package com.yourname.villagerwar.ui;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public class TitleManager {
    private final Game game;

    public TitleManager(Game game) {
        this.game = game;
    }

    public void sendTitle(GamePlayer gp, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Player player = gp.getPlayer();
        if (player == null || !player.isOnline()) return;
        player.sendTitle(
                colorize(title),
                colorize(subtitle),
                fadeIn,
                stay,
                fadeOut
        );
    }

    public void sendTitleToAll(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for (GamePlayer gp : game.getPlayers()) {
            sendTitle(gp, title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    public void sendActionBar(GamePlayer gp, String message) {
        Player player = gp.getPlayer();
        if (player == null || !player.isOnline()) return;
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent(colorize(message)));
    }

    public void sendActionBarToAll(String message) {
        for (GamePlayer gp : game.getPlayers()) {
            sendActionBar(gp, message);
        }
    }

    public void sendCountdown(int seconds) {
        for (int i = seconds; i > 0; i--) {
            int finalI = i;
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                    com.yourname.villagerwar.VillagerWar.getInstance(),
                    () -> sendTitleToAll(
                            "§e" + finalI,
                            "§7游戏即将开始...",
                            5, 20, 5
                    ),
                    (long) (seconds - i) * 20L
            );
        }
        // Final "GO!"
        org.bukkit.Bukkit.getScheduler().runTaskLater(
                com.yourname.villagerwar.VillagerWar.getInstance(),
                () -> sendTitleToAll("§a§lGO!", "§7战斗吧！", 5, 30, 10),
                (long) seconds * 20L
        );
    }

    public void clearAll() {
        for (GamePlayer gp : game.getPlayers()) {
            Player player = gp.getPlayer();
            if (player != null && player.isOnline()) {
                player.resetTitle();
            }
        }
    }

    private String colorize(String text) {
        if (text == null) return "";
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }
}
