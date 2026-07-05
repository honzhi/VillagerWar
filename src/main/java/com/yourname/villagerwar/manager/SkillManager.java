package com.yourname.villagerwar.manager;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.GameState;
import com.yourname.villagerwar.skill.GameSkill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;

public class SkillManager {

    private final Game game;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final java.util.Set<UUID> skillSelected = new java.util.HashSet<>();
    private final java.util.Map<UUID, Integer> passiveTickCounters = new java.util.HashMap<>();
    private final java.util.Map<UUID, ItemCooldownDisplay> cooldownDisplays = new java.util.HashMap<>();

    public SkillManager(Game game) {
        this.game = game;
    }

    /**
     * 重置所有玩家的技能选择状态（进入SKILL_SELECT时调用）
     */
    public void resetSelections() {
        skillSelected.clear();
    }

    /**
     * 标记玩家已选择技能
     */
    public void markSkillSelected(UUID playerUuid) {
        skillSelected.add(playerUuid);
    }

    /**
     * 检查玩家是否已选择技能
     */
    public boolean hasSelected(UUID playerUuid) {
        return skillSelected.contains(playerUuid);
    }

    /**
     * 检查是否所有在线玩家都已选择技能
     */
    public boolean allPlayersReady() {
        for (GamePlayer gp : game.getPlayers()) {
            org.bukkit.entity.Player p = gp.getPlayer();
            if (p != null && p.isOnline() && !skillSelected.contains(gp.getUuid())) {
                return false;
            }
        }
        return game.getPlayers().isEmpty() ? false : true;
    }

    /**
     * 打开技能选择 GUI（仅 SKILL_SELECT 状态）。
     */
    public void openSkillSelectGUI() {
        if (game.getState() != GameState.SKILL_SELECT) return;
        // 为每个在线玩家打开技能选择 GUI
        for (GamePlayer gp : game.getPlayers()) {
            org.bukkit.entity.Player player = gp.getPlayer();
            if (player != null && player.isOnline()) {
                com.yourname.villagerwar.gui.SkillSelectGUI.open(player);
            }
        }
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
     * @return true=技能已释放, false=冷却中或无技能
     */
    public boolean useSkill(GamePlayer player) {
        GameSkill skill = player.getSkill();
        if (skill == null) return false;

        if (isOnCooldown(player)) {
            Player p = player.getPlayer();
            if (p != null && p.isOnline()) {
                long remainSec = (getRemainingCooldown(player) + 999) / 1000;
                p.sendActionBar("§c技能冷却中: §e" + remainSec + "§c秒");
            }
            return false;
        }

        // 触发 MythicMobs 技能
        if (!skill.getMythicSkillName().isEmpty()) {
            Player p = player.getPlayer();
            if (p != null && p.isOnline()) {
                boolean casted = com.yourname.villagerwar.bridge.MMBridge.castSkill(skill.getMythicSkillName(), p);
                if (!casted) {
                    p.sendActionBar("§c技能释放失败");
                    return false;
                }
            }
        }

        // 记录冷却
        UUID uuid = player.getUuid();
        long now = System.currentTimeMillis();
        cooldowns.put(uuid, now);

        // 冷却提示
        Player p = player.getPlayer();
        if (p != null && p.isOnline()) {
            p.sendActionBar("§a已释放技能: §e" + skill.getDisplayName());
        }
        return true;
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
        return elapsed < skill.getCooldown() * 1000L;
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
        long total = skill.getCooldown() * 1000L;
        return Math.max(0, total - elapsed);
    }

    public Game getGame() {
        return game;
    }

    public void tick() {
        if (game.getState() != com.yourname.villagerwar.GameState.PLAYING) return;
        long now = System.currentTimeMillis();
        for (GamePlayer gp : game.getPlayers()) {
            Player p = gp.getPlayer();
            if (p == null || !p.isOnline()) continue;
            GameSkill skill = gp.getSkill();
            if (skill == null) continue;
            java.util.UUID uuid = gp.getUuid();
            if (skill.getModeKey().equalsIgnoreCase("timer") && skill.getTimer() > 0) {
                int counter = passiveTickCounters.getOrDefault(uuid, 0) + 1;
                passiveTickCounters.put(uuid, counter);
                if (counter >= skill.getTimer()) {
                    passiveTickCounters.put(uuid, 0);
                    if (!skill.getMythicSkillName().isEmpty()) {
                        com.yourname.villagerwar.bridge.MMBridge.castSkill(skill.getMythicSkillName(), p);
                    }
                }
            }
        }
    }

    private void updateCooldownDisplay(Player p, GamePlayer gp, GameSkill skill, long now) {
        if (skill.getCooldown() <= 0) return;
        if (skill.getModeKey().equalsIgnoreCase("timer") || skill.getModeKey().equalsIgnoreCase("always")) return;
        org.bukkit.inventory.ItemStack item = p.getInventory().getItem(8);
        if (item == null || !com.yourname.villagerwar.util.MessageUtil.isSkillItem(item)) return;
        long lastUse = cooldowns.getOrDefault(gp.getUuid(), 0L);
        long elapsed = now - lastUse;
        long total = skill.getCooldown() * 1000L;
        long remaining = Math.max(0, total - elapsed);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7右键释放技能"));
        if (remaining > 0) {
            int remainSec = (int)((remaining + 999) / 1000);
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&c冷却中: &e" + remainSec + "&c秒"));
        } else {
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&a就绪"));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    public void resetPassiveCounters() {
        passiveTickCounters.clear();
    }

    private static class ItemCooldownDisplay {
    }
}
