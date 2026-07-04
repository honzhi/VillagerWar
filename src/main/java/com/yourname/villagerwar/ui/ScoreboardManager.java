package com.yourname.villagerwar.ui;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.GameState;
import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.config.holder.StatusConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScoreboardManager {
    private final Game game;
    private final Map<GamePlayer, org.bukkit.scoreboard.Scoreboard> playerBoards = new HashMap<>();
    private StatusConfig currentConfig;

    public ScoreboardManager(Game game) {
        this.game = game;
    }

    /**
     * 根据状态配置创建/更新计分板
     */
    public void setupFromConfig(StatusConfig config) {
        this.currentConfig = config;
        if (config == null || !config.isScoreboardEnabled()) {
            clearAll();
            return;
        }

        for (GamePlayer gp : game.getPlayers()) {
            createOrUpdateBoard(gp);
        }
    }

    private void createOrUpdateBoard(GamePlayer gp) {
        Player player = gp.getPlayer();
        if (player == null) return;

        org.bukkit.scoreboard.ScoreboardManager bukkitManager = Bukkit.getScoreboardManager();
        if (bukkitManager == null) return;

        org.bukkit.scoreboard.Scoreboard board = playerBoards.get(gp);
        boolean isNew = false;
        if (board == null) {
            board = bukkitManager.getNewScoreboard();
            playerBoards.put(gp, board);
            isNew = true;
        }

        Objective obj = board.getObjective(DisplaySlot.SIDEBAR);
        if (obj == null) {
            try {
                obj = board.registerNewObjective("vw_status", "dummy",
                        ChatColor.translateAlternateColorCodes('&',
                                currentConfig != null ? currentConfig.getScoreboardTitle() : "&6&l村民战争"));
            } catch (Exception e) {
                return;
            }
        }

        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        updateBoardLines(gp, board, obj);

        if (isNew) {
            player.setScoreboard(board);
        }
    }

    /**
     * 更新计分板中的行内容
     */
    private void updateBoardLines(GamePlayer gp, org.bukkit.scoreboard.Scoreboard board, Objective obj) {
        // 清除旧行
        for (String entryName : board.getEntries()) {
            board.resetScores(entryName);
        }

        List<String> lines = currentConfig != null ? currentConfig.getScoreboardLines() : null;
        if (lines == null || lines.isEmpty()) {
            // 默认硬编码回退
            String teamName = gp.getTeam() != null ? gp.getTeam().name() : "无";
            String teamColor = gp.getTeam() == GamePlayer.Team.RED ? "&c" : "&9";
            obj.getScore("&7━━━━━━━━━━━━━━").setScore(5);
            obj.getScore("&6队伍: " + teamColor + teamName).setScore(4);
            obj.getScore("&e金币: &f" + gp.getGold()).setScore(3);
            obj.getScore("&a击杀: &f" + gp.getKills()).setScore(2);
            obj.getScore("&c死亡: &f" + gp.getDeaths()).setScore(1);
            obj.getScore("&7━━━━━━━━━━━━━━").setScore(0);
            return;
        }

        // 计算状态时间（stateTime是tick数，转换为秒）
        int stateSec = game.getStateTime() / 20;
        int duration = currentConfig.getDuration();
        int remainSec = Math.max(0, duration - stateSec);
        String remainStr = formatTime(remainSec);

        // 游戏总时间
        int gameTick = game.getGameTime();
        int gameSec = gameTick / 20;
        String timeStr = formatTime(gameSec);

        // 队伍数据
        int teamAKills = 0, teamBKills = 0;
        for (GamePlayer p : game.getPlayers()) {
            if (p.getTeam() == GamePlayer.Team.RED) teamAKills += p.getKills();
            else teamBKills += p.getKills();
        }

        // 准备就绪人数
        int readyCount = 0;
        for (GamePlayer p : game.getPlayers()) {
            if (p.getSkill() != null) readyCount++;
        }
        int totalCount = game.getPlayerCount();

        // 行数据（按配置中的顺序，从底部往上显示）
        int score = lines.size();
        for (String line : lines) {
            String parsed = parseLine(line, gp, remainStr, timeStr,
                    teamAKills, teamBKills, readyCount, totalCount);
            if (!parsed.isEmpty()) {
                String colored = ChatColor.translateAlternateColorCodes('&', parsed);
                obj.getScore(colored).setScore(score);
            }
            score--;
        }
    }

    private String parseLine(String line, GamePlayer gp, String remain, String time,
                             int teamAKills, int teamBKills, int ready, int total) {
        String result = line;

        // 玩家数据
        String teamName = gp.getTeam() != null ? gp.getTeam().name() : "无";
        result = result.replace("{team}", teamName);
        result = result.replace("{skill}", gp.getSkill() != null ? gp.getSkill().getDisplayName() : "未选择");
        result = result.replace("{kills}", String.valueOf(gp.getKills()));
        result = result.replace("{deaths}", String.valueOf(gp.getDeaths()));
        result = result.replace("{assists}", String.valueOf(gp.getAssists()));
        result = result.replace("{gold}", String.valueOf(gp.getGold()));

        // 时间
        result = result.replace("{remain}", remain);
        result = result.replace("{time}", time);

        // 地图和模式
        result = result.replace("{map}", game.getMapId());
        result = result.replace("{mode}", game.getModeId());

        // 队伍数量
        int redCount = 0, blueCount = 0;
        for (GamePlayer p : game.getPlayers()) {
            if (p.getTeam() == GamePlayer.Team.RED) redCount++;
            else if (p.getTeam() == GamePlayer.Team.BLUE) blueCount++;
        }
        result = result.replace("{team_red}", String.valueOf(redCount));
        result = result.replace("{team_blue}", String.valueOf(blueCount));
        result = result.replace("{current}", String.valueOf(game.getPlayerCount()));
        result = result.replace("{max}", String.valueOf(game.getGameRule().getMaxPlayers()));

        // 队伍数据
        result = result.replace("{team_a_kills}", String.valueOf(teamAKills));
        result = result.replace("{team_b_kills}", String.valueOf(teamBKills));

        // 准备状态
        result = result.replace("{ready_count}", String.valueOf(ready));
        result = result.replace("{total_count}", String.valueOf(total));

        // 结算数据
        result = result.replace("{kill_gold}", "0");
        result = result.replace("{assist_gold}", "0");
        result = result.replace("{participation_gold}", "0");
        result = result.replace("{total_gold}", "0");
        result = result.replace("{winner}", teamName);

        return result;
    }

    private String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    public void tick(int gameTime) {
        if (currentConfig == null) return;
        for (Map.Entry<GamePlayer, org.bukkit.scoreboard.Scoreboard> entry : new ArrayList<>(playerBoards.entrySet())) {
            GamePlayer gp = entry.getKey();
            org.bukkit.scoreboard.Scoreboard board = entry.getValue();
            Objective obj = board.getObjective(DisplaySlot.SIDEBAR);
            if (obj == null) continue;
            updateBoardLines(gp, board, obj);
        }
    }

    public void removeScoreboard(GamePlayer gp) {
        org.bukkit.scoreboard.Scoreboard board = playerBoards.remove(gp);
        Player player = gp.getPlayer();
        if (player != null) {
            org.bukkit.scoreboard.ScoreboardManager bukkitManager = Bukkit.getScoreboardManager();
            if (bukkitManager != null) {
                player.setScoreboard(bukkitManager.getNewScoreboard());
            }
        }
    }

    public void clearAll() {
        // 使用副本迭代，避免 ConcurrentModificationException
        for (GamePlayer gp : new ArrayList<>(playerBoards.keySet())) {
            removeScoreboard(gp);
        }
        playerBoards.clear();
        currentConfig = null;
    }
}