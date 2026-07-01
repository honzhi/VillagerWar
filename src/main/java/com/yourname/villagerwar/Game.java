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
    private final GameWorld gameWorld;
    private final GameRule gameRule;
    private int gameTime;
    private final List<GamePlayer> players;

    private final TeamManager teamManager;
    private final SkillManager skillManager;
    private final SpawnManager spawnManager;
    private final EconomyManager economyManager;
    private final RespawnManager respawnManager;
    private final VictoryManager victoryManager;
    private final UIManager uiManager;

    private final GameController controller;

    public Game(String gameName, GameWorld gameWorld, GameRule gameRule) {
        this.gameId = UUID.randomUUID();
        this.gameName = gameName;
        this.state = GameState.WAITING;
        this.gameWorld = gameWorld;
        this.gameRule = gameRule;
        this.gameTime = 0;
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

    public void setState(GameState newState) {
        this.state = newState;
        controller.onStateChange(newState);
    }

    public void tick() {
        if (state != GameState.PLAYING) return;
        gameTime++;
        spawnManager.tick(gameTime);
        economyManager.tick(gameTime);
        respawnManager.tick(gameTime);
        victoryManager.tick(gameTime);
        uiManager.tick(gameTime);
    }

    public void addPlayer(GamePlayer player) { players.add(player); }
    public void removePlayer(GamePlayer player) { players.remove(player); }
    public GamePlayer getPlayer(UUID uuid) {
        return players.stream().filter(p -> p.getUuid().equals(uuid)).findFirst().orElse(null);
    }

    public UUID getGameId() { return gameId; }
    public String getGameName() { return gameName; }
    public GameState getState() { return state; }
    public GameWorld getGameWorld() { return gameWorld; }
    public GameRule getGameRule() { return gameRule; }
    public int getGameTime() { return gameTime; }
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
