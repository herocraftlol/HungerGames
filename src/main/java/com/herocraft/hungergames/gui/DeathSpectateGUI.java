package com.herocraft.hungergames.gui;

import com.herocraft.hungergames.HungerGamesPlugin;
import com.herocraft.hungergames.arena.Arena;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * GUI ouvert par les joueurs morts d'une arène pour se téléporter à un joueur
 * encore en vie (voir l'item boussole donné à la mort, {@code DeathItems}).
 * Chaque tête est étiquetée avec l'UUID du joueur ciblé (donnée persistante),
 * lu par {@code DeathSpectateListener} au clic.
 */
public class DeathSpectateGUI {

    public static final String TITLE = "§8☠ Joueurs encore en vie";

    private final HungerGamesPlugin plugin;

    public DeathSpectateGUI(HungerGamesPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer, Arena arena) {
        viewer.openInventory(build(arena));
    }

    public Inventory build(Arena arena) {
        List<Player> alive = arena.getAlivePlayersOnline();
        int size = Math.max(9, (((alive.size() - 1) / 9) + 1) * 9);
        Inventory inv = Bukkit.createInventory(null, size, LegacyComponentSerializer.legacySection().deserialize(TITLE));

        if (alive.isEmpty()) {
            ItemStack placeholder = new ItemStack(Material.BARRIER);
            ItemMeta meta = placeholder.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("Plus aucun joueur en vie", NamedTextColor.GRAY));
                placeholder.setItemMeta(meta);
            }
            inv.setItem(0, placeholder);
            return inv;
        }

        int slot = 0;
        for (Player target : alive) {
            inv.setItem(slot, buildHeadItem(target));
            slot++;
        }
        return inv;
    }

    private ItemStack buildHeadItem(Player target) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return item;

        meta.setOwningPlayer(target);
        meta.displayName(Component.text(target.getName(), NamedTextColor.GREEN, TextDecoration.BOLD));
        meta.lore(List.of(
                Component.empty(),
                Component.text("▶ Clique pour te téléporter", NamedTextColor.YELLOW)
        ));
        meta.getPersistentDataContainer().set(targetKey(plugin), PersistentDataType.STRING, target.getUniqueId().toString());

        item.setItemMeta(meta);
        return item;
    }

    public static NamespacedKey targetKey(HungerGamesPlugin plugin) {
        return new NamespacedKey(plugin, "hg_teleport_target");
    }

    public static boolean isTitle(String title) {
        return title != null && title.equals(TITLE);
    }
}
