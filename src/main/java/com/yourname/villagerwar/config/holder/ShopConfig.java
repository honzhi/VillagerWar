package com.yourname.villagerwar.config.holder;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class ShopConfig {

    private final String name;
    private final String title;
    private final int rows;
    private final List<ShopCategory> categories;

    public ShopConfig(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.name = file.getName().replace(".yml", "");
        this.title = config.getString("title", "Shop");
        this.rows = config.getInt("rows", 6);
        this.categories = new ArrayList<>();

        ConfigurationSection categoriesSection = config.getConfigurationSection("categories");
        if (categoriesSection == null) return;
        for (String key : categoriesSection.getKeys(false)) {
            ConfigurationSection catSection = categoriesSection.getConfigurationSection(key);
            if (catSection == null) continue;
            this.categories.add(new ShopCategory(key, catSection));
        }
    }

    public String getName() { return name; }
    public String getTitle() { return title; }
    public int getRows() { return rows; }
    public List<ShopCategory> getCategories() { return Collections.unmodifiableList(categories); }

    public static class ShopCategory {
        private final String id;
        private final String displayName;
        private final Material icon;
        private final int slot;
        private final List<ShopItem> items;

        public ShopCategory(String id, ConfigurationSection section) {
            this.id = id;
            this.displayName = section.getString("display-name", id);
            String matName = section.getString("icon", "CHEST");
            Material mat;
            try {
                mat = Material.valueOf(matName.toUpperCase());
            } catch (IllegalArgumentException e) {
                mat = Material.CHEST;
            }
            this.icon = mat;
            this.slot = section.getInt("slot", -1);
            this.items = new ArrayList<>();

            ConfigurationSection itemsSection = section.getConfigurationSection("items");
            if (itemsSection == null) return;
            for (String itemKey : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
                if (itemSection == null) continue;
                this.items.add(new ShopItem(itemKey, itemSection));
            }
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public Material getIcon() { return icon; }
        public int getSlot() { return slot; }
        public List<ShopItem> getItems() { return Collections.unmodifiableList(items); }
    }

    public static class ShopItem {
        private final String id;
        private final Material material;
        private final int amount;
        private final String displayName;
        private final List<String> lore;
        private final int slot;
        private final int buyPrice;
        private final Material buyCurrency;
        private final List<String> commands;

        public ShopItem(String id, ConfigurationSection section) {
            this.id = id;
            String matName = section.getString("material", "STONE");
            Material mat;
            try {
                mat = Material.valueOf(matName.toUpperCase());
            } catch (IllegalArgumentException e) {
                mat = Material.STONE;
            }
            this.material = mat;
            this.amount = section.getInt("amount", 1);
            this.displayName = section.getString("display-name", null);
            this.lore = section.getStringList("lore");
            this.slot = section.getInt("slot", -1);
            this.buyPrice = section.getInt("buy-price", 0);
            String currencyName = section.getString("buy-currency", "GOLD_INGOT");
            Material currency;
            try {
                currency = Material.valueOf(currencyName.toUpperCase());
            } catch (IllegalArgumentException e) {
                currency = Material.GOLD_INGOT;
            }
            this.buyCurrency = currency;
            this.commands = section.getStringList("commands");
        }

        public String getId() { return id; }
        public Material getMaterial() { return material; }
        public int getAmount() { return amount; }
        public String getDisplayName() { return displayName; }
        public List<String> getLore() { return lore; }
        public int getSlot() { return slot; }
        public int getBuyPrice() { return buyPrice; }
        public Material getBuyCurrency() { return buyCurrency; }
        public List<String> getCommands() { return commands; }
    }
}
