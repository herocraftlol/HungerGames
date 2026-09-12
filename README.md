<div align="center">

# 🏆 HungerGames

### Plugin battle royale *à l'ancienne* pour serveurs **Paper 1.21**

*Spawns dispersés sur une vraie map générée · arènes persistantes qui tournent en boucle · bordures de zone individuelles par joueur avec dégâts vraiment fiables et réduction vraiment progressive · bordure en deux phases avec kill-feed · bordure vanilla repeinte en continu pour compenser le bug Paper · morts invisibles en silence avec boussole de suivi · mode spectateur complet · GUI d'arènes dynamique · tableau des scores persistant · lobby central procédural · kits configurables.*

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
| 🧊 **Lobby flottant sécurisé, puis entièrement retiré** | Plateforme de verre au centre de chaque zone, entourée d'une cage de barrières invisibles pendant l'attente ; **cage + verre + lanterne marine supprimés dès le lancement** pour libérer l'espace aux spectateurs. |
| 🚧 **Bordures par joueur, vraiment fiables** | Chaque arène applique une `WorldBorder` *individuelle* à ses participants — plusieurs arènes peuvent tourner simultanément sans interférer. Les dégâts hors-bordure sont infligés manuellement (~1 ❤️/s, sans buffer) et la taille est **réellement interpolée** à chaque tick pour suivre l'animation annoncée. |
| 🧱 **Bordure vanilla repeinte à chaque tick** | Comme `WorldBorder#setSize(cible, durée)` ne s'anime pas toujours correctement côté client pour une bordure virtuelle par-joueur (bug Paper connu), on **renvoie nous-mêmes `setSize(diamètreActuel)` à chaque seconde de bordure** : le client reçoit ainsi en continu la position réelle du mur vanilla, sans aucun rendu custom. |
| 📉 **Bordure en deux phases, vraiment progressive** | Une première réduction, une pause stabilisée, puis une réduction finale jusqu'au centre, **avec la phase, la taille courante et le temps restant affichés en direct dans le tableau de bord**. La taille affichée/utilisée est interpolée manuellement pour rester synchronisée avec le temps annoncé. |
| 📏 **Distance du centre dans le scoreboard** | Le tableau de bord affiche aussi la **distance au sol (XZ)** entre le joueur et le centre exact de la zone, mise à jour en temps réel pendant la partie. |
| 💬 **Kill-feed en jeu** | À chaque mort, un message est envoyé à tous les participants et spectateurs de l'arène : « X a éliminé Y ! » ou « Y est mort. ». |
| 👁️ **Mode spectateur complet** | Suivez n'importe quelle partie en `SPECTATOR`, téléporté sur le lobby de la zone avec la bordure appliquée. |
| 🕶️ **Morts vraiment invisibles et silencieux** | Les joueurs éliminés deviennent **invisibles aux vivants** (`hidePlayer`), leur **chat est isolé** entre morts/spectateurs de la même arène, et une **boussole de suivi** leur ouvre un GUI des têtes des joueurs encore en vie pour se téléporter à l'un d'eux. |
| 🗂️ **GUI d'arènes dynamique** | Inventaire 54-slots, paginé, qui liste toutes les zones avec couleur selon l'état (vert = rejoignable, jaune = chargement, rouge = en cours). Mise à jour en temps réel. |
| 🏛️ **Lobby central procédural** | `/hgadmin hub build` construit un plaza, des collines, des montagnes, un campement, une entrée de mine, des **PNJ villageois** qui ouvrent le GUI d'arènes, et un **PNJ dédié au tableau des scores**. |
| 🏆 **Tableau des scores persistant** | Chaque victoire est enregistrée dans `stats.yml` ; ouvrez le classement à tout moment avec `/hg top` (alias `/hg scores`) ou clic droit sur le PNJ trophée du hub — classement en têtes de joueurs, médailles 🥇🥈🥉, persistant à travers les redémarrages. |
| 🎒 **Kits configurables** | 4 kits par défaut (Guerrier, Bucheron, Mineur, Archer), entièrement éditables via `/hgadmin kit ...`. |
| 📦 **Confort** | Délai de fin configurable avant le retour automatique au hub, **XP remise à zéro** au lancement, inventaire vidé à chaque retour au hub, faim toujours pleine hors partie, pseudos masqués pendant la partie, scatter intelligent (pas de spawn en pleine mer), suggestions cliquables. |
| ⌨️ **Complétion `TAB`** | `/hg` et `/hgadmin` proposent contextuellement les sous-commandes, noms de zones et identifiants de kits. |

---

## 🚀 Installation

1. Téléchargez `hungergames-1.11.0.jar` depuis la [release v1.11.0](../../releases/latest).
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
3. **Lancement** — Dès que `min-players` est atteint et que la zone est prête, un compte à rebours démarre (ou `/hgadmin zone forcestart <nom>`). À T0 : la cage de barrières, la plateforme de verre et la lanterne marine du lobby sont **toutes retirées**, le niveau d'XP de chaque joueur est remis à zéro, et les pseudos au-dessus des têtes sont masqués.
4. **Partie** — Scatter aléatoire → période de grâce sans PVP → PVP → bordure en deux phases (réduction → pause → réduction finale), avec un **mur de particules rouges** visible à chaque bord et des **dégâts manuels** appliqués si un joueur se retrouve hors bordure.
5. **Mort en partie** — Le joueur devient spectateur de sa propre arène, devient invisible aux vivants (`hidePlayer`), ne peut plus chatter qu'avec les autres morts/spectateurs, et reçoit une boussole « Suivre un joueur en vie » qui ouvre un GUI listant les têtes des survivants.
6. **Fin & cycle** — À la dernière mort, tout le monde reste `end-delay-seconds` secondes (10 par défaut) avant le retour automatique au hub ; le vainqueur reste sur la zone, les autres sont spectateurs. L'arène tire ensuite immédiatement une **nouvelle** cellule (forcément différente), reconstruit son lobby, et redevient jouable. L'ancienne zone est régénérée en arrière-plan puis relâchée dans le pool.

### Commandes joueur

```
/hg join [nom]    Rejoindre une arène par son nom (ou une au hasard)
/hg leave         Quitter la partie (ou le mode spectateur)
/hg kit           Choisir un kit
/hg arenas        Ouvrir le GUI listant toutes les arènes (alias : /hg gui)
/hg top           Ouvrir le tableau des scores des victoires (alias : /hg scores)
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

## 🆕 Nouveautés de la version 1.11.0

La 1.11.0 est une mise à jour de **fiabilité du rétrécissement de bordure** : la `WorldBorder` vanilla est désormais repeinte en continu pour suivre le compte à rebours annoncé, et le tableau de bord affiche en plus la distance au centre de la zone. Le rendu custom du mur (particules) a été retiré — il suffit désormais de bien tenir le mur vanilla à jour.

### 🧱 Bordure vanilla repeinte à chaque tick

Le rendu custom du mur de bordure en particules (`DUST` rouges, ajouté en 1.5.0) n'était qu'un pansement sur le bug de Paper #12372/#7748 : dans certaines conditions, `WorldBorder#setSize(cible, durée)` ne s'anime pas côté client pour une bordure virtuelle par-joueur. La 1.11.0 remplace ce pansement par une solution **plus simple et plus fiable** :

- 🎯 **`WorldBorder#setSize(diamètreActuel)` rappelé chaque seconde** depuis `tickBorderDamage`, sans durée (= instantané), sur la bordure de chaque joueur encore dans la zone.
- 📡 **Le client reçoit en continu** la taille interpolée du moment — la bordure vanilla se déplace donc visuellement comme annoncé, sans aucun rendu custom superposé.
- 🧹 **Code mort supprimé** : la tâche `tickBorderVisuals`, le rendu du mur `renderBorderWall` et les helpers `drawWallAtX/Z` ont été retirés — il n'y a plus qu'un seul chemin, et c'est le vanilla.
- ✨ **Cohérence parfaite** entre ce qui est annoncé dans le scoreboard (taille courante, temps restant) et ce qui est effectivement affiché au client.

### 📏 Distance du centre dans le tableau de bord

Pendant la partie, le tableau de bord indique maintenant **où l'on se trouve par rapport au centre de la zone** :

- 📐 **Nouvelle ligne « Distance du centre »** dans le tableau de bord, mise à jour en continu (distance XZ au sol, arrondie au mètre).
- 📊 **Nouvelle ligne « Taille de la zone »** qui affiche le **diamètre courant** interpolé (`getLiveBorderDiameter`) — pour qu'on voie la bordure rétrécir *aussi* dans le tableau de bord, pas seulement sur le terrain.
- 🛡️ **Fix d'affichage avant le premier rétrécissement** : `getLiveBorderDiameter` retombe sur la taille nominale de la zone tant que la phase 1 n'a pas démarré, au lieu d'afficher « 0m » comme avant.

### 🧹 Qualité de code

- 🔎 `tickBorderDamage` ne dépend plus que de `getCurrentBorderDiameter` pour tout (dégâts + repaint vanilla), et lit ses variables membres via des noms parlés (`diameter`, `half`, `centerX`, `centerZ`) plutôt que des valeurs redérivées en place.
- 🧽 Méthodes utilitaires bien commentées (`getLiveBorderDiameter`, `distanceToCenter`) pour expliquer *pourquoi* on retombait sur la taille nominale hors phase 1, et *comment* on calcule la distance au centre sans planter si le joueur a changé de monde.

---

## 🆕 Nouveautés de la version 1.6.0

La 1.6.0 est une mise à jour de **fiabilité de la bordure et d'expérience de fin de partie** : les morts deviennent de vrais spectateurs (invisibles et silencieux), la bordure est désormais fluide et fait vraiment des dégâts, et le lobby d'attente disparaît complètement au lancement pour ne laisser personne sur la cage.

### 🛡️ Bordure avec dégâts vraiment fiables

La `WorldBorder` par-joueur de Paper a un comportement connu sur les bordures *virtuelles* (sans tick de monde attitré) : ses **dégâts vanille** ne sont pas toujours appliqués correctement, et son **animation de rétrécissement** ne se déroule pas forcément côté serveur. Résultat avant la 1.6.0 : un joueur pouvait se retrouver juste hors bordure sans subir le moindre dégât, ou pire, se faire éliminer sans avoir vu la zone jouable bouger.

La 1.6.0 ne se fie plus du tout au système vanille pour les **dégâts** :

- 💥 **Tâche dédiée `tickBorderDamage`** (chaque seconde, du début du PVP jusqu'à la fin de la partie) qui compare la position réelle du joueur aux limites de sa bordure calculées par le plugin et applique `player.damage(2.0)` s'il est dehors.
- 🎯 **Dommages uniformes** : que la bordure soit en train de bouger ou parfaitement stable, un joueur hors zone prend exactement **2 points de dégâts par seconde** (1 ❤️/s sans marge), comme annoncé dans le tableau de bord.
- 🚫 **Plus de ticket à gratter** sur une bordure par-joueur capricieuse : la sécurité est désormais dans le code du plugin, pas dans une API tierce.

### 📐 Réduction de bordure vraiment progressive

Pour la même raison (bordure virtuelle par-joueur non rattachée au tick d'un vrai monde), `WorldBorder#getSize()` ne reflétait pas l'animation en cours côté serveur et sautait directement à la taille finale : dégâts et mur de particules suivaient donc une bordure qui *« téléportait »* au lieu de rétrécir.

La 1.6.0 calcule elle-même la taille de bordure utilisée pour les dégâts, le rendu du mur et l'application visuelle :

- 🎞️ **`getCurrentBorderDiameter()` interpole** entre la taille de départ et la cible sur toute la durée de chaque phase (`phase1`, `pause`, `phase2`), en se basant sur l'horloge système.
- 🪜 **Synchronisation parfaite avec le tableau de bord** : ce qui est annoncé (« bordure à 250 dans 1:23 ») est exactement ce qui est utilisé pour les dégâts et le mur de particules — fini la bordure qui « pop » au dernier moment.
- ♻️ **Enchaînement propre des phases** : la nouvelle phase repart bien de la taille *actuelle* interpolée (pas de la dernière cible brute), donc la transition reste fluide même si une phase a été interrompue avant la fin.

### 🕶️ Joueurs morts = vrais spectateurs

Avant la 1.6.0, un joueur éliminé était bien passé en `GameMode.SPECTATOR`, mais restait techniquement *visible* des autres vivants et pouvait leur écrire dans le chat. La 1.6.0 va jusqu'au bout de la logique « mort = spectateur » :

- 👻 **`hidePlayer` côté vivants** : à chaque mort, le défunt est caché (`Player#hidePlayer`) à tous les joueurs encore en vie de la même arène. La visibilité est intégralement restaurée à la fin de la manche via `restoreVisibility()`.
- 💬 **Chat isolé des morts** : un nouveau `DeadChatListener` intercepte `AsyncChatEvent` et ne relaie les messages d'un mort qu'aux autres morts/spectateurs de la même arène — les vivants (et le hub / les autres arènes) ne reçoivent rien. Plus de mort qui spoil une position dans le chat général.
- 🧭 **Boussole « Suivre un joueur en vie »** (slot 1 de la hotbar des morts) : clic droit pour ouvrir un **GUI 27-slots listant les têtes des joueurs encore en vie** de l'arène, avec leur pseudo actuel et leur nombre de points de vie. Cliquer sur une tête téléporte instantanément le mort à ce joueur (avec vérification qu'il est toujours vivant au moment du clic, sinon le slot est retiré dynamiquement du GUI).
- 🛡️ **Item protégé** : la boussole ne peut ni être lâchée ni déplacée dans l'inventaire (`DeathSpectateListener`).

Un mort ne peut plus rien voir de ce qui se passe côté vivant, ne peut plus rien leur écrire, et n'a qu'à choisir qui suivre : c'est maintenant un vrai spectateur.

### 🪟 Lobby d'attente entièrement retiré au lancement

Au lancement d'une partie, **toute la machinerie du lobby flottant est nettoyée** — plus seulement la cage :

- 🧹 **`removeLobbyCage()`** retire la cage de barrières (déjà fait avant la 1.6.0).
- 🧹 **`removeLobbyPlatform()`** (nouveau) efface aussi la **plateforme de verre** et la **lanterne marine** qui la soutenait.
- 👀 **Spectateurs libres de voler** : comme la plateforme disparaît, les spectateurs qui rejoignent une partie en cours ne sont plus coincés sur une petite dalle de verre — ils peuvent voler librement dans toute la zone jusqu'à la `WorldBorder`.

### ✨ Niveau d'XP remis à zéro

À chaque lancement de partie, le niveau et la barre d'expérience de chaque joueur sont réinitialisés à 0 (`Player#setLevel/setExp/setTotalExperience`) — pas de carry-over d'XP d'une partie à l'autre, pas de bonus involontaire (enchantements Fortune, etc.) au démarrage de la manche.

---

## 🆕 Récapitulatif des versions précédentes

### 1.5.0 — Mur de bordure en particules, délai de fin configurable & stats paramétrables

La 1.5.0 est une mise à jour de **confort visuel et de fiabilité de fin de partie** : la bordure est désormais *toujours* visible, et tout le monde a un instant pour savourer la victoire avant de revenir au hub.

- 🧱 **Mur de bordure en particules** (`DUST` rouges) — corrige le bug Paper où le rendu client du mur n'était pas envoyé. *(Remplacé en 1.11.0 par un repeint vanilla plus fiable, voir plus haut.)*
- ⏱️ **Délai de fin avant retour au hub** (`game.end-delay-seconds`, 10 par défaut) — le vainqueur reste sur zone, les autres en spectateurs, puis tout le monde rentre au hub.
- 🗂️ **Fichier de stats configurable** (`stats-file`) — chemin du fichier `stats.yml` désormais exposé dans `config.yml`.

### 1.4.0 — Tableau des scores persistant & PNJ trophée

La 1.4.0 transforme chaque victoire en **progression sauvegardée** et donne enfin au hub un but à explorer :

- 🏆 **Tableau des scores persistant** : `/hg top` (alias `/hg scores`) ouvre un inventaire 27-slots en forme de **trophée** listant les meilleurs joueurs du serveur, du plus grand nombre de victoires au plus petit.
- 🥇 **Têtes de joueurs avec médailles** : chaque entrée est la vraie tête Minecraft du joueur, avec son pseudo actuel et son nombre de victoires. Les trois premiers reçoivent une médaille (or / argent / bronze).
- 💾 **Persistance dans `stats.yml`** : les victoires sont enregistrées dans `plugins/HungerGames/stats.yml` et survivent aux redémarrages du serveur — les pseudos sont mis à jour à chaque victoire pour suivre un joueur même s'il a changé de nom.
- 🧑‍🌾 **PNJ trophée au hub** : `/hgadmin hub build` place désormais, en plus des PNJ d'arène, un villageois sur un piédestal en or (du côté opposé aux PNJ d'arène) qui ouvre le classement au clic droit.
- 🔁 **Refactor `Kit`** : les kits sont maintenant gérés via une vraie classe `Kit` dédiée — code plus simple et plus extensible.

### 1.3.0 — Bordure en deux phases avec suivi au tableau de bord & kill-feed

La 1.3.0 peaufine l'expérience de jeu en pleine partie et la lecture de la fin de match :

- 📉 **Bordure en deux phases** : après la période de grâce, la bordure se contracte en trois temps — réduction vers un premier cercle → pause stabilisée → réduction finale jusqu'à un petit cercle central. Chaque étape (durée restante + phase) est affichée en direct dans le tableau de bord à droite pendant tout le PVP.
- 💬 **Kill-feed dans le chat** : à chaque mort, tous les participants et spectateurs reçoivent « X a éliminé Y ! » pour les morts causées par un autre joueur (analyse du dernier dégât reçu) ou « Y est mort. » pour les morts environnementales.

### 1.2.0 — Lobby central procédural, cycle auto des arènes, spectateur & confort

La 1.2.0 a transformé le plugin d'un simple « système de zones » en une expérience complète clé-en-main :

- 🏛️ **Lobby central procédural** (`/hgadmin hub build`) : plaza circulaire en quartz, collines herbeuses qui montent en montagnes enneigées, arbres, fleurs, campement, entrée de mine creusée avec minerais apparents, et un mur de barrières invisibles doublé de la `WorldBorder` du hub.
- 🧑‍🌾 **PNJ « Rejoindre une arène »** : 1 à 7 villageois (configurable) sur pupitres en bloc d'émeraude face au spawn, qui ouvrent le GUI d'arènes au clic droit.
- ♻️ **Cycle automatique des arènes** : à chaque fin de partie, la zone jouée est régénérée en arrière-plan puis relâchée dans le pool, et l'arène tire immédiatement une nouvelle cellule.
- 💾 **Persistance des arènes** : `plugins/HungerGames/arenas.yml` sauvegarde la cellule de chaque arène — toutes les arènes sont recréées automatiquement à leur cellule au redémarrage du plugin.
- 🎒 **Items de salle d'attente** : une épée en pierre (slot 1, ouvre `/hg kit`) et un bloc barrière (slot 5, exécute `/hg leave`), protégés contre le drop et le déplacement.
- 👁️ **Mode spectateur amélioré** : boussole protégée, spectateur automatique à la mort, bordure de la zone appliquée, bouton central « Rejoindre une partie aléatoire » dans le GUI.
- 🪧 **Cage de lobby dynamique** : la plateforme flottante est entourée d'une cage de barrières pendant l'attente, retirée au lancement de la partie.
- 🎯 **Scatter intelligent** : les points de spawn en pleine eau/glace/lave sont rejetés — fini les apparitions en plein océan.
- 👤 **Pseudos masqués pendant la partie** via une équipe de scoreboard dédiée, puis restaurés à la fin.
- 📊 **GUI d'arènes en temps réel** : le nombre de joueurs et l'état des zones se mettent à jour en direct toutes les secondes.
- 🛡️ **Conforts divers** : inventaire vidé au hub, faim toujours pleine hors partie, suggestion de kit cliquable, `WorldBorder` avec `damageBuffer = 0` (1 ❤️/s sans marge).

### 1.1.0 — GUI d'arènes dynamique & mode spectateur

- 👁️ **Mode spectateur** complet avec bordure de zone personnelle.
- 🗂️ **GUI d'arènes dynamique** paginée (alias `/hg gui`), clic pour rejoindre ou regarder.
- 🧭 **Boussole de sortie** pour quitter le spectateur instantanément.
- 🔄 **Éliminés → spectateurs** automatique dans leur propre arène.
- 🎮 Vraie map générée, kits configurables, bordure rétrécissante.

### 1.0.0 — Première publication

- 🎯 Zones jamais réutilisées (recherche en spirale, persistance dans `zones.yml`).
- 🎮 Vraie map générée, lobby flottant, préchargement asynchrone.
- 🗺️ `WorldBorder` personnelle par participant.
- 🛡️ Période de grâce sans PVP, bordure rétrécissante, kits configurables.

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
- **`game.end-delay-seconds`** : délai entre la dernière mort et le retour automatique au hub (10 par défaut).
- **`game.border.shrink.phase1.*`** : première réduction de la bordure après la période de grâce (diamètre et durée).
- **`game.border.shrink.pause-seconds`** : durée de la pause stabilisée entre les deux phases.
- **`game.border.shrink.phase2.*`** : réduction finale jusqu'au centre de la zone (diamètre et durée).
- **`stats-file`** : chemin du fichier de statistiques (tableau des scores) dans `plugins/HungerGames/` (défaut `stats.yml`).

---

## 🔨 Compilation

```bash
mvn clean package
```

Le jar est généré dans `target/hungergames-1.11.0.jar`.

> **Note API Paper 1.21.1** : la régénération de chunks (`World#regenerateChunk`) est une API **spécifique à Paper** — elle lève une exception sur Spigot/CraftBukkit vanilla. C'est pour cela que le plugin dépend de `paper-api` et pas de `bukkit-api`.

---

## 📜 Limitations connues & pistes d'amélioration

- La régénération vanilla recalcule le terrain à partir du générateur du monde : toute construction permanente dans une cellule du pool sera perdue à la première régénération. Les zones du pool doivent rester de la nature « brute ».
- La taille d'une zone (`zone.size`) est globale à tout le pool ; impossible d'avoir des arènes de tailles différentes sans changer la config pour tout le monde.
- Le « mid » de la map n'est pas matérialisé par une structure — c'est simplement le centre géométrique de la zone (le lobby flottant est juste au-dessus).
- Pas de système d'alliance in-game : comme demandé, ça reste au niveau des messages privés entre joueurs, en dehors du plugin.
- Le repeint vanilla de la bordure (1.11.0) et les dégâts manuels (1.6.0) ne remplacent pas la `WorldBorder` : ils la **complètent**. Les déplacements, la collision et la zone jouable restent gérés par la `WorldBorder` elle-même. La 1.11.0 a remplacé le rendu custom du mur de particules (1.5.0) par ce repeint vanilla, plus simple et plus fiable.
- La complétion `TAB` propose les noms de zones/kits déjà existants et les sous-commandes, mais ne valide pas qu'un nouveau nom n'est pas déjà pris.

---

## 📝 Licence & crédits

Plugin écrit pour la communauté francophone HungerGames. Développé par **zzaee**.

Suggestions, bugs, pull requests : ouvrez une [issue](../../issues) sur le dépôt.