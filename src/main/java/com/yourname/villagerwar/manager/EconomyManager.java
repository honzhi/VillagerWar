package com.yourname.villagerwar.manager;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.GameState;

public class EconomyManager {

    private final Game game;
    private int lastGoldTime = 0;

    public EconomyManager(Game game) {
        this.game = game;
    }

    /**
     * 每 tick 检查是否到了发放金币的时间。
     * @param gameTime 当前游戏刻数
     */
    public void tick(int gameTime) {
        if (game.getState() != GameState.PLAYING) return;

        // TODO: 从 GameRule 读取 goldInterval
        int goldInterval = 20; // 默认每秒（20 tick）发一次

        if (gameTime - lastGoldTime >= goldInterval) {
            lastGoldTime = gameTime;

            // TODO: 根据配置决定基础金币量，可以叠加杀敌/助攻奖励
            int baseGold = 1;
            for (GamePlayer p : game.getPlayers()) {
                addGold(p, baseGold);
            }
        }
    }

    /**
     * 给玩家增加指定数量的金币。
     */
    public void addGold(GamePlayer player, int amount) {
        player.addGold(amount);
    }

    /**
     * 扣除玩家金币。余额不足时返回 false。
     */
    public boolean takeGold(GamePlayer player, int amount) {
        return player.takeGold(amount);
    }

    public Game getGame() {
        return game;
    }
}
