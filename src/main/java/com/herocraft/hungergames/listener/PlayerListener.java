package com.herocraft.hungergames.listener;

import com.herocraft.hungergames.HungerGamesPlugin;
import com.herocraft.hungergames.arena.Arena;
import com.herocraft.hungergames.arena.ArenaState;
import com.herocraft.hungergames.gui.LeaderboardGUI;
import com.herocraft.hungergames.kit.KitSelectorGUI;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.List;
import java.util.Optional;

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

    /**
     * Garde la barre de faim pleine en permanence tant qu'aucune partie n'est
     * activement en cours pour ce joueur : au hub, dans une salle d'attente
     * d'arène (avant le scatter), ou en spectateur. Seuls le farm/la période
     * de grâce et le PVP laissent la faim évoluer normalement.
     */
    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Optional<Arena> arenaOpt = plugin.getArenaManager().getArenaOf(player);
        boolean activeRound = arenaOpt.isPresent()
                && (arenaOpt.get().getState() == ArenaState.GRACE_PERIOD || arenaOpt.get().getState() == ArenaState.PVP);
        if (activeRound) {
            return;
        }
        event.setCancelled(true);
        if (player.getFoodLevel() < 20) {
            player.setFoodLevel(20);
        }
        player.setSaturation(20f);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = LegacyComponentSerializer.legacySection().serialize(event.getView().title());

        if (LeaderboardGUI.isLeaderboardTitle(title)) {
            event.setCancelled(true);
            return;
        }

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
