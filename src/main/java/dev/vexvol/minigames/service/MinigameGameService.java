package dev.vexvol.minigames.service;

import dev.vexvol.minigames.VexVolMinigamesPlugin;
import dev.vexvol.minigames.model.MinigameArena;
import dev.vexvol.minigames.model.MinigameMode;
import dev.vexvol.minigames.model.MinigameTeam;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class MinigameGameService {
    private final VexVolMinigamesPlugin plugin;
    private final MinigameService config;
    private final ScoreboardService scoreboard;
    private final ShopService shops;
    private final Map<String, ActiveMatch> matches = new HashMap<>();
    private final Map<UUID, Location> bedRespawns = new HashMap<>();
    private final Set<UUID> shopEntities = new HashSet<>();
    private final List<Material> randomItems;

    public MinigameGameService(VexVolMinigamesPlugin plugin, MinigameService config) {
        this.plugin = plugin; this.config = config; this.scoreboard = new ScoreboardService(); this.shops = new ShopService(plugin);
        randomItems = Arrays.stream(Material.values()).filter(m -> m.isItem() && m != Material.AIR && m != Material.BEDROCK && m != Material.BARRIER && m != Material.COMMAND_BLOCK && m != Material.STRUCTURE_BLOCK && m != Material.JIGSAW && m != Material.LIGHT).toList();
    }

    public Optional<ActiveMatch> match(MinigameArena arena) { return Optional.ofNullable(arena == null ? null : matches.get(arena.id().toLowerCase(Locale.ROOT))); }
    public boolean join(Player player, MinigameArena arena) {
        if (arena == null || !arena.enabled()) return false;
        if (arena.lobbySpawn() == null) return false;
        ActiveMatch match = matches.computeIfAbsent(key(arena), k -> new ActiveMatch(arena));
        if (match.running) return false;
        int max = maxPlayers(arena);
        if (match.players.size() >= max) return false;
        if (!match.players.add(player.getUniqueId())) return true;
        player.teleport(arena.lobbySpawn());
        scoreboard.showLobby(player, arena, match.players.size(), max);
        if (match.players.size() >= minPlayers(arena)) ensureCountdown(match);
        return true;
    }

    public boolean leave(Player player) {
        ActiveMatch match = byPlayer(player.getUniqueId()); if (match == null) return false;
        match.players.remove(player.getUniqueId());
        match.eliminated.add(player.getUniqueId());
        if (!match.running && match.countdownTask != null) { match.countdownTask.cancel(); match.countdownTask=null; }
        cleanupPlayer(player);
        if (!match.running) {
            if (match.players.isEmpty()) finish(match, null);
            else refreshLobby(match);
        } else checkWinner(match);
        return true;
    }

    public void forceStart(MinigameArena arena) { if (arena != null) { ActiveMatch m=matches.computeIfAbsent(key(arena),k->new ActiveMatch(arena)); start(m); } }
    public void stop(MinigameArena arena) { ActiveMatch m=match(arena).orElse(null); if(m!=null) finish(m,null); }
    public boolean isInMatch(UUID uuid) { return byPlayer(uuid) != null; }
    public boolean isRunning(UUID uuid) { ActiveMatch m=byPlayer(uuid); return m != null && m.running; }
    public MinigameTeam team(UUID uuid) { ActiveMatch m=byPlayer(uuid); return m == null ? null : m.teamByPlayer.get(uuid); }
    public void onPlayerQuit(Player player) { if (isInMatch(player.getUniqueId())) eliminate(player, true); }
    public void onPlayerDeath(Player player) { ActiveMatch m=byPlayer(player.getUniqueId()); if(m==null || !m.running)return; plugin.getServer().getScheduler().runTask(plugin, () -> {
        if (m.arena.mode() == MinigameMode.BEDROCKBOX && bedAlive(m, player.getUniqueId())) player.sendMessage("§aВаша кровать цела — вы возродитесь.");
        else if (m.arena.mode() == MinigameMode.ONEBLOCK && bedRespawns.containsKey(player.getUniqueId())) player.sendMessage("§aВы возродитесь на своей кровати.");
    }); }

    public void eliminateAfterRespawn(Player player) {
        ActiveMatch m=byPlayer(player.getUniqueId()); if(m==null || !m.running || m.eliminated.contains(player.getUniqueId()))return;
        m.eliminated.add(player.getUniqueId());
        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage("§cВы выбыли из игры.");
        checkWinner(m);
    }

    public Location respawnLocation(Player player) {
        ActiveMatch m=byPlayer(player.getUniqueId()); if(m==null || !m.running)return null;
        if (m.arena.mode() == MinigameMode.BEDROCKBOX) {
            MinigameTeam team=m.teamByPlayer.get(player.getUniqueId());
            if(team!=null && Boolean.TRUE.equals(m.beds.get(team.name().toLowerCase(Locale.ROOT))) && team.spawn()!=null) return team.spawn();
            return null;
        }
        Location bed=bedRespawns.get(player.getUniqueId());
        return bed;
    }

    public void onBedEnter(Player player, Location bed) {
        ActiveMatch m=byPlayer(player.getUniqueId()); if(m==null || !m.running)return;
        bedRespawns.put(player.getUniqueId(), bed.clone());
        player.sendMessage("§aТочка возрождения сохранена.");
    }

    public void onBedBreak(Location broken) {
        for (ActiveMatch match: matches.values()) {
            if (!match.running) continue;
            for (MinigameTeam team: match.arena.teams().values()) {
                if (sameBlock(team.bed(), broken)) {
                    match.beds.put(team.name().toLowerCase(Locale.ROOT), false);
                    bedRespawns.entrySet().removeIf(entry -> match.players.contains(entry.getKey()) && sameBlock(entry.getValue(), broken));
                    Bukkit.getOnlinePlayers().forEach(p -> { if (match.players.contains(p.getUniqueId())) p.sendMessage(team.color()+"Команда "+team.name()+" §cлишилась кровати!"); });
                    checkWinner(match);
                }
            }
        }
    }

    public void onShopInteract(Player player, Entity entity) {
        if (!shopEntities.contains(entity.getUniqueId())) return;
        ActiveMatch match=byPlayer(player.getUniqueId()); if(match==null || !match.running)return;
        MinigameArena arena=match.arena;
        String marker=entity.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(plugin,"vex-shop"), org.bukkit.persistence.PersistentDataType.STRING);
        if("CENTER".equals(marker)) shops.openCenterShop(player,arena); else shops.openBaseShop(player,arena);
    }

    public boolean handleShopClick(Player player, String title, int slot) {
        if (!shops.isShop(title)) return false;
        if (title.contains("Центральный" ) && !isRunning(player.getUniqueId())) return false;
        // The ShopService owns the price table and product metadata.
        org.bukkit.inventory.InventoryView view=player.getOpenInventory(); shops.click(player, view.getTopInventory(), slot); return true;
    }

    public void shutdown() { new ArrayList<>(matches.values()).forEach(m -> finish(m,null)); }

    private void ensureCountdown(ActiveMatch match) {
        if (match.countdownTask != null) return;
        match.countdown = match.arena.lobbyPlayStartSeconds();
        match.countdownTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (match.players.isEmpty()) { match.countdownTask.cancel(); match.countdownTask=null; return; }
            if (match.players.size() < minPlayers(match.arena)) { refreshLobby(match); return; }
            refreshLobby(match);
            if (match.countdown <= 0) { match.countdownTask.cancel(); match.countdownTask=null; start(match); return; }
            if (match.countdown <= 5 || match.countdown % 5 == 0) for(UUID uuid:match.players){Player p=Bukkit.getPlayer(uuid);if(p!=null)p.sendTitle("§e"+match.countdown,"§7До начала игры",0,20,0);}
            match.countdown--;
        }, 0L, 20L);
    }

    private void start(ActiveMatch match) {
        if (match.running || match.players.size() < minPlayers(match.arena)) return;
        if (!validSetup(match.arena)) {
            for(UUID uuid:match.players){Player p=Bukkit.getPlayer(uuid);if(p!=null)p.sendMessage("§cАрена не настроена: нужны точки спавна команд и лобби.");}
            return;
        }
        match.running=true; match.startMillis=System.currentTimeMillis(); assignTeams(match);
        for (MinigameTeam team : match.arena.teams().values()) {
            match.beds.put(team.name().toLowerCase(Locale.ROOT), team.bed() != null);
        }
        if (match.arena.lobbyCleaning()) cleanLobby(match.arena);
        World world=world(match.arena); if(world!=null){ world.setPVP(true); world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false); world.setGameRule(GameRule.DO_MOB_SPAWNING,false); }
        for(UUID uuid:match.players){ Player p=Bukkit.getPlayer(uuid); if(p==null)continue; MinigameTeam team=match.teamByPlayer.get(uuid); if(team==null||team.spawn()==null)continue; p.setGameMode(GameMode.SURVIVAL); p.getInventory().clear(); p.setHealth(p.getMaxHealth()); p.setFoodLevel(20); p.setSaturation(20); p.teleport(team.spawn()); if(match.arena.mode()==MinigameMode.BEDROCKBOX){giveStarterArmor(p,team); if(team.bed()!=null) match.beds.put(team.name().toLowerCase(Locale.ROOT),true); scoreboard.showBedrock(p,match.arena,match.beds,count(p,Material.EMERALD),0);} }
        if(match.arena.mode()==MinigameMode.BEDROCKBOX){ spawnShops(match); }
        else setupOneBlock(match);
        if(match.arena.mode()==MinigameMode.ONEBLOCK){ startOneBlockTasks(match); }
        else match.scoreboardTask=plugin.getServer().getScheduler().runTaskTimer(plugin, ()->refreshRunningScoreboard(match),0L,20L);
    }

    private void assignTeams(ActiveMatch match) {
        List<MinigameTeam> teams=new ArrayList<>(match.arena.teams().values()); teams.sort(Comparator.comparing(t->t.name().toLowerCase(Locale.ROOT)));
        Map<String,Integer> counts=new HashMap<>(); for(MinigameTeam t:teams)counts.put(t.name().toLowerCase(Locale.ROOT),0);
        int index=0; for(UUID uuid:match.players){ if(teams.isEmpty())break; MinigameTeam selected;
            if(match.arena.mode()==MinigameMode.ONEBLOCK) selected=teams.get(Math.min(index,teams.size()-1)); else selected=teams.stream().min(Comparator.comparingInt(t->counts.get(t.name().toLowerCase(Locale.ROOT)))).orElse(teams.get(0));
            match.teamByPlayer.put(uuid,selected); counts.put(selected.name().toLowerCase(Locale.ROOT),counts.get(selected.name().toLowerCase(Locale.ROOT))+1); index++;
        }
    }

    private void setupOneBlock(ActiveMatch match) {
        World world=world(match.arena); if(world==null)return;
        WorldBorder border=world.getWorldBorder(); Location center=barrierCenter(match.arena); border.setCenter(center); border.setSize(initialBarrierSize(match.arena)); border.setDamageBuffer(0); border.setWarningDistance(8); border.setWarningTimeTicks(100);
        int i=0; for(UUID uuid:match.players){ Player p=Bukkit.getPlayer(uuid); if(p==null)continue; MinigameTeam team=match.teamByPlayer.get(uuid); if(team==null)continue; Location s=team.spawn(); if(s==null)continue; BlockStatePlatform.ensure(s); p.teleport(s.clone().add(0.5,1.0,0.5)); i++; }
    }

    private void startOneBlockTasks(ActiveMatch match) {
        match.itemTask=plugin.getServer().getScheduler().runTaskTimer(plugin, ()-> {
            if(!match.running)return;
            for(UUID uuid:match.players){ if(match.eliminated.contains(uuid))continue; Player p=Bukkit.getPlayer(uuid); if(p==null)continue; int amount=ThreadLocalRandom.current().nextInt(match.arena.oneBlockItemMinAmount(),match.arena.oneBlockItemMaxAmount()+1); Material material=randomItems.get(ThreadLocalRandom.current().nextInt(randomItems.size())); Map<Integer,ItemStack> left=p.getInventory().addItem(new ItemStack(material,amount)); left.values().forEach(x->p.getWorld().dropItemNaturally(p.getLocation(),x)); match.itemsDelivered+=amount; }
        },0L,Math.max(1,match.arena.oneBlockItemIntervalSeconds())*20L);
        match.borderTask=plugin.getServer().getScheduler().runTaskLater(plugin, ()-> {
            World world=world(match.arena); if(world!=null)world.getWorldBorder().changeSize(1.0,Math.max(1,match.arena.oneBlockBarrierShrinkSeconds())*20L);
        },Math.max(0,match.arena.oneBlockBarrierDelaySeconds())*20L);
        match.scoreboardTask=plugin.getServer().getScheduler().runTaskTimer(plugin,()->refreshRunningScoreboard(match),0L,20L);
    }

    private void spawnShops(ActiveMatch match) {
        World world=world(match.arena); if(world==null)return;
        org.bukkit.NamespacedKey key=new org.bukkit.NamespacedKey(plugin,"vex-shop");
        for(MinigameTeam team:match.arena.teams().values()){
            if(team.shop()!=null && team.shopType()!=null){ Entity e=spawnShop(world,team.shop(),team.shopType(),team.name()+" §f• Магазин"); if(e!=null){e.getPersistentDataContainer().set(key,org.bukkit.persistence.PersistentDataType.STRING,"BASE");shopEntities.add(e.getUniqueId());match.shopIds.add(e.getUniqueId());} }
        }
        if(match.arena.shopBox()!=null && match.arena.shopBoxType()!=null){ Entity e=spawnShop(world,match.arena.shopBox(),match.arena.shopBoxType(),"§eЦентральный торговец");if(e!=null){e.getPersistentDataContainer().set(key,org.bukkit.persistence.PersistentDataType.STRING,"CENTER");shopEntities.add(e.getUniqueId());match.shopIds.add(e.getUniqueId());} }
    }

    private Entity spawnShop(World world, Location loc, String type, String name){
        try { org.bukkit.entity.Entity e=world.spawnEntity(loc,org.bukkit.entity.EntityType.valueOf(type)); e.setInvulnerable(true);e.setSilent(true);e.setPersistent(false);e.setCustomNameVisible(true);e.setCustomName(name); if(e instanceof Mob mob)mob.setAI(false); if(e instanceof LivingEntity living)living.setCollidable(false); return e; } catch(IllegalArgumentException ignored){return null;}
    }

    private void refreshRunningScoreboard(ActiveMatch m){
        long elapsed=(System.currentTimeMillis()-m.startMillis)/1000L; long border= -1;
        if(m.arena.mode()==MinigameMode.ONEBLOCK){ long delay=m.arena.oneBlockBarrierDelaySeconds(); long shrink=m.arena.oneBlockBarrierShrinkSeconds(); long after=Math.max(0,elapsed-delay); if(elapsed>=delay) border=Math.max(0,shrink-after); int alive=aliveTeams(m); for(UUID uuid:m.players){Player p=Bukkit.getPlayer(uuid);if(p!=null)scoreboard.showOneBlock(p,m.arena,alive,border,elapsed,m.itemsDelivered);} }
        else for(UUID uuid:m.players){Player p=Bukkit.getPlayer(uuid);if(p!=null){MinigameTeam t=m.teamByPlayer.get(uuid); scoreboard.showBedrock(p,m.arena,m.beds,count(p,Material.EMERALD),0);}}
    }

    private void refreshLobby(ActiveMatch m){for(UUID uuid:m.players){Player p=Bukkit.getPlayer(uuid);if(p!=null)scoreboard.showLobby(p,m.arena,m.players.size(),maxPlayers(m.arena));}}
    private int aliveTeams(ActiveMatch m){Set<String> alive=new HashSet<>();for(UUID uuid:m.players){if(m.eliminated.contains(uuid))continue;MinigameTeam t=m.teamByPlayer.get(uuid);if(t!=null)alive.add(t.name().toLowerCase(Locale.ROOT));}return alive.size();}
    private boolean bedAlive(ActiveMatch m,UUID uuid){MinigameTeam t=m.teamByPlayer.get(uuid);return t!=null && Boolean.TRUE.equals(m.beds.get(t.name().toLowerCase(Locale.ROOT)));}

    private void eliminate(Player player, boolean disconnect){ ActiveMatch m=byPlayer(player.getUniqueId());if(m==null)return; m.eliminated.add(player.getUniqueId());if(m.running){ if(!disconnect){ player.setGameMode(GameMode.SPECTATOR); } checkWinner(m);} else m.players.remove(player.getUniqueId()); }
    private void checkWinner(ActiveMatch m){if(!m.running)return;Set<String> alive=new LinkedHashSet<>();for(UUID uuid:m.players){if(m.eliminated.contains(uuid))continue;MinigameTeam t=m.teamByPlayer.get(uuid);if(t!=null)alive.add(t.name().toLowerCase(Locale.ROOT));} if(alive.size()<=1){String winner=alive.isEmpty()?null:alive.iterator().next();finish(m,winner);} }
    private boolean allTeamEliminated(ActiveMatch m,MinigameTeam team){boolean has=false;for(UUID uuid:m.players){MinigameTeam current=m.teamByPlayer.get(uuid); if(current!=null && team.name().equalsIgnoreCase(current.name())){has=true;if(!m.eliminated.contains(uuid))return false;}}return has;}
    private void finish(ActiveMatch m,String winner){if(m==null)return; if(m.countdownTask!=null)m.countdownTask.cancel();if(m.itemTask!=null)m.itemTask.cancel();if(m.borderTask!=null)m.borderTask.cancel();if(m.scoreboardTask!=null)m.scoreboardTask.cancel();for(UUID id:m.shopIds){Entity e=Bukkit.getEntity(id);if(e!=null)e.remove();shopEntities.remove(id);}World w=world(m.arena);if(w!=null && m.arena.mode()==MinigameMode.ONEBLOCK)w.getWorldBorder().reset();for(UUID uuid:new HashSet<>(m.players)){Player p=Bukkit.getPlayer(uuid);if(p!=null){if(winner!=null && Objects.equals(m.teamByPlayer.get(uuid),findTeam(m.arena,winner)))p.sendTitle("§aПОБЕДА","§fКоманда: "+winner,10,60,10);cleanupPlayer(p);plugin.lobby().teleport(p);scoreboard.clear(p);}}matches.remove(key(m.arena));}
    private MinigameTeam findTeam(MinigameArena a,String name){for(MinigameTeam t:a.teams().values())if(t.name().equalsIgnoreCase(name))return t;return null;}

    private void cleanLobby(MinigameArena a){Location p1=a.lobbyPos1(),p2=a.lobbyPos2();if(p1==null||p2==null||p1.getWorld()==null||p2.getWorld()==null||!p1.getWorld().getUID().equals(p2.getWorld().getUID()))return;World w=p1.getWorld();int minX=Math.min(p1.getBlockX(),p2.getBlockX()),maxX=Math.max(p1.getBlockX(),p2.getBlockX());int minY=Math.max(w.getMinHeight(),Math.min(p1.getBlockY(),p2.getBlockY())),maxY=Math.min(w.getMaxHeight()-1,Math.max(p1.getBlockY(),p2.getBlockY()));int minZ=Math.min(p1.getBlockZ(),p2.getBlockZ()),maxZ=Math.max(p1.getBlockZ(),p2.getBlockZ());for(int x=minX;x<=maxX;x++)for(int y=minY;y<=maxY;y++)for(int z=minZ;z<=maxZ;z++)w.getBlockAt(x,y,z).setType(Material.AIR,false);}
    private boolean validSetup(MinigameArena a){if(a.teams().size()<2)return false;if(a.lobbySpawn()==null)return false;for(MinigameTeam t:a.teams().values())if(t.spawn()==null)return false;return true;}
    private int maxPlayers(MinigameArena a){return a.mode()==MinigameMode.ONEBLOCK?Math.min(8,a.teams().size()):Math.min(12,Math.max(2,a.teams().size()*2));}
    private int minPlayers(MinigameArena a){return 2;}
    private String key(MinigameArena a){return a.id().toLowerCase(Locale.ROOT);}
    private ActiveMatch byPlayer(UUID uuid){for(ActiveMatch m:matches.values())if(m.players.contains(uuid))return m;return null;}
    private World world(MinigameArena a){ return plugin.arenas().get(a.id())==null?null:Bukkit.getWorld(plugin.arenas().get(a.id()).worldName()); }
    private double initialBarrierSize(MinigameArena a){Location p1=a.barrierPos1(),p2=a.barrierPos2();if(p1!=null&&p2!=null)return Math.max(1.0,Math.max(Math.abs(p2.getX()-p1.getX()),Math.abs(p2.getZ()-p1.getZ())));return a.oneBlockBarrierInitialSize();}
    private Location barrierCenter(MinigameArena a){Location p1=a.barrierPos1(),p2=a.barrierPos2();if(p1!=null&&p2!=null&&p1.getWorld()!=null&&p1.getWorld().getUID().equals(p2.getWorld().getUID()))return new Location(p1.getWorld(),(p1.getX()+p2.getX())/2.0,0,(p1.getZ()+p2.getZ())/2.0);return a.teams().values().iterator().next().spawn();}
    private boolean sameBlock(Location a,Location b){return a!=null&&b!=null&&a.getWorld()!=null&&b.getWorld()!=null&&a.getWorld().getUID().equals(b.getWorld().getUID())&&a.getBlockX()==b.getBlockX()&&a.getBlockY()==b.getBlockY()&&a.getBlockZ()==b.getBlockZ();}
    private int count(Player p,Material m){int c=0;for(ItemStack i:p.getInventory().getStorageContents())if(i!=null&&i.getType()==m)c+=i.getAmount();return c;}
    private void giveStarterArmor(Player p,MinigameTeam team){p.getInventory().setHelmet(colored(Material.LEATHER_HELMET,team.color()));p.getInventory().setChestplate(colored(Material.LEATHER_CHESTPLATE,team.color()));}
    private ItemStack colored(Material mat,ChatColor color){ItemStack item=new ItemStack(mat);LeatherArmorMeta meta=(LeatherArmorMeta)item.getItemMeta();meta.setColor(colorMap(color));meta.setUnbreakable(true);item.setItemMeta(meta);return item;}
    private Color colorMap(ChatColor c){return switch(c){case RED,DARK_RED->Color.RED;case BLUE,DARK_BLUE->Color.BLUE;case GREEN,DARK_GREEN->Color.GREEN;case YELLOW,GOLD->Color.YELLOW;case AQUA,DARK_AQUA->Color.AQUA;case LIGHT_PURPLE,DARK_PURPLE->Color.PURPLE;default->Color.WHITE;};}
    private void cleanupPlayer(Player p){p.closeInventory();bedRespawns.remove(p.getUniqueId());p.setGameMode(GameMode.SURVIVAL);p.getInventory().remove(Material.WOODEN_PICKAXE);p.getInventory().remove(Material.STONE_PICKAXE);p.getInventory().remove(Material.IRON_PICKAXE);p.getInventory().remove(Material.GOLDEN_PICKAXE);p.getInventory().remove(Material.DIAMOND_PICKAXE);p.getInventory().remove(Material.NETHERITE_PICKAXE);}

    public final class ActiveMatch {
        final MinigameArena arena; final Set<UUID> players=new LinkedHashSet<>(); final Set<UUID> eliminated=new HashSet<>(); final Map<UUID,MinigameTeam> teamByPlayer=new HashMap<>(); final Map<String,Boolean> beds=new HashMap<>(); final Set<UUID> shopIds=new HashSet<>(); int countdown; boolean running; long startMillis; int itemsDelivered; BukkitTask countdownTask,itemTask,borderTask,scoreboardTask;
        ActiveMatch(MinigameArena arena){this.arena=arena;}
    }
}
