package dev.vexvol.minigames;

import dev.vexvol.minigames.command.SimpleCommandExecutor;
import dev.vexvol.minigames.command.VexCommand;
import dev.vexvol.minigames.integration.MultiverseWorldService;
import dev.vexvol.minigames.integration.NoopVoidRpIntegration;
import dev.vexvol.minigames.integration.VoidRpIntegration;
import dev.vexvol.minigames.listener.VexListener;
import dev.vexvol.minigames.model.Arena;
import dev.vexvol.minigames.service.ArenaService;
import dev.vexvol.minigames.service.GameSessionService;
import dev.vexvol.minigames.service.LocalizationService;
import dev.vexvol.minigames.service.MainLobbyService;
import dev.vexvol.minigames.service.MapService;
import dev.vexvol.minigames.service.MatchmakingService;
import dev.vexvol.minigames.service.MinigameGameService;
import dev.vexvol.minigames.service.MinigameService;
import dev.vexvol.minigames.service.FlyService;
import dev.vexvol.minigames.service.NicknameService;
import dev.vexvol.minigames.service.PermissionService;
import dev.vexvol.minigames.service.PlayerLanguageService;
import dev.vexvol.minigames.service.ScopeService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class VexVolMinigamesPlugin extends JavaPlugin {
    private LocalizationService locale;
    private PermissionService permissions;
    private ScopeService scopes;
    private MapService maps;
    private ArenaService arenas;
    private GameSessionService sessions;
    private MatchmakingService matchmaking;
    private NicknameService nicknames;
    private PlayerLanguageService language;
    private MainLobbyService lobby;
    private MultiverseWorldService worlds;
    private MinigameService minigames;
    private MinigameGameService minigamesGame;
    private FlyService fly;
    private VoidRpIntegration voidrp;
    private boolean maintenance;
    private final Map<UUID, String> editMode = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Could not create plugin data folder.");
        }

        locale = new LocalizationService(this);
        locale.reload();
        permissions = new PermissionService(this);
        scopes = new ScopeService(this);
        scopes.load();
        maps = new MapService(this);
        maps.load();
        arenas = new ArenaService(this);
        arenas.load();
        sessions = new GameSessionService();
        matchmaking = new MatchmakingService(arenas, sessions);
        language = new PlayerLanguageService(this);
        nicknames = new NicknameService(this, permissions, locale);
        lobby = new MainLobbyService(this);
        worlds = new MultiverseWorldService(locale);
        voidrp = new NoopVoidRpIntegration();
        minigames = new MinigameService(this);
        minigames.load();
        minigamesGame = new MinigameGameService(this, minigames);
        fly = new FlyService();

        registerCommands();
        getServer().getPluginManager().registerEvents(new VexListener(this), this);

        long cleanupTicks = Math.max(20L, getConfig().getLong("scopes.cleanup-expired-minutes", 1L) * 60L * 20L);
        getServer().getScheduler().runTaskTimer(this, () -> {
            scopes.cleanupExpired();
            nicknames.cleanupExpired(true);
            sessions.removeStopped();
        }, cleanupTicks, cleanupTicks);

        getLogger().info("VexVolMinigames " + getDescription().getVersion() + " enabled. Paper target 26.2.");
        if (!worlds.available()) getLogger().warning("Multiverse-Core API is not ready yet; world commands will be unavailable until it loads.");
    }

    @Override
    public void onDisable() {
        if (minigamesGame != null) minigamesGame.shutdown();
        editMode.clear();
        scopes.save();
        maps.save();
        arenas.save();
        nicknames.save();
        if (minigames != null) minigames.save();
    }

    private void registerCommands() {
        VexCommand vex = new VexCommand(this);
        if (getCommand("vex") != null) {
            getCommand("vex").setExecutor(vex);
            getCommand("vex").setTabCompleter(vex);
        }

        SimpleCommandExecutor simple = new SimpleCommandExecutor(this);
        for (String name : new String[]{"fly", "flyspeed", "day", "night", "weather", "tp", "tphere", "gm", "spawn", "kick", "heal", "feed", "vban", "vunban", "mute", "unmute"}) {
            if (getCommand(name) != null) {
                getCommand(name).setExecutor(simple);
                getCommand(name).setTabCompleter(simple);
            }
        }
    }

    public void reloadAll() {
        reloadConfig();
        locale.reload();
        language.load();
        maps.load();
        arenas.load();
        scopes.load();
        nicknames.reload();
        if (minigamesGame != null) minigamesGame.shutdown();
        minigames.load();
        minigamesGame = new MinigameGameService(this, minigames);
    }

    public String msg(CommandSender sender, String key) {
        return msg(sender, key, Map.of());
    }

    public String msg(CommandSender sender, String key, Map<String, String> placeholders) {
        String lang = sender instanceof Player player ? language.get(player) : "EN";
        return locale.message(lang, key, placeholders);
    }

    public LocalizationService locale() { return locale; }
    public PermissionService permissions() { return permissions; }
    public ScopeService scopes() { return scopes; }
    public MapService maps() { return maps; }
    public ArenaService arenas() { return arenas; }
    public GameSessionService sessions() { return sessions; }
    public MatchmakingService matchmaking() { return matchmaking; }
    public NicknameService nicknames() { return nicknames; }
    public PlayerLanguageService language() { return language; }
    public MainLobbyService lobby() { return lobby; }
    public MultiverseWorldService worlds() { return worlds; }
    public MinigameService minigames() { return minigames; }
    public MinigameGameService minigamesGame() { return minigamesGame; }
    public FlyService fly() { return fly; }
    public VoidRpIntegration voidrp() { return voidrp; }
    public Map<UUID, String> editMode() { return editMode; }
    public boolean isMaintenance() { return maintenance; }
    public void setMaintenance(boolean maintenance) { this.maintenance = maintenance; }

    public Arena arenaForWorld(String worldName) {
        return arenas.all().stream().filter(a -> a.worldName().equals(worldName)).findFirst().orElse(null);
    }
}
