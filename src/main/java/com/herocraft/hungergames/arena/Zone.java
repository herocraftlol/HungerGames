package com.herocraft.hungergames.arena;

/**
 * Décrit une zone carrée de {@code size} blocs de côté centrée sur
 * (centerX, centerZ). Utilisé aussi bien pour la zone de construction du hub
 * (dans le monde partagé) que pour la zone jouable d'une arène (toujours
 * centrée sur (0,0) dans son propre monde dédié, voir {@link WorldAllocator}).
 */
public record Zone(int centerX, int centerZ, int size) {

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
