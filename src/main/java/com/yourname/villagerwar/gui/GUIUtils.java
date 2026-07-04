package com.yourname.villagerwar.gui;

import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import org.bukkit.Bukkit;
import java.util.*;

public class GUIUtils {

    private static final Map<String, String> openGUIMap = new HashMap<>();

    public static String getOpenGUI(String playerName) {
        return openGUIMap.get(playerName);
    }

    public static void setOpenGUI(String playerName, String guiName) {
        openGUIMap.put(playerName, guiName);
    }

    public static void removePlayer(String playerName) {
        openGUIMap.remove(playerName);
    }

    public static YamlConfiguration loadConfig(String guiName) {
        File file = new File(VillagerWar.getInstance().getDataFolder(), "game_gui/" + guiName + ".yml");
        if (!file.exists()) return null;
        return YamlConfiguration.loadConfiguration(file);
    }

    public static ItemStack buildItem(ConfigurationSection section) {
        if (section == null) return null;
        String materialName = section.getString("material", "stone");
        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material == null) material = Material.STONE;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String displayName = section.getString("display_name", "");
        if (!displayName.isEmpty()) meta.setDisplayName(MessageUtil.colorize(displayName));

        List<String> lore = section.getStringList("lore");
        if (!lore.isEmpty()) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) coloredLore.add(MessageUtil.colorize(line));
            meta.setLore(coloredLore);
        }
        item.setItemMeta(meta);
        return item;
    }

    public static Inventory createInventory(YamlConfiguration config, String titleSuffix) {
        String rawTitle = config.getString("title", "&6GUI");
        if (titleSuffix != null) rawTitle = rawTitle.replace("{map_name}", titleSuffix);
        String title = MessageUtil.colorize(rawTitle);

        ConfigurationSection layoutSection = config.getConfigurationSection("layout");
        List<String> layoutLines = new ArrayList<>();
        if (layoutSection != null) {
            layoutLines.addAll(layoutSection.getStringList("page1"));
            if (layoutLines.isEmpty()) layoutLines.addAll(layoutSection.getStringList("1"));
        }
        int rows = Math.max(1, Math.min(layoutLines.size(), 6));
        return Bukkit.createInventory(null, rows * 9, title);
    }

    public static void buildBaseLayout(Inventory inv, YamlConfiguration config) {
        ConfigurationSection layoutSection = config.getConfigurationSection("layout");
        List<String> layoutLines = new ArrayList<>();
        if (layoutSection != null) {
            layoutLines.addAll(layoutSection.getStringList("page1"));
            if (layoutLines.isEmpty()) layoutLines.addAll(layoutSection.getStringList("1"));
        }
        ConfigurationSection baseSection = config.getConfigurationSection("base");

        for (int row = 0; row < layoutLines.size(); row++) {
            String line = layoutLines.get(row);
            for (int col = 0; col < line.length() && col < 9; col++) {
                char c = line.charAt(col);
                String key = String.valueOf(c);
                if (key.equals("0")) continue;

                int slot = row * 9 + col;
                ItemStack item = null;

                if (config.contains(key)) {
                    item = buildItem(config.getConfigurationSection(key));
                }
                if (item == null && baseSection != null && baseSection.contains(key)) {
                    item = buildItem(baseSection.getConfigurationSection(key));
                }
                if (item != null) inv.setItem(slot, item);
            }
        }
    }

    public static char getSlotChar(YamlConfiguration config, int slot) {
        ConfigurationSection layoutSection = config.getConfigurationSection("layout");
        List<String> layoutLines = new ArrayList<>();
        if (layoutSection != null) {
            layoutLines.addAll(layoutSection.getStringList("page1"));
            if (layoutLines.isEmpty()) layoutLines.addAll(layoutSection.getStringList("1"));
        }
        int row = slot / 9;
        int col = slot % 9;
        if (row < layoutLines.size() && col < layoutLines.get(row).length()) {
            return layoutLines.get(row).charAt(col);
        }
        return '0';
    }

    public static ConfigurationSection findSection(YamlConfiguration config, String guiName, String key) {
        ConfigurationSection section = null;
        if (guiName.equals("map_select") && config.contains("maps." + key)) {
            section = config.getConfigurationSection("maps." + key);
        } else if (guiName.equals("mode_select") && config.contains("modes." + key)) {
            section = config.getConfigurationSection("modes." + key);
        } else if (guiName.equals("skill_select") && config.contains("skills." + key)) {
            section = config.getConfigurationSection("skills." + key);
        }
        if (section == null && config.contains(key)) {
            section = config.getConfigurationSection(key);
        }
        if (section == null && config.contains("base." + key)) {
            section = config.getConfigurationSection("base." + key);
        }
        return section;
    }
}