package dev.vexvol.minigames.service;

import dev.vexvol.minigames.model.StaffRole;
import dev.vexvol.minigames.util.DurationParser;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class NicknameService {
    public record NicknameData(String displayName, long expiresAt) {
        public boolean permanent() { return expiresAt <= 0; }
        public boolean expired() { return !permanent() && expiresAt <= System.currentTimeMillis(); }
    }

    private final JavaPlugin plugin;
    private final PermissionService permissions;
    private final LocalizationService locale;
    private final File file;
    private final ConcurrentHashMap<UUID, NicknameData> nicknames = new ConcurrentHashMap<>();
    private Pattern allowedPattern;

    public NicknameService(JavaPlugin plugin, PermissionService permissions, LocalizationService locale) {
        this.plugin = plugin;
        this.permissions = permissions;
        this.locale = locale;
        this.file = new File(plugin.getDataFolder(), "profiles.yml");
        reload();
    }

    public void reload() {
        allowedPattern = Pattern.compile(plugin.getConfig().getString("nickname.allowed-pattern", "^[A-Za-z0-9_]{3,16}$"));
        nicknames.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("profiles");
        if (section == null) return;
        for (String uuidText : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidText);
                String name = section.getString(uuidText + ".display-name");
                long expires = section.getLong(uuidText + ".expires", 0L);
                if (name != null && !name.isBlank()) nicknames.put(uuid, new NicknameData(name, expires));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Invalid UUID in profiles.yml: " + uuidText);
            }
        }
    }

    public boolean canManage(Player player) {
        return permissions.role(player).atLeast(StaffRole.ADMIN);
    }

    public boolean set(Player actor, Player target, String nickname, String duration) {
        if (!canManage(actor)) return false;
        if (!validate(nickname, target.getUniqueId())) return false;
        long expires = parseExpiry(duration);
        if (expires == -1L) return false;
        nicknames.put(target.getUniqueId(), new NicknameData(nickname, expires));
        apply(target, nickname);
        save();
        return true;
    }

    public boolean reset(Player actor, Player target) {
        if (!canManage(actor)) return false;
        nicknames.remove(target.getUniqueId());
        apply(target, target.getName());
        save();
        return true;
    }

    public NicknameData get(UUID uuid) {
        NicknameData data = nicknames.get(uuid);
        if (data != null && data.expired()) {
            nicknames.remove(uuid);
            save();
            return null;
        }
        return data;
    }

    public void apply(Player player) {
        NicknameData data = get(player.getUniqueId());
        apply(player, data == null ? player.getName() : data.displayName());
    }

    private void apply(Player player, String displayName) {
        player.setDisplayName(displayName);
        player.setPlayerListName(displayName);
    }

    private boolean validate(String nickname, UUID target) {
        if (nickname == null) return false;
        String trimmed = nickname.trim();
        int min = plugin.getConfig().getInt("nickname.min-length", 3);
        int max = plugin.getConfig().getInt("nickname.max-length", 16);
        if (trimmed.length() < min || trimmed.length() > max) return false;
        if (!allowedPattern.matcher(trimmed).matches()) return false;

        for (String banned : plugin.getConfig().getStringList("nickname.banned-words")) {
            if (trimmed.toLowerCase(Locale.ROOT).contains(banned.toLowerCase(Locale.ROOT))) return false;
        }
        if (plugin.getConfig().getBoolean("nickname.unique", true)) {
            for (var entry : nicknames.entrySet()) {
                if (entry.getKey().equals(target)) continue;
                if (entry.getValue().expired()) continue;
                if (entry.getValue().displayName().equalsIgnoreCase(trimmed)) return false;
            }
        }
        return true;
    }

    private long parseExpiry(String duration) {
        if (duration == null || duration.equalsIgnoreCase("permanent")) return 0L;
        long millis = DurationParser.parseMillis(duration);
        return millis < 0 ? -1L : System.currentTimeMillis() + millis;
    }

    public void cleanupExpired(boolean notify) {
        ArrayList<UUID> expired = new ArrayList<>();
        nicknames.forEach((uuid, data) -> { if (data.expired()) expired.add(uuid); });
        for (UUID uuid : expired) {
            nicknames.remove(uuid);
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                apply(player, player.getName());
                if (notify) player.sendMessage(locale.message("RU", "nick.expired"));
            }
        }
        if (!expired.isEmpty()) save();
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (var entry : nicknames.entrySet()) {
            yaml.set("profiles." + entry.getKey() + ".display-name", entry.getValue().displayName());
            yaml.set("profiles." + entry.getKey() + ".expires", entry.getValue().expiresAt());
        }
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save profiles.yml: " + exception.getMessage());
        }
    }
}
