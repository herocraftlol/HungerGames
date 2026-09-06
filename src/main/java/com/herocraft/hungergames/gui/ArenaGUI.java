package com.herocraft.hungergames.gui;

import com.herocraft.hungergames.HungerGamesPlugin;
import com.herocraft.hungergames.arena.Arena;
import com.herocraft.hungergames.arena.ArenaState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GUI listant toutes les zones de Hunger Games créées par les administrateurs
 * (voir {@code /hgadmin zone create}) et permettant :
 * - de rejoindre une zone ouverte (WAITING/STARTING, pas pleine — PAS pendant
 *   le préchargement, voir {@link Arena#isJoinable()})
 * - de regarder une zone déjà lancée en mode spectateur (GRACE_PERIOD/PVP)
 * - via un bouton dédié, de rejoindre directement une zone disponible au hasard
 *
 * Le contenu de l'inventaire est réactualisé en direct pour tous les joueurs
 * qui l'ont ouvert (voir {@link #refreshOpenViewers()}, appelé chaque seconde
 * par une tâche répétitive), sans fermer/rouvrir leur inventaire.
 *
 * L'ordre d'affichage suit l'ordre de création des zones (voir
 * {@link com.herocraft.hungergames.arena.ArenaManager#getArenasOrdered()}), et
 * chaque zone est identifiée par sa position dans cette liste pour un clic donné.
 */
public class ArenaGUI {

    public static final String TITLE_BASE = "§4§l⚔ Parties Hunger Games";

    private static final int GUI_SIZE = 54;
    private static final int PAGE_SIZE = 45;
    private static final int SLOT_PREV_PAGE = 45;
    private static final int SLOT_RANDOM_START = 46;
    private static final int SLOT_RANDOM_END = 52;
    private static final int SLOT_NEXT_PAGE = 53;

    private static final Pattern PAGE_PATTERN = Pattern.compile("\\((\\d+)/(\\d+)\\)");

    private final HungerGamesPlugin plugin;

    public ArenaGUI(HungerGamesPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        player.openInventory(buildInventory(page));
    }

    public Inventory buildInventory(int page) {
        List<Arena> arenas = plugin.getArenaManager().getArenasOrdered();
        int totalPages = Math.max(1, (int) Math.ceil(arenas.size() / (double) PAGE_SIZE));
        int clampedPage = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = Bukkit.createInventory(null, GUI_SIZE,
                LegacyComponentSerializer.legacySection().deserialize(titleFor(clampedPage, totalPages)));
        populateInventory(inv, clampedPage);
        return inv;
    }

    /**
     * Réécrit le contenu d'un inventaire déjà ouvert (sans le remplacer) pour
     * refléter l'état actuel des zones. Utilisé aussi bien à l'ouverture qu'au
     * rafraîchissement périodique.
     */
    private void populateInventory(Inventory inv, int page) {
        List<Arena> arenas = plugin.getArenaManager().getArenasOrdered();

        int start = page * PAGE_SIZE;
        int end = Math.min(arenas.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            inv.setItem(i - start, buildArenaItem(arenas.get(i)));
        }

        ItemStack filler = buildFiller();
        for (int i = end - start; i < PAGE_SIZE; i++) {
            inv.setItem(i, filler);
        }

        int totalPages = Math.max(1, (int) Math.ceil(arenas.size() / (double) PAGE_SIZE));
        inv.setItem(SLOT_PREV_PAGE, page > 0 ? buildPageButton(false) : filler);
        ItemStack randomBtn = buildRandomButton(arenas);
        for (int i = SLOT_RANDOM_START; i <= SLOT_RANDOM_END; i++) {
            inv.setItem(i, randomBtn);
        }
        inv.setItem(SLOT_NEXT_PAGE, page < totalPages - 1 ? buildPageButton(true) : filler);
    }

    /**
     * Rafraîchit en direct le contenu de ce GUI pour tous les joueurs qui l'ont
     * actuellement ouvert (nombre de joueurs, état, nombre de zones...), sans
     * fermer leur inventaire. À appeler périodiquement (voir HungerGamesPlugin).
     */
    public void refreshOpenViewers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String title = LegacyComponentSerializer.legacySection().serialize(player.getOpenInventory().title());
            if (!isArenaGuiTitle(title)) continue;
            int page = parsePageFromTitle(title);
            populateInventory(player.getOpenInventory().getTopInventory(), page);
        }
    }

    private String titleFor(int page, int totalPages) {
        if (totalPages <= 1) return TITLE_BASE;
        return TITLE_BASE + "§r§7 (" + (page + 1) + "/" + totalPages + ")";
    }

    private ItemStack buildArenaItem(Arena arena) {
        ArenaState state = arena.getState();
        Material mat;
        String displayName;
        String statusLine;
        String statusColor;

        boolean joinable = arena.isJoinable();
        boolean spectatable = arena.isSpectatable();

        if (joinable) {
            mat = Material.LIME_STAINED_GLASS_PANE;
            displayName = "§a§l✔ " + arena.getName();
            statusLine = "§aEn attente de joueurs";
            statusColor = "§a";
        } else if (spectatable) {
            mat = Material.RED_STAINED_GLASS_PANE;
            displayName = "§c§l⚔ " + arena.getName();
            statusLine = "§cPartie en cours";
            statusColor = "§c";
        } else if (state == ArenaState.PRELOADING) {
            mat = Material.YELLOW_STAINED_GLASS_PANE;
            displayName = "§e§l⏳ " + arena.getName();
            statusLine = "§eChargement de la zone...";
            statusColor = "§e";
        } else {
            mat = Material.GRAY_STAINED_GLASS_PANE;
            displayName = "§7✖ " + arena.getName();
            statusLine = "§7Indisponible";
            statusColor = "§7";
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(LegacyComponentSerializer.legacySection().deserialize(displayName));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(LegacyComponentSerializer.legacySection().deserialize(
                "§7Joueurs : " + statusColor + arena.getPlayers().size() + "§8/§7" + arena.getMaxPlayers()));
        lore.add(LegacyComponentSerializer.legacySection().deserialize("§7Statut  : " + statusLine));
        if (spectatable) {
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§7Spectateurs : §b" + arena.getSpectatorCount()));
        }
        lore.add(Component.empty());
        if (joinable) {
            lore.add(Component.text("▶ Clique pour rejoindre !", NamedTextColor.YELLOW));
        } else if (spectatable) {
            lore.add(Component.text("\uD83D\uDC41 Clique pour regarder en spectateur !", NamedTextColor.AQUA));
        } else if (state == ArenaState.PRELOADING) {
            lore.add(Component.text("Pas encore rejoignable, reviens dans un instant.", NamedTextColor.YELLOW));
        } else {
            lore.add(Component.text("✖ Indisponible", NamedTextColor.RED));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildRandomButton(List<Arena> arenas) {
        long joinableCount = arenas.stream().filter(Arena::isJoinable).count();

        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.text("✦ Rejoindre une partie aléatoire", NamedTextColor.GOLD, TextDecoration.BOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Te place automatiquement dans une", NamedTextColor.GRAY));
        lore.add(Component.text("zone disponible parmi celles créées", NamedTextColor.GRAY));
        lore.add(Component.text("par les administrateurs.", NamedTextColor.GRAY));
        lore.add(Component.empty());
        if (joinableCount > 0) {
            lore.add(Component.text(joinableCount + " partie(s) disponible(s)", NamedTextColor.GREEN));
            lore.add(Component.empty());
            lore.add(Component.text("▶ Clique pour jouer !", NamedTextColor.YELLOW));
        } else {
            lore.add(Component.text("Aucune partie ouverte pour le moment.", NamedTextColor.RED));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildPageButton(boolean next) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(next ? "Page suivante ▶" : "◀ Page précédente", NamedTextColor.YELLOW, TextDecoration.BOLD));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildFiller() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Retourne l'arène au slot cliqué pour une page donnée, ou null si hors zone/vide. */
    public Arena getArenaAt(int page, int slot) {
        if (slot < 0 || slot >= PAGE_SIZE) return null;
        List<Arena> arenas = plugin.getArenaManager().getArenasOrdered();
        int index = page * PAGE_SIZE + slot;
        if (index < 0 || index >= arenas.size()) return null;
        return arenas.get(index);
    }

    public static boolean isRandomButton(int slot) {
        return slot >= SLOT_RANDOM_START && slot <= SLOT_RANDOM_END;
    }

    public static boolean isPrevPageButton(int slot) {
        return slot == SLOT_PREV_PAGE;
    }

    public static boolean isNextPageButton(int slot) {
        return slot == SLOT_NEXT_PAGE;
    }

    public static boolean isArenaGuiTitle(String title) {
        return title != null && title.startsWith(TITLE_BASE);
    }

    public static int parsePageFromTitle(String title) {
        if (title == null) return 0;
        Matcher matcher = PAGE_PATTERN.matcher(title);
        if (!matcher.find()) return 0;
        try {
            return Math.max(0, Integer.parseInt(matcher.group(1)) - 1);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
