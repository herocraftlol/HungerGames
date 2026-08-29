package com.herocraft.hungergames.arena;

public enum ArenaState {
    /** Zone allouée, préchargement des chunks en cours. */
    PRELOADING,
    /** En attente de joueurs (lobby flottant ouvert, préchargement terminé). */
    WAITING,
    /** Compte à rebours avant le scatter. */
    STARTING,
    /** Joueurs dispersés, PVP désactivé, farm autorisé. */
    GRACE_PERIOD,
    /** PVP activé, bordure éventuellement en train de rétrécir. */
    PVP,
    /** Partie terminée, nettoyage en cours. */
    ENDED
}
