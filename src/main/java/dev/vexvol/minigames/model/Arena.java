package dev.vexvol.minigames.model;

import org.bukkit.Location;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Arena {
    private final String id;
    private String mapTemplateId;
    private String worldName;
    private boolean enabled;
    private Location lobby;
    private Location regionPos1;
    private Location regionPos2;
    private final Map<String, Location> spawns = new LinkedHashMap<>();

    public Arena(String id, String mapTemplateId, String worldName) {
        this.id = id;
        this.mapTemplateId = mapTemplateId;
        this.worldName = worldName;
        this.enabled = false;
    }

    public String id() { return id; }
    public String mapTemplateId() { return mapTemplateId; }
    public String worldName() { return worldName; }
    public boolean enabled() { return enabled; }
    public Location lobby() { return lobby == null ? null : lobby.clone(); }
    public Location regionPos1() { return regionPos1 == null ? null : regionPos1.clone(); }
    public Location regionPos2() { return regionPos2 == null ? null : regionPos2.clone(); }
    public Map<String, Location> spawns() { return Map.copyOf(spawns); }
    public void setMapTemplateId(String id) { this.mapTemplateId = id; }
    public void setWorldName(String name) { this.worldName = name; }
    public void setEnabled(boolean value) { this.enabled = value; }
    public void setLobby(Location location) { this.lobby = location == null ? null : location.clone(); }
    public void setRegionPos1(Location location) { this.regionPos1 = location == null ? null : location.clone(); }
    public void setRegionPos2(Location location) { this.regionPos2 = location == null ? null : location.clone(); }
    public void setSpawn(String team, Location location) { spawns.put(team, location.clone()); }
    public boolean hasRegion() { return regionPos1 != null && regionPos2 != null; }

    public boolean contains(Location location) {
        if (!hasRegion() || location == null || location.getWorld() == null || regionPos1.getWorld() == null) {
            return false;
        }
        if (!location.getWorld().getUID().equals(regionPos1.getWorld().getUID())) return false;
        double minX = Math.min(regionPos1.getX(), regionPos2.getX());
        double maxX = Math.max(regionPos1.getX(), regionPos2.getX());
        double minY = Math.min(regionPos1.getY(), regionPos2.getY());
        double maxY = Math.max(regionPos1.getY(), regionPos2.getY());
        double minZ = Math.min(regionPos1.getZ(), regionPos2.getZ());
        double maxZ = Math.max(regionPos1.getZ(), regionPos2.getZ());
        return location.getX() >= minX && location.getX() <= maxX
            && location.getY() >= minY && location.getY() <= maxY
            && location.getZ() >= minZ && location.getZ() <= maxZ;
    }
}
