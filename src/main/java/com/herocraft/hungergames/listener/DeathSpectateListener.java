package com.herocraft.hungergames.listener;

import com.herocraft.hungergames.HungerGamesPlugin;
import com.herocraft.hungergames.gui.DeathSpectateGUI;
import com.herocraft.hungergames.util.DeathItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * Gère l'item "suivre un joueur en vie" donné aux morts (clic droit → ouvre
 * {@link DeathSpectateGUI}) et les clics sur les têtes de ce GUI (téléportation).
 */
public class DeathSpectateListener implements Listener {

    private final HungerGamesPlugin plugin;

    public DeathSpectateListener(HungerGamesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!DeathItems.isTeleportItem(plugin, event.getItem())) return;
        event.setCancelled(true);

        plugin.getArenaManager().getArenaOf(player).ifPresentOrElse(arena -> {
            if (arena.isAlive(player)) {
                player.sendMessage(Component.text("Tu es encore en vie !", NamedTextColor.RED));
                return;
            }
            plugin.getDeathSpectateGUI().open(player, arena);
        }, () -> player.sendMessage(Component.text("Tu n'es dans aucune partie.", NamedTextColor.RED)));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = LegacyComponentSerializer.legacySection().serialize(event.getView().title());
        if (!DeathSpectateGUI.isTitle(title)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.PLAYER_HEAD || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        String rawUuid = meta.getPersistentDataContainer().get(DeathSpectateGUI.targetKey(plugin), PersistentDataType.STRING);
        if (rawUuid == null) return;

        UUID targetId;
        try {
            targetId = UUID.fromString(rawUuid);
        } catch (IllegalArgumentException e) {
            return;
        }

        Player target = Bukkit.getPlayer(targetId);
        boolean stillAlive = target != null && plugin.getArenaManager().getArenaOf(player)
                .map(arena -> arena.isAlive(target))
                .orElse(false);

        if (!stillAlive) {
            player.sendMessage(Component.text("Ce joueur n'est plus en vie.", NamedTextColor.RED));
            player.closeInventory();
            return;
        }

        player.teleport(target.getLocation());
        player.sendMessage(Component.text("Téléporté à " + target.getName() + ".", NamedTextColor.AQUA));
        player.closeInventory();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent event) {
        if (DeathItems.isTeleportItem(plugin, event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }
}
