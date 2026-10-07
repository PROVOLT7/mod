package dev.vexvol.minigames.service;

import org.bukkit.Location;
import org.bukkit.Material;

public final class BlockStatePlatform {
    private BlockStatePlatform() {}
    public static void ensure(Location spawn) {
        if (spawn == null || spawn.getWorld() == null) return;
        spawn.getWorld().getBlockAt(spawn.getBlockX(), spawn.getBlockY() - 1, spawn.getBlockZ()).setType(Material.BEDROCK, false);
    }
}
