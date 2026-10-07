package dev.vexvol.minigames.service;

import dev.vexvol.minigames.model.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ArenaService {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();

    public ArenaService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
    }

    public void load() {
        arenas.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("arenas");
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            Arena arena = new Arena(id,
                section.getString(id + ".map-template", id),
                section.getString(id + ".world", id));
            arena.setEnabled(section.getBoolean(id + ".enabled", false));
            arena.setLobby(readLocation(section, id + ".lobby"));
            arena.setRegionPos1(readLocation(section, id + ".region.pos1"));
            arena.setRegionPos2(readLocation(section, id + ".region.pos2"));
            ConfigurationSection spawns = section.getConfigurationSection(id + ".spawns");
            if (spawns != null) {
                for (String team : spawns.getKeys(false)) {
                    Location location = readLocation(spawns, team);
                    if (location != null) arena.setSpawn(team, location);
                }
            }
            arenas.put(id, arena);
        }
    }

    public boolean create(String id, String mapTemplateId, String worldName) {
        if (arenas.containsKey(id)) return false;
        arenas.put(id, new Arena(id, mapTemplateId, worldName));
        save();
        return true;
    }

    public Arena get(String id) { return arenas.get(id); }
    public Collection<Arena> all() { return arenas.values(); }

    public boolean delete(String id) {
        if (arenas.remove(id) == null) return false;
        save();
        return true;
    }

    public void setSpawn(String arenaId, String team, Location location) {
        Arena arena = arenas.get(arenaId);
        if (arena == null) return;
        arena.setSpawn(team, location);
        save();
    }

    public void setLobby(String arenaId, Location location) {
        Arena arena = arenas.get(arenaId);
        if (arena == null) return;
        arena.setLobby(location);
        save();
    }

    public void setRegion(String arenaId, int point, Location location) {
        Arena arena = arenas.get(arenaId);
        if (arena == null) return;
        if (point == 1) arena.setRegionPos1(location); else arena.setRegionPos2(location);
        save();
    }

    public void setEnabled(String arenaId, boolean enabled) {
        Arena arena = arenas.get(arenaId);
        if (arena == null) return;
        arena.setEnabled(enabled);
        save();
    }

    public void reset(String arenaId) {
        Arena arena = arenas.get(arenaId);
        if (arena == null) return;
        arena.setEnabled(false);
        arena.setLobby(null);
        arena.setRegionPos1(null);
        arena.setRegionPos2(null);
        save();
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Arena arena : arenas.values()) {
            String path = "arenas." + arena.id();
            yaml.set(path + ".map-template", arena.mapTemplateId());
            yaml.set(path + ".world", arena.worldName());
            yaml.set(path + ".enabled", arena.enabled());
            writeLocation(yaml, path + ".lobby", arena.lobby());
            writeLocation(yaml, path + ".region.pos1", arena.regionPos1());
            writeLocation(yaml, path + ".region.pos2", arena.regionPos2());
            for (Map.Entry<String, Location> entry : arena.spawns().entrySet()) {
                writeLocation(yaml, path + ".spawns." + entry.getKey(), entry.getValue());
            }
        }
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save arenas.yml: " + exception.getMessage());
        }
    }

    private Location readLocation(ConfigurationSection section, String path) {
        String worldName = section.getString(path + ".world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world,
            section.getDouble(path + ".x"),
            section.getDouble(path + ".y"),
            section.getDouble(path + ".z"),
            (float) section.getDouble(path + ".yaw"),
            (float) section.getDouble(path + ".pitch"));
    }

    private void writeLocation(YamlConfiguration yaml, String path, Location location) {
        if (location == null || location.getWorld() == null) return;
        yaml.set(path + ".world", location.getWorld().getName());
        yaml.set(path + ".x", location.getX());
        yaml.set(path + ".y", location.getY());
        yaml.set(path + ".z", location.getZ());
        yaml.set(path + ".yaw", location.getYaw());
        yaml.set(path + ".pitch", location.getPitch());
    }
}
