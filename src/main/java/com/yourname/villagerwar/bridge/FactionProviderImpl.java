package com.yourname.villagerwar.bridge;

import com.yourname.villagerwar.Game;
import com.yourname.villagerwar.GamePlayer;
import com.yourname.villagerwar.VillagerWar;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.UUID;

/**
 * MythicMobs FactionProvider 动态代理实现
 * 通过反射注册，避免编译时依赖 MythicMobs
 */
public class FactionProviderImpl {

    private static Object proxyInstance;

    /**
     * 注册到 MythicMobs PlayerManager
     */
    public static void register() {
        try {
            Class<?> factionProviderClass = Class.forName("io.lumine.mythic.core.players.factions.FactionProvider");
            Class<?> playerManagerClass = Class.forName("io.lumine.mythic.core.players.PlayerManager");

            // 创建动态代理
            proxyInstance = Proxy.newProxyInstance(
                factionProviderClass.getClassLoader(),
                new Class<?>[]{factionProviderClass},
                new FactionHandler()
            );

            // MythicBukkit.inst().getPlayerManager().registerFactionProvider(proxy)
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Method instMethod = mythicBukkitClass.getMethod("inst");
            Object mythicBukkit = instMethod.invoke(null);
            Method getPlayerManager = mythicBukkitClass.getMethod("getPlayerManager");
            Object playerManager = getPlayerManager.invoke(mythicBukkit);
            Method registerMethod = playerManagerClass.getMethod("registerFactionProvider", factionProviderClass);
            registerMethod.invoke(playerManager, proxyInstance);

            VillagerWar.getInstance().getLogger().info("[FactionProvider] MM 阵营系统已注册 (RED→red_team, BLUE→blue_team)");
        } catch (Exception e) {
            VillagerWar.getInstance().getLogger().warning("[FactionProvider] 注册失败: " + e.getMessage());
        }
    }

    private static class FactionHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();

            // isInFaction(AbstractPlayer, String)
            if ("isInFaction".equals(name) && args != null && args.length == 2) {
                UUID uuid = extractUuid(args[0]);
                String faction = (String) args[1];
                String pf = getFactionName(uuid);
                return pf != null && pf.equalsIgnoreCase(faction);
            }

            // getFaction(AbstractPlayer) → Optional<String>
            if ("getFaction".equals(name) && args != null && args.length == 1) {
                UUID uuid = extractUuid(args[0]);
                String faction = getFactionName(uuid);
                if (faction != null) {
                    return Optional.of(faction);
                }
                return Optional.empty();
            }

            // getFaction(UUID) → Optional<String>
            if ("getFaction".equals(name) && args != null && args.length == 1 && args[0] instanceof UUID) {
                UUID uuid = (UUID) args[0];
                String faction = getFactionName(uuid);
                if (faction != null) {
                    return Optional.of(faction);
                }
                return Optional.empty();
            }

            return null;
        }

        private UUID extractUuid(Object abstractPlayer) throws Exception {
            Method getUniqueId = abstractPlayer.getClass().getMethod("getUniqueId");
            return (UUID) getUniqueId.invoke(abstractPlayer);
        }

        private String getFactionName(UUID uuid) {
            VillagerWar plugin = VillagerWar.getInstance();
            if (plugin == null) return null;
            for (Game game : plugin.getGameManager().getGames()) {
                GamePlayer gp = game.getPlayer(uuid);
                if (gp != null && gp.getTeam() != null) {
                    return gp.getTeam() == GamePlayer.Team.RED ? "red_team" : "blue_team";
                }
            }
            return null;
        }
    }
}