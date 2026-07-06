package com.yourname.villagerwar.config.holder;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import org.bukkit.Location;
import org.bukkit.World;

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

    /**
     * 获取指定队伍的基地配置
     * @return {entity: {type, id}, spawn: {point, yaw, pitch}} 或 null
     */
    public Map<String, Object> getTeamBaseConfig(String teamId) {
        List<?> teamsList = config.getList("teams");
        if (teamsList == null) return null;
        for (Object obj : teamsList) {
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> teamMap = (Map<String, Object>) obj;
                if (teamId.equals(teamMap.get("id"))) {
                    Object baseObj = teamMap.get("base");
                    if (baseObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> baseMap = (Map<String, Object>) baseObj;
                        return baseMap;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 获取指定队伍的小兵配置
     * @return {entity: {type}, loop: [...], spawn: {point, yaw, pitch}, path: [...]} 或 null
     */
    public Map<String, Object> getTeamSoldierConfig(String teamId) {
        List<?> teamsList = config.getList("teams");
        if (teamsList == null) return null;
        for (Object obj : teamsList) {
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> teamMap = (Map<String, Object>) obj;
                if (teamId.equals(teamMap.get("id"))) {
                    Object soldierObj = teamMap.get("soldier");
                    if (soldierObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> soldierMap = (Map<String, Object>) soldierObj;
                        return soldierMap;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 获取指定队伍的小兵路径点列表
     * @return List of Location (不含世界)
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getTeamSoldierPath(String teamId) {
        Map<String, Object> soldierConfig = getTeamSoldierConfig(teamId);
        if (soldierConfig == null) return new ArrayList<>();
        Object pathObj = soldierConfig.get("path");
        if (pathObj instanceof List) {
            return (List<Map<String, Object>>) pathObj;
        }
        return new ArrayList<>();
    }

    /**
     * 将路径点 Map 转换为 Location（使用指定的世界）
     */
    public static Location pathPointToLocation(Map<String, Object> point, World world) {
        if (point == null) return null;
        Number x = point.get("x") instanceof Number ? (Number) point.get("x") : null;
        Number y = point.get("y") instanceof Number ? (Number) point.get("y") : null;
        Number z = point.get("z") instanceof Number ? (Number) point.get("z") : null;
        if (x == null || y == null || z == null) return null;
        return new Location(world, x.doubleValue(), y.doubleValue(), z.doubleValue());
    }

    /**
     * 从 spawn 配置 Map 中提取 Location
     */
    public static Location extractLocation(Map<String, Object> spawnMap, World world) {
        if (spawnMap == null) return null;
        Object pointsObj = spawnMap.containsKey("points") ? spawnMap.get("points") : spawnMap.get("point");
        if (pointsObj == null) {
            // 直接 x,y,z 在 spawnMap 层级
            Number x = spawnMap.get("x") instanceof Number ? (Number) spawnMap.get("x") : null;
            Number y = spawnMap.get("y") instanceof Number ? (Number) spawnMap.get("y") : null;
            Number z = spawnMap.get("z") instanceof Number ? (Number) spawnMap.get("z") : null;
            if (x != null && y != null && z != null) {
                return new Location(world, x.doubleValue(), y.doubleValue(), z.doubleValue());
            }
            return null;
        }
        if (pointsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> pt = (Map<String, Object>) pointsObj;
            Number x = pt.get("x") instanceof Number ? (Number) pt.get("x") : null;
            Number y = pt.get("y") instanceof Number ? (Number) pt.get("y") : null;
            Number z = pt.get("z") instanceof Number ? (Number) pt.get("z") : null;
            if (x != null && y != null && z != null) {
                return new Location(world, x.doubleValue(), y.doubleValue(), z.doubleValue());
            }
        }
        return null;
    }

    /**
     * 获取指定队伍的 MM 基地实体 ID
     */
    public String getTeamBaseMobId(String teamId) {
        Map<String, Object> baseConfig = getTeamBaseConfig(teamId);
        if (baseConfig == null) return null;
        Object entityObj = baseConfig.get("entity");
        if (entityObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> entityMap = (Map<String, Object>) entityObj;
            Object idObj = entityMap.get("id");
            return idObj instanceof String ? (String) idObj : null;
        }
        return null;
    }

    /**
     * 获取指定队伍的小兵循环列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getTeamSoldierLoop(String teamId) {
        Map<String, Object> soldierConfig = getTeamSoldierConfig(teamId);
        if (soldierConfig == null) return new ArrayList<>();
        Object loopObj = soldierConfig.get("loop");
        if (loopObj instanceof List) {
            return (List<String>) loopObj;
        }
        return new ArrayList<>();
    }

    /**
     * 获取小兵循环间隔（tick）
     */
    public int getSoldierLoopInterval(String teamId) {
        Map<String, Object> soldierConfig = getTeamSoldierConfig(teamId);
        if (soldierConfig == null) return 60;
        Object val = soldierConfig.get("loop_interval");
        if (val instanceof Number) return ((Number) val).intValue();
        return 60;
    }

    /**
     * 获取小兵第一波延迟（tick）
     */
    public int getSoldierDelay(String teamId) {
        Map<String, Object> soldierConfig = getTeamSoldierConfig(teamId);
        if (soldierConfig == null) return 60;
        Object val = soldierConfig.get("delay");
        if (val instanceof Number) return ((Number) val).intValue();
        return 60;
    }

}