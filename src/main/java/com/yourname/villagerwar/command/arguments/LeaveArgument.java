package com.yourname.villagerwar.command.arguments;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GameState;
import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.gui.GUIUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class LeaveArgument implements SubCommand {
    private final VillagerWar plugin;

    public LeaveArgument(VillagerWar plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "leave";
    }

    @Override
    public String getDescription() {
        return "离开匹配队列（匹配成功后不可用）";
    }

    @Override
    public String getUsage() {
        return "/vw leave";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令仅限玩家执行");
            return true;
        }

        Player player = (Player) sender;

        // 如果已经在游戏中且游戏已进入正式阶段 → 拒绝
        Optional<Game> gameOpt = plugin.getGameManager().getGame(player);
        if (gameOpt.isPresent()) {
            Game game = gameOpt.get();
            if (game.getState() != GameState.PREPARING && game.getState() != null) {
                sender.sendMessage("§7[§6村民战争§7] §c匹配已完成，游戏已开始，无法退出匹配");
                return true;
            }
            // 在等待或 PREPARING 阶段→ 退出匹配
            GUIUtils.removePlayer(player.getName());
            if (plugin.getInventoryManager().hasSnapshot(player)) {
                plugin.getInventoryManager().clear(player);
                plugin.getInventoryManager().restore(player);
            }
            plugin.getGameManager().leaveGame(player);
            player.resetTitle();
            sender.sendMessage("§7[§6村民战争§7] §e你已退出匹配队列");
            return true;
        }

        // 不在任何游戏中→尝试清理队列残留
        GUIUtils.removePlayer(player.getName());
        if (plugin.getInventoryManager().hasSnapshot(player)) {
            plugin.getInventoryManager().clear(player);
            plugin.getInventoryManager().restore(player);
            player.resetTitle();
            sender.sendMessage("§7[§6村民战争§7] §e你已退出匹配队列");
            return true;
        }

        sender.sendMessage("§7[§6村民战争§7] §c你不在任何匹配队列中");
        return true;
    }

    @Override
    public @Nullable List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}