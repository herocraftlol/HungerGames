package com.herocraft.hungergames.gui;

import com.herocraft.hungergames.HungerGamesPlugin;
import com.herocraft.hungergames.arena.Arena;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Écoute les clics dans le GUI d'arènes ({@link ArenaGUI}) :
 * - Clic sur une arène joignable → rejoint la partie
 * - Clic sur une arène en cours → passe en spectateur
 * - Clic sur le bouton "aléatoire" → rejoint/crée automatiquement une partie
 * - Clic sur les flèches → change de page
 */
public class ArenaGUIListener implements Listener {

    private final HungerGamesPlugin plugin;
    private final ArenaGUI arenaGUI;

    public ArenaGUIListener(HungerGamesPlugin plugin, ArenaGUI arenaGUI) {
        this.plugin = plugin;
        this.arenaGUI = arenaGUI;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = LegacyComponentSerializer.legacySection().serialize(event.getView().title());
        if (!ArenaGUI.isArenaGuiTitle(title)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) return;

        int slot = event.getRawSlot();
        int page = ArenaGUI.parsePageFromTitle(title);

        if (ArenaGUI.isPrevPageButton(slot)) {
            arenaGUI.open(player, page - 1);
            return;
        }
        if (ArenaGUI.isNextPageButton(slot)) {
            arenaGUI.open(player, page + 1);
            return;
        }
        if (ArenaGUI.isRandomButton(slot)) {
            player.closeInventory();
            plugin.getArenaManager().joinPlayer(player);
            return;
        }

        Arena arena = arenaGUI.getArenaAt(page, slot);
        if (arena == null) return;

        player.closeInventory();

        if (arena.isJoinable()) {
            plugin.getArenaManager().joinArena(player, arena);
        } else if (arena.isSpectatable()) {
            plugin.getArenaManager().spectateArena(player, arena);
        } else {
            player.sendMessage(Component.text("Cette partie n'est plus disponible.", NamedTextColor.RED));
        }
    }
}
