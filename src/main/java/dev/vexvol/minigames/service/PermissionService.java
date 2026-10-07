package dev.vexvol.minigames.service;

import dev.vexvol.minigames.model.StaffRole;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class PermissionService {
    private final JavaPlugin plugin;

    public PermissionService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean has(CommandSender sender, String permission) {
        if (sender.hasPermission(permission) || sender.hasPermission("vex.*")) return true;
        int dot = permission.lastIndexOf('.');
        while (dot > 0) {
            if (sender.hasPermission(permission.substring(0, dot) + ".*")) return true;
            dot = permission.lastIndexOf('.', dot - 1);
        }
        return false;
    }

    public StaffRole role(Player player) {
        if (player.hasPermission("vex.*") || player.hasPermission(plugin.getConfig().getString("staff.owner-permission", "vex.staff.owner"))) {
            return StaffRole.OWNER;
        }
        if (player.hasPermission(plugin.getConfig().getString("staff.admin-permission", "vex.staff.admin"))) {
            return StaffRole.ADMIN;
        }
        if (player.hasPermission(plugin.getConfig().getString("staff.moderator-permission", "vex.staff.moderator"))) {
            return StaffRole.MODERATOR;
        }
        if (player.hasPermission(plugin.getConfig().getString("staff.builder-permission", "vex.staff.builder"))) {
            return StaffRole.BUILDER;
        }
        return player.hasPermission("vex.staff.sponsor") ? StaffRole.SPONSOR : StaffRole.DEFAULT;
    }

    public boolean canTarget(Player actor, Player target) {
        return role(actor).level() > role(target).level();
    }

    public boolean canUseNickname(Player player) {
        return role(player).atLeast(StaffRole.ADMIN);
    }
}
