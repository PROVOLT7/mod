package dev.vexvol.minigames.service;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class LocalizationService {
    private final JavaPlugin plugin;
    private final Map<String, YamlConfiguration> locales = new HashMap<>();

    public LocalizationService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        File dir = new File(plugin.getDataFolder(), "locales");
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().warning("Could not create locales directory: " + dir);
        }
        ensureResource("locales/ru.yml");
        ensureResource("locales/en.yml");
        locales.clear();
        for (String language : new String[]{"ru", "en"}) {
            File file = new File(dir, language + ".yml");
            locales.put(language.toUpperCase(), YamlConfiguration.loadConfiguration(file));
        }
    }

    private void ensureResource(String path) {
        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) plugin.saveResource(path, false);
    }

    public String text(String language, String key, Map<String, String> placeholders) {
        String lang = language == null ? "EN" : language.toUpperCase();
        YamlConfiguration locale = locales.getOrDefault(lang, locales.get("EN"));
        String value = locale == null ? key : locale.getString(key, key);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                value = value.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    public String prefix(String language) {
        return text(language, "prefix", Map.of());
    }

    public String message(String language, String key) {
        return prefix(language) + text(language, key, Map.of());
    }

    public String message(String language, String key, Map<String, String> placeholders) {
        return prefix(language) + text(language, key, placeholders);
    }
}
