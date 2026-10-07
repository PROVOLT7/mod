package dev.vexvol.minigames.listener;

import dev.vexvol.minigames.VexVolMinigamesPlugin;
import dev.vexvol.minigames.model.Arena;
import dev.vexvol.minigames.model.StaffRole;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class VexListener implements Listener {
    private final VexVolMinigamesPlugin plugin;

    public VexListener(VexVolMinigamesPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) { plugin.nicknames().apply(event.getPlayer()); }

    @EventHandler
    public void onLocale(PlayerLocaleChangeEvent event) { plugin.language().handleLocaleChange(event.getPlayer(), event); }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) { plugin.minigamesGame().onPlayerQuit(event.getPlayer()); plugin.editMode().remove(event.getPlayer().getUniqueId()); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().getType().name().endsWith("BED")) plugin.minigamesGame().onBedBreak(event.getBlock().getLocation());
        if (!mayBuild(event.getPlayer(), event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!mayBuild(event.getPlayer(), event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractShop(PlayerInteractEntityEvent event) { plugin.minigamesGame().onShopInteract(event.getPlayer(), event.getRightClicked()); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (plugin.minigamesGame().handleShopClick(player, event.getView().getTitle(), event.getRawSlot())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().startsWith("§8BedrockBox •")) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBedEnter(PlayerBedEnterEvent event) { plugin.minigamesGame().onBedEnter(event.getPlayer(), event.getBed().getLocation()); }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) { plugin.minigamesGame().onPlayerDeath(event.getPlayer()); }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Location target = plugin.minigamesGame().respawnLocation(event.getPlayer());
        if (target != null) event.setRespawnLocation(target);
        else if (plugin.minigamesGame().isRunning(event.getPlayer().getUniqueId())) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Player player = event.getPlayer();
                if (plugin.minigamesGame().isRunning(player.getUniqueId())) plugin.minigamesGame().eliminateAfterRespawn(player);
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFriendlyFire(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !(event.getDamager() instanceof Player attacker)) return;
        var a = plugin.minigamesGame().team(attacker.getUniqueId());
        var v = plugin.minigamesGame().team(victim.getUniqueId());
        if (a != null && v != null && a.name().equalsIgnoreCase(v.name())) event.setCancelled(true);
    }

    private boolean mayBuild(Player player, org.bukkit.Location location) {
        if (plugin.minigamesGame().isRunning(player.getUniqueId())) return true;
        String arenaId = plugin.editMode().get(player.getUniqueId());
        if (arenaId != null) {
            Arena arena = plugin.arenas().get(arenaId);
            if (arena == null || !location.getWorld().getName().equals(arena.worldName())) return false;
            if (plugin.permissions().role(player).atLeast(StaffRole.ADMIN)) return true;
            return plugin.permissions().has(player, "vex.map.build") && plugin.scopes().hasScope(player, arena.id());
        }
        for (Arena arena : plugin.arenas().all()) {
            if (!arena.enabled()) continue;
            if (!arena.worldName().equals(location.getWorld().getName())) continue;
            return plugin.permissions().role(player).atLeast(StaffRole.ADMIN);
        }
        return true;
    }
}
