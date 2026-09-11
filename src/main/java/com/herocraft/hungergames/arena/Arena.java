package com.herocraft.hungergames.arena;

import com.herocraft.hungergames.HungerGamesPlugin;
import com.herocraft.hungergames.kit.Kit;
import com.herocraft.hungergames.util.DeathItems;
import com.herocraft.hungergames.util.RandomLocationUtil;
import com.herocraft.hungergames.util.ScoreboardUtil;
import com.herocraft.hungergames.util.SpectatorItems;
import com.herocraft.hungergames.util.WaitingRoomItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Arena {

    private final HungerGamesPlugin plugin;
    private final UUID id = UUID.randomUUID();
    private String name;
    private final World world;
    private ZoneAllocator.Zone zone;
    private Location lobbyLocation;

    private ArenaState state = ArenaState.PRELOADING;
    private final Set<UUID> players = new LinkedHashSet<>();
    private final Set<UUID> alive = new LinkedHashSet<>();
    private final Set<UUID> spectators = new LinkedHashSet<>();
    private final Map<UUID, String> selectedKits = new LinkedHashMap<>();

    private BossBar preloadBar;
    private BukkitTask countdownTask;
    private BukkitTask graceTask;
    private BukkitTask shrinkTask;
    private BukkitTask scoreboardTask;
    private BukkitTask borderDamageTask;
    private BukkitTask borderVisualTask;
    private int countdownSecondsLeft;
    private boolean forcedStart = false;
    private boolean destroyed = false;
    private BorderPhase borderPhase = BorderPhase.NONE;
    private long borderPhaseEndMillis = 0L;
    private double borderSizeAtPhaseStart = 0;
    private double borderSizeTarget = 0;
    private long borderAnimStartMillis = 0L;
    private long borderAnimDurationMillis = 0L;

    public Arena(HungerGamesPlugin plugin, String name, World world, ZoneAllocator.Zone zone) {
        this.plugin = plugin;
        this.name = name;
        this.world = world;
        setupZone(zone);
        startScoreboardLoop();
    }

    /**
     * (Ré)initialise l'arène sur une nouvelle cellule de la grille : reconstruit la
     * plateforme de lobby flottante au centre de cette nouvelle zone et met à jour
     * la position de téléportation du lobby. Appelé à la création de l'arène, puis
     * à chaque nouveau tour du cycle (voir {@link #cycleToNewZone()}).
     */
    private void setupZone(ZoneAllocator.Zone newZone) {
        this.zone = newZone;
        int lobbyY = plugin.getConfig().getInt("lobby.y", 200);
        this.lobbyLocation = new Location(world, newZone.centerX() + 0.5, lobbyY, newZone.centerZ() + 0.5);
        buildLobbyPlatform();
    }

    /**
     * Construit une petite plateforme flottante (verre) au centre de la zone,
     * entourée d'une cage de blocs barrière (sol invisible + murs) pour empêcher
     * les joueurs en attente d'en tomber ou de s'en éloigner en marchant. Cette
     * cage est retirée au lancement de la partie (voir {@link #removeLobbyCage()}),
     * puisque les joueurs sont alors téléportés ailleurs et que les spectateurs
     * doivent pouvoir voler librement dans toute la zone.
     */
    private void buildLobbyPlatform() {
        int radius = plugin.getConfig().getInt("lobby.radius", 8);
        int lobbyY = plugin.getConfig().getInt("lobby.y", 200);
        int cageMargin = plugin.getConfig().getInt("lobby.cage-margin", 3);
        int cageRadius = radius + cageMargin;
        world.getChunkAt(zone.centerX() >> 4, zone.centerZ() >> 4);

        for (int dx = -cageRadius; dx <= cageRadius; dx++) {
            for (int dz = -cageRadius; dz <= cageRadius; dz++) {
                double d2 = (double) dx * dx + (double) dz * dz;
                org.bukkit.block.Block floor = world.getBlockAt(zone.centerX() + dx, lobbyY - 1, zone.centerZ() + dz);
                if (d2 <= (double) radius * radius) {
                    floor.setType(org.bukkit.Material.GLASS, false);
                } else if (d2 <= (double) cageRadius * cageRadius) {
                    // Sol invisible entre le bord de la plateforme et le mur, pour qu'on
                    // ne puisse jamais tomber entre les deux.
                    floor.setType(org.bukkit.Material.BARRIER, false);
                }
            }
        }
        world.getBlockAt(zone.centerX(), lobbyY - 1, zone.centerZ()).setType(org.bukkit.Material.SEA_LANTERN, false);

        int cageHeight = plugin.getConfig().getInt("lobby.cage-height", 5);
        for (int dx = -cageRadius; dx <= cageRadius; dx++) {
            for (int dz = -cageRadius; dz <= cageRadius; dz++) {
                double d = Math.sqrt((double) dx * dx + (double) dz * dz);
                if (d >= cageRadius - 1.5 && d <= cageRadius + 0.5) {
                    for (int dy = 0; dy <= cageHeight; dy++) {
                        world.getBlockAt(zone.centerX() + dx, lobbyY - 1 + dy, zone.centerZ() + dz)
                                .setType(org.bukkit.Material.BARRIER, false);
                    }
                }
            }
        }
    }

    /**
     * Retire la cage de blocs barrière autour de la plateforme de lobby (appelé au
     * lancement de la partie). Le verre de la plateforme elle-même reste en place.
     */
    private void removeLobbyCage() {
        int radius = plugin.getConfig().getInt("lobby.radius", 8);
        int lobbyY = plugin.getConfig().getInt("lobby.y", 200);
        int cageMargin = plugin.getConfig().getInt("lobby.cage-margin", 3);
        int cageRadius = radius + cageMargin;
        int cageHeight = plugin.getConfig().getInt("lobby.cage-height", 5);

        for (int dx = -cageRadius; dx <= cageRadius; dx++) {
            for (int dz = -cageRadius; dz <= cageRadius; dz++) {
                for (int dy = -1; dy <= cageHeight; dy++) {
                    org.bukkit.block.Block b = world.getBlockAt(zone.centerX() + dx, lobbyY - 1 + dy, zone.centerZ() + dz);
                    if (b.getType() == org.bukkit.Material.BARRIER) {
                        b.setType(org.bukkit.Material.AIR, false);
                    }
                }
            }
        }
    }

    /**
     * Configure les dégâts de bordure : ~1 cœur (2 PV) par seconde dès qu'on est
     * hors de la bordure, sans zone tampon (le vanilla par défaut laisse quelques
     * blocs de marge avant de faire mal).
     */
    private static void configureBorderDamage(WorldBorder border) {
        border.setDamageAmount(2.0);
        border.setDamageBuffer(0.0);
    }

    // ---------------------------------------------------------------- getters

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArenaState getState() {
        return state;
    }

    public ZoneAllocator.Zone getZone() {
        return zone;
    }

    public World getWorld() {
        return world;
    }

    public Set<UUID> getPlayers() {
        return players;
    }

    public int getMaxPlayers() {
        return plugin.getConfig().getInt("game.max-players", 100);
    }

    public boolean isFull() {
        return players.size() >= getMaxPlayers();
    }

    public boolean isJoinable() {
        return (state == ArenaState.WAITING || state == ArenaState.STARTING) && !isFull();
    }

    /** Une arène se regarde en spectateur une fois la partie lancée (farm ou PVP). */
    public boolean isSpectatable() {
        return state == ArenaState.GRACE_PERIOD || state == ArenaState.PVP;
    }

    public int getSpectatorCount() {
        return spectators.size();
    }

    public boolean isSpectator(Player player) {
        return spectators.contains(player.getUniqueId());
    }

    public Location getLobbyLocation() {
        return lobbyLocation.clone();
    }

    // ---------------------------------------------------------------- préchargement

    public void startPreload() {
        state = ArenaState.PRELOADING;
        preloadBar = org.bukkit.Bukkit.createBossBar("§ePréparation de la zone... 0%", BarColor.YELLOW, BarStyle.SOLID);

        int chunksPerTick = plugin.getConfig().getInt("zone.chunks-per-tick", 8);
        ChunkPreloader preloader = new ChunkPreloader(plugin, chunksPerTick);
        preloader.preload(world, zone, (loaded, total) -> {
            double progress = total == 0 ? 1.0 : (double) loaded / total;
            preloadBar.setProgress(Math.min(1.0, progress));
            preloadBar.setTitle("§ePréparation de la zone... " + (int) (progress * 100) + "%");
        }, () -> {
            preloadBar.setProgress(1.0);
            preloadBar.setTitle("§aZone prête !");
            state = ArenaState.WAITING;
            for (UUID uuid : players) {
                Player p = org.bukkit.Bukkit.getPlayer(uuid);
                if (p != null) {
                    preloadBar.removePlayer(p);
                    p.sendMessage(kitSuggestionMessage("La zone est prête, "));
                }
            }
            checkStartConditions();
        });
    }

    /** Message cliquable invitant à ouvrir le menu de sélection de kit. */
    private Component kitSuggestionMessage(String prefix) {
        return Component.text(prefix, NamedTextColor.GREEN)
                .append(Component.text("choisis ton kit avec ", NamedTextColor.GREEN))
                .append(Component.text("/hg kit", NamedTextColor.YELLOW)
                        .clickEvent(ClickEvent.runCommand("/hg kit"))
                        .hoverEvent(HoverEvent.showText(Component.text("Clique pour ouvrir le menu des kits", NamedTextColor.GRAY))))
                .append(Component.text(" !", NamedTextColor.GREEN));
    }

    // ---------------------------------------------------------------- joueurs

    public void addPlayer(Player player) {
        players.add(player.getUniqueId());
        alive.add(player.getUniqueId());
        if (state == ArenaState.PRELOADING && preloadBar != null) {
            preloadBar.addPlayer(player);
        }

        WorldBorder border = org.bukkit.Bukkit.createWorldBorder();
        border.setCenter(zone.centerX() + 0.5, zone.centerZ() + 0.5);
        border.setSize(zone.size());
        configureBorderDamage(border);
        player.setWorldBorder(border);

        player.getInventory().clear();
        player.getInventory().setItem(0, WaitingRoomItems.createKitSelectorItem(plugin));
        player.getInventory().setItem(4, WaitingRoomItems.createLeaveItem(plugin));
        player.setFoodLevel(20);
        player.setSaturation(20f);

        player.teleport(lobbyLocation);
        player.setGameMode(GameMode.ADVENTURE);
        refreshScoreboard(player);
        broadcast(Component.text(player.getName() + " a rejoint la partie (" + players.size() + "/" + getMaxPlayers() + ")", NamedTextColor.YELLOW));
        if (state != ArenaState.PRELOADING) {
            player.sendMessage(kitSuggestionMessage(""));
        }
        checkStartConditions();
    }

    public void removePlayer(Player player) {
        players.remove(player.getUniqueId());
        alive.remove(player.getUniqueId());
        selectedKits.remove(player.getUniqueId());
        if (preloadBar != null) preloadBar.removePlayer(player);
        player.setScoreboard(org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard());

        if (state == ArenaState.STARTING && !forcedStart && players.size() < getMinPlayers()) {
            cancelCountdown("Pas assez de joueurs.");
        }
        if ((state == ArenaState.GRACE_PERIOD || state == ArenaState.PVP)) {
            checkWinCondition();
        }
    }

    public void selectKit(Player player, String kitId) {
        selectedKits.put(player.getUniqueId(), kitId);
        player.sendMessage(Component.text("Kit sélectionné : " + kitId, NamedTextColor.GREEN));
    }

    // ---------------------------------------------------------------- spectateurs

    /**
     * Fait rejoindre un joueur externe (qui ne participe pas) en spectateur de cette
     * arène. Renvoie false si l'arène n'est pas dans un état "regardable".
     */
    public boolean addSpectator(Player player) {
        if (!isSpectatable()) return false;
        if (spectators.contains(player.getUniqueId())) return false;

        spectators.add(player.getUniqueId());
        player.teleport(lobbyLocation);
        player.setGameMode(GameMode.SPECTATOR);
        player.getInventory().clear();
        player.getInventory().setItem(8, SpectatorItems.createLeaveItem(plugin));

        WorldBorder border = org.bukkit.Bukkit.createWorldBorder();
        border.setCenter(zone.centerX() + 0.5, zone.centerZ() + 0.5);
        border.setSize(zone.size());
        player.setWorldBorder(border);

        refreshScoreboard(player);
        player.sendMessage(Component.text("Tu observes la partie en spectateur. Utilise la boussole (ou /hg unspectate) pour repartir.", NamedTextColor.AQUA));
        return true;
    }

    /** Fait sortir un joueur du mode spectateur de cette arène (ne fait rien s'il ne spectate pas). */
    public void removeSpectator(Player player) {
        if (!spectators.remove(player.getUniqueId())) return;
        player.setWorldBorder(null);
        player.getInventory().clear();
        player.setScoreboard(org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard());
    }

    private void removeAllSpectators() {
        for (UUID uuid : new LinkedHashSet<>(spectators)) {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p != null) {
                removeSpectator(p);
                plugin.getArenaManager().sendToHub(p);
                p.sendMessage(Component.text("La partie que tu regardais est terminée.", NamedTextColor.GRAY));
            }
        }
        spectators.clear();
    }

    private int getMinPlayers() {
        return plugin.getConfig().getInt("game.min-players", 2);
    }

    // ---------------------------------------------------------------- lancement

    private void checkStartConditions() {
        if (state != ArenaState.WAITING) return;
        if (players.size() >= getMinPlayers()) {
            startCountdown();
        }
    }

    private void startCountdown() {
        state = ArenaState.STARTING;
        countdownSecondsLeft = plugin.getConfig().getInt("game.countdown-seconds", 30);
        broadcast(Component.text("La partie démarre dans " + countdownSecondsLeft + " secondes !", NamedTextColor.GOLD));

        countdownTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!forcedStart && players.size() < getMinPlayers()) {
                cancelCountdown("Pas assez de joueurs.");
                return;
            }
            countdownSecondsLeft--;
            if (countdownSecondsLeft <= 0) {
                if (countdownTask != null) countdownTask.cancel();
                beginGame();
                return;
            }
            if (countdownSecondsLeft <= 5 || countdownSecondsLeft % 10 == 0) {
                broadcast(Component.text("Début dans " + countdownSecondsLeft + "s...", NamedTextColor.YELLOW));
            }
            for (UUID uuid : players) {
                Player p = org.bukkit.Bukkit.getPlayer(uuid);
                if (p != null) refreshScoreboard(p);
            }
        }, 20L, 20L);
    }

    private void cancelCountdown(String reason) {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        forcedStart = false;
        state = ArenaState.WAITING;
        broadcast(Component.text("Décompte annulé : " + reason, NamedTextColor.RED));
    }

    /**
     * Force le lancement de la partie, même en dessous du nombre minimum de joueurs
     * configuré (utilisé par /hgadmin zone forcestart).
     */
    public boolean forceStart() {
        if (state != ArenaState.WAITING || players.isEmpty()) return false;
        forcedStart = true;
        startCountdown();
        return true;
    }

    /**
     * Arrête définitivement l'arène (suppression admin) : contrairement à une fin de
     * partie normale, la zone n'est pas reprise pour un nouveau tour — elle est
     * régénérée puis relâchée dans le pool, et l'arène elle-même est détruite.
     */
    public void forceCancel(String reason) {
        if (destroyed) return;
        destroyed = true;
        state = ArenaState.ENDED;
        if (countdownTask != null) countdownTask.cancel();
        if (graceTask != null) graceTask.cancel();
        if (shrinkTask != null) shrinkTask.cancel();
        if (scoreboardTask != null) scoreboardTask.cancel();
        if (borderDamageTask != null) borderDamageTask.cancel();
        if (borderVisualTask != null) borderVisualTask.cancel();

        Component message = Component.text("Partie annulée : " + reason, NamedTextColor.RED);
        for (UUID uuid : players) {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p == null) continue;
            p.sendMessage(message);
            plugin.getArenaManager().sendToHub(p);
        }
        if (preloadBar != null) preloadBar.removeAll();
        restoreVisibility();
        removeAllSpectators();
        plugin.getArenaManager().onArenaEnded(this);
        regenerateAndRelease(zone);
    }

    private void beginGame() {
        removeLobbyCage();
        removeLobbyPlatform();

        int marginBlocks = plugin.getConfig().getInt("zone.scatter-margin", 40);
        double minDistance = plugin.getConfig().getDouble("game.scatter.min-distance-between-players", 20);

        List<Location> spawnPoints = RandomLocationUtil.scatter(world, zone, marginBlocks, players.size(), minDistance);

        int i = 0;
        for (UUID uuid : players) {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p == null) continue;
            Location spawn = spawnPoints.get(i++);
            p.teleport(spawn);
            p.setGameMode(GameMode.SURVIVAL);
            p.getInventory().clear();
            p.getInventory().setArmorContents(null);

            String kitId = selectedKits.get(uuid);
            if (kitId != null) {
                plugin.getKitManager().get(kitId).ifPresent(kit -> giveKit(p, kit));
            }
            p.setHealth(p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
            p.setFoodLevel(20);
            p.setLevel(0);
            p.setExp(0f);
            p.setTotalExperience(0);

            WorldBorder border = org.bukkit.Bukkit.createWorldBorder();
            border.setCenter(zone.centerX() + 0.5, zone.centerZ() + 0.5);
            border.setSize(zone.size());
            configureBorderDamage(border);
            p.setWorldBorder(border);

            p.sendMessage(Component.text("La partie commence ! PVP désactivé pendant " +
                    (plugin.getConfig().getInt("game.grace-period-seconds", 300) / 60) + " minutes.", NamedTextColor.GREEN));
        }

        state = ArenaState.GRACE_PERIOD;

        // Suivi manuel de la taille de bordure "actuelle" (voir getCurrentBorderDiameter) :
        // on ne se fie pas à WorldBorder#getSize() pour une bordure virtuelle par-joueur,
        // qui ne s'anime pas forcément côté serveur. Au lancement, la bordure est fixe
        // (pas d'animation en cours) à la taille complète de la zone.
        borderSizeAtPhaseStart = zone.size();
        borderSizeTarget = zone.size();
        borderAnimStartMillis = System.currentTimeMillis();
        borderAnimDurationMillis = 0L;

        int graceSeconds = plugin.getConfig().getInt("game.grace-period-seconds", 300);
        graceTask = org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, this::endGracePeriod, graceSeconds * 20L);

        // Dégâts manuels hors bordure : le WorldBorder par joueur n'applique pas
        // toujours fidèlement ses dégâts vanilla, donc on les gère nous-mêmes,
        // que la bordure soit en train de bouger ou non.
        borderDamageTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, this::tickBorderDamage, 20L, 20L);

        // Rendu manuel du mur de bordure (particules) : le WorldBorder par-joueur
        // de Paper est connu pour ne pas toujours envoyer le paquet client qui
        // affiche le mur (bug Paper non résolu, cf. PaperMC/Paper#12372/#7748),
        // donc on ne se fie plus qu'à notre propre rendu, systématiquement visible.
        borderVisualTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, this::tickBorderVisuals, 10L, 10L);
    }

    /**
     * Retire la plateforme de lobby elle-même (verre + lanterne marine), en plus
     * de la cage, au lancement de la partie. Les spectateurs qui viendront
     * ensuite regarder (mode SPECTATOR, sans gravité) n'ont pas besoin de sol.
     */
    private void removeLobbyPlatform() {
        int radius = plugin.getConfig().getInt("lobby.radius", 8);
        int lobbyY = plugin.getConfig().getInt("lobby.y", 200);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= (double) radius * radius) {
                    world.getBlockAt(zone.centerX() + dx, lobbyY - 1, zone.centerZ() + dz)
                            .setType(org.bukkit.Material.AIR, false);
                }
            }
        }
    }

    /** Inflige ~1 cœur (2 PV) à chaque joueur actuellement hors de sa bordure. */
    private void tickBorderDamage() {
        double half = getCurrentBorderDiameter() / 2.0;
        double centerX = zone.centerX() + 0.5;
        double centerZ = zone.centerZ() + 0.5;

        for (UUID uuid : players) {
            if (!alive.contains(uuid)) continue;
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p == null) continue;

            Location loc = p.getLocation();
            double dx = Math.abs(loc.getX() - centerX);
            double dz = Math.abs(loc.getZ() - centerZ);
            if (dx > half || dz > half) {
                p.damage(2.0);
            }
        }
    }

    /** Affiche un mur de particules le long de la bordure, pour chaque joueur proche d'un bord. */
    private void tickBorderVisuals() {
        for (UUID uuid : players) {
            if (!alive.contains(uuid)) continue;
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p != null) renderBorderWall(p);
        }
    }

    private void renderBorderWall(Player p) {
        double half = getCurrentBorderDiameter() / 2.0;
        double centerX = zone.centerX() + 0.5;
        double centerZ = zone.centerZ() + 0.5;
        Location loc = p.getLocation();

        double minX = centerX - half, maxX = centerX + half;
        double minZ = centerZ - half, maxZ = centerZ + half;
        double visibility = 32;

        if (Math.abs(loc.getX() - minX) <= visibility) drawWallAtX(p, minX, loc.getZ(), visibility);
        if (Math.abs(loc.getX() - maxX) <= visibility) drawWallAtX(p, maxX, loc.getZ(), visibility);
        if (Math.abs(loc.getZ() - minZ) <= visibility) drawWallAtZ(p, loc.getX(), minZ, visibility);
        if (Math.abs(loc.getZ() - maxZ) <= visibility) drawWallAtZ(p, loc.getX(), maxZ, visibility);
    }

    /**
     * Taille (diamètre) actuelle "réelle" de la bordure, en interpolant nous-mêmes
     * entre le début et la fin de la dernière animation demandée
     * ({@link #applyBorderSize}). Nécessaire car {@code WorldBorder#getSize()} sur
     * une bordure virtuelle par-joueur ne reflète pas forcément l'animation en
     * cours côté serveur (pas de tick de monde pour la faire progresser) : sans ce
     * suivi manuel, le mur de particules et les dégâts sautaient directement à la
     * taille finale au lieu de réduire progressivement.
     */
    private double getCurrentBorderDiameter() {
        if (borderAnimDurationMillis <= 0) return borderSizeTarget;
        long elapsed = System.currentTimeMillis() - borderAnimStartMillis;
        if (elapsed >= borderAnimDurationMillis) return borderSizeTarget;
        double t = elapsed / (double) borderAnimDurationMillis;
        return borderSizeAtPhaseStart + (borderSizeTarget - borderSizeAtPhaseStart) * t;
    }

    /**
     * Taille "en direct" de la zone (diamètre) pour l'affichage dans le tableau de bord :
     * avant le premier rétrécissement (borderPhase == NONE), la bordure n'a pas encore
     * été animée et {@link #getCurrentBorderDiameter()} vaudrait 0, donc on retombe sur
     * la taille nominale de la zone.
     */
    private double getLiveBorderDiameter() {
        if (borderPhase == BorderPhase.NONE) return zone.size();
        return getCurrentBorderDiameter();
    }

    /** Distance (au sol, XZ) entre le joueur et le centre de la zone, ou null si autre monde. */
    private Double distanceToCenter(Player viewer) {
        Location loc = viewer.getLocation();
        if (loc.getWorld() == null || !loc.getWorld().equals(world)) return null;
        double centerX = zone.centerX() + 0.5;
        double centerZ = zone.centerZ() + 0.5;
        double dx = loc.getX() - centerX;
        double dz = loc.getZ() - centerZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static final org.bukkit.Particle.DustOptions BORDER_DUST =
            new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(255, 50, 50), 1.3f);

    private void drawWallAtX(Player p, double x, double centerZ, double span) {
        double baseY = p.getLocation().getY();
        for (double dz = -span; dz <= span; dz += 2.0) {
            for (double dy = -4; dy <= 6; dy += 1.5) {
                p.spawnParticle(org.bukkit.Particle.DUST, x, baseY + dy, centerZ + dz, 1, 0, 0, 0, 0, BORDER_DUST);
            }
        }
    }

    private void drawWallAtZ(Player p, double centerX, double z, double span) {
        double baseY = p.getLocation().getY();
        for (double dx = -span; dx <= span; dx += 2.0) {
            for (double dy = -4; dy <= 6; dy += 1.5) {
                p.spawnParticle(org.bukkit.Particle.DUST, centerX + dx, baseY + dy, z, 1, 0, 0, 0, 0, BORDER_DUST);
            }
        }
    }

    private void giveKit(Player player, Kit kit) {
        for (var item : kit.getItems()) {
            player.getInventory().addItem(item.clone());
        }
    }

    private void endGracePeriod() {
        if (state != ArenaState.GRACE_PERIOD) return;
        state = ArenaState.PVP;
        for (UUID uuid : players) {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p != null) {
                p.showTitle(Title.title(Component.text("PVP ACTIVÉ", NamedTextColor.RED), Component.text("Que le meilleur gagne !")));
            }
        }
        broadcast(Component.text("Le PVP est maintenant activé !", NamedTextColor.RED));

        if (plugin.getConfig().getBoolean("game.border.shrink.enabled", true)) {
            startBorderShrink();
        }
    }

    /**
     * Enchaîne les phases de réduction de bordure : une première réduction
     * (comme avant), puis une pause de quelques minutes une fois celle-ci
     * terminée, puis une réduction finale jusqu'à un tout petit cercle au
     * centre de la zone. Chaque étape est suivie dans {@link #borderPhase} /
     * {@link #borderPhaseEndMillis} pour l'affichage dans le tableau de bord
     * (voir {@link #refreshScoreboard}).
     */
    private void startBorderShrink() {
        int phase1Target = plugin.getConfig().getInt("game.border.shrink.phase1.target-diameter", 150);
        long phase1Duration = plugin.getConfig().getInt("game.border.shrink.phase1.duration-seconds", 900);
        long pauseSeconds = plugin.getConfig().getInt("game.border.shrink.pause-seconds", 300);
        int phase2Target = plugin.getConfig().getInt("game.border.shrink.phase2.target-diameter", 10);
        long phase2Duration = plugin.getConfig().getInt("game.border.shrink.phase2.duration-seconds", 600);

        borderPhase = BorderPhase.SHRINKING_1;
        borderPhaseEndMillis = System.currentTimeMillis() + phase1Duration * 1000L;
        applyBorderSize(phase1Target, phase1Duration);
        broadcast(Component.text("La zone jouable commence à se refermer !", NamedTextColor.RED));

        shrinkTask = org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            borderPhase = BorderPhase.PAUSED;
            borderPhaseEndMillis = System.currentTimeMillis() + pauseSeconds * 1000L;
            broadcast(Component.text("La zone se stabilise pendant " + (pauseSeconds / 60) + " minutes...", NamedTextColor.YELLOW));

            shrinkTask = org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                borderPhase = BorderPhase.SHRINKING_2;
                borderPhaseEndMillis = System.currentTimeMillis() + phase2Duration * 1000L;
                applyBorderSize(phase2Target, phase2Duration);
                broadcast(Component.text("La zone se referme jusqu'au centre !", NamedTextColor.RED));

                shrinkTask = org.bukkit.Bukkit.getScheduler().runTaskLater(plugin,
                        () -> borderPhase = BorderPhase.FINAL, phase2Duration * 20L);
            }, pauseSeconds * 20L);
        }, phase1Duration * 20L);
    }

    private void applyBorderSize(int targetDiameter, long durationSeconds) {
        // Source de vérité pour nos propres dégâts/rendu (voir getCurrentBorderDiameter) :
        // on repart de la taille "actuelle" interpolée, pas de la dernière cible brute,
        // pour enchaîner proprement une nouvelle animation même si la précédente n'est
        // pas totalement terminée.
        borderSizeAtPhaseStart = getCurrentBorderDiameter();
        borderSizeTarget = targetDiameter;
        borderAnimStartMillis = System.currentTimeMillis();
        borderAnimDurationMillis = durationSeconds * 1000L;

        for (UUID uuid : players) {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p == null) continue;
            WorldBorder border = p.getWorldBorder();
            if (border == null) continue;
            border.setSize(targetDiameter, durationSeconds * 1000L);
        }
    }

    private static String formatTime(long totalSeconds) {
        long m = Math.max(0, totalSeconds) / 60;
        long s = Math.max(0, totalSeconds) % 60;
        return String.format("%d:%02d", m, s);
    }

    /** Phase de rétrécissement de bordure en cours, pour l'affichage dans le tableau de bord. */
    private enum BorderPhase {
        NONE, SHRINKING_1, PAUSED, SHRINKING_2, FINAL
    }

    // ---------------------------------------------------------------- mort / victoire

    public void onPlayerDeath(Player victim, Player killer) {
        alive.remove(victim.getUniqueId());
        hideFromLiving(victim);
        victim.getInventory().setItem(0, DeathItems.createTeleportItem(plugin));
        announceDeath(victim, killer);
        checkWinCondition();
    }

    /**
     * Rend le joueur qui vient de mourir invisible aux joueurs encore en vie de
     * cette arène (comme un vrai spectateur), tout en lui redonnant la vue sur
     * les autres morts/spectateurs qu'il avait pu avoir cachés tant qu'il était
     * lui-même en vie (voir la même boucle plus haut, exécutée pour chaque mort
     * précédente).
     */
    private void hideFromLiving(Player deadPlayer) {
        for (UUID uuid : alive) {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p != null) p.hidePlayer(plugin, deadPlayer);
        }
        for (UUID uuid : players) {
            if (alive.contains(uuid) || uuid.equals(deadPlayer.getUniqueId())) continue;
            Player other = org.bukkit.Bukkit.getPlayer(uuid);
            if (other != null) {
                deadPlayer.showPlayer(plugin, other);
                other.showPlayer(plugin, deadPlayer);
            }
        }
        for (UUID uuid : spectators) {
            Player other = org.bukkit.Bukkit.getPlayer(uuid);
            if (other != null) {
                deadPlayer.showPlayer(plugin, other);
                other.showPlayer(plugin, deadPlayer);
            }
        }
    }

    /** Annule toutes les visibilités cachées entre participants de cette arène (fin de manche). */
    private void restoreVisibility() {
        List<Player> everyone = new java.util.ArrayList<>();
        for (UUID uuid : players) {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p != null) everyone.add(p);
        }
        for (UUID uuid : spectators) {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p != null) everyone.add(p);
        }
        for (Player a : everyone) {
            for (Player b : everyone) {
                if (a != b) a.showPlayer(plugin, b);
            }
        }
    }

    /** Liste des joueurs de cette arène actuellement en vie et connectés (pour le GUI de téléportation des morts). */
    public List<Player> getAlivePlayersOnline() {
        List<Player> result = new java.util.ArrayList<>();
        for (UUID uuid : alive) {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p != null) result.add(p);
        }
        return result;
    }

    private void announceDeath(Player victim, Player killer) {
        Component message;
        if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId())) {
            message = Component.text(killer.getName(), NamedTextColor.YELLOW)
                    .append(Component.text(" a éliminé ", NamedTextColor.RED))
                    .append(Component.text(victim.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(" !", NamedTextColor.RED));
        } else {
            message = Component.text(victim.getName() + " est mort.", NamedTextColor.GRAY);
        }
        broadcastToAll(message);
    }

    private void checkWinCondition() {
        if (state != ArenaState.GRACE_PERIOD && state != ArenaState.PVP) return;
        if (alive.size() <= 1) {
            UUID winnerId = alive.stream().findFirst().orElse(null);
            endGame(winnerId);
        }
    }

    public boolean isAlive(Player player) {
        return alive.contains(player.getUniqueId());
    }

    private void endGame(UUID winnerId) {
        state = ArenaState.ENDED;
        if (countdownTask != null) countdownTask.cancel();
        if (graceTask != null) graceTask.cancel();
        if (shrinkTask != null) shrinkTask.cancel();
        if (borderDamageTask != null) borderDamageTask.cancel();
        if (borderVisualTask != null) borderVisualTask.cancel();

        Player winner = winnerId != null ? org.bukkit.Bukkit.getPlayer(winnerId) : null;
        Component message = winner != null
                ? Component.text(winner.getName() + " a gagné la partie !", NamedTextColor.GOLD)
                : Component.text("Partie terminée, aucun survivant.", NamedTextColor.GOLD);

        int endDelaySeconds = plugin.getConfig().getInt("game.end-delay-seconds", 10);

        for (UUID uuid : players) {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p == null) continue;
            boolean isWinner = winner != null && winner.getUniqueId().equals(uuid);
            p.showTitle(Title.title(
                    isWinner ? Component.text("VICTOIRE", NamedTextColor.GOLD) : Component.text("Partie terminée", NamedTextColor.GRAY),
                    message));
            p.sendMessage(message);
            if (!isWinner) {
                // Tout le monde sauf le vainqueur passe/reste spectateur pendant les
                // quelques secondes de fin de partie, avant le retour au hub.
                p.setGameMode(GameMode.SPECTATOR);
            }
        }
        broadcastToAll(Component.text("Retour au lobby dans " + endDelaySeconds + " secondes...", NamedTextColor.GRAY));

        if (winner != null) {
            plugin.getStatsManager().addWin(winner.getUniqueId(), winner.getName());
        }

        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            restoreVisibility();
            for (UUID uuid : players) {
                Player p = org.bukkit.Bukkit.getPlayer(uuid);
                if (p != null) plugin.getArenaManager().sendToHub(p);
            }
            if (preloadBar != null) preloadBar.removeAll();
            removeAllSpectators();
            plugin.getArenaManager().onRoundEnded(this);
            cycleToNewZone();
        }, endDelaySeconds * 20L);
    }

    /**
     * Fin normale d'une partie : l'arène ne se détruit PAS. Elle se réinitialise,
     * tire une nouvelle cellule libre au hasard dans le pool (forcément différente de
     * l'ancienne, puisque celle-ci reste "tenue" tant qu'elle n'est pas régénérée),
     * reconstruit son lobby dessus et relance le préchargement — prête à accueillir
     * une nouvelle partie. L'ancienne zone, elle, est régénérée en arrière-plan puis
     * relâchée dans le pool pour qu'une autre arène (ou celle-ci, plus tard) puisse
     * la reprendre.
     */
    private void cycleToNewZone() {
        if (destroyed) return;
        ZoneAllocator.Zone oldZone = this.zone;

        players.clear();
        alive.clear();
        spectators.clear();
        selectedKits.clear();
        forcedStart = false;
        countdownSecondsLeft = 0;
        borderPhase = BorderPhase.NONE;
        borderSizeAtPhaseStart = 0;
        borderSizeTarget = 0;
        borderAnimDurationMillis = 0L;

        ZoneAllocator.Zone newZone = plugin.getArenaManager().getZoneAllocator().allocateRandomFreeCell();
        setupZone(newZone);
        startPreload();
        plugin.getArenaManager().savePersistedArenas();

        regenerateAndRelease(oldZone);
    }

    /** Régénère une zone en arrière-plan (annule les dégâts/constructions de la partie), puis la relâche dans le pool. */
    private void regenerateAndRelease(ZoneAllocator.Zone oldZone) {
        ZoneAllocator allocator = plugin.getArenaManager().getZoneAllocator();
        int chunksPerTick = plugin.getConfig().getInt("zone.regen-chunks-per-tick", 2);
        ZoneRegenerator regenerator = new ZoneRegenerator(plugin, chunksPerTick);
        regenerator.regenerate(world, oldZone, (done, total) -> {
        }, () -> {
            allocator.release(oldZone);
            plugin.getLogger().info("Cellule (" + oldZone.cellX() + "," + oldZone.cellZ() + ") régénérée et relâchée dans le pool.");
        });
    }

    // ---------------------------------------------------------------- scoreboard

    private void startScoreboardLoop() {
        scoreboardTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (destroyed) return;
            for (UUID uuid : players) {
                Player p = org.bukkit.Bukkit.getPlayer(uuid);
                if (p != null) refreshScoreboard(p);
            }
            for (UUID uuid : spectators) {
                Player p = org.bukkit.Bukkit.getPlayer(uuid);
                if (p != null) refreshScoreboard(p);
            }
        }, 20L, 20L);
    }

    private void refreshScoreboard(Player viewer) {
        List<String> lines = new java.util.ArrayList<>();
        lines.add("§7Zone: §f" + name);
        lines.add("§7Taille de la zone: §f" + (int) getLiveBorderDiameter() + "m");
        Double distanceToMid = distanceToCenter(viewer);
        if (distanceToMid != null) {
            lines.add("§7Distance du centre: §f" + (int) Math.round(distanceToMid) + "m");
        }
        lines.add("§7Joueurs: §f" + players.size() + "/" + getMaxPlayers());
        lines.add("§7Vivants: §f" + alive.size());
        if (!spectators.isEmpty()) {
            lines.add("§7Spectateurs: §f" + spectators.size());
        }
        switch (state) {
            case PRELOADING -> lines.add("§eChargement de la zone...");
            case WAITING -> lines.add("§eEn attente de joueurs...");
            case STARTING -> lines.add("§6Départ dans " + countdownSecondsLeft + "s");
            case GRACE_PERIOD -> lines.add("§aPVP désactivé");
            case PVP -> lines.add("§cPVP activé !");
            case ENDED -> lines.add("§7Partie terminée");
        }

        if (state == ArenaState.PVP) {
            long remaining = Math.max(0, (borderPhaseEndMillis - System.currentTimeMillis()) / 1000);
            switch (borderPhase) {
                case SHRINKING_1 -> lines.add("§cBordure : réduction (" + formatTime(remaining) + ")");
                case PAUSED -> lines.add("§ePause bordure : " + formatTime(remaining));
                case SHRINKING_2 -> lines.add("§4Bordure finale : " + formatTime(remaining));
                case FINAL -> lines.add("§4Bordure au centre !");
                case NONE -> {
                }
            }
        }

        // Masque les pseudos au-dessus des têtes des joueurs encore EN VIE pendant
        // la partie active (les morts sont de toute façon invisibles aux vivants,
        // et on laisse les pseudos normaux entre spectateurs/morts qui se voient).
        java.util.List<String> hiddenNameTags = java.util.List.of();
        if (state == ArenaState.GRACE_PERIOD || state == ArenaState.PVP) {
            hiddenNameTags = new java.util.ArrayList<>();
            for (UUID uuid : alive) {
                Player p = org.bukkit.Bukkit.getPlayer(uuid);
                if (p != null) hiddenNameTags.add(p.getName());
            }
        }

        ScoreboardUtil.update(viewer, "§c§lHUNGER GAMES", lines, hiddenNameTags);
    }

    private void broadcast(Component message) {
        for (UUID uuid : players) {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        }
    }

    /** Comme {@link #broadcast}, mais touche aussi les spectateurs de cette arène. */
    private void broadcastToAll(Component message) {
        broadcast(message);
        for (UUID uuid : spectators) {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        }
    }
}
