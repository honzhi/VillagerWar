package com.yourname.villagerwar.command.arguments;

import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.bridge.MMBridge;
import com.yourname.villagerwar.config.holder.SkillsConfig;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ReloadArgument implements SubCommand {
    private final VillagerWar plugin;

    public ReloadArgument(VillagerWar plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "重载所有配置文件";
    }

    @Override
    public String getUsage() {
        return "/vw reload";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("villagerwar.reload")) {
            sender.sendMessage("§7[§6村民战争§7] §c你没有权限执行此命令");
            return true;
        }

        sender.sendMessage("§7[§6村民战争§7] §e正在重载配置文件...");

        // 重载配置
        plugin.getConfigManager().reloadAll();
        MMBridge.reload();

        // 输出重载结果
        boolean debug = plugin.getConfigManager().getPluginConfig().isDebug();

        if (MMBridge.isMythicMobsLoaded()) {
            plugin.getLogger().info("[MMBridge] MythicMobs 插件已就绪");
        } else {
            plugin.getLogger().info("[MMBridge] MythicMobs 插件未安装或未启用，技能/怪物功能不可用");
        }

        if (MMBridge.isMythicMobsLoaded()) {
            SkillsConfig sc = plugin.getConfigManager().getSkillsConfig();
            for (SkillsConfig.SkillDef skill : sc.getSkills()) {
                String mmSkillId = skill.getMythicSkill();
                boolean found = MMBridge.hasSkill(mmSkillId);
                if (debug) {
                    if (found) {
                        plugin.getLogger().info("  [技能] " + skill.getId() + " (" + mmSkillId + ") 已就绪");
                    } else {
                        plugin.getLogger().warning("  [技能] " + skill.getId() + " (" + mmSkillId + ") 未在 MythicMobs 中找到");
                    }
                }
            }
        }

        sender.sendMessage("§7[§6村民战争§7] §a配置文件重载完成");
        return true;
    }

    @Override
    public @Nullable List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}