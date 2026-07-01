package com.yourname.villagerwar.command.arguments;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface SubCommand {
    String getName();
    String getDescription();
    String getUsage();
    boolean execute(CommandSender sender, String[] args);
    @Nullable List<String> tabComplete(CommandSender sender, String[] args);
}
