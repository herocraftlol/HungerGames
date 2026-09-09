package com.herocraft.hungergames.arena;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Chaque arène vit désormais dans son propre monde Bukkit dédié (au lieu d'une
 * simple région d'un grand monde partagé), pour pouvoir utiliser la VRAIE
 * bordure de monde vanilla ({@code World#getWorldBorder()}) — animée et
 * infligeant ses dégâts nativement, fiable, au lieu d'une bordure "virtuelle"
 * par-joueur (celle-ci a un bug connu côté Paper qui empêchait un rendu et une
 * animation fiables, voir le README).
 *
 * Un nom de monde unique est généré à chaque création (préfixe + UUID court) :
 * pas besoin de gérer un pool de cellules à réserver/libérer comme avant,
 * puisque chaque monde est indépendant et son dossier est supprimé du disque
 * une fois l'arène régénérée/détruite.
 */
public class WorldAllocator {

    private final JavaPlugin plugin;
    private final String namePrefix;

    public WorldAllocator(JavaPlugin plugin, String namePrefix) {
        this.plugin = plugin;
        this.namePrefix = namePrefix;
    }

    /** Crée un nouveau monde vanilla dédié (génération normale, pas plat/vide), avec sa bordure configurée. */
    public World createArenaWorld(int size) {
        String worldName = namePrefix + UUID.randomUUID().toString().substring(0, 8);

        World world = new WorldCreator(worldName)
                .environment(World.Environment.NORMAL)
                .type(WorldType.NORMAL)
                .generateStructures(true)
                .createWorld();

        if (world == null) {
            throw new IllegalStateException("Échec de création du monde d'arène '" + worldName + "'.");
        }

        world.setAutoSave(false);
        world.setKeepSpawnInMemory(false);

        org.bukkit.WorldBorder border = world.getWorldBorder();
        border.setCenter(0.5, 0.5);
        border.setSize(size);
        border.setDamageAmount(2.0);
        border.setDamageBuffer(0.0);
        border.setWarningDistance(0);

        return world;
    }

    /**
     * Décharge et supprime définitivement du disque le monde d'une arène. À appeler
     * uniquement une fois tous les joueurs/spectateurs sortis du monde (sinon
     * Bukkit refuse de le décharger).
     */
    public void deleteArenaWorld(World world) {
        if (world == null) return;
        String name = world.getName();
        File folder = world.getWorldFolder();

        boolean unloaded = Bukkit.unloadWorld(world, false);
        if (!unloaded) {
            plugin.getLogger().warning("Impossible de décharger le monde '" + name + "' (des entités/joueurs y sont peut-être encore).");
            return;
        }

        // La suppression du dossier (I/O disque) se fait en arrière-plan pour ne
        // pas geler le serveur, une fois le monde correctement déchargé (étape
        // ci-dessus, elle, obligatoirement synchrone côté API Bukkit).
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Stream<Path> walk = Files.walk(folder.toPath())) {
                walk.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                plugin.getLogger().warning("Impossible de supprimer le dossier du monde '" + name + "' : " + e.getMessage());
            }
        });
    }

    /**
     * Recharge un monde d'arène existant sur disque (utilisé au redémarrage pour
     * restaurer une arène sauvegardée). Si le dossier n'existe plus, un nouveau
     * monde vide de ce nom est créé à la place (cas rare, log un avertissement).
     */
    public World loadOrCreateArenaWorld(String worldName, int size) {
        World existing = Bukkit.getWorld(worldName);
        if (existing != null) return existing;

        World world = new WorldCreator(worldName)
                .environment(World.Environment.NORMAL)
                .type(WorldType.NORMAL)
                .createWorld();

        if (world == null) {
            throw new IllegalStateException("Échec de rechargement du monde d'arène '" + worldName + "'.");
        }

        world.setAutoSave(false);
        world.setKeepSpawnInMemory(false);

        org.bukkit.WorldBorder border = world.getWorldBorder();
        border.setCenter(0.5, 0.5);
        border.setSize(size);
        border.setDamageAmount(2.0);
        border.setDamageBuffer(0.0);
        border.setWarningDistance(0);

        return world;
    }
}
