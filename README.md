# 🏆 HungerGames

> Plugin Minecraft **Paper 1.21** — Battle royale "à l'ancienne", sur un monde réel généré, avec des zones **jamais réutilisées**.

---

## 🎮 C'est quoi HungerGames ?

HungerGames est un plugin de **battle royale "à l'ancienne"** pour serveurs **Paper 1.21**. Les joueurs apparaissent dispersés sur une vraie map générée — pas de coffres ni de scénario façon jeu de plateau—et chaque partie utilise une **nouvelle zone jamais réutilisée** du monde.

Le principe repose sur une mécanique originale : le plugin découpe le monde en **grandes cellules carrées** et alloue à chaque partie la **prochaine cellule jamais utilisée**, retrouvée par une recherche en spirale à partir du centre du monde. Une fois qu'une zone a servi, elle est marquée dans `plugins/HungerGames/zones.yml` et **ne sera plus jamais réutilisée** : chaque partie se joue donc sur un terrain intact, sans traces des parties précédentes.

![Version](https://img.shields.io/badge/version-1.0.0-green)
![Paper](https://img.shields.io/badge/Paper-1.21-blue?logo=minecraft)

---

## ✨ Fonctionnement

1. **`/hg join`** — rejoint une arène disponible, ou en crée une nouvelle.
   - À la création, le plugin alloue automatiquement la **prochaine cellule de grille jamais utilisée** — taille configurable 1000×1000 par défaut, soit un rayon de 500 blocs.
   - Une petite **plateforme de verre** est construite immédiatement au centre de la zone, en l'air — `lobby.y` configurable — elle sert de lobby pour cette partie.
   - Le reste de la zone — jusqu'à ~4000 chunks pour 1000×1000 — est préchargé et généré **en arrière-plan**, avec une **barre de progression** visible par les joueurs en attente.
2. **`/hg kit`** — ouvre un menu de sélection de **kit** avant le lancement. Les admins peuvent configurer des kits — épée en pierre, pioche en bois, arc et flèches... — qui donnent un simple avantage de départ.
3. Dès que le nombre minimum de joueurs est atteint **et** que la zone est prête, un **compte à rebours** démarre — `game.countdown-seconds`.
4. Au lancement, chaque joueur est **téléporté à un point aléatoire** dispersé dans la zone — distance minimale entre joueurs configurable —, reçoit son kit, et une **bordure de monde personnelle** — Paper per-player `WorldBorder` — est posée sur les limites de la zone.
5. Pendant la **période de grâce** — `game.grace-period-seconds`, 5 min par défaut — le PVP est désactivé, les joueurs peuvent farmer tranquillement.
6. Une fois la période de grâce terminée : le **PVP est activé**, et si `game.border.shrink.enabled` est à `true`, la bordure se referme progressivement vers le centre pour **forcer les joueurs à se regrouper**.
7. Le **dernier survivant** remporte la partie ! Tout le monde est renvoyé au hub, l'arène est détruite, mais la zone reste marquée comme utilisée **pour toujours** — `zones.yml`.

---

## 📦 Installation

1. Compiler le plugin — voir ci-dessous — ou télécharger le `.jar` depuis les [releases](../../releases).
2. Copier `hungergames-1.0.0.jar` dans le dossier `plugins/` de votre serveur **Paper 1.21.x**.
3. Redémarrer le serveur. Le plugin génère automatiquement `config.yml`, `kits.yml` et `zones.yml` dans `plugins/HungerGames/`.

### Compilation

```bash
mvn clean package
```

Le jar est généré dans `target/hungergames-1.0.0.jar`.

> ⚠️ Paper 1.21 a migré une partie du système d'Attributs — `Attribute` — vers un `Registry`. Le code utilise `Attribute.GENERIC_MAX_HEALTH` — toujours présent en compat sur la plupart des builds 1.21.1. Si votre build précise donne une erreur de compilation dessus, utilisez l'équivalent du Registry—`RegistryAccess.registryAccess().getRegistry()`—selon la version exacte de votre paper-api.

---

## 🕹️ Commandes

### Joueurs

| Commande | Description |
|-----------|-------------|
| `/hg join` | Rejoindre une partie — ou en créer une nouvelle |
| `/hg leave` | Quitter la partie en cours |
| `/hg kit` | Ouvrir le menu de sélection des kits |

### Admin — `hungergames.admin`, op par défaut

| Commande | Description |
|-----------|-------------|
| `/hgadmin reload` | Recharge `config.yml` et `kits.yml` |
| `/hgadmin list` | Nombre d'arènes actives / zones déjà utilisées |
| `/hgadmin kit create <id> <nom>` | Crée un nouveau kit |
| `/hgadmin kit delete <id>` | Supprime un kit existant |
| `/hgadmin kit additem <id>` | Ajoute l'item en main au contenu du kit |
| `/hgadmin kit seticon <id>` | Définit l'item en main comme icône du kit dans le menu |
| `/hgadmin kit list` | Liste tous les kits disponibles |

---

## ⚙️ Configuration — `config.yml`

| Option | Description |
|--------|-------------|
| `world` | Monde — généré, pas plat — dans lequel les arènes sont créées |
| `zone.size` | Diamètre d'une zone en blocs — 1000 = rayon 500 |
| `zone.scatter-margin` | Marge de sécurité à l'intérieur de la bordure pour le scatter |
| `zone.chunks-per-tick` | Chunks chargés/générés par tick pendant le préchargement |
| `lobby.y` | Hauteur de la plateforme flottante de lobby |
| `lobby.radius` | Rayon de la plateforme de lobby |
| `game.min-players` | Nombre minimum de joueurs pour lancer la partie |
| `game.max-players` | Nombre maximum de joueurs — jusqu'à 100 |
| `game.countdown-seconds` | Durée du compte à rebours avant le lancement |
| `game.grace-period-seconds` | Durée sans PVP après le scatter |
| `game.scatter.min-distance-between-players` | Distance minimale entre les joueurs au scatter |
| `game.border.shrink.*` | Rétrécissement progressif de la bordure après la période de grâce |

---

## 🆕 Nouveautés en 1.0.0

- 🎯 **Zones jamais réutilisées** : allocation automatique par recherche en spirale, persistance dans `zones.yml`.
- 🎮 **Vraie map générée** : pas de plateau, le terrain est généré naturellement à chaque partie.
- 🧊 **Lobby flottant** : plateforme de verre au centre de chaque zone, en l'air.
- ⏳ **Préchargement asynchrone** des chunks avec barre de progression visible.
- 🗺️ **Bordure par joueur** : chaque participant voit sa propre `WorldBorder` sur les limites de sa zone.
- 🛡️ **Période de grâce** : PVP désactivé au début, pour farmer en paix.
- 📉 **Bordure rétrécissante** : la zone jouable se referme progressivement pour forcer les confrontations.
- 🎒 **Kits configurables** avec menu de sélection graphique—GUI—, gestion complète par commandes admin.
- 📊 **Scoreboard** dédié et suivi du nombre de joueurs restants.

---

## 📁 Structure du projet

```
src/main/java/com/herocraft/hungergames/
├── HungerGamesPlugin.java          # Classe principale—JavaPlugin
├── arena/                           # Gestion des arènes, zones et préchargement
│   ├── Arena.java
│   ├── ArenaManager.java
│   ├── ArenaState.java
│   ├── ChunkPreloader.java
│   └── ZoneAllocator.java
├── command/                         # Commandes joueur et admin
│   ├── HGCommand.java
│   └── HGAdminCommand.java
├── kit/                             # Système de kits
│   ├── Kit.java
│   ├── KitManager.java
│   └── KitSelectorGUI.java
├── listener/                        # Écouteurs d'événements
│   ├── PlayerListener.java
│   └── CombatListener.java
└── util/                            # Utilitaires
    ├── RandomLocationUtil.java
    └── ScoreboardUtil.java
```

---

## ❗ Limitations connues / pistes d'amélioration

- Le hub — point de retour hors partie — est un point fixe dans le même monde — `hub.x/y/z` — pensezà construire une petite zone de spawn sûre à ces coordonnées.
- Le "mid" de la map n'est pas matérialisé par une structure : c'est simplement le centre géométrique de la zone. Un marqueur—beacon, colonne—pourra être ajouté à l'avenir pour le rendre visible depuis le sol.
- Pas de système d'alliance in-game : les alliances se font au niveau des messages privés entre joueurs, en dehors du plugin.
- Le fichier `zones.yml` grossit indéfiniment—une ligne par partie jouée—, c'est voulu pour garantir zéro réutilisation, mais pensezà surveiller sa taille sur le très long terme.

---

## 📄 Licence

Ce projet est publié sous licence **MIT**.

© herocraftlol — HungerGames
