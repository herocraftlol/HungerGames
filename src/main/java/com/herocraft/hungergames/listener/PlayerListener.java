package com.herocraft.hungergames.listener;

import com.herocraft.hungergames.HungerGamesPlugin;
import com.herocraft.hungergames.arena.Arena;
import com.herocraft.hungergames.kit.KitSelectorGUI;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.List;

public class PlayerListener implements Listener {

    private final HungerGamesPlugin plugin;

    public PlayerListener(HungerGamesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.getArenaManager().getArenaOf(player).isEmpty()) {
            plugin.getArenaManager().sendToHub(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getArenaManager().handleDisconnect(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        event.setRespawnLocation(plugin.getArenaManager().getHubLocation());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = LegacyComponentSerializer.legacySection().serialize(event.getView().title());
        if (!title.equals(KitSelectorGUI.TITLE)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        List<String> kitIds = plugin.getKitSelectorGUI().orderedKitIds();
        if (slot >= kitIds.size()) return;
        String kitId = kitIds.get(slot);

        plugin.getArenaManager().getArenaOf(player).ifPresentOrElse(arena -> {
            if (arena.getState() == com.herocraft.hungergames.arena.ArenaState.GRACE_PERIOD
                    || arena.getState() == com.herocraft.hungergames.arena.ArenaState.PVP
                    || arena.getState() == com.herocraft.hungergames.arena.ArenaState.ENDED) {
                player.sendMessage(net.kyori.adventure.text.Component.text("Trop tard, la partie a déjà commencé.",
                        net.kyori.adventure.text.format.NamedTextColor.RED));
            } else {
                arena.selectKit(player, kitId);
            }
        }, () -> player.sendMessage(net.kyori.adventure.text.Component.text("Rejoins d'abord une partie avec /hg join.",
                net.kyori.adventure.text.format.NamedTextColor.RED)));

        player.closeInventory();
    }
}
