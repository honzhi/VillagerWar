package com.yourname.villagerwar.config.holder;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

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
        initBounds();
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

        var teamsList = config.getList("teams");
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
     * 获取商店模板名称
     */
    public String getShops() {
        return config.getString("shops", "defaults");
    }

    private Double boundsMinX;
    private Double boundsMaxX;
    private Double boundsMinZ;
    private Double boundsMaxZ;
    private boolean boundsEnabled;

    /**
     * 初始化边界：从 points1/points2 两个角点计算矩形边界
     */
    private void initBounds() {
        ConfigurationSection boundsSection = config.getConfigurationSection("bounds");
        if (boundsSection != null) {
            ConfigurationSection p1 = boundsSection.getConfigurationSection("points1");
            ConfigurationSection p2 = boundsSection.getConfigurationSection("points2");
            if (p1 != null && p2 != null) {
                double x1 = p1.getDouble("x", 0);
                double z1 = p1.getDouble("z", 0);
                double x2 = p2.getDouble("x", 0);
                double z2 = p2.getDouble("z", 0);
                this.boundsMinX = Math.min(x1, x2);
                this.boundsMaxX = Math.max(x1, x2);
                this.boundsMinZ = Math.min(z1, z2);
                this.boundsMaxZ = Math.max(z1, z2);
                this.boundsEnabled = true;
                return;
            }
        }
        this.boundsMinX = null;
        this.boundsMaxX = null;
        this.boundsMinZ = null;
        this.boundsMaxZ = null;
        this.boundsEnabled = false;
    }

    /**
     * 检查坐标是否在地图边界内
     * @return true 在边界内或无边界配置，false 超出边界
     */
    public boolean isWithinBounds(double x, double z) {
        if (!boundsEnabled) return true;
        return x >= boundsMinX && x <= boundsMaxX && z >= boundsMinZ && z <= boundsMaxZ;
    }

    /**
     * 检查坐标是否在地图边界内（Location 版本）
     */
    public boolean isWithinBounds(org.bukkit.Location loc) {
        return isWithinBounds(loc.getX(), loc.getZ());
    }

    /**
     * 边界是否已启用（配置了两个角点）
     */
    public boolean isBoundsEnabled() { return boundsEnabled; }

    /**
     * 获取边界最小 X
     */
    public Double getBoundsMinX() { return boundsMinX; }

    /**
     * 获取边界最大 X
     */
    public Double getBoundsMaxX() { return boundsMaxX; }

    /**
     * 获取边界最小 Z
     */
    public Double getBoundsMinZ() { return boundsMinZ; }

    /**
     * 获取边界最大 Z
     */
    public Double getBoundsMaxZ() { return boundsMaxZ; }

    public String getDisplayName() { return displayName; }
    public File getFile() { return file; }
    public YamlConfiguration getConfig() { return config; }
}
