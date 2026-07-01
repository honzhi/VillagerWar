package com.yourname.villagerwar.config.rule;

/**
 * 运行时不可变规则对象，由 GameRuleLoader 从 RulePreset 构建。
 * 所有字段均为 final，确保线程安全与运行时一致性。
 */
public class GameRule {

    private final String name;
    private final String displayName;
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

    public GameRule(String name, String displayName,
                    int maxPlayers, int minPlayers,
                    int gameTime, int prepareTime,
                    int skillSelectTime, int skillShowTime,
                    int respawnTime, int invincibleTime,
                    int startGold, int goldPerInterval, int goldInterval,
                    int killReward, int assistReward,
                    int maxTeamSize,
                    boolean friendlyFire, boolean allowSpectate, boolean autoBalance,
                    String winCondition) {
        this.name = name;
        this.displayName = displayName;
        this.maxPlayers = maxPlayers;
        this.minPlayers = minPlayers;
        this.gameTime = gameTime;
        this.prepareTime = prepareTime;
        this.skillSelectTime = skillSelectTime;
        this.skillShowTime = skillShowTime;
        this.respawnTime = respawnTime;
        this.invincibleTime = invincibleTime;
        this.startGold = startGold;
        this.goldPerInterval = goldPerInterval;
        this.goldInterval = goldInterval;
        this.killReward = killReward;
        this.assistReward = assistReward;
        this.maxTeamSize = maxTeamSize;
        this.friendlyFire = friendlyFire;
        this.allowSpectate = allowSpectate;
        this.autoBalance = autoBalance;
        this.winCondition = winCondition;
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
    public String getWinCondition() { return winCondition; }
}
