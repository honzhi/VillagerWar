package com.yourname.villagerwar.ui;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;

public class ScoreboardManager {
    private final Game game;
    private final Map<GamePlayer, org.bukkit.scoreboard.Scoreboard> playerBoards = new HashMap<>();

    public ScoreboardManager(Game game) {
        this.game = game;
    }

    public void createScoreboard(GamePlayer gp) {
        Player player = gp.getPlayer();
        if (player == null) return;

        org.bukkit.scoreboard.ScoreboardManager bukkitManager = Bukkit.getScoreboardManager();
        if (bukkitManager == null) return;

        org.bukkit.scoreboard.Scoreboard board = bukkitManager.getNewScoreboard();
        Objective obj = board.registerNewObjective("villagerwar", "dummy",
                ChatColor.GOLD + "" + ChatColor.BOLD + "村民战争");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        playerBoards.put(gp, board);
        player.setScoreboard(board);
    }

    public void tick(int gameTime) {
        for (Map.Entry<GamePlayer, org.bukkit.scoreboard.Scoreboard> entry : playerBoards.entrySet()) {
            GamePlayer gp = entry.getKey();
            org.bukkit.scoreboard.Scoreboard board = entry.getValue();
            Player player = gp.getPlayer();
            if (player == null) continue;

            Objective obj = board.getObjective(DisplaySlot.SIDEBAR);
            if (obj == null) continue;

            for (String entryName : board.getEntries()) {
                board.resetScores(entryName);
            }

            String teamName = gp.getTeam() != null ? gp.getTeam().name() : "无";
            String teamColor = gp.getTeam() == GamePlayer.Team.RED ? "§c" : "§9";

            obj.getScore("§7═══════════════").setScore(7);
            obj.getScore("§6队伍: " + teamColor + teamName).setScore(6);
            obj.getScore("§7═══════════════").setScore(5);
            obj.getScore("§e金币: §f" + gp.getGold()).setScore(4);
            obj.getScore("§a击杀: §f" + gp.getKills()).setScore(3);
            obj.getScore("§c死亡: §f" + gp.getDeaths()).setScore(2);
            obj.getScore("§7═══════════════").setScore(1);

            int seconds = gameTime / 20;
            int mins = seconds / 60;
            int secs = seconds % 60;
            String timeStr = String.format("§b时间: §f%02d:%02d", mins, secs);
            obj.getScore(timeStr).setScore(0);
        }
    }

    public void removeScoreboard(GamePlayer gp) {
        org.bukkit.scoreboard.Scoreboard board = playerBoards.remove(gp);
        Player player = gp.getPlayer();
        if (player != null && board != null) {
            org.bukkit.scoreboard.ScoreboardManager bukkitManager = Bukkit.getScoreboardManager();
            if (bukkitManager != null) {
                player.setScoreboard(bukkitManager.getNewScoreboard());
            }
        }
    }

    public void clearAll() {
        for (GamePlayer gp : playerBoards.keySet()) {
            removeScoreboard(gp);
        }
        playerBoards.clear();
    }
}