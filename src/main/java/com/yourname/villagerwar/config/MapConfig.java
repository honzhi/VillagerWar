package com.yourname.villagerwar.config;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class MapConfig {

    private final String id;
    private final String displayName;
    private final String worldName;
    private final int minPlayers;
    private final int maxPlayers;
    private final List<String> allowedModes;
    private final Map<String, Location> spawnPoints;
    private final Map<String, Location> teamSpawns;
    private final Location spectatorSpawn;
    private final Location lobbySpawn;

    public MapConfig(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.id = file.getParentFile().getName();
        this.displayName = config.getString("display-name", id);
        this.worldName = config.getString("world", id);
        this.minPlayers = config.getInt("min-players", 2);
        this.maxPlayers = config.getInt("max-players", 40);
        this.allowedModes = config.getStringList("allowed-modes");
        this.spawnPoints = parseLocationMap(config.getConfigurationSection("spawn-points"));
        this.teamSpawns = parseLocationMap(config.getConfigurationSection("team-spawns"));
        this.spectatorSpawn = parseLocation(config.getConfigurationSection("spectator-spawn"));
        this.lobbySpawn = parseLocation(config.getConfigurationSection("lobby-spawn"));
    }

    private Map<String, Location> parseLocationMap(ConfigurationSection section) {
        Map<String, Location> map = new LinkedHashMap<>();
        if (section == null) return map;
        for (String key : section.getKeys(false)) {
            Location loc = parseLocation(section.getConfigurationSection(key));
            if (loc != null) {
                map.put(key, loc);
            }
        }
        return map;
    }

    private Location parseLocation(ConfigurationSection section) {
        if (section == null) return null;
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0);
        float pitch = (float) section.getDouble("pitch", 0);
        return new Location(null, x, y, z, yaw, pitch);
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getWorldName() { return worldName; }
    public int getMinPlayers() { return minPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public List<String> getAllowedModes() { return Collections.unmodifiableList(allowedModes); }
    public Map<String, Location> getSpawnPoints() { return Collections.unmodifiableMap(spawnPoints); }
    public Map<String, Location> getTeamSpawns() { return Collections.unmodifiableMap(teamSpawns); }
    public Location getSpectatorSpawn() { return spectatorSpawn; }
    public Location getLobbySpawn() { return lobbySpawn; }
}
