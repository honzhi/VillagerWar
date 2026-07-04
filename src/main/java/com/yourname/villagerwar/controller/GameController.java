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
        if (game.getGameWorld() != null) {
            game.getGameWorld().unload();
        }
        game.getUiManager().clearAll();
    }

    public void onStateChange(GameState newState) {
        switch (newState) {
            case PREPARING:
                game.createGameWorld();
                game.getTeamManager().assignTeams();
                break;
            case SKILL_SELECT:
                // 切换为技能选择背包
                for (GamePlayer gp : game.getPlayers()) {
                    Player player = gp.getPlayer();
                    if (player != null && player.isOnline()) {
                        VillagerWar.getInstance().getInventoryManager().clear(player);
                        VillagerWar.getInstance().getInventoryManager().apply(player, "skill_select");
                    }
                }
                game.getSkillManager().openSkillSelectGUI();
                break;
            case SKILL_SHOW:
                // 切换为技能选择后背包
                for (GamePlayer gp : game.getPlayers()) {
                    Player player = gp.getPlayer();
                    if (player != null && player.isOnline()) {
                        VillagerWar.getInstance().getInventoryManager().clear(player);
                        VillagerWar.getInstance().getInventoryManager().apply(player, "after_skill_select");
                    }
                }
                break;
            case TELEPORT:
                // 传送至游戏地图，清空背包
                for (GamePlayer gp : game.getPlayers()) {
                    Player player = gp.getPlayer();
                    if (player != null && player.isOnline()) {
                        VillagerWar.getInstance().getInventoryManager().clear(player);
                    }
                }
                game.getGameWorld().teleportPlayers(game);
                if (game.getReservesSeatName() != null) {
                    VillagerWar.getInstance().getWorldManager().deleteReservesSeat(game.getReservesSeatName());
                    game.setReservesSeatName(null);
                }
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
                game.destroyGameWorld();
                break;
        }
    }

    public void transitionTo(GameState nextState) {
        game.setState(nextState);
    }

    public boolean isTicking() { return ticking; }
}