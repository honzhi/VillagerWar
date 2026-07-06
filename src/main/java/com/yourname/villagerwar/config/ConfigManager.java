package com.yourname.villagerwar.config;

import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.config.holder.*;
import com.yourname.villagerwar.config.rule.GameRule;
import com.yourname.villagerwar.config.rule.GameRuleLoader;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

public class ConfigManager {

    private final VillagerWar plugin;
    private final File dataFolder;

    private PluginConfig pluginConfig;
    private SkillsConfig skillsConfig;
    private MessagesConfig messagesConfig;
    private GameModesConfig gameModesConfig;
    private GameConfig gameConfig;
    private final Map<String, StatusConfig> statusConfigs = new LinkedHashMap<>();
    
    private final Map<String, ShopConfig> shopConfigs = new LinkedHashMap<>();
    private final Map<String, MapConfig> mapConfigs = new LinkedHashMap<>();

    public ConfigManager(VillagerWar plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
    }

    public void loadAll() {
        saveDefaultFiles();
        loadPluginConfig();
        loadSkillsConfig();
        loadMessagesConfig();
        loadGameModesConfig();
        loadGameConfig();
        loadShopConfigs();
        loadMapConfigs();
        loadStatusConfigs();
        plugin.getLogger().info("All configs loaded (" + shopConfigs.size() + " shops, "
            + mapConfigs.size() + " maps)");
    }

    public void reloadAll() {
        // 清空所有缓存配置
        shopConfigs.clear();
        mapConfigs.clear();
        statusConfigs.clear();
        pluginConfig = null;
        skillsConfig = null;
        messagesConfig = null;
        gameModesConfig = null;
        gameConfig = null;
        // 重新从磁盘加载
        plugin.reloadConfig();
        loadAll();
    }

    private void saveDefaultFiles() {
        saveResource("config.yml");
        saveResource("skills.yml");
        saveResource("messages.yml");
        saveResource("game_modes.yml");
        saveResource("game_config.yml");
        saveResource("shop/defaults.yml");
        saveResource("shop/weapon_shop.yml");
        saveResource("shop/armor_shop.yml");
        saveResource("game_status/preparing.yml");
        saveResource("game_status/skills_select.yml");
        saveResource("game_status/skill_show.yml");
        saveResource("game_status/playing.yml");
        saveResource("game_status/ending.yml");
        saveResource("game_status/reward.yml");
        saveResource("game_gui/map_select.yml");
        saveResource("game_gui/mode_select.yml");
        saveResource("game_gui/skill_select.yml");
        saveResource("map_defaults.yml");
        saveResource("inventory_presets.yml");
    }

    private void saveResource(String path) {
        File file = new File(dataFolder, path);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            plugin.saveResource(path, false);
        }
    }

    private void loadPluginConfig() {
        FileConfiguration config = plugin.getConfig();
        this.pluginConfig = new PluginConfig(config);
        plugin.getLogger().info("[Config] debug: " + pluginConfig.isDebug());
        plugin.getLogger().info("[Config] raw debug: " + config.getBoolean("debug", false));
        plugin.getLogger().info("[Config] keys: " + config.getKeys(false));
    }

    private void loadSkillsConfig() {
        File file = new File(dataFolder, "skills.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.skillsConfig = new SkillsConfig(config);
    }

    private void loadMessagesConfig() {
        File file = new File(dataFolder, "messages.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.messagesConfig = new MessagesConfig(config);
    }

    private void loadGameModesConfig() {
        File file = new File(dataFolder, "game_modes.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.gameModesConfig = new GameModesConfig(config);
    }

    private void loadGameConfig() {
        File file = new File(dataFolder, "game_config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.gameConfig = new GameConfig(config);
    }

    private void loadShopConfigs() {
        File shopDir = new File(dataFolder, "shop");
        if (!shopDir.isDirectory()) {
            shopDir.mkdirs();
            return;
        }
        File[] files = shopDir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            try {
                ShopConfig sc = new ShopConfig(file);
                shopConfigs.put(sc.getName(), sc);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load shop: " + file.getName(), e);
            }
        }
    }

    private void loadMapConfigs() {
        File mapsDir = new File(dataFolder, "maps");
        if (!mapsDir.isDirectory()) {
            mapsDir.mkdirs();
            return;
        }
        File[] mapDirs = mapsDir.listFiles(File::isDirectory);
        if (mapDirs == null) return;
        for (File mapDir : mapDirs) {
            File mapFile = new File(mapDir, "map.yml");
            if (!mapFile.exists()) continue;
            try {
                MapConfig mc = new MapConfig(mapFile);
                mapConfigs.put(mc.getId(), mc);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load map: " + mapFile.getPath(), e);
            }
        }
    }

    public void refreshMapConfigs() {
        mapConfigs.clear();
        File mapsDir = new File(dataFolder, "maps");
        if (!mapsDir.isDirectory()) {
            mapsDir.mkdirs();
            return;
        }
        File[] mapDirs = mapsDir.listFiles(File::isDirectory);
        if (mapDirs == null) return;
        for (File mapDir : mapDirs) {
            File mapFile = new File(mapDir, "map.yml");
            if (!mapFile.exists()) continue;
            try {
                MapConfig mc = new MapConfig(mapFile);
                mapConfigs.put(mc.getId(), mc);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load map: " + mapFile.getPath(), e);
            }
        }
    }

    public GameRule createGameRule(String modeName) {
        GameModesConfig.RulePreset preset = gameModesConfig.getPreset(modeName);
        if (preset == null) {
            plugin.getLogger().warning("Game mode not found: " + modeName + ", using defaults");
            return createDefaultGameRule(modeName);
        }
        return GameRuleLoader.load(preset);
    }

    private GameRule createDefaultGameRule(String name) {
        return new GameRule(name, name,
            40, 2, 600, 30, 20, 10,
            5, 3,
            0, 5, 10,
            10, 3,
            20,
            false, true, true,
            200,
            "KILL_ALL");
    }

    public PluginConfig getPluginConfig() { return pluginConfig; }
    public SkillsConfig getSkillsConfig() { return skillsConfig; }
    public MessagesConfig getMessagesConfig() { return messagesConfig; }
    public GameModesConfig getGameModesConfig() { return gameModesConfig; }
    public GameConfig getGameConfig() { return gameConfig; }

    public ShopConfig getShopConfig(String name) { return shopConfigs.get(name); }
    public Map<String, ShopConfig> getShopConfigs() { return Collections.unmodifiableMap(shopConfigs); }

    public MapConfig getMapConfig(String id) { return mapConfigs.get(id); }
    public Map<String, MapConfig> getMapConfigs() { return Collections.unmodifiableMap(mapConfigs); }

    private void loadStatusConfigs() {
        File statusDir = new File(dataFolder, "game_status");
        if (!statusDir.isDirectory()) {
            statusDir.mkdirs();
            return;
        }
        File[] files = statusDir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            try {
                String stateName = file.getName().replace(".yml", "");
                StatusConfig sc = new StatusConfig(file);
                statusConfigs.put(stateName, sc);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load status config: " + file.getName(), e);
            }
        }
        plugin.getLogger().info("Loaded " + statusConfigs.size() + " status configs");
    }

    public StatusConfig getStatusConfig(String stateName) { return statusConfigs.get(stateName); }
    public Map<String, StatusConfig> getStatusConfigs() { return Collections.unmodifiableMap(statusConfigs); }
}