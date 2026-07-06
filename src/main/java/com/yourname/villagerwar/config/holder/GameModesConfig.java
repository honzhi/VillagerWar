package com.yourname.villagerwar.config.holder;

import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class GameModesConfig {

    private final Map<String, RulePreset> presets;

    public GameModesConfig(ConfigurationSection section) {
        this.presets = new LinkedHashMap<>();
        if (section == null) return;

        // 获取 presets 子节点
        ConfigurationSection presetsSection = section.getConfigurationSection("presets");
        if (presetsSection == null) return;

        for (String key : presetsSection.getKeys(false)) {
            ConfigurationSection presetSection = presetsSection.getConfigurationSection(key);
            if (presetSection == null) continue;
            this.presets.put(key, new RulePreset(key, presetSection));
        }
    }

    public RulePreset getPreset(String name) {
        return presets.get(name);
    }

    public Map<String, RulePreset> getPresets() {
        return Collections.unmodifiableMap(presets);
    }

    public static class RulePreset {
        private final String name;
        private final String displayName;
        private int maxPlayers = 40;
        private int minPlayers = 2;
        private int gameTime = 600;
        private int prepareTime = 30;
        private int skillSelectTime = 30;
        private int skillShowTime = 5;
        private int respawnTime = 3;
        private int invincibleTime = 3;
        private int startGold = 20;
        private int goldPerInterval = 5;
        private int goldInterval = 20;
        private int killReward = 10;
        private int assistReward = 5;
        private int maxTeamSize = 20;
        private boolean friendlyFire = false;
        private boolean allowSpectate = true;
        private boolean autoBalance = true;
        private int waveInterval = 200;
        private String winCondition = "last_team_standing";

        public RulePreset(String name, ConfigurationSection section) {
            this.name = name;
            this.displayName = section.getString("display_name", name);
            this.minPlayers = section.getInt("min_players", minPlayers);

            // 游戏参数
            ConfigurationSection gameSection = section.getConfigurationSection("game");
            if (gameSection != null) {
                this.gameTime = gameSection.getInt("max_time", gameTime);
                this.respawnTime = gameSection.getInt("respawn_time", respawnTime);
                this.invincibleTime = gameSection.getInt("invincible_time", invincibleTime);
            }

            // 经济参数
            ConfigurationSection economySection = section.getConfigurationSection("economy");
            if (economySection != null) {
                this.goldInterval = economySection.getInt("gold_interval", goldInterval);
                this.goldPerInterval = economySection.getInt("gold_per_interval", goldPerInterval);
                this.killReward = economySection.getInt("kill_reward", killReward);
                this.assistReward = economySection.getInt("assist_reward", assistReward);
                this.startGold = economySection.getInt("start_gold", startGold);
            }

            // 出兵间隔
            ConfigurationSection spawnSection = section.getConfigurationSection("spawn");
            if (spawnSection != null) {
                this.waveInterval = spawnSection.getInt("wave_interval", waveInterval);
            }

            // 胜利条件
            ConfigurationSection victorySection = section.getConfigurationSection("victory");
            if (victorySection != null) {
                this.winCondition = victorySection.getString("mode", winCondition);
            }
        }

        public String getName() { return name; }
        public String getDisplayName() { return displayName; }
        public int getMaxPlayers() { return maxPlayers; }
        public int getMinPlayers() { return minPlayers; }
        public int getGameTime() { return gameTime; }
        public int getPrepareTime() { return prepareTime; }
        public int getSkillSelectTime() { return skillSelectTime; }
        public int getSkillShowTime() { return skillShowTime; }
        public int getRespawnTime() { return respawnTime; }
        public int getInvincibleTime() { return invincibleTime; }
        public int getStartGold() { return startGold; }
        public int getGoldPerInterval() { return goldPerInterval; }
        public int getGoldInterval() { return goldInterval; }
        public int getKillReward() { return killReward; }
        public int getAssistReward() { return assistReward; }
        public int getMaxTeamSize() { return maxTeamSize; }
        public boolean isFriendlyFire() { return friendlyFire; }
        public boolean isAllowSpectate() { return allowSpectate; }
        public boolean isAutoBalance() { return autoBalance; }
        public int getWaveInterval() { return waveInterval; }
        public String getWinCondition() { return winCondition; }
    }
}