package com.herocraft.hungergames.util;

import com.herocraft.hungergames.arena.ZoneAllocator;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Calcule des points de spawn dispersés sur une zone en évitant que deux joueurs
 * n'apparaissent trop proches l'un de l'autre (rejection sampling), puis trouve
 * une hauteur de sol sûre (pas dans l'eau/la lave si possible).
 */
public final class RandomLocationUtil {

    private RandomLocationUtil() {
    }

    public static List<Location> scatter(World world, ZoneAllocator.Zone zone, int marginBlocks,
                                          int count, double minDistance) {
        Random random = new Random();
        List<Location> result = new ArrayList<>();

        int radius = zone.radius() - marginBlocks;
        if (radius < 10) radius = 10;

        double currentMinDistance = minDistance;
        int attemptsPerPoint = 300;

        for (int i = 0; i < count; i++) {
            Location loc = null;
            for (int attempt = 0; attempt < attemptsPerPoint; attempt++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double dist = Math.sqrt(random.nextDouble()) * radius;
                int x = (int) Math.round(zone.centerX() + Math.cos(angle) * dist);
                int z = (int) Math.round(zone.centerZ() + Math.sin(angle) * dist);

                if (isFarEnough(result, x, z, currentMinDistance)) {
                    loc = toSafeLocation(world, x, z);
                    break;
                }
            }
            if (loc == null) {
                // On n'a pas réussi à respecter la distance minimale : on réduit
                // progressivement l'exigence pour garantir qu'on trouve toujours une place
                // (utile quand il y a beaucoup de joueurs sur une zone donnée).
                currentMinDistance = Math.max(2.0, currentMinDistance * 0.85);
                int x = (int) Math.round(zone.centerX() + (random.nextDouble() - 0.5) * 2 * radius);
                int z = (int) Math.round(zone.centerZ() + (random.nextDouble() - 0.5) * 2 * radius);
                loc = toSafeLocation(world, x, z);
            }
            result.add(loc);
        }
        return result;
    }

    private static boolean isFarEnough(List<Location> existing, int x, int z, double minDistance) {
        for (Location loc : existing) {
            double dx = loc.getX() - x;
            double dz = loc.getZ() - z;
            if (Math.sqrt(dx * dx + dz * dz) < minDistance) {
                return false;
            }
        }
        return true;
    }

    private static Location toSafeLocation(World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z);
        Block block = world.getBlockAt(x, y, z);
        // Évite de spawn dans l'eau/la lave : on remonte jusqu'à trouver un bloc solide non liquide.
        int safety = 0;
        while (safety < 20 && (block.isLiquid())) {
            y++;
            block = world.getBlockAt(x, y, z);
            safety++;
        }
        return new Location(world, x + 0.5, y + 1, z + 0.5);
    }
}
