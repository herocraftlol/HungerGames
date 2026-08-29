package com.herocraft.hungergames.arena;

import com.herocraft.hungergames.HungerGamesPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ArenaManager {

    private final HungerGamesPlugin plugin;
    private final ZoneAllocator zoneAllocator;
    private final Map<UUID, Arena> arenas = new LinkedHashMap<>();
    private final Map<UUID, Arena> playerArena = new LinkedHashMap<>();
    private final Map<UUID, Arena> spectatorArena = new LinkedHashMap<>();

    public ArenaManager(HungerGamesPlugin plugin) {
        this.plugin = plugin;
        String zonesFile = plugin.getConfig().getString("zone.zones-file", "zones.yml");
        int cellSize = plugin.getConfig().getInt("zone.size", 1000);
        this.zoneAllocator = new ZoneAllocator(plugin, zonesFile, cellSize);
    }

    public World getGameWorld() {
        String worldName = plugin.getConfig().getString("world", "world");
        World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null) {
            throw new IllegalStateException("Le monde '" + worldName + "' est introuvable. Vérifie config.yml.");
        }
        return world;
    }

    public Location getHubLocation() {
        World world = getGameWorld();
        double x = plugin.getConfig().getDouble("hub.x", 0.5);
        double y = plugin.getConfig().getDouble("hub.y", 100);
        double z = plugin.getConfig().getDouble("hub.z", 0.5);
        return new Location(world, x, y, z);
    }

    public void sendToHub(Player player) {
        player.setWorldBorder(null);
        player.teleport(getHubLocation());
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        player.setScoreboard(org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard());
    }

    /** Trouve une arène joignable, ou en crée une nouvelle sur une zone jamais utilisée. */
    public Arena findOrCreateJoinableArena() {
        for (Arena arena : arenas.values()) {
            if (arena.isJoinable()) {
                return arena;
            }
        }
        return createArena();
    }

    private Arena createArena() {
        World world = getGameWorld();
        ZoneAllocator.Zone zone = zoneAllocator.allocateNext();
        Arena arena = new Arena(plugin, world, zone);
        arenas.put(arena.getId(), arena);
        arena.startPreload();
        plugin.getLogger().info("Nouvelle arène créée sur la cellule (" + zone.cellX() + "," + zone.cellZ() +
                ") -> centre (" + zone.centerX() + "," + zone.centerZ() + "), taille " + zone.size());
        return arena;
    }

    public boolean joinPlayer(Player player) {
        if (playerArena.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("Tu es déjà dans une partie.", NamedTextColor.RED));
            return false;
        }
        if (spectatorArena.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("Tu es en mode spectateur. Fais /hg unspectate d'abord.", NamedTextColor.RED));
            return false;
        }
        Arena arena = findOrCreateJoinableArena();
        return joinArena(player, arena);
    }

    /** Fait rejoindre un joueur dans une arène précise (utilisé par le GUI). */
    public boolean joinArena(Player player, Arena arena) {
        if (playerArena.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("Tu es déjà dans une partie.", NamedTextColor.RED));
            return false;
        }
        if (spectatorArena.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("Tu es en mode spectateur. Fais /hg unspectate d'abord.", NamedTextColor.RED));
            return false;
        }
        if (!arena.isJoinable()) {
            player.sendMessage(Component.text("Cette partie n'est plus disponible.", NamedTextColor.RED));
            return false;
        }
        playerArena.put(player.getUniqueId(), arena);
        arena.addPlayer(player);
        return true;
    }

    public boolean leavePlayer(Player player) {
        Arena arena = playerArena.remove(player.getUniqueId());
        if (arena == null) {
            player.sendMessage(Component.text("Tu n'es dans aucune partie.", NamedTextColor.RED));
            return false;
        }
        arena.removePlayer(player);
        sendToHub(player);
        player.sendMessage(Component.text("Tu as quitté la partie.", NamedTextColor.YELLOW));
        return true;
    }

    public Optional<Arena> getArenaOf(Player player) {
        return Optional.ofNullable(playerArena.get(player.getUniqueId()));
    }

    // ---------------------------------------------------------------- spectateurs

    /** Fait rejoindre un joueur en spectateur d'une arène en cours. */
    public boolean spectateArena(Player player, Arena arena) {
        if (playerArena.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("Tu es déjà dans une partie. Fais /hg leave d'abord.", NamedTextColor.RED));
            return false;
        }
        Arena currentSpectate = spectatorArena.get(player.getUniqueId());
        if (currentSpectate != null) {
            if (currentSpectate.getId().equals(arena.getId())) {
                player.sendMessage(Component.text("Tu regardes déjà cette partie en spectateur.", NamedTextColor.RED));
                return false;
            }
            currentSpectate.removeSpectator(player);
            spectatorArena.remove(player.getUniqueId());
        }
        if (!arena.addSpectator(player)) {
            player.sendMessage(Component.text("Cette partie ne peut pas être regardée en spectateur pour le moment.", NamedTextColor.RED));
            return false;
        }
        spectatorArena.put(player.getUniqueId(), arena);
        return true;
    }

    public boolean unspectate(Player player) {
        Arena arena = spectatorArena.remove(player.getUniqueId());
        if (arena == null) {
            player.sendMessage(Component.text("Tu n'es pas en mode spectateur.", NamedTextColor.RED));
            return false;
        }
        arena.removeSpectator(player);
        sendToHub(player);
        player.sendMessage(Component.text("Tu as quitté le mode spectateur.", NamedTextColor.YELLOW));
        return true;
    }

    public Optional<Arena> findSpectatorArenaOf(Player player) {
        return Optional.ofNullable(spectatorArena.get(player.getUniqueId()));
    }

    public void onArenaEnded(Arena arena) {
        arenas.remove(arena.getId());
        playerArena.values().removeIf(a -> a.getId().equals(arena.getId()));
        spectatorArena.values().removeIf(a -> a.getId().equals(arena.getId()));
        plugin.getLogger().info("Arène " + arena.getId() + " terminée et libérée. Zone jamais réutilisée.");
    }

    public void handleDisconnect(Player player) {
        Arena arena = playerArena.remove(player.getUniqueId());
        if (arena != null) {
            arena.removePlayer(player);
        }
        Arena spectating = spectatorArena.remove(player.getUniqueId());
        if (spectating != null) {
            spectating.removeSpectator(player);
        }
    }

    /** Liste ordonnée et stable des arènes actives, utilisée par le GUI (index = position dans la liste). */
    public List<Arena> getArenasOrdered() {
        return new ArrayList<>(arenas.values());
    }

    public Map<UUID, Arena> getArenas() {
        return arenas;
    }

    public ZoneAllocator getZoneAllocator() {
        return zoneAllocator;
    }
}
