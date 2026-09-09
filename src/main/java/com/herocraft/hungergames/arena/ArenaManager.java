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
    private final WorldAllocator worldAllocator;
    private final File arenasFile;
    private final Map<UUID, Arena> arenas = new LinkedHashMap<>();
    private final Map<String, Arena> namedArenas = new LinkedHashMap<>();
    private final Map<UUID, Arena> playerArena = new LinkedHashMap<>();
    private final Map<UUID, Arena> spectatorArena = new LinkedHashMap<>();

    public ArenaManager(HungerGamesPlugin plugin) {
        this.plugin = plugin;
        String prefix = plugin.getConfig().getString("arena-worlds.arena-name-prefix", "hg_arena_");
        this.worldAllocator = new WorldAllocator(plugin, prefix);
        this.arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
        loadPersistedArenas();
    }

    /** Le monde partagé où vit le hub (configuré via {@code world} dans config.yml). */
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

    public WorldAllocator getWorldAllocator() {
        return worldAllocator;
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
     * Crée une nouvelle zone nommée : génère un monde Bukkit dédié à cette arène
     * (voir {@link WorldAllocator}, qui lui configure une vraie bordure vanilla),
     * et lance le préchargement. L'arène reste ouverte en continu par la suite :
     * à chaque fin de partie, elle supprime son monde et en crée un nouveau (voir
     * {@link Arena}). Renvoie la zone créée, ou vide si le nom est invalide/déjà pris.
     */
    public Optional<Arena> createNamedZone(String name) {
        if (!isValidName(name)) return Optional.empty();
        if (namedArenas.containsKey(name.toLowerCase())) return Optional.empty();

        int size = plugin.getConfig().getInt("zone.size", 1000);
        World world = worldAllocator.createArenaWorld(size);
        Arena arena = new Arena(plugin, name, world, new Zone(0, 0, size));
        arenas.put(arena.getId(), arena);
        namedArenas.put(name.toLowerCase(), arena);
        arena.startPreload();
        savePersistedArenas();
        plugin.getLogger().info("Zone '" + name + "' créée sur le monde dédié '" + world.getName() + "', taille " + size);
        return Optional.of(arena);
    }

    /**
     * Supprime définitivement une zone nommée : si une partie y est en cours, elle
     * est annulée (tout le monde renvoyé au hub, pas de vainqueur), son monde dédié
     * est supprimé du disque. Le nom redevient immédiatement disponible.
     */
    public boolean deleteZone(String name) {
        Arena arena = namedArenas.get(name.toLowerCase());
        if (arena == null) return false;
        // forceCancel() est protégé en interne (drapeau "destroyed") et déclenche
        // onArenaEnded(), qui nettoie déjà toutes les maps + supprime le monde.
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
     * cycle vers un nouveau monde, voir {@link Arena#getState()}) : seuls les
     * joueurs/spectateurs sont détachés, l'arène elle-même reste enregistrée.
     * Le nouveau nom de monde occupé doit être sauvegardé (voir Arena#cycleToNewWorld
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
     * Réécrit entièrement {@code arenas.yml} avec l'état courant (nom -> nom du
     * monde dédié) de toutes les arènes nommées. Appelé à chaque création,
     * suppression, renommage, et à chaque cycle vers un nouveau monde (voir
     * Arena#cycleToNewWorld).
     */
    public void savePersistedArenas() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Arena arena : namedArenas.values()) {
            yaml.set("arenas." + arena.getName() + ".world", arena.getWorld().getName());
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

    /** Recrée au démarrage les arènes sauvegardées, sur les mêmes mondes dédiés qu'avant l'arrêt. */
    private void loadPersistedArenas() {
        if (!arenasFile.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(arenasFile);
        ConfigurationSection section = yaml.getConfigurationSection("arenas");
        if (section == null) return;

        int size = plugin.getConfig().getInt("zone.size", 1000);

        for (String name : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(name);
            if (s == null) continue;
            String worldName = s.getString("world");
            if (worldName == null) continue;
            try {
                World world = worldAllocator.loadOrCreateArenaWorld(worldName, size);
                Arena arena = new Arena(plugin, name, world, new Zone(0, 0, size));
                arenas.put(arena.getId(), arena);
                namedArenas.put(name.toLowerCase(), arena);
                arena.startPreload();
                plugin.getLogger().info("Zone '" + name + "' restaurée sur le monde '" + worldName + "'.");
            } catch (IllegalStateException e) {
                plugin.getLogger().warning("Impossible de restaurer la zone '" + name + "' (" + worldName + ") : " + e.getMessage());
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
}
