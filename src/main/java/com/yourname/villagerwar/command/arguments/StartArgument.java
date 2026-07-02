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

public class StartArgument implements SubCommand {
    private final VillagerWar plugin;

    public StartArgument(VillagerWar plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "start";
    }

    @Override
    public String getDescription() {
        return "开始游戏（管理员）";
    }

    @Override
    public String getUsage() {
        return "/vw start";
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

        if (game.getState() != GameState.PREPARING) {
            sender.sendMessage("§7[§6村民战争§7] §c游戏已经开始或正在进行中");
            return true;
        }

        game.getController().transitionTo(GameState.SKILL_SELECT);
        game.getUiManager().getMessageManager().broadcastMessage("game.start");
        return true;
    }

    @Override
    public @Nullable List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}