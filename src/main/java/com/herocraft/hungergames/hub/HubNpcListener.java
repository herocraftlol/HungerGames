package com.herocraft.hungergames.hub;

import com.herocraft.hungergames.HungerGamesPlugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * Fait s'ouvrir le bon GUI quand un joueur clique sur l'un des PNJ posés par
 * {@link HubBuilder} dans le lobby : le GUI des arènes pour les PNJ "rejoindre
 * une arène", le tableau des scores pour le PNJ dédié.
 */
public class HubNpcListener implements Listener {

    private final HungerGamesPlugin plugin;

    public HubNpcListener(HungerGamesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        if (entity.getPersistentDataContainer().has(HubBuilder.npcKey(plugin), PersistentDataType.BYTE)) {
            event.setCancelled(true);
            plugin.getArenaGUI().open(player);
            return;
        }
        if (entity.getPersistentDataContainer().has(HubBuilder.leaderboardNpcKey(plugin), PersistentDataType.BYTE)) {
            event.setCancelled(true);
            plugin.getLeaderboardGUI().open(player);
        }
    }
}
