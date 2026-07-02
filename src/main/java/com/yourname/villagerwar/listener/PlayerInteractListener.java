package com.yourname.villagerwar.listener;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.VillagerWar;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Optional;

public class PlayerInteractListener implements Listener {
    private final VillagerWar plugin;

    public PlayerInteractListener(VillagerWar plugin) {
        this.plugin = plugin;
    }

    private boolean isBook(Material type) {
        return type == Material.BOOK || type == Material.WRITABLE_BOOK || type == Material.WRITTEN_BOOK;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !isBook(item.getType())) return;

        // 匹配队列中的物品交互（有快照但不在游戏中 = 排队/备战状态）
        if (plugin.getInventoryManager().hasSnapshot(player)) {
            event.setCancelled(true);
            ItemMeta meta = item.getItemMeta();
            String name = (meta != null && meta.hasDisplayName()) ? meta.getDisplayName() : "";

            if (name.contains("退出匹配")) {
                // 退出匹配 - 传送回原位并恢复背包
                plugin.getInventoryManager().restoreLocation(player);
                plugin.getInventoryManager().clear(player);
                plugin.getInventoryManager().restore(player);
                plugin.getGameManager().leaveGame(player);
                com.yourname.villagerwar.gui.LobbyGUI.removePlayer(player.getName());
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&7[&6村民战争&7] &e你已退出匹配队列"));
            } else if (name.contains("游戏信息")) {
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&7[&6村民战争&7] &e暂无游戏信息"));
            }
            return;
        }

        // 游戏中技能使用
        Optional<Game> gameOpt = plugin.getGameManager().getGame(player);
        if (!gameOpt.isPresent()) return;
        Game game = gameOpt.get();
        GamePlayer gp = game.getPlayer(player.getUniqueId());
        if (gp == null || gp.getSkill() == null) return;

        if (item.getType() == gp.getSkill().getIcon()) {
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