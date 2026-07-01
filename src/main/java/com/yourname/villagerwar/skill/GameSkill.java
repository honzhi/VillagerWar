package com.yourname.villagerwar.skill;

import org.bukkit.Material;

public class GameSkill {
    private final String id;
    private final String displayName;
    private final int cooldown;
    private final String mythicSkillName;
    private final Material icon;
    private final String description;
    private final String modeKey;
    private final int timer;

    public GameSkill(String id, String displayName, int cooldown, String mythicSkillName,
                     Material icon, String description, String modeKey, int timer) {
        this.id = id;
        this.displayName = displayName;
        this.cooldown = cooldown;
        this.mythicSkillName = mythicSkillName;
        this.icon = icon;
        this.description = description;
        this.modeKey = modeKey;
        this.timer = timer;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getCooldown() { return cooldown; }
    public String getMythicSkillName() { return mythicSkillName; }
    public Material getIcon() { return icon; }
    public String getDescription() { return description; }
    public String getModeKey() { return modeKey; }
    public int getTimer() { return timer; }
}
