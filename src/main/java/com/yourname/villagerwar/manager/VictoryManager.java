package com.yourname.villagerwar.manager;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.GameState;

public class VictoryManager {

    private final Game game;

    public VictoryManager(Game game) {
        this.game = game;
    }

    /**
     * 每 tick 检查胜负条件。
     * @param gameTime 当前游戏刻数
     */
    public void tick(int gameTime) {
        if (game.getState() != GameState.PLAYING) return;

        GamePlayer.Team winner = checkVictory();
        if (winner != null) {
            // TODO: 记录胜利方数据
            game.setState(GameState.ENDING);
        }
    }

    /**
     * 检查是否有队伍获胜。
     * 条件：敌方基地被摧毁 / 敌方全体退出 / 超时后按某些规则判定。
     * @return 胜利队伍，若无则返回 null
     */
    public GamePlayer.Team checkVictory() {
        // TODO: 检查 RED 基地是否存活，不存活则 BLUE 获胜
        // TODO: 检查 BLUE 基地是否存活，不存活则 RED 获胜
        // TODO: 检查游戏是否超时，超时按击杀数/基地血量判定胜者
        return null;
    }

    public Game getGame() {
        return game;
    }
}
