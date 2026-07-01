package com.yourname.villagerwar.controller;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GameState;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class GameController {
    private final Game game;
    private boolean ticking;
    private BukkitTask tickTask;

    public GameController(Game game) {
        this.game = game;
    }

    public void start() {
        ticking = true;
        tickTask = Bukkit.getScheduler().runTaskTimer(
            com.yourname.villagerwar.VillagerWar.getInstance(), () -> {
                if (!ticking) return;
                game.tick();
            }, 0L, 1L);
    }

    public void destroy() {
        ticking = false;
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        game.getGameWorld().unload();
        game.getUiManager().clearAll();
    }

    public void onStateChange(GameState newState) {
        switch (newState) {
            case WAITING:
                break;
            case PREPARING:
                game.getTeamManager().assignTeams();
                break;
            case SKILL_SELECT:
                game.getSkillManager().openSkillSelectGUI();
                break;
            case SKILL_SHOW:
                break;
            case TELEPORT:
                game.getGameWorld().teleportPlayers(game);
                break;
            case READY:
                break;
            case PLAYING:
                break;
            case ENDING:
                game.getUiManager().showEnding();
                break;
            case REWARD:
                game.getUiManager().showReward();
                break;
            case RETURNING:
                game.getGameWorld().returnToLobby(game);
                break;
        }
    }

    public void transitionTo(GameState nextState) {
        game.setState(nextState);
    }

    public boolean isTicking() { return ticking; }
}
