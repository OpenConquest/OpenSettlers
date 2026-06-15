# 🏰 OpenSettlers

**OpenSettlers** est un clone open-source web de *The Settlers II: 10th
Anniversary*. Le projet recrée l'expérience classique : génération de carte
procédurale, chaînes de production, logistique routière, expansion territoriale
militaire, expéditions maritimes et adversaires contrôlés par l'IA — le tout sur
une architecture web temps réel à **serveur autoritaire**.

## 📑 Table des matières

1. [Architecture globale](#-architecture-globale)
2. [Stack technique](#-stack-technique)
3. [Mécaniques implémentées](#-mécaniques-implémentées)
4. [Structure du projet](#-structure-du-projet)
5. [Installation et lancement](#-installation-et-lancement)
6. [API réseau](#-api-réseau)
7. [Game loop & systèmes](#-game-loop--systèmes)
8. [Feuille de route](#-feuille-de-route)

---

## 🏗 Architecture globale

* **Serveur (Quarkus / Java 21)** — le cœur du jeu. Une *game loop* tourne à un
  tick-rate fixe (**10 TPS**), applique les commandes des joueurs, exécute une
  liste ordonnée de **systèmes** de simulation sur un unique `GameState`, puis
  diffuse à chaque tick un instantané JSON filtré par le brouillard de guerre.
  Toute mutation d'état se fait sur le thread de la boucle : la simulation est
  mono-thread et sans verrou, les WebSockets ne font qu'enfiler des commandes.
* **Client (Nuxt 3 + WebGL)** — gère le lobby, le HUD et le rendu de la carte. Il
  reçoit les *snapshots*, interpole le mouvement des unités pour un rendu fluide
  et transmet les intentions du joueur.

---

## 🛠 Stack technique

| Composant | Technologie | Rôle |
| --- | --- | --- |
| **Backend** | **Quarkus 3.36 (Java 21)** | Moteur serveur, injection de dépendances (ArC), scheduler. |
| **Persistance** | **PostgreSQL + Hibernate Panache** | Sauvegarde/chargement des parties (pattern Active Record). |
| **Temps réel** | **Quarkus WebSockets Next** | WebSocket réactif non bloquant pour la diffusion d'état. |
| **JSON** | **Jackson** | Sérialisation des messages et des snapshots. |
| **Frontend** | **Nuxt 3 (Vue.js) + Pinia** | UI réactive, HUD, état partagé client. |

---

## ⚙️ Mécaniques implémentées

Le backend est un moteur de jeu complet (`fr.opensettlers.*`) :

* **Carte hexagonale procédurale** — coordonnées *doubled-height*, génération par
  bruit de Perlin (élévation + humidité) et semis de ressources par échantillonnage
  Poisson-disk (`service.mapgen`).
* **Logistique** — drapeaux, routes, réseau et **pathfinding Dijkstra**
  (`state.RoadNetwork`), porteurs, ânes et **routes principales de niveau 2**.
* **Construction** — chantiers, livraison des matériaux, terrassement puis
  maçonnerie, mise en service et affectation des ouvriers spécialisés.
* **Chaînes de production complètes** — 27 types de bâtiments, ~20 ressources :
  bois → planches, minerais → acier → armes/outils/pièces d'or, blé → farine →
  pain, élevage, brasserie, etc.
* **Géologues** — prospection des montagnes et pose de panneaux de minerai.
* **Militaire** — recrutement (épée + bouclier + bière), garnisons, projection de
  **territoire**, promotions par pièces d'or, attaques, duels et **capture** de
  bâtiments ; **catapultes** de siège.
* **Brouillard de guerre** — exploration et diffusion d'état filtrée par joueur.
* **Conditions de victoire** — élimination d'un joueur quand il perd tous ses
  entrepôts, désignation du vainqueur et message `GAME_OVER`.
* **Adversaires IA** — joueurs contrôlés par ordinateur qui développent leur
  économie, relient leurs bâtiments et attaquent.
* **Naval** — chantiers navals, ports côtiers et **expéditions maritimes** qui
  colonisent des rivages inexplorés.
* **Sauvegarde / chargement** — instantané complet d'une partie en base et
  restauration en une nouvelle partie jouable.

---

## 📂 Structure du projet

```text
OpenSettlers/
├── backend/                       # Serveur Quarkus (Java 21)
│   ├── src/main/java/fr/opensettlers/
│   │   ├── utils/                 # Enums + GameConfig (toutes les constantes de réglage)
│   │   ├── entities/              # Modèle de jeu (bâtiments, unités, drapeaux, routes, tuiles)
│   │   ├── state/                 # GameState, GameSession, RoadNetwork, TerritoryManager, FogOfWar
│   │   ├── systems/               # Systèmes de simulation (1 par mécanique, interface ISystem)
│   │   ├── service/               # GameEngine (boucle), GameEngineService, GameActions, mapgen/
│   │   ├── controller/            # REST (Game/Save) + WebSocket + dto/
│   │   └── persistence/           # Panache GameSaveEntity + GameSnapshot + service
│   ├── src/test/java/             # Suite de tests unitaires JUnit 5
│   ├── src/main/resources/        # application.properties (Dev Services PostgreSQL)
│   ├── CLAUDE.md                  # Guide de développement du backend
│   └── pom.xml
└── frontend/                      # Client Nuxt 3 (en cours)
```

---

## 🚀 Installation et lancement

### Prérequis

* **Java 21+** (GraalVM recommandé pour les builds natifs)
* **Node.js 20+**
* **Docker** (PostgreSQL éphémère via Quarkus Dev Services)

### Backend

```bash
cd backend
export JAVA_HOME=$(brew --prefix openjdk)   # un JDK 21+
./mvnw quarkus:dev
```

* Dev UI Quarkus : `http://localhost:8080/q/dev/`
* API REST du lobby : `http://localhost:8080/games`
* WebSocket de jeu : `ws://localhost:8080/game/{gameId}`

> Les **tests unitaires ne nécessitent pas Docker** : `./mvnw test`.

### Frontend

```bash
cd frontend
npm install
npm run dev   # http://localhost:3000
```

---

## 📡 API réseau

### REST — lobby & sauvegardes

| Méthode | Route | Description |
| --- | --- | --- |
| `POST` | `/games` | Crée une partie. Corps : `{"playerCount": 2, "aiPlayers": 1}`. Renvoie le `gameId`. |
| `GET` | `/games` | Liste les parties actives. |
| `DELETE` | `/games/{gameId}` | Arrête une partie. |
| `POST` | `/games/{gameId}/save` | Sauvegarde une partie en cours. Corps : `{"name": "..."}`. |
| `GET` | `/saves` | Liste les sauvegardes. |
| `POST` | `/saves/{saveId}/load` | Recharge une sauvegarde comme nouvelle partie. |

### WebSocket — temps réel

`ws://localhost:8080/game/{gameId}?playerId=N` (omettre `playerId` pour un
spectateur).

* À la connexion, le serveur envoie un message `MAP` (terrain, élévation,
  ressources), puis un message `STATE` à chaque tick (bâtiments, drapeaux, routes,
  porteurs, ouvriers, soldats, navires, territoire), et enfin `GAME_OVER`.
* Le client envoie des `GameMessage` de type `BUILD_BUILDING`,
  `DESTROY_BUILDING`, `PLACE_FLAG`, `LINK_FLAGS`, `ATTACK_BUILDING` ou
  `SEND_GEOLOGIST`.

---

## 🔁 Game loop & systèmes

À chaque tick, `GameEngine` applique les commandes en file puis exécute les
systèmes dans l'ordre : IA → militaire → combat → catapultes → mouvement →
géologues → ouvriers → économie → construction → production → transport → ânes →
naval → vision → victoire, avant de diffuser l'état. Chaque système implémente
`ISystem` et n'agit que sur le `GameState`. Tout le **réglage** (tick, distances,
garnisons, IA, naval…) vit dans `fr.opensettlers.utils.GameConfig`.

---

## 🗺️ Feuille de route

* [x] **Socle technique** — Quarkus, WebSockets, REST, Dev Services PostgreSQL.
* [x] **Fondations logistiques** — carte hexagonale, drapeaux/routes,
  pathfinding, porteurs.
* [x] **Bâtiments & économie** — QG, extracteurs, chaînes de production complètes.
* [x] **Militaire & frontières** — territoire, recrutement, combats, captures,
  catapultes, promotions.
* [x] **Conditions de victoire** — élimination et désignation du vainqueur.
* [x] **Adversaires IA** — économie et offensive automatisées.
* [x] **Naval** — chantiers, ports, expéditions de colonisation.
* [x] **Persistance** — sauvegarde et chargement des parties.
* [ ] **Frontend** — rendu WebGL de la carte, interactions de construction,
  interpolation 60 FPS.
* [ ] **Comptes & classement** — authentification et ELO persistés.

---

## 📄 Licence

Voir [LICENSE](LICENSE).
