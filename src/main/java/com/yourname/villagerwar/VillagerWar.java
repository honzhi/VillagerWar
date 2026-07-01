package com.yourname.villagerwar;

import com.yourname.villagerwar.config.ConfigManager;
import com.yourname.villagerwar.world.WorldManager;
import org.bukkit.plugin.java.JavaPlugin;

public class VillagerWar extends JavaPlugin {
    private static VillagerWar instance;
    private GameManager gameManager;
    private ConfigManager configManager;
    private WorldManager worldManager;

    @Override
    public void onEnable() {
        instance = this;
        this.configManager = new ConfigManager(this);
        this.worldManager = new WorldManager(this);
        this.gameManager = new GameManager();

        configManager.loadAll();
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
        pm.registerEvents(new com.yourname.villagerwar.listener.GUIListener(this), this);
    }

    public static VillagerWar getInstance() { return instance; }
    public GameManager getGameManager() { return gameManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public WorldManager getWorldManager() { return worldManager; }
}