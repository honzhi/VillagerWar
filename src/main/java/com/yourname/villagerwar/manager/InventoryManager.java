package com.yourname.villagerwar.manager;

import com.yourname.villagerwar.VillagerWar;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 背包管理器
 * 负责：保存快照、应用预设、清空、归还
 */
public class InventoryManager {

    private final VillagerWar plugin;
    private final Map<String, List<PresetItem>> presets = new HashMap<>();
    // 玩家背包快照
    private final Map<UUID, SavedInventory> snapshots = new ConcurrentHashMap<>();

    public InventoryManager(VillagerWar plugin) {
        this.plugin = plugin;
        loadPresets();
    }

    // ========== 配置加载 ==========

    public void loadPresets() {
        presets.clear();
        File file = new File(plugin.getDataFolder(), "inventory_presets.yml");
        if (!file.exists()) {
            plugin.saveResource("inventory_presets.yml", false);
            file = new File(plugin.getDataFolder(), "inventory_presets.yml");
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection presetsSection = config.getConfigurationSection("presets");
        if (presetsSection == null) return;

        for (String key : presetsSection.getKeys(false)) {
            List<PresetItem> items = new ArrayList<>();
            for (String itemKey : presetsSection.getConfigurationSection(key).getKeys(false)) {
                ConfigurationSection itemSection = presetsSection.getConfigurationSection(key + "." + itemKey);
                if (itemSection == null) continue;
                items.add(new PresetItem(itemSection));
            }
            presets.put(key.toLowerCase(), items);
        }
    }

    // ========== 核心方法 ==========

    /**
     * 保存玩家背包快照
     */
    public void save(Player player) {
        PlayerInventory inv = player.getInventory();
        snapshots.put(player.getUniqueId(), new SavedInventory(
            inv.getContents(),
            inv.getArmorContents(),
            inv.getExtraContents()
        ));
    }

    /**
     * 应用指定预设（清空 + 给物品）
     */
    public void apply(Player player, String presetName) {
        clear(player);
        List<PresetItem> items = presets.get(presetName.toLowerCase());
        if (items == null) return;

        PlayerInventory inv = player.getInventory();
        for (PresetItem preset : items) {
            ItemStack item = preset.build();
            if (item == null) continue;
            if (preset.slot >= 0 && preset.slot < 40) {
                inv.setItem(preset.slot, item);
            } else {
                inv.addItem(item);
            }
        }
    }

    /**
     * 清空玩家背包（含盔甲、副手）
     */
    public void clear(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setArmorContents(new ItemStack[4]);
        inv.setExtraContents(new ItemStack[1]);
    }

    /**
     * 归还背包快照
     */
    public void restore(Player player) {
        UUID uuid = player.getUniqueId();
        SavedInventory saved = snapshots.remove(uuid);
        if (saved == null) return;

        PlayerInventory inv = player.getInventory();
        inv.setContents(saved.contents);
        inv.setArmorContents(saved.armor);
        inv.setExtraContents(saved.extra);
    }

    /**
     * 检查玩家是否有快照
     */
    public boolean hasSnapshot(Player player) {
        return snapshots.containsKey(player.getUniqueId());
    }

    /**
     * 获取当前预设列表（用于调试）
     */
    public Set<String> getPresetNames() {
        return presets.keySet();
    }

    // ========== 内部类 ==========

    private static class PresetItem {
        private final Material material;
        private final int slot;
        private final int amount;
        private final String displayName;
        private final List<String> lore;

        PresetItem(ConfigurationSection section) {
            String matName = section.getString("material", "STONE");
            this.material = Material.getMaterial(matName.toUpperCase());
            this.slot = section.getInt("slot", -1);
            this.amount = section.getInt("amount", 1);
            this.displayName = section.getString("display_name", "");
            this.lore = section.getStringList("lore");
        }

        ItemStack build() {
            if (material == null) return null;
            ItemStack item = new ItemStack(material, amount);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return item;
            if (!displayName.isEmpty()) {
                meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', displayName));
            }
            if (!lore.isEmpty()) {
                List<String> colored = new ArrayList<>();
                for (String line : lore) {
                    colored.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(colored);
            }
            item.setItemMeta(meta);
            return item;
        }
    }

    private static class SavedInventory {
        private final ItemStack[] contents;
        private final ItemStack[] armor;
        private final ItemStack[] extra;

        SavedInventory(ItemStack[] contents, ItemStack[] armor, ItemStack[] extra) {
            this.contents = contents;
            this.armor = armor;
            this.extra = extra;
        }
    }
}