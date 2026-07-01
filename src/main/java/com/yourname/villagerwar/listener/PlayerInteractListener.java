package com.yourname.villagerwar.listener;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.VillagerWar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class PlayerInteractListener implements Listener {
    private final VillagerWar plugin;

    public PlayerInteractListener(VillagerWar plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Optional<Game> gameOpt = plugin.getGameManager().getGame(player);
        if (!gameOpt.isPresent()) return;
        Game game = gameOpt.get();
        GamePlayer gp = game.getPlayer(player.getUniqueId());
        if (gp == null) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && gp.getSkill() != null && item.getType() == gp.getSkill().getIcon()) {
            game.getSkillManager().useSkill(gp);
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity clicked = event.getRightClicked();

        if (clicked instanceof Villager) {
            Optional<Game> gameOpt = plugin.getGameManager().getGame(player);
            if (gameOpt.isPresent()) {
                event.setCancelled(true);
                com.yourname.villagerwar.shop.ShopGUI shopGUI = new com.yourname.villagerwar.shop.ShopGUI();
                shopGUI.open(player, "defaults");
            }
        }
    }
}