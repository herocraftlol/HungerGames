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
 * Item donné aux spectateurs (boussole, slot 8) pour repartir du mode spectateur
 * d'un simple clic, sans avoir à taper de commande.
 */
public final class SpectatorItems {

    private SpectatorItems() {
    }

    private static NamespacedKey key(HungerGamesPlugin plugin) {
        return new NamespacedKey(plugin, "hg_spectator_leave");
    }

    public static ItemStack createLeaveItem(HungerGamesPlugin plugin) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Quitter le mode spectateur", NamedTextColor.YELLOW, TextDecoration.BOLD));
            meta.lore(java.util.List.of(Component.text("Clique pour repartir", NamedTextColor.GRAY)));
            meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isLeaveItem(HungerGamesPlugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(key(plugin), PersistentDataType.BYTE);
    }
}
