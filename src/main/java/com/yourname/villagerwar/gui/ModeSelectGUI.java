package com.yourname.villagerwar.gui;

import com.yourname.villagerwar.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ModeSelectGUI {

    public static void open(Player player, String mapId) {
        YamlConfiguration config = GUIUtils.loadConfig("mode_select");
        if (config == null) {
            player.sendMessage(MessageUtil.colorize("&c模式选择GUI配置不存在"));
            return;
        }

        Inventory inv = GUIUtils.createInventory(config, mapId);
        ConfigurationSection baseSection = config.getConfigurationSection("base");

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

                if (config.contains("modes." + key)) {
                    item = buildModeItem(config.getConfigurationSection("modes." + key), mapId);
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

        player.openInventory(inv);
        GUIUtils.setOpenGUI(player.getName(), "mode_select");
    }

    private static ItemStack buildModeItem(ConfigurationSection section, String mapId) {
        if (section == null) return null;
        String materialName = section.getString("material", "knowledge_book");
        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material == null) material = Material.KNOWLEDGE_BOOK;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String displayName = section.getString("display_name", "Mode");
        if (!displayName.isEmpty()) meta.setDisplayName(MessageUtil.colorize(displayName));

        List<String> lore = new ArrayList<>();
        for (String line : section.getStringList("lore")) {
            line = line.replace("{max_time}", "600")
                       .replace("{respawn_time}", "5")
                       .replace("{victory_mode}", "KILL_ALL");
            lore.add(MessageUtil.colorize(line));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static void handleClick(Player player, int slot) {
        YamlConfiguration config = GUIUtils.loadConfig("mode_select");
        if (config == null) return;

        char c = GUIUtils.getSlotChar(config, slot);
        String key = String.valueOf(c);
        if (key.equals("0")) return;

        ConfigurationSection section = config.getConfigurationSection("modes." + key);
        if (section == null) section = config.getConfigurationSection(key);
        if (section == null) section = config.getConfigurationSection("base." + key);
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
            } else if (action.equals("match")) {
                String modeId = section.getString("mode", "standard");
                MatchHandler.handleMatch(player, modeId);
            }
        }
    }
}