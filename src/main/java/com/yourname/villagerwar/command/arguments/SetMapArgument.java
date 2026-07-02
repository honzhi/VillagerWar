package com.yourname.villagerwar.command.arguments;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GameState;
import com.yourname.villagerwar.VillagerWar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SetMapArgument implements SubCommand {
    private final VillagerWar plugin;

    public SetMapArgument(VillagerWar plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "setmap";
    }

    @Override
    public String getDescription() {
        return "设置游戏地图（管理员）";
    }

    @Override
    public String getUsage() {
        return "/vw setmap <地图名>";
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
        Optional<Game> gameOpt = plugin.getGameManager().getGame(player);

        if (gameOpt.isEmpty()) {
            sender.sendMessage("§7[§6村民战争§7] §c你不在任何游戏中");
            return true;
        }

        Game game = gameOpt.get();
        if (game.getState() != GameState.PREPARING) {
            sender.sendMessage("§7[§6村民战争§7] §c游戏已经开始，无法修改地图");
            return true;
        }

        String mapName = args[0];
        Optional<com.yourname.villagerwar.world.GameWorld> worldOpt =
                plugin.getWorldManager().getAvailableWorlds().stream()
                        .filter(w -> w.getWorldName().equalsIgnoreCase(mapName))
                        .findFirst();

        if (worldOpt.isEmpty()) {
            sender.sendMessage("§7[§6村民战争§7] §c未找到名为 §e" + mapName + " §c的地图");
            return true;
        }

        sender.sendMessage("§7[§6村民战争§7] §a地图已设置为 §e" + mapName);
        return true;
    }

    @Override
    public @Nullable List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("villagerwar.admin")) {
            String partial = args[0].toLowerCase();
            return plugin.getWorldManager().getAvailableWorlds().stream()
                    .map(w -> w.getWorldName())
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}