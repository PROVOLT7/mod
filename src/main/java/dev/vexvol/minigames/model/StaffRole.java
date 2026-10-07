package dev.vexvol.minigames.model;

public enum StaffRole {
    OWNER(4),
    ADMIN(3),
    MODERATOR(2),
    BUILDER(1),
    SPONSOR(0),
    DEFAULT(0);

    private final int level;

    StaffRole(int level) {
        this.level = level;
    }

    public int level() {
        return level;
    }

    public boolean atLeast(StaffRole other) {
        return level >= other.level;
    }
}
