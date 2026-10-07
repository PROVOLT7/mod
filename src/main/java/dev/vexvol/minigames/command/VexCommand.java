package dev.vexvol.minigames.command;

import dev.vexvol.minigames.VexVolMinigamesPlugin;
import dev.vexvol.minigames.model.Arena;
import dev.vexvol.minigames.model.GameSession;
import dev.vexvol.minigames.model.StaffRole;
import dev.vexvol.minigames.model.ArenaScope;
import dev.vexvol.minigames.model.MinigameArena;
import dev.vexvol.minigames.model.MinigameMode;
import dev.vexvol.minigames.model.MinigameTeam;
import dev.vexvol.minigames.service.MatchmakingService;
import dev.vexvol.minigames.service.MinigameService;
import dev.vexvol.minigames.util.CommandUtil;
import dev.vexvol.minigames.util.DurationParser;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public final class VexCommand implements TabExecutor {
    private final VexVolMinigamesPlugin plugin;

    public VexCommand(VexVolMinigamesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String first = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        boolean publicCommand = first.equals("lang") || first.equals("minigame");
        if (!publicCommand && !plugin.permissions().has(sender, "vex.use")) {
            sender.sendMessage(plugin.msg(sender, "generic.no_permission"));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            help(sender);
            return true;
        }
        String root = args[0].toLowerCase(Locale.ROOT);
        return switch (root) {
            case "arena" -> arena(sender, Arrays.copyOfRange(args, 1, args.length));
            case "map" -> map(sender, Arrays.copyOfRange(args, 1, args.length));
            case "world" -> world(sender, Arrays.copyOfRange(args, 1, args.length));
            case "game" -> game(sender, Arrays.copyOfRange(args, 1, args.length));
            case "player" -> player(sender, Arrays.copyOfRange(args, 1, args.length));
            case "admin" -> admin(sender, Arrays.copyOfRange(args, 1, args.length));
            case "moderation" -> moderation(sender, Arrays.copyOfRange(args, 1, args.length));
            case "debug" -> debug(sender, Arrays.copyOfRange(args, 1, args.length));
            case "nick" -> nick(sender, Arrays.copyOfRange(args, 1, args.length));
            case "minigame" -> minigame(sender, Arrays.copyOfRange(args, 1, args.length));
            case "setlobbypos1" -> editPoint(sender, 1, "lobby-pos1");
            case "setlobbypos2" -> editPoint(sender, 2, "lobby-pos2");
            case "lobbyspawn" -> editLocation(sender, "lobbyspawn");
            case "setlobbycleaning" -> setLobbyCleaning(sender, args);
            case "lobbyplaystart" -> setLobbyPlayStart(sender, args);
            case "createteam" -> createTeam(sender, args);
            case "setteambed" -> setTeamLocation(sender, args, "bed");
            case "setteamspawn" -> setTeamLocation(sender, args, "spawn");
            case "setteamshop" -> setTeamShop(sender, args);
            case "setshopbox" -> setShopBox(sender, args);
            case "setgamebarrierpos1" -> editPoint(sender, 1, "barrier-pos1");
            case "setgamebarrierpos2" -> editPoint(sender, 2, "barrier-pos2");
            case "oneblock" -> oneblock(sender, Arrays.copyOfRange(args, 1, args.length));
            case "lang" -> language(sender, args);
            default -> { sender.sendMessage(plugin.msg(sender, "generic.invalid_argument")); yield true; }
        };
    }


    private boolean minigame(CommandSender sender, String[] args) {
        if (args.length == 0) return missing(sender, "/vex minigame <create|setup|delete|list|enable|disable|join|leave|start|stop> [mode] <arena>");
        String action=args[0].toLowerCase(Locale.ROOT);
        return switch(action) {
            case "list" -> require(sender, "vex.minigame.manage", () -> {
                String text=plugin.minigames().all().stream().map(a -> a.id()+"["+a.mode().id()+"]"+(a.enabled()?"":"(off)")).collect(Collectors.joining(", "));
                sender.sendMessage("§7Minigames: §f"+(text.isBlank()?"-":text));
            });
            case "create" -> require(sender, "vex.minigame.manage", () -> {
                if(args.length<3){usage(sender,"/vex minigame create <bedrockbox|oneblock> <arena>");return;}
                MinigameMode mode=MinigameMode.parse(args[1]); if(mode==null){sender.sendMessage("§cUnknown mode. §fbedrockbox, oneblock");return;}
                if(plugin.arenas().get(args[2])==null){notFound(sender,args[2]);return;}
                if(!plugin.minigames().create(args[2],mode))sender.sendMessage("§cMinigame config already exists.");else sender.sendMessage("§aРежим §f"+mode.id()+" §aсоздан для арены §f"+args[2]);
            });
            case "setup" -> require(sender, "vex.minigame.edit", () -> setupMinigame(sender,args));
            case "delete" -> require(sender, "vex.minigame.delete", () -> {String id=lastArg(args);if(id==null){usage(sender,"/vex minigame delete <arena>");return;} if(plugin.minigamesGame().match(plugin.minigames().get(id)).isPresent()){sender.sendMessage("§cНа этой арене идёт игра.");return;} if(!plugin.minigames().delete(id))notFound(sender,id);else {plugin.editMode().values().removeIf(id::equalsIgnoreCase);sender.sendMessage("§aНастройки мини-игры удалены: §f"+id);}});
            case "enable","disable" -> require(sender, "vex.minigame.manage", () -> {String id=lastArg(args);if(id==null){usage(sender,"/vex minigame "+action+" <arena>");return;}boolean state=action.equals("enable");if(!plugin.minigames().setEnabled(id,state))notFound(sender,id);else sender.sendMessage("§7Minigame §f"+id+"§7: "+(state?"§aON":"§cOFF"));});
            case "join" -> { if(args.length<2){usage(sender,"/vex minigame join <arena>");yield true;} MinigameArena a=plugin.minigames().get(args[1]);if(a==null){notFound(sender,args[1]);yield true;} if(!(sender instanceof Player p)){sender.sendMessage(plugin.msg(sender,"generic.player_only"));yield true;} sender.sendMessage(plugin.minigamesGame().join(p,a)?"§aВы вошли в лобби §f"+a.id()+"§a.":"§cНе удалось войти: проверьте включение арены, лобби и свободные места.");yield true; }
            case "leave" -> { if(!(sender instanceof Player p)){sender.sendMessage(plugin.msg(sender,"generic.player_only"));yield true;} sender.sendMessage(plugin.minigamesGame().leave(p)?"§eВы вышли из мини-игры.":"§cВы сейчас не на мини-игре.");yield true; }
            case "start" -> require(sender,"vex.minigame.start",()->{String id=lastArg(args);MinigameArena a=plugin.minigames().get(id);if(a==null){notFound(sender,id);return;}plugin.minigamesGame().forceStart(a);sender.sendMessage("§aПринудительный запуск: §f"+id);});
            case "stop" -> require(sender,"vex.minigame.stop",()->{String id=lastArg(args);MinigameArena a=plugin.minigames().get(id);if(a==null){notFound(sender,id);return;}plugin.minigamesGame().stop(a);sender.sendMessage("§eМини-игра остановлена: §f"+id);});
            default -> {help(sender);yield true;}
        };
    }

    private void setupMinigame(CommandSender sender,String[] args){
        String id=args.length>=2?args[args.length-1]:null;MinigameMode requested=null;
        if(args.length>=3) requested=MinigameMode.parse(args[1]);
        MinigameArena a=plugin.minigames().get(id);if(a==null){notFound(sender,id);return;}
        if(requested!=null && requested!=a.mode()){sender.sendMessage("§cАрена настроена как §f"+a.mode().id()+"§c, а не §f"+requested.id());return;}
        if(!(sender instanceof Player p)){sender.sendMessage(plugin.msg(sender,"generic.player_only"));return;}
        plugin.editMode().put(p.getUniqueId(),a.id());
        org.bukkit.World world=plugin.arenas().get(a.id())==null?null:Bukkit.getWorld(plugin.arenas().get(a.id()).worldName());
        if(world!=null && !p.getWorld().getUID().equals(world.getUID())) p.teleport(world.getSpawnLocation());
        sender.sendMessage("§aSetup включён для §f"+a.id()+" §7("+a.mode().id()+").");
        sender.sendMessage("§7Теперь можно использовать: §f/vex setlobbypos1 §7и остальные короткие команды.");
    }

    private MinigameArena current(CommandSender sender){
        if(!(sender instanceof Player p)){sender.sendMessage(plugin.msg(sender,"generic.player_only"));return null;}
        String id=plugin.editMode().get(p.getUniqueId());if(id==null){sender.sendMessage("§cСначала: /vex minigame setup <mode> <arena>");return null;}
        MinigameArena a=plugin.minigames().get(id);if(a==null){sender.sendMessage("§cРежим текущей арены не найден.");return null;}return a;
    }

    private boolean canEdit(CommandSender sender){
        if(!(sender instanceof Player p))return false;
        if(plugin.permissions().has(p,"vex.minigame.edit") || plugin.permissions().role(p).atLeast(StaffRole.ADMIN))return true;
        sender.sendMessage(plugin.msg(sender,"generic.no_permission"));return false;
    }
    private boolean editPoint(CommandSender sender,int point,String what){
        if(!canEdit(sender))return true; MinigameArena a=current(sender);if(a==null)return true; Player p=(Player)sender;
        if(what.startsWith("lobby")){plugin.minigames().setLobbyPos(a.id(),point,p.getLocation());sender.sendMessage("§aLobby pos"+point+" установлена.");}
        else {plugin.minigames().setBarrierPos(a.id(),point,p.getLocation());sender.sendMessage("§aGame barrier pos"+point+" установлена.");}
        return true;
    }
    private boolean editLocation(CommandSender sender,String what){
        if(!canEdit(sender))return true;MinigameArena a=current(sender);if(a==null)return true;Player p=(Player)sender;plugin.minigames().setLobbySpawn(a.id(),p.getLocation());sender.sendMessage("§aТочка lobby spawn установлена.");return true;
    }
    private boolean setLobbyCleaning(CommandSender sender,String[] args){if(!canEdit(sender))return true;MinigameArena a=current(sender);if(a==null)return true;if(args.length<2){usage(sender,"/vex setlobbycleaning <true|false>");return true;}String val=args[1];if(!val.equalsIgnoreCase("true")&&!val.equalsIgnoreCase("false")){sender.sendMessage("§cОжидается true/false.");return true;}plugin.minigames().setLobbyCleaning(a.id(),Boolean.parseBoolean(val));sender.sendMessage("§aОчистка lobby: §f"+val);return true;}
    private boolean setLobbyPlayStart(CommandSender sender,String[] args){if(!canEdit(sender))return true;MinigameArena a=current(sender);if(a==null)return true;if(args.length<2){usage(sender,"/vex lobbyplaystart <seconds>");return true;}try{int s=Integer.parseInt(args[1]);if(s<1||s>3600)throw new NumberFormatException();plugin.minigames().setLobbyPlayStart(a.id(),s);sender.sendMessage("§aОтсчёт старта: §f"+s+"§a сек.");}catch(NumberFormatException e){sender.sendMessage("§cСекунды: 1-3600.");}return true;}
    private boolean createTeam(CommandSender sender,String[] args){if(!canEdit(sender))return true;MinigameArena a=current(sender);if(a==null)return true;if(args.length<3){usage(sender,"/vex createteam <color> <teamName>");return true;}ChatColor c=MinigameService.parseColor(args[1]);if(c==null){sender.sendMessage("§cЦвет недопустим. §f"+MinigameService.colorNameList());return true;}if(a.teams().size()>=12){sender.sendMessage("§cМаксимум 12 команд.");return true;}if(!a.addTeam(args[2],c)){sender.sendMessage("§cТакая команда уже существует.");return true;}plugin.minigames().save();sender.sendMessage(c+"Команда §f"+args[2]+c+" создана.");return true;}
    private Location coordOrPlayer(CommandSender sender,String[] args,int start){Player p=(Player)sender;if(args.length<start+3)return p.getLocation();try{double x=Double.parseDouble(args[start]),y=Double.parseDouble(args[start+1]),z=Double.parseDouble(args[start+2]);return new Location(p.getWorld(),x,y,z,p.getLocation().getYaw(),p.getLocation().getPitch());}catch(NumberFormatException e){sender.sendMessage("§cКоординаты должны быть x y z.");return null;}}
    private boolean setTeamLocation(CommandSender sender,String[] args,String type){if(!canEdit(sender))return true;MinigameArena a=current(sender);if(a==null)return true;if(args.length<2){usage(sender,"/vex setteam"+type+" <team> [x y z]");return true;}MinigameTeam team=a.team(args[1]);if(team==null){notFound(sender,args[1]);return true;}Location loc=coordOrPlayer(sender,args,2);if(loc==null)return true;boolean ok=type.equals("bed")?plugin.minigames().setTeamBed(a.id(),team.name(),loc):plugin.minigames().setTeamSpawn(a.id(),team.name(),loc);sender.sendMessage(ok?"§aУстановлено: §f"+type+" §aдля команды §f"+team.name():"§cОшибка сохранения.");return true;}
    private boolean setTeamShop(CommandSender sender,String[] args){if(!canEdit(sender))return true;MinigameArena a=current(sender);if(a==null)return true;if(a.mode()!=MinigameMode.BEDROCKBOX){sender.sendMessage("§csetteamshop доступен только BedrockBox.");return true;}if(args.length<3){usage(sender,"/vex setteamshop <team> <mobType> [x y z]");return true;}MinigameTeam team=a.team(args[1]);if(team==null){notFound(sender,args[1]);return true;}Location loc=coordOrPlayer(sender,args,3);if(loc==null)return true;boolean ok=plugin.minigames().setTeamShop(a.id(),team.name(),args[2],loc);sender.sendMessage(ok?"§aТорговец команды установлен: §f"+team.name()+" §7("+args[2]+")":"§cНеизвестный тип моба или команда.");return true;}
    private boolean setShopBox(CommandSender sender,String[] args){if(!canEdit(sender))return true;MinigameArena a=current(sender);if(a==null)return true;if(a.mode()!=MinigameMode.BEDROCKBOX){sender.sendMessage("§csetshopbox доступен только BedrockBox.");return true;}if(args.length<2){usage(sender,"/vex setshopbox <mobType> [x y z]");return true;}Location loc=coordOrPlayer(sender,args,2);if(loc==null)return true;boolean ok=plugin.minigames().setShopBox(a.id(),args[1],loc);sender.sendMessage(ok?"§aЦентральный торговец установлен: §f"+args[1]:"§cНеизвестный тип моба.");return true;}
    private boolean oneblock(CommandSender sender,String[] args){
        if(!canEdit(sender))return true; MinigameArena a=current(sender); if(a==null)return true;
        if(a.mode()!=MinigameMode.ONEBLOCK){sender.sendMessage("§cЭта настройка доступна только для OneBlock.");return true;}
        if(args.length==0){usage(sender,"/vex oneblock <iteminterval|itemamount|barrier> ...");return true;}
        try{
            switch(args[0].toLowerCase(Locale.ROOT)){
                case "iteminterval","itemspeed" -> { if(args.length<2){usage(sender,"/vex oneblock iteminterval <seconds>");return true;} int sec=Integer.parseInt(args[1]); if(sec<1||sec>3600)throw new NumberFormatException(); plugin.minigames().setOneBlockInterval(a.id(),sec); sender.sendMessage("§aВыдача предмета: §fкаждые "+sec+" сек."); }
                case "itemamount" -> { if(args.length<2){usage(sender,"/vex oneblock itemamount <min> [max]");return true;} int min=Integer.parseInt(args[1]); int max=args.length>=3?Integer.parseInt(args[2]):min; if(min<1||max<min||max>64)throw new NumberFormatException(); plugin.minigames().setOneBlockAmount(a.id(),min,max); sender.sendMessage("§aКоличество предметов: §f"+min+(max==min?"":"-"+max)); }
                case "barrier" -> { if(args.length<2){usage(sender,"/vex oneblock barrier <size> [delaySec] [shrinkSec]");return true;} double size=Double.parseDouble(args[1]); int delay=args.length>=3?Integer.parseInt(args[2]):1200; int shrink=args.length>=4?Integer.parseInt(args[3]):600; if(size<1||size>100000||delay<0||shrink<1)throw new NumberFormatException(); plugin.minigames().setOneBlockBarrier(a.id(),size,delay,shrink); sender.sendMessage("§aБарьер OneBlock: §f"+size+"§7, старт сжатия §f"+delay+"с§7, длительность §f"+shrink+"с"); }
                default -> usage(sender,"/vex oneblock <iteminterval|itemamount|barrier> ...");
            }
        }catch(NumberFormatException e){sender.sendMessage("§cНекорректное значение.");}
        return true;
    }

    private boolean language(CommandSender sender,String[] args){if(!(sender instanceof Player p)){sender.sendMessage(plugin.msg(sender,"generic.player_only"));return true;}if(args.length<2){sender.sendMessage("§7Язык: §f/vex lang <ru|en>");return true;}if(!args[1].equalsIgnoreCase("ru")&&!args[1].equalsIgnoreCase("en")){sender.sendMessage("§cДоступно: ru, en.");return true;}plugin.language().set(p,args[1]);sender.sendMessage("§aLanguage: §f"+args[1].toUpperCase(Locale.ROOT));return true;}
    private String lastArg(String[] args){return args.length<2?null:args[args.length-1];}

    private boolean arena(CommandSender sender, String[] args) {
        if (args.length == 0) return missing(sender, "/vex arena <list|info|create|delete|enable|disable|setspawn|setlobby|setregion|reset|test|edit>");
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> require(sender, "vex.arena.list", () -> {
                String text = plugin.arenas().all().stream().map(Arena::id).collect(Collectors.joining(", "));
                sender.sendMessage("§7Arenas: §f" + (text.isBlank() ? "-" : text));
            });
            case "info" -> require(sender, "vex.arena.info", () -> {
                Arena a = arenaArg(sender, args, 1); if (a == null) return;
                sender.sendMessage("§7Arena §f" + a.id() + "§7 | map=" + a.mapTemplateId() + " | world=" + a.worldName() + " | enabled=" + a.enabled() + " | region=" + a.hasRegion());
            });
            case "create" -> require(sender, "vex.arena.create", () -> {
                if (args.length < 4) { usage(sender, "/vex arena create <id> <mapTemplate> <world>"); return; }
                if (!plugin.arenas().create(args[1], args[2], args[3])) sender.sendMessage("§cArena already exists.");
                else sender.sendMessage(plugin.msg(sender, "arena.created", Map.of("arena", args[1])));
            });
            case "delete" -> require(sender, "vex.arena.delete", () -> {
                Arena a = arenaArg(sender, args, 1); if (a == null) return;
                if (plugin.sessions().all().stream().anyMatch(s -> s.arenaId().equalsIgnoreCase(a.id()) && s.state() != GameSession.State.STOPPED)) { sender.sendMessage("§cArena has active sessions."); return; }
                plugin.arenas().delete(a.id());
                sender.sendMessage(plugin.msg(sender, "arena.deleted", Map.of("arena", a.id())));
            });
            case "enable" -> require(sender, "vex.arena.enable", () -> toggleArena(sender, args, true));
            case "disable" -> require(sender, "vex.arena.disable", () -> toggleArena(sender, args, false));
            case "setspawn" -> requirePlayer(sender, "vex.arena.setspawn", () -> {
                if (args.length < 3) { usage(sender, "/vex arena setspawn <arena> <team>"); return; }
                Arena a = arenaArg(sender, args, 1); if (a == null) return;
                plugin.arenas().setSpawn(a.id(), args[2].toUpperCase(Locale.ROOT), CommandUtil.player(sender).getLocation());
                sender.sendMessage(plugin.msg(sender, "arena.spawn_set", Map.of("arena", a.id(), "team", args[2].toUpperCase(Locale.ROOT))));
            });
            case "setlobby" -> requirePlayer(sender, "vex.arena.setlobby", () -> {
                if (args.length < 2) { usage(sender, "/vex arena setlobby <arena>"); return; }
                Arena a = arenaArg(sender, args, 1); if (a == null) return;
                plugin.arenas().setLobby(a.id(), CommandUtil.player(sender).getLocation());
                sender.sendMessage(plugin.msg(sender, "arena.lobby_set", Map.of("arena", a.id())));
            });
            case "setregion" -> requirePlayer(sender, "vex.arena.setregion", () -> {
                if (args.length < 3 || (!args[2].equalsIgnoreCase("pos1") && !args[2].equalsIgnoreCase("pos2"))) { usage(sender, "/vex arena setregion <arena> <pos1|pos2>"); return; }
                Arena a = arenaArg(sender, args, 1); if (a == null) return;
                int point = args[2].equalsIgnoreCase("pos1") ? 1 : 2;
                plugin.arenas().setRegion(a.id(), point, CommandUtil.player(sender).getLocation());
                sender.sendMessage(plugin.msg(sender, "arena.region_set", Map.of("arena", a.id(), "point", "pos" + point)));
            });
            case "reset" -> require(sender, "vex.arena.reset", () -> {
                Arena a = arenaArg(sender, args, 1); if (a == null) return;
                plugin.arenas().reset(a.id()); sender.sendMessage(plugin.msg(sender, "generic.done"));
            });
            case "test" -> require(sender, "vex.arena.test", () -> {
                Arena a = arenaArg(sender, args, 1); if (a == null) return;
                sender.sendMessage(plugin.msg(sender, "arena.test", Map.of("arena", a.id())));
            });
            case "edit" -> require(sender, "vex.arena.edit", () -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(plugin.msg(sender, "generic.player_only")); return; }
                Arena a = arenaArg(sender, args, 1); if (a == null) return;
                if (!plugin.permissions().has(p, "vex.map.build") && !plugin.scopes().hasScope(p, a.id()) && !plugin.permissions().role(p).atLeast(StaffRole.ADMIN)) {
                    sender.sendMessage(plugin.msg(sender, "generic.no_permission")); return;
                }
                plugin.editMode().put(p.getUniqueId(), a.id());
                sender.sendMessage(plugin.msg(sender, "arena.edit", Map.of("arena", a.id())));
            });
            default -> { help(sender); yield true; }
        };
    }

    private void toggleArena(CommandSender sender, String[] args, boolean enabled) {
        Arena a = arenaArg(sender, args, 1); if (a == null) return;
        plugin.arenas().setEnabled(a.id(), enabled);
        sender.sendMessage(plugin.msg(sender, enabled ? "arena.enabled" : "arena.disabled", Map.of("arena", a.id())));
    }

    private boolean map(CommandSender sender, String[] args) {
        if (args.length == 0) return missing(sender, "/vex map <list|info|create|delete|edit|import|export|test|build>");
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> require(sender, "vex.map.list", () -> sender.sendMessage("§7Maps: §f" + plugin.maps().all().stream().map(m -> m.id()).collect(Collectors.joining(", "))));
            case "info" -> require(sender, "vex.map.info", () -> {
                if (args.length < 2) { usage(sender, "/vex map info <id>"); return; }
                var map = plugin.maps().get(args[1]); if (map == null) { notFound(sender, args[1]); return; }
                sender.sendMessage("§7Map §f" + map.id() + "§7 | game=" + map.gameType() + " | world=" + map.worldName() + " | build=" + map.buildAllowed());
            });
            case "create" -> require(sender, "vex.map.create", () -> {
                if (args.length < 4) { usage(sender, "/vex map create <id> <gameType> <world>"); return; }
                if (!plugin.maps().create(args[1], args[2], args[3])) sender.sendMessage("§cMap already exists."); else sender.sendMessage(plugin.msg(sender, "generic.done"));
            });
            case "delete" -> require(sender, "vex.map.delete", () -> {
                if (args.length < 2) { usage(sender, "/vex map delete <id>"); return; }
                if (!plugin.maps().delete(args[1])) notFound(sender, args[1]); else sender.sendMessage(plugin.msg(sender, "generic.done"));
            });
            case "edit" -> require(sender, "vex.map.edit", () -> {
                if (args.length < 4 || !args[2].equalsIgnoreCase("build")) { usage(sender, "/vex map edit <id> build <on|off>"); return; }
                var map = plugin.maps().get(args[1]); if (map == null) { notFound(sender, args[1]); return; }
                boolean allowed = args[3].equalsIgnoreCase("on");
                plugin.maps().setBuildAllowed(map.id(), allowed);
                sender.sendMessage("§aBuild on map " + map.id() + ": " + allowed);
            });
            case "build" -> require(sender, "vex.map.build", () -> {
                if (args.length < 3) { usage(sender, "/vex map build <id> <on|off>"); return; }
                var map = plugin.maps().get(args[1]); if (map == null) { notFound(sender, args[1]); return; }
                plugin.maps().setBuildAllowed(map.id(), args[2].equalsIgnoreCase("on"));
                sender.sendMessage(plugin.msg(sender, "generic.done"));
            });
            case "test" -> require(sender, "vex.map.test", () -> sender.sendMessage(plugin.msg(sender, "generic.done")));
            case "import", "export" -> require(sender, "import".equalsIgnoreCase(args[0]) ? "vex.map.import" : "vex.map.export", () -> {
                if (args.length < 2) { usage(sender, "/vex map " + args[0] + " <id>"); return; }
                sender.sendMessage("§eMap " + args[0] + " is metadata-only in v0.3; use FAWE/world tooling for block data.");
            });
            default -> { help(sender); yield true; }
        };
    }

    private boolean world(CommandSender sender, String[] args) {
        if (args.length == 0) return missing(sender, "/vex world <list|create|load|unload|delete|tp|info>");
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> require(sender, "vex.world.list", () -> sender.sendMessage("§7Loaded worlds: §f" + plugin.worlds().loadedWorlds().stream().map(w -> w.getBukkitWorld().map(org.bukkit.World::getName).getOrElse("?")).collect(Collectors.joining(", "))));
            case "create" -> require(sender, "vex.world.create", () -> {
                if (args.length < 2) { usage(sender, "/vex world create <name> [generator]"); return; }
                String result = plugin.worlds().create(args[1], args.length >= 3 ? args[2] : plugin.getConfig().getString("worlds.generator", "VoidGen"), org.bukkit.World.Environment.valueOf(plugin.getConfig().getString("worlds.environment", "NORMAL").toUpperCase(Locale.ROOT)));
                if (result.equals("ok")) sender.sendMessage(plugin.msg(sender, "world.created", Map.of("world", args[1]))); else sender.sendMessage("§cWorld create failed: " + result);
            });
            case "load" -> require(sender, "vex.world.load", () -> {
                if (args.length < 2) { usage(sender, "/vex world load <name>"); return; }
                String result = plugin.worlds().load(args[1]); sender.sendMessage(result.equals("ok") ? plugin.msg(sender, "world.loaded", Map.of("world", args[1])) : "§c" + result);
            });
            case "unload" -> require(sender, "vex.world.unload", () -> {
                if (args.length < 2) { usage(sender, "/vex world unload <name>"); return; }
                String result = plugin.worlds().unload(args[1]); sender.sendMessage(result.equals("ok") ? plugin.msg(sender, "world.unloaded", Map.of("world", args[1])) : "§c" + result);
            });
            case "delete" -> require(sender, "vex.world.delete", () -> {
                if (args.length < 2) { usage(sender, "/vex world delete <name>"); return; }
                String result = plugin.worlds().delete(args[1]); sender.sendMessage(result.equals("ok") ? plugin.msg(sender, "world.deleted", Map.of("world", args[1])) : "§c" + result);
            });
            case "tp" -> requirePlayer(sender, "vex.world.teleport", () -> {
                if (args.length < 2) { usage(sender, "/vex world tp <name>"); return; }
                if (!plugin.worlds().teleport(CommandUtil.player(sender), args[1])) sender.sendMessage("§cWorld not loaded: " + args[1]); else sender.sendMessage(plugin.msg(sender, "world.teleported", Map.of("world", args[1])));
            });
            case "info" -> require(sender, "vex.world.info", () -> { if (args.length < 2) { usage(sender, "/vex world info <name>"); return; } plugin.worlds().sendInfo(sender, plugin.language().get(sender instanceof Player p ? p : null), args[1]); });
            default -> { help(sender); yield true; }
        };
    }

    private boolean game(CommandSender sender, String[] args) {
        if (args.length == 0) return missing(sender, "/vex game <list|info|start|stop|restart|join|leave|forcestart>");
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> require(sender, "vex.game.list", () -> sender.sendMessage("§7Sessions: §f" + plugin.sessions().all().stream().map(s -> "#" + s.id() + "(" + s.state() + ")").collect(Collectors.joining(", "))));
            case "info" -> require(sender, "vex.game.info", () -> {
                if (args.length < 2) { usage(sender, "/vex game info <sessionId>"); return; }
                GameSession s = sessionArg(sender, args, 1); if (s == null) return;
                sender.sendMessage("§7Session §f#" + s.id() + "§7 | arena=" + s.arenaId() + " | state=" + s.state() + " | players=" + s.players().size());
            });
            case "start" -> require(sender, "vex.game.start", () -> startGame(sender, args, false));
            case "forcestart" -> require(sender, "vex.game.forcestart", () -> startGame(sender, args, true));
            case "stop" -> require(sender, "vex.game.stop", () -> {
                GameSession s = sessionArg(sender, args, 1); if (s == null) return; plugin.sessions().stop(s); sender.sendMessage(plugin.msg(sender, "game.stopped", Map.of("session", "#" + s.id())));
            });
            case "restart" -> require(sender, "vex.game.restart", () -> {
                GameSession s = sessionArg(sender, args, 1); if (s == null) return; Arena a = plugin.arenas().get(s.arenaId()); if (a == null) return;
                GameSession fresh = plugin.sessions().restart(s, a); plugin.sessions().start(fresh); sender.sendMessage(plugin.msg(sender, "game.restarted", Map.of("session", "#" + fresh.id())));
            });
            case "join" -> require(sender, "vex.game.join", () -> {
                Player p = args.length >= 3 ? CommandUtil.findPlayer(args[2]) : sender instanceof Player self ? self : null;
                if (p == null) { sender.sendMessage("§cPlayer not found."); return; }
                GameSession s = sessionArg(sender, args, 1); if (s == null) return;
                if (!plugin.sessions().join(s, p)) { sender.sendMessage("§cCannot join session."); return; }
                p.sendMessage(plugin.msg(p, "game.joined", Map.of("session", "#" + s.id())));
            });
            case "leave" -> require(sender, "vex.game.leave", () -> {
                Player p = args.length >= 2 ? CommandUtil.findPlayer(args[1]) : sender instanceof Player self ? self : null;
                if (p == null) { sender.sendMessage("§cPlayer not found."); return; }
                GameSession s = plugin.sessions().findByPlayer(p.getUniqueId()).orElse(null); if (s == null) { sender.sendMessage("§cPlayer is not in a game."); return; }
                plugin.sessions().leave(s, p); p.sendMessage(plugin.msg(p, "game.left", Map.of("session", "#" + s.id())));
            });
            default -> { help(sender); yield true; }
        };
    }

    private void startGame(CommandSender sender, String[] args, boolean force) {
        if (args.length < 2) { usage(sender, "/vex game " + (force ? "forcestart" : "start") + " <arena>"); return; }
        Arena a = plugin.arenas().get(args[1]); if (a == null) { notFound(sender, args[1]); return; }
        if (!force && !a.enabled()) { sender.sendMessage("§cArena is disabled."); return; }
        GameSession s = plugin.sessions().create(a); plugin.sessions().start(s);
        sender.sendMessage(plugin.msg(sender, "game.started", Map.of("session", "#" + s.id(), "arena", a.id())));
    }

    private boolean player(CommandSender sender, String[] args) {
        if (args.length == 0) return missing(sender, "/vex player <info|teleport|tphere|fly|heal|feed|gamemode|kick>");
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "info" -> require(sender, "vex.player.info", () -> {
                if (args.length < 2) { usage(sender, "/vex player info <player>"); return; }
                Player p = CommandUtil.findPlayer(args[1]); if (p == null) { notFound(sender, args[1]); return; }
                sender.sendMessage("§7Player §f" + p.getName() + "§7 | world=" + p.getWorld().getName() + " | gm=" + p.getGameMode() + " | role=" + plugin.permissions().role(p));
            });
            case "teleport" -> require(sender, "vex.player.teleport", () -> {
                if (!(sender instanceof Player p) || args.length < 2) { usage(sender, "/vex player teleport <player>"); return; }
                Player target = CommandUtil.findPlayer(args[1]); if (target == null) { notFound(sender, args[1]); return; }
                p.teleport(target);
            });
            case "tphere" -> require(sender, "vex.player.tphere", () -> {
                if (!(sender instanceof Player p) || args.length < 2) { usage(sender, "/vex player tphere <player>"); return; }
                Player target = CommandUtil.findPlayer(args[1]); if (target == null) { notFound(sender, args[1]); return; }
                target.teleport(p);
            });
            case "fly" -> require(sender, "vex.player.fly", () -> {
                if (args.length < 2) { usage(sender, "/vex player fly <player>"); return; }
                Player target = CommandUtil.findPlayer(args[1]); if (target == null) { notFound(sender, args[1]); return; }
                target.setAllowFlight(!target.getAllowFlight()); if (!target.getAllowFlight()) target.setFlying(false);
            });
            case "heal" -> require(sender, "vex.heal", () -> { Player target = args.length >= 2 ? CommandUtil.findPlayer(args[1]) : sender instanceof Player p ? p : null; if (target == null) { sender.sendMessage("§cPlayer not found."); return; } target.setHealth(target.getMaxHealth()); });
            case "feed" -> require(sender, "vex.feed", () -> { Player target = args.length >= 2 ? CommandUtil.findPlayer(args[1]) : sender instanceof Player p ? p : null; if (target == null) { sender.sendMessage("§cPlayer not found."); return; } target.setFoodLevel(20); target.setSaturation(20); });
            case "gamemode" -> require(sender, "vex.gamemode", () -> {
                if (args.length < 3) { usage(sender, "/vex player gamemode <player> <mode>"); return; }
                Player target = CommandUtil.findPlayer(args[1]); GameMode gm = CommandUtil.gameMode(args[2]); if (target == null || gm == null) { sender.sendMessage("§cPlayer or gamemode invalid."); return; } target.setGameMode(gm);
            });
            case "kick" -> require(sender, "vex.kick", () -> {
                if (args.length < 2) { usage(sender, "/vex player kick <player> [reason]"); return; }
                Player target = CommandUtil.findPlayer(args[1]); if (target == null) { notFound(sender, args[1]); return; }
                target.kickPlayer(args.length >= 3 ? CommandUtil.join(args, 2) : "Kicked by VexVol");
            });
            default -> { help(sender); yield true; }
        };
    }

    private boolean admin(CommandSender sender, String[] args) {
        if (args.length == 0) return missing(sender, "/vex admin <reload|status|reloadlocale|maintenance|permissions|scopes>");
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> require(sender, "vex.admin.reload", () -> { plugin.reloadAll(); sender.sendMessage(plugin.msg(sender, "admin.reloaded")); });
            case "status" -> require(sender, "vex.admin.status", () -> sender.sendMessage(plugin.msg(sender, "admin.status", Map.of("version", plugin.getDescription().getVersion(), "arenas", String.valueOf(plugin.arenas().all().size()), "sessions", String.valueOf(plugin.sessions().all().size()), "worlds", String.valueOf(plugin.worlds().loadedWorlds().size())))));
            case "reloadlocale" -> require(sender, "vex.admin.reloadlocale", () -> { plugin.locale().reload(); sender.sendMessage(plugin.msg(sender, "admin.locale_reloaded")); });
            case "maintenance" -> require(sender, "vex.admin.maintenance", () -> { boolean state = args.length >= 2 ? Boolean.parseBoolean(args[1]) : !plugin.isMaintenance(); plugin.setMaintenance(state); sender.sendMessage(plugin.msg(sender, "admin.maintenance", Map.of("state", String.valueOf(state)))); });
            case "permissions" -> require(sender, "vex.admin.permissions", () -> {
                if (args.length < 3) { usage(sender, "/vex admin permissions <player> <node>"); return; }
                Player p = CommandUtil.findPlayer(args[1]); if (p == null) { notFound(sender, args[1]); return; }
                sender.sendMessage("§7" + p.getName() + " §f" + args[2] + "§7 = §f" + p.hasPermission(args[2]));
            });
            case "scopes" -> require(sender, "vex.admin.scopes", () -> scopes(sender, Arrays.copyOfRange(args, 1, args.length)));
            default -> { help(sender); yield true; }
        };
    }

    private void scopes(CommandSender sender, String[] args) {
        if (args.length == 0) { usage(sender, "/vex admin scopes <grant|revoke|list>"); return; }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "grant" -> {
                if (args.length < 4) { usage(sender, "/vex admin scopes grant <player> <arena> <duration>"); return; }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (plugin.arenas().get(args[2]) == null) { notFound(sender, args[2]); return; }
                long expiry = plugin.scopes().parseExpiry(args[3]); if (expiry < 0) { sender.sendMessage("§cInvalid duration."); return; }
                plugin.scopes().grant(target.getUniqueId(), args[2], expiry); sender.sendMessage("§aScope granted.");
            }
            case "revoke" -> {
                if (args.length < 3) { usage(sender, "/vex admin scopes revoke <player> <arena>"); return; }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                sender.sendMessage(plugin.scopes().revoke(target.getUniqueId(), args[2]) ? "§aScope revoked." : "§eScope not found.");
            }
            case "list" -> {
                if (args.length < 2) { usage(sender, "/vex admin scopes list <player>"); return; }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                for (ArenaScope scope : plugin.scopes().list(target.getUniqueId())) sender.sendMessage("§7" + scope.arenaId() + " | expires=" + scope.expiresAtEpochMillis());
            }
            default -> usage(sender, "/vex admin scopes <grant|revoke|list>");
        }
    }

    private boolean moderation(CommandSender sender, String[] args) {
        if (args.length == 0) return missing(sender, "/vex moderation <ban|unban|mute|unmute|kick>");
        String action = args[0].toLowerCase(Locale.ROOT);
        String perm = "vex.moderation." + action;
        return require(sender, perm, () -> {
            if (args.length < 2) { usage(sender, "/vex moderation " + action + " <player> [duration] [reason]"); return; }
            String target = args[1];
            String duration = args.length >= 3 ? args[2] : "permanent";
            String reason = args.length >= 4 ? CommandUtil.join(args, 3) : "No reason provided";
            var result = switch (action) {
                case "ban" -> plugin.voidrp().ban(sender instanceof Player p ? p : null, target, duration, reason);
                case "unban" -> plugin.voidrp().unban(sender instanceof Player p ? p : null, target, reason);
                case "mute" -> plugin.voidrp().mute(sender instanceof Player p ? p : null, target, duration, reason);
                case "unmute" -> plugin.voidrp().unmute(sender instanceof Player p ? p : null, target, reason);
                case "kick" -> { Player p = CommandUtil.findPlayer(target); if (p != null) p.kickPlayer(reason); yield dev.vexvol.minigames.integration.VoidRpIntegration.ActionResult.ok(); }
                default -> dev.vexvol.minigames.integration.VoidRpIntegration.ActionResult.unavailable();
            };
            if (!result.success()) sender.sendMessage(plugin.msg(sender, "external.not_configured"));
            else sender.sendMessage("§aDone.");
        });
    }

    private boolean debug(CommandSender sender, String[] args) {
        String sub = args.length == 0 ? "info" : args[0].toLowerCase(Locale.ROOT);
        String perm = "vex.debug." + sub;
        return require(sender, perm, () -> { switch (sub) {
            case "info" -> sender.sendMessage("§7VexVol §f" + plugin.getDescription().getVersion() + "§7; Paper API target 26.2");
            case "players" -> sender.sendMessage("§7Players: §f" + Bukkit.getOnlinePlayers().size());
            case "arenas" -> sender.sendMessage("§7Arenas: §f" + plugin.arenas().all().size());
            case "sessions" -> sender.sendMessage("§7Sessions: §f" + plugin.sessions().all().size());
            case "worlds" -> sender.sendMessage("§7Loaded Multiverse worlds: §f" + plugin.worlds().loadedWorlds().size());
            case "permissions" -> { if (!(sender instanceof Player p)) { sender.sendMessage("§7Console has no player permissions."); } else sender.sendMessage("§7Role=" + plugin.permissions().role(p)); }
            default -> help(sender);
        } });
    }

    private boolean nick(CommandSender sender, String[] args) {
        if (!(sender instanceof Player actor)) { sender.sendMessage(plugin.msg(sender, "generic.player_only")); return true; }
        if (!plugin.permissions().canUseNickname(actor)) { sender.sendMessage(plugin.msg(sender, "nick.role_denied")); return true; }
        if (args.length == 0) return missing(sender, "/vex nick <set|reset|info>");
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "set" -> require(sender, "vex.nickname.self.set", () -> {
                if (args.length < 2) { usage(sender, "/vex nick set <nickname> [duration]"); return; }
                Player target = actor;
                String nickname;
                String duration = "permanent";
                // Three arguments are interpreted as self + duration when the last token is a valid duration.
                boolean selfForm = args.length == 2 || (args.length == 3 && DurationParser.parseMillis(args[2]) > 0);
                if (selfForm) {
                    nickname = args[1];
                    if (args.length == 3) duration = args[2];
                } else {
                    Player maybeTarget = CommandUtil.findPlayer(args[1]);
                    if (maybeTarget == null) { sender.sendMessage("§cPlayer not found."); return; }
                    target = maybeTarget;
                    if (args.length < 3) { usage(sender, "/vex nick set <player> <nickname> [duration]"); return; }
                    nickname = args[2];
                    if (args.length >= 4) duration = args[3];
                }
                if (target.equals(actor) && !plugin.permissions().has(actor, "vex.nickname.self.set")) { sender.sendMessage(plugin.msg(sender, "generic.no_permission")); return; }
                if (!target.equals(actor) && !plugin.permissions().has(actor, "vex.nickname.others.set")) { sender.sendMessage(plugin.msg(sender, "generic.no_permission")); return; }
                if (!plugin.nicknames().set(actor, target, nickname, duration)) { sender.sendMessage(plugin.msg(sender, "nick.invalid")); return; }
                sender.sendMessage(plugin.msg(sender, target.equals(actor) ? "nick.set" : "nick.set_other", Map.of("nickname", nickname, "player", target.getName())));
            });
            case "reset" -> require(sender, "vex.nickname.self.reset", () -> {
                Player target = actor;
                if (args.length >= 2) { target = CommandUtil.findPlayer(args[1]); if (target == null) { sender.sendMessage("§cPlayer not found."); return; } if (!plugin.permissions().has(actor, "vex.nickname.others.reset")) { sender.sendMessage(plugin.msg(sender, "generic.no_permission")); return; } }
                plugin.nicknames().reset(actor, target); sender.sendMessage(plugin.msg(sender, target.equals(actor) ? "nick.reset" : "nick.reset_other", Map.of("player", target.getName())));
            });
            case "info" -> require(sender, "vex.nickname.info", () -> {
                Player target = args.length >= 2 ? CommandUtil.findPlayer(args[1]) : actor;
                if (target == null) { sender.sendMessage("§cPlayer not found."); return; }
                var data = plugin.nicknames().get(target.getUniqueId());
                sender.sendMessage("§7Nickname §f" + target.getName() + "§7 = §f" + (data == null ? target.getName() : data.displayName()) + "§7 | expires=" + (data == null ? "-" : data.expiresAt()));
            });
            default -> { help(sender); yield true; }
        };
    }

    private Arena arenaArg(CommandSender sender, String[] args, int index) {
        if (args.length <= index) { usage(sender, "/vex arena <action> <arena>"); return null; }
        Arena arena = plugin.arenas().get(args[index]);
        if (arena == null) notFound(sender, args[index]);
        return arena;
    }

    private GameSession sessionArg(CommandSender sender, String[] args, int index) {
        if (args.length <= index) { usage(sender, "/vex game <action> <sessionId>"); return null; }
        try {
            GameSession session = plugin.sessions().get(Long.parseLong(args[index]));
            if (session == null) notFound(sender, args[index]);
            return session;
        } catch (NumberFormatException e) { sender.sendMessage("§cSession id must be a number."); return null; }
    }

    private boolean require(CommandSender sender, String permission, Runnable action) {
        if (!plugin.permissions().has(sender, permission)) { sender.sendMessage(plugin.msg(sender, "generic.no_permission")); return true; }
        action.run(); return true;
    }

    private boolean requirePlayer(CommandSender sender, String permission, Runnable action) {
        if (!plugin.permissions().has(sender, permission)) { sender.sendMessage(plugin.msg(sender, "generic.no_permission")); return true; }
        action.run(); return true;
    }

    private boolean missing(CommandSender sender, String usage) { usage(sender, usage); return true; }
    private void usage(CommandSender sender, String usage) { sender.sendMessage(plugin.msg(sender, "generic.usage", Map.of("usage", usage))); }
    private void notFound(CommandSender sender, String value) { sender.sendMessage(plugin.msg(sender, "generic.not_found", Map.of("value", value))); }

    private void help(CommandSender sender) {
        sender.sendMessage("§bVexVol §7| §f/vex arena · map · world · game · player · admin · moderation · debug · nick");
        sender.sendMessage("§bMiniGames §7| §f/vex minigame create|setup|delete|list|enable|disable|join|leave|start|stop");
        sender.sendMessage("§7Setup: §f/vex setlobbypos1 §7/ §fsetlobbypos2 §7/ §flobbyspawn §7/ §fcreateteam §7/ §fsetteamspawn §7/ §fsetteambed");
        sender.sendMessage("§7BedrockBox: §fsetteamshop · setshopbox §7| OneBlock: §foneblock iteminterval|itemamount|barrier");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(List.of("arena", "map", "world", "game", "player", "admin", "moderation", "debug", "nick", "minigame", "setlobbypos1", "setlobbypos2", "lobbyspawn", "setlobbycleaning", "lobbyplaystart", "createteam", "setteambed", "setteamspawn", "setteamshop", "setshopbox", "setgamebarrierpos1", "setgamebarrierpos2", "oneblock", "lang"), args[0]);
        if (args[0].equalsIgnoreCase("arena") && args.length == 2) return filter(List.of("list", "info", "create", "delete", "enable", "disable", "setspawn", "setlobby", "setregion", "reset", "test", "edit"), args[1]);
        if (args[0].equalsIgnoreCase("map") && args.length == 2) return filter(List.of("list", "info", "create", "delete", "edit", "import", "export", "test", "build"), args[1]);
        if (args[0].equalsIgnoreCase("world") && args.length == 2) return filter(List.of("list", "create", "load", "unload", "delete", "tp", "info"), args[1]);
        if (args[0].equalsIgnoreCase("game") && args.length == 2) return filter(List.of("list", "info", "start", "stop", "restart", "join", "leave", "forcestart"), args[1]);
        if (args[0].equalsIgnoreCase("admin") && args.length == 2) return filter(List.of("reload", "status", "reloadlocale", "maintenance", "permissions", "scopes"), args[1]);
        if (args[0].equalsIgnoreCase("admin") && args.length == 3 && args[1].equalsIgnoreCase("scopes")) return filter(List.of("grant", "revoke", "list"), args[2]);
        if (args[0].equalsIgnoreCase("moderation") && args.length == 2) return filter(List.of("ban", "unban", "mute", "unmute", "kick"), args[1]);
        if (args[0].equalsIgnoreCase("debug") && args.length == 2) return filter(List.of("info", "players", "arenas", "sessions", "worlds", "permissions"), args[1]);
        if (args[0].equalsIgnoreCase("nick") && args.length == 2) return filter(List.of("set", "reset", "info"), args[1]);
        if (args[0].equalsIgnoreCase("minigame") && args.length == 2) return filter(List.of("create","setup","delete","list","enable","disable","join","leave","start","stop"), args[1]);
        if (args[0].equalsIgnoreCase("minigame") && args.length == 3 && List.of("create","setup").contains(args[1].toLowerCase(Locale.ROOT))) return args[1].equalsIgnoreCase("create") ? filter(List.of("bedrockbox","oneblock"), args[2]) : filter(List.of("bedrockbox","oneblock"), args[2]);
        if (args[0].equalsIgnoreCase("minigame") && args.length == 4 && (args[1].equalsIgnoreCase("create") || args[1].equalsIgnoreCase("setup"))) return filter(plugin.arenas().all().stream().map(Arena::id).toList(), args[3]);
        if (args[0].equalsIgnoreCase("minigame") && args.length == 3 && !args[1].equalsIgnoreCase("list")) return filter(plugin.minigames().all().stream().map(MinigameArena::id).toList(), args[2]);
        if (List.of("setlobbycleaning").contains(args[0].toLowerCase(Locale.ROOT)) && args.length == 2) return filter(List.of("true","false"),args[1]);
        if (args[0].equalsIgnoreCase("lobbyplaystart") && args.length == 2) return filter(List.of("5","10","15","30","60"),args[1]);
        if (args[0].equalsIgnoreCase("createteam") && args.length == 2) return filter(Arrays.asList(MinigameService.TEAM_COLORS),args[1]);
        if (args[0].equalsIgnoreCase("createteam") && args.length == 3) return List.of();
        if ((args[0].equalsIgnoreCase("setteambed") || args[0].equalsIgnoreCase("setteamspawn") || args[0].equalsIgnoreCase("setteamshop")) && args.length == 2) { MinigameArena a=currentSilent(sender); return a==null?List.of():filter(a.teams().keySet().stream().toList(),args[1]); }
        if ((args[0].equalsIgnoreCase("setteamshop") || args[0].equalsIgnoreCase("setshopbox")) && ((args[0].equalsIgnoreCase("setteamshop") && args.length == 3) || (args[0].equalsIgnoreCase("setshopbox") && args.length == 2))) return filter(List.of("VILLAGER","WANDERING_TRADER","ALLAY","PIGLIN","IRON_GOLEM"),args[args.length-1]);
        if (args[0].equalsIgnoreCase("oneblock") && args.length == 2) return filter(List.of("iteminterval","itemspeed","itemamount","barrier"),args[1]);
        if (args[0].equalsIgnoreCase("oneblock") && args.length == 3 && (args[1].equalsIgnoreCase("iteminterval") || args[1].equalsIgnoreCase("itemspeed"))) return filter(List.of("1","2","5","10","20"),args[2]);
        if (args[0].equalsIgnoreCase("oneblock") && args.length >= 3 && args[1].equalsIgnoreCase("itemamount")) return filter(List.of("1","2","4","8","16","32","64"),args[args.length-1]);
        if (args[0].equalsIgnoreCase("lang") && args.length == 2) return filter(List.of("ru","en"),args[1]);
        if ((args[0].equalsIgnoreCase("setlobbypos1") || args[0].equalsIgnoreCase("setlobbypos2") || args[0].equalsIgnoreCase("lobbyspawn") || args[0].equalsIgnoreCase("setgamebarrierpos1") || args[0].equalsIgnoreCase("setgamebarrierpos2"))) return List.of();
        return List.of();
    }

    private MinigameArena currentSilent(CommandSender sender) { if (!(sender instanceof Player p)) return null; String id=plugin.editMode().get(p.getUniqueId()); return id==null?null:plugin.minigames().get(id); }

    private List<String> filter(List<String> values, String input) {
        return values.stream().filter(v -> v.toLowerCase(Locale.ROOT).startsWith(input.toLowerCase(Locale.ROOT))).toList();
    }
}
