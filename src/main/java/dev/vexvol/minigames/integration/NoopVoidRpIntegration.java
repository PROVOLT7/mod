package dev.vexvol.minigames.integration;

import org.bukkit.entity.Player;

public final class NoopVoidRpIntegration implements VoidRpIntegration {
    @Override public boolean isAvailable() { return false; }
    @Override public ActionResult ban(Player source, String target, String duration, String reason) { return ActionResult.unavailable(); }
    @Override public ActionResult unban(Player source, String target, String reason) { return ActionResult.unavailable(); }
    @Override public ActionResult mute(Player source, String target, String duration, String reason) { return ActionResult.unavailable(); }
    @Override public ActionResult unmute(Player source, String target, String reason) { return ActionResult.unavailable(); }
}
