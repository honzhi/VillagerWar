package com.yourname.villagerwar.listener;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * 技能物品锁定 + 死亡不掉落
 */
public class SkillItemListener implements Listener {

    private final VillagerWar plugin;

    public SkillItemListener(VillagerWar plugin) {
        this.plugin = plugin;
    }

    /**
     * 禁止丢弃技能物品
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (MessageUtil.isSkillItem(item)) {
            event.setCancelled(true);
        }
    }

    /**
     * 禁止在背包界面移动技能物品
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        // 检查被点击的物品
        ItemStack current = event.getCurrentItem();
        if (current != null && MessageUtil.isSkillItem(current)) {
            event.setCancelled(true);
            return;
        }

        // 检查光标上的物品
        ItemStack cursor = event.getCursor();
        if (cursor != null && MessageUtil.isSkillItem(cursor)) {
            event.setCancelled(true);
            return;
        }

        // 检查是否将物品移到槽位8（技能物品专用槽位）
        if (event.getSlot() == 8 && current != null && !MessageUtil.isSkillItem(current)) {
            // 防止其它物品放入槽位8
            Player player = (Player) event.getWhoClicked();
            Optional<Game> gameOpt = plugin.getGameManager().getGame(player);
            if (gameOpt.isPresent() && gameOpt.get().getState() == com.yourname.villagerwar.GameState.PLAYING) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * 禁止拖拽技能物品
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        // 检查拖拽的原始物品
        ItemStack oldCursor = event.getOldCursor();
        if (oldCursor != null && MessageUtil.isSkillItem(oldCursor)) {
            event.setCancelled(true);
            return;
        }

        // 检查涉及技能物品槽位的拖拽（包括槽位8）
        for (int slot : event.getRawSlots()) {
            if (slot == 8) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * 技能物品死亡不掉落
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Optional<Game> gameOpt = plugin.getGameManager().getGame(player);
        if (gameOpt.isEmpty()) return;
        if (gameOpt.get().getState() != com.yourname.villagerwar.GameState.PLAYING) return;

        // 从死亡掉落物中移除技能物品
        if (event.getDrops() != null) {
            event.getDrops().removeIf(MessageUtil::isSkillItem);
        }

        // 同时从保留物品中确保技能物品被保留（但默认keepInventory=false，需要额外逻辑）
        // Bukkit 的 keepInventory 由游戏规则控制，此处仅清理掉落列表
    }
}