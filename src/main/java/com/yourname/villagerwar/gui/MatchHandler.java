package com.yourname.villagerwar.gui;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.GameState;
import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.config.rule.GameRule;
import com.yourname.villagerwar.util.MessageUtil;
import com.yourname.villagerwar.world.GameWorld;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;


public class MatchHandler {

    public static void handleMatch(Player player, String modeId) {
        String mapId = MapSelectGUI.getSelectedMap(player.getName());
        if (mapId == null || mapId.isEmpty()) {
            player.sendMessage(MessageUtil.colorize("&c请先选择一张地图"));
            MapSelectGUI.open(player);
            return;
        }
        player.closeInventory();

        VillagerWar.getInstance().getLogger().info("[Debug] handleMatch: map=" + mapId + " mode=" + modeId);

        if (VillagerWar.getInstance().getGameManager().getGame(player).isPresent()) {
            player.sendMessage(MessageUtil.colorize("&c你已在游戏中"));
            return;
        }

        VillagerWar.getInstance().getInventoryManager().save(player);
        VillagerWar.getInstance().getInventoryManager().apply(player, "matching");

        Game game = VillagerWar.getInstance().getGameManager().findOrCreateGame(mapId, modeId);
        if (game == null) {
            player.sendMessage(MessageUtil.colorize("&c游戏创建失败"));
            VillagerWar.getInstance().getInventoryManager().clear(player);
            VillagerWar.getInstance().getInventoryManager().restore(player);
            return;
        }

        VillagerWar.getInstance().getGameManager().joinGame(player, game);

        GameWorld reservesSeat = null;
        if (game.getReservesSeatName() != null) {
            reservesSeat = VillagerWar.getInstance().getWorldManager().findReservesSeat(game.getReservesSeatName());
        }
        if (reservesSeat == null) {
            reservesSeat = VillagerWar.getInstance().getWorldManager().createReservesSeat();
            if (reservesSeat != null && reservesSeat.isLoaded()) {
                game.setReservesSeatName(reservesSeat.getWorldName());
            }
        }
        org.bukkit.Location seatLoc = VillagerWar.getInstance().getWorldManager().getReservesSeatLocation(reservesSeat);
        if (seatLoc != null) {
            player.teleport(seatLoc);
            player.setFallDistance(0);
        }
        MapSelectGUI.clearSelectedMap(player.getName());

        // 显示正在匹配标题（等待更多人加入）
        game.getUiManager().applyWaitingUI();

        player.sendMessage(MessageUtil.colorize("&a已加入游戏！当前人数: &e" + game.getPlayerCount()));

        GameRule gameRule = game.getGameRule();
        int teamCount = GamePlayer.Team.values().length;
        int totalMinPlayers = gameRule.getMinPlayers() * teamCount;
        VillagerWar.getInstance().getLogger().info("[Debug] Players=" + game.getPlayerCount() + "/" + totalMinPlayers);

        if (game.getPlayerCount() >= totalMinPlayers) {
            com.yourname.villagerwar.config.holder.StatusConfig prepConfig =
                VillagerWar.getInstance().getConfigManager().getStatusConfig("preparing");
            int totalSec = (prepConfig != null) ? prepConfig.getDuration() : 10;
            VillagerWar.getInstance().getLogger().info("[Debug] 人齐了！开始 PREPARING 阶段（" + totalSec + "秒后进入技能选择）");
            for (GamePlayer gp : game.getPlayers()) {
                Player p = gp.getPlayer();
                if (p != null) {
                    p.sendTitle(MessageUtil.colorize("&a&l玩家人数已满"),
                        MessageUtil.colorize("&7准备开始游戏..."), 10, 60, 20);
                }
            }

            game.getController().start();
            game.setState(GameState.PREPARING);
            VillagerWar.getInstance().getLogger().info("[Debug] 第一步：分配队伍（备战席）");
        } else {
            int need = totalMinPlayers - game.getPlayerCount();
            player.sendMessage(MessageUtil.colorize("&e等待更多玩家加入... 还需要&c" + need + " &e人"));
        }
    }
}