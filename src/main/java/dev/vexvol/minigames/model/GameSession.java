package dev.vexvol.minigames.model;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class GameSession {
    public enum State { WAITING, RUNNING, ENDING, STOPPED }

    private final long id;
    private final String arenaId;
    private State state;
    private final Set<UUID> players = new LinkedHashSet<>();

    public GameSession(long id, String arenaId) {
        this.id = id;
        this.arenaId = arenaId;
        this.state = State.WAITING;
    }

    public long id() { return id; }
    public String arenaId() { return arenaId; }
    public State state() { return state; }
    public void setState(State state) { this.state = state; }
    public Set<UUID> players() { return Set.copyOf(players); }
    public boolean addPlayer(UUID uuid) { return players.add(uuid); }
    public boolean removePlayer(UUID uuid) { return players.remove(uuid); }
}
