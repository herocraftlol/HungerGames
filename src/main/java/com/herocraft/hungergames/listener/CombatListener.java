package com.herocraft.hungergames.listener;

import com.herocraft.hungergames.HungerGamesPlugin;
import com.herocraft.hungergames.arena.Arena;
import com.herocraft.hungergames.arena.ArenaState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Optional;

public class CombatListener implements Listener {

    private final HungerGamesPlugin plugin;

    public CombatListener(HungerGamesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Optional<Arena> arenaOpt = plugin.getArenaManager().getArenaOf(victim);
        if (arenaOpt.isEmpty()) {
            // Personne au hub/lobby ne doit jamais prendre de dégâts.
            event.setCancelled(true);
            return;
        }
        Arena arena = arenaOpt.get();

        if (arena.getState() != ArenaState.GRACE_PERIOD && arena.getState() != ArenaState.PVP) {
            event.setCancelled(true);
            return;
        }

        if (arena.getState() == ArenaState.GRACE_PERIOD && event instanceof EntityDamageByEntityEvent byEntity) {
            if (byEntity.getDamager() instanceof Player) {
                event.setCancelled(true);
            }
        }

        if (!arena.isAlive(victim)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        plugin.getArenaManager().getArenaOf(player).ifPresent(arena -> {
            event.setCancelled(true);
            player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
            player.getInventory().clear();
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(Component.text("Tu es mort ! Tu passes en mode spectateur.", NamedTextColor.RED));
            arena.onPlayerDeath(player);
        });
    }
}
