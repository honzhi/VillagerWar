package com.yourname.villagerwar.manager;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.GameState;
import com.yourname.villagerwar.bridge.MMBridge;

public class SpawnManager {

    private final Game game;
    private int lastWaveTime = 0;

    public SpawnManager(Game game) {
        this.game = game;
    }

    /**
     * 每 tick 检查是否该出兵。
     * @param gameTime 当前游戏刻数
     */
    public void tick(int gameTime) {
        if (game.getState() != GameState.PLAYING) return;

        // TODO: 从 GameRule 读取 spawnInterval
        int spawnInterval = 100; // 默认每 100 tick 一波

        if (gameTime - lastWaveTime >= spawnInterval) {
            spawnWave();
            lastWaveTime = gameTime;
        }
    }

    /**
     * 为双方队伍生成一波士兵。
     */
    public void spawnWave() {
        // TODO: 通过 MMBridge.spawnMob() 为 RED / BLUE 各生成一波 NPC 士兵
        // 例如: MMBridge.spawnWave(game, GamePlayer.Team.RED);
    }

    public Game getGame() {
        return game;
    }
}
