package com.yourname.villagerwar.manager;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.GameState;
import com.yourname.villagerwar.skill.GameSkill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkillManager {

    private final Game game;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public SkillManager(Game game) {
        this.game = game;
    }

    /**
     * 打开技能选择 GUI（仅 SKILL_SELECT 状态）。
     */
    public void openSkillSelectGUI() {
        if (game.getState() != GameState.SKILL_SELECT) return;
        // TODO: 打开技能选择 GUI，让玩家从可用技能列表中选取一个
    }

    /**
     * 在 SKILL_SHOW 阶段展示双方队伍选择的技能。
     */
    public void showSkills() {
        if (game.getState() != GameState.SKILL_SHOW) return;
        // TODO: 展示双方每个玩家选择的技能信息
    }

    /**
     * 玩家释放技能，执行冷却检查。
     */
    public void useSkill(GamePlayer player) {
        GameSkill skill = player.getSkill();
        if (skill == null) return;

        if (isOnCooldown(player)) {
            // TODO: 发送冷却未结束的消息
            return;
        }

        UUID uuid = player.getUuid();
        long now = System.currentTimeMillis();
        cooldowns.put(uuid, now);

        // TODO: 通过 MMBridge 触发技能效果（MythicMobs Skill）
    }

    /**
     * 检查玩家技能是否处于冷却中。
     */
    public boolean isOnCooldown(GamePlayer player) {
        GameSkill skill = player.getSkill();
        if (skill == null) return true;

        long now = System.currentTimeMillis();
        long lastUse = cooldowns.getOrDefault(player.getUuid(), 0L);
        long elapsed = now - lastUse;
        return elapsed < skill.getCooldown() * 50L;
    }

    /**
     * 获取剩余冷却时间（毫秒）。
     */
    public long getRemainingCooldown(GamePlayer player) {
        GameSkill skill = player.getSkill();
        if (skill == null) return 0;

        long now = System.currentTimeMillis();
        long lastUse = cooldowns.getOrDefault(player.getUuid(), 0L);
        long elapsed = now - lastUse;
        long total = skill.getCooldown() * 50L;
        return Math.max(0, total - elapsed);
    }

    public Game getGame() {
        return game;
    }
}
