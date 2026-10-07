package dev.vexvol.minigames.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class MainLobbyService {
    private final JavaPlugin plugin;

    public MainLobbyService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public Location getLocation() {
        String worldName = plugin.getConfig().getString("main-lobby.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world,
            plugin.getConfig().getDouble("main-lobby.x", 0.5),
            plugin.getConfig().getDouble("main-lobby.y", 100.0),
            plugin.getConfig().getDouble("main-lobby.z", 0.5),
            (float) plugin.getConfig().getDouble("main-lobby.yaw", 0.0),
            (float) plugin.getConfig().getDouble("main-lobby.pitch", 0.0));
    }

    public boolean teleport(Player player) {
        Location location = getLocation();
        return location != null && player.teleport(location);
    }
}
