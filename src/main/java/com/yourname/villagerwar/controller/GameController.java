package com.yourname.villagerwar.controller;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.GameState;
import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.config.holder.StatusConfig;
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
        // 应用状态UI配置（标题、记分板、操作栏）
        game.getUiManager().applyStateUI(newState);

        switch (newState) {
            case PREPARING:
                game.createGameWorld();
                game.getTeamManager().assignTeams();
                break;
            case SKILL_SELECT:
                // 重置技能选择状态，切换为技能选择背包
                game.getSkillManager().resetSelections();
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
                StatusConfig showConfig = VillagerWar.getInstance().getConfigManager().getStatusConfig("skill_show");
                int showDuration = (showConfig != null) ? showConfig.getDuration() : 5;
                // 技能展示后 → TELEPORT → 1tick → READY(3s) → PLAYING
                Bukkit.getScheduler().runTaskLater(VillagerWar.getInstance(), () -> {
                    if (game.getState() != GameState.SKILL_SHOW) return;
                    VillagerWar.getInstance().getLogger().info("[Debug] 第四步：传送至游戏地图");
                    game.setState(GameState.TELEPORT);

                    Bukkit.getScheduler().runTaskLater(VillagerWar.getInstance(), () -> {
                        if (game.getState() != GameState.TELEPORT) return;
                        VillagerWar.getInstance().getLogger().info("[Debug] 第五步：Ready 倒计时");
                        game.setState(GameState.READY);

                        Bukkit.getScheduler().runTaskLater(VillagerWar.getInstance(), () -> {
                            if (game.getState() != GameState.READY) return;
                            VillagerWar.getInstance().getLogger().info("[Debug] 第六步：游戏开始！");
                            game.setState(GameState.PLAYING);
                        }, 60L);  // Ready 3s

                    }, 1L);  // 传送后立即倒下个tick开始Ready倒计时

                }, showDuration * 20L);  // 技能展示时间（配置）
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
                VillagerWar.getInstance().getLogger().info("[Debug] 第七步：进入结束展示阶段！");
                game.getUiManager().showEnding();
                break;
            case REWARD:
                VillagerWar.getInstance().getLogger().info("[Debug] 第八步：进入奖励结算阶段");
                game.getUiManager().showReward();
                break;
            case RETURNING:
                VillagerWar.getInstance().getLogger().info("[Debug] 第九步：返回大厅，清除背包、传送、销毁世界");
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