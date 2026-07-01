package com.yourname.villagerwar;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class GamePlayer {
    private final UUID uuid;
    private Team team;
    private com.yourname.villagerwar.skill.GameSkill skill;
    private int gold;
    private int kills;
    private int deaths;
    private int assists;
    private long lastClickTime;

    public GamePlayer(UUID uuid) {
        this.uuid = uuid;
        this.gold = 0;
        this.kills = 0;
        this.deaths = 0;
        this.assists = 0;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public UUID getUuid() { return uuid; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public com.yourname.villagerwar.skill.GameSkill getSkill() { return skill; }
    public void setSkill(com.yourname.villagerwar.skill.GameSkill skill) { this.skill = skill; }
    public int getGold() { return gold; }
    public void setGold(int gold) { this.gold = gold; }
    public void addGold(int amount) { this.gold += amount; }
    public boolean takeGold(int amount) {
        if (gold < amount) return false;
        gold -= amount;
        return true;
    }
    public int getKills() { return kills; }
    public void addKill() { this.kills++; }
    public int getDeaths() { return deaths; }
    public void addDeath() { this.deaths++; }
    public int getAssists() { return assists; }
    public void addAssist() { this.assists++; }
    public long getLastClickTime() { return lastClickTime; }
    public void setLastClickTime(long time) { this.lastClickTime = time; }

    public enum Team {
        RED, BLUE;

        public Team getOpposite() {
            return this == RED ? BLUE : RED;
        }
    }
}
