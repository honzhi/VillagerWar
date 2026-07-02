package com.yourname.villagerwar.gui;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.config.holder.MapConfig;
import com.yourname.villagerwar.config.rule.GameRule;
import com.yourname.villagerwar.util.MessageUtil;
import com.yourname.villagerwar.world.GameWorld;
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
import java.util.concurrent.ConcurrentHashMap;

public class LobbyGUI {

    private static final Map<String, String> openGUIMap = new HashMap<>();
    private static final Map<String, String> selectedMap = new HashMap<>();
    private static final Map<String, List<Player>> matchQueue = new ConcurrentHashMap<>();

    public static void open(Player player, String guiName) {
        open(player, guiName, null);
    }

    public static void open(Player player, String guiName, String mapId) {
        File guiFile = new File(VillagerWar.getInstance().getDataFolder(), "game_gui/" + guiName + ".yml");
        if (!guiFile.exists()) {
            player.sendMessage(MessageUtil.colorize("&cGUI文件不存在: " + guiName));
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
            layoutLines.addAll(layoutSection.getStringList("page1"));
            if (layoutLines.isEmpty()) {
                layoutLines.addAll(layoutSection.getStringList("1"));
            }
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
                    item = buildMapItem(config.getConfigurationSection("maps." + key));
                } else if (guiName.equals("mode_select") && config.contains("modes." + key)) {
                    item = buildModeItem(config.getConfigurationSection("modes." + key), mapId);
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

    private static ItemStack buildMapItem(ConfigurationSection section) {
        if (section == null) return null;
        String materialName = section.getString("material", "book");
        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material == null) material = Material.BOOK;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String displayName = section.getString("display_name", "Map");
        meta.setDisplayName(MessageUtil.colorize(displayName));

        List<String> lore = new ArrayList<>();
        for (String line : section.getStringList("lore")) {
            MapConfig mapCfg = VillagerWar.getInstance().getConfigManager().getMapConfig(
                section.getString("map", ""));
            line = line.replace("{mode}", "CLASSIC")
                       .replace("{current}", String.valueOf(0))
                       .replace("{max}", String.valueOf(40));
            lore.add(MessageUtil.colorize(line));
        }
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
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
        meta.setDisplayName(MessageUtil.colorize(displayName));

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

    // ========== Click Handler ==========

    public static void handleClick(Player player, String guiName, int slot, String title, ItemStack clicked) {
        if (clicked == null || !clicked.hasItemMeta()) return;

        File guiFile = new File(VillagerWar.getInstance().getDataFolder(), "game_gui/" + guiName + ".yml");
        if (!guiFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(guiFile);

        List<String> layoutLines = new ArrayList<>();
        ConfigurationSection layoutSection = config.getConfigurationSection("layout");
        if (layoutSection != null) {
            layoutLines.addAll(layoutSection.getStringList("page1"));
        }

        char slotChar = '0';
        int row = slot / 9;
        int col = slot % 9;
        if (row < layoutLines.size() && col < layoutLines.get(row).length()) {
            slotChar = layoutLines.get(row).charAt(col);
        }
        String key = String.valueOf(slotChar);
        if (key.equals("0")) return;

        ConfigurationSection itemSection = null;
        if (guiName.equals("map_select")) {
            itemSection = config.getConfigurationSection("maps." + key);
        } else if (guiName.equals("mode_select")) {
            itemSection = config.getConfigurationSection("modes." + key);
        }
        if (itemSection == null) itemSection = config.getConfigurationSection(key);
        if (itemSection == null) itemSection = config.getConfigurationSection("base." + key);
        if (itemSection == null) return;

        List<String> actions = itemSection.getStringList("click");
        if (actions.isEmpty()) return;

        for (String action : actions) {
            executeAction(player, guiName, itemSection, action.trim());
        }
    }

    private static void executeAction(Player player, String guiName, ConfigurationSection itemSection, String action) {
        if (action.startsWith("sound:")) {
            String[] parts = action.substring(6).trim().split(" ");
            try {
                Sound sound = Sound.valueOf(parts[0]);
                float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
                float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (Exception ignored) {}
            return;
        }

        if (action.equals("close")) {
            player.closeInventory();
            return;
        }

        if (action.startsWith("message:")) {
            String msg = action.substring(8).trim();
            player.sendMessage(MessageUtil.colorize(msg));
            return;
        }

        if (action.equals("open") && guiName.equals("map_select")) {
            String mapId = itemSection.getString("map", "");
            if (!mapId.isEmpty()) {
                selectedMap.put(player.getName(), mapId);
                open(player, "mode_select", mapId);
            }
            return;
        }

        if (action.equals("match")) {
            handleMatch(player);
            return;
        }
    }

    // ========== Match Logic ==========

    private static void handleMatch(Player player) {
        String mapId = selectedMap.get(player.getName());
        if (mapId == null || mapId.isEmpty()) {
            player.sendMessage(MessageUtil.colorize("&c请先选择一张地图"));
            open(player, "map_select");
            return;
        }

        String modeId = "CLASSIC";

        if (VillagerWar.getInstance().getGameManager().getGame(player).isPresent()) {
            player.sendMessage(MessageUtil.colorize("&c你已在游戏中"));
            return;
        }

        // Save inventory and apply queuing preset
        VillagerWar.getInstance().getInventoryManager().save(player);
        VillagerWar.getInstance().getInventoryManager().apply(player, "queuing");

        String queueKey = mapId + ":" + modeId;
        matchQueue.computeIfAbsent(queueKey, k -> Collections.synchronizedList(new ArrayList<>()));
        List<Player> queue = matchQueue.get(queueKey);

        if (queue.contains(player)) {
            player.sendMessage(MessageUtil.colorize("&c你已在匹配队列中"));
            return;
        }

        queue.add(player);
        player.sendMessage(MessageUtil.colorize("&a已加入匹配队列（" + mapId + " - 模式 " + modeId + "）"));

        GameRule gameRule = VillagerWar.getInstance().getConfigManager().createGameRule(modeId);
        int minPlayers = gameRule.getMinPlayers();

        if (queue.size() >= minPlayers) {
            List<Player> matched = new ArrayList<>(queue);
            queue.clear();

            GameWorld gameWorld = VillagerWar.getInstance().getWorldManager().createWorld(mapId);
            if (gameWorld == null) {
                for (Player p : matched) {
                    p.sendMessage(MessageUtil.colorize("&c地图加载失败"));
                    VillagerWar.getInstance().getInventoryManager().clear(p);
                    VillagerWar.getInstance().getInventoryManager().restore(p);
                }
                return;
            }

            Game game = VillagerWar.getInstance().getGameManager().createGame(mapId, gameWorld, gameRule);
            if (game == null) {
                for (Player p : matched) {
                    p.sendMessage(MessageUtil.colorize("&c游戏创建失败"));
                    VillagerWar.getInstance().getInventoryManager().clear(p);
                    VillagerWar.getInstance().getInventoryManager().restore(p);
                }
                return;
            }

            for (Player p : matched) {
                VillagerWar.getInstance().getGameManager().joinGame(p, game);
                VillagerWar.getInstance().getInventoryManager().apply(p, "lobby");
                selectedMap.remove(p.getName());
            }

            for (Player p : matched) {
                p.sendTitle(MessageUtil.colorize("&a&lMatch Found!"),
                            MessageUtil.colorize("&7Entering game..."), 10, 40, 20);
            }

            Bukkit.getScheduler().runTaskLater(VillagerWar.getInstance(), () -> {
                game.getController().transitionTo(com.yourname.villagerwar.GameState.SKILL_SELECT);
            }, 60L);
        } else {
            int need = minPlayers - queue.size();
            player.sendMessage(MessageUtil.colorize("&e等待更多玩家加入... 还需要 &c" + need + " &e人"));
        }
    }

    // ========== Utility ==========

    public static String getOpenGUI(String playerName) { return openGUIMap.get(playerName); }

    public static void removePlayer(String playerName) {
        openGUIMap.remove(playerName);
        for (List<Player> queue : matchQueue.values()) {
            queue.removeIf(p -> p.getName().equals(playerName));
        }
    }
}