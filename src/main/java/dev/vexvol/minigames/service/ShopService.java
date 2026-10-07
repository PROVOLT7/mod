package dev.vexvol.minigames.service;

import dev.vexvol.minigames.model.MinigameArena;
import dev.vexvol.minigames.model.MinigameTeam;
import dev.vexvol.minigames.model.MinigameMode;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ShopService {
    private static final int SLOT_COUNT = 27;
    private final NamespacedKey productKey;
    private final Map<String, Product> products = new LinkedHashMap<>();

    public ShopService(JavaPlugin plugin) {
        productKey = new NamespacedKey(plugin, "shop-product");
        registerProducts();
    }

    public void openBaseShop(Player player, MinigameArena arena) {
        Inventory inv = org.bukkit.Bukkit.createInventory(null, SLOT_COUNT, "§8BedrockBox • Магазин базы");
        put(inv, 0, "pick_wood", Material.WOODEN_PICKAXE);
        put(inv, 1, "pick_stone", Material.STONE_PICKAXE);
        put(inv, 2, "pick_iron", Material.IRON_PICKAXE);
        put(inv, 3, "pick_gold", Material.GOLDEN_PICKAXE);
        put(inv, 4, "pick_diamond", Material.DIAMOND_PICKAXE);
        put(inv, 5, "pick_netherite", Material.NETHERITE_PICKAXE);
        put(inv, 9, "furnace", Material.FURNACE);
        put(inv, 10, "wood", Material.OAK_PLANKS);
        put(inv, 11, "netherrack", Material.NETHERRACK);
        put(inv, 12, "obsidian", Material.OBSIDIAN);
        put(inv, 14, "bread", Material.BREAD);
        put(inv, 15, "cooked_beef", Material.COOKED_BEEF);
        put(inv, 16, "bow", Material.BOW);
        put(inv, 17, "arrows", Material.ARROW);
        put(inv, 22, "shears", Material.SHEARS);
        put(inv, 26, "close", Material.BARRIER);
        player.openInventory(inv);
    }

    public void openCenterShop(Player player, MinigameArena arena) {
        Inventory inv = org.bukkit.Bukkit.createInventory(null, SLOT_COUNT, "§8BedrockBox • Центральный");
        put(inv, 0, "diamond_sword", Material.DIAMOND_SWORD);
        put(inv, 1, "diamond_helmet", Material.DIAMOND_HELMET);
        put(inv, 2, "diamond_chestplate", Material.DIAMOND_CHESTPLATE);
        put(inv, 3, "diamond_leggings", Material.DIAMOND_LEGGINGS);
        put(inv, 4, "diamond_boots", Material.DIAMOND_BOOTS);
        put(inv, 5, "netherite_sword", Material.NETHERITE_SWORD);
        put(inv, 6, "netherite_helmet", Material.NETHERITE_HELMET);
        put(inv, 7, "netherite_chestplate", Material.NETHERITE_CHESTPLATE);
        put(inv, 8, "netherite_leggings", Material.NETHERITE_LEGGINGS);
        put(inv, 9, "netherite_boots", Material.NETHERITE_BOOTS);
        put(inv, 12, "protection_2", Material.ENCHANTED_BOOK);
        put(inv, 13, "sharpness_2", Material.ENCHANTED_BOOK);
        put(inv, 14, "golden_apple", Material.GOLDEN_APPLE);
        put(inv, 26, "close", Material.BARRIER);
        player.openInventory(inv);
    }

    public boolean isShop(String title) { return title.startsWith("§8BedrockBox •"); }

    public void click(Player player, Inventory inventory, int slot) {
        ItemStack stack = slot >= 0 && slot < inventory.getSize() ? inventory.getItem(slot) : null;
        if (stack == null || !stack.hasItemMeta()) return;
        ItemMeta meta = stack.getItemMeta();
        String id = meta.getPersistentDataContainer().get(productKey, PersistentDataType.STRING);
        if (id == null || id.equals("close")) return;
        Product product = products.get(id); if (product == null) return;
        purchase(player, product);
    }

    private void purchase(Player player, Product product) {
        if (product.id.equals("protection_2")) {
            if (count(player, Material.EMERALD) < product.price) { player.sendMessage("§cНедостаточно изумрудов. Нужно: §a" + product.price); return; }
            boolean applied = false;
            ItemStack[] armor = {player.getInventory().getHelmet(), player.getInventory().getChestplate(), player.getInventory().getLeggings(), player.getInventory().getBoots()};
            for (ItemStack armorPiece : armor) {
                if (armorPiece == null || armorPiece.getType().isAir()) continue;
                ItemMeta meta = armorPiece.getItemMeta();
                if (meta != null) { meta.addEnchant(Enchantment.PROTECTION, 2, true); meta.setUnbreakable(true); armorPiece.setItemMeta(meta); applied = true; }
            }
            if (!applied) { player.sendMessage("§cНа вас нет брони."); return; }
            remove(player, Material.EMERALD, product.price);
            player.sendMessage("§aЗащита II применена к вашей броне.");
            return;
        }
        if (product.id.equals("sharpness_2")) {
            if (count(player, Material.EMERALD) < product.price) { player.sendMessage("§cНедостаточно изумрудов. Нужно: §a" + product.price); return; }
            ItemStack weapon = player.getInventory().getItemInMainHand();
            if (weapon == null || weapon.getType().isAir() || !weapon.getType().name().endsWith("SWORD")) { player.sendMessage("§cВозьмите меч в основную руку."); return; }
            ItemMeta meta = weapon.getItemMeta(); meta.addEnchant(Enchantment.SHARPNESS, 2, true); weapon.setItemMeta(meta);
            remove(player, Material.EMERALD, product.price); player.sendMessage("§aОстрота II применена к мечу."); return;
        }
        int emeralds = count(player, Material.EMERALD);
        if (emeralds < product.price) {
            player.sendMessage("§cНедостаточно изумрудов. Нужно: §a" + product.price);
            return;
        }
        ItemStack item = product.make(player);
        remove(player, Material.EMERALD, product.price);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        leftovers.values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        player.sendMessage("§aКуплено: §f" + product.display + " §7за §a" + product.price + " изумрудов.");
    }

    private int count(Player player, Material material) {
        int total = 0; for (ItemStack stack : player.getInventory().getStorageContents()) if (stack != null && stack.getType() == material) total += stack.getAmount(); return total;
    }
    private void remove(Player player, Material material, int amount) {
        int left = amount;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack == null || stack.getType() != material || left <= 0) continue;
            int take = Math.min(left, stack.getAmount()); stack.setAmount(stack.getAmount() - take); left -= take;
        }
    }
    private void put(Inventory inv, int slot, String id, Material fallback) {
        Product p = products.get(id); if (p == null) return;
        ItemStack item = new ItemStack(fallback);
        ItemMeta meta = item.getItemMeta(); meta.setDisplayName("§f" + p.display + " §8• §a" + p.price + " изумрудов"); meta.getPersistentDataContainer().set(productKey, PersistentDataType.STRING, id); meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); item.setItemMeta(meta); inv.setItem(slot, item);
    }

    private void registerProducts() {
        products.put("pick_wood", new Product("pick_wood", "Деревянная кирка", 1, Material.WOODEN_PICKAXE, 1));
        products.put("pick_stone", new Product("pick_stone", "Каменная кирка", 2, Material.STONE_PICKAXE, 1));
        products.put("pick_iron", new Product("pick_iron", "Железная кирка", 4, Material.IRON_PICKAXE, 1));
        products.put("pick_gold", new Product("pick_gold", "Золотая кирка", 6, Material.GOLDEN_PICKAXE, 1));
        products.put("pick_diamond", new Product("pick_diamond", "Алмазная кирка", 10, Material.DIAMOND_PICKAXE, 1));
        products.put("pick_netherite", new Product("pick_netherite", "Незеритовая кирка", 16, Material.NETHERITE_PICKAXE, 1));
        products.put("furnace", new Product("furnace", "Печь", 2, Material.FURNACE, 1));
        products.put("wood", new Product("wood", "Доски", 1, Material.OAK_PLANKS, 16));
        products.put("netherrack", new Product("netherrack", "Незерак", 1, Material.NETHERRACK, 16));
        products.put("obsidian", new Product("obsidian", "Обсидиан", 64, Material.OBSIDIAN, 1));
        products.put("bread", new Product("bread", "Хлеб", 1, Material.BREAD, 8));
        products.put("cooked_beef", new Product("cooked_beef", "Жареная говядина", 2, Material.COOKED_BEEF, 8));
        products.put("bow", new Product("bow", "Лук", 5, Material.BOW, 1));
        products.put("arrows", new Product("arrows", "Стрелы", 1, Material.ARROW, 16));
        products.put("shears", new Product("shears", "Ножницы", 3, Material.SHEARS, 1));
        products.put("diamond_sword", new Product("diamond_sword", "Алмазный меч", 12, Material.DIAMOND_SWORD, 1));
        products.put("diamond_helmet", new Product("diamond_helmet", "Алмазный шлем", 8, Material.DIAMOND_HELMET, 1));
        products.put("diamond_chestplate", new Product("diamond_chestplate", "Алмазный нагрудник", 12, Material.DIAMOND_CHESTPLATE, 1));
        products.put("diamond_leggings", new Product("diamond_leggings", "Алмазные поножи", 10, Material.DIAMOND_LEGGINGS, 1));
        products.put("diamond_boots", new Product("diamond_boots", "Алмазные ботинки", 7, Material.DIAMOND_BOOTS, 1));
        products.put("netherite_sword", new Product("netherite_sword", "Незеритовый меч", 24, Material.NETHERITE_SWORD, 1));
        products.put("netherite_helmet", new Product("netherite_helmet", "Незеритовый шлем", 16, Material.NETHERITE_HELMET, 1));
        products.put("netherite_chestplate", new Product("netherite_chestplate", "Незеритовый нагрудник", 24, Material.NETHERITE_CHESTPLATE, 1));
        products.put("netherite_leggings", new Product("netherite_leggings", "Незеритовые поножи", 20, Material.NETHERITE_LEGGINGS, 1));
        products.put("netherite_boots", new Product("netherite_boots", "Незеритовые ботинки", 14, Material.NETHERITE_BOOTS, 1));
        products.put("protection_2", new Product("protection_2", "Защита II", 8, Material.ENCHANTED_BOOK, 1) {
            @Override ItemStack make(Player player) { return new ItemStack(Material.ENCHANTED_BOOK); }
        });
        products.put("sharpness_2", new Product("sharpness_2", "Острота II", 8, Material.ENCHANTED_BOOK, 1) {
            @Override ItemStack make(Player player) { return new ItemStack(Material.ENCHANTED_BOOK); }
        });
        products.put("golden_apple", new Product("golden_apple", "Золотое яблоко", 6, Material.GOLDEN_APPLE, 2));
    }

    private static class Product {
        final String id; final String display; final int price; final Material material; final int amount;
        Product(String display, int price, Material material, int amount) { this("", display, price, material, amount); }
        Product(String id, String display, int price, Material material, int amount) { this.id=id;this.display=display;this.price=price;this.material=material;this.amount=amount; }
        ItemStack make(Player player) { ItemStack item = new ItemStack(material, amount); if (material.name().endsWith("HELMET") || material.name().endsWith("CHESTPLATE") || material.name().endsWith("LEGGINGS") || material.name().endsWith("BOOTS")) { ItemMeta meta = item.getItemMeta(); meta.setUnbreakable(true); item.setItemMeta(meta); } return item; }
    }
}
