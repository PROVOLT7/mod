package dev.vexvol.minigames.service;

import dev.vexvol.minigames.model.MinigameArena;
import dev.vexvol.minigames.model.MinigameMode;
import dev.vexvol.minigames.model.MinigameTeam;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;

public final class ScoreboardService {
    public void showLobby(Player player, MinigameArena arena, int players, int max) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = objective(board, title(arena.mode()));
        line(objective, "§7Игроки: §f" + players + "§7/§f" + max, 10);
        line(objective, "§8----------------", 9);
        line(objective, arena.mode() == MinigameMode.BEDROCKBOX ? "§aBedrockBox" : "§bOneBlock", 8);
        line(objective, "§7Ожидание...", 7);
        line(objective, "§8VexVol", 6);
        player.setScoreboard(board);
    }

    public void showBedrock(Player player, MinigameArena arena, java.util.Map<String, Boolean> bedState, int emeralds, long countdown) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = objective(board, title(MinigameMode.BEDROCKBOX));
        int score = 15;
        if (countdown > 0) line(objective, "§eСтарт: §f" + countdown + "с", score--);
        line(objective, "§7Изумруды: §a" + emeralds, score--);
        line(objective, "§8 ", score--);
        List<MinigameTeam> teams = arena.teams().values().stream().toList();
        for (MinigameTeam team : teams) {
            Boolean alive = bedState.get(team.name().toLowerCase(java.util.Locale.ROOT));
            String mark = Boolean.TRUE.equals(alive) ? "§a✓" : "§c✗";
            line(objective, team.color() + team.name() + " §8| " + mark, score--);
        }
        line(objective, "§8VexVol", score);
        player.setScoreboard(board);
    }

    public void showOneBlock(Player player, MinigameArena arena, int aliveTeams, long remainingBorderSeconds, long elapsedSeconds, int itemsDelivered) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = objective(board, title(MinigameMode.ONEBLOCK));
        int score = 10;
        if (remainingBorderSeconds >= 0) line(objective, "§cБарьер: §f" + format(remainingBorderSeconds), score--);
        else line(objective, "§cБарьер: §fОЖИДАНИЕ", score--);
        line(objective, "§7Команд: §f" + aliveTeams, score--);
        line(objective, "§7Время: §f" + format(elapsedSeconds), score--);
        line(objective, "§7Предметов: §f" + itemsDelivered, score--);
        line(objective, "§8 ", score--);
        line(objective, "§8VexVol", score);
        player.setScoreboard(board);
    }

    public void clear(Player player) { player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard()); }

    private Objective objective(Scoreboard board, String title) {
        Objective o = board.registerNewObjective("vex_mg", Criteria.DUMMY, org.bukkit.Bukkit.getServer().getName().isEmpty() ? title : title, RenderType.INTEGER);
        o.setDisplaySlot(DisplaySlot.SIDEBAR);
        return o;
    }
    private void line(Objective objective, String text, int score) { objective.getScore(text + "§" + Math.max(0, score)).setScore(score); }
    private String title(MinigameMode mode) { return mode == MinigameMode.BEDROCKBOX ? "§cBEDROCKBOX" : "§bONEBLOCK"; }
    private String format(long seconds) {
        Duration d = Duration.ofSeconds(Math.max(0, seconds));
        return String.format("%02d:%02d", d.toMinutesPart(), d.toSecondsPart());
    }
}
