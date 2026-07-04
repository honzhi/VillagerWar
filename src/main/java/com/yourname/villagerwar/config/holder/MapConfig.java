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



    public String getDisplayName() { return displayName; }
    public File getFile() { return file; }
    public YamlConfiguration getConfig() { return config; }
}
