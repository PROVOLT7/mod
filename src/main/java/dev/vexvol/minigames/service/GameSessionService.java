package dev.vexvol.minigames.service;

import dev.vexvol.minigames.model.Arena;
import dev.vexvol.minigames.model.GameSession;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class GameSessionService {
    private final Map<Long, GameSession> sessions = new LinkedHashMap<>();
    private long nextId = 1L;

    public GameSession create(Arena arena) {
        GameSession session = new GameSession(nextId++, arena.id());
        sessions.put(session.id(), session);
        return session;
    }

    public GameSession get(long id) { return sessions.get(id); }

    public Collection<GameSession> all() { return sessions.values(); }

    public Optional<GameSession> findByPlayer(java.util.UUID uuid) {
        return sessions.values().stream().filter(s -> s.players().contains(uuid)).findFirst();
    }

    public boolean join(GameSession session, Player player) {
        if (session == null || session.state() == GameSession.State.STOPPED) return false;
        findByPlayer(player.getUniqueId()).ifPresent(old -> old.removePlayer(player.getUniqueId()));
        return session.addPlayer(player.getUniqueId());
    }

    public boolean leave(GameSession session, Player player) {
        return session != null && session.removePlayer(player.getUniqueId());
    }

    public void start(GameSession session) {
        if (session != null) session.setState(GameSession.State.RUNNING);
    }

    public void stop(GameSession session) {
        if (session != null) session.setState(GameSession.State.STOPPED);
    }

    public GameSession restart(GameSession session, Arena arena) {
        if (session != null) stop(session);
        GameSession fresh = create(arena);
        return fresh;
    }

    public void removeStopped() {
        sessions.values().removeIf(s -> s.state() == GameSession.State.STOPPED && s.players().isEmpty());
    }
}
