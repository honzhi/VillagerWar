package com.yourname.villagerwar.command.arguments;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GameState;
import com.yourname.villagerwar.VillagerWar;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class ListArgument implements SubCommand {
    private final VillagerWar plugin;

    public ListArgument(VillagerWar plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "列出所有活跃的游戏";
    }

    @Override
    public String getUsage() {
        return "/vw list";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        var games = plugin.getGameManager().getGames();

        if (games.isEmpty()) {
            sender.sendMessage("§7[§6村民战争§7] §e当前没有活跃的游戏");
            return true;
        }

        sender.sendMessage("§7===== §6活跃游戏列表 §7=====");
        for (Game game : games) {
            String stateDisplay = formatState(game.getState());
            sender.sendMessage("§e" + game.getGameName()
                    + " §7| §f状态: " + stateDisplay
                    + " §7| §f人数: §b" + game.getPlayerCount() + "§7/§e" + game.getGameRule().getMaxPlayers());
        }
        return true;
    }

    private String formatState(GameState state) {
        switch (state) {
            case WAITING: return "§a等待中";
            case PREPARING: return "§6准备中";
            case SKILL_SELECT: return "§e技能选择";
            case SKILL_SHOW: return "§e技能展示";
            case TELEPORT: return "§d传送中";
            case READY: return "§a准备就绪";
            case PLAYING: return "§c进行中";
            case ENDING: return "§6结算中";
            case REWARD: return "§6奖励发放";
            case RETURNING: return "§e返回大厅";
            default: return "§7未知";
        }
    }

    @Override
    public @Nullable List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
