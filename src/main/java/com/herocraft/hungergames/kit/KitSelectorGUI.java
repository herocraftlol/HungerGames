package com.herocraft.hungergames.kit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Construit l'inventaire de sélection de kit et garde en mémoire, pour chaque
 * inventaire ouvert, la correspondance slot -> id de kit (utilisé par le listener
 * de clic pour savoir quel kit a été choisi).
 *
 * Les noms de kits sont interprétés avec les codes couleur '&' (format Bukkit
 * classique, utilisé dans kits.yml), pas '§' — {@code &6Bucheron} s'affiche donc
 * bien en couleur au lieu du texte brut.
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
                meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(kit.getDisplayName()));

                List<Component> lore = new ArrayList<>();
                if (kit.getItems().isEmpty()) {
                    lore.add(Component.text("(kit vide)", NamedTextColor.DARK_GRAY));
                } else {
                    lore.add(Component.text("Contenu :", NamedTextColor.GRAY));
                    for (ItemStack content : kit.getItems()) {
                        lore.add(Component.text("- ", NamedTextColor.GRAY)
                                .append(Component.text(prettify(content.getType()), NamedTextColor.WHITE))
                                .append(Component.text(" x" + content.getAmount(), NamedTextColor.GRAY)));
                    }
                }
                lore.add(Component.empty());
                lore.add(Component.text("Clique pour sélectionner ce kit", NamedTextColor.YELLOW));
                meta.lore(lore);

                icon.setItemMeta(meta);
            }
            inv.setItem(slot, icon);
            slot++;
        }
        return inv;
    }

    /** Retourne, dans l'ordre d'affichage, la liste des ids de kits (index = slot). */
    public List<String> orderedKitIds() {
        return new ArrayList<>(kitManager.getAll().keySet());
    }

    /** Transforme STONE_SWORD en "Stone Sword" pour un affichage lisible. */
    private static String prettify(Material material) {
        String[] parts = material.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
