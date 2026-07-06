package com.yourname.villagerwar.listener;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.GameState;
import com.yourname.villagerwar.VillagerWar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

/**
 * 防止玩家攻击友方小兵/基地
 * 通过监听 EntityDamageByEntityEvent 检查双方阵营
 */
public class EntityDamageListener implements Listener {

    private final VillagerWar plugin;

    public EntityDamageListener(VillagerWar plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // 只处理玩家作为伤害来源
        if (!(event.getDamager() instanceof Player player)) return;

        // 只处理带有 vw_team 标记的实体（小兵/基地）
        Entity damaged = event.getEntity();
        String entityTeam = damaged.getPersistentDataContainer().get(
            new org.bukkit.NamespacedKey(plugin, "vw_team"),
            PersistentDataType.STRING
        );
        if (entityTeam == null) return;

        // 获取玩家所在的游戏
        Optional<Game> gameOpt = plugin.getGameManager().getGame(player);
        if (gameOpt.isEmpty()) return;
        Game game = gameOpt.get();
        if (game.getState() != GameState.PLAYING) return;

        // 获取玩家的队伍
        GamePlayer gp = game.getPlayer(player.getUniqueId());
        if (gp == null || gp.getTeam() == null) return;

        String playerTeam = gp.getTeam().name(); // "RED" 或 "BLUE"

        // 同队 → 取消伤害
        if (playerTeam.equalsIgnoreCase(entityTeam)) {
            event.setCancelled(true);
        }
    }
}