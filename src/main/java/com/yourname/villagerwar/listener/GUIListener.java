package com.yourname.villagerwar.listener;

import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.shop.GameGUI;
import com.yourname.villagerwar.shop.ShopGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class GUIListener implements Listener {

    private final VillagerWar plugin;

    public GUIListener(VillagerWar plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // 检查是否在 GameGUI 中
        String guiName = GameGUI.getOpenGUI(player.getName());
        if (guiName != null) {
            event.setCancelled(true); // 禁止拿物品

            if (event.getCurrentItem() == null) return;
            String title = event.getView().getTitle();

            // 处理点击动作
            int slot = event.getSlot();
            GameGUI.handleClick(player, guiName, slot, title, event.getCurrentItem());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            GameGUI.removePlayer(event.getPlayer().getName());
        }
    }
}