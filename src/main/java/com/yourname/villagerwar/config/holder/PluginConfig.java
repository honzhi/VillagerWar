package com.yourname.villagerwar.config.holder;

import org.bukkit.configuration.ConfigurationSection;

public class PluginConfig {
    private final boolean debug;

    private final String lobbyWorld;
    private final String defaultGameMode;
    private final String language;
    private final boolean mapEditorAutoSave;
    private final int mapEditorAutoSaveInterval;
    private final DatabaseConfig database;

    public PluginConfig(ConfigurationSection section) {
        this.lobbyWorld = section.getString("lobby-world", "world");
        this.defaultGameMode = section.getString("default-game-mode", "CLASSIC");
        this.language = section.getString("language", "zh_cn");
        // 地图编辑器自动保存
        ConfigurationSection mapEditor = section.getConfigurationSection("map_editor");
        if (mapEditor != null) {
            this.mapEditorAutoSave = mapEditor.getBoolean("auto_save", true);
            this.mapEditorAutoSaveInterval = mapEditor.getInt("auto_save_interval", 300);
        } else {
            this.mapEditorAutoSave = true;
            this.mapEditorAutoSaveInterval = 300;
        }
        this.database = new DatabaseConfig(section.getConfigurationSection("database"));
        this.debug = section.getBoolean("debug", false);
    }

    public boolean isDebug() { return debug; }
    public String getLobbyWorld() { return lobbyWorld; }
    public String getDefaultGameMode() { return defaultGameMode; }
    public String getLanguage() { return language; }
    public boolean isMapEditorAutoSave() { return mapEditorAutoSave; }
    public int getMapEditorAutoSaveInterval() { return mapEditorAutoSaveInterval; }
    public DatabaseConfig getDatabase() { return database; }

    public static class DatabaseConfig {
        private final String host;
        private final int port;
        private final String database;
        private final String username;
        private final String password;
        private final boolean useSSL;

        public DatabaseConfig(ConfigurationSection section) {
            if (section == null) {
                this.host = "localhost";
                this.port = 3306;
                this.database = "villagerwar";
                this.username = "root";
                this.password = "";
                this.useSSL = false;
                return;
            }
            this.host = section.getString("host", "localhost");
            this.port = section.getInt("port", 3306);
            this.database = section.getString("database", "villagerwar");
            this.username = section.getString("username", "root");
            this.password = section.getString("password", "");
            this.useSSL = section.getBoolean("use-ssl", false);
        }

        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getDatabase() { return database; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public boolean isUseSSL() { return useSSL; }
    }
}
