package dev.vexvol.minigames.command;

import dev.vexvol.minigames.VexVolMinigamesPlugin;
import dev.vexvol.minigames.util.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public final class SimpleCommandExecutor implements CommandExecutor, TabCompleter {
    private final VexVolMinigamesPlugin plugin;

    public SimpleCommandExecutor(VexVolMinigamesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        String permission = switch (name) {
            case "fly" -> "vex.fly";
            case "flyspeed" -> "vex.fly.speed";
            case "day" -> "vex.server.day";
            case "night" -> "vex.server.night";
            case "weather" -> "vex.server.weather";
            case "tp" -> "vex.player.teleport";
            case "tphere" -> "vex.player.tphere";
            case "gm" -> "vex.gamemode";
            case "spawn" -> "vex.spawn";
            case "kick" -> "vex.kick";
            case "heal" -> "vex.heal";
            case "feed" -> "vex.feed";
            case "vban" -> "vex.moderation.ban";
            case "vunban" -> "vex.moderation.unban";
            case "mute" -> "vex.moderation.mute";
            case "unmute" -> "vex.moderation.unmute";
            default -> "vex.use";
        };
        boolean adminFlightOverride = (name.equals("fly") || name.equals("flyspeed"))
                && sender instanceof Player actor
                && plugin.permissions().role(actor).atLeast(dev.vexvol.minigames.model.StaffRole.ADMIN);
        if (!plugin.permissions().has(sender, permission) && !adminFlightOverride) {
            sender.sendMessage(plugin.msg(sender, "generic.no_permission"));
            return true;
        }

        return switch (name) {
            case "fly" -> fly(sender, args);
            case "flyspeed" -> flySpeed(sender, args);
            case "day" -> setTime(sender, 1000L);
            case "night" -> setTime(sender, 13000L);
            case "weather" -> weather(sender, args);
            case "tp" -> tp(sender, args);
            case "tphere" -> tpHere(sender, args);
            case "gm" -> gameMode(sender, args);
            case "spawn" -> spawn(sender);
            case "kick" -> kick(sender, args);
            case "heal" -> heal(sender, args);
            case "feed" -> feed(sender, args);
            case "vban" -> external(sender, args, "ban");
            case "vunban" -> external(sender, args, "unban");
            case "mute" -> external(sender, args, "mute");
            case "unmute" -> external(sender, args, "unmute");
            default -> true;
        };
    }

    private boolean fly(CommandSender sender, String[] args) {
        Player target = args.length >= 1 ? CommandUtil.findPlayer(args[0]) : CommandUtil.player(sender);
        if (target == null) { sender.sendMessage("§cPlayer not found."); return true; }
        if (!target.equals(sender) && !plugin.permissions().has(sender, "vex.fly.others") && !(sender instanceof Player actor && plugin.permissions().role(actor).atLeast(dev.vexvol.minigames.model.StaffRole.ADMIN))) { sender.sendMessage(plugin.msg(sender, "generic.no_permission")); return true; }
        boolean state;
        if (args.length < 2) state = !target.getAllowFlight();
        else if (args[1].equalsIgnoreCase("true")) state = true;
        else if (args[1].equalsIgnoreCase("false")) state = false;
        else { sender.sendMessage("§cUsage: /fly [player] [true|false]"); return true; }
        target.setAllowFlight(state);
        if (!state) target.setFlying(false);
        sender.sendMessage("§7Fly for §f" + target.getName() + "§7: §f" + state);
        return true;
    }

    private boolean flySpeed(CommandSender sender, String[] args) {
        if (args.length < 1 || args.length > 2) { sender.sendMessage("§cUsage: /flyspeed <0.001-10> [player]"); return true; }
        Player target;
        double speed;
        if (args.length == 1) {
            target = CommandUtil.player(sender);
            speed = parseSpeed(args[0]);
        } else {
            double first = parseSpeed(args[0]);
            if (first >= 0) {
                speed = first;
                target = CommandUtil.findPlayer(args[1]);
            } else {
                target = CommandUtil.findPlayer(args[0]);
                speed = parseSpeed(args[1]);
            }
            if (target == null) { sender.sendMessage("§cPlayer not found."); return true; }
            if (!plugin.permissions().has(sender, "vex.fly.others") && !(sender instanceof Player actor && plugin.permissions().role(actor).atLeast(dev.vexvol.minigames.model.StaffRole.ADMIN))) { sender.sendMessage(plugin.msg(sender, "generic.no_permission")); return true; }
        }
        if (target == null) { sender.sendMessage(plugin.msg(sender, "generic.player_only")); return true; }
        if (speed < 0) { sender.sendMessage("§cSpeed must be from 0.001 to 10."); return true; }
        plugin.fly().set(target, speed);
        sender.sendMessage("§7FlySpeed for §f" + target.getName() + "§7: §f" + speed);
        return true;
    }

    private double parseSpeed(String value) {
        try { double d=Double.parseDouble(value); return d >= 0.001 && d <= 10.0 ? d : -1; } catch (NumberFormatException e) { return -1; }
    }

    private boolean setTime(CommandSender sender, long time) {
        Player player = CommandUtil.player(sender);
        if (player == null) { sender.sendMessage("§cRun this command in-game."); return true; }
        player.getWorld().setTime(time);
        return true;
    }

    private boolean weather(CommandSender sender, String[] args) {
        Player player = CommandUtil.player(sender);
        if (player == null) { sender.sendMessage("§cRun this command in-game."); return true; }
        String type = args.length == 0 ? "clear" : args[0].toLowerCase(Locale.ROOT);
        World world = player.getWorld();
        switch (type) {
            case "clear" -> { world.setStorm(false); world.setThundering(false); }
            case "rain" -> { world.setStorm(true); world.setThundering(false); }
            case "thunder" -> { world.setStorm(true); world.setThundering(true); }
            default -> { sender.sendMessage("§cUsage: /weather <clear|rain|thunder>"); return true; }
        }
        return true;
    }

    private boolean tp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player) || args.length < 1) { sender.sendMessage("§cUsage: /tp <player>"); return true; }
        Player target = CommandUtil.findPlayer(args[0]);
        if (target == null) { sender.sendMessage("§cPlayer not found."); return true; }
        player.teleport(target);
        return true;
    }

    private boolean tpHere(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player) || args.length < 1) { sender.sendMessage("§cUsage: /tphere <player>"); return true; }
        Player target = CommandUtil.findPlayer(args[0]);
        if (target == null) { sender.sendMessage("§cPlayer not found."); return true; }
        target.teleport(player);
        return true;
    }

    private boolean gameMode(CommandSender sender, String[] args) {
        if (args.length < 1) { sender.sendMessage("§cUsage: /gm <survival|creative|adventure|spectator> [player]"); return true; }
        GameMode mode = CommandUtil.gameMode(args[0]);
        if (mode == null) { sender.sendMessage("§cInvalid gamemode."); return true; }
        Player target = args.length >= 2 ? CommandUtil.findPlayer(args[1]) : CommandUtil.player(sender);
        if (target == null) { sender.sendMessage("§cPlayer not found."); return true; }
        target.setGameMode(mode);
        return true;
    }

    private boolean spawn(CommandSender sender) {
        if (!(sender instanceof Player player)) { sender.sendMessage("§cRun this command in-game."); return true; }
        if (!plugin.lobby().teleport(player)) { sender.sendMessage("§cMain lobby is not configured or world is not loaded."); return true; }
        sender.sendMessage(plugin.msg(sender, "spawn.teleported"));
        return true;
    }

    private boolean kick(CommandSender sender, String[] args) {
        if (args.length < 1) { sender.sendMessage("§cUsage: /kick <player> [reason]"); return true; }
        Player target = CommandUtil.findPlayer(args[0]);
        if (target == null) { sender.sendMessage("§cPlayer not found."); return true; }
        target.kickPlayer(args.length >= 2 ? CommandUtil.join(args, 1) : "Kicked by VexVol");
        return true;
    }

    private boolean heal(CommandSender sender, String[] args) {
        Player target = args.length >= 1 ? CommandUtil.findPlayer(args[0]) : CommandUtil.player(sender);
        if (target == null) { sender.sendMessage("§cPlayer not found."); return true; }
        target.setHealth(target.getMaxHealth());
        return true;
    }

    private boolean feed(CommandSender sender, String[] args) {
        Player target = args.length >= 1 ? CommandUtil.findPlayer(args[0]) : CommandUtil.player(sender);
        if (target == null) { sender.sendMessage("§cPlayer not found."); return true; }
        target.setFoodLevel(20);
        target.setSaturation(20);
        return true;
    }

    private boolean external(CommandSender sender, String[] args, String action) {
        if (args.length < 1) { sender.sendMessage("§cUsage: /" + action + " <player> [duration] [reason]"); return true; }
        String target = args[0];
        String duration = args.length >= 2 ? args[1] : "permanent";
        String reason = args.length >= 3 ? CommandUtil.join(args, 2) : "No reason provided";
        var source = sender instanceof Player p ? p : null;
        var result = switch (action) {
            case "ban" -> plugin.voidrp().ban(source, target, duration, reason);
            case "unban" -> plugin.voidrp().unban(source, target, reason);
            case "mute" -> plugin.voidrp().mute(source, target, duration, reason);
            case "unmute" -> plugin.voidrp().unmute(source, target, reason);
            default -> dev.vexvol.minigames.integration.VoidRpIntegration.ActionResult.unavailable();
        };
        sender.sendMessage(result.success() ? "§aDone." : plugin.msg(sender, "external.not_configured"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "weather" -> args.length == 1 ? List.of("clear", "rain", "thunder") : List.of();
            case "flyspeed" -> args.length == 1 ? List.of("0.001", "0.1", "1", "2", "5", "10") : args.length == 2 ? Bukkit.getOnlinePlayers().stream().map(Player::getName).toList() : List.of();
            case "fly" -> args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(Player::getName).toList() : args.length == 2 ? List.of("true", "false") : List.of();
            case "gm" -> args.length == 1 ? List.of("survival", "creative", "adventure", "spectator") : List.of();
            default -> List.of();
        };
    }
}
