package com.herocraft.hungergames.util;

import com.herocraft.hungergames.HungerGamesPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Items donnés aux joueurs dans la salle d'attente d'une arène (avant le lancement
 * de la partie) : une épée en pierre pour choisir son kit, une barrière pour quitter.
 * Retirés automatiquement au lancement (l'inventaire est vidé avant de donner le kit).
 */
public final class WaitingRoomItems {

    private WaitingRoomItems() {
    }

    private static NamespacedKey kitKey(HungerGamesPlugin plugin) {
        return new NamespacedKey(plugin, "hg_waiting_kit_selector");
    }

    private static NamespacedKey leaveKey(HungerGamesPlugin plugin) {
        return new NamespacedKey(plugin, "hg_waiting_leave");
    }

    public static ItemStack createKitSelectorItem(HungerGamesPlugin plugin) {
        ItemStack item = new ItemStack(Material.STONE_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Choisir un kit", NamedTextColor.GREEN, TextDecoration.BOLD));
            meta.lore(java.util.List.of(Component.text("Clique pour ouvrir le menu des kits", NamedTextColor.GRAY)));
            meta.getPersistentDataContainer().set(kitKey(plugin), PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createLeaveItem(HungerGamesPlugin plugin) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Quitter la partie", NamedTextColor.RED, TextDecoration.BOLD));
            meta.lore(java.util.List.of(Component.text("Clique pour quitter (/hg leave)", NamedTextColor.GRAY)));
            meta.getPersistentDataContainer().set(leaveKey(plugin), PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isKitSelectorItem(HungerGamesPlugin plugin, ItemStack item) {
        return hasKey(plugin, item, kitKey(plugin));
    }

    public static boolean isLeaveItem(HungerGamesPlugin plugin, ItemStack item) {
        return hasKey(plugin, item, leaveKey(plugin));
    }

    private static boolean hasKey(HungerGamesPlugin plugin, ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}
