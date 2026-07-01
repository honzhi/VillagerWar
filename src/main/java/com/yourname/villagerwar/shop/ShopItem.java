package com.yourname.villagerwar.shop;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商店物品数据类
 * 映射 shop/*.yml 中 base 下每个物品的配置
 */
public class ShopItem {

    private final String slotKey;
    private final Material material;
    private final int amount;
    private final String displayName;
    private final List<String> lore;
    private final int value;
    private final Map<String, Object> conditions;
    private final ShopActions buyActions;

    public ShopItem(String slotKey, ConfigurationSection section) {
        this.slotKey = slotKey;
        this.amount = 1;

        // Material
        String matName = section.getString("material", "STONE");
        Material mat;
        try {
            mat = Material.valueOf(matName.toUpperCase());
        } catch (IllegalArgumentException e) {
            mat = Material.STONE;
        }
        this.material = mat;

        // Display name / Lore
        this.displayName = section.getString("display_name", "");
        this.lore = section.getStringList("lore");

        // Value (price)
        this.value = section.getInt("value", 0);

        // Conditions
        ConfigurationSection condSection = section.getConfigurationSection("conditions");
        this.conditions = new HashMap<>();
        if (condSection != null) {
            for (String key : condSection.getKeys(false)) {
                this.conditions.put(key, condSection.get(key));
            }
        }

        // Buy actions
        ConfigurationSection buySection = section.getConfigurationSection("buy");
        if (buySection != null) {
            List<String> success = buySection.getStringList("success");
            List<String> failure = buySection.getStringList("failure");
            this.buyActions = new ShopActions(
                    success != null ? success : new ArrayList<>(),
                    failure != null ? failure : new ArrayList<>()
            );
        } else {
            this.buyActions = new ShopActions(new ArrayList<>(), new ArrayList<>());
        }
    }

    /**
     * 检查玩家是否满足所有购买条件
     * @param player 游戏玩家（需提供 hasMoney, gameTime, level 等）
     * @return true 如果所有条件都满足
     */
    public boolean checkConditions(com.yourname.villagerwar.GamePlayer player, int gameTime) {
        // has_money: 检查玩家金币是否 >= 需求
        if (conditions.containsKey("has_money")) {
            int required = ((Number) conditions.get("has_money")).intValue();
            if (player.getGold() < required) return false;
        }
        // game_time: 检查游戏时间是否 >= 需求（单位: 秒）
        if (conditions.containsKey("game_time")) {
            int required = ((Number) conditions.get("game_time")).intValue();
            if (gameTime < required) return false;
        }
        return true;
    }

    /**
     * 获取条件值
     * @param key 条件键名
     * @param def 默认值
     * @return 条件值（需自行转换类型）
     */
    public Object getCondition(String key, Object def) {
        return conditions.getOrDefault(key, def);
    }

    // ─── Getters ───

    public String getSlotKey() { return slotKey; }
    public Material getMaterial() { return material; }
    public int getAmount() { return amount; }
    public String getDisplayName() { return displayName; }
    public List<String> getLore() { return Collections.unmodifiableList(lore); }
    public int getValue() { return value; }
    public Map<String, Object> getConditions() { return Collections.unmodifiableMap(conditions); }
    public ShopActions getBuyActions() { return buyActions; }

    /**
     * 购买动作链
     * success: 条件满足时执行的动作列表
     * failure: 条件不满足时执行的动作列表
     */
    public static class ShopActions {
        private final List<String> success;
        private final List<String> failure;

        public ShopActions(List<String> success, List<String> failure) {
            this.success = success != null ? success : new ArrayList<>();
            this.failure = failure != null ? failure : new ArrayList<>();
        }

        public List<String> getSuccess() { return Collections.unmodifiableList(success); }
        public List<String> getFailure() { return Collections.unmodifiableList(failure); }

        public boolean hasSuccess() { return !success.isEmpty(); }
        public boolean hasFailure() { return !failure.isEmpty(); }
    }
}