package com.yourname.villagerwar.ui;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class BossBarManager {
    private final Game game;
    private final Map<GamePlayer, BossBar> playerBars = new HashMap<>();
    private BossBar gameBossBar;

    public BossBarManager(Game game) {
        this.game = game;
    }

    public void createBossBar(GamePlayer gp) {
        Player player = gp.getPlayer();
        if (player == null) return;

        BossBar bar = Bukkit.createBossBar(
                "§6村民战争",
                BarColor.YELLOW,
                BarStyle.SOLID
        );
        bar.addPlayer(player);
        bar.setVisible(true);
        bar.setProgress(1.0);
        playerBars.put(gp, bar);
    }

    public void createGlobalBossBar(String title, BarColor color) {
        if (gameBossBar != null) {
            gameBossBar.removeAll();
        }
        gameBossBar = Bukkit.createBossBar(title, color, BarStyle.SOLID);
        gameBossBar.setVisible(true);
        gameBossBar.setProgress(1.0);

        for (GamePlayer gp : game.getPlayers()) {
            Player player = gp.getPlayer();
            if (player != null && player.isOnline()) {
                gameBossBar.addPlayer(player);
            }
        }
    }

    public void tick(int gameTime) {
        if (gameBossBar == null) return;

        int totalTicks = game.getGameRule().getGameTime();
        double progress = Math.max(0.0, 1.0 - (double) gameTime / totalTicks);
        gameBossBar.setProgress(progress);

        int remaining = Math.max(0, (totalTicks - gameTime) / 20);
        int mins = remaining / 60;
        int secs = remaining % 60;
        gameBossBar.setTitle(String.format("§6剩余时间: §e%02d:%02d", mins, secs));
    }

    public void removeBossBar(GamePlayer gp) {
        BossBar bar = playerBars.remove(gp);
        if (bar != null) {
            bar.removeAll();
        }
        if (gameBossBar != null) {
            gameBossBar.removePlayer(gp.getPlayer());
        }
    }

    public void clearAll() {
        for (BossBar bar : playerBars.values()) {
            bar.removeAll();
        }
        playerBars.clear();
        if (gameBossBar != null) {
            gameBossBar.removeAll();
            gameBossBar = null;
        }
    }
}