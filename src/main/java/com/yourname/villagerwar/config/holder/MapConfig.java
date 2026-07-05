package com.yourname.villagerwar.config.holder;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 单张地图的配置映射
 * 对应 maps/<map_id>/map.yml
 */
public class MapConfig {

    private final String id;
    private final File file;
    private final YamlConfiguration config;
    private final String displayName;

    public MapConfig(File file) {
        this.file = file;
        this.config = YamlConfiguration.loadConfiguration(file);
        this.id = file.getParentFile().getName();
        this.displayName = config.getString("display_name", id);
    }

    /**
     * 获取地图唯一标识（对应文件夹名）
     */
    public String getId() {
        return id;
    }

    /**
     * 获取指定队伍的配置段
     * @param teamId red / blue
     * @return 队伍配置段，若不存在返回 null
     */
    public ConfigurationSection getTeamSection(String teamId) {
        if (config == null) return null;

        // 方式1: 从 ConfigurationSection 方式获取（当 YAML 列表被解析为 Section 时）
        ConfigurationSection teamsSection = config.getConfigurationSection("teams");
        if (teamsSection != null) {
            for (String key : teamsSection.getKeys(false)) {
                ConfigurationSection team = teamsSection.getConfigurationSection(key);
                if (team != null && teamId.equals(team.getString("id"))) {
                    return team;
                }
            }
        }

        // 方式2: 从 MapList 方式获取（Bukkit getList 返回 Map 对象）
        List<?> teamsList = config.getList("teams");
        if (teamsList == null) return null;

        for (Object obj : teamsList) {
            if (obj instanceof ConfigurationSection section) {
                if (teamId.equals(section.getString("id"))) {
                    return section;
                }
            }
        }
        return null;
    }

    /**
     * 直接从 MapList 中获取队伍 spawn 坐标（绕过 ConfigurationSection 限制）
     * @param teamId red / blue
     * @return {x, y, z} 或 null
     */
    public Map<String, Object> getTeamSpawnPoint(String teamId) {
        List<?> teamsList = config.getList("teams");
        if (teamsList == null) return null;

        for (Object obj : teamsList) {
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> teamMap = (Map<String, Object>) obj;
                String id = teamMap.get("id") instanceof String ? (String) teamMap.get("id") : null;
                if (teamId.equals(id)) {
                    // 获取 player.spawn.points
                    Object playerObj = teamMap.get("player");
                    if (playerObj instanceof Map) {
                        Map<String, Object> playerMap = (Map<String, Object>) playerObj;
                        Object spawnObj = playerMap.get("spawn");
                        if (spawnObj instanceof Map) {
                            Map<String, Object> spawnMap = (Map<String, Object>) spawnObj;
                            // 获取yaw/pitch
                            Number yawNum = spawnMap.get("yaw") instanceof Number ? (Number) spawnMap.get("yaw") : null;
                            Number pitchNum = spawnMap.get("pitch") instanceof Number ? (Number) spawnMap.get("pitch") : null;
                            // 获取points
                            Object pointsObj = spawnMap.get("points");
                            if (pointsObj instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> points = (Map<String, Object>) pointsObj;
                                points.put("_yaw", yawNum != null ? yawNum.floatValue() : 0f);
                                points.put("_pitch", pitchNum != null ? pitchNum.floatValue() : 0f);
                                return points;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 获取商店模板名称
     */
    public String getShops() {
        return config.getString("shops", "defaults");
    }

    public String getDisplayName() { return displayName; }
    public File getFile() { return file; }
    public YamlConfiguration getConfig() { return config; }
}
