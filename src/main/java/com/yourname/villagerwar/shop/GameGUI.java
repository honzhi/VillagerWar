package com.yourname.villagerwar.shop;

import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class GameGUI {

    private static final Map<String, String> openGUIMap = new HashMap<>();

    public static void open(Player player, String guiName) {
        open(player, guiName, null);
    }

    public static void open(Player player, String guiName, String mapId) {
        File guiFile = new File(VillagerWar.getInstance().getDataFolder(), "game_gui/" + guiName + ".yml");
        if (!guiFile.exists()) {
            player.sendMessage(MessageUtil.colorize("&cGUI 文件不存在: " + guiName));
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(guiFile);
        String rawTitle = config.getString("title", "&6GUI");
        if (mapId != null) {
            rawTitle = rawTitle.replace("{map_name}", mapId);
        }
        String title = MessageUtil.colorize(rawTitle);

        ConfigurationSection layoutSection = config.getConfigurationSection("layout");
        List<String> layoutLines = new ArrayList<>();
        if (layoutSection != null) {
            List<String> page = layoutSection.getStringList("page1");
            if (page.isEmpty()) page = layoutSection.getStringList("1");
            layoutLines.addAll(page);
        }

        int rows = layoutLines.size();
        if (rows < 1 || rows > 6) rows = 6;

        Inventory inv = Bukkit.createInventory(null, rows * 9, title);
        ConfigurationSection baseSection = config.getConfigurationSection("base");

        for (int row = 0; row < layoutLines.size(); row++) {
            String line = layoutLines.get(row);
            for (int col = 0; col < line.length() && col < 9; col++) {
                char c = line.charAt(col);
                String key = String.valueOf(c);
                if (key.equals("0")) continue;

                int slot = row * 9 + col;
                ItemStack item = null;

                if (guiName.equals("map_select") && config.contains("maps." + key)) {
                    item = buildItem(config.getConfigurationSection("maps." + key));
                } else if (guiName.equals("mode_select") && config.contains("modes." + key)) {
                    item = buildItem(config.getConfigurationSection("modes." + key));
                }

                if (item == null && config.contains(key)) {
                    item = buildItem(config.getConfigurationSection(key));
                }

                if (item == null && baseSection != null && baseSection.contains(key)) {
                    item = buildItem(baseSection.getConfigurationSection(key));
                }

                if (item != null) inv.setItem(slot, item);
            }
        }

        player.openInventory(inv);
        openGUIMap.put(player.getName(), guiName);
    }

    private static ItemStack buildItem(ConfigurationSection section) {
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

    private static String stripColor(String input) {
        return input == null ? "" : input.replaceAll("§[0-9a-fk-or]", "").trim();
    }

    public static void handleClick(Player player, String guiName, int slot, String title, ItemStack clicked) {
        if (clicked == null || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        String displayName = meta.getDisplayName();
        if (displayName == null) return;

        String stripped = stripColor(displayName);

        if (stripped.contains("关闭")) {
            player.closeInventory();
            openGUIMap.remove(player.getName());
            return;
        }
        if (stripped.contains("返回")) {
            open(player, "map_select");
            return;
        }
        if (stripped.contains("页")) {
            return;
        }

        if (guiName.equals("map_select")) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            open(player, "mode_select", "map");
        }
    }

    public static String getOpenGUI(String playerName) { return openGUIMap.get(playerName); }
    public static void removePlayer(String playerName) { openGUIMap.remove(playerName); }
}