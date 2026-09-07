<div align="center">

# 🏆 HungerGames

### Plugin battle royale *à l'ancienne* pour serveurs **Paper 1.21**

*Spawns dispersés sur une vraie map générée · arènes persistantes qui tournent en boucle · bordures de zone individuelles par joueur · bordure en deux phases avec kill-feed · mur de bordure en particules · mode spectateur complet · GUI d'arènes dynamique · tableau des scores persistant · lobby central procédural · kits configurables.*

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
| 🧱 **Mur de bordure en particules** | Un rideau de particules rouges entoure la zone jouable à hauteur des yeux — visible par tous, même quand le rendu natif de Paper n'envoie pas le paquet au client. |
| 📉 **Bordure en deux phases** | Une première réduction, une pause stabilisée, puis une réduction finale jusqu'au centre, **avec la phase et le temps restant affichés en direct dans le tableau de bord**. |
| 💬 **Kill-feed en jeu** | À chaque mort, un message est envoyé à tous les participants et spectateurs de l'arène : « X a éliminé Y ! » ou « Y est mort. ». |
| 👁️ **Mode spectateur complet** | Suivez n'importe quelle partie en `SPECTATOR`, téléporté sur le lobby de la zone avec la bordure appliquée. |
| 🗂️ **GUI d'arènes dynamique** | Inventaire 54-slots, paginé, qui liste toutes les zones avec couleur selon l'état (vert = rejoignable, jaune = chargement, rouge = en cours). Mise à jour en temps réel. |
| 🏛️ **Lobby central procédural** | `/hgadmin hub build` construit un plaza, des collines, des montagnes, un campement, une entrée de mine, des **PNJ villageois** qui ouvrent le GUI d'arènes, et un **PNJ dédié au tableau des scores**. |
| 🏆 **Tableau des scores persistant** | Chaque victoire est enregistrée dans `stats.yml` ; ouvrez le classement à tout moment avec `/hg top` (alias `/hg scores`) ou clic droit sur le PNJ trophée du hub — classement en têtes de joueurs, médailles 🥇🥈🥉, persistant à travers les redémarrages. |
| 🎒 **Kits configurables** | 4 kits par défaut (Guerrier, Bucheron, Mineur, Archer), entièrement éditables via `/hgadmin kit ...`. |
| 📦 **Confort** | Délai de fin configurable avant le retour automatique au hub, inventaire vidé à chaque retour au hub, faim toujours pleine hors partie, pseudos masqués pendant la partie, scatter intelligent (pas de spawn en pleine mer), suggestions cliquables. |
| ⌨️ **Complétion `TAB`** | `/hg` et `/hgadmin` proposent contextuellement les sous-commandes, noms de zones et identifiants de kits. |

---

## 🚀 Installation

1. Téléchargez `hungergames-1.5.0.jar` depuis la [release v1.5.0](../../releases/latest).
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
4. **Partie** — Scatter aléatoire → période de grâce sans PVP → PVP → bordure en deux phases (réduction → pause → réduction finale), avec un **mur de particules rouges** visible à chaque bord.
5. **Fin & cycle** — À la dernière mort, tout le monde reste `end-delay-seconds` secondes (10 par défaut) avant le retour automatique au hub ; le vainqueur reste sur la zone, les autres sont spectateurs. L'arène tire ensuite immédiatement une **nouvelle** cellule (forcément différente), reconstruit son lobby, et redevient jouable. L'ancienne zone est régénérée en arrière-plan puis relâchée dans le pool.

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

## 🆕 Nouveautés de la version 1.5.0

La 1.5.0 est une mise à jour de **confort visuel et de fiabilité de fin de partie** : la bordure est désormais *toujours* visible, et tout le monde a un instant pour savourer la victoire avant de revenir au hub.

### 🧱 Mur de bordure en particules (correction d'un bug Paper)

Paper applique par joueur une `WorldBorder` individuelle à chaque participant — c'est ce qui permet à plusieurs arènes de tourner simultanément sans que leurs bordures ne se chevauchent. Mais le rendu client du mur (le « rideau » bleu que Minecraft affiche nativement) **dépend d'un paquet réseau que Paper n'envoie pas toujours** dans cette configuration : bugs ouverts côté PaperMC ([#12372](https://github.com/PaperMC/Paper/issues/12372), [#7748](https://github.com/PaperMC/Paper/issues/7748)). Résultat : la `WorldBorder` limitait bien les déplacements et infligeait les dégâts attendus, mais le mur était souvent **invisible**, et les joueurs dépassaient sans le voir.

La 1.5.0 ne se fie plus qu'à son propre rendu :

- 🎨 **Rideau de particules `DUST` rouges** (RVB 255/50/50, taille 1.3) dessiné toutes les 10 ticks (2×/seconde) le long des 4 bords de la `WorldBorder` du joueur.
- 👀 **Visible uniquement quand on s'approche** : le mur n'est dessiné que dans un rayon de 32 blocs autour du joueur sur l'axe X ou Z, ce qui évite de saturer les clients éloignés du bord.
- 🪜 **Hauteur centrée sur le joueur** : le rideau monte de 4 blocs sous ses pieds et monte jusqu'à 6 blocs au-dessus, pour rester lisible dans les reliefs.
- ♻️ **Annulé proprement** entre les parties (au reset de l'arène, et au redémarrage du plugin via la chaîne d'annulation existante).

Plus de joueurs qui se font éliminer « sans avoir vu la bordure » : le mur est désormais *toujours* là, peu importe la version exacte de Paper.

### ⏱️ Délai de fin avant retour au hub (`end-delay-seconds`)

Avant, dès qu'il ne restait plus qu'un survivant, le plugin considérait la partie comme terminée et renvoyait tout le monde au hub quasi instantanément. La 1.5.0 introduit une **fenêtre de victoire configurable** :

- ⏳ Nouveau paramètre **`game.end-delay-seconds`** (10 par défaut) dans `config.yml`.
- 🏆 À la dernière mort, le vainqueur reste sur la zone le temps de ce délai ; les autres sont (ou restent) spectateurs de l'arène.
- 🏛️ À la fin du délai, retour automatique de tout le monde au hub, déclenchement de la régénération de la zone, et tirage d'une nouvelle cellule comme avant.

C'est le moment idéal pour applaudir le vainqueur, prendre un screenshot, ou juste respirer après le rush final.

### 🗂️ Fichier de stats configurable (`stats-file`)

Le tableau des scores persistait déjà dans `plugins/HungerGames/stats.yml`. Le chemin est désormais exposé dans `config.yml` sous la clé **`stats-file`** (défaut `stats.yml`), pour les serveurs qui préfèrent mutualiser leurs données avec d'autres plugins ou ranger les stats ailleurs.

---

## 🆕 Récapitulatif des versions précédentes

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

Le jar est généré dans `target/hungergames-1.5.0.jar`.

> **Note API Paper 1.21.1** : la régénération de chunks (`World#regenerateChunk`) est une API **spécifique à Paper** — elle lève une exception sur Spigot/CraftBukkit vanilla. C'est pour cela que le plugin dépend de `paper-api` et pas de `bukkit-api`.

---

## 📜 Limitations connues & pistes d'amélioration

- La régénération vanilla recalcule le terrain à partir du générateur du monde : toute construction permanente dans une cellule du pool sera perdue à la première régénération. Les zones du pool doivent rester de la nature « brute ».
- La taille d'une zone (`zone.size`) est globale à tout le pool ; impossible d'avoir des arènes de tailles différentes sans changer la config pour tout le monde.
- Le « mid » de la map n'est pas matérialisé par une structure — c'est simplement le centre géométrique de la zone (le lobby flottant est juste au-dessus).
- Pas de système d'alliance in-game : comme demandé, ça reste au niveau des messages privés entre joueurs, en dehors du plugin.
- Le mur de bordure en particules de la 1.5.0 ne remplace pas la `WorldBorder` : il la **complète visuellement**. Les dégâts, la collision et la zone jouable restent gérés par la `WorldBorder` elle-même, comme avant.
- La complétion `TAB` propose les noms de zones/kits déjà existants et les sous-commandes, mais ne valide pas qu'un nouveau nom n'est pas déjà pris.

---

## 📝 Licence & crédits

Plugin écrit pour la communauté francophone HungerGames. Développé par **zzaee**.

Suggestions, bugs, pull requests : ouvrez une [issue](../../issues) sur le dépôt.
