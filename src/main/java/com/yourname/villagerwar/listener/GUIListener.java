package com.yourname.villagerwar.listener;

import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.gui.*;
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

        // 大厅 GUI
        String guiName = GUIUtils.getOpenGUI(player.getName());
        if (guiName != null) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            int slot = event.getSlot();
            switch (guiName) {
                case "map_select":
                    MapSelectGUI.handleClick(player, slot);
                    break;
                case "mode_select":
                    ModeSelectGUI.handleClick(player, slot);
                    break;
                case "skill_select":
                    SkillSelectGUI.handleClick(player, slot);
                    break;
            }
            return;
        }

        // 商店 GUI
        if (ShopGUI.isOpen(player)) {
            ShopGUI.handleClick(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            String playerName = event.getPlayer().getName();
            if (GUIUtils.getOpenGUI(playerName) != null) {
                String closedGui = GUIUtils.getOpenGUI(playerName);
                if ("skill_select".equals(closedGui)) {
                    SkillSelectGUI.clearPageData(playerName);
                }
                GUIUtils.removePlayer(playerName);
            }
            if (ShopGUI.isOpen((Player) event.getPlayer())) {
                ShopGUI.close(playerName);
            }
        }
    }
}