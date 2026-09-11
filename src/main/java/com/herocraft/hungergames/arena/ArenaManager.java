package com.herocraft.hungergames.arena;

import com.herocraft.hungergames.HungerGamesPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ArenaManager {

    private final HungerGamesPlugin plugin;
    private final ZoneAllocator zoneAllocator;
    private final File arenasFile;
    private final Map<UUID, Arena> arenas = new LinkedHashMap<>();
    private final Map<String, Arena> namedArenas = new LinkedHashMap<>();
    private final Map<UUID, Arena> playerArena = new LinkedHashMap<>();
    private final Map<UUID, Arena> spectatorArena = new LinkedHashMap<>();

    public ArenaManager(HungerGamesPlugin plugin) {
        this.plugin = plugin;
        int cellSize = plugin.getConfig().getInt("zone.size", 1000);
        int poolRadiusCells = plugin.getConfig().getInt("zone.pool-radius-cells", 50);
        this.zoneAllocator = new ZoneAllocator(cellSize, poolRadiusCells);
        this.arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
        loadPersistedArenas();
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
        int radius = plugin.getConfig().getInt("hub.radius", 100);
        org.bukkit.WorldBorder border = org.bukkit.Bukkit.createWorldBorder();
        border.setCenter(0.5, 0.5);
        border.setSize(radius * 2L);
        border.setDamageAmount(2.0);
        border.setDamageBuffer(0.0);
        player.setWorldBorder(border);

        player.getInventory().clear();
        player.setFoodLevel(20);
        player.setSaturation(20f);

        player.teleport(getHubLocation());
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        player.setScoreboard(org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard());
    }

    // ---------------------------------------------------------------- gestion des zones (admin)

    private static final java.util.regex.Pattern NAME_PATTERN = java.util.regex.Pattern.compile("^[a-zA-Z0-9_-]{1,32}$");

    public boolean isValidName(String name) {
        return name != null && NAME_PATTERN.matcher(name).matches();
    }

    public Optional<Arena> findByName(String name) {
        return Optional.ofNullable(namedArenas.get(name.toLowerCase()));
    }

    /**
     * Crée une nouvelle zone nommée : tire une cellule au hasard, actuellement libre,
     * dans le pool (voir {@link ZoneAllocator}), et lance le préchargement. L'arène
     * reste ouverte en continu par la suite : à chaque fin de partie, elle régénère
     * la zone utilisée et en reprend une autre au hasard (voir {@link Arena}).
     * Renvoie la zone créée, ou vide si le nom est invalide/déjà pris.
     */
    public Optional<Arena> createNamedZone(String name) {
        if (!isValidName(name)) return Optional.empty();
        if (namedArenas.containsKey(name.toLowerCase())) return Optional.empty();

        World world = getGameWorld();
        ZoneAllocator.Zone zone = zoneAllocator.allocateRandomFreeCell();
        Arena arena = new Arena(plugin, name, world, zone);
        arenas.put(arena.getId(), arena);
        namedArenas.put(name.toLowerCase(), arena);
        arena.startPreload();
        savePersistedArenas();
        plugin.getLogger().info("Zone '" + name + "' créée sur la cellule (" + zone.cellX() + "," + zone.cellZ() +
                ") -> centre (" + zone.centerX() + "," + zone.centerZ() + "), taille " + zone.size());
        return Optional.of(arena);
    }

    /**
     * Supprime définitivement une zone nommée : si une partie y est en cours, elle
     * est annulée (tout le monde renvoyé au hub, pas de vainqueur), la zone est
     * régénérée puis relâchée dans le pool. Le nom redevient immédiatement disponible.
     */
    public boolean deleteZone(String name) {
        Arena arena = namedArenas.get(name.toLowerCase());
        if (arena == null) return false;
        // forceCancel() est protégé en interne (drapeau "destroyed") et déclenche
        // onArenaEnded(), qui nettoie déjà toutes les maps + régénère/relâche la zone.
        arena.forceCancel("Zone supprimée par un administrateur.");
        return true;
    }

    public boolean renameZone(String oldName, String newName) {
        if (!isValidName(newName)) return false;
        if (namedArenas.containsKey(newName.toLowerCase())) return false;
        Arena arena = namedArenas.remove(oldName.toLowerCase());
        if (arena == null) return false;
        arena.setName(newName);
        namedArenas.put(newName.toLowerCase(), arena);
        savePersistedArenas();
        return true;
    }

    public List<Arena> getNamedArenasOrdered() {
        return new ArrayList<>(namedArenas.values());
    }

    // ---------------------------------------------------------------- joueurs

    /** Trouve n'importe quelle zone nommée actuellement joignable (pas de création automatique). */
    public Optional<Arena> findAnyJoinableArena() {
        return namedArenas.values().stream().filter(Arena::isJoinable).findFirst();
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
        Optional<Arena> arena = findAnyJoinableArena();
        if (arena.isEmpty()) {
            player.sendMessage(Component.text("Aucune partie n'est ouverte pour le moment. Regarde /hg gui.", NamedTextColor.RED));
            return false;
        }
        return joinArena(player, arena.get());
    }

    public boolean joinPlayerByName(Player player, String name) {
        Optional<Arena> arena = findByName(name);
        if (arena.isEmpty()) {
            player.sendMessage(Component.text("Aucune zone nommée '" + name + "' n'existe.", NamedTextColor.RED));
            return false;
        }
        return joinArena(player, arena.get());
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

    /**
     * Nettoyage après une fin de partie NORMALE (l'arène continue d'exister et
     * cycle vers une nouvelle zone, voir {@link Arena#getState()}) : seuls les
     * joueurs/spectateurs sont détachés, l'arène elle-même reste enregistrée.
     * La nouvelle cellule occupée doit être sauvegardée (voir Arena#cycleToNewZone
     * qui appelle {@link #savePersistedArenas()} juste après).
     */
    public void onRoundEnded(Arena arena) {
        playerArena.values().removeIf(a -> a.getId().equals(arena.getId()));
        spectatorArena.values().removeIf(a -> a.getId().equals(arena.getId()));
    }

    /** Suppression DÉFINITIVE d'une arène (admin), utilisé par {@link Arena#forceCancel}. */
    public void onArenaEnded(Arena arena) {
        arenas.remove(arena.getId());
        namedArenas.values().removeIf(a -> a.getId().equals(arena.getId()));
        playerArena.values().removeIf(a -> a.getId().equals(arena.getId()));
        spectatorArena.values().removeIf(a -> a.getId().equals(arena.getId()));
        savePersistedArenas();
        plugin.getLogger().info("Zone '" + arena.getName() + "' définitivement supprimée.");
    }

    // ---------------------------------------------------------------- persistance des zones

    /**
     * Réécrit entièrement {@code arenas.yml} avec l'état courant (nom -> cellule
     * occupée) de toutes les arènes nommées. Appelé à chaque création, suppression,
     * renommage, et à chaque cycle vers une nouvelle zone (voir Arena#cycleToNewZone).
     */
    public void savePersistedArenas() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Arena arena : namedArenas.values()) {
            String base = "arenas." + arena.getName();
            yaml.set(base + ".cellX", arena.getZone().cellX());
            yaml.set(base + ".cellZ", arena.getZone().cellZ());
        }
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            yaml.save(arenasFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder arenas.yml : " + e.getMessage());
        }
    }

    /** Recrée au démarrage les arènes sauvegardées, sur les mêmes cellules qu'avant l'arrêt. */
    private void loadPersistedArenas() {
        if (!arenasFile.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(arenasFile);
        ConfigurationSection section = yaml.getConfigurationSection("arenas");
        if (section == null) return;

        World world;
        try {
            world = getGameWorld();
        } catch (IllegalStateException e) {
            plugin.getLogger().warning("Impossible de restaurer les zones sauvegardées : " + e.getMessage());
            return;
        }

        for (String name : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(name);
            if (s == null) continue;
            int cellX = s.getInt("cellX");
            int cellZ = s.getInt("cellZ");
            try {
                ZoneAllocator.Zone zone = zoneAllocator.reserveCell(cellX, cellZ);
                Arena arena = new Arena(plugin, name, world, zone);
                arenas.put(arena.getId(), arena);
                namedArenas.put(name.toLowerCase(), arena);
                arena.startPreload();
                plugin.getLogger().info("Zone '" + name + "' restaurée sur la cellule (" + cellX + "," + cellZ + ").");
            } catch (IllegalStateException e) {
                plugin.getLogger().warning("Cellule (" + cellX + "," + cellZ + ") indisponible, zone '" + name + "' non restaurée.");
            }
        }
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
