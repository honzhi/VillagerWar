package com.yourname.villagerwar.gui;

import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.config.holder.MapConfig;
import com.yourname.villagerwar.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MapSelectGUI {

    private static final Map<String, String> selectedMap = new HashMap<>();

    public static String getSelectedMap(String playerName) {
        return selectedMap.get(playerName);
    }

    public static void clearSelectedMap(String playerName) {
        selectedMap.remove(playerName);
    }

    public static void open(Player player) {
        YamlConfiguration config = GUIUtils.loadConfig("map_select");
        if (config == null) {
            player.sendMessage(MessageUtil.colorize("&c地图选择GUI配置不存在"));
            return;
        }

        Inventory inv = GUIUtils.createInventory(config, null);
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

                if (config.contains("maps." + key)) {
                    item = buildMapItem(config.getConfigurationSection("maps." + key));
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
        GUIUtils.setOpenGUI(player.getName(), "map_select");
    }

    private static ItemStack buildMapItem(ConfigurationSection section) {
        if (section == null) return null;
        String materialName = section.getString("material", "map");
        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material == null) material = Material.MAP;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String displayName = section.getString("display_name", "Map");
        if (!displayName.isEmpty()) meta.setDisplayName(MessageUtil.colorize(displayName));

        List<String> lore = new ArrayList<>();
        for (String line : section.getStringList("lore")) {
            lore.add(MessageUtil.colorize(line));
        }
        String mapId = section.getString("map", "");
        MapConfig mapConfig = VillagerWar.getInstance().getConfigManager().getMapConfig(mapId);
        if (mapConfig != null) {
            String modeInfo = " &7可用模式: ";
            List<String> modes = new ArrayList<>(VillagerWar.getInstance().getConfigManager().getGameModesConfig().getPresets().keySet());
            modeInfo += String.join("&7, &e", modes);
            lore.add(MessageUtil.colorize(modeInfo));
        }
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    public static void handleClick(Player player, int slot) {
        YamlConfiguration config = GUIUtils.loadConfig("map_select");
        if (config == null) return;

        char c = GUIUtils.getSlotChar(config, slot);
        String key = String.valueOf(c);
        if (key.equals("0")) return;

        ConfigurationSection section = config.getConfigurationSection("maps." + key);
        if (section == null) return;

        List<String> clicks = section.getStringList("click");
        if (clicks.isEmpty()) clicks = config.getStringList("base." + key + ".click");

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
            } else if (action.equals("open")) {
                String mapId = section.getString("map", "");
                if (!mapId.isEmpty()) {
                    selectedMap.put(player.getName(), mapId);
                    String modeId = section.getString("mode", "standard");
                    ModeSelectGUI.open(player, mapId);
                }
            }
        }
    }
}