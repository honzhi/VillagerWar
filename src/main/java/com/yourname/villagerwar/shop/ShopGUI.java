package com.yourname.villagerwar.shop;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.util.MessageUtil;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商店 GUI
 * 读取 shop/*.yml 配置，构建可交互的物品商店界面
 */
public class ShopGUI {

    private static final Map<String, ShopGUI> shopCache = new HashMap<>();
    private static final Map<String, String> openShopMap = new HashMap<>(); // PlayerName → ShopName

    private final String name;
    private final String title;
    private final List<String> layout;
    private final Map<String, ShopItem> items;
    private final int rows;

    /**
     * 从配置文件加载商店
     * @param file shop/*.yml 文件
     */
    public ShopGUI() {
        this(new java.io.File(VillagerWar.getInstance().getDataFolder(), "shop/defaults.yml"));
    }
    public ShopGUI(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.name = file.getName().replace(".yml", "");
        this.title = MessageUtil.colorize(config.getString("title", "&6商店"));
        this.layout = config.getStringList("layout");
        this.items = new HashMap<>();

        // 解析 rows: layout 的行数决定
        this.rows = layout.size();

        // 解析 base 段
        ConfigurationSection baseSection = config.getConfigurationSection("base");
        if (baseSection != null) {
            for (String key : baseSection.getKeys(false)) {
                ConfigurationSection itemSection = baseSection.getConfigurationSection(key);
                if (itemSection != null) {
                    items.put(key.toUpperCase(), new ShopItem(key.toUpperCase(), itemSection));
                }
            }
        }
    }

    /**
     * 打开商店 GUI（静态入口）
     * @param player   玩家
     * @param shopName 商店名称（不带 .yml）
     */
    public static void open(Player player, String shopName) {
        // 从缓存或文件加载商店
        ShopGUI shop = shopCache.get(shopName);
        if (shop == null) {
            File shopFile = new File(VillagerWar.getInstance().getDataFolder(), "shop/" + shopName + ".yml");
            if (!shopFile.exists()) {
                MessageUtil.sendMessage(player, "error.command_denied");
                return;
            }
            shop = new ShopGUI(shopFile);
            shopCache.put(shopName, shop);
        }

        Inventory inv = shop.buildInventory(player);
        player.openInventory(inv);
        openShopMap.put(player.getName(), shopName);
    }

    /**
     * 构建商店物品栏
     */
    private Inventory buildInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, rows * 9, title);

        // 按 layout 布局放置物品
        for (int row = 0; row < layout.size(); row++) {
            String line = layout.get(row);
            for (int col = 0; col < line.length() && col < 9; col++) {
                char c = line.charAt(col);
                String key = String.valueOf(c).toUpperCase();

                if (key.equals("0")) continue; // 空位

                ShopItem shopItem = items.get(key);
                if (shopItem == null) continue;

                int slot = row * 9 + col;
                ItemStack displayItem = createDisplayItem(shopItem, player);
                inv.setItem(slot, displayItem);
            }
        }

        return inv;
    }

    /**
     * 创建展示物品
     */
    private ItemStack createDisplayItem(ShopItem shopItem, Player player) {
        ItemStack item = new ItemStack(shopItem.getMaterial(), shopItem.getAmount());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (!shopItem.getDisplayName().isEmpty()) {
                meta.setDisplayName(MessageUtil.colorize(shopItem.getDisplayName()));
            }
            List<String> coloredLore = new ArrayList<>();
            for (String line : shopItem.getLore()) {
                coloredLore.add(MessageUtil.colorize(line));
            }
            if (shopItem.getValue() > 0) {
                coloredLore.add(MessageUtil.colorize("&7价格: &6" + shopItem.getValue() + " 金币"));
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 处理点击事件（由监听器调用）
     * @param event 物品栏点击事件
     */
    public static void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String shopName = openShopMap.get(player.getName());
        if (shopName == null) return;

        event.setCancelled(true);

        ShopGUI shop = shopCache.get(shopName);
        if (shop == null) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= shop.rows * 9) return;

        // 计算 layout 中对应的键
        int row = slot / 9;
        int col = slot % 9;
        if (row >= shop.layout.size()) return;

        String line = shop.layout.get(row);
        if (col >= line.length()) return;

        char c = line.charAt(col);
        String key = String.valueOf(c).toUpperCase();
        if (key.equals("0")) return;

        ShopItem shopItem = shop.items.get(key);
        if (shopItem == null) return;

        // 获取玩家所属游戏
        Game game = VillagerWar.getInstance().getGameManager().getGame(player).orElse(null);
        GamePlayer gp = (game != null) ? game.getPlayer(player.getUniqueId()) : null;

        boolean success;
        if (gp != null) {
            success = shopItem.checkConditions(gp, game.getGameTime() / 20);
        } else {
            // 不在游戏中时仅检查 value=0
            success = shopItem.getValue() == 0;
        }

        // 扣除金币
        if (success && gp != null && shopItem.getValue() > 0) {
            if (gp.takeGold(shopItem.getValue())) {
                success = true;
            } else {
                success = false;
            }
        }

        // 执行动作链
        List<String> actions = success ? shopItem.getBuyActions().getSuccess()
                                       : shopItem.getBuyActions().getFailure();
        executeActions(player, actions, shop, shopItem);
    }

    /**
     * 执行动作链
     * 支持的动作类型: command, title, message, sound, close, open
     */
    private static void executeActions(Player player, List<String> actions, ShopGUI currentShop, ShopItem item) {
        if (actions == null) return;

        for (String action : actions) {
            action = action.trim();
            if (action.isEmpty()) continue;

            if (action.startsWith("command:")) {
                String cmd = action.substring("command:".length()).trim();
                cmd = cmd.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);

            } else if (action.startsWith("title:")) {
                String[] parts = action.substring("title:".length()).trim().split(" ");
                if (parts.length >= 1) {
                    StringBuilder titleText = new StringBuilder();
                    int fadeIn = 20, stay = 60, fadeOut = 20;
                    int index = 0;

                    // 尝试解析时间和标题
                    if (parts.length >= 3) {
                        try {
                            fadeIn = Integer.parseInt(parts[parts.length - 3]);
                            stay = Integer.parseInt(parts[parts.length - 2]);
                            fadeOut = Integer.parseInt(parts[parts.length - 1]);
                            for (int i = 0; i < parts.length - 3; i++) {
                                if (i > 0) titleText.append(" ");
                                titleText.append(parts[i]);
                            }
                        } catch (NumberFormatException e) {
                            for (String part : parts) {
                                if (titleText.length() > 0) titleText.append(" ");
                                titleText.append(part);
                            }
                        }
                    } else {
                        for (String part : parts) {
                            if (titleText.length() > 0) titleText.append(" ");
                            titleText.append(part);
                        }
                    }

                    player.sendTitle(MessageUtil.colorize(titleText.toString()), "", fadeIn, stay, fadeOut);
                }

            } else if (action.startsWith("message:")) {
                String msg = action.substring("message:".length()).trim();
                player.sendMessage(MessageUtil.colorize(msg));

            } else if (action.startsWith("sound:")) {
                String soundStr = action.substring("sound:".length()).length() > 0
                        ? action.substring("sound:".length()).trim() : "";
                String[] soundParts = soundStr.split(" ");
                if (soundParts.length >= 1) {
                    try {
                        Sound sound = Sound.valueOf(soundParts[0].toUpperCase());
                        float volume = soundParts.length >= 2 ? Float.parseFloat(soundParts[1]) : 1.0f;
                        float pitch = soundParts.length >= 3 ? Float.parseFloat(soundParts[2]) : 1.0f;
                        player.playSound(player.getLocation(), sound, volume, pitch);
                    } catch (IllegalArgumentException ignored) {
                    }
                }

            } else if (action.startsWith("open:")) {
                String targetShop = action.substring("open:".length()).trim();
                // 关闭当前商店，打开目标商店
                player.closeInventory();
                openShopMap.remove(player.getName());
                open(player, targetShop);

            } else if (action.equals("close")) {
                player.closeInventory();
                openShopMap.remove(player.getName());

            } else if (action.startsWith("close")) {
                // 兼容 "close" 的各种写法
                player.closeInventory();
                openShopMap.remove(player.getName());
            }
        }
    }

    /**
     * 清除玩家的商店打开记录（用于退出游戏等）
     */
    public static void clearPlayer(Player player) {
        openShopMap.remove(player.getName());
    }

    /**
     * 清除所有缓存（reload 时调用）
     */
    public static void clearCache() {
        shopCache.clear();
        openShopMap.clear();
    }

    // ─── Getters ───

    public String getName() { return name; }
    public String getTitle() { return title; }
    public int getRows() { return rows; }
    public Map<String, ShopItem> getItems() { return items; }
}