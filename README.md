<div align="center">

# 🏆 HungerGames

### Plugin battle royale *à l'ancienne* pour serveurs **Paper 1.21**

*Spawns dispersés sur une vraie map générée · arènes persistantes qui tournent en boucle · bordures de zone individuelles par joueur · mode spectateur complet · GUI d'arènes dynamique · lobby central procédural · kits configurables.*

</div>

---

## 🎮 Qu'est-ce que c'est ?

**HungerGames** est un plugin Minecraft qui transforme votre serveur Paper 1.21 en arène de battle royale "à l'ancienne", dans l'esprit des Hunger Games d'origine : pas de plateau ni de coffres scénarisés, **une vraie carte générée par Minecraft**, des joueurs dispersés au compte à rebours, puis un seul survivant.

Chaque partie se joue sur une **zone de 1000×1000 blocs** tirée au hasard dans un grand pool de cellules, ce qui garantit que **deux parties ne se jouent jamais au même endroit**. À la fin d'une partie, la zone est régénérée puis remise dans le pool ; l'arène continue ensuite immédiatement sur une nouvelle cellule, sans intervention de l'admin.

---

## ✨ Fonctionnalités principales

| Catégorie | Ce que vous obtenez |
|---|---|
| 🌍 **Zones jamais réutilisées** | Pool de ~10 000 cellules autour du hub ; tirage aléatoire à chaque partie, régénération automatique des zones jouées. |
| 🏗️ **Arènes persistantes** | Créez une arène nommée, elle tourne en boucle sous ce nom tant que vous ne la supprimez pas. Persistance des cellules dans `arenas.yml` à travers les redémarrages. |
| 🧊 **Lobby flottant sécurisé** | Plateforme de verre au centre de chaque zone, entourée d'une cage de barrières invisibles pendant l'attente (retirée au lancement). |
| 🚧 **Bordures par joueur** | Chaque arène applique une `WorldBorder` *individuelle* à ses participants — plusieurs arènes peuvent tourner simultanément sans interférer. |
| 👁️ **Mode spectateur complet** | Suivez n'importe quelle partie en `SPECTATOR`, téléporté sur le lobby de la zone avec la bordure appliquée. |
| 🗂️ **GUI d'arènes dynamique** | Inventaire 54-slots, paginé, qui liste toutes les zones avec couleur selon l'état (vert = rejoignable, jaune = chargement, rouge = en cours). Mise à jour en temps réel. |
| 🏛️ **Lobby central procédural** | `/hgadmin hub build` construit un plaza, des collines, des montagnes, un campement, une entrée de mine, et des **PNJ villageois** qui ouvrent le GUI d'arènes. |
| 🎒 **Kits configurables** | 4 kits par défaut (Guerrier, Bucheron, Mineur, Archer), entièrement éditables via `/hgadmin kit ...`. |
| 📦 **Confort** | Inventaire vidé à chaque retour au hub, faim toujours pleine hors partie, pseudos masqués pendant la partie, scatter intelligent (pas de spawn en pleine mer), suggestions cliquables. |
| ⌨️ **Complétion `TAB`** | `/hg` et `/hgadmin` proposent contextuellement les sous-commandes, noms de zones et identifiants de kits. |

---

## 🚀 Installation

1. Téléchargez `hungergames-1.2.0.jar` depuis la [release v1.2.0](../../releases/latest).
2. Copiez-le dans le dossier `plugins/` de votre serveur **Paper 1.21.x**.
3. (Re)démarrez le serveur : `config.yml` et `kits.yml` sont générés automatiquement.
4. Éditez `world` dans `config.yml` pour cibler le monde vanilla généré que vous voulez utiliser.
5. Construisez votre lobby avec `/hgadmin hub build`, puis créez votre première arène : `/hgadmin zone create maZone1`.

⚠️ Le plugin dépend de `paper-api` ; il ne fonctionnera **pas** sur Spigot/CraftBukkit vanilla (la régénération de chunks et la `WorldBorder` par joueur sont spécifiques à Paper).

---

## 📖 Guide rapide

### Cycle de vie d'une arène

1. **Création** — `/hgadmin zone create <nom>` : tirage d'une cellule libre, construction du lobby flottant, préchargement asynchrone des chunks.
2. **Attente** — L'arène reste ouverte ; les joueurs la rejoignent via `/hg join <nom>` ou le GUI.
3. **Lancement** — Dès que `min-players` est atteint et que la zone est prête, un compte à rebours démarre (ou `/hgadmin zone forcestart <nom>`).
4. **Partie** — Scatter aléatoire → période de grâce sans PVP → PVP → bordure qui se referme vers le centre.
5. **Fin & cycle** — L'arène tire immédiatement une **nouvelle** cellule (forcément différente), reconstruit son lobby, et redevient jouable. L'ancienne zone est régénérée en arrière-plan puis relâchée dans le pool.

### Commandes joueur

```
/hg join [nom]    Rejoindre une arène par son nom (ou une au hasard)
/hg leave         Quitter la partie (ou le mode spectateur)
/hg kit           Choisir un kit
/hg arenas        Ouvrir le GUI listant toutes les arènes (alias : /hg gui)
/hg unspectate    Quitter le mode spectateur
```

### Commandes admin (`hungergames.admin`, op par défaut)

```
/hgadmin zone create <nom>       Créer une nouvelle arène persistante
/hgadmin zone delete <nom>       Supprimer définitivement une arène
/hgadmin zone rename <a> <b>     Renommer une arène
/hgadmin zone list               Lister toutes les arènes et leur état
/hgadmin zone info <nom>         Détails (cellule, joueurs, spectateurs...)
/hgadmin zone tp <nom>           Téléportation au lobby de l'arène
/hgadmin zone forcestart <nom>   Forcer le démarrage d'une partie

/hgadmin hub build               Construire (ou reconstruire) le lobby central
/hgadmin hub delete              Supprimer le lobby construit

/hgadmin kit create <id> <nom>   Créer un kit
/hgadmin kit delete <id>         Supprimer un kit
/hgadmin kit additem <id>        Ajouter l'item en main au kit
/hgadmin kit seticon <id>        Définir l'item en main comme icône
/hgadmin kit list                Lister les kits

/hgadmin reload                  Recharger config.yml et kits.yml
/hgadmin list                    Nombre d'arènes actives / cellules du pool occupées
```

---

## 🆕 Nouveautés de la version 1.2.0

La 1.2.0 est une mise à jour majeure qui transforme le plugin d'un simple « système de zones » en une expérience complète clé-en-main :

### 🏛️ Lobby central procédural (`/hgadmin hub build`)
Fini le lobby vide : la commande construit un vrai mini-village autour du spawn — plaza circulaire en quartz, collines herbeuses qui montent en montagnes enneigées, arbres, fleurs, un petit campement (feu de camp, table de craft, coffre, four, bottes de foin), et une **entrée de mine creusée** dans un flanc de colline avec minerais apparents. Un **mur de barrières invisibles** doublé de la `WorldBorder` du hub empêche quiconque de s'en égarer.

### 🧑‍🌾 PNJ « Rejoindre une arène »
3 ou 5 (configurable) pupitres en bloc d'émeraude alignés face au spawn, chacun surmonté d'un villageois figé (sans IA, invulnérable). Un clic droit dessus ouvre le même GUI que `/hg gui`.

### ♻️ Cycle automatique des arènes
Les arènes nommées tournent désormais **en continu** : à chaque fin de partie, la zone jouée est régénérée en arrière-plan (`World#regenerateChunk`, étalé pour préserver le TPS) puis relâchée dans le pool, et l'arène tire immédiatement une nouvelle cellule. Plus besoin de recréer une arène à chaque fois.

### 💾 Persistance des arènes
Chaque arène sauvegarde sa cellule actuelle dans `plugins/HungerGames/arenas.yml`. Au redémarrage du plugin, toutes les arènes sont automatiquement recréées sur leur cellule (et leur préchargement relancé) — sans intervention manuelle.

### 🎒 Items de salle d'attente
Chaque joueur en attente reçoit automatiquement une **épée en pierre** (slot 1, ouvre `/hg kit`) et un **bloc barrière** (slot 5, exécute `/hg leave`), tous deux protégés contre le drop et le déplacement.

### 👁️ Mode spectateur amélioré
- Boussole de spectateur (slot 8) protégée contre le drop/le déplacement, comme dans HikaBrain.
- Spectateurs automatiquement déplacés en `GameMode.SPECTATOR` à leur mort, avec la bordure de la zone appliquée pour ne pas pouvoir voler au-delà.
- Bouton central « Rejoindre une partie aléatoire » dans le GUI.

### 🪧 Cage de lobby dynamique
La plateforme flottante est entourée d'une cage de barrières pendant l'attente (impossible d'en tomber ou de s'en éloigner en marchant). La cage est **automatiquement retirée** au lancement de la partie pour que les spectateurs puissent voler librement dans toute la zone.

### 🎯 Scatter intelligent (`RandomLocationUtil`)
Les points de spawn sont désormais rejetés s'ils tombent dans l'eau, la glace ou la lave — fini les apparitions en plein océan. Repli progressif de la distance minimale entre joueurs si la zone est trop densément aquatique.

### 👤 Pseudos masqués pendant la partie
Pendant `GRACE_PERIOD` et `PVP`, le pseudo au-dessus de la tête de chaque participant est masqué via une équipe de scoreboard dédiée (`NAME_TAG_VISIBILITY = NEVER`), puis restauré à la fin.

### 📊 GUI d'arènes en temps réel
Une tâche répétitive rafraîchit le contenu de l'inventaire toutes les secondes sans le fermer/rouvrir — le nombre de joueurs et l'état des zones restent à jour en direct.

### 🛡️ Divers conforts
- Inventaire vidé à chaque retour au hub ou à chaque entrée d'arène.
- Faim toujours pleine hors partie active (annule les pertes et remet la barre à 20).
- `WorldBorder` configurée avec `damageBuffer = 0` et `damageAmount = 2.0` (1 cœur/seconde hors limites, sans marge par défaut).
- Une zone en cours de préchargement apparaît en jaune « Chargement... » dans le GUI et n'est ni cliquable pour rejoindre, ni proposée par `/hg join` ou le bouton aléatoire tant qu'elle n'est pas prête.

---

## ⚙️ Configuration (`config.yml`)

Les réglages les plus importants sont commentés en français dans le fichier. Quelques points clés :

- **`world`** : le monde vanilla (généré, pas plat) où les zones sont allouées.
- **`hub.x / hub.y / hub.z`** : centre du hub borné (par défaut `0.5, 100, 0.5`).
- **`hub.radius`** : rayon du hub en blocs (défaut 100, soit de -100 à 100 en X/Z).
- **`hub.build.radius`** : rayon de la zone décorée par `/hgadmin hub build` (40 par défaut).
- **`hub.build.npc-count`** : nombre de PNJ « rejoindre une arène » (1 à 7, 1 par défaut).
- **`zone.size`** : diamètre d'une zone (1000 par défaut = rayon 500).
- **`zone.pool-radius-cells`** : rayon du pool en nombre de cellules (50 par défaut, ~10 000 cellules possibles).
- **`zone.chunks-per-tick`** : vitesse de préchargement (8 par défaut).
- **`zone.regen-chunks-per-tick`** : vitesse de régénération en fin de partie (2 par défaut, plus conservateur).
- **`lobby.cage-margin`** / **`lobby.cage-height`** : géométrie de la cage du lobby flottant.
- **`game.min-players`** / **`game.max-players`** : seuils de joueurs par arène.
- **`game.grace-period-seconds`** : durée sans PVP après le scatter (300 par défaut).
- **`game.border.shrink.*`** : rétrécissement de la bordure après la période de grâce.

---

## 🔨 Compilation

```bash
mvn clean package
```

Le jar est généré dans `target/hungergames-1.2.0.jar`.

> **Note API Paper 1.21.1** : la régénération de chunks (`World#regenerateChunk`) est une API **spécifique à Paper** — elle lève une exception sur Spigot/CraftBukkit vanilla. C'est pour cela que le plugin dépend de `paper-api` et pas de `bukkit-api`.

---

## 📜 Limitations connues & pistes d'amélioration

- La régénération vanilla recalcule le terrain à partir du générateur du monde : toute construction permanente dans une cellule du pool sera perdue à la première régénération. Les zones du pool doivent rester de la nature « brute ».
- La taille d'une zone (`zone.size`) est globale à tout le pool ; impossible d'avoir des arènes de tailles différentes sans changer la config pour tout le monde.
- Le « mid » de la map n'est pas matérialisé par une structure — c'est simplement le centre géométrique de la zone (le lobby flottant est juste au-dessus).
- Pas de système d'alliance in-game : comme demandé, ça reste au niveau des messages privés entre joueurs, en dehors du plugin.
- La complétion `TAB` propose les noms de zones/kits déjà existants et les sous-commandes, mais ne valide pas qu'un nouveau nom n'est pas déjà pris.

---

## 📝 Licence & crédits

Plugin écrit pour la communauté francophone HungerGames. Développé par **zzaee**.

Suggestions, bugs, pull requests : ouvrez une [issue](../../issues) sur le dépôt.
