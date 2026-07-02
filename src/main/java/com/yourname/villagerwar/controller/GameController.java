package com.yourname.villagerwar.controller;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.GameState;
import com.yourname.villagerwar.VillagerWar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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
            VillagerWar.getInstance(), () -> {
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
                for (GamePlayer gp : game.getPlayers()) {
                    Player player = gp.getPlayer();
                    if (player != null && player.isOnline()) {
                        VillagerWar.getInstance().getInventoryManager().apply(player, "playing");
                    }
                }
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
                for (GamePlayer gp : game.getPlayers()) {
                    Player player = gp.getPlayer();
                    if (player != null && player.isOnline()) {
                        VillagerWar.getInstance().getInventoryManager().clear(player);
                        VillagerWar.getInstance().getInventoryManager().restore(player);
                    }
                }
                game.getGameWorld().returnToLobby(game);
                break;
        }
    }

    public void transitionTo(GameState nextState) {
        game.setState(nextState);
    }

    public boolean isTicking() { return ticking; }
}