package com.yourname.villagerwar;

import org.bukkit.entity.Player;

import java.util.*;

public class GameManager {
    private final Map<UUID, Game> games = new HashMap<>();
    private final Map<UUID, UUID> playerGameMap = new HashMap<>();

    public Game createGame(String gameName, com.yourname.villagerwar.world.GameWorld gameWorld,
                           com.yourname.villagerwar.config.rule.GameRule gameRule) {
        Game game = new Game(gameName, gameWorld, gameRule);
        games.put(game.getGameId(), game);
        game.getController().start();
        return game;
    }

    public void removeGame(UUID gameId) {
        Game game = games.get(gameId);
        if (game != null) {
            game.getController().destroy();
            for (GamePlayer gp : game.getPlayers()) {
                playerGameMap.remove(gp.getUuid());
            }
            games.remove(gameId);
        }
    }

    public Optional<Game> getGame(UUID gameId) {
        return Optional.ofNullable(games.get(gameId));
    }

    public Optional<Game> getGame(Player player) {
        return getGame(player.getUniqueId());
    }

    public Optional<Game> getGameByPlayer(UUID playerUuid) {
        UUID gameId = playerGameMap.get(playerUuid);
        return gameId != null ? Optional.ofNullable(games.get(gameId)) : Optional.empty();
    }

    public Collection<Game> getGames() {
        return games.values();
    }

    public void joinGame(Player player, Game game) {
        if (playerGameMap.containsKey(player.getUniqueId())) return;
        GamePlayer gp = new GamePlayer(player.getUniqueId());
        game.addPlayer(gp);
        playerGameMap.put(player.getUniqueId(), game.getGameId());
    }

    public void leaveGame(Player player) {
        UUID playerUuid = player.getUniqueId();
        UUID gameId = playerGameMap.remove(playerUuid);
        if (gameId != null) {
            Game game = games.get(gameId);
            if (game != null) {
                GamePlayer gp = game.getPlayer(playerUuid);
                if (gp != null) game.removePlayer(gp);
                if (game.getPlayerCount() == 0) {
                    removeGame(gameId);
                }
            }
        }
    }

    public boolean isInGame(Player player) {
        return playerGameMap.containsKey(player.getUniqueId());
    }
}