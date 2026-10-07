package dev.vexvol.minigames.service;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FlyService {
    private final Map<UUID, Double> speeds = new ConcurrentHashMap<>();

    public double get(Player player) { return speeds.getOrDefault(player.getUniqueId(), 1.0); }

    public void set(Player player, double speed) {
        double value = Math.max(0.001, Math.min(10.0, speed));
        speeds.put(player.getUniqueId(), value);
        // Bukkit accepts -1..1. 1.0 in our command scale is the vanilla default 0.1.
        player.setFlySpeed((float) Math.min(1.0, Math.max(0.0001, value / 10.0)));
    }

    public void reset(Player player) {
        speeds.remove(player.getUniqueId());
        player.setFlySpeed(0.1f);
    }
}
