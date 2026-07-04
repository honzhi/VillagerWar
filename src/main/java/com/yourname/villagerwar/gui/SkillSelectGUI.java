package com.yourname.villagerwar.gui;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.GameState;
import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.config.holder.SkillsConfig;
import com.yourname.villagerwar.skill.GameSkill;
import com.yourname.villagerwar.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SkillSelectGUI {

    public static void open(Player player) {
        YamlConfiguration config = GUIUtils.loadConfig("skill_select");
        if (config == null) {
            player.sendMessage(MessageUtil.colorize("&c技能选择GUI配置不存在"));
            return;
        }

        Inventory inv = GUIUtils.createInventory(config, null);
        ConfigurationSection baseSection = config.getConfigurationSection("base");

        // 构建基础布局
        ConfigurationSection layoutSection = config.getConfigurationSection("layout");
        List<String> layoutLines = new ArrayList<>();
        if (layoutSection != null) {
            layoutLines.addAll(layoutSection.getStringList("page1"));
            if (layoutLines.isEmpty()) layoutLines.addAll(layoutSection.getStringList("1"));
        }

        for (int row = 0; row < layoutLines.size(); row++) {
            String line = layoutLines.get(row);
            for (int col = 0; col < line.length() && col < 9; col++) {
                char c = line.charAt(col);
                String key = String.valueOf(c);
                if (key.equals("0")) continue;

                int slot = row * 9 + col;
                ItemStack item = null;

                if (config.contains("skills." + key)) {
                    item = buildSkillItem(config.getConfigurationSection("skills." + key));
                }
                if (item == null && config.contains(key)) {
                    item = GUIUtils.buildItem(config.getConfigurationSection(key));
                }
                if (item == null && baseSection != null && baseSection.contains(key)) {
                    item = GUIUtils.buildItem(baseSection.getConfigurationSection(key));
                }
                if (item != null) inv.setItem(slot, item);
            }
        }

        // 动态填充技能到空槽位
        ConfigurationSection skillsSec = config.getConfigurationSection("skills");
        if (skillsSec != null) {
            int slot = 0;
            for (String skillKey : skillsSec.getKeys(false)) {
                ConfigurationSection skillSec = skillsSec.getConfigurationSection(skillKey);
                if (skillSec == null) continue;
                while (slot < inv.getSize() && inv.getItem(slot) != null) {
                    slot++;
                }
                if (slot >= inv.getSize()) break;
                inv.setItem(slot, buildSkillItem(skillSec));
                slot++;
            }
        }

        player.openInventory(inv);
        GUIUtils.setOpenGUI(player.getName(), "skill_select");
    }

    private static ItemStack buildSkillItem(ConfigurationSection section) {
        if (section == null) return null;
        String materialName = section.getString("material", "book");
        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material == null) material = Material.BOOK;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String displayName = section.getString("display_name", "Skill");
        if (!displayName.isEmpty()) meta.setDisplayName(MessageUtil.colorize(displayName));

        List<String> lore = new ArrayList<>();
        for (String line : section.getStringList("lore")) {
            lore.add(MessageUtil.colorize(line));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static void handleClick(Player player, int slot) {
        YamlConfiguration config = GUIUtils.loadConfig("skill_select");
        if (config == null) return;

        char c = GUIUtils.getSlotChar(config, slot);
        String key = String.valueOf(c);
        if (key.equals("0")) return;

        ConfigurationSection section = config.getConfigurationSection("skills." + key);
        if (section == null) {
            // 动态填充的技能没有layout字符映射，通过检查物品是否匹配技能配置
            org.bukkit.inventory.ItemStack clicked = player.getOpenInventory().getItem(slot);
            if (clicked == null || !clicked.hasItemMeta()) return;
            String displayName = clicked.getItemMeta().getDisplayName();
            ConfigurationSection skillsSec = config.getConfigurationSection("skills");
            if (skillsSec != null) {
                for (String skillKey : skillsSec.getKeys(false)) {
                    ConfigurationSection sec = skillsSec.getConfigurationSection(skillKey);
                    if (sec != null && MessageUtil.colorize(sec.getString("display_name", "")).equals(displayName)) {
                        section = sec;
                        break;
                    }
                }
            }
        }
        if (section == null) {
            // 尝试从base找
            section = config.getConfigurationSection("base." + key);
        }
        if (section == null) return;

        List<String> clicks = section.getStringList("click");
        if (clicks.isEmpty()) return;

        for (String action : clicks) {
            action = action.trim();
            if (action.startsWith("sound:")) {
                String[] parts = action.substring(6).trim().split(" ");
                try {
                    org.bukkit.Sound sound = org.bukkit.Sound.valueOf(parts[0]);
                    float vol = parts.length > 1 ? Float.parseFloat(parts[1]) : 1f;
                    float pit = parts.length > 2 ? Float.parseFloat(parts[2]) : 1f;
                    player.playSound(player.getLocation(), sound, vol, pit);
                } catch (Exception ignored) {}
            } else if (action.equals("close")) {
                player.closeInventory();
            } else if (action.startsWith("message:")) {
                player.sendMessage(MessageUtil.colorize(action.substring(8).trim()));
            } else if (action.equals("select_skill")) {
                handleSelectSkill(player, section);
            }
        }
    }

    private static void handleSelectSkill(Player player, ConfigurationSection section) {
        String skillId = section.getString("skill", "");
        if (skillId.isEmpty()) return;

        VillagerWar plugin = VillagerWar.getInstance();
        java.util.Optional<Game> gameOpt = plugin.getGameManager().getGame(player);
        if (gameOpt.isEmpty()) return;

        Game game = gameOpt.get();
        GamePlayer gp = game.getPlayer(player.getUniqueId());
        if (gp == null || game.getState() != GameState.SKILL_SELECT) return;

        SkillsConfig.SkillDef skillDef = plugin.getConfigManager().getSkillsConfig().getSkill(skillId);
        if (skillDef != null) {
            gp.setSkill(new GameSkill(
                skillDef.getId(), skillDef.getDisplayName(), skillDef.getCooldown(),
                skillDef.getMythicSkill(), skillDef.getMaterial(), "",
                skillDef.getModeKey(), skillDef.getTimer()));
            game.getSkillManager().markSkillSelected(player.getUniqueId());
            player.sendMessage(MessageUtil.colorize("&a已选择技能: &e" + skillDef.getDisplayName()));
            player.closeInventory();
        } else {
            player.sendMessage(MessageUtil.colorize("&c技能配置未找到: " + skillId));
        }
    }
}