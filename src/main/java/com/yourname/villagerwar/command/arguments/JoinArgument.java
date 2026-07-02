package com.yourname.villagerwar.command.arguments;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GameState;
import com.yourname.villagerwar.VillagerWar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class JoinArgument implements SubCommand {
    private final VillagerWar plugin;

    public JoinArgument(VillagerWar plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public String getDescription() {
        return "加入一局游戏（管理员）";
    }

    @Override
    public String getUsage() {
        return "/vw join <游戏名>";
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

        if (plugin.getGameManager().getGame(player).isPresent()) {
            sender.sendMessage("§7[§6村民战争§7] §c你已经在该游戏中");
            return true;
        }

        String gameName = args[0];

        Optional<Game> gameOpt = plugin.getGameManager().getGames().stream()
                .filter(g -> g.getGameName().equalsIgnoreCase(gameName))
                .findFirst();

        if (gameOpt.isEmpty()) {
            sender.sendMessage("§7[§6村民战争§7] §c未找到名为 §e" + gameName + " §c的游戏");
            return true;
        }

        Game game = gameOpt.get();

        if (game.getState() != GameState.PREPARING) {
            sender.sendMessage("§7[§6村民战争§7] §c游戏已经开始或正在进行中，无法加入");
            return true;
        }

        int maxPlayers = game.getGameRule().getMaxPlayers();
        if (game.getPlayerCount() >= maxPlayers) {
            sender.sendMessage("§7[§6村民战争§7] §c游戏已满员");
            return true;
        }

        plugin.getGameManager().joinGame(player, game);
        return true;
    }

    @Override
    public @Nullable List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return plugin.getGameManager().getGames().stream()
                    .filter(g -> g.getState() == GameState.PREPARING)
                    .map(Game::getGameName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}