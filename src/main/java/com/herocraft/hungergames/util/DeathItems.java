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
 * Item donné aux joueurs morts (désormais spectateurs de leur propre arène) pour
 * ouvrir la liste des joueurs encore en vie et se téléporter à l'un d'eux
 * (voir {@code com.herocraft.hungergames.gui.DeathSpectateGUI}).
 */
public final class DeathItems {

    private DeathItems() {
    }

    private static NamespacedKey key(HungerGamesPlugin plugin) {
        return new NamespacedKey(plugin, "hg_death_teleport");
    }

    public static ItemStack createTeleportItem(HungerGamesPlugin plugin) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Suivre un joueur en vie", NamedTextColor.AQUA, TextDecoration.BOLD));
            meta.lore(java.util.List.of(Component.text("Clique pour te téléporter à un joueur", NamedTextColor.GRAY)));
            meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isTeleportItem(HungerGamesPlugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(key(plugin), PersistentDataType.BYTE);
    }
}
