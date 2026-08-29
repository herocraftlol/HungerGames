package com.herocraft.hungergames.listener;

import com.herocraft.hungergames.HungerGamesPlugin;
import com.herocraft.hungergames.util.SpectatorItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Gère l'item "quitter le mode spectateur" (boussole) : clic droit/gauche pour
 * repartir, et interdiction de le lâcher ou de le déplacer dans l'inventaire.
 */
public class SpectatorListener implements Listener {

    private final HungerGamesPlugin plugin;

    public SpectatorListener(HungerGamesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!SpectatorItems.isLeaveItem(plugin, event.getItem())) return;
        event.setCancelled(true);

        if (plugin.getArenaManager().findSpectatorArenaOf(player).isEmpty()) {
            player.sendMessage(Component.text("Tu n'es pas en mode spectateur.", NamedTextColor.RED));
            return;
        }
        plugin.getArenaManager().unspectate(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent event) {
        if (SpectatorItems.isLeaveItem(plugin, event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (plugin.getArenaManager().findSpectatorArenaOf(player).isEmpty()) return;
        if (SpectatorItems.isLeaveItem(plugin, event.getCurrentItem())) {
            event.setCancelled(true);
        }
    }
}
