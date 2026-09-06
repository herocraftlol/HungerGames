package com.herocraft.hungergames.hub;

import com.herocraft.hungergames.HungerGamesPlugin;
import com.herocraft.hungergames.arena.ZoneAllocator;
import com.herocraft.hungergames.arena.ZoneRegenerator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Construit procéduralement un lobby central autour du point de hub configuré :
 * une plaza de spawn, une allée symétrique menant à un ou plusieurs pupitres de PNJ
 * "rejoindre une arène" (hub.build.npc-count), entourée de collines/montagnes naturelles décorées
 * (arbres, fleurs, campement de survie, entrée de mine), le tout ceinturé
 * d'un mur de blocs barrière invisible pour empêcher toute sortie.
 *
 * Le terrain est reconstruit en un lourd traitement par colonnes, étalé sur
 * plusieurs ticks (voir {@link #build}) pour éviter de geler le serveur.
 */
public final class HubBuilder {

    private HubBuilder() {
    }

    public static NamespacedKey npcKey(HungerGamesPlugin plugin) {
        return new NamespacedKey(plugin, "hg_arena_npc");
    }

    public static void build(HungerGamesPlugin plugin, CommandSender sender) {
        World world;
        try {
            world = plugin.getArenaManager().getGameWorld();
        } catch (IllegalStateException e) {
            sender.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
            return;
        }

        int cx = (int) Math.floor(plugin.getConfig().getDouble("hub.x", 0.5));
        int cz = (int) Math.floor(plugin.getConfig().getDouble("hub.z", 0.5));
        int baseY = (int) Math.floor(plugin.getConfig().getDouble("hub.y", 100));
        int groundY = baseY - 1;

        int hubRadius = plugin.getConfig().getInt("hub.radius", 100);
        int buildRadius = plugin.getConfig().getInt("hub.build.radius", 40);
        if (buildRadius > hubRadius - 5) buildRadius = Math.max(15, hubRadius - 5);

        int npcCount = plugin.getConfig().getInt("hub.build.npc-count", 1);
        if (npcCount < 1 || npcCount > 7) npcCount = 1;
        int npcDistance = plugin.getConfig().getInt("hub.build.npc-distance", 18);
        if (npcDistance > buildRadius - 8) npcDistance = Math.max(8, buildRadius - 8);

        List<int[]> npcOffsets = computeNpcOffsets(npcCount, npcDistance);

        sender.sendMessage(Component.text("Construction du lobby en cours (rayon " + buildRadius +
                ")... ça peut prendre 15-30 secondes, quelques à-coups de TPS sont normaux.", NamedTextColor.YELLOW));

        removeExistingNpcs(plugin, world, cx, cz, buildRadius + 10);

        Deque<int[]> queue = new ArrayDeque<>();
        for (int dx = -buildRadius; dx <= buildRadius; dx++) {
            for (int dz = -buildRadius; dz <= buildRadius; dz++) {
                if (dx * dx + dz * dz <= (double) buildRadius * buildRadius) {
                    queue.add(new int[]{dx, dz});
                }
            }
        }
        int total = queue.size();
        int[] done = {0};
        int columnsPerTick = 25;

        final int finalBuildRadius = buildRadius;
        final int finalGroundY = groundY;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (queue.isEmpty()) {
                    cancel();
                    sender.sendMessage(Component.text("Terrain terminé, ajout des décorations...", NamedTextColor.YELLOW));
                    decorate(plugin, world, cx, cz, finalGroundY, finalBuildRadius, npcOffsets, sender);
                    return;
                }
                for (int i = 0; i < columnsPerTick && !queue.isEmpty(); i++) {
                    int[] c = queue.poll();
                    buildColumn(world, cx, cz, finalGroundY, finalBuildRadius, npcOffsets, c[0], c[1]);
                    done[0]++;
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);

        // Le total est utile si on veut un jour afficher une progression ; conservé pour référence.
        if (total == 0) {
            sender.sendMessage(Component.text("Rayon de construction invalide.", NamedTextColor.RED));
        }
    }

    /**
     * Supprime le lobby construit par {@link #build} : enlève les PNJ et régénère
     * naturellement (via {@link com.herocraft.hungergames.arena.ZoneRegenerator})
     * tout le terrain de la zone construite, comme s'il n'avait jamais été touché.
     */
    public static void delete(HungerGamesPlugin plugin, CommandSender sender) {
        World world;
        try {
            world = plugin.getArenaManager().getGameWorld();
        } catch (IllegalStateException e) {
            sender.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
            return;
        }

        int cx = (int) Math.floor(plugin.getConfig().getDouble("hub.x", 0.5));
        int cz = (int) Math.floor(plugin.getConfig().getDouble("hub.z", 0.5));
        int hubRadius = plugin.getConfig().getInt("hub.radius", 100);
        int buildRadius = plugin.getConfig().getInt("hub.build.radius", 40);
        if (buildRadius > hubRadius - 5) buildRadius = Math.max(15, hubRadius - 5);

        removeExistingNpcs(plugin, world, cx, cz, buildRadius + 10);

        sender.sendMessage(Component.text("Suppression du lobby en cours (régénération du terrain naturel)... " +
                "peut prendre 15-30 secondes.", NamedTextColor.YELLOW));

        ZoneAllocator.Zone box = new ZoneAllocator.Zone(0, 0, cx, cz, buildRadius * 2);
        int chunksPerTick = plugin.getConfig().getInt("zone.regen-chunks-per-tick", 2);
        ZoneRegenerator regenerator = new ZoneRegenerator(plugin, chunksPerTick);
        regenerator.regenerate(world, box, (regenerated, total) -> {
        }, () -> sender.sendMessage(Component.text(
                "Lobby supprimé, terrain naturel restauré. Utilise /hgadmin hub build pour le reconstruire.",
                NamedTextColor.GREEN)));
    }

    private static void removeExistingNpcs(HungerGamesPlugin plugin, World world, int cx, int cz, int radius) {
        Location center = new Location(world, cx, plugin.getConfig().getInt("hub.y", 100), cz);
        Collection<Entity> nearby = world.getNearbyEntities(center, radius, 60, radius);
        NamespacedKey key = npcKey(plugin);
        for (Entity e : nearby) {
            if (e.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
                e.remove();
            }
        }
    }

    private static List<int[]> computeNpcOffsets(int count, int distance) {
        List<int[]> list = new ArrayList<>();
        int spacing = 5;
        int half = (count - 1) / 2;
        for (int i = -half; i <= half; i++) {
            list.add(new int[]{i * spacing, -distance});
        }
        return list;
    }

    // ---------------------------------------------------------------- terrain (phase 1)

    private static boolean isFlatZone(int dx, int dz, List<int[]> npcOffsets) {
        double r = Math.sqrt((double) dx * dx + (double) dz * dz);
        if (r <= 6) return true; // plaza principale

        if (!npcOffsets.isEmpty()) {
            int rowDz = npcOffsets.get(0)[1]; // négatif
            if (dz <= 0 && dz >= rowDz && Math.abs(dx) <= 3) return true; // allée
        }
        for (int[] off : npcOffsets) {
            double d = Math.hypot(dx - off[0], dz - off[1]);
            if (d <= 2.5) return true; // plateforme de pupitre
        }
        return false;
    }

    private static void buildColumn(World world, int cx, int cz, int groundY, int buildRadius,
                                     List<int[]> npcOffsets, int dx, int dz) {
        double r = Math.sqrt((double) dx * dx + (double) dz * dz);
        boolean flat = isFlatZone(dx, dz, npcOffsets);
        int height = flat ? groundY : computeHillHeight(dx, dz, groundY, buildRadius);

        int bottom = groundY - 12;
        int top = groundY + 41;

        for (int y = bottom; y <= top; y++) {
            Material mat;
            if (y > height) {
                mat = Material.AIR;
            } else if (y == height) {
                mat = surfaceMaterial(r, height, groundY, flat, dx, dz);
            } else if (y >= height - 3) {
                mat = Material.DIRT;
            } else {
                mat = Material.STONE;
            }
            world.getBlockAt(cx + dx, y, cz + dz).setType(mat, false);
        }

        if (r >= buildRadius - 3) {
            for (int y = groundY - 10; y <= groundY + 46; y++) {
                world.getBlockAt(cx + dx, y, cz + dz).setType(Material.BARRIER, false);
            }
        }
    }

    private static int computeHillHeight(int dx, int dz, int groundY, int buildRadius) {
        double r = Math.sqrt((double) dx * dx + (double) dz * dz);
        double hillsStart = 10, hillsEnd = buildRadius - 8;
        double bump = smoothNoise(dx, dz);

        if (r <= hillsStart) {
            return groundY + (int) Math.round(bump * 0.4);
        }
        if (r <= hillsEnd) {
            double t = (r - hillsStart) / (hillsEnd - hillsStart);
            double base = t * 9;
            return groundY + (int) Math.round(base + bump * 3);
        }
        double mt = Math.min(1.0, (r - hillsEnd) / Math.max(1.0, buildRadius - hillsEnd));
        double base = 9 + mt * 15;
        return groundY + (int) Math.round(base + smoothNoise(dx * 1.6, dz * 1.6) * 4);
    }

    private static Material surfaceMaterial(double r, int height, int groundY, boolean flat, int dx, int dz) {
        if (flat) {
            if (r <= 2) return Material.SMOOTH_QUARTZ;
            if (r <= 6) return ((dx + dz) % 4 == 0) ? Material.POLISHED_ANDESITE : Material.SMOOTH_QUARTZ;
            return Material.POLISHED_ANDESITE;
        }
        int rel = height - groundY;
        if (rel >= 20) return Material.SNOW_BLOCK;
        if (rel >= 13) return (Math.abs(dx * 7 + dz * 13) % 5 == 0) ? Material.ANDESITE : Material.STONE;
        if (rel >= 8) return (Math.abs(dx * 3 + dz * 5) % 6 == 0) ? Material.MOSSY_COBBLESTONE : Material.GRASS_BLOCK;
        return Material.GRASS_BLOCK;
    }

    private static double hashNoise(int x, int z) {
        long h = x * 374761393L + z * 668265263L;
        h = (h ^ (h >> 13)) * 1274126177L;
        h = h ^ (h >> 16);
        return ((h & 0xFFFFFF) / (double) 0xFFFFFF) * 2 - 1;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double smoothNoise(double x, double z) {
        double scale = 1.0 / 10.0;
        double sx = x * scale;
        double sz = z * scale;
        int x0 = (int) Math.floor(sx);
        int z0 = (int) Math.floor(sz);
        double fx = sx - x0;
        double fz = sz - z0;
        double v00 = hashNoise(x0, z0);
        double v10 = hashNoise(x0 + 1, z0);
        double v01 = hashNoise(x0, z0 + 1);
        double v11 = hashNoise(x0 + 1, z0 + 1);
        double i1 = lerp(v00, v10, fx);
        double i2 = lerp(v01, v11, fx);
        return lerp(i1, i2, fz);
    }

    // ---------------------------------------------------------------- décorations (phase 2)

    private static void decorate(HungerGamesPlugin plugin, World world, int cx, int cz, int groundY,
                                  int buildRadius, List<int[]> npcOffsets, CommandSender sender) {
        markSpawn(world, cx, cz, groundY);
        placeNpcs(plugin, world, cx, cz, groundY, npcOffsets);
        placeBaseCamp(world, cx, cz, groundY, npcOffsets);
        placeMineEntrance(world, cx, cz, groundY, buildRadius);
        scatterTrees(world, cx, cz, groundY, buildRadius, npcOffsets);
        scatterFlora(world, cx, cz, groundY, buildRadius, npcOffsets);

        sender.sendMessage(Component.text("Lobby terminé ! Le spawn est en (" + cx + "," + (groundY + 1) + "," + cz +
                "), " + npcOffsets.size() + " pupitre(s) de PNJ installés.", NamedTextColor.GREEN));
    }

    private static void markSpawn(World world, int cx, int cz, int groundY) {
        int[][] marks = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}};
        for (int[] m : marks) {
            world.getBlockAt(cx + m[0], groundY + 1, cz + m[1]).setType(Material.LANTERN, false);
        }
        world.getBlockAt(cx, groundY + 1, cz).setType(Material.AIR, false);
    }

    private static void placeNpcs(HungerGamesPlugin plugin, World world, int cx, int cz, int groundY, List<int[]> npcOffsets) {
        NamespacedKey key = npcKey(plugin);
        for (int[] off : npcOffsets) {
            int x = cx + off[0];
            int z = cz + off[1];
            world.getBlockAt(x, groundY, z).setType(Material.EMERALD_BLOCK, false);
            world.getBlockAt(x - 2, groundY + 1, z).setType(Material.LANTERN, false);
            world.getBlockAt(x + 2, groundY + 1, z).setType(Material.LANTERN, false);

            Location spawnLoc = new Location(world, x + 0.5, groundY + 1, z + 0.5, 0f, 0f);
            Villager npc = world.spawn(spawnLoc, Villager.class, v -> {
                v.setAI(false);
                v.setInvulnerable(true);
                v.setSilent(true);
                v.setCollidable(false);
                v.setPersistent(true);
                v.setRemoveWhenFarAway(false);
                v.setProfession(Villager.Profession.LIBRARIAN);
                v.customName(Component.text("⚔ Rejoindre une arène", NamedTextColor.GREEN));
                v.setCustomNameVisible(true);
                v.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            });
            npc.teleport(spawnLoc); // s'assure de l'orientation même si Bukkit l'ignore au spawn
        }
    }

    private static void placeBaseCamp(World world, int cx, int cz, int groundY, List<int[]> npcOffsets) {
        // Petit campement de survie, à l'écart de l'allée principale.
        int ox = 13, oz = 9;
        int height = computeHillHeight(ox, oz, groundY, 999);
        int x = cx + ox, z = cz + oz;

        // Petite plateforme plane sous le campement.
        for (int ddx = -2; ddx <= 2; ddx++) {
            for (int ddz = -2; ddz <= 2; ddz++) {
                world.getBlockAt(x + ddx, height, z + ddz).setType(Material.COARSE_DIRT, false);
                world.getBlockAt(x + ddx, height + 1, z + ddz).setType(Material.AIR, false);
            }
        }

        world.getBlockAt(x, height + 1, z).setType(Material.CAMPFIRE, false);
        world.getBlockAt(x + 2, height + 1, z).setType(Material.CRAFTING_TABLE, false);
        world.getBlockAt(x + 2, height + 1, z + 1).setType(Material.FURNACE, false);
        world.getBlockAt(x - 2, height + 1, z).setType(Material.CHEST, false);
        world.getBlockAt(x - 2, height + 1, z + 1).setType(Material.CHEST, false);
        world.getBlockAt(x, height + 1, z - 2).setType(Material.HAY_BLOCK, false);
        world.getBlockAt(x + 1, height + 1, z - 2).setType(Material.HAY_BLOCK, false);
        for (int i = -1; i <= 1; i += 2) {
            world.getBlockAt(x + i, height + 1, z + i).setType(Material.OAK_LOG, false);
        }
    }

    private static void placeMineEntrance(World world, int cx, int cz, int groundY, int buildRadius) {
        // Entrée de mine creusée dans le flanc de colline, du côté opposé aux PNJ.
        int oz = Math.min(buildRadius - 12, 22);
        int height = computeHillHeight(0, oz, groundY, buildRadius);
        int x = cx;

        for (int depth = 0; depth < 6; depth++) {
            int z = cz + oz + depth;
            for (int w = -1; w <= 1; w++) {
                world.getBlockAt(x + w, height + 1, z).setType(Material.CAVE_AIR, false);
                world.getBlockAt(x + w, height + 2, z).setType(Material.CAVE_AIR, false);
            }
            if (depth % 3 == 0) {
                world.getBlockAt(x - 1, height + 1, z).setType(Material.TORCH, false);
                world.getBlockAt(x + 1, height + 1, z).setType(Material.TORCH, false);
            }
            if (depth == 5) {
                world.getBlockAt(x - 1, height, z).setType(Material.COAL_ORE, false);
                world.getBlockAt(x, height, z).setType(Material.IRON_ORE, false);
                world.getBlockAt(x + 1, height, z).setType(Material.STONE, false);
            }
        }
        // Cadre d'entrée en bois façon galerie de mine.
        int entranceZ = cz + oz - 1;
        world.getBlockAt(x - 1, height + 1, entranceZ).setType(Material.OAK_LOG, false);
        world.getBlockAt(x + 1, height + 1, entranceZ).setType(Material.OAK_LOG, false);
        world.getBlockAt(x - 1, height + 2, entranceZ).setType(Material.OAK_LOG, false);
        world.getBlockAt(x, height + 2, entranceZ).setType(Material.OAK_SLAB, false);
        world.getBlockAt(x + 1, height + 2, entranceZ).setType(Material.OAK_LOG, false);
    }

    private static void scatterTrees(World world, int cx, int cz, int groundY, int buildRadius, List<int[]> npcOffsets) {
        for (int dx = -buildRadius; dx <= buildRadius; dx += 6) {
            for (int dz = -buildRadius; dz <= buildRadius; dz += 6) {
                double r = Math.hypot(dx, dz);
                if (r <= 12 || r >= buildRadius - 9) continue;
                if (isFlatZone(dx, dz, npcOffsets)) continue;
                if (hashNoise(dx + 1000, dz + 1000) < 0.25) continue; // ~40% de densité

                int jitterX = (int) (hashNoise(dx, dz + 500) * 2);
                int jitterZ = (int) (hashNoise(dx + 500, dz) * 2);
                int tx = dx + jitterX;
                int tz = dz + jitterZ;
                if (isFlatZone(tx, tz, npcOffsets)) continue;
                int height = computeHillHeight(tx, tz, groundY, buildRadius);
                placeTree(world, cx + tx, height + 1, cz + tz, hashNoise(tx, tz) > 0);
            }
        }
    }

    private static void placeTree(World world, int x, int y, int z, boolean spruce) {
        Material log = spruce ? Material.SPRUCE_LOG : Material.OAK_LOG;
        Material leaves = spruce ? Material.SPRUCE_LEAVES : Material.OAK_LEAVES;
        int trunkHeight = 4 + (spruce ? 1 : 0);

        for (int i = 0; i < trunkHeight; i++) {
            world.getBlockAt(x, y + i, z).setType(log, false);
        }
        for (int ly = -1; ly <= 1; ly++) {
            for (int ldx = -2; ldx <= 2; ldx++) {
                for (int ldz = -2; ldz <= 2; ldz++) {
                    if (Math.abs(ldx) == 2 && Math.abs(ldz) == 2) continue;
                    org.bukkit.block.Block b = world.getBlockAt(x + ldx, y + trunkHeight - 1 + ly, z + ldz);
                    if (b.getType() == Material.AIR) {
                        b.setType(leaves, false);
                    }
                }
            }
        }
        world.getBlockAt(x, y + trunkHeight, z).setType(leaves, false);
    }

    private static void scatterFlora(World world, int cx, int cz, int groundY, int buildRadius, List<int[]> npcOffsets) {
        Material[] flowers = {Material.POPPY, Material.DANDELION, Material.BLUE_ORCHID, Material.SHORT_GRASS, Material.FERN};
        Random random = new Random();
        for (int dx = -buildRadius; dx <= buildRadius; dx += 2) {
            for (int dz = -buildRadius; dz <= buildRadius; dz += 2) {
                double r = Math.hypot(dx, dz);
                if (r <= 8 || r >= buildRadius - 10) continue;
                if (isFlatZone(dx, dz, npcOffsets)) continue;
                if (hashNoise(dx + 3000, dz + 3000) < 0.55) continue; // faible densité

                int height = computeHillHeight(dx, dz, groundY, buildRadius);
                org.bukkit.block.Block top = world.getBlockAt(cx + dx, height, cz + dz);
                org.bukkit.block.Block above = world.getBlockAt(cx + dx, height + 1, cz + dz);
                if (top.getType() == Material.GRASS_BLOCK && above.getType() == Material.AIR) {
                    above.setType(flowers[random.nextInt(flowers.length)], false);
                }
            }
        }
    }
}
