package com.yourname.villagerwar.config.holder;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 游戏状态配置加载器
 * 读取 game_status/ 目录下的每个状态的 yml 配置
 */
public class StatusConfig {

    private final int duration;
    private final boolean scoreboardEnabled;
    private final String scoreboardTitle;
    private final List<String> scoreboardLines;
    private final boolean titleEnabled;
    private final String titleText;
    private final String subtitleText;
    private final boolean actionBarEnabled;
    private final String actionBarText;

    // Playing-specific
    private final boolean readyEnabled;
    private final String readyTitle;
    private final String readySubtitle;
    private final String readySound;
    private final String readyFinalSound;
    private final boolean friendlyFire;
    private final boolean naturalRegen;
    private final boolean hungerDeplete;
    private final boolean fallDamage;
    private final boolean keepInventory;
    private final boolean keepLevel;

    // Ending-specific
    private final boolean respawnDead;
    private final boolean removePotions;

    // Reward-specific
    private final int rewardParticipation;
    private final int rewardKill;
    private final int rewardAssist;
    private final int rewardWinBonus;
    private final boolean afterClearInventory;
    private final boolean afterTeleportLobby;

    // Preparing-specific
    private final int startDelay;
    private final String waitingTitle;
    private final String waitingSubtitle;
    private final String waitingActionBar;
    private final boolean matchStartSound;
    private final String matchStartSoundStr;
    private final String matchStartTitle;
    private final String matchStartSubtitle;
    private final String matchStartBroadcast;
    private final String countdownSound;
    private final String countdownFinalSound;
    private final String countdownFinalTitle;
    private final String countdownFinalSubtitle;
    private final boolean playerFreeze;

    public StatusConfig(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // 获取该文件的第一个顶级key（状态名）
        String topKey = null;
        for (String key : config.getKeys(false)) {
            topKey = key;
            break;
        }
        ConfigurationSection section = topKey != null ? config.getConfigurationSection(topKey) : config;

        this.duration = section.getInt("duration", 10);

        // 标题
        ConfigurationSection titleHint = section.getConfigurationSection("title_hint");
        if (titleHint != null) {
            this.titleEnabled = true;
            this.titleText = titleHint.getString("title", "");
            this.subtitleText = titleHint.getString("subtitle", "");
        } else {
            this.titleEnabled = false;
            this.titleText = "";
            this.subtitleText = "";
        }

        // 操作栏
        String actionBarStr = section.getString("actionBar", section.getString("actionbar.template", ""));
        this.actionBarEnabled = !actionBarStr.isEmpty();
        this.actionBarText = actionBarStr;

        // 计分板
        ConfigurationSection sb = section.getConfigurationSection("scoreboard");
        if (sb != null) {
            this.scoreboardEnabled = sb.getBoolean("enabled", true);
            this.scoreboardTitle = sb.getString("title", "");
            this.scoreboardLines = sb.getStringList("lines");
        } else {
            this.scoreboardEnabled = false;
            this.scoreboardTitle = "";
            this.scoreboardLines = new ArrayList<>();
        }

        // Playing - Ready 倒计时
        ConfigurationSection ready = section.getConfigurationSection("ready");
        if (ready != null) {
            this.readyEnabled = true;
            this.readyTitle = ready.getString("title", "&e{remain}");
            this.readySubtitle = ready.getString("subtitle", "");
            this.readySound = ready.getString("sound", "");
            this.readyFinalSound = ready.getString("final_sound", "");
        } else {
            this.readyEnabled = false;
            this.readyTitle = "";
            this.readySubtitle = "";
            this.readySound = "";
            this.readyFinalSound = "";
        }

        ConfigurationSection player = section.getConfigurationSection("player");
        if (player != null) {
            this.friendlyFire = player.getBoolean("friendly_fire", false);
            this.naturalRegen = player.getBoolean("natural_regen", true);
            this.hungerDeplete = player.getBoolean("hunger_deplete", false);
            this.fallDamage = player.getBoolean("fall_damage", true);
            this.keepInventory = player.getBoolean("keep_inventory", true);
            this.keepLevel = player.getBoolean("keep_level", true);
        } else {
            this.friendlyFire = false;
            this.naturalRegen = true;
            this.hungerDeplete = false;
            this.fallDamage = true;
            this.keepInventory = true;
            this.keepLevel = true;
        }

        // Ending - 特殊配置
        this.respawnDead = section.getBoolean("respawn_dead", true);
        this.removePotions = section.getBoolean("remove_potions", true);

        // Reward - 奖励配置
        ConfigurationSection rewards = section.getConfigurationSection("rewards");
        if (rewards != null) {
            this.rewardParticipation = rewards.getInt("participation", 10);
            this.rewardKill = rewards.getInt("kill", 10);
            this.rewardAssist = rewards.getInt("assist", 5);
            this.rewardWinBonus = rewards.getInt("win_bonus", 30);
        } else {
            this.rewardParticipation = 10;
            this.rewardKill = 10;
            this.rewardAssist = 5;
            this.rewardWinBonus = 30;
        }

        ConfigurationSection after = section.getConfigurationSection("after");
        if (after != null) {
            this.afterClearInventory = after.getBoolean("clear_inventory", true);
            this.afterTeleportLobby = after.getBoolean("teleport_lobby", true);
        } else {
            this.afterClearInventory = true;
            this.afterTeleportLobby = true;
        }

        // Preparing - 匹配等待
        ConfigurationSection waiting = section.getConfigurationSection("waiting");
        if (waiting != null) {
            this.waitingTitle = waiting.getString("title", "");
            this.waitingSubtitle = waiting.getString("subtitle", "");
            this.waitingActionBar = waiting.getString("action_bar", "");
        } else {
            this.waitingTitle = "";
            this.waitingSubtitle = "";
            this.waitingActionBar = "";
        }

        // Preparing - 匹配成功延迟
        this.startDelay = section.getInt("start_delay", 10);

        // Preparing - 匹配开始效果
        ConfigurationSection matchStart = section.getConfigurationSection("match_start");
        if (matchStart != null) {
            this.matchStartSound = true;
            this.matchStartSoundStr = matchStart.getString("sound", "ENTITY_EXPERIENCE_ORB_PICKUP 1 1");
            this.matchStartTitle = matchStart.getString("title", "");
            this.matchStartSubtitle = matchStart.getString("subtitle", "");
            this.matchStartBroadcast = matchStart.getString("broadcast", "");
        } else {
            this.matchStartSound = false;
            this.matchStartSoundStr = "";
            this.matchStartTitle = "";
            this.matchStartSubtitle = "";
            this.matchStartBroadcast = "";
        }

        // Preparing - 倒计时效果
        ConfigurationSection cd = section.getConfigurationSection("countdown");
        if (cd != null) {
            this.countdownSound = cd.getString("sound", "BLOCK_NOTE_BLOCK_PLING 1 1");
            this.countdownFinalSound = cd.getString("final_sound", "ENTITY_EXPERIENCE_ORB_PICKUP 1 1");
            ConfigurationSection finalTitle = cd.getConfigurationSection("final_title");
            if (finalTitle != null) {
                this.countdownFinalTitle = finalTitle.getString("title", "&e{remain}");
                this.countdownFinalSubtitle = finalTitle.getString("subtitle", "");
            } else {
                this.countdownFinalTitle = "";
                this.countdownFinalSubtitle = "";
            }
        } else {
            this.countdownSound = "";
            this.countdownFinalSound = "";
            this.countdownFinalTitle = "";
            this.countdownFinalSubtitle = "";
        }

        // Preparing - 玩家行为
        ConfigurationSection playerSec = section.getConfigurationSection("player");
        if (playerSec != null) {
            this.playerFreeze = playerSec.getBoolean("freeze", true);
        } else {
            this.playerFreeze = true;
        }
    }

    public int getDuration() { return duration; }
    public boolean isScoreboardEnabled() { return scoreboardEnabled; }
    public String getScoreboardTitle() { return scoreboardTitle; }
    public List<String> getScoreboardLines() { return Collections.unmodifiableList(scoreboardLines); }
    public boolean isTitleEnabled() { return titleEnabled; }
    public String getTitleText() { return titleText; }
    public String getSubtitleText() { return subtitleText; }
    public boolean isActionBarEnabled() { return actionBarEnabled; }
    public String getActionBarText() { return actionBarText; }

    // Playing
    public boolean hasReady() { return readyEnabled; }
    public String getReadyTitle() { return readyTitle; }
    public String getReadySubtitle() { return readySubtitle; }
    public String getReadySound() { return readySound; }
    public String getReadyFinalSound() { return readyFinalSound; }
    public boolean isFriendlyFire() { return friendlyFire; }
    public boolean isNaturalRegen() { return naturalRegen; }
    public boolean isHungerDeplete() { return hungerDeplete; }
    public boolean isFallDamage() { return fallDamage; }
    public boolean isKeepInventory() { return keepInventory; }
    public boolean isKeepLevel() { return keepLevel; }

    // Ending
    public boolean isRespawnDead() { return respawnDead; }
    public boolean isRemovePotions() { return removePotions; }

    // Reward
    public int getRewardParticipation() { return rewardParticipation; }
    public int getRewardKill() { return rewardKill; }
    public int getRewardAssist() { return rewardAssist; }
    public int getRewardWinBonus() { return rewardWinBonus; }
    public boolean isAfterClearInventory() { return afterClearInventory; }
    public boolean isAfterTeleportLobby() { return afterTeleportLobby; }

    // Preparing
    public int getStartDelay() { return startDelay; }
    public String getWaitingTitle() { return waitingTitle; }
    public String getWaitingSubtitle() { return waitingSubtitle; }
    public String getWaitingActionBar() { return waitingActionBar; }
    public boolean hasMatchStartSound() { return matchStartSound; }
    public String getMatchStartSound() { return matchStartSoundStr; }
    public String getMatchStartTitle() { return matchStartTitle; }
    public String getMatchStartSubtitle() { return matchStartSubtitle; }
    public String getMatchStartBroadcast() { return matchStartBroadcast; }
    public String getCountdownSound() { return countdownSound; }
    public String getCountdownFinalSound() { return countdownFinalSound; }
    public String getCountdownFinalTitle() { return countdownFinalTitle; }
    public String getCountdownFinalSubtitle() { return countdownFinalSubtitle; }
    public boolean isPlayerFreeze() { return playerFreeze; }
}
