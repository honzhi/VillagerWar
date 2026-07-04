package com.yourname.villagerwar;

import com.yourname.villagerwar.controller.GameController;
import com.yourname.villagerwar.manager.*;
import com.yourname.villagerwar.config.rule.GameRule;
import com.yourname.villagerwar.world.GameWorld;

import java.util.ArrayList;
import java.util.List;

import java.util.UUID;

public class Game {
    private final UUID gameId;
    private final String gameName;
    private GameState state;
    private GameWorld gameWorld;
    private final GameRule gameRule;
    private final String mapId;
    private final String modeId;
    private String reservesSeatName;
    private int gameTime;
    private int stateTime;
    private final List<GamePlayer> players;

    private final TeamManager teamManager;
    private final SkillManager skillManager;
    private final SpawnManager spawnManager;
    private final EconomyManager economyManager;
    private final RespawnManager respawnManager;
    private final VictoryManager victoryManager;
    private final UIManager uiManager;

    private final GameController controller;

    public Game(String gameName, String mapId, String modeId, GameRule gameRule) {
        this.gameId = UUID.randomUUID();
        this.gameName = gameName;
        this.mapId = mapId;
        this.modeId = modeId;
        this.state = GameState.PREPARING;
        this.gameWorld = null;
        this.reservesSeatName = null;
        this.gameRule = gameRule;
        this.gameTime = 0;
        this.stateTime = 0;
        this.players = new ArrayList<>();

        this.teamManager = new TeamManager(this);
        this.skillManager = new SkillManager(this);
        this.spawnManager = new SpawnManager(this);
        this.economyManager = new EconomyManager(this);
        this.respawnManager = new RespawnManager(this);
        this.victoryManager = new VictoryManager(this);
        this.uiManager = new UIManager(this);
        this.controller = new GameController(this);
    }

    /**
     * 创建并加载游戏地图世界（延迟加载，在 TELEPORT 阶段调用）
     */
    public boolean createGameWorld() {
        if (gameWorld != null && gameWorld.isLoaded()) return true;
        gameWorld = VillagerWar.getInstance().getWorldManager().createWorld(mapId);
        if (gameWorld == null || !gameWorld.isLoaded()) {
            VillagerWar.getInstance().getLogger().severe("[Debug] Failed to create world for " + mapId);
            return false;
        }
        return true;
    }

    /**
     * 销毁游戏地图世界
     */
    public void destroyGameWorld() {
        if (gameWorld != null) {
            VillagerWar.getInstance().getWorldManager().deleteGameWorld(gameWorld);
            gameWorld = null;
        }
    }

    public void setState(GameState newState) {
        this.state = newState;
        this.stateTime = 0;
        controller.onStateChange(newState);
    }

    public void tick() {
        gameTime++;
        stateTime++;

        // 实时更新状态UI（计分板、标题、操作栏）
        uiManager.tick(gameTime);

        // 仅在游戏中执行游戏逻辑
        if (state != GameState.PLAYING) return;
        spawnManager.tick(gameTime);
        economyManager.tick(gameTime);
        respawnManager.tick(gameTime);
        victoryManager.tick(gameTime);

    }


    public void addPlayer(GamePlayer player) { players.add(player); }
    public void removePlayer(GamePlayer player) { players.remove(player); }
    public GamePlayer getPlayer(UUID uuid) {
        return players.stream().filter(p -> p.getUuid().equals(uuid)).findFirst().orElse(null);
    }

    public UUID getGameId() { return gameId; }
    public String getGameName() { return gameName; }
    public String getMapId() { return mapId; }
    public String getModeId() { return modeId; }
    public GameState getState() { return state; }
    public GameWorld getGameWorld() { return gameWorld; }
    public GameRule getGameRule() { return gameRule; }
    public String getReservesSeatName() { return reservesSeatName; }
    public void setReservesSeatName(String name) { this.reservesSeatName = name; }
    public int getGameTime() { return gameTime; }
    public int getStateTime() { return stateTime; }
    public void resetStateTime() { this.stateTime = 0; }
    public List<GamePlayer> getPlayers() { return players; }
    public int getPlayerCount() { return players.size(); }
    public TeamManager getTeamManager() { return teamManager; }
    public SkillManager getSkillManager() { return skillManager; }
    public SpawnManager getSpawnManager() { return spawnManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public RespawnManager getRespawnManager() { return respawnManager; }
    public VictoryManager getVictoryManager() { return victoryManager; }
    public UIManager getUiManager() { return uiManager; }
    public GameController getController() { return controller; }
}