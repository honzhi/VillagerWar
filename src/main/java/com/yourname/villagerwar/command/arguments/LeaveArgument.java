package com.yourname.villagerwar.command.arguments;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.gui.LobbyGUI;
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
        return "离开当前游戏或匹配队列";
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

        // 如果在匹配队列中 → 清理
        LobbyGUI.removePlayer(player.getName());
        if (plugin.getInventoryManager().hasSnapshot(player)) {
            plugin.getInventoryManager().clear(player);
            plugin.getInventoryManager().restore(player);
            sender.sendMessage("§7[§6村民战争§7] §e你已退出匹配队列");
            return true;
        }

        // 如果在游戏中 → 离开
        Optional<Game> gameOpt = plugin.getGameManager().getGame(player);
        if (gameOpt.isPresent()) {
            Game game = gameOpt.get();
            plugin.getInventoryManager().clear(player);
            plugin.getInventoryManager().restore(player);
            plugin.getGameManager().leaveGame(player);
            sender.sendMessage("§7[§6村民战争§7] §e你已离开游戏 §a" + game.getGameName());
            return true;
        }

        sender.sendMessage("§7[§6村民战争§7] §c你不在任何游戏或匹配队列中");
        return true;
    }

    @Override
    public @Nullable List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}