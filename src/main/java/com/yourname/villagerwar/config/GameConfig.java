package com.yourname.villagerwar.config;

import org.bukkit.configuration.ConfigurationSection;

public class GameConfig {

    private final int maxPlayers;
    private final int minPlayers;
    private final int gameTime;
    private final int prepareTime;
    private final int skillSelectTime;
    private final int skillShowTime;
    private final int respawnTime;
    private final int invincibleTime;
    private final int startGold;
    private final int goldPerInterval;
    private final int goldInterval;
    private final int killReward;
    private final int assistReward;
    private final int maxTeamSize;
    private final boolean friendlyFire;
    private final boolean allowSpectate;
    private final boolean autoBalance;
    private final String winCondition;

    public GameConfig(ConfigurationSection section) {
        this.maxPlayers = section.getInt("max-players", 40);
        this.minPlayers = section.getInt("min-players", 2);
        this.gameTime = section.getInt("game-time", 600);
        this.prepareTime = section.getInt("prepare-time", 30);
        this.skillSelectTime = section.getInt("skill-select-time", 20);
        this.skillShowTime = section.getInt("skill-show-time", 10);
        this.respawnTime = section.getInt("respawn-time", 5);
        this.invincibleTime = section.getInt("invincible-time", 3);
        this.startGold = section.getInt("start-gold", 0);
        this.goldPerInterval = section.getInt("gold-per-interval", 5);
        this.goldInterval = section.getInt("gold-interval", 10);
        this.killReward = section.getInt("kill-reward", 10);
        this.assistReward = section.getInt("assist-reward", 3);
        this.maxTeamSize = section.getInt("max-team-size", 20);
        this.friendlyFire = section.getBoolean("friendly-fire", false);
        this.allowSpectate = section.getBoolean("allow-spectate", true);
        this.autoBalance = section.getBoolean("auto-balance", true);
        this.winCondition = section.getString("win-condition", "KILL_ALL");
    }

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
    public String getWinCondition() { return winCondition; }
}
