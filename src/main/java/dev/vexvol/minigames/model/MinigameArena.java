package dev.vexvol.minigames.model;

import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MinigameArena {
    private final String id;
    private MinigameMode mode;
    private boolean enabled;
    private Location lobbyPos1;
    private Location lobbyPos2;
    private Location lobbySpawn;
    private boolean lobbyCleaning = true;
    private int lobbyPlayStartSeconds = 10;
    private Location barrierPos1;
    private Location barrierPos2;
    private Location shopBox;
    private String shopBoxType;
    private int oneBlockItemIntervalSeconds = 1;
    private int oneBlockItemMinAmount = 1;
    private int oneBlockItemMaxAmount = 1;
    private double oneBlockBarrierInitialSize = 128.0;
    private int oneBlockBarrierDelaySeconds = 1200;
    private int oneBlockBarrierShrinkSeconds = 600;
    private final Map<String, MinigameTeam> teams = new LinkedHashMap<>();

    public MinigameArena(String id, MinigameMode mode) {
        this.id = id;
        this.mode = mode;
    }

    public String id() { return id; }
    public MinigameMode mode() { return mode; }
    public boolean enabled() { return enabled; }
    public Location lobbyPos1() { return lobbyPos1 == null ? null : lobbyPos1.clone(); }
    public Location lobbyPos2() { return lobbyPos2 == null ? null : lobbyPos2.clone(); }
    public Location lobbySpawn() { return lobbySpawn == null ? null : lobbySpawn.clone(); }
    public boolean lobbyCleaning() { return lobbyCleaning; }
    public int lobbyPlayStartSeconds() { return lobbyPlayStartSeconds; }
    public Location barrierPos1() { return barrierPos1 == null ? null : barrierPos1.clone(); }
    public Location barrierPos2() { return barrierPos2 == null ? null : barrierPos2.clone(); }
    public Location shopBox() { return shopBox == null ? null : shopBox.clone(); }
    public String shopBoxType() { return shopBoxType; }
    public int oneBlockItemIntervalSeconds() { return oneBlockItemIntervalSeconds; }
    public int oneBlockItemMinAmount() { return oneBlockItemMinAmount; }
    public int oneBlockItemMaxAmount() { return oneBlockItemMaxAmount; }
    public double oneBlockBarrierInitialSize() { return oneBlockBarrierInitialSize; }
    public int oneBlockBarrierDelaySeconds() { return oneBlockBarrierDelaySeconds; }
    public int oneBlockBarrierShrinkSeconds() { return oneBlockBarrierShrinkSeconds; }
    public Map<String, MinigameTeam> teams() { return Map.copyOf(teams); }
    public MinigameTeam team(String name) { return teams.get(name.toLowerCase(java.util.Locale.ROOT)); }
    public void setMode(MinigameMode value) { this.mode = value; }
    public void setEnabled(boolean value) { this.enabled = value; }
    public void setLobbyPos1(Location value) { lobbyPos1 = cloneOrNull(value); }
    public void setLobbyPos2(Location value) { lobbyPos2 = cloneOrNull(value); }
    public void setLobbySpawn(Location value) { lobbySpawn = cloneOrNull(value); }
    public void setLobbyCleaning(boolean value) { lobbyCleaning = value; }
    public void setLobbyPlayStartSeconds(int value) { lobbyPlayStartSeconds = Math.max(1, value); }
    public void setBarrierPos1(Location value) { barrierPos1 = cloneOrNull(value); }
    public void setBarrierPos2(Location value) { barrierPos2 = cloneOrNull(value); }
    public void setShopBox(Location value) { shopBox = cloneOrNull(value); }
    public void setShopBoxType(String value) { shopBoxType = value; }
    public void setOneBlockItemIntervalSeconds(int value) { oneBlockItemIntervalSeconds = Math.max(1, value); }
    public void setOneBlockItemMinAmount(int value) { oneBlockItemMinAmount = Math.max(1, value); }
    public void setOneBlockItemMaxAmount(int value) { oneBlockItemMaxAmount = Math.max(oneBlockItemMinAmount, value); }
    public void setOneBlockBarrierInitialSize(double value) { oneBlockBarrierInitialSize = Math.max(1.0, value); }
    public void setOneBlockBarrierDelaySeconds(int value) { oneBlockBarrierDelaySeconds = Math.max(0, value); }
    public void setOneBlockBarrierShrinkSeconds(int value) { oneBlockBarrierShrinkSeconds = Math.max(1, value); }

    public boolean addTeam(String name, ChatColor color) {
        String key = name.toLowerCase(java.util.Locale.ROOT);
        if (teams.containsKey(key)) return false;
        teams.put(key, new MinigameTeam(name, color));
        return true;
    }
    public boolean removeTeam(String name) { return teams.remove(name.toLowerCase(java.util.Locale.ROOT)) != null; }

    private Location cloneOrNull(Location location) { return location == null ? null : location.clone(); }
}
