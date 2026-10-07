# VexVolMinigames

Core Paper 26.2 plugin for VexVol. This build includes the foundation from the fixed archive plus two playable mini-games: **BedrockBox** and **OneBlock**.

## Target

- Paper API: `26.2.build.130-stable`
- Java: 25
- Multiverse-Core API: 5.8.1 (compile-only)
- Main plugin: `dev.vexvol.minigames.VexVolMinigamesPlugin`

## Mini-games

### BedrockBox
- 2+ players, up to 12 configured slots.
- Team color/name configuration with 12 allowed colors.
- Team bed + respawn + base shop.
- Central hidden trader/shop.
- Emeralds are the only shop currency.
- Team starter helmet/chestplate are colored and unbreakable.
- Base shop: every pickaxe tier, furnace, oak planks, netherrack, obsidian, food, bow, arrows, shears.
- Central shop: diamond/netherite gear, Protection II, Sharpness II, golden apples.
- Right-side sidebar scoreboard.

### OneBlock
- 2–8 players; one configured team per player.
- Void-style arena is expected; each configured spawn gets a protected bedrock platform.
- Random Minecraft item delivery on a configurable interval and amount.
- After the default 20 minutes the world border shrinks to 1 block over the next 10 minutes.
- Sleeping in a found bed gives a temporary respawn point; if that bed is destroyed the respawn point is removed.
- The last surviving team wins.
- Right-side sidebar scoreboard.

## Mini-game setup commands

Create a mode definition for an existing arena:

```text
/vex minigame create <bedrockbox|oneblock> <arena>
/vex minigame setup <bedrockbox|oneblock> <arena>
/vex minigame list
/vex minigame enable <arena>
/vex minigame disable <arena>
/vex minigame delete <arena>
/vex minigame join <arena>
/vex minigame leave
/vex minigame start <arena>
/vex minigame stop <arena>
```

After entering setup mode, these short commands target the selected mini-game arena:

```text
/vex setlobbypos1
/vex setlobbypos2
/vex lobbyspawn
/vex setlobbycleaning <true|false>
/vex lobbyplaystart <seconds>

/vex createteam <color> <team>
/vex setteambed <team> [x y z]
/vex setteamspawn <team> [x y z]
/vex setteamshop <team> <mobType> [x y z]
/vex setshopbox <mobType> [x y z]

/vex setgamebarrierpos1
/vex setgamebarrierpos2
```

For OneBlock:

```text
/vex oneblock iteminterval <seconds>
/vex oneblock itemspeed <seconds>
/vex oneblock itemamount <min> [max]
/vex oneblock barrier <size> [delaySeconds] [shrinkSeconds]
```

The `/vex` tab completer supplies modes, arena names, team names, the 12 team colors, mob types and boolean/language values. Coordinates are optional; when omitted, the player's current location is saved.

## Language and staff flight

```text
/vex lang <ru|en>
/fly [player] [true|false]
/flyspeed <0.001-10> [player]
```

`/fly` and `/flyspeed` can target another player only with `vex.fly.others` or an OWNER/ADMIN staff role. `/flyspeed` uses a 0.001–10 VexVol scale and maps it to the native Bukkit flight-speed range.

## Build

Use JDK 25 and Gradle 9.x. On Windows from the project directory:

```powershell
gradle clean build
```

The plugin JAR will be under `build\libs\`.

## Runtime dependencies

Multiverse-Core 5.x is compile-time integrated. VoidRP/LuckPerms/FAWE/WorldGuard and the other stack integrations remain optional boundaries from the existing fixed project.
