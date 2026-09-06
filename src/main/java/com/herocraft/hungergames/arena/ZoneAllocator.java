package com.herocraft.hungergames.arena;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Gère un pool de cellules carrées de {@code cellSize} blocs de côté, réparties sur
 * une grille bornée par {@code poolRadiusCells} cellules autour du centre du monde.
 * Contrairement à l'ancienne version, une cellule n'est PAS marquée comme utilisée
 * pour toujours : elle est simplement "tenue" (held) par une arène tant qu'elle
 * l'utilise, puis relâchée dans le pool une fois régénérée (voir {@link ZoneRegenerator}),
 * ce qui permet à d'autres arènes (ou la même, au tour suivant) de la reprendre plus tard.
 *
 * La cellule (0,0) est toujours réservée au hub et n'est donc jamais distribuée.
 */
public class ZoneAllocator {

    private final int cellSize;
    private final int poolRadiusCells;
    private final Random random = new Random();
    private final Set<Long> heldCells = new HashSet<>();

    public ZoneAllocator(int cellSize, int poolRadiusCells) {
        this.cellSize = cellSize;
        this.poolRadiusCells = Math.max(1, poolRadiusCells);
    }

    public int getCellSize() {
        return cellSize;
    }

    private static long key(int cellX, int cellZ) {
        return (((long) cellX) << 32) ^ (cellZ & 0xffffffffL);
    }

    /**
     * Tire une cellule libre au hasard dans le pool et la marque comme tenue.
     * Essaie d'abord un tirage aléatoire (rapide tant que le pool n'est pas presque
     * plein), puis retombe sur un balayage exhaustif si nécessaire pour garantir
     * qu'une cellule libre est trouvée dès qu'il en existe une.
     */
    public synchronized ZoneAllocator.Zone allocateRandomFreeCell() {
        int span = poolRadiusCells * 2 + 1;

        for (int attempt = 0; attempt < 500; attempt++) {
            int x = random.nextInt(span) - poolRadiusCells;
            int z = random.nextInt(span) - poolRadiusCells;
            if (x == 0 && z == 0) continue; // réservé au hub
            if (heldCells.add(key(x, z))) {
                return toZone(x, z);
            }
        }

        for (int x = -poolRadiusCells; x <= poolRadiusCells; x++) {
            for (int z = -poolRadiusCells; z <= poolRadiusCells; z++) {
                if (x == 0 && z == 0) continue;
                if (heldCells.add(key(x, z))) {
                    return toZone(x, z);
                }
            }
        }

        throw new IllegalStateException(
                "Plus aucune zone libre dans le pool (augmente zone.pool-radius-cells dans config.yml).");
    }

    /** Relâche une cellule dans le pool (à appeler une fois qu'elle a été régénérée). */
    public synchronized void release(Zone zone) {
        heldCells.remove(key(zone.cellX(), zone.cellZ()));
    }

    /**
     * Réserve une cellule précise (utilisé au redémarrage pour restaurer une arène
     * sur la cellule qu'elle occupait avant l'arrêt du serveur). Échoue si la
     * cellule est déjà tenue par quelqu'un d'autre.
     */
    public synchronized Zone reserveCell(int cellX, int cellZ) {
        if (!heldCells.add(key(cellX, cellZ))) {
            throw new IllegalStateException("Cellule (" + cellX + "," + cellZ + ") déjà tenue.");
        }
        return toZone(cellX, cellZ);
    }

    public synchronized int getHeldCount() {
        return heldCells.size();
    }

    public int getPoolCapacity() {
        int span = poolRadiusCells * 2 + 1;
        return span * span - 1; // -1 pour la cellule du hub
    }

    private Zone toZone(int cellX, int cellZ) {
        int centerX = cellX * cellSize;
        int centerZ = cellZ * cellSize;
        return new Zone(cellX, cellZ, centerX, centerZ, cellSize);
    }

    /** Représente une cellule du pool, actuellement allouée à une arène. */
    public record Zone(int cellX, int cellZ, int centerX, int centerZ, int size) {

        public int radius() {
            return size / 2;
        }

        public int minX() {
            return centerX - radius();
        }

        public int maxX() {
            return centerX + radius();
        }

        public int minZ() {
            return centerZ - radius();
        }

        public int maxZ() {
            return centerZ + radius();
        }
    }
}
