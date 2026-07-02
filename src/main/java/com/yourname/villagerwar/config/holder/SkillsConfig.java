package com.yourname.villagerwar.config.holder;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SkillsConfig {

    private final List<SkillDef> skills;

    public SkillsConfig(ConfigurationSection section) {
        this.skills = new ArrayList<>();
        if (section == null) return;
        ConfigurationSection skillsSection = section.getConfigurationSection("skills");
        if (skillsSection == null) return;
        for (String key : skillsSection.getKeys(false)) {
            ConfigurationSection skillSection = skillsSection.getConfigurationSection(key);
            if (skillSection == null) continue;
            this.skills.add(new SkillDef(key, skillSection));
        }
    }

    public List<SkillDef> getSkills() {
        return Collections.unmodifiableList(skills);
    }

    public SkillDef getSkill(String id) {
        for (SkillDef s : skills) {
            if (s.getId().equals(id)) return s;
        }
        return null;
    }

    public static class SkillDef {
        private final String id;
        private final String displayName;
        private final Material material;
        private final int cooldown;
        private final String mythicSkill;
        private final String modeKey;
        private final int timer;

        public SkillDef(String id, ConfigurationSection section) {
            this.id = id;
            this.displayName = section.getString("display-name", id);
            String matName = section.getString("material", "DIAMOND_SWORD");
            Material mat;
            try {
                mat = Material.valueOf(matName.toUpperCase());
            } catch (IllegalArgumentException e) {
                mat = Material.DIAMOND_SWORD;
            }
            this.material = mat;
            this.cooldown = section.getInt("cooldown", 10);
            this.mythicSkill = section.getString("skill.id", "");
            this.modeKey = section.getString("mode-key", "DEFAULT");
            this.timer = section.getInt("timer", 0);
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public Material getMaterial() { return material; }
        public int getCooldown() { return cooldown; }
        public String getMythicSkill() { return mythicSkill; }
        public String getModeKey() { return modeKey; }
        public int getTimer() { return timer; }
    }
}