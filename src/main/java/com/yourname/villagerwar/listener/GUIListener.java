package com.yourname.villagerwar.listener;

import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.gui.LobbyGUI;
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
        String guiName = LobbyGUI.getOpenGUI(player.getName());
        if (guiName != null) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            String title = event.getView().getTitle();
            int slot = event.getSlot();
            LobbyGUI.handleClick(player, guiName, slot, title, event.getCurrentItem());
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
            // 仅当玩家确实打开了插件GUI时才清理
            if (LobbyGUI.getOpenGUI(playerName) != null) {
                LobbyGUI.removePlayer(playerName);
            }
            if (ShopGUI.isOpen((Player) event.getPlayer())) {
                ShopGUI.close(playerName);
            }
        }
    }
}