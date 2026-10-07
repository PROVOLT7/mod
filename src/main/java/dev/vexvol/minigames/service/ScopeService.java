package dev.vexvol.minigames.service;

import dev.vexvol.minigames.model.ArenaScope;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class ScopeService {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Map<String, ArenaScope>> scopes = new HashMap<>();

    public ScopeService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "scopes.yml");
    }

    public void load() {
        scopes.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) return;
        for (String uuidText : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidText);
                ConfigurationSection playerSection = players.getConfigurationSection(uuidText);
                if (playerSection == null) continue;
                for (String arenaId : playerSection.getKeys(false)) {
                    long expires = playerSection.getLong(arenaId + ".expires", 0L);
                    scopes.computeIfAbsent(uuid, ignored -> new HashMap<>())
                        .put(arenaId, new ArenaScope(uuid, arenaId, expires));
                }
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Invalid UUID in scopes.yml: " + uuidText);
            }
        }
        cleanupExpired();
    }

    public boolean hasScope(Player player, String arenaId) {
        Map<String, ArenaScope> playerScopes = scopes.get(player.getUniqueId());
        if (playerScopes == null) return false;
        ArenaScope scope = playerScopes.get(arenaId);
        if (scope == null) return false;
        if (scope.expired(System.currentTimeMillis())) {
            playerScopes.remove(arenaId);
            save();
            return false;
        }
        return true;
    }

    public void grant(UUID playerId, String arenaId, long expiresAt) {
        scopes.computeIfAbsent(playerId, ignored -> new HashMap<>())
            .put(arenaId, new ArenaScope(playerId, arenaId, expiresAt));
        save();
    }

    public boolean revoke(UUID playerId, String arenaId) {
        Map<String, ArenaScope> playerScopes = scopes.get(playerId);
        if (playerScopes == null) return false;
        boolean removed = playerScopes.remove(arenaId) != null;
        if (playerScopes.isEmpty()) scopes.remove(playerId);
        if (removed) save();
        return removed;
    }

    public List<ArenaScope> list(UUID playerId) {
        cleanupExpired();
        return new ArrayList<>(scopes.getOrDefault(playerId, Map.of()).values());
    }

    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        boolean changed = false;
        Iterator<Map.Entry<UUID, Map<String, ArenaScope>>> playerIt = scopes.entrySet().iterator();
        while (playerIt.hasNext()) {
            Map<String, ArenaScope> values = playerIt.next().getValue();
            Iterator<Map.Entry<String, ArenaScope>> scopeIt = values.entrySet().iterator();
            while (scopeIt.hasNext()) {
                if (scopeIt.next().getValue().expired(now)) {
                    scopeIt.remove();
                    changed = true;
                }
            }
            if (values.isEmpty()) playerIt.remove();
        }
        if (changed) save();
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, ArenaScope>> playerEntry : scopes.entrySet()) {
            for (ArenaScope scope : playerEntry.getValue().values()) {
                String path = "players." + playerEntry.getKey() + "." + scope.arenaId() + ".expires";
                yaml.set(path, scope.expiresAtEpochMillis());
            }
        }
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save scopes.yml: " + exception.getMessage());
        }
    }

    public long parseExpiry(String duration) {
        if (duration == null || duration.equalsIgnoreCase("permanent")) return 0L;
        long millis = dev.vexvol.minigames.util.DurationParser.parseMillis(duration);
        return millis < 0 ? -1L : System.currentTimeMillis() + millis;
    }
}
