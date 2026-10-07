package dev.vexvol.minigames.integration;

import org.bukkit.entity.Player;

/**
 * Boundary for VoidRP. The VexVol core intentionally never touches VoidRP SQL.
 * A real adapter can implement this interface when the VoidRP API contract is available.
 */
public interface VoidRpIntegration {
    boolean isAvailable();

    ActionResult ban(Player source, String target, String duration, String reason);
    ActionResult unban(Player source, String target, String reason);
    ActionResult mute(Player source, String target, String duration, String reason);
    ActionResult unmute(Player source, String target, String reason);

    record ActionResult(boolean success, String message) {
        public static ActionResult unavailable() {
            return new ActionResult(false, "VoidRP API adapter is not configured.");
        }
        public static ActionResult ok() {
            return new ActionResult(true, "ok");
        }
    }
}
