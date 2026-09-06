package com.herocraft.hungergames.listener;

import com.herocraft.hungergames.HungerGamesPlugin;
import com.herocraft.hungergames.arena.Arena;
import com.herocraft.hungergames.arena.ArenaState;
import com.herocraft.hungergames.util.WaitingRoomItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Gère les deux items donnés dans la salle d'attente d'une arène : l'épée en
 * pierre (choisir un kit) et la barrière (quitter), avec protection contre le
 * drop/déplacement dans l'inventaire, comme pour la boussole des spectateurs.
 */
public class WaitingRoomListener implements Listener {

    private final HungerGamesPlugin plugin;

    public WaitingRoomListener(HungerGamesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (WaitingRoomItems.isKitSelectorItem(plugin, item)) {
            event.setCancelled(true);
            openKitSelector(player);
            return;
        }
        if (WaitingRoomItems.isLeaveItem(plugin, item)) {
            event.setCancelled(true);
            leaveOrUnspectate(player);
        }
    }

    private void openKitSelector(Player player) {
        plugin.getArenaManager().getArenaOf(player).ifPresentOrElse(arena -> {
            ArenaState state = arena.getState();
            if (state == ArenaState.GRACE_PERIOD || state == ArenaState.PVP || state == ArenaState.ENDED) {
                player.sendMessage(Component.text("Trop tard, la partie a déjà commencé.", NamedTextColor.RED));
            } else {
                player.openInventory(plugin.getKitSelectorGUI().build(player));
            }
        }, () -> player.sendMessage(Component.text("Rejoins d'abord une partie avec /hg join.", NamedTextColor.RED)));
    }

    private void leaveOrUnspectate(Player player) {
        if (plugin.getArenaManager().getArenaOf(player).isPresent()) {
            plugin.getArenaManager().leavePlayer(player);
        } else if (plugin.getArenaManager().findSpectatorArenaOf(player).isPresent()) {
            plugin.getArenaManager().unspectate(player);
        } else {
            player.sendMessage(Component.text("Tu n'es dans aucune partie.", NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (WaitingRoomItems.isKitSelectorItem(plugin, item) || WaitingRoomItems.isLeaveItem(plugin, item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        if (WaitingRoomItems.isKitSelectorItem(plugin, current) || WaitingRoomItems.isLeaveItem(plugin, current)) {
            event.setCancelled(true);
        }
    }
}
