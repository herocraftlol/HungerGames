package com.herocraft.hungergames.kit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Construit l'inventaire de sélection de kit et garde en mémoire, pour chaque
 * inventaire ouvert, la correspondance slot -> id de kit (utilisé par le listener
 * de clic pour savoir quel kit a été choisi).
 */
public class KitSelectorGUI {

    public static final String TITLE = "§8Choisis ton kit";

    private final KitManager kitManager;

    public KitSelectorGUI(KitManager kitManager) {
        this.kitManager = kitManager;
    }

    public Inventory build(Player player) {
        Map<String, Kit> kits = kitManager.getAll();
        int size = Math.max(9, ((kits.size() / 9) + 1) * 9);
        Inventory inv = org.bukkit.Bukkit.createInventory(null, size, LegacyComponentSerializer.legacySection().deserialize(TITLE));

        int slot = 0;
        for (Kit kit : kits.values()) {
            ItemStack icon = kit.getIcon().clone();
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.displayName(LegacyComponentSerializer.legacySection().deserialize(kit.getDisplayName()));
                meta.lore(java.util.List.of(Component.text("§7Clique pour sélectionner ce kit")));
                icon.setItemMeta(meta);
            }
            inv.setItem(slot, icon);
            slot++;
        }
        return inv;
    }

    /** Retourne, dans l'ordre d'affichage, la liste des ids de kits (index = slot). */
    public java.util.List<String> orderedKitIds() {
        return new java.util.ArrayList<>(kitManager.getAll().keySet());
    }
}
