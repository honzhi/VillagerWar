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
 * 鍟嗗簵 GUI
 * 璇诲彇 shop/*.yml 閰嶇疆锛屾瀯寤哄彲浜や簰鐨勭墿鍝佸晢搴楃晫闈?
 */
public class ShopGUI {

    private static final Map<String, ShopGUI> shopCache = new HashMap<>();
    private static final Map<String, String> openShopMap = new HashMap<>(); // PlayerName 鈫?ShopName

    private final String name;
    private final String title;
    private final List<String> layout;
    private final Map<String, ShopItem> items;
    private final int rows;

    /**
     * 浠庨厤缃枃浠跺姞杞藉晢搴?
     * @param file shop/*.yml 鏂囦欢
     */
    public ShopGUI() {
        this(new java.io.File(VillagerWar.getInstance().getDataFolder(), "shop/defaults.yml"));
    }
    public ShopGUI(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.name = file.getName().replace(".yml", "");
        this.title = MessageUtil.colorize(config.getString("title", "&6鍟嗗簵"));
        this.layout = config.getStringList("layout");
        this.items = new HashMap<>();

        // 瑙ｆ瀽 rows: layout 鐨勮鏁板喅瀹?
        this.rows = layout.size();

        // 瑙ｆ瀽 base 娈?
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
     * 鎵撳紑鍟嗗簵 GUI锛堥潤鎬佸叆鍙ｏ級
     * @param player   鐜╁
     * @param shopName 鍟嗗簵鍚嶇О锛堜笉甯?.yml锛?
     */
    public static void open(Player player, String shopName) {
        // 浠庣紦瀛樻垨鏂囦欢鍔犺浇鍟嗗簵
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
     * 鏋勫缓鍟嗗簵鐗╁搧鏍?
     */
    private Inventory buildInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, rows * 9, title);

        // 鎸?layout 甯冨眬鏀剧疆鐗╁搧
        for (int row = 0; row < layout.size(); row++) {
            String line = layout.get(row);
            for (int col = 0; col < line.length() && col < 9; col++) {
                char c = line.charAt(col);
                String key = String.valueOf(c).toUpperCase();

                if (key.equals("0")) continue; // 绌轰綅

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
     * 鍒涘缓灞曠ず鐗╁搧
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
                coloredLore.add(MessageUtil.colorize("&7浠锋牸: &6" + shopItem.getValue() + " 閲戝竵"));
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 澶勭悊鐐瑰嚮浜嬩欢锛堢敱鐩戝惉鍣ㄨ皟鐢級
     * @param event 鐗╁搧鏍忕偣鍑讳簨浠?
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

        // 璁＄畻 layout 涓搴旂殑閿?
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

        // 鑾峰彇鐜╁鎵€灞炴父鎴?
        Game game = VillagerWar.getInstance().getGameManager().getGame(player).orElse(null);
        GamePlayer gp = (game != null) ? game.getPlayer(player.getUniqueId()) : null;

        boolean success;
        if (gp != null) {
            success = shopItem.checkConditions(gp, game.getGameTime() / 20);
        } else {
            // 涓嶅湪娓告垙涓椂浠呮鏌?value=0
            success = shopItem.getValue() == 0;
        }

        // 鎵ｉ櫎閲戝竵
        if (success && gp != null && shopItem.getValue() > 0) {
            if (gp.takeGold(shopItem.getValue())) {
                success = true;
            } else {
                success = false;
            }
        }

        // 鎵ц鍔ㄤ綔閾?
        List<String> actions = success ? shopItem.getBuyActions().getSuccess()
                                       : shopItem.getBuyActions().getFailure();
        executeActions(player, actions, shop, shopItem);
    }

    /**
     * 鎵ц鍔ㄤ綔閾?
     * 鏀寔鐨勫姩浣滅被鍨? command, title, message, sound, close, open
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

                    // 灏濊瘯瑙ｆ瀽鏃堕棿鍜屾爣棰?
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
                // 鍏抽棴褰撳墠鍟嗗簵锛屾墦寮€鐩爣鍟嗗簵
                player.closeInventory();
                openShopMap.remove(player.getName());
                open(player, targetShop);

            } else if (action.equals("close")) {
                player.closeInventory();
                openShopMap.remove(player.getName());

            } else if (action.startsWith("close")) {
                // 鍏煎 "close" 鐨勫悇绉嶅啓娉?
                player.closeInventory();
                openShopMap.remove(player.getName());
            }
        }
    }

    /**
     * 娓呴櫎鐜╁鐨勫晢搴楁墦寮€璁板綍锛堢敤浜庨€€鍑烘父鎴忕瓑锛?
     */
    public static void clearPlayer(Player player) {
        openShopMap.remove(player.getName());
    }

    /**
     * 娓呴櫎鎵€鏈夌紦瀛橈紙reload 鏃惰皟鐢級
     */
    public static void clearCache() {
        shopCache.clear();
        openShopMap.clear();
    }

    /**
     * 检查玩家是否打开了商店
     */
    public static boolean isOpen(Player player) {
        return openShopMap.containsKey(player.getName());
    }

    /**
     * 获取玩家当前打开的商店实例
     */
    public static ShopGUI getOpenShop(Player player) {
        String shopName = openShopMap.get(player.getName());
        if (shopName == null) return null;
        return shopCache.get(shopName);
    }

    /**
     * 关闭玩家的商店界面
     */
    public static void close(String playerName) {
        openShopMap.remove(playerName);
    }

    // 鈹€鈹€鈹€ Getters 鈹€鈹€鈹€

    public String getName() { return name; }
    public String getTitle() { return title; }
    public int getRows() { return rows; }
    public Map<String, ShopItem> getItems() { return items; }
}