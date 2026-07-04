package com.yourname.villagerwar.manager;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.GameState;
import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.config.holder.StatusConfig;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import com.yourname.villagerwar.ui.ScoreboardManager;
import com.yourname.villagerwar.ui.BossBarManager;
import com.yourname.villagerwar.ui.TitleManager;
import com.yourname.villagerwar.util.MessageUtil;
import com.yourname.villagerwar.ui.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class UIManager {

    private final Game game;
    private final ScoreboardManager scoreboardManager;
    private final BossBarManager bossBarManager;
    private final TitleManager titleManager;
    private final MessageManager messageManager;
    private BukkitTask waitingTask;

    public UIManager(Game game) {
        this.game = game;
        this.scoreboardManager = new ScoreboardManager(game);
        this.bossBarManager = new BossBarManager(game);
        this.titleManager = new TitleManager(game);
        this.messageManager = new MessageManager(game);
    }

    /**
     * 应用匹配等待阶段 UI（备战席等人时显示）
     */
    public void applyWaitingUI() {
        StatusConfig config = VillagerWar.getInstance().getConfigManager().getStatusConfig("preparing");
        if (config == null) return;

        String title = config.getWaitingTitle();
        String rawSubtitle = config.getWaitingSubtitle();
        String actionBar = config.getWaitingActionBar();
        int current = game.getPlayerCount();
        int max = game.getGameRule().getMaxPlayers();

        for (GamePlayer gp : game.getPlayers()) {
            Player p = gp.getPlayer();
            if (p == null || !p.isOnline()) continue;

            if (!title.isEmpty()) {
                String subtitle = rawSubtitle
                    .replace("{current}", String.valueOf(current))
                    .replace("{max}", String.valueOf(max));
                titleManager.sendTitle(gp, title, subtitle, 10, 999999, 10);
            }
            if (!actionBar.isEmpty()) {
                String msg = actionBar.replace("{current}", String.valueOf(current))
                    .replace("{max}", String.valueOf(max));
                titleManager.sendActionBar(gp, msg);
            }
        }
        VillagerWar.getInstance().getLogger().info("[UI] applyWaitingUI: 已为 " + game.getPlayerCount() + " 人设置等待标题");
    }

    /**
     * 根据当前游戏状态应用 UI 配置（标题、记分板、操作栏等）
     */
    public void applyStateUI(GameState state) {
        String configName = getConfigNameForState(state);
        if (configName == null) return;

        StatusConfig config = VillagerWar.getInstance().getConfigManager().getStatusConfig(configName);
        if (config == null) return;

        // [PREPARING] 匹配开始音效
        if (state == GameState.PREPARING && config.hasMatchStartSound()) {
            String soundStr = config.getMatchStartSound();
            String[] parts = soundStr.split(" ");
            try {
                Sound sound = Sound.valueOf(parts[0]);
                float vol = parts.length > 1 ? Float.parseFloat(parts[1]) : 1f;
                float pit = parts.length > 2 ? Float.parseFloat(parts[2]) : 1f;
                for (GamePlayer gp : game.getPlayers()) {
                    Player p = gp.getPlayer();
                    if (p != null) p.playSound(p.getLocation(), sound, vol, pit);
                }
            } catch (Exception ignored) {}
            if (!config.getMatchStartTitle().isEmpty()) {
                for (GamePlayer gp : game.getPlayers()) {
                    titleManager.sendTitle(gp, config.getMatchStartTitle(), config.getMatchStartSubtitle(), 10, 60, 10);
                }
            }
            if (!config.getMatchStartBroadcast().isEmpty()) {
                for (GamePlayer gp : game.getPlayers()) {
                    Player p = gp.getPlayer();
                    if (p != null) {
                        for (GamePlayer gp2 : game.getPlayers()) {
                            Player p2 = gp2.getPlayer();
                            if (p2 != null) p2.sendMessage(MessageUtil.colorize(config.getMatchStartBroadcast().replace("{player}", p.getName())));
                        }
                    }
                }
            }
        }

        // 计分板
        scoreboardManager.setupFromConfig(config);

        // 操作栏
        if (config.isActionBarEnabled()) {
            for (GamePlayer gp : game.getPlayers()) {
                titleManager.sendActionBar(gp, formatActionBar(config, gp));
            }
        }
    }

    /**
     * 每 tick 更新 UI（当前支持计分板和操作栏动态更新）
     */
    public void tick(int gameTime) {
        GameState state = game.getState();

        // 获取当前状态配置
        String configName = getConfigNameForState(state);
        StatusConfig config = configName != null ?
                VillagerWar.getInstance().getConfigManager().getStatusConfig(configName) : null;

        if (config != null) {
            // 实时更新计分板
            if (config.isScoreboardEnabled()) {
                scoreboardManager.tick(gameTime);
            }

            // 实时更新操作栏
            if (config.isActionBarEnabled()) {
                for (GamePlayer gp : game.getPlayers()) {
                    titleManager.sendActionBar(gp, formatActionBar(config, gp));
                }
            }

            // [READY] 倒计时音效+标题（最后3秒）
            if (state == GameState.READY && game.getStateTime() % 20 == 0 && config.hasReady()) {
                int remain = Math.max(0, 3 - game.getStateTime() / 20);
                String rSound = remain <= 1 ? config.getReadyFinalSound() : config.getReadySound();
                if (!rSound.isEmpty()) {
                    try {
                        String[] sP = rSound.split(" ");
                        Sound s = Sound.valueOf(sP[0]);
                        float v = sP.length > 1 ? Float.parseFloat(sP[1]) : 1f;
                        float p = sP.length > 2 ? Float.parseFloat(sP[2]) : 1f;
                        for (GamePlayer gp : game.getPlayers()) {
                            Player pl = gp.getPlayer();
                            if (pl != null) pl.playSound(pl.getLocation(), s, v, p);
                        }
                    } catch (Exception ignored) {}
                }
                if (remain > 0 && !config.getReadyTitle().isEmpty()) {
                    String t = config.getReadyTitle().replace("{remain}", String.valueOf(remain));
                    String sb = config.getReadySubtitle().replace("{remain}", String.valueOf(remain));
                    for (GamePlayer gp : game.getPlayers()) {
                        Player pl = gp.getPlayer();
                        if (pl != null) pl.sendTitle(
                            org.bukkit.ChatColor.translateAlternateColorCodes('&', t),
                            org.bukkit.ChatColor.translateAlternateColorCodes('&', sb),
                            0, 25, 5);
                    }
                }
            }

            // 实时更新标题倒计时（仅对有标题且有余数的状态）
            if (config.isTitleEnabled() && game.getStateTime() % 20 == 0) {
                int stateSec = game.getStateTime() / 20;
                int remain = Math.max(0, config.getDuration() - stateSec);
                String remainStr = String.valueOf(remain);
                for (GamePlayer gp : game.getPlayers()) {
                    String title = config.getTitleText().replace("{remain}", remainStr);
                    String subtitle = config.getSubtitleText().replace("{remain}", remainStr);
                    titleManager.sendTitle(gp, title, subtitle, 0, 30, 5);
                }
            }
        }

        // 保留旧的 PLAYING-only 逻辑（BossBar）
        if (state == GameState.PLAYING) {
            bossBarManager.tick(gameTime);
        }
    }

    private String formatActionBar(StatusConfig config, GamePlayer gp) {
        String template = config.getActionBarText();
        int stateSec = game.getStateTime() / 20;
        int remain = Math.max(0, config.getDuration() - stateSec);
        template = template.replace("{remain}", String.valueOf(remain));
        template = template.replace("{gold}", String.valueOf(gp.getGold()));
        template = template.replace("{kills}", String.valueOf(gp.getKills()));
        template = template.replace("{deaths}", String.valueOf(gp.getDeaths()));
        template = template.replace("{health}", String.valueOf((int)gp.getPlayer().getHealth()));
        int secs = game.getGameTime() / 20;
        template = template.replace("{time}", String.format("%02d:%02d", secs / 60, secs % 60));
        return template;
    }

    private String getConfigNameForState(GameState state) {
        switch (state) {
            case PREPARING: return "preparing";
            case SKILL_SELECT: return "skills_select";
            case SKILL_SHOW: return "skill_show";
            case TELEPORT: return null;  // 纯传送阶段，不显示UI
            case READY: return "playing"; // Ready倒计时，使用playing配置的ready段
            case PLAYING: return "playing";
            case ENDING: return "ending";
            case REWARD: return "reward";
            case RETURNING: return null;
            default: return null;
        }
    }

    public void showEnding() {
        applyStateUI(GameState.ENDING);
    }

    public void showReward() {
        applyStateUI(GameState.REWARD);
    }

    private void startWaitingTask() {
        if (waitingTask != null) {
            waitingTask.cancel();
            waitingTask = null;
        }
        waitingTask = Bukkit.getScheduler().runTaskTimer(VillagerWar.getInstance(), () -> {
            if (game.getState() != null) {
                if (waitingTask != null) {
                    waitingTask.cancel();
                    waitingTask = null;
                }
                return;
            }
            com.yourname.villagerwar.config.holder.StatusConfig config = VillagerWar.getInstance().getConfigManager().getStatusConfig("preparing");
            if (config == null) return;
            String actionBar = config.getWaitingActionBar();
            if (actionBar.isEmpty()) return;
            int current = game.getPlayerCount();
            int max = game.getGameRule().getMaxPlayers();
            String msg = actionBar.replace("{current}", String.valueOf(current)).replace("{max}", String.valueOf(max));
            for (com.yourname.villagerwar.GamePlayer gp : game.getPlayers()) {
                org.bukkit.entity.Player p = gp.getPlayer();
                if (p != null && p.isOnline()) {
                    titleManager.sendActionBar(gp, msg);
                }
            }
        }, 0L, 20L);
    }

    public void clearAll() {
        if (waitingTask != null) {
            waitingTask.cancel();
            waitingTask = null;
        }
        scoreboardManager.clearAll();
        bossBarManager.clearAll();
        titleManager.clearAll();
        messageManager.clearAll();
    }

    // Getters
    public Game getGame() { return game; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public BossBarManager getBossBarManager() { return bossBarManager; }
    public TitleManager getTitleManager() { return titleManager; }
    public MessageManager getMessageManager() { return messageManager; }
}