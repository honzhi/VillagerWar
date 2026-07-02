package com.yourname.villagerwar.manager;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.GameState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TeamManager {

    private final Game game;

    public TeamManager(Game game) {
        this.game = game;
    }

    /**
     * 将当前所有玩家平均分配到 RED / BLUE 两队。
     * 仅在 PREPARING 状态执行。
     */
    public void assignTeams() {
        if (game.getState() != GameState.PREPARING) { com.yourname.villagerwar.VillagerWar.getInstance().getLogger().warning("[Debug] assignTeams skipped: state=" + game.getState()); return; }

        List<GamePlayer> players = new ArrayList<>(game.getPlayers());
        Collections.shuffle(players);

        int mid = players.size() / 2;
        for (int i = 0; i < players.size(); i++) {
            players.get(i).setTeam(i < mid ? GamePlayer.Team.RED : GamePlayer.Team.BLUE);
        }
    }

    /**
     * 获取指定队伍的所有玩家列表。
     */
    public List<GamePlayer> getTeamPlayers(GamePlayer.Team team) {
        List<GamePlayer> result = new ArrayList<>();
        for (GamePlayer p : game.getPlayers()) {
            if (p.getTeam() == team) {
                result.add(p);
            }
        }
        return result;
    }

    public Game getGame() {
        return game;
    }
}
