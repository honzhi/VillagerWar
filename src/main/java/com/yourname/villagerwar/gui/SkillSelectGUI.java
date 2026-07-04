package com.yourname.villagerwar.gui;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.GameState;
import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.config.holder.SkillsConfig;
import com.yourname.villagerwar.skill.GameSkill;
import com.yourname.villagerwar.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class SkillSelectGUI {

    private static final NamespacedKey SKILL_KEY = new NamespacedKey(VillagerWar.getInstance(), "skill_id");

    public static void open(Player player) {
        YamlConfiguration config = GUIUtils.loadConfig("skill_select");
        if (config == null) {
            player.sendMessage(MessageUtil.colorize("&c技能选择GUI配置不存在"));
            return;
        }

        Inventory inv = GUIUtils.createInventory(config, null);
        ConfigurationSection baseSection = config.getConfigurationSection("base");

        // 读取 symbol 配置（默认 0）
        ConfigurationSection skillsSection = config.getConfigurationSection("skills");
        String symbol = "0";
        List<String> defaultClicks = new ArrayList<>();
        if (skillsSection != null) {
            symbol = skillsSection.getString("symbol", "0");
            defaultClicks.addAll(skillsSection.getStringList("click"));
        }

        // 构建布局（跳过 symbol 位置，留空给技能）
        ConfigurationSection layoutSection = config.getConfigurationSection("layout");
        List<String> layoutLines = new ArrayList<>();
        if (layoutSection != null) {
            layoutLines.addAll(layoutSection.getStringList("page1"));
            if (layoutLines.isEmpty()) layoutLines.addAll(layoutSection.getStringList("1"));
        }

        for (int row = 0; row < layoutLines.size(); row++) {
            String line = layoutLines.get(row);
            for (int col = 0; col < line.length() && col < 9; col++) {
                char c = line.charAt(col);
                String key = String.valueOf(c);
                if (key.equals("0") || key.equals(symbol)) continue;

                int slot = row * 9 + col;
                ItemStack item = null;
                if (config.contains(key)) {
                    item = GUIUtils.buildItem(config.getConfigurationSection(key));
                }
                if (item == null && baseSection != null && baseSection.contains(key)) {
                    item = GUIUtils.buildItem(baseSection.getConfigurationSection(key));
                }
                if (item != null) inv.setItem(slot, item);
            }
        }

        // 从 SkillsConfig 获取技能列表，顺序填入 symbol 位置
        SkillsConfig skillsConfig = VillagerWar.getInstance().getConfigManager().getSkillsConfig();
        List<SkillsConfig.SkillDef> skills = (skillsConfig != null) ? skillsConfig.getSkills() : new ArrayList<>();
        if (!skills.isEmpty()) {
            int skillIndex = 0;
            for (int row = 0; row < layoutLines.size() && skillIndex < skills.size(); row++) {
                String line = layoutLines.get(row);
                for (int col = 0; col < line.length() && col < 9 && skillIndex < skills.size(); col++) {
                    char c = line.charAt(col);
                    String key = String.valueOf(c);
                    if (!key.equals("0") && !key.equals(symbol)) continue;
                    int slot = row * 9 + col;
                    SkillsConfig.SkillDef skillDef = skills.get(skillIndex);
                    inv.setItem(slot, buildSkillItem(skillDef, defaultClicks));
                    skillIndex++;
                }
            }
        }

        player.openInventory(inv);
        GUIUtils.setOpenGUI(player.getName(), "skill_select");
    }

    private static ItemStack buildSkillItem(SkillsConfig.SkillDef skillDef, List<String> defaultClicks) {
        Material material = skillDef.getMaterial();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(MessageUtil.colorize("&e" + skillDef.getDisplayName()));

        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.colorize(" &7冷却: &f" + skillDef.getCooldown() + "秒"));
        if (!skillDef.getMythicSkill().isEmpty()) {
            lore.add(MessageUtil.colorize(" &7技能ID: &f" + skillDef.getMythicSkill()));
        }
        lore.add("");
        lore.add(MessageUtil.colorize("&e点击选择此技能"));
        meta.setLore(lore);

        // 存储技能ID到物品
        meta.getPersistentDataContainer().set(SKILL_KEY, PersistentDataType.STRING, skillDef.getId());

        item.setItemMeta(meta);
        return item;
    }

    public static void handleClick(Player player, int slot) {
        YamlConfiguration config = GUIUtils.loadConfig("skill_select");
        if (config == null) return;

        // 读取 symbol 和默认 click
        ConfigurationSection skillsSection = config.getConfigurationSection("skills");
        String symbol = "0";
        List<String> defaultClicks = new ArrayList<>();
        if (skillsSection != null) {
            symbol = skillsSection.getString("symbol", "0");
            defaultClicks.addAll(skillsSection.getStringList("click"));
        }

        // 检查点击的槽位
        char c = GUIUtils.getSlotChar(config, slot);
        String key = String.valueOf(c);

        // 优先检查是否是技能物品（通过 PDC）
        org.bukkit.inventory.InventoryView view = player.getOpenInventory();
        if (view != null) {
            ItemStack clicked = view.getItem(slot);
            if (clicked != null && clicked.hasItemMeta()) {
                String skillId = clicked.getItemMeta().getPersistentDataContainer()
                    .get(SKILL_KEY, PersistentDataType.STRING);
                if (skillId != null && !skillId.isEmpty()) {
                    // 执行默认 click 效果（如音效）
                    for (String action : defaultClicks) {
                        action = action.trim();
                        if (action.startsWith("sound:")) {
                            String[] parts = action.substring(6).trim().split(" ");
                            try {
                                org.bukkit.Sound sound = org.bukkit.Sound.valueOf(parts[0]);
                                float vol = parts.length > 1 ? Float.parseFloat(parts[1]) : 1f;
                                float pit = parts.length > 2 ? Float.parseFloat(parts[2]) : 1f;
                                player.playSound(player.getLocation(), sound, vol, pit);
                            } catch (Exception ignored) {}
                        }
                    }
                    // 自动触发 select_skill
                    handleSelectSkill(player, skillId);
                    return;
                }
            }
        }

        // 非技能物品，按普通 layout 处理（关闭、翻页等）
        if (key.equals("0") || key.equals(symbol)) return;
        ConfigurationSection section = config.getConfigurationSection(key);
        if (section == null && config.contains("base." + key)) {
            section = config.getConfigurationSection("base." + key);
        }
        if (section == null) return;

        List<String> clicks = section.getStringList("click");
        for (String action : clicks) {
            action = action.trim();
            if (action.startsWith("sound:")) {
                String[] parts = action.substring(6).trim().split(" ");
                try {
                    org.bukkit.Sound sound = org.bukkit.Sound.valueOf(parts[0]);
                    float vol = parts.length > 1 ? Float.parseFloat(parts[1]) : 1f;
                    float pit = parts.length > 2 ? Float.parseFloat(parts[2]) : 1f;
                    player.playSound(player.getLocation(), sound, vol, pit);
                } catch (Exception ignored) {}
            } else if (action.equals("close")) {
                player.closeInventory();
            } else if (action.startsWith("message:")) {
                player.sendMessage(MessageUtil.colorize(action.substring(8).trim()));
            }
        }
    }

    private static void handleSelectSkill(Player player, String skillId) {
        VillagerWar plugin = VillagerWar.getInstance();
        java.util.Optional<Game> gameOpt = plugin.getGameManager().getGame(player);
        if (gameOpt.isEmpty()) return;

        Game game = gameOpt.get();
        GamePlayer gp = game.getPlayer(player.getUniqueId());
        if (gp == null || game.getState() != GameState.SKILL_SELECT) return;

        SkillsConfig.SkillDef skillDef = plugin.getConfigManager().getSkillsConfig().getSkill(skillId);
        if (skillDef != null) {
            gp.setSkill(new GameSkill(
                skillDef.getId(), skillDef.getDisplayName(), skillDef.getCooldown(),
                skillDef.getMythicSkill(), skillDef.getMaterial(), "",
                skillDef.getModeKey(), skillDef.getTimer()));
            game.getSkillManager().markSkillSelected(player.getUniqueId());
            player.sendMessage(MessageUtil.colorize("&a已选择技能: &e" + skillDef.getDisplayName()));
            player.closeInventory();
        } else {
            player.sendMessage(MessageUtil.colorize("&c技能配置未找到: " + skillId));
        }
    }
}