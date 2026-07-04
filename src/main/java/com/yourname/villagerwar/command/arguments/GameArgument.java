package com.yourname.villagerwar.command.arguments;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.GameState;
import com.yourname.villagerwar.VillagerWar;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class GameArgument implements SubCommand {
    private final VillagerWar plugin;
    private static final List<String> ACTIONS = Arrays.asList("leave", "start", "stop", "addvalue");

    public GameArgument(VillagerWar plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "game";
    }

    @Override
    public String getDescription() {
        return "游戏管理：离开/开始/结束/增加金币";
    }

    @Override
    public String getUsage() {
        return "/vw game <leave|start|stop|addvalue> [玩家] [数量]";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令仅限玩家执行");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§7[§6村民战争§7] §c用法: " + getUsage());
            return true;
        }

        Player player = (Player) sender;
        Optional<Game> gameOpt = plugin.getGameManager().getGame(player);

        if (gameOpt.isEmpty()) {
            sender.sendMessage("§7[§6村民战争§7] §c你不在任何游戏中");
            return true;
        }

        Game game = gameOpt.get();
        String action = args[0].toLowerCase();

        switch (action) {
            case "leave":
                return handleLeave(sender, player, game);
            case "start":
                return handleStart(sender, player, game);
            case "stop":
                return handleStop(sender, player, game);
            case "addvalue":
                return handleAddValue(sender, player, game, args);
            default:
                sender.sendMessage("§7[§6村民战争§7] §c未知操作 §e" + args[0]);
                return true;
        }
    }

    private boolean handleLeave(CommandSender sender, Player player, Game game) {
        if (!player.hasPermission("villagerwar.admin")) {
            sender.sendMessage("§c你没有权限执行此操作");
            return true;
        }
        plugin.getInventoryManager().clear(player);
        plugin.getInventoryManager().restore(player);
        plugin.getGameManager().leaveGame(player);
        sender.sendMessage("§7[§6村民战争§7] §e已强制离开游戏 §a" + game.getGameName());
        return true;
    }

    private boolean handleStart(CommandSender sender, Player player, Game game) {
        if (!player.hasPermission("villagerwar.admin")) {
            sender.sendMessage("§c你没有权限执行此操作");
            return true;
        }
        if (game.getState() != GameState.PREPARING) {
            sender.sendMessage("§7[§6村民战争§7] §c游戏已经开始或正在进行中");
            return true;
        }
        // 强制开始：跳转到技能选择阶段
        sender.sendMessage("§7[§6村民战争§7] §a已强制开始游戏");
        game.getController().start();
        game.setState(GameState.SKILL_SELECT);
        return true;
    }

    private boolean handleStop(CommandSender sender, Player player, Game game) {
        if (!player.hasPermission("villagerwar.admin")) {
            sender.sendMessage("§c你没有权限执行此操作");
            return true;
        }
        if (game.getState() != GameState.PLAYING) {
            sender.sendMessage("§7[§6村民战争§7] §c游戏不在进行中，无法强制结束");
            return true;
        }
        // 广播平局结束
        for (GamePlayer gp : game.getPlayers()) {
            Player p = gp.getPlayer();
            if (p != null && p.isOnline()) {
                p.sendMessage("§7[§6村民战争§7] §e管理员强制结束了本局游戏（平局）");
            }
        }
        VillagerWar.getInstance().getLogger().info("[Debug] 管理员强制结束游戏，进入ENDING状态");
        game.setState(GameState.ENDING);
        sender.sendMessage("§7[§6村民战争§7] §a已以平局结束游戏");
        return true;
    }

    private boolean handleAddValue(CommandSender sender, Player player, Game game, String[] args) {
        if (!player.hasPermission("villagerwar.admin")) {
            sender.sendMessage("§c你没有权限执行此操作");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("§7[§6村民战争§7] §c用法: /vw game addvalue <玩家名> <数量>");
            return true;
        }

        String targetName = args[1];
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§7[§6村民战争§7] §c数量必须为整数");
            return true;
        }
        if (amount <= 0) {
            sender.sendMessage("§7[§6村民战争§7] §c数量必须大于0");
            return true;
        }

        // 查找目标玩家
        Player target = Bukkit.getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            sender.sendMessage("§7[§6村民战争§7] §c玩家 §e" + targetName + " §c不在线");
            return true;
        }

        Optional<Game> targetGameOpt = plugin.getGameManager().getGame(target);
        if (targetGameOpt.isEmpty() || !targetGameOpt.get().getGameId().equals(game.getGameId())) {
            sender.sendMessage("§7[§6村民战争§7] §c玩家 §e" + targetName + " §c不在此游戏中");
            return true;
        }

        GamePlayer gp = game.getPlayer(target.getUniqueId());
        if (gp == null) {
            sender.sendMessage("§7[§6村民战争§7] §c无法找到该玩家的游戏数据");
            return true;
        }

        game.getEconomyManager().addGold(gp, amount);
        sender.sendMessage("§7[§6村民战争§7] §a已给 §e" + targetName + " §a增加 §6" + amount + " §a金币");
        target.sendMessage("§7[§6村民战争§7] §a管理员给你增加了 §6" + amount + " §a金币");
        return true;
    }

    @Override
    public @Nullable List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return ACTIONS.stream()
                    .filter(a -> a.startsWith(partial))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("addvalue")) {
            // 补全在线玩家名
            String partial = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}