package dev.vexvol.minigames.model;

import java.util.Objects;

public final class MapTemplate {
    private final String id;
    private String gameType;
    private String worldName;
    private boolean buildAllowed;

    public MapTemplate(String id, String gameType, String worldName, boolean buildAllowed) {
        this.id = Objects.requireNonNull(id);
        this.gameType = gameType;
        this.worldName = worldName;
        this.buildAllowed = buildAllowed;
    }

    public String id() { return id; }
    public String gameType() { return gameType; }
    public String worldName() { return worldName; }
    public boolean buildAllowed() { return buildAllowed; }
    public void setGameType(String gameType) { this.gameType = gameType; }
    public void setWorldName(String worldName) { this.worldName = worldName; }
    public void setBuildAllowed(boolean buildAllowed) { this.buildAllowed = buildAllowed; }
}
