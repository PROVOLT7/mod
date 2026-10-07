package dev.vexvol.minigames.model;

import java.util.UUID;

public record ArenaScope(UUID playerId, String arenaId, long expiresAtEpochMillis) {
    public boolean permanent() { return expiresAtEpochMillis <= 0; }
    public boolean expired(long now) { return !permanent() && expiresAtEpochMillis <= now; }
}
