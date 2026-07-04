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
     * 搴旂敤鍖归厤绛夊緟闃舵 UI锛堝鎴樺腑绛変汉鏃舵樉绀猴級
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
        startWaitingTask();
    }

    /**
     * 鏍规嵁褰撳墠娓告垙鐘舵€佸簲鐢?UI 閰嶇疆锛堟爣棰樸€佽鍒嗘澘銆佹搷浣滄爮绛夛級
     */
    public void applyStateUI(GameState state) {
        // 清除上一个状态的标题和操作栏
        for (GamePlayer gp : game.getPlayers()) {
            Player p = gp.getPlayer();
            if (p != null && p.isOnline()) {
                p.resetTitle();
            }
        }
        String configName = getConfigNameForState(state);
        if (configName == null) return;

        StatusConfig config = VillagerWar.getInstance().getConfigManager().getStatusConfig(configName);
        if (config == null) return;

        // [PREPARING] 鍖归厤寮€濮嬮煶鏁?
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

        // 璁″垎鏉?
        scoreboardManager.setupFromConfig(config);

        // 鎿嶄綔鏍?
        if (config.isActionBarEnabled()) {
            for (GamePlayer gp : game.getPlayers()) {
                titleManager.sendActionBar(gp, formatActionBar(config, gp));
            }
        }
    }

    /**
     * 姣?tick 鏇存柊 UI锛堝綋鍓嶆敮鎸佽鍒嗘澘鍜屾搷浣滄爮鍔ㄦ€佹洿鏂帮級
     */
    public void tick(int gameTime) {
        GameState state = game.getState();

        // 鑾峰彇褰撳墠鐘舵€侀厤缃?
        String configName = getConfigNameForState(state);
        StatusConfig config = configName != null ?
                VillagerWar.getInstance().getConfigManager().getStatusConfig(configName) : null;

        if (config != null) {
            // 瀹炴椂鏇存柊璁″垎鏉?
            if (config.isScoreboardEnabled()) {
                scoreboardManager.tick(gameTime);
            }

            // 瀹炴椂鏇存柊鎿嶄綔鏍?
            if (config.isActionBarEnabled()) {
                for (GamePlayer gp : game.getPlayers()) {
                    titleManager.sendActionBar(gp, formatActionBar(config, gp));
                }
            }

            // [READY] 鍊掕鏃堕煶鏁?鏍囬锛堟渶鍚?绉掞級
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

            // 瀹炴椂鏇存柊鏍囬鍊掕鏃讹紙浠呭鏈夋爣棰樹笖鏈変綑鏁扮殑鐘舵€侊級
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

        // 淇濈暀鏃х殑 PLAYING-only 閫昏緫锛圔ossBar锛?
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
            case TELEPORT: return null;  // 绾紶閫侀樁娈碉紝涓嶆樉绀篣I
            case READY: return "playing"; // Ready鍊掕鏃讹紝浣跨敤playing閰嶇疆鐨剅eady娈?
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