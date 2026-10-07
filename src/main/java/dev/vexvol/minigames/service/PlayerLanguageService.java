package dev.vexvol.minigames.service;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerLanguageService {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, String> languages = new ConcurrentHashMap<>();

    public PlayerLanguageService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "languages.yml");
        load();
    }

    public void load() {
        languages.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getConfigurationSection("players") == null ? java.util.List.<String>of() : yaml.getConfigurationSection("players").getKeys(false)) {
            try {
                languages.put(UUID.fromString(key), normalize(yaml.getString("players." + key, "EN")));
            } catch (IllegalArgumentException ignored) { }
        }
    }

    public String get(Player player) {
        if (player == null) return "EN";
        return languages.getOrDefault(player.getUniqueId(), detect(player));
    }

    public String detect(Player player) {
        String locale = player.locale().getLanguage();
        return locale.equalsIgnoreCase("ru") ? "RU" : "EN";
    }

    public void set(Player player, String language) {
        String normalized = normalize(language);
        if (!normalized.equals("RU") && !normalized.equals("EN")) return;
        languages.put(player.getUniqueId(), normalized);
        save();
    }

    public void handleLocaleChange(Player player, PlayerLocaleChangeEvent event) {
        if (!languages.containsKey(player.getUniqueId())) {
            set(player, event.locale().getLanguage().equalsIgnoreCase("ru") ? "RU" : "EN");
        }
    }

    private String normalize(String language) {
        return language == null ? "EN" : language.toUpperCase(java.util.Locale.ROOT);
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (var entry : languages.entrySet()) yaml.set("players." + entry.getKey(), entry.getValue());
        try { yaml.save(file); } catch (IOException e) { plugin.getLogger().warning("Could not save languages.yml: " + e.getMessage()); }
    }
}
