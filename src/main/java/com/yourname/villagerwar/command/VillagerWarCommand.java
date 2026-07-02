package com.yourname.villagerwar.command;

import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.command.arguments.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class VillagerWarCommand implements CommandExecutor, TabCompleter {
    private final VillagerWar plugin;
    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    public VillagerWarCommand(VillagerWar plugin) {
        this.plugin = plugin;
        registerSubCommands();
    }

    private void registerSubCommands() {
        register(new CreateArgument(plugin));
        register(new JoinArgument(plugin));
        register(new LeaveArgument(plugin));
        register(new StartArgument(plugin));
        register(new ListArgument(plugin));
        register(new SetMapArgument(plugin));
        register(new GuiArgument(plugin));
        register(new MapArgument(plugin));
        register(new ReloadArgument(plugin));
    }

    private void register(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                            @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subName = args[0].toLowerCase();
        SubCommand sub = subCommands.get(subName);

        if (sub == null) {
            sender.sendMessage("§7[§6村民战争§7] §c未知子命令 §e" + subName);
            sendHelp(sender);
            return true;
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return sub.execute(sender, subArgs);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return subCommands.keySet().stream()
                    .filter(name -> name.startsWith(partial))
                    .collect(Collectors.toList());
        }

        if (args.length >= 2) {
            SubCommand sub = subCommands.get(args[0].toLowerCase());
            if (sub != null) {
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                return sub.tabComplete(sender, subArgs);
            }
        }

        return Collections.emptyList();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§7===== §6村民战争 §7=====");
        sender.sendMessage("§e/vw help §7- 显示帮助");
        for (SubCommand sub : subCommands.values()) {
            sender.sendMessage("§e" + sub.getUsage() + " §7- " + sub.getDescription());
        }
    }
}