package com.herocraft.hungergames.arena;

import com.herocraft.hungergames.HungerGamesPlugin;
import com.herocraft.hungergames.kit.Kit;
import com.herocraft.hungergames.util.RandomLocationUtil;
import com.herocraft.hungergames.util.ScoreboardUtil;
import net.kyori.adventure.text.Component;
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
    private final World world;
    private final ZoneAllocator.Zone zone;
    private final Location lobbyLocation;

    private ArenaState state = ArenaState.PRELOADING;
    private final Set<UUID> players = new LinkedHashSet<>();
    private final Set<UUID> alive = new LinkedHashSet<>();
    private final Map<UUID, String> selectedKits = new LinkedHashMap<>();

    private BossBar preloadBar;
    private BukkitTask countdownTask;
    private BukkitTask graceTask;
    private BukkitTask shrinkTask;
    private int countdownSecondsLeft;

    public Arena(HungerGamesPlugin plugin, World world, ZoneAllocator.Zone zone) {
        this.plugin = plugin;
        this.world = world;
        this.zone = zone;
        int lobbyY = plugin.getConfig().getInt("lobby.y", 200);
        this.lobbyLocation = new Location(world, zone.centerX() + 0.5, lobbyY, zone.centerZ() + 0.5);
        buildLobbyPlatform();
    }

    /**
     * Construit une petite plateforme flottante (verre) au centre de la zone,
     * en forçant le chargement du chunk concerné. Le reste de la zone n'est
     * préchargé qu'ensuite, de façon asynchrone, via {@link #startPreload()}.
     */
    private void buildLobbyPlatform() {
        int radius = plugin.getConfig().getInt("lobby.radius", 8);
        int lobbyY = plugin.getConfig().getInt("lobby.y", 200);
        world.getChunkAt(zone.centerX() >> 4, zone.centerZ() >> 4);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= radius * radius) {
                    world.getBlockAt(zone.centerX() + dx, lobbyY - 1, zone.centerZ() + dz)
                            .setType(org.bukkit.Material.GLASS, false);
                }
            }
        }
        world.getBlockAt(zone.centerX(), lobbyY - 1, zone.centerZ()).setType(org.bukkit.Material.SEA_LANTERN, false);
    }

    // ---------------------------------------------------------------- getters

    public UUID getId() {
        return id;
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
        return (state == ArenaState.PRELOADING || state == ArenaState.WAITING || state == ArenaState.STARTING) && !isFull();
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
                    p.sendMessage(Component.text("La zone est prête, choisis ton kit avec /hg kit !", NamedTextColor.GREEN));
                }
            }
            checkStartConditions();
        });
    }

    // ---------------------------------------------------------------- joueurs

    public void addPlayer(Player player) {
        players.add(player.getUniqueId());
        alive.add(player.getUniqueId());
        if (state == ArenaState.PRELOADING && preloadBar != null) {
            preloadBar.addPlayer(player);
        }
        player.teleport(lobbyLocation);
        player.setGameMode(GameMode.ADVENTURE);
        refreshScoreboard(player);
        broadcast(Component.text(player.getName() + " a rejoint la partie (" + players.size() + "/" + getMaxPlayers() + ")", NamedTextColor.YELLOW));
        checkStartConditions();
    }

    public void removePlayer(Player player) {
        players.remove(player.getUniqueId());
        alive.remove(player.getUniqueId());
        selectedKits.remove(player.getUniqueId());
        if (preloadBar != null) preloadBar.removePlayer(player);
        player.setScoreboard(org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard());

        if (state == ArenaState.STARTING && players.size() < getMinPlayers()) {
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
            if (players.size() < getMinPlayers()) {
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
        state = ArenaState.WAITING;
        broadcast(Component.text("Décompte annulé : " + reason, NamedTextColor.RED));
    }

    private void beginGame() {
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

            WorldBorder border = org.bukkit.Bukkit.createWorldBorder();
            border.setCenter(zone.centerX() + 0.5, zone.centerZ() + 0.5);
            border.setSize(zone.size());
            p.setWorldBorder(border);

            p.sendMessage(Component.text("La partie commence ! PVP désactivé pendant " +
                    (plugin.getConfig().getInt("game.grace-period-seconds", 300) / 60) + " minutes.", NamedTextColor.GREEN));
        }

        state = ArenaState.GRACE_PERIOD;
        int graceSeconds = plugin.getConfig().getInt("game.grace-period-seconds", 300);
        graceTask = org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, this::endGracePeriod, graceSeconds * 20L);

        startScoreboardLoop();
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

    private void startBorderShrink() {
        int targetDiameter = plugin.getConfig().getInt("game.border.shrink.target-diameter", 150);
        long durationSeconds = plugin.getConfig().getInt("game.border.shrink.duration-seconds", 900);
        for (UUID uuid : players) {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p == null) continue;
            WorldBorder border = p.getWorldBorder();
            if (border == null) continue;
            border.setSize((double) targetDiameter, durationSeconds * 20L); // ticks
        }
        broadcast(Component.text("La zone jouable va se refermer vers le centre !", NamedTextColor.RED));
    }

    // ---------------------------------------------------------------- mort / victoire

    public void onPlayerDeath(Player player) {
        alive.remove(player.getUniqueId());
        checkWinCondition();
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

        Player winner = winnerId != null ? org.bukkit.Bukkit.getPlayer(winnerId) : null;
        Component message = winner != null
                ? Component.text(winner.getName() + " a gagné la partie !", NamedTextColor.GOLD)
                : Component.text("Partie terminée, aucun survivant.", NamedTextColor.GOLD);

        for (UUID uuid : players) {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p == null) continue;
            p.showTitle(Title.title(
                    winner != null && winner.getUniqueId().equals(uuid)
                            ? Component.text("VICTOIRE", NamedTextColor.GOLD)
                            : Component.text("Partie terminée", NamedTextColor.GRAY),
                    message));
            p.sendMessage(message);
            plugin.getArenaManager().sendToHub(p);
        }

        if (preloadBar != null) preloadBar.removeAll();
        plugin.getArenaManager().onArenaEnded(this);
    }

    // ---------------------------------------------------------------- scoreboard

    private void startScoreboardLoop() {
        org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (state == ArenaState.ENDED) return;
            for (UUID uuid : players) {
                Player p = org.bukkit.Bukkit.getPlayer(uuid);
                if (p != null) refreshScoreboard(p);
            }
        }, 20L, 20L);
    }

    private void refreshScoreboard(Player viewer) {
        List<String> lines = new java.util.ArrayList<>();
        lines.add("§7Zone: §f" + zone.cellX() + "," + zone.cellZ());
        lines.add("§7Joueurs: §f" + players.size() + "/" + getMaxPlayers());
        lines.add("§7Vivants: §f" + alive.size());
        switch (state) {
            case PRELOADING -> lines.add("§eChargement de la zone...");
            case WAITING -> lines.add("§eEn attente de joueurs...");
            case STARTING -> lines.add("§6Départ dans " + countdownSecondsLeft + "s");
            case GRACE_PERIOD -> lines.add("§aPVP désactivé");
            case PVP -> lines.add("§cPVP activé !");
            case ENDED -> lines.add("§7Partie terminée");
        }
        ScoreboardUtil.update(viewer, "§c§lHUNGER GAMES", lines);
    }

    private void broadcast(Component message) {
        for (UUID uuid : players) {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        }
    }
}
