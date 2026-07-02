package com.yourname.villagerwar.command.arguments;

import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.gui.LobbyGUI;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class GuiArgument implements SubCommand {

    private final VillagerWar plugin;

    public GuiArgument(VillagerWar plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "gui";
    }

    @Override
    public String getDescription() {
        return "打开大厅界面";
    }

    @Override
    public String getUsage() {
        return "/vw gui";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c该命令只能由玩家执行");
            return true;
        }
        Player player = (Player) sender;
        LobbyGUI.open(player, "map_select");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}