package dev.vexvol.minigames.service;

import dev.vexvol.minigames.model.MapTemplate;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MapService {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, MapTemplate> maps = new LinkedHashMap<>();

    public MapService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "maps.yml");
    }

    public void load() {
        maps.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("maps");
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            maps.put(id, new MapTemplate(
                id,
                section.getString(id + ".game-type", "generic"),
                section.getString(id + ".world", id),
                section.getBoolean(id + ".build-allowed", false)
            ));
        }
    }

    public boolean create(String id, String gameType, String world) {
        if (maps.containsKey(id)) return false;
        maps.put(id, new MapTemplate(id, gameType, world, false));
        save();
        return true;
    }

    public boolean delete(String id) {
        if (maps.remove(id) == null) return false;
        save();
        return true;
    }

    public MapTemplate get(String id) { return maps.get(id); }
    public Collection<MapTemplate> all() { return maps.values(); }

    public void setBuildAllowed(String id, boolean allowed) {
        MapTemplate map = maps.get(id);
        if (map == null) return;
        map.setBuildAllowed(allowed);
        save();
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (MapTemplate map : maps.values()) {
            String path = "maps." + map.id();
            yaml.set(path + ".game-type", map.gameType());
            yaml.set(path + ".world", map.worldName());
            yaml.set(path + ".build-allowed", map.buildAllowed());
        }
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save maps.yml: " + exception.getMessage());
        }
    }
}
