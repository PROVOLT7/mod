package dev.vexvol.minigames.service;

import dev.vexvol.minigames.model.MinigameArena;
import dev.vexvol.minigames.model.MinigameMode;
import dev.vexvol.minigames.model.MinigameTeam;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class MinigameService {
    public static final String[] TEAM_COLORS = {
        "RED", "BLUE", "GREEN", "YELLOW", "AQUA", "LIGHT_PURPLE",
        "GOLD", "DARK_RED", "DARK_BLUE", "DARK_GREEN", "DARK_PURPLE", "WHITE"
    };

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, MinigameArena> arenas = new LinkedHashMap<>();

    public MinigameService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "minigames.yml");
    }

    public void load() {
        arenas.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("minigames");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            MinigameMode mode = MinigameMode.parse(root.getString(id + ".mode", "bedrockbox"));
            if (mode == null) continue;
            MinigameArena arena = new MinigameArena(id, mode);
            arena.setEnabled(root.getBoolean(id + ".enabled", false));
            arena.setLobbyPos1(readLocation(root, id + ".lobby.pos1"));
            arena.setLobbyPos2(readLocation(root, id + ".lobby.pos2"));
            arena.setLobbySpawn(readLocation(root, id + ".lobby.spawn"));
            arena.setLobbyCleaning(root.getBoolean(id + ".lobby.cleaning", true));
            arena.setLobbyPlayStartSeconds(root.getInt(id + ".lobby.play-start-seconds", 10));
            arena.setBarrierPos1(readLocation(root, id + ".barrier.pos1"));
            arena.setBarrierPos2(readLocation(root, id + ".barrier.pos2"));
            arena.setShopBox(readLocation(root, id + ".bedrockbox.shop-box.location"));
            arena.setShopBoxType(root.getString(id + ".bedrockbox.shop-box.type"));
            arena.setOneBlockItemIntervalSeconds(root.getInt(id + ".oneblock.item.interval-seconds", 1));
            arena.setOneBlockItemMinAmount(root.getInt(id + ".oneblock.item.min-amount", 1));
            arena.setOneBlockItemMaxAmount(root.getInt(id + ".oneblock.item.max-amount", 1));
            arena.setOneBlockBarrierInitialSize(root.getDouble(id + ".oneblock.barrier.initial-size", 128.0));
            arena.setOneBlockBarrierDelaySeconds(root.getInt(id + ".oneblock.barrier.delay-seconds", 1200));
            arena.setOneBlockBarrierShrinkSeconds(root.getInt(id + ".oneblock.barrier.shrink-seconds", 600));

            ConfigurationSection teams = root.getConfigurationSection(id + ".teams");
            if (teams != null) {
                for (String key : teams.getKeys(false)) {
                    String colorText = teams.getString(key + ".color", "WHITE");
                    ChatColor color = parseColor(colorText);
                    if (color == null) color = ChatColor.WHITE;
                    String name = teams.getString(key + ".name", key);
                    arena.addTeam(name, color);
                    MinigameTeam team = arena.team(name);
                    team.setBed(readLocation(teams, key + ".bed"));
                    team.setSpawn(readLocation(teams, key + ".spawn"));
                    team.setShop(readLocation(teams, key + ".shop.location"));
                    team.setShopType(teams.getString(key + ".shop.type"));
                }
            }
            arenas.put(id.toLowerCase(Locale.ROOT), arena);
        }
    }

    public MinigameArena get(String id) { return id == null ? null : arenas.get(id.toLowerCase(Locale.ROOT)); }
    public Collection<MinigameArena> all() { return arenas.values(); }
    public boolean create(String id, MinigameMode mode) {
        if (id == null || id.isBlank() || mode == null || arenas.containsKey(id.toLowerCase(Locale.ROOT))) return false;
        arenas.put(id.toLowerCase(Locale.ROOT), new MinigameArena(id, mode));
        save();
        return true;
    }
    public boolean delete(String id) {
        if (id == null || arenas.remove(id.toLowerCase(Locale.ROOT)) == null) return false;
        save();
        return true;
    }
    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (MinigameArena arena : arenas.values()) {
            String p = "minigames." + arena.id();
            yaml.set(p + ".mode", arena.mode().id());
            yaml.set(p + ".enabled", arena.enabled());
            writeLocation(yaml, p + ".lobby.pos1", arena.lobbyPos1());
            writeLocation(yaml, p + ".lobby.pos2", arena.lobbyPos2());
            writeLocation(yaml, p + ".lobby.spawn", arena.lobbySpawn());
            yaml.set(p + ".lobby.cleaning", arena.lobbyCleaning());
            yaml.set(p + ".lobby.play-start-seconds", arena.lobbyPlayStartSeconds());
            writeLocation(yaml, p + ".barrier.pos1", arena.barrierPos1());
            writeLocation(yaml, p + ".barrier.pos2", arena.barrierPos2());
            yaml.set(p + ".bedrockbox.shop-box.type", arena.shopBoxType());
            writeLocation(yaml, p + ".bedrockbox.shop-box.location", arena.shopBox());
            yaml.set(p + ".oneblock.item.interval-seconds", arena.oneBlockItemIntervalSeconds());
            yaml.set(p + ".oneblock.item.min-amount", arena.oneBlockItemMinAmount());
            yaml.set(p + ".oneblock.item.max-amount", arena.oneBlockItemMaxAmount());
            yaml.set(p + ".oneblock.barrier.initial-size", arena.oneBlockBarrierInitialSize());
            yaml.set(p + ".oneblock.barrier.delay-seconds", arena.oneBlockBarrierDelaySeconds());
            yaml.set(p + ".oneblock.barrier.shrink-seconds", arena.oneBlockBarrierShrinkSeconds());
            for (MinigameTeam team : arena.teams().values()) {
                String tp = p + ".teams." + team.name();
                yaml.set(tp + ".name", team.name());
                yaml.set(tp + ".color", team.color().name());
                writeLocation(yaml, tp + ".bed", team.bed());
                writeLocation(yaml, tp + ".spawn", team.spawn());
                yaml.set(tp + ".shop.type", team.shopType());
                writeLocation(yaml, tp + ".shop.location", team.shop());
            }
        }
        try { yaml.save(file); }
        catch (IOException e) { plugin.getLogger().severe("Could not save minigames.yml: " + e.getMessage()); }
    }

    public boolean setEnabled(String id, boolean value) {
        MinigameArena arena = get(id); if (arena == null) return false;
        arena.setEnabled(value); save(); return true;
    }
    public boolean setLobbyPos(String id, int point, Location location) { MinigameArena a = get(id); if (a == null) return false; if (point == 1) a.setLobbyPos1(location); else a.setLobbyPos2(location); save(); return true; }
    public boolean setLobbySpawn(String id, Location location) { MinigameArena a = get(id); if (a == null) return false; a.setLobbySpawn(location); save(); return true; }
    public boolean setLobbyCleaning(String id, boolean value) { MinigameArena a = get(id); if (a == null) return false; a.setLobbyCleaning(value); save(); return true; }
    public boolean setLobbyPlayStart(String id, int seconds) { MinigameArena a = get(id); if (a == null) return false; a.setLobbyPlayStartSeconds(seconds); save(); return true; }
    public boolean setBarrierPos(String id, int point, Location location) { MinigameArena a = get(id); if (a == null) return false; if (point == 1) a.setBarrierPos1(location); else a.setBarrierPos2(location); save(); return true; }
    public boolean setShopBox(String id, String mobType, Location location) { MinigameArena a = get(id); if (a == null || a.mode() != MinigameMode.BEDROCKBOX) return false; if (!validMob(mobType)) return false; a.setShopBoxType(mobType.toUpperCase(Locale.ROOT)); a.setShopBox(location); save(); return true; }
    public boolean setTeamShop(String id, String teamName, String mobType, Location location) { MinigameArena a = get(id); if (a == null) return false; if (!validMob(mobType)) return false; MinigameTeam t = a.team(teamName); if (t == null) return false; t.setShopType(mobType.toUpperCase(Locale.ROOT)); t.setShop(location); save(); return true; }
    public boolean setTeamBed(String id, String teamName, Location location) { MinigameArena a = get(id); if (a == null) return false; MinigameTeam t = a.team(teamName); if (t == null) return false; t.setBed(location); save(); return true; }
    public boolean setTeamSpawn(String id, String teamName, Location location) { MinigameArena a = get(id); if (a == null) return false; MinigameTeam t = a.team(teamName); if (t == null) return false; t.setSpawn(location); save(); return true; }
    public boolean setTeamColor(String id, String teamName, ChatColor color) { MinigameArena a = get(id); if (a == null || color == null) return false; MinigameTeam t = a.team(teamName); if (t == null) return false; t.setColor(color); save(); return true; }
    public void setMode(String id, MinigameMode mode) { MinigameArena a = get(id); if (a != null && mode != null) { a.setMode(mode); save(); } }
    public boolean setOneBlockInterval(String id, int seconds) { MinigameArena a=get(id); if(a==null || a.mode()!=MinigameMode.ONEBLOCK)return false; a.setOneBlockItemIntervalSeconds(seconds); save(); return true; }
    public boolean setOneBlockAmount(String id, int min, int max) { MinigameArena a=get(id); if(a==null || a.mode()!=MinigameMode.ONEBLOCK)return false; a.setOneBlockItemMinAmount(min); a.setOneBlockItemMaxAmount(max); save(); return true; }
    public boolean setOneBlockBarrier(String id, double initial, int delay, int shrink) { MinigameArena a=get(id); if(a==null || a.mode()!=MinigameMode.ONEBLOCK)return false; a.setOneBlockBarrierInitialSize(initial); a.setOneBlockBarrierDelaySeconds(delay); a.setOneBlockBarrierShrinkSeconds(shrink); save(); return true; }

    public static ChatColor parseColor(String raw) {
        if (raw == null) return null;
        try {
            ChatColor color = ChatColor.valueOf(raw.toUpperCase(Locale.ROOT));
            for (String allowed : TEAM_COLORS) if (allowed.equals(color.name())) return color;
        } catch (IllegalArgumentException ignored) { }
        return null;
    }

    public static String colorNameList() { return String.join(", ", TEAM_COLORS); }

    private boolean validMob(String raw) {
        if (raw == null) return false;
        try {
            EntityType type = EntityType.valueOf(raw.toUpperCase(Locale.ROOT));
            return type.isAlive() && type != EntityType.PLAYER;
        } catch (IllegalArgumentException e) { return false; }
    }
    private Location readLocation(ConfigurationSection section, String path) {
        String worldName = section.getString(path + ".world"); if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName); if (world == null) return null;
        return new Location(world, section.getDouble(path+".x"), section.getDouble(path+".y"), section.getDouble(path+".z"), (float)section.getDouble(path+".yaw"), (float)section.getDouble(path+".pitch"));
    }
    private void writeLocation(YamlConfiguration yaml, String path, Location location) {
        if (location == null || location.getWorld() == null) return;
        yaml.set(path+".world", location.getWorld().getName()); yaml.set(path+".x", location.getX()); yaml.set(path+".y", location.getY()); yaml.set(path+".z", location.getZ()); yaml.set(path+".yaw", location.getYaw()); yaml.set(path+".pitch", location.getPitch());
    }
}
