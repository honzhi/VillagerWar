package com.yourname.villagerwar.listener;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.GameState;
import com.yourname.villagerwar.VillagerWar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Optional;

public class PlayerDeathListener implements Listener {
    private final VillagerWar plugin;

    public PlayerDeathListener(VillagerWar plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Optional<Game> gameOpt = plugin.getGameManager().getGame(victim);
        if (gameOpt.isEmpty()) return;

        Game game = gameOpt.get();
        if (game.getState() != GameState.PLAYING) return;

        GamePlayer gpVictim = game.getPlayer(victim.getUniqueId());
        if (gpVictim == null) return;

        // Track death
        gpVictim.addDeath();

        // Handle killer
        Player killer = victim.getKiller();
        if (killer != null) {
            GamePlayer gpKiller = game.getPlayer(killer.getUniqueId());
            if (gpKiller != null) {
                gpKiller.addKill();
                int killReward = game.getGameRule().getKillReward();
                gpKiller.addGold(killReward);

                // Broadcast kill message and check kill streak
                int streak = gpKiller.getKills();
                if (streak == 3) {
                    broadcastKillStreak(game, killer, "&e" + killer.getName() + " &7正在大杀特杀！（3连杀）");
                } else if (streak == 5) {
                    broadcastKillStreak(game, killer, "&6" + killer.getName() + " &7无人能挡！（5连杀）");
                } else if (streak == 10) {
                    broadcastKillStreak(game, killer, "&4" + killer.getName() + " &7已经超越神了！（10连杀）");
                } else {
                    game.getUiManager().getMessageManager().broadcastMessage("kill.normal",
                            "player", victim.getName(),
                            "killer", killer.getName());
                }

                // Send kill bonus message to killer
                game.getUiManager().getMessageManager().sendMessage(killer, "economy.kill_bonus",
                        "amount", String.valueOf(killReward));
            }
        }

        // Respawn handling delegated to RespawnManager
                game.getRespawnManager().addToRespawnQueue(gpVictim, game.getGameRule().getRespawnTime());

        // Clear default death message (we handle it via UIManager)
        event.deathMessage(null);
    }

    private void broadcastKillStreak(Game game, Player player, String message) {
        game.getPlayers().forEach(gp -> {
            Player p = gp.getPlayer();
            if (p != null && p.isOnline()) {
                p.sendMessage("§7[§6村民战争§7] §r" + message);
            }
        });
    }
}