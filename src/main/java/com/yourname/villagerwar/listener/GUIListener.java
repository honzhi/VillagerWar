package com.yourname.villagerwar.listener;

import com.yourname.villagerwar.VillagerWar;
import com.yourname.villagerwar.gui.LobbyGUI;
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

        // 只处理大厅 GUI (game_gui/)
        String guiName = LobbyGUI.getOpenGUI(player.getName());
        if (guiName == null) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        String title = event.getView().getTitle();
        int slot = event.getSlot();
        LobbyGUI.handleClick(player, guiName, slot, title, event.getCurrentItem());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            LobbyGUI.removePlayer(event.getPlayer().getName());
        }
    }
}