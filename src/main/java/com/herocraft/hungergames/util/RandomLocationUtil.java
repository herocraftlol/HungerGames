package com.herocraft.hungergames.util;

import com.herocraft.hungergames.arena.ZoneAllocator;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Calcule des points de spawn dispersés sur une zone en évitant que deux joueurs
 * n'apparaissent trop proches l'un de l'autre (rejection sampling) ET en évitant
 * de les faire apparaître en pleine eau (océan, lac, lave) : on rejette d'abord
 * les points candidats situés sur de l'eau/lave avant de retenir une position.
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

            // Passe 1 : distance minimale entre joueurs ET pas d'eau/lave.
            for (int attempt = 0; attempt < attemptsPerPoint; attempt++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double dist = Math.sqrt(random.nextDouble()) * radius;
                int x = (int) Math.round(zone.centerX() + Math.cos(angle) * dist);
                int z = (int) Math.round(zone.centerZ() + Math.sin(angle) * dist);

                if (isFarEnough(result, x, z, currentMinDistance) && !isWaterOrLava(world, x, z)) {
                    loc = toSafeLocation(world, x, z);
                    break;
                }
            }

            // Passe 2 : on relâche la distance minimale mais on évite toujours l'eau/la lave
            // (utile quand il y a beaucoup de joueurs sur une zone donnée).
            if (loc == null) {
                currentMinDistance = Math.max(2.0, currentMinDistance * 0.85);
                for (int attempt = 0; attempt < 150; attempt++) {
                    int x = (int) Math.round(zone.centerX() + (random.nextDouble() - 0.5) * 2 * radius);
                    int z = (int) Math.round(zone.centerZ() + (random.nextDouble() - 0.5) * 2 * radius);
                    if (!isWaterOrLava(world, x, z)) {
                        loc = toSafeLocation(world, x, z);
                        break;
                    }
                }
            }

            // Dernier recours (zone quasi entièrement recouverte d'eau) : on accepte
            // même sur l'eau plutôt que de ne pas faire apparaître le joueur du tout.
            if (loc == null) {
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

    /** Vrai si le point le plus haut de cette colonne est de l'eau, de la glace flottant sur l'eau, ou de la lave. */
    private static boolean isWaterOrLava(World world, int x, int z) {
        Material type = world.getHighestBlockAt(x, z).getType();
        return switch (type) {
            case WATER, LAVA, ICE, FROSTED_ICE, KELP, KELP_PLANT, SEAGRASS, TALL_SEAGRASS, BUBBLE_COLUMN -> true;
            default -> false;
        };
    }

    private static Location toSafeLocation(World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z);
        Block block = world.getBlockAt(x, y, z);
        // Filet de sécurité si une petite étendue d'eau/lave a échappé à isWaterOrLava
        // (ex. juste sous un pont de feuilles) : on remonte jusqu'à un bloc non liquide.
        int safety = 0;
        while (safety < 20 && block.isLiquid()) {
            y++;
            block = world.getBlockAt(x, y, z);
            safety++;
        }
        return new Location(world, x + 0.5, y + 1, z + 0.5);
    }
}
