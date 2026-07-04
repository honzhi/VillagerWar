package com.yourname.villagerwar.gui;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.GameState;
import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.config.holder.SkillsConfig;
import com.yourname.villagerwar.skill.GameSkill;
import com.yourname.villagerwar.util.MessageUtil;
import org.bukkit.Bukkit;
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
    private static final Map<String, Integer> playerPage = new HashMap<>();
    private static final Map<String, Integer> playerTotalPages = new HashMap<>();

    public static void open(Player player) {
        YamlConfiguration config = GUIUtils.loadConfig("skill_select");
        if (config == null) {
            player.sendMessage(MessageUtil.colorize("&c技能选择GUI配置不存在"));
            return;
        }
        open(player, config, 1);
    }

    private static void open(Player player, YamlConfiguration config, int page) {
        // 读取扁平 layout
        List<String> layoutLines = config.getStringList("layout");
        if (layoutLines.isEmpty()) {
            player.sendMessage(MessageUtil.colorize("&c技能选择GUI布局未配置"));
            return;
        }
        int rows = Math.max(1, Math.min(layoutLines.size(), 6));
        String rawTitle = config.getString("title", "&6选择技能");
        String title = MessageUtil.colorize(rawTitle);
        Inventory inv = Bukkit.createInventory(null, rows * 9, title);
        ConfigurationSection baseSection = config.getConfigurationSection("base");

        // 读取 skills 配置
        ConfigurationSection skillsSection = config.getConfigurationSection("skills");
        String symbol = "0";
        List<String> defaultClicks = new ArrayList<>();
        if (skillsSection != null) {
            symbol = skillsSection.getString("symbol", "0");
            defaultClicks.addAll(skillsSection.getStringList("click"));
        }

        // 计算每页几个技能槽位
        int skillsPerPage = 0;
        for (String line : layoutLines) {
            for (int col = 0; col < line.length(); col++) {
                String c = String.valueOf(line.charAt(col));
                if (c.equals("0") || c.equals(symbol)) skillsPerPage++;
            }
        }

        // 获取技能列表
        SkillsConfig skillsConfig = VillagerWar.getInstance().getConfigManager().getSkillsConfig();
        List<SkillsConfig.SkillDef> allSkills = (skillsConfig != null) ? skillsConfig.getSkills() : new ArrayList<>();
        int totalPages = Math.max(1, (int) Math.ceil((double) allSkills.size() / Math.max(1, skillsPerPage)));

        // 记录分页信息
        playerPage.put(player.getName(), page);
        playerTotalPages.put(player.getName(), totalPages);

        // 当前页的技能范围
        int startIndex = (page - 1) * skillsPerPage;
        int endIndex = Math.min(startIndex + skillsPerPage, allSkills.size());
        List<SkillsConfig.SkillDef> pageSkills = (startIndex < allSkills.size())
            ? allSkills.subList(startIndex, endIndex)
            : new ArrayList<>();

        // 构建布局
        int skillIdx = 0;
        for (int row = 0; row < layoutLines.size(); row++) {
            String line = layoutLines.get(row);
            for (int col = 0; col < line.length() && col < 9; col++) {
                String key = String.valueOf(line.charAt(col));
                if (key.equals("0") || key.equals(symbol)) continue; // 留给技能
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

        // 填充当前页的技能
        for (int row = 0; row < layoutLines.size() && skillIdx < pageSkills.size(); row++) {
            String line = layoutLines.get(row);
            for (int col = 0; col < line.length() && col < 9 && skillIdx < pageSkills.size(); col++) {
                String key = String.valueOf(line.charAt(col));
                if (!key.equals("0") && !key.equals(symbol)) continue;
                int slot = row * 9 + col;
                SkillsConfig.SkillDef skillDef = pageSkills.get(skillIdx);
                inv.setItem(slot, buildSkillItem(skillDef, defaultClicks));
                skillIdx++;
            }
        }

        // 更新翻页按钮
        if (baseSection != null) {
            if (page > 1) {
                ConfigurationSection prevSec = baseSection.getConfigurationSection("\u2190");
                if (prevSec != null) {
                    for (int slot = 0; slot < inv.getSize(); slot++) {
                        ItemStack exist = inv.getItem(slot);
                        if (exist != null && exist.hasItemMeta()) {
                            String dn = exist.getItemMeta().getDisplayName();
                            if (dn.contains(MessageUtil.colorize("&c\u4e0a\u4e00\u9875")) || dn.contains(MessageUtil.colorize("&cPrevious")) || dn.contains("←") || dn.contains("\u2190")) {
                                // already placed
                                break;
                            }
                        }
                    }
                }
            }
            if (page < totalPages) {
                ConfigurationSection nextSec = baseSection.getConfigurationSection("\u2192");
                // next page button already in layout
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

        // 存储技能ID到物品 NBT
        meta.getPersistentDataContainer().set(SKILL_KEY, PersistentDataType.STRING, skillDef.getId());
        item.setItemMeta(meta);
        return item;
    }

    public static void handleClick(Player player, int slot) {
        YamlConfiguration config = GUIUtils.loadConfig("skill_select");
        if (config == null) return;

        List<String> layoutLines = config.getStringList("layout");
        if (layoutLines.isEmpty()) return;

        ConfigurationSection skillsSection = config.getConfigurationSection("skills");
        String symbol = "0";
        List<String> defaultClicks = new ArrayList<>();
        if (skillsSection != null) {
            symbol = skillsSection.getString("symbol", "0");
            defaultClicks.addAll(skillsSection.getStringList("click"));
        }

        // 检查点击位置对应的字符
        int row = slot / 9;
        int col = slot % 9;
        String key = "0";
        if (row < layoutLines.size() && col < layoutLines.get(row).length()) {
            key = String.valueOf(layoutLines.get(row).charAt(col));
        }

        // 优先检查是否是技能物品（通过 PDC 判断）
        org.bukkit.inventory.InventoryView view = player.getOpenInventory();
        if (view != null) {
            ItemStack clicked = view.getItem(slot);
            if (clicked != null && clicked.hasItemMeta()) {
                String skillId = clicked.getItemMeta().getPersistentDataContainer()
                    .get(SKILL_KEY, PersistentDataType.STRING);
                if (skillId != null && !skillId.isEmpty()) {
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
                    handleSelectSkill(player, skillId);
                    return;
                }
            }
        }

        // 处理非技能的 layout 物品
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
            } else if (action.equals("next_page")) {
                int currentPage = playerPage.getOrDefault(player.getName(), 1);
                int totalPages = playerTotalPages.getOrDefault(player.getName(), 1);
                if (currentPage < totalPages) {
                    open(player, config, currentPage + 1);
                }
            } else if (action.equals("previous_page")) {
                int currentPage = playerPage.getOrDefault(player.getName(), 1);
                if (currentPage > 1) {
                    open(player, config, currentPage - 1);
                }
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

    public static void clearPageData(String playerName) {
        playerPage.remove(playerName);
        playerTotalPages.remove(playerName);
    }
}