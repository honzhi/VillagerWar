package com.yourname.villagerwar.manager;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GameState;
import com.yourname.villagerwar.ui.ScoreboardManager;
import com.yourname.villagerwar.ui.BossBarManager;
import com.yourname.villagerwar.ui.TitleManager;
import com.yourname.villagerwar.ui.MessageManager;

public class UIManager {

    private final Game game;
    private final ScoreboardManager scoreboardManager;
    private final BossBarManager bossBarManager;
    private final TitleManager titleManager;
    private final MessageManager messageManager;

    public UIManager(Game game) {
        this.game = game;
        this.scoreboardManager = new ScoreboardManager(game);
        this.bossBarManager = new BossBarManager(game);
        this.titleManager = new TitleManager(game);
        this.messageManager = new MessageManager(game);
    }

    /**
     * 每 tick 更新 UI 组件。
     * @param gameTime 当前游戏刻数
     */
    public void tick(int gameTime) {
        if (game.getState() != GameState.PLAYING) return;

        scoreboardManager.tick(gameTime);
        bossBarManager.tick(gameTime);
    }

    /**
     * 展示游戏结算界面（ENDING 阶段）。
     */
    public void showEnding() {
        // TODO: 显示计分板、胜负标题、播放音效
    }

    /**
     * 展示奖励界面（REWARD 阶段）。
     */
    public void showReward() {
        // TODO: 发放游戏币/经验等奖励，并展示结算面板
    }

    /**
     * 清除所有 UI 组件（游戏结束/销毁时调用）。
     */
    public void clearAll() {
        scoreboardManager.clearAll();
        bossBarManager.clearAll();
        titleManager.clearAll();
        messageManager.clearAll();
    }

    public Game getGame() {
        return game;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }

    public TitleManager getTitleManager() {
        return titleManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }
}
