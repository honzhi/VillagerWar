package com.yourname.villagerwar.gui;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GameState;

import com.yourname.villagerwar.VillagerWar;import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.config.holder.MapConfig;
import com.yourname.villagerwar.GamePlayer;import com.yourname.villagerwar.config.rule.GameRule;import com.yourname.villagerwar.GamePlayer;
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
            String modeId = itemSection.getString("mode", "standard"); handleMatch(player, modeId);
            return;
        }
    }

    // ========== Match Logic ==========

        private static void handleMatch(Player player, String modeId) {
        String mapId = selectedMap.get(player.getName());
        if (mapId == null || mapId.isEmpty()) {
            player.sendMessage(MessageUtil.colorize("&c请先选择一张地图"));
            open(player, "map_select");
            return;
        }
        player.closeInventory();

        VillagerWar.getInstance().getLogger().info("[Debug] handleMatch: map=" + mapId + " mode=" + modeId);

        // 检查是否已在游戏中
        if (VillagerWar.getInstance().getGameManager().getGame(player).isPresent()) {
            player.sendMessage(MessageUtil.colorize("&c你已在游戏中"));
            return;
        }

        // 保存背包并应用备战席预设
        VillagerWar.getInstance().getInventoryManager().save(player);
        VillagerWar.getInstance().getInventoryManager().apply(player, "matching");

        // 查找或创建游戏
        Game game = VillagerWar.getInstance().getGameManager().findOrCreateGame(mapId, modeId);
        if (game == null) {
            player.sendMessage(MessageUtil.colorize("&c游戏创建失败"));
            VillagerWar.getInstance().getInventoryManager().clear(player);
            VillagerWar.getInstance().getInventoryManager().restore(player);
            return;
        }

                // 加入游戏并传送到备战席（reserves_seat）
        VillagerWar.getInstance().getGameManager().joinGame(player, game);

        // 创建备战席世界实例
        GameWorld reservesSeat = VillagerWar.getInstance().getWorldManager().createReservesSeat();
        if (reservesSeat == null || !reservesSeat.isLoaded()) {
            player.sendMessage(MessageUtil.colorize("&c未能识别到备战席地图，请联系管理员"));
            VillagerWar.getInstance().getInventoryManager().clear(player);
            VillagerWar.getInstance().getInventoryManager().restore(player);
            VillagerWar.getInstance().getGameManager().leaveGame(player);
            return;
        }
        game.setReservesSeatName(reservesSeat.getWorldName());
        org.bukkit.Location seatLoc = VillagerWar.getInstance().getWorldManager().getReservesSeatLocation(reservesSeat);
        if (seatLoc != null) {
            player.teleport(seatLoc);
            player.setFallDistance(0);
        }
        selectedMap.remove(player.getName());

        // 通知玩家
        player.sendTitle(MessageUtil.colorize("&a&l匹配成功"),
            MessageUtil.colorize("&7正在进入 " + game.getMapId()), 10, 40, 20);

        player.sendMessage(MessageUtil.colorize("&a已加入游戏！当前人数: &e" + game.getPlayerCount()));

        // 检查是否满足开始条件（人齐了开始游戏）
        GameRule gameRule = game.getGameRule();
        VillagerWar.getInstance().getLogger().info("[Debug] Players=" + game.getPlayerCount() + "/" + gameRule.getMinPlayers());
                if (game.getPlayerCount() >= gameRule.getMinPlayers()) {
            VillagerWar.getInstance().getLogger().info("[Debug] 人齐了！10秒后开始游戏...");
            // 通知所有玩家准备
            for (GamePlayer gp : game.getPlayers()) {
                Player p = gp.getPlayer();
                if (p != null) {
                    p.sendTitle(MessageUtil.colorize("&a&l玩家人数已满"),
                        MessageUtil.colorize("&710秒后游戏开始..."), 10, 60, 20);
                }
            }

            // 状态链：分配队伍 -> 技能选择 -> 技能展示 -> 传送 -> Ready -> 游戏开始
            Bukkit.getScheduler().runTaskLater(VillagerWar.getInstance(), () -> {
                Optional<Game> gameOpt = VillagerWar.getInstance().getGameManager().getGame(game.getGameId());
                if (gameOpt.isEmpty()) return;

                VillagerWar.getInstance().getLogger().info("[Debug] 第一步：分配队伍（备战席）");
                game.setState(GameState.PREPARING);

                Bukkit.getScheduler().runTaskLater(VillagerWar.getInstance(), () -> {
                    if (game.getState() != GameState.PREPARING) return;
                    VillagerWar.getInstance().getLogger().info("[Debug] 第二步：技能选择");
                    game.setState(GameState.SKILL_SELECT);

                    Bukkit.getScheduler().runTaskLater(VillagerWar.getInstance(), () -> {
                        if (game.getState() != GameState.SKILL_SELECT) return;
                        VillagerWar.getInstance().getLogger().info("[Debug] 第三步：技能展示");
                        game.setState(GameState.SKILL_SHOW);

                        Bukkit.getScheduler().runTaskLater(VillagerWar.getInstance(), () -> {
                            if (game.getState() != GameState.SKILL_SHOW) return;
                            VillagerWar.getInstance().getLogger().info("[Debug] 第四步：传送至游戏地图");
                            game.setState(GameState.TELEPORT);

                            Bukkit.getScheduler().runTaskLater(VillagerWar.getInstance(), () -> {
                                if (game.getState() != GameState.TELEPORT) return;
                                VillagerWar.getInstance().getLogger().info("[Debug] 第五步：Ready 倒计时");
                                game.setState(GameState.READY);

                                Bukkit.getScheduler().runTaskLater(VillagerWar.getInstance(), () -> {
                                    if (game.getState() != GameState.READY) return;
                                    VillagerWar.getInstance().getLogger().info("[Debug] 第六步：游戏开始！");
                                    game.setState(GameState.PLAYING);
                                }, 60L);  // Ready 3s

                            }, 60L);  // 传送后3s

                        }, 60L);  // 技能展示3s

                    }, 100L);  // 技能选择5s

                }, 100L);  // 分配队伍后5s

            }, 200L);  // 总倒计时10s
            int need = gameRule.getMinPlayers() - game.getPlayerCount();
            player.sendMessage(MessageUtil.colorize("&e等待更多玩家加入... 还需要 &c" + need + " &e人"));
        }
    }

    public static String getOpenGUI(String playerName) { return openGUIMap.get(playerName); }

    public static void removePlayer(String playerName) {
        openGUIMap.remove(playerName);
        for (List<Player> queue : matchQueue.values()) {
            queue.removeIf(p -> p.getName().equals(playerName));
        }
    }
}// test
