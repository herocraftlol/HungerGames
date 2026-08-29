package com.herocraft.hungergames.arena;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Découpe le monde en cellules carrées de {@code cellSize} blocs de côté et garantit
 * qu'une cellule n'est jamais réutilisée entre deux parties, même après un redémarrage
 * du serveur (persistance dans un fichier YAML).
 *
 * Les cellules sont identifiées par leurs coordonnées entières (cellX, cellZ).
 * Le centre en blocs d'une cellule (i, j) est (i * cellSize, j * cellSize).
 * Une recherche en spirale part de (0,0) pour trouver la prochaine cellule libre,
 * ce qui permet d'utiliser un maximum de cellules proches du centre du monde avant
 * de s'étendre vers l'extérieur.
 */
public class ZoneAllocator {

    private final JavaPlugin plugin;
    private final File file;
    private final int cellSize;
    private final Set<Long> usedCells = new HashSet<>();

    public ZoneAllocator(JavaPlugin plugin, String fileName, int cellSize) {
        this.plugin = plugin;
        this.cellSize = cellSize;
        this.file = new File(plugin.getDataFolder(), fileName);
        load();
    }

    public int getCellSize() {
        return cellSize;
    }

    private static long key(int cellX, int cellZ) {
        // Encode deux int (avec signe) sur un long pour servir de clé unique.
        return (((long) cellX) << 32) ^ (cellZ & 0xffffffffL);
    }

    /**
     * Alloue et marque comme utilisée la prochaine cellule libre, en spirale
     * autour du centre du monde (0,0). La cellule (0,0) est réservée au hub
     * et n'est donc jamais proposée.
     */
    public synchronized Zone allocateNext() {
        int x = 0, z = 0;
        int dx = 0, dz = -1;
        // Nombre de cellules max avant d'abandonner : évite une boucle infinie
        // si jamais quelque chose tourne mal (pratiquement jamais atteint).
        int maxSteps = 1_000_000;

        for (int i = 0; i < maxSteps; i++) {
            boolean isOrigin = (x == 0 && z == 0);
            if (!isOrigin && !usedCells.contains(key(x, z))) {
                markUsed(x, z);
                return toZone(x, z);
            }
            // Algorithme de spirale carrée classique.
            if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
                int tmp = dx;
                dx = -dz;
                dz = tmp;
            }
            x += dx;
            z += dz;
        }
        throw new IllegalStateException("Impossible de trouver une zone libre (limite atteinte).");
    }

    private void markUsed(int cellX, int cellZ) {
        usedCells.add(key(cellX, cellZ));
        save();
    }

    private Zone toZone(int cellX, int cellZ) {
        int centerX = cellX * cellSize;
        int centerZ = cellZ * cellSize;
        return new Zone(cellX, cellZ, centerX, centerZ, cellSize);
    }

    public int getUsedCount() {
        return usedCells.size();
    }

    private void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<String> raw = yaml.getStringList("used");
        for (String entry : raw) {
            String[] parts = entry.split(",");
            if (parts.length != 2) continue;
            try {
                int cx = Integer.parseInt(parts[0].trim());
                int cz = Integer.parseInt(parts[1].trim());
                usedCells.add(key(cx, cz));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        List<String> raw = new ArrayList<>();
        for (Long k : usedCells) {
            int cx = (int) (k >> 32);
            int cz = (int) (long) k;
            raw.add(cx + "," + cz);
        }
        yaml.set("used", raw);
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder " + file.getName() + " : " + e.getMessage());
        }
    }

    /** Représente une zone allouée (cellule de la grille). */
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
