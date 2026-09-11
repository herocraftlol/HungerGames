package com.herocraft.hungergames.arena;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Charge/génère à l'avance tous les chunks d'une {@link ZoneAllocator.Zone} avant
 * qu'une partie ne puisse commencer, pour éviter tout lag de génération pendant
 * le jeu et pour que la carte soit "indisponible" tant qu'elle n'est pas prête.
 */
public class ChunkPreloader {

    private final JavaPlugin plugin;
    private final int chunksPerTick;

    public ChunkPreloader(JavaPlugin plugin, int chunksPerTick) {
        this.plugin = plugin;
        this.chunksPerTick = Math.max(1, chunksPerTick);
    }

    /**
     * @param onProgress appelé régulièrement avec (chunksCharges, chunksTotal)
     * @param onDone     appelé une fois tous les chunks chargés
     */
    public void preload(World world, ZoneAllocator.Zone zone, BiConsumer<Integer, Integer> onProgress, Runnable onDone) {
        int minChunkX = zone.minX() >> 4;
        int maxChunkX = zone.maxX() >> 4;
        int minChunkZ = zone.minZ() >> 4;
        int maxChunkZ = zone.maxZ() >> 4;

        Deque<int[]> queue = new ArrayDeque<>();
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                queue.add(new int[]{cx, cz});
            }
        }
        int total = queue.size();
        int[] loaded = {0};
        boolean[] doneFired = {false};

        new BukkitRunnable() {
            @Override
            public void run() {
                if (queue.isEmpty()) {
                    if (loaded[0] >= total && !doneFired[0]) {
                        doneFired[0] = true;
                        onDone.run();
                    }
                    if (loaded[0] >= total) {
                        cancel();
                    }
                    return;
                }
                int batch = 0;
                List<int[]> batchList = new ArrayList<>();
                while (batch < chunksPerTick && !queue.isEmpty()) {
                    batchList.add(queue.poll());
                    batch++;
                }
                for (int[] coords : batchList) {
                    world.getChunkAtAsync(coords[0], coords[1]).thenAccept(chunk -> {
                        loaded[0]++;
                        onProgress.accept(loaded[0], total);
                    });
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
