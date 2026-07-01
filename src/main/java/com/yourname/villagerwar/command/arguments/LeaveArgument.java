package com.yourname.villagerwar.command.arguments;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.VillagerWar;
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
        return "离开当前游戏";
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
        Optional<Game> gameOpt = plugin.getGameManager().getGame(player);

        if (gameOpt.isEmpty()) {
            sender.sendMessage("§7[§6村民战争§7] §c你不在任何游戏中");
            return true;
        }

        Game game = gameOpt.get();
        plugin.getGameManager().leaveGame(player);
        sender.sendMessage("§7[§6村民战争§7] §e你已离开游戏 §a" + game.getGameName());
        return true;
    }

    @Override
    public @Nullable List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
