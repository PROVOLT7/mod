package dev.vexvol.minigames.model;

public enum MinigameMode {
    BEDROCKBOX("bedrockbox"),
    ONEBLOCK("oneblock");

    private final String id;
    MinigameMode(String id) { this.id = id; }
    public String id() { return id; }

    public static MinigameMode parse(String value) {
        for (MinigameMode mode : values()) if (mode.id.equalsIgnoreCase(value) || mode.name().equalsIgnoreCase(value)) return mode;
        return null;
    }
}
