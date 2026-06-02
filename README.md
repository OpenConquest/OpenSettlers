# 🏰 OpenSettlers

**OpenSettlers** est un clone open-source web de *The Settlers II: 10th Anniversary*. Ce projet vise à recréer l'expérience classique de gestion de chaînes de production, d'expansion territoriale et de logistique routière, en utilisant une architecture web moderne orientée temps réel avec un serveur hautement performant.

## 📑 Table des matières

1. [Architecture globale](#-architecture-globale)
2. [Stack Technique](#-stack-technique)
3. [Mécaniques implémentées & Architecture du Code](#-mecaniques-implementees--architecture-du-code)
4. [Structure du projet](#-structure-du-projet)
5. [Installation et Lancement](#-installation-et-lancement)
6. [Gestion du Réseau & Game Loop](#-gestion-du-reseau--game-loop)
7. [Feuille de route (Roadmap)](#-feuille-de-route-roadmap)

---

## 🏗 Architecture globale

Le projet repose sur un paradigme de **Serveur Autoritaire** avec un client axé sur le rendu et l'interface :

*   **Le Serveur (Quarkus) :** C'est le cerveau du jeu. Il fait tourner la "Game Loop" à un tick-rate fixe (10 TPS), calcule le pathfinding (recherche de chemin des porteurs par l'algorithme A*), l'économie, et valide chaque action. Il utilise **Quarkus WebSockets Next** pour diffuser périodiquement l'état du jeu aux clients de manière non bloquante.
*   **Le Client (Nuxt 3 + WebGL) :** Gère le matchmaking, les menus et le HUD de construction via Vue.js. La carte en elle-même est rendue via un canvas (Three.js/WebGL). Le client reçoit les "snapshots" du serveur, interpole le mouvement des unités pour une fluidité à 60+ FPS, et transmet les intentions du joueur (ex: "Construire un bûcheron ici").

---

## 🛠 Stack Technique

| Composant | Technologie | Rôle |
| --- | --- | --- |
| **Framework Backend** | **Quarkus 3.36.0 (Java 21)** | Moteur serveur ultra-rapide avec injection de dépendances (ArC) et scheduler. |
| **Base de Données & ORM** | **PostgreSQL + Hibernate Panache** | Persistance des comptes utilisateurs, des sauvegardes globales et des stats d'ELO via le pattern *Active Record*. |
| **Logique Temps Réel** | **Quarkus WebSockets Next** | WebSocket moderne réactif pour des connexions bi-directionnelles ultra-rapides et non-bloquantes. |
| **Moteur JSON** | **Jackson** | Sérialisation et désérialisation polymorphique automatique des messages. |
| **Framework Frontend** | **Nuxt 3 (Vue.js)** | UI réactive, HUD, menus, et architecture globale du client web. |
| **State Management** | **Pinia** | Pont de données réactif entre les WebSockets, l'UI et le moteur de rendu. |

---

## ⚙️ Mécaniques implémentées & Architecture du Code

### 1. Modèle de Carte Hexagonale (`fr.opensettlers.model.*`)
*   **`MapTile.java`** : Représente une tuile de la carte avec ses caractéristiques physiques (terrain, altitude, ressources naturelles récoltables).
*   **`GameMap.java`** : Système de coordonnées hexagonales (décalées / offset coordinates) permettant de calculer géométriquement les 6 voisins de chaque tuile.
*   **`GameState.java`** : "Single source of truth" contenant en RAM l'état des drapeaux, routes, porteurs, bâtiments et joueurs actifs pour chaque partie.

### 2. Réseau Logistique & Pathfinding (`fr.opensettlers.engine.*`)
*   **Drapeaux et Routes (`Flag.java` / `Road.java`)** : Les drapeaux font office de nœuds logistiques dotés d'un inventaire tampon de 8 ressources maximum. Les routes relient deux drapeaux.
*   **Pathfinding A\* (`PathfindingService.java`)** : Calcule dynamiquement le chemin optimal le plus court à travers le réseau routier pour acheminer les matières premières de leur point de production à leur bâtiment cible.
*   **Moteur Économique (`EconomyService.java`)** : Gère la vitesse de déplacement normalisée des porteurs le long des routes ainsi que la progression de la construction des bâtiments à chaque tick.

---

## 📂 Structure du projet

```bash
opensettlers/
├── backend/opensettlers/      # Serveur Quarkus
│   ├── src/main/java/fr/opensettlers/
│   │   ├── engine/            # Moteur temps réel (GameLoop, Economy, Pathfinding A*, GameRoom)
│   │   ├── entities/          # Entités JPA Panache persistées (PlayerEntity, GameSaveEntity, PlayerStatsEntity)
│   │   ├── model/             # Modèles de jeu en mémoire pure (POJOs hexagonaux, GameMap, Flag, Building)
│   │   │   └── message/       # Protocole WebSocket (Messages polymorphiques Jackson & StateSnapshot)
│   │   ├── network/           # Endpoints d'API REST (LobbyResource) et WebSockets (GameSocket)
│   │   └── services/          # Services métier (LobbyService, PlayerService, StatsService)
│   ├── src/main/resources/    # Configuration (application.properties avec Dev Services PostgreSQL)
│   └── pom.xml                # Dépendances Maven (WebSockets-next, Scheduler, Panache, Postgresql)
│
├── frontend/                 # Client Nuxt 3 (En cours)
│   ├── components/           # Composants Vue (HUD, Minimap, Menus)
│   ├── game/                 # Moteur WebGL (Three.js)
│   ├── pages/                # Vues Nuxt (Accueil, Lobby, InGame)
│   └── nuxt.config.ts        # Configuration Nuxt
```

---

## 🚀 Installation et Lancement

### Prérequis

*   **Java 21+** (GraalVM recommandé pour les compilations natives)
*   **Node.js 20+**
*   **Docker** (nécessaire pour exécuter automatiquement PostgreSQL via Testcontainers/Dev Services)

### Démarrage Rapide

**1. Backend (Quarkus avec Dev Services PostgreSQL)**

Grâce à Quarkus Dev Services, pas besoin de configurer manuellement une base de données locale ou d'exécuter un docker-compose. Quarkus lancera automatiquement une instance PostgreSQL Docker éphémère et configurera l'ORM Panache dès que vous démarrerez en mode développement.

```bash
cd backend/opensettlers
./mvnw clean compile quarkus:dev
```

*   L'interface d'administration Quarkus Dev UI est accessible sur `http://localhost:8080/q/dev`.
*   Les API REST du lobby sont servies sur `http://localhost:8080/api/lobby/*`.
*   Le point d'accès WebSocket du jeu est disponible sur `ws://localhost:8080/game/{roomId}`.

**2. Frontend (Nuxt 3)**

```bash
cd frontend
npm install
npm run dev
```

*   Le client web sera disponible sur `http://localhost:3000`.

---

## 📡 Gestion du Réseau & Game Loop

Pour garantir un serveur autoritaire sans pénaliser les performances :

*   **GameLoop (`GameLoop.java`) :** Un scheduler Quarkus s'exécute de manière asynchrone toutes les 100ms (`@Scheduled(every = "0.1s")`, soit 10 Ticks Par Seconde).
*   **Diffusion Réseau :** Toutes les 5 ticks (2 Hz), un instantané de l'état du jeu (`StateSnapshot.java`) est généré à partir de la simulation en mémoire vive et diffusé en JSON à tous les joueurs connectés sous forme d'événements asynchrones non-bloquants.
*   **Persistance Asynchrone :** La base de données PostgreSQL est totalement exclue de la boucle de simulation temps réel pour éviter tout blocage I/O. L'état en RAM n'est sauvegardé en base de données via Panache (`GameSaveEntity`) que sur commande ou périodiquement (ex: toutes les 5 minutes).

---

## 🗺️ Feuille de route (Roadmap)

*   [x] **Phase 1 : Socle Technique (PoC) Backend**
    *   [x] Initialisation Quarkus Java 21 & Maven dependencies.
    *   [x] Configuration PostgreSQL Dev Services automatique.
    *   [x] Point d'entrée WebSocket bi-directionnel avec `@WebSocket`.
    *   [x] REST API pour le lobby de matchmaking et l'authentification.
*   [ ] **Phase 1 : Socle Technique (PoC) Frontend**
    *   [ ] Connexion WebSocket côté Nuxt.
    *   [ ] Affichage d'une grille/carte basique dans le canvas web.

*   [x] **Phase 2 : Fondations Logistiques (Backend complété !)**
    *   [x] Grille de carte hexagonale procédurale (`GameMap` + `MapTile`).
    *   [x] Réseau de drapeaux (`Flag`) et de routes (`Road`).
    *   [x] Pathfinding optimal A* côté Quarkus (`PathfindingService`).
    *   [x] Mouvement normalisé des transporteurs sur les routes (`EconomyService`).
*   [ ] **Phase 2 : Fondations Logistiques (Frontend)**
    *   [ ] Tracé de routes et pose de drapeaux interactifs dans l'interface WebGL.
    *   [ ] Rendu fluide des porteurs et interpolation à 60 FPS.

*   [ ] **Phase 3 : Bâtiments & Économie primaire**
    *   [ ] Quartier Général (stockage initial).
    *   [ ] Bâtiments de base : Bûcheron, Forestier, Carrière de pierre.
    *   [ ] Transfert de ressources physiques entre les drapeaux.

*   [ ] **Phase 4 : Économie avancée & Métallurgie**
    *   [ ] Chaînes de nourriture : Fermes, moulins, boulangeries.
    *   [ ] Réseau minier : charbon, fer, or.
    *   [ ] Armurerie et fonderie pour fabriquer des outils et armes.

*   [ ] **Phase 5 : Militaire & Frontières**
    *   [ ] Algorithme d'expansion territoriale géométrique.
    *   [ ] Recrutement de soldats, casernes et forts militaires.
    *   [ ] Combats aux frontières et conquêtes de territoires.
