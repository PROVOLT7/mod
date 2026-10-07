package dev.vexvol.minigames.util;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CommandUtil {
    private CommandUtil() {}

    public static Player player(CommandSender sender) {
        return sender instanceof Player p ? p : null;
    }

    public static Player findPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        return Bukkit.getPlayer(name);
    }

    public static GameMode gameMode(String value) {
        try { return GameMode.valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    public static String join(String[] args, int start) {
        if (start >= args.length) return "";
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) builder.append(' ');
            builder.append(args[i]);
        }
        return builder.toString();
    }

    public static String normalize(String value) {
        return value.toLowerCase(java.util.Locale.ROOT);
    }
}
