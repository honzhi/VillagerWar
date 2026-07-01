package com.yourname.villagerwar.config.rule;

import com.yourname.villagerwar.config.holder.GameModesConfig;

/**
 * 从 RulePreset 构建不可变的 GameRule 实例。
 * 工厂方法模式，集中处理默认值回退与校验逻辑。
 */
public final class GameRuleLoader {

    private GameRuleLoader() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 从 RulePreset 创建 GameRule。
     *
     * @param preset 规则预设（非 null）
     * @return 不可变的 GameRule 实例
     */
    public static GameRule load(GameModesConfig.RulePreset preset) {
        return new GameRule(
            preset.getName(),
            preset.getDisplayName(),
            preset.getMaxPlayers(),
            preset.getMinPlayers(),
            preset.getGameTime(),
            preset.getPrepareTime(),
            preset.getSkillSelectTime(),
            preset.getSkillShowTime(),
            preset.getRespawnTime(),
            preset.getInvincibleTime(),
            preset.getStartGold(),
            preset.getGoldPerInterval(),
            preset.getGoldInterval(),
            preset.getKillReward(),
            preset.getAssistReward(),
            preset.getMaxTeamSize(),
            preset.isFriendlyFire(),
            preset.isAllowSpectate(),
            preset.isAutoBalance(),
            preset.getWinCondition()
        );
    }
}
