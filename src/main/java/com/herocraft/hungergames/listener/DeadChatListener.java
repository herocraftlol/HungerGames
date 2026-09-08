package com.herocraft.hungergames.listener;

import com.herocraft.hungergames.HungerGamesPlugin;
import com.herocraft.hungergames.arena.Arena;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Optional;

/**
 * Empêche un joueur mort (spectateur de sa propre arène) de parler aux joueurs
 * encore en vie : ses messages ne sont visibles que par les autres morts et
 * spectateurs de la même arène (et par lui-même). Les joueurs encore en vie et
 * les joueurs hors arène (hub, autre arène) ne sont pas concernés.
 */
public class DeadChatListener implements Listener {

    private final HungerGamesPlugin plugin;

    public DeadChatListener(HungerGamesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        Optional<Arena> arenaOpt = plugin.getArenaManager().getArenaOf(sender);
        if (arenaOpt.isEmpty()) return; // hub ou hors arène : chat normal, inchangé
        Arena arena = arenaOpt.get();
        if (arena.isAlive(sender)) return; // encore en vie : chat normal, inchangé

        event.viewers().removeIf(viewer -> {
            if (!(viewer instanceof Player p)) return false; // console, etc. : on garde
            if (p.getUniqueId().equals(sender.getUniqueId())) return false; // lui-même : on garde

            boolean sameArenaDeadOrSpectator = plugin.getArenaManager().getArenaOf(p)
                    .map(a -> a.getId().equals(arena.getId()) && !a.isAlive(p))
                    .orElse(false)
                    || plugin.getArenaManager().findSpectatorArenaOf(p)
                    .map(a -> a.getId().equals(arena.getId()))
                    .orElse(false);

            return !sameArenaDeadOrSpectator;
        });
    }
}
