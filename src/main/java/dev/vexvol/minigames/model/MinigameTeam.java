package dev.vexvol.minigames.model;

import org.bukkit.ChatColor;
import org.bukkit.Location;

public final class MinigameTeam {
    private final String name;
    private ChatColor color;
    private Location bed;
    private Location spawn;
    private Location shop;
    private String shopType;

    public MinigameTeam(String name, ChatColor color) {
        this.name = name;
        this.color = color;
    }

    public String name() { return name; }
    public ChatColor color() { return color; }
    public Location bed() { return bed == null ? null : bed.clone(); }
    public Location spawn() { return spawn == null ? null : spawn.clone(); }
    public Location shop() { return shop == null ? null : shop.clone(); }
    public String shopType() { return shopType; }
    public void setColor(ChatColor value) { this.color = value; }
    public void setBed(Location value) { this.bed = value == null ? null : value.clone(); }
    public void setSpawn(Location value) { this.spawn = value == null ? null : value.clone(); }
    public void setShop(Location value) { this.shop = value == null ? null : value.clone(); }
    public void setShopType(String value) { this.shopType = value; }
}
