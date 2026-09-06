package com.herocraft.hungergames.hub;

import com.herocraft.hungergames.HungerGamesPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * Fait s'ouvrir le GUI des arènes ({@code /hg gui}) quand un joueur clique sur
 * l'un des PNJ posés par {@link HubBuilder} dans le lobby.
 */
public class HubNpcListener implements Listener {

    private final HungerGamesPlugin plugin;

    public HubNpcListener(HungerGamesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!event.getRightClicked().getPersistentDataContainer().has(HubBuilder.npcKey(plugin), PersistentDataType.BYTE)) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        plugin.getArenaGUI().open(player);
    }
}
