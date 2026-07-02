package com.yourname.villagerwar.command.arguments;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.config.rule.GameRule;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class CreateArgument implements SubCommand {
    private final VillagerWar plugin;

    public CreateArgument(VillagerWar plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "创建一局新的游戏";
    }

    @Override
    public String getUsage() {
        return "/vw create <游戏名>";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令仅限玩家执行");
            return true;
        }

        if (!sender.hasPermission("villagerwar.admin")) {
            sender.sendMessage("§c你没有权限执行此命令");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§c用法: " + getUsage());
            return true;
        }

        Player player = (Player) sender;
        String gameName = args[0];

        // Check player isn't already in a game
        if (plugin.getGameManager().getGame(player).isPresent()) {
            sender.sendMessage("§c你已经在游戏中");
            return true;
        }

        // Create default world and rule for now
            GameRule gameRule = com.yourname.villagerwar.config.rule.GameRuleLoader.load(plugin.getConfigManager().getGameModesConfig().getPresets().values().iterator().next());

        Game game = plugin.getGameManager().createGame(gameName, gameName, "default", gameRule);
        plugin.getGameManager().joinGame(player, game);

        sender.sendMessage("§7[§6村民战争§7] §a游戏 §e" + gameName + " §a已创建，你已自动加入！");
        return true;
    }

    @Override
    public @Nullable List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}