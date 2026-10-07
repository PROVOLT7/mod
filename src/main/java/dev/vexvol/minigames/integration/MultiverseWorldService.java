package dev.vexvol.minigames.integration;

import dev.vexvol.minigames.service.LocalizationService;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.MultiverseWorld;
import org.mvplugins.multiverse.core.world.LoadedMultiverseWorld;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.core.world.options.CreateWorldOptions;
import org.mvplugins.multiverse.core.world.options.DeleteWorldOptions;
import org.mvplugins.multiverse.core.world.options.LoadWorldOptions;

import java.util.Collection;

public final class MultiverseWorldService {
    private final LocalizationService locale;
    private WorldManager worldManager;

    public MultiverseWorldService(LocalizationService locale) {
        this.locale = locale;
        if (Bukkit.getPluginManager().getPlugin("Multiverse-Core") != null) {
            MultiverseCoreApi.whenLoaded(api -> this.worldManager = api.getWorldManager());
        }
    }

    public boolean available() {
        return worldManager != null && MultiverseCoreApi.isLoaded();
    }

    public LoadedMultiverseWorld getLoaded(String name) {
        if (!available()) return null;
        return worldManager.getLoadedWorldByNameOrAlias(name).getOrNull();
    }

    public MultiverseWorld get(String name) {
        if (!available()) return null;
        return worldManager.getWorldByNameOrAlias(name).getOrNull();
    }

    public Collection<LoadedMultiverseWorld> loadedWorlds() {
        return available() ? worldManager.getLoadedWorlds() : java.util.List.of();
    }

    public String create(String worldName, String generator, World.Environment environment) {
        if (!available()) return "multiverse_missing";
        CreateWorldOptions options = CreateWorldOptions.worldName(worldName)
            .environment(environment)
            .worldType(WorldType.NORMAL)
            .generator(generator)
            .generateStructures(false)
            .useSpawnAdjust(false);
        var result = worldManager.createWorld(options);
        if (result.isFailure()) return "create_failed:" + result.getFailureReason();
        return "ok";
    }

    public String load(String worldName) {
        if (!available()) return "multiverse_missing";
        MultiverseWorld world = get(worldName);
        if (world == null) return "not_found";
        var result = worldManager.loadWorld(LoadWorldOptions.world(world));
        return result.isSuccess() ? "ok" : "load_failed:" + result.getFailureReason();
    }

    public String delete(String worldName) {
        if (!available()) return "multiverse_missing";
        MultiverseWorld world = get(worldName);
        if (world == null) return "not_found";
        var result = worldManager.deleteWorld(DeleteWorldOptions.world(world));
        return result.isSuccess() ? "ok" : "delete_failed:" + result.getFailureReason();
    }

    public String unload(String worldName) {
        if (!available()) return "multiverse_missing";
        LoadedMultiverseWorld world = getLoaded(worldName);
        if (world == null) return "not_loaded";
        var result = worldManager.unloadWorld(
            org.mvplugins.multiverse.core.world.options.UnloadWorldOptions.world(world)
                .unloadBukkitWorld(true)
                .saveBukkitWorld(true));
        return result.isSuccess() ? "ok" : "unload_failed:" + result.getFailureReason();
    }

    public boolean teleport(Player player, String worldName) {
        LoadedMultiverseWorld world = getLoaded(worldName);
        if (world == null) return false;
        World bukkitWorld = world.getBukkitWorld().getOrNull();
        if (bukkitWorld == null) return false;
        return player.teleport(bukkitWorld.getSpawnLocation());
    }

    public void sendInfo(CommandSender sender, String language, String worldName) {
        if (!available()) {
            sender.sendMessage(locale.message(language, "world.multiverse_missing"));
            return;
        }
        MultiverseWorld world = get(worldName);
        if (world == null) {
            sender.sendMessage(locale.message(language, "generic.not_found", java.util.Map.of("value", worldName)));
            return;
        }
        sender.sendMessage("§7World §f" + worldName + " §7| loaded=" + worldManager.isLoadedWorld(worldName)
            + " | generator=" + world.getGenerator()
            + " | environment=" + world.getEnvironment());
    }
}
