package dev.vexvol.minigames.service;

import dev.vexvol.minigames.model.Arena;
import dev.vexvol.minigames.model.GameSession;
import org.bukkit.entity.Player;

import java.util.*;

public final class MatchmakingService {
    private final ArenaService arenas;
    private final GameSessionService sessions;
    private final Map<String, Deque<UUID>> queues = new HashMap<>();

    public MatchmakingService(ArenaService arenas, GameSessionService sessions) {
        this.arenas = arenas;
        this.sessions = sessions;
    }

    public void enqueue(String gameType, Player player) {
        queues.computeIfAbsent(gameType.toLowerCase(Locale.ROOT), ignored -> new ArrayDeque<>()).add(player.getUniqueId());
    }

    public void leaveQueue(String gameType, UUID player) {
        Deque<UUID> queue = queues.get(gameType.toLowerCase(Locale.ROOT));
        if (queue != null) queue.remove(player);
    }

    public GameSession tryMatch(String gameType) {
        Arena candidate = arenas.all().stream()
            .filter(Arena::enabled)
            .findFirst().orElse(null);
        if (candidate == null) return null;
        Deque<UUID> queue = queues.get(gameType.toLowerCase(Locale.ROOT));
        if (queue == null || queue.isEmpty()) return null;
        GameSession session = sessions.create(candidate);
        while (!queue.isEmpty()) session.addPlayer(queue.pollFirst());
        return session;
    }
}
