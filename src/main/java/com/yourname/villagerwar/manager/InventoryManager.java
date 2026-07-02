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

public class InventoryManager {

    private final VillagerWar plugin;
    private final Map<String, List<PresetItem>> presets = new HashMap<>();
    private final Map<UUID, SavedInventory> snapshots = new ConcurrentHashMap<>();

    public InventoryManager(VillagerWar plugin) {
        this.plugin = plugin;
        loadPresets();
    }

    @SuppressWarnings("unchecked")
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
            List<?> rawList = presetsSection.getList(key);
            if (rawList == null || rawList.isEmpty()) continue;

            List<PresetItem> items = new ArrayList<>();
            for (Object obj : rawList) {
                if (obj instanceof Map) {
                    items.add(new PresetItem((Map<String, Object>) obj));
                }
            }
            presets.put(key.toLowerCase(), items);
        }
    }

    public void save(Player player) {
        PlayerInventory inv = player.getInventory();
        snapshots.put(player.getUniqueId(), new SavedInventory(
            inv.getContents(),
            inv.getArmorContents(),
            inv.getExtraContents()
        ));
    }

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

    public void clear(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setArmorContents(new ItemStack[4]);
        inv.setExtraContents(new ItemStack[1]);
    }

    public void restore(Player player) {
        UUID uuid = player.getUniqueId();
        SavedInventory saved = snapshots.remove(uuid);
        if (saved == null) return;

        PlayerInventory inv = player.getInventory();
        inv.setContents(saved.contents);
        inv.setArmorContents(saved.armor);
        inv.setExtraContents(saved.extra);
    }

    public boolean hasSnapshot(Player player) {
        return snapshots.containsKey(player.getUniqueId());
    }

    public Set<String> getPresetNames() {
        return presets.keySet();
    }

    private static class PresetItem {
        private final Material material;
        private final int slot;
        private final int amount;
        private final String displayName;
        private final List<String> lore;

        PresetItem(Map<String, Object> map) {
            String matName = (String) map.getOrDefault("material", "STONE");
            this.material = Material.getMaterial(matName.toUpperCase());
            this.slot = toInt(map.getOrDefault("slot", -1));
            this.amount = toInt(map.getOrDefault("amount", 1));
            this.displayName = (String) map.getOrDefault("display_name", "");
            Object loreObj = map.get("lore");
            if (loreObj instanceof List) {
                this.lore = new ArrayList<>();
                for (Object line : (List<?>) loreObj) {
                    this.lore.add(line.toString());
                }
            } else {
                this.lore = new ArrayList<>();
            }
        }

        private int toInt(Object value) {
            if (value instanceof Number) return ((Number) value).intValue();
            try { return Integer.parseInt(value.toString()); } catch (Exception e) { return 0; }
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