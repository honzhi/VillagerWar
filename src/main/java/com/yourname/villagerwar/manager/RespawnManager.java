package com.yourname.villagerwar.manager;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.GameState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RespawnManager {

    private final Game game;
    private final Map<UUID, Integer> respawnQueue = new HashMap<>();

    public RespawnManager(Game game) {
        this.game = game;
    }

    /**
     * 每 tick 检查复活队列中是否有到时的玩家。
     * @param gameTime 当前游戏刻数
     */
    public void tick(int gameTime) {
        if (game.getState() != GameState.PLAYING) return;

        List<UUID> ready = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : respawnQueue.entrySet()) {
            if (gameTime >= entry.getValue()) {
                ready.add(entry.getKey());
            }
        }

        for (UUID uuid : ready) {
            respawnQueue.remove(uuid);
            GamePlayer player = game.getPlayer(uuid);
            if (player != null) {
                respawnPlayer(player);
            }
        }
    }

    /**
     * 将玩家加入复活队列，delayTicks 后自动复活。
     * @param player     要复活的玩家
     * @param delayTicks 延迟刻数
     */
    public void addToRespawnQueue(GamePlayer player, int delayTicks) {
        respawnQueue.put(player.getUuid(), game.getGameTime() + delayTicks);
        // TODO: 发送已加入复活队列的消息
    }

    /**
     * 立即复活玩家到己方基地。
     */
    public void respawnPlayer(GamePlayer player) {
        if (player == null) return;
        // TODO: 将玩家传送到己方基地重生点
        // TODO: 设置玩家状态（清除伤害、设置生命值等）
    }

    /**
     * 检查玩家是否在复活队列中。
     */
    public boolean isInQueue(GamePlayer player) {
        return respawnQueue.containsKey(player.getUuid());
    }

    public Game getGame() {
        return game;
    }
}
