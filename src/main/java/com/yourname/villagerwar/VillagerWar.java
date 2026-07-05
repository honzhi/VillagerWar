package com.yourname.villagerwar;

import com.yourname.villagerwar.bridge.MMBridge;
import com.yourname.villagerwar.config.ConfigManager;
import com.yourname.villagerwar.config.holder.SkillsConfig;
import com.yourname.villagerwar.config.holder.PluginConfig;
import com.yourname.villagerwar.manager.InventoryManager;
import com.yourname.villagerwar.world.WorldManager;
import org.bukkit.plugin.java.JavaPlugin;

public class VillagerWar extends JavaPlugin {
    private static VillagerWar instance;
    private GameManager gameManager;
    private ConfigManager configManager;
    private WorldManager worldManager;
    private InventoryManager inventoryManager;

    @Override
    public void onEnable() {
        instance = this;
        this.configManager = new ConfigManager(this);
        this.worldManager = new WorldManager(this);
        this.gameManager = new GameManager();
        this.inventoryManager = new InventoryManager(this);

        configManager.loadAll();

        PluginConfig pc = configManager.getPluginConfig();
        boolean debug = pc.isDebug();
        if (debug) {
            getLogger().info("Debug 模式已启用");
        }

        if (MMBridge.isMythicMobsLoaded()) {
            getLogger().info("[MMBridge] MythicMobs 插件已就绪");
        } else {
            getLogger().info("[MMBridge] MythicMobs 插件未安装或未启用，技能/怪物功能不可用");
        }

        if (MMBridge.isMythicMobsLoaded()) {
            SkillsConfig sc = configManager.getSkillsConfig();
            for (SkillsConfig.SkillDef skill : sc.getSkills()) {
                String mmSkillId = skill.getMythicSkill();
                boolean found = MMBridge.hasSkill(mmSkillId);
                if (debug) {
                    if (found) {
                        getLogger().info("  [技能] " + skill.getId() + " (" + mmSkillId + ") 已就绪");
                    } else {
                        getLogger().warning("  [技能] " + skill.getId() + " (" + mmSkillId + ") 未在 MythicMobs 中找到");
                    }
                }
            }
        }

        registerCommands();
        registerListeners();

        getLogger().info("村民战争 v" + getDescription().getVersion() + " 已启用");
    }

    @Override
    public void onDisable() {
        for (Game game : gameManager.getGames()) {
            game.getController().destroy();
        }
        getLogger().info("村民战争 已禁用");
    }

    private void registerCommands() {
        getCommand("villagerwar").setExecutor(new com.yourname.villagerwar.command.VillagerWarCommand(this));
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new com.yourname.villagerwar.listener.PlayerJoinListener(this), this);
        pm.registerEvents(new com.yourname.villagerwar.listener.PlayerQuitListener(this), this);
        pm.registerEvents(new com.yourname.villagerwar.listener.PlayerDeathListener(this), this);
        pm.registerEvents(new com.yourname.villagerwar.listener.PlayerInteractListener(this), this);
        pm.registerEvents(new com.yourname.villagerwar.listener.PlayerMoveListener(this), this);
        pm.registerEvents(new com.yourname.villagerwar.listener.PlayerRespawnListener(this), this);
        pm.registerEvents(new com.yourname.villagerwar.listener.GUIListener(this), this);
        pm.registerEvents(new com.yourname.villagerwar.listener.SkillItemListener(this), this);
    }

    public static VillagerWar getInstance() { return instance; }
    public GameManager getGameManager() { return gameManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public WorldManager getWorldManager() { return worldManager; }
    public InventoryManager getInventoryManager() { return inventoryManager; }
}