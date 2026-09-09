# HungerGames (Paper 1.21)

Plugin de battle royale "à l'ancienne" : les joueurs apparaissent dispersés
sur une vraie map générée (pas de coffres ni de scénario façon jeu de plateau).
Chaque arène nommée, créée par un admin, **tourne en continu** dans son propre
monde Bukkit dédié : elle reste ouverte en permanence (sauf pendant qu'une
partie est lancée), et à chaque fin de partie elle supprime ce monde et en
crée un nouveau, ce qui permet de faire tourner plusieurs arènes en parallèle
sans jamais qu'elles se marchent dessus — et surtout, permet à chaque arène
d'avoir sa **vraie** bordure de monde vanilla (voir plus bas).

## Téléchargement

Le jar prêt à l'emploi est dans la [release v1.7.0](../../releases/tag/v1.7.0). Tu peux aussi le compiler toi-même :

```bash
mvn clean package
```

…ce qui produit `target/hungergames-1.7.0.jar` (voir plus bas pour le détail de la compilation).

## Compilation (détails)

```bash
mvn clean package
```

Produit `target/hungergames-1.7.0.jar`. Copie-le dans `plugins/` de ton
serveur Paper 1.21.x. Le plugin dépend de `paper-api` (Bukkit seul ne suffit
pas : `Bukkit#createWorld(WorldCreator)` et la `WorldBorder` réelle du monde
sont utilisées en natif).

⚠️ Paper API cible : `1.21.1-R0.1-SNAPSHOT` (POM). Sur des builds plus
récentes de Paper 1.21+, l'API peut utiliser une surcharge
`world.getWorldBorder().setSize(double, java.time.Duration)` ; le code utilise
ici la surcharge `(double, long milliseconds)` qui est universelle. Si tu
dois cibler une build très récente, tu peux passer cette ligne à `setSize(d,
Duration.ofSeconds(seconds))` si ta version de `paper-api` l'expose.

## Comment ça marche

### Le hub

Une zone bornée entre **X/Z = -100 et 100** (rayon configurable via
`hub.radius`), dans le monde partagé configuré (`world` dans `config.yml`),
où les joueurs attendent hors partie. Une bordure de monde personnelle y
confine automatiquement chaque joueur envoyé au hub (`ArenaManager#sendToHub`).
Construis ton spawn/lobby central à l'intérieur de ces limites.

### Un monde Bukkit dédié par arène

Contrairement à une simple région découpée dans un grand monde partagé,
**chaque arène vit dans son propre monde Bukkit** (voir `WorldAllocator`),
créé dynamiquement sur le disque du serveur (dossier `hg_arena_<id>/`,
préfixe configurable via `arena-worlds.arena-name-prefix`). C'est ce qui
permet d'utiliser la **vraie** bordure de monde vanilla
(`World#getWorldBorder()`) pour chaque arène — animée et infligeant ses
dégâts nativement, de façon fiable — plutôt qu'une bordure "virtuelle"
par-joueur (l'approche précédente, qui souffrait d'un bug Paper non résolu
empêchant un rendu et une animation fiables, voir plus bas).

### Cycle de vie d'une arène (persistante)

1. `/hgadmin zone create <nom>` : un nouveau monde vanilla est créé (génération
   normale, pas plat/vide) avec sa propre bordure configurée dessus, une
   plateforme de verre est construite au centre (lobby flottant de cette
   zone), et le reste de la zone se précharge en arrière-plan.
2. L'arène reste **ouverte en continu** sous ce nom : `/hg join <nom>` ou le
   GUI (`/hg gui`) permettent de la rejoindre à tout moment tant qu'une
   partie n'y est pas en cours.
3. Dès que le nombre minimum de joueurs est atteint et que la zone est
   prête, un compte à rebours démarre (ou `/hgadmin zone forcestart <nom>`
   force le départ).
4. Pendant la partie : scatter aléatoire, période de grâce sans PVP, PVP,
   bordure qui se referme progressivement — comme avant.
5. **Fin de partie** : au lieu de disparaître, l'arène se réinitialise, crée
   un **tout nouveau monde** dédié, y reconstruit son lobby et relance le
   préchargement — elle redevient jouable dès que possible sous le même nom.
   L'ancien monde, lui, est déchargé puis **supprimé du disque** en
   arrière-plan une fois tout le monde parti (`WorldAllocator#deleteArenaWorld`),
   pour repartir sur un terrain garanti vierge à chaque manche.

Un admin peut aussi supprimer une arène à tout moment avec
`/hgadmin zone delete <nom>` : la partie en cours (s'il y en a une) est
annulée, tout le monde renvoyé au hub, le monde dédié supprimé, et le nom se
libère immédiatement — cette fois l'arène ne redémarre pas.

### Bordure par arène — la vraie bordure vanilla

Chaque monde d'arène a sa **propre bordure de monde réelle**
(`World#getWorldBorder()`), configurée à sa création par `WorldAllocator` :
- centrée sur (0,0), taille = `zone.size` (1000 par défaut) ;
- `damageBuffer = 0` et `damageAmount = 2.0`, soit environ 1 cœur par
  seconde dès qu'on est hors des limites, sans la marge de quelques blocs
  que Minecraft laisse par défaut avant de faire mal ;
- animée nativement et fidèlement par le serveur lors des phases de
  réduction (voir `Arena#startBorderShrink`), puisqu'il s'agit d'une vraie
  bordure de monde et non d'une émulation.

Comme chaque arène a son propre monde, sa bordure ne concerne que les
joueurs qui s'y trouvent — plusieurs arènes tournent donc en parallèle sans
jamais interférer entre elles, sans avoir besoin de gérer des bordures
virtuelles par-joueur ni de particules pour simuler un mur.

**Ancienne approche (abandonnée)** : les versions précédentes utilisaient une
bordure virtuelle par-joueur (`Bukkit.createWorldBorder()` +
`Player#setWorldBorder`), seule façon d'avoir plusieurs bordures différentes
sur un même monde partagé. Mais Paper a un bug connu et non résolu sur cette
fonctionnalité ([PaperMC/Paper#12372](https://github.com/PaperMC/Paper/issues/12372),
[#7748](https://github.com/PaperMC/Paper/issues/7748)) : le paquet client qui
affiche le mur n'est pas toujours envoyé, et l'animation de réduction ne
progresse pas forcément côté serveur (pas de tick de monde pour la faire
avancer sur une bordure "virtuelle"). Le plugin avait fini par compenser ça
avec des dégâts et un mur de particules gérés à la main. Le passage à un
monde dédié par arène rend tout ce contournement inutile : c'est maintenant
la vraie mécanique vanilla qui s'occupe de tout, de façon fiable.

## Commandes joueur

- `/hg join [nom]` — rejoint une zone précise par son nom, ou n'importe
  quelle zone ouverte si aucun nom n'est donné
- `/hg leave` — quitte la partie en cours, **ou** le mode spectateur si tu observais une partie
- `/hg kit`
- `/hg arenas` (alias `/hg gui`) — ouvre le GUI listant toutes les zones actives
- `/hg unspectate`

## Système de spectateur et GUI d'arènes (inspiré de HikaBrain)

`/hg arenas` ouvre un inventaire (54 slots, paginé au-delà de 45 zones) qui
liste **toutes les zones créées**, avec leur nom :

- **Vert/jaune** (chargement ou en attente) : clique pour rejoindre
  directement cette zone précise.
- **Rouge** (période de grâce ou PVP en cours) : clique pour la regarder en
  **mode spectateur** (`GameMode.SPECTATOR`, téléporté sur la plateforme du
  lobby de cette zone, avec la bordure de la zone appliquée).
- Un bouton central **"Rejoindre une partie aléatoire"** place le joueur
  automatiquement dans n'importe quelle zone actuellement ouverte.

Un spectateur reçoit une boussole (slot 8, comme le `SPECTATOR_LEAVE_SLOT`
d'HikaBrain) : clic pour repartir instantanément (`/hg unspectate`), sans
pouvoir la lâcher ni la déplacer dans son inventaire
(`com.herocraft.hungergames.listener.SpectatorListener`).

Les joueurs éliminés en cours de partie basculent eux aussi automatiquement
en spectateur de leur propre zone (déjà géré dans `CombatListener` /
`Arena#onPlayerDeath`), avec la même bordure de zone.

## Commandes admin (`hungergames.admin`, op par défaut)

### Gestion des zones

- `/hgadmin zone create <nom>` — crée une nouvelle arène persistante (nouveau
  monde Bukkit dédié avec sa bordure configurée, construit le lobby flottant,
  lance le préchargement)
- `/hgadmin zone delete <nom>` — supprime définitivement une arène (annule la
  partie en cours s'il y en a une, supprime son monde dédié, libère le nom)
- `/hgadmin zone rename <ancien nom> <nouveau nom>`
- `/hgadmin zone list` — liste toutes les arènes avec leur état
- `/hgadmin zone info <nom>` — détails d'une arène (monde dédié, joueurs, spectateurs...)
- `/hgadmin zone tp <nom>` — téléporte au lobby flottant de la zone actuelle de l'arène
- `/hgadmin zone forcestart <nom>` — force le lancement même sous le seuil minimum de joueurs

### Autres

- `/hgadmin reload` — recharge `config.yml` et `kits.yml`
- `/hgadmin list` — nombre d'arènes actuellement actives
- `/hgadmin hub build` — (re)construit procéduralement le lobby central (voir ci-dessous)
- `/hgadmin hub delete` — supprime les PNJ et régénère le terrain naturel du lobby
- `/hgadmin kit create <id> <nom affiché>`
- `/hgadmin kit delete <id>`
- `/hgadmin kit additem <id>` — ajoute l'item en main au contenu du kit
- `/hgadmin kit seticon <id>` — définit l'item en main comme icône du kit dans le menu
- `/hgadmin kit list`

## Le lobby central (`/hgadmin hub build`)

Construit procéduralement, autour du point `hub.x/y/z`, un lobby complet :

- **Spawn** : plaza circulaire (quartz/andésite polie) au centre, exactement
  au point de spawn/retour configuré — c'est là que les joueurs apparaissent
  en rejoignant le monde ou en revenant d'une partie.
- **PNJ "rejoindre une arène"** : 3 ou 5 (`hub.build.npc-count`) pupitres
  (bloc d'émeraude + lanternes) alignés symétriquement en face du spawn, à
  `hub.build.npc-distance` blocs, reliés par une allée. Chaque pupitre porte
  un villageois figé (sans IA, invulnérable) : clic droit dessus ouvre le
  même GUI que `/hg gui`.
- **Collines et montagnes** : le terrain autour de la plaza monte
  progressivement en collines herbeuses puis en montagnes (andésite, neige
  aux sommets) jusqu'au bord de la zone construite, avec un bruit procédural
  (fonctions mathématiques, pas de vraie génération Minecraft) pour un rendu
  naturel et pas répétitif.
- **Références à la survie** : arbres (chêne/épicéa) dispersés dans les
  collines, fleurs/herbes hautes, un petit campement (feu de camp, table de
  craft, four, coffres, bottes de foin), et une entrée de mine creusée dans
  un flanc de colline (torches, minerais de fer/charbon apparents, cadre en
  bois).
- **Barrière physique** : un mur de blocs `BARRIER` invisible est construit
  tout autour de la zone (dans l'épaisseur des montagnes, donc caché), en
  plus de la `WorldBorder` par joueur déjà posée par `ArenaManager#sendToHub`
  — double sécurité pour qu'un joueur ne sorte jamais du lobby.

⚠️ La construction touche potentiellement des dizaines de milliers de blocs
(rayon 40 par défaut) : elle est étalée sur plusieurs secondes (~15-30s) via
une tâche répétitive pour limiter l'impact sur le TPS, mais attends-toi à
quelques à-coups. Lance-la de préférence hors des heures de forte affluence,
et seulement une fois `world` configuré sur le bon monde. La relancer efface
et reconstruit entièrement la zone (y compris les PNJ, qui sont d'abord
supprimés proprement) — c'est le moyen le plus simple de changer le nombre
de PNJ (`hub.build.npc-count`, 1 par défaut) : modifie la config puis relance
`/hgadmin hub build`. Si tu veux repartir d'un terrain totalement naturel
avant de reconstruire, utilise `/hgadmin hub delete` en premier (ça régénère
le terrain vanilla, sans reconstruire ensuite).

## Config (`config.yml`)

Voir les commentaires dans le fichier, notamment :
- `world` : le monde partagé où vit le **hub** uniquement (les arènes, elles,
  ont chacune leur propre monde dédié, voir `arena-worlds`).
- `hub.x/y/z` : point de spawn dans le hub (doit rester dans les bornes ci-dessous).
- `hub.radius` : rayon (en blocs) de la zone hub bornée autour de (0,0), 100 par défaut
  (donc -100 à 100 en X et Z).
- `hub.build.*` : rayon construit, nombre de PNJ (1 à 7) et leur distance au
  spawn pour `/hgadmin hub build` (voir section dédiée ci-dessus).
- `arena-worlds.arena-name-prefix` : préfixe du nom de dossier des mondes
  d'arène générés dynamiquement (`hg_arena_` par défaut).
- `zone.size` : diamètre d'une zone/arène (1000 = rayon 500). Toutes les
  arènes créées partagent cette même taille.
- `zone.chunks-per-tick` : vitesse du préchargement d'un nouveau monde d'arène
  (et de la zone construite du hub).
- `zone.regen-chunks-per-tick` : vitesse de régénération de la zone du hub
  uniquement (`/hgadmin hub delete`) — les arènes, elles, suppriment tout
  leur monde d'un coup en fin de partie, pas besoin de régénérer chunk par
  chunk.
- `game.min-players` / `game.max-players` (jusqu'à 100).
- `game.grace-period-seconds` : durée sans PVP après le scatter.
- `game.border.shrink.*` : rétrécissement de la bordure en deux phases après
  la période de grâce — `phase1` (comme avant), puis `pause-seconds` sans
  rétrécir, puis `phase2` jusqu'à un petit cercle final au centre (10 blocs
  de diamètre par défaut). La phase et le temps restant s'affichent dans le
  tableau de bord pendant le PVP.

## Persistance des zones entre redémarrages

Chaque arène nommée sauvegarde le nom de son monde dédié actuel dans
`plugins/HungerGames/arenas.yml`, mis à jour automatiquement à la création,
au renommage, à la suppression, et **à chaque cycle vers un nouveau monde**
en fin de partie. Au démarrage du plugin, toutes les arènes sauvegardées sont
automatiquement rechargées sur le monde qu'elles occupaient (le dossier de
monde est rechargé tel quel s'il existe encore, le lobby flottant reconstruit,
le préchargement relancé) — plus besoin de refaire `/hgadmin zone create`
après chaque redémarrage. Si le dossier sauvegardé est introuvable (cas rare,
ex. suppression manuelle), un nouveau monde vide est créé à la place avec un
avertissement dans les logs.

## Complétion par tabulation

`/hg` et `/hgadmin` (et leurs sous-commandes) proposent une complétion
contextuelle : sous-commandes disponibles, noms de zones existantes pour
`/hg join`, `/hgadmin zone delete|rename|info|tp|forcestart`, et identifiants
de kits pour `/hgadmin kit delete|additem|seticon`.

## Confort hors partie (hub, attente, spectateurs)

- **Inventaire toujours vide** : à chaque envoi au hub (connexion, fin de
  partie, `/hg leave`, `/hg unspectate`, respawn) ainsi qu'à chaque
  rejointe d'une arène (`Arena#addPlayer`), l'inventaire du joueur est vidé.
- **Faim toujours pleine hors partie active** : un `FoodLevelChangeEvent`
  global annule toute perte de faim et remet la barre à fond tant que le
  joueur n'est pas dans une partie en `GRACE_PERIOD` ou `PVP` — ça couvre le
  hub, les salles d'attente d'arène (`PRELOADING`/`WAITING`/`STARTING`), et
  le mode spectateur. La faim ne redevient "normale" (et donc utile pour le
  farm) qu'une fois la partie réellement lancée.
- **Cage dans la salle d'attente** : la plateforme flottante de chaque zone
  est maintenant entourée d'une cage de blocs barrière (sol invisible entre
  le bord du verre et le mur, + mur de plusieurs blocs de haut) pour qu'on ne
  puisse ni en tomber ni s'en éloigner en marchant pendant l'attente. Cette
  cage est **automatiquement retirée au lancement de la partie**
  (`Arena#removeLobbyCage`, appelé en tout début de `beginGame()`), pour que
  les spectateurs qui viennent ensuite observer le match puissent voler
  librement dans toute la zone (jusqu'à sa `WorldBorder`) sans être coincés
  sur la petite plateforme. Réglages : `lobby.cage-margin` et
  `lobby.cage-height`.
- **Suggestion de kit cliquable** : dès qu'un joueur rejoint une zone (si
  elle est déjà prête) ou dès qu'une zone en cours de préchargement devient
  prête, un message cliquable "choisis ton kit avec `/hg kit`" est envoyé —
  cliquer dessus exécute directement la commande.
- **Pas de spawn en pleine mer** : le scatter aléatoire (`RandomLocationUtil`)
  rejette maintenant les points candidats situés sur de l'eau, de la glace
  flottante ou de la lave avant de les retenir (deux passes de tentatives,
  avec repli progressif de la distance minimale entre joueurs si besoin) —
  les joueurs n'apparaissent plus en plein océan au lancement d'une partie.
  Cas extrême (zone quasi entièrement recouverte d'eau) : en dernier
  recours, un point est quand même choisi pour garantir que tout le monde
  apparaît bien quelque part.
- **Impossible de rejoindre pendant le chargement** : `Arena#isJoinable()` ne
  renvoie plus vrai que pour `WAITING`/`STARTING` (plus `PRELOADING`) — une
  zone en cours de préchargement apparaît en jaune "Chargement..." dans le
  GUI et n'est ni cliquable pour la rejoindre, ni proposée par
  `/hg join`/le bouton aléatoire tant qu'elle n'est pas prête.
- **GUI des arènes en temps réel** : une tâche répétitive
  (`ArenaGUI#refreshOpenViewers`, toutes les secondes) réécrit le contenu de
  l'inventaire de chaque joueur qui a le GUI ouvert (sans le fermer/rouvrir)
  — nombre de joueurs, état, nombre total de zones se mettent à jour en direct.
- **Items de la salle d'attente** : en plus de la boussole des spectateurs,
  chaque joueur en attente dans une arène reçoit une épée en pierre (slot 1,
  clique pour ouvrir `/hg kit`) et un bloc barrière en 5ᵉ case de la hotbar
  (slot 5, clique pour `/hg leave`), tous deux protégés contre le drop et le
  déplacement dans l'inventaire (`WaitingRoomListener`).
- **Pseudos masqués pendant la partie** : dès qu'une arène passe en
  `GRACE_PERIOD` ou `PVP`, le pseudo au-dessus de la tête de chaque
  participant est masqué pour tous ceux qui regardent cette arène (joueurs
  et spectateurs), via une équipe de scoreboard dédiée
  (`Team.Option.NAME_TAG_VISIBILITY` à `NEVER`) recréée à chaque
  rafraîchissement du tableau de bord. Redevient normal une fois la partie
  terminée (nouvelle zone, nouveau tableau de bord).
- **Dégâts et réduction de bordure** : chaque monde d'arène a sa propre
  **vraie** bordure vanilla (`World#getWorldBorder()`, configurée par
  `WorldAllocator` — `damageBuffer = 0`, `damageAmount = 2.0`, soit environ
  1 cœur par seconde hors des limites). C'est le serveur qui gère nativement
  l'animation et les dégâts, de façon fiable — plus aucun bricolage de notre
  côté (dégâts manuels, mur de particules) n'est nécessaire, contrairement à
  l'ancienne approche par bordure virtuelle par-joueur (voir la section
  "Bordure par arène" plus haut pour le détail du problème que ça réglait).
- **Kill-feed dans le chat** : à chaque mort, un message est envoyé à tous
  les participants de l'arène (joueurs + spectateurs) — "X a éliminé Y !" si
  le dernier dégât reçu venait d'un autre joueur, sinon "Y est mort." (mort
  hors combat direct : chute, faim, bordure...). Ce message part dans tous
  les cas, pas seulement sur un kill entre joueurs.
- **Bordure en deux phases avec suivi dans le tableau de bord** : après la
  période de grâce, une première réduction de bordure se lance (comme
  avant) ; une fois terminée, la zone reste stable pendant
  `game.border.shrink.pause-seconds` (5 minutes par défaut), puis une
  réduction finale ramène la bordure à un petit cercle central
  (`phase2.target-diameter`, 10 blocs par défaut). La phase en cours et le
  temps restant s'affichent en direct dans le tableau de bord à droite
  pendant tout le PVP (`Arena#refreshScoreboard`). Le tableau de bord affiche
  aussi, dès le début de la partie (période de grâce comprise), la **taille
  actuelle de la zone** (`World#getWorldBorder().getSize()`, donc toujours à
  jour même en pleine animation) et la **distance du joueur au centre de
  l'arène** — propre à chaque joueur, calculée depuis sa position réelle à
  chaque rafraîchissement.
- **Fin de partie différée (~10s)** : dès qu'il ne reste plus qu'un survivant
  (ou zéro), la partie est décidée immédiatement (plus de dégâts/PVP), mais
  le retour au hub est différé de `game.end-delay-seconds` (10s par défaut) :
  le vainqueur reste en `SURVIVAL` sur place, tout le monde (joueurs déjà
  spectateurs, et les rares survivants non-vainqueurs dans un cas limite) est
  mis/laissé en `SPECTATOR`. Une fois le délai écoulé, tout le monde
  (participants + spectateurs externes) est automatiquement téléporté au hub
  et l'arène cycle vers sa prochaine zone.
- **Niveau d'XP remis à zéro** : au lancement de chaque partie, le niveau et
  la barre d'expérience de chaque joueur sont réinitialisés à 0
  (`Player#setLevel/setExp/setTotalExperience`), pour repartir sur une base
  propre à chaque manche.
- **Lobby d'attente retiré au lancement** : en plus de la cage (déjà retirée
  auparavant), la plateforme de verre elle-même et sa lanterne marine sont
  maintenant effacées dès le tout début de `beginGame()`. Les spectateurs qui
  viendront ensuite regarder le match (mode `SPECTATOR`, sans gravité) n'ont
  pas besoin de sol pour tenir en l'air.
- **Tableau des scores (`/hg top`)** : chaque victoire est comptabilisée par
  joueur et persistée dans `plugins/HungerGames/stats.yml`
  (`StatsManager`). `/hg top` (alias `/hg scores`) ouvre un classement en
  têtes de joueurs triées par nombre de victoires. Un PNJ dédié est aussi
  placé au hub (`/hgadmin hub build`), du côté opposé aux PNJ d'arène, pour y
  accéder directement en jeu.
- **Affichage des kits corrigé** : le sélecteur de kits utilisait
  `legacySection()` (codes `§`) pour interpréter des noms écrits en `&`
  (format `kits.yml`, ex. `&6Bucheron`), qui s'affichaient donc en texte brut
  avec le `&` littéral au lieu d'être colorés. Il utilise maintenant
  `legacyAmpersand()`, le bon parseur pour ce format. Chaque kit affiche
  aussi désormais son contenu dans sa description (liste des items et
  quantités) directement dans le menu `/hg kit`.
- **Joueurs morts = vrais spectateurs** : à la mort, un joueur devient
  invisible aux joueurs encore en vie de son arène (`Player#hidePlayer`,
  restauré à la fin de la manche via `Arena#restoreVisibility`) — en plus
  d'être déjà en `GameMode.SPECTATOR` (donc sans collision). Son chat est
  également isolé (`DeadChatListener`, basé sur `AsyncChatEvent`) : ses
  messages ne sont visibles que par les autres morts et spectateurs de la
  même arène, jamais par les joueurs encore en jeu. Il reçoit aussi une
  boussole ("Suivre un joueur en vie", slot 1) qui ouvre un GUI listant tous
  les joueurs encore en vie (têtes de joueurs) — cliquer dessus téléporte
  instantanément à ce joueur, avec vérification qu'il est toujours en vie
  au moment du clic.

## Limitations connues / pistes d'amélioration

- **Espace disque et temps de création** : chaque arène est un vrai monde
  Bukkit sur le disque (dossier complet avec fichiers de région). Créer une
  arène crée un monde (quelques secondes, hitch possible pendant la
  génération de la zone de spawn initiale) ; en fin de partie, l'ancien monde
  est supprimé automatiquement, mais prévois de la marge disque si tu fais
  tourner beaucoup d'arènes en simultané, et surveille le nombre de mondes
  chargés (`/hgadmin list`).
- La taille d'une arène (`zone.size`) est globale : impossible d'avoir des
  arènes de tailles différentes sans changer la config pour toutes les
  arènes futures.
- Le "mid" de la map n'est pas matérialisé par une structure : c'est
  simplement le centre géométrique de la zone (le lobby flottant est juste
  au-dessus). Je peux ajouter un marqueur (beacon, colonne) si tu veux qu'il
  soit visible depuis le sol.
- Pas de système d'alliance in-game : comme demandé, ça reste au niveau des
  MP entre joueurs, en dehors du plugin.
- La complétion par tabulation propose les noms de zones/kits déjà existants
  et les sous-commandes, mais ne valide rien d'autre (elle ne vérifie pas par
  exemple qu'un nouveau nom de zone n'est pas déjà pris).
