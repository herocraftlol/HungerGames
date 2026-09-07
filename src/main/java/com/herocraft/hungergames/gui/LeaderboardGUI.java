package com.herocraft.hungergames.gui;

import com.herocraft.hungergames.HungerGamesPlugin;
import com.herocraft.hungergames.stats.StatsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Tableau des scores du hub : classement des joueurs par nombre de victoires,
 * sous forme de têtes de joueurs (voir {@code /hg top}).
 */
public class LeaderboardGUI {

    public static final String TITLE = "§6§l🏆 Tableau des scores";

    private static final int SIZE = 27;

    private final HungerGamesPlugin plugin;

    public LeaderboardGUI(HungerGamesPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        player.openInventory(build());
    }

    public Inventory build() {
        Inventory inv = Bukkit.createInventory(null, SIZE, LegacyComponentSerializer.legacySection().deserialize(TITLE));
        List<StatsManager.Entry> top = plugin.getStatsManager().getTop(SIZE);

        if (top.isEmpty()) {
            ItemStack placeholder = new ItemStack(Material.BARRIER);
            ItemMeta meta = placeholder.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("Aucune victoire enregistrée pour le moment", NamedTextColor.GRAY));
                placeholder.setItemMeta(meta);
            }
            inv.setItem(SIZE / 2, placeholder);
            return inv;
        }

        for (int i = 0; i < top.size() && i < SIZE; i++) {
            inv.setItem(i, buildHeadItem(i + 1, top.get(i)));
        }
        return inv;
    }

    private ItemStack buildHeadItem(int rank, StatsManager.Entry entry) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return item;

        OfflinePlayer owner = Bukkit.getOfflinePlayer(entry.name);
        meta.setOwningPlayer(owner);

        String medal = switch (rank) {
            case 1 -> "§6🥇";
            case 2 -> "§7🥈";
            case 3 -> "§c🥉";
            default -> "§7#" + rank;
        };
        meta.displayName(LegacyComponentSerializer.legacySection().deserialize(medal + " §f" + entry.name));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Victoires : ", NamedTextColor.GRAY)
                .append(Component.text(entry.wins, NamedTextColor.GREEN, TextDecoration.BOLD)));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isLeaderboardTitle(String title) {
        return title != null && title.equals(TITLE);
    }
}
