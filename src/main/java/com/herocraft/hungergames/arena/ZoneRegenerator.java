package com.herocraft.hungergames.arena;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BiConsumer;

/**
 * Régénère (au sens vanilla : recalcule le terrain à partir du générateur du monde,
 * comme s'il n'avait jamais été modifié) tous les chunks d'une zone, une fois qu'une
 * partie y est terminée. Contrairement au préchargement ({@link ChunkPreloader}), qui
 * peut charger les chunks de façon asynchrone, la régénération d'un chunk
 * ({@code World#regenerateChunk}) doit s'exécuter sur le thread principal : le travail
 * est donc étalé sur plusieurs ticks (peu de chunks par tick) pour éviter tout à-coup
 * de TPS pendant qu'une autre partie tourne peut-être en parallèle ailleurs sur la map.
 */
public class ZoneRegenerator {

    private final JavaPlugin plugin;
    private final int chunksPerTick;

    public ZoneRegenerator(JavaPlugin plugin, int chunksPerTick) {
        this.plugin = plugin;
        this.chunksPerTick = Math.max(1, chunksPerTick);
    }

    /**
     * @param onProgress appelé régulièrement avec (chunks régénérés, chunks total)
     * @param onDone     appelé une fois toute la zone régénérée
     */
    public void regenerate(World world, ZoneAllocator.Zone zone, BiConsumer<Integer, Integer> onProgress, Runnable onDone) {
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
        int[] done = {0};

        new BukkitRunnable() {
            @Override
            public void run() {
                if (queue.isEmpty()) {
                    onDone.run();
                    cancel();
                    return;
                }
                for (int i = 0; i < chunksPerTick && !queue.isEmpty(); i++) {
                    int[] coords = queue.poll();
                    try {
                        world.regenerateChunk(coords[0], coords[1]);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Échec de régénération du chunk (" + coords[0] + "," + coords[1] + ") : " + e.getMessage());
                    }
                    done[0]++;
                }
                onProgress.accept(done[0], total);
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
