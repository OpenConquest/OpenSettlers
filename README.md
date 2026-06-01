# 🏰 OpenSettlers

**OpenSettlers** est un clone open-source web de *The Settlers II: 10th Anniversary*. Ce projet vise à recréer l'expérience classique de gestion de chaînes de production, d'expansion territoriale et de logistique routière, en utilisant une architecture web moderne orientée temps réel.

## 📑 Table des matières

1. [Architecture globale](https://www.google.com/search?q=%23architecture-globale)
2. [Stack Technique](https://www.google.com/search?q=%23stack-technique)
3. [Mécaniques de base à implémenter](https://www.google.com/search?q=%23m%C3%A9caniques-de-base-%C3%A0-impl%C3%A9menter)
4. [Structure du projet](https://www.google.com/search?q=%23structure-du-projet)
5. [Installation et Lancement](https://www.google.com/search?q=%23installation-et-lancement)
6. [Gestion du Réseau & Game Loop](https://www.google.com/search?q=%23gestion-du-r%C3%A9seau--game-loop)
7. [Feuille de route (Roadmap)](https://www.google.com/search?q=%23feuille-de-route-roadmap)

---

## 🏗 Architecture globale

Le projet repose sur un paradigme de **Serveur Autoritaire** avec un client axé sur le rendu et l'interface :

* **Le Serveur (Quarkus) :** C'est le cerveau du jeu. Il fait tourner la "Game Loop" à un tick-rate fixe, calcule le pathfinding (recherche de chemin des porteurs), l'économie, et valide chaque action. Il utilise Vert.x pour diffuser l'état du jeu aux clients sans bloquer les threads.
* **Le Client (Nuxt 3 + WebGL) :** Gère le matchmaking, les menus et le HUD de construction via Vue.js. La carte en elle-même est rendue via un canvas (Three.js ou PixiJS). Le client reçoit les "snapshots" du serveur, interpole le mouvement des unités pour une fluidité à 60+ FPS, et transmet les intentions du joueur (ex: "Construire un bûcheron ici").

---

## 🛠 Stack Technique

| Composant | Technologie | Rôle |
| --- | --- | --- |
| **Framework Backend** | **Quarkus** | Application serveur ultra-rapide, compilation native possible via GraalVM. |
| **Base de Données & ORM** | **PostgreSQL + Hibernate Panache** | Persistance des utilisateurs, sauvegardes, et statistiques via le pattern *Active Record*. |
| **Temps Réel (Back)** | **Eclipse Vert.x (WebSockets)** | Gestion asynchrone et non-bloquante des connexions multijoueurs. |
| **Framework Frontend** | **Nuxt 3 (Vue.js)** | UI réactive, HUD, menus, et architecture globale du client web. |
| **State Management** | **Pinia** | Pont de données entre les WebSockets, l'UI Vue.js et le moteur de rendu. |
| **Moteur de Rendu** | **Three.js** (ou Babylon/Pixi) | Rendu de la carte, des bâtiments et des "settlers" à l'intérieur d'un composant Nuxt. |

---

## ⚙️ Mécaniques de base à implémenter

1. **Le Système Logistique (Nœuds et Routes) :**
* Le cœur de *Settlers II*. Les drapeaux sont des nœuds de graphe.
* L'algorithme A* ou Dijkstra côté serveur doit router les ressources de leur point d'extraction à leur point d'utilisation.


2. **Chaînes de Production (Économie) :**
* Un système d'inventaire décentralisé. Exemple : Blé ➔ Moulin ➔ Farine ➔ Boulangerie ➔ Pain.


3. **Expansion Territoriale :**
* Génération de diagrammes de Voronoï modifiés ou propagation géométrique depuis les bâtiments militaires (Baraques, Forts) pour définir la frontière du joueur.



---

## 📂 Structure du projet

```bash
opensettlers/
├── backend/                  # Serveur Quarkus
│   ├── src/main/java/.../
│   │   ├── engine/           # Game loop, Pathfinding, Algorithmes spatiaux
│   │   ├── entities/         # Entités Panache (ex: extends PanacheEntity)
│   │   ├── network/          # Vert.x WebSocket handlers (Endpoints)
│   │   └── services/         # Logique métier (Combat, Économie)
│   └── pom.xml               # Configuration Maven (ou build.gradle)
│
├── frontend/                 # Client Nuxt 3
│   ├── components/           # Composants Vue (HUD, Minimap, Menus)
│   ├── composables/          # Logique réutilisable (ex: useWebSocket)
│   ├── game/                 # Moteur WebGL pur (Three.js : Scène, Caméra, Mesh)
│   ├── pages/                # Vues Nuxt (Accueil, Lobby, InGame)
│   ├── stores/               # Pinia stores (GameStore, PlayerStore)
│   └── nuxt.config.ts        # Configuration Nuxt
│
└── docs/                     # Game Design Document (GDD) et API Specs

```

---

## 🚀 Installation et Lancement

### Prérequis

* Java 21+ (GraalVM recommandé)
* Node.js 20+
* Docker & Docker Compose (pour la base de données)

### Démarrage Rapide

**1. Base de données**

```bash
docker-compose up -d db

```

*(Assurez-vous que votre `application.properties` Quarkus pointe vers la bonne URL JDBC).*

**2. Backend (Quarkus avec Live Reload)**

```bash
cd backend
./mvnw compile quarkus:dev

```

*L'interface de dev Quarkus sera disponible sur `http://localhost:8080/q/dev`.*

**3. Frontend (Nuxt 3)**

```bash
cd frontend
npm install
npm run dev

```

*Le jeu sera jouable sur `http://localhost:3000`.*

---

## 📡 Gestion du Réseau & Game Loop

Pour garantir un serveur autoritaire sans pénaliser les performances :

* **Séparation des Threads :** La *Game Loop* Java doit tourner sur un thread dédié (ex: via les schedulers Quarkus à 10 Ticks Par Seconde). Le réseau (Vert.x) tourne sur des threads I/O séparés.
* **Architecture Panache :** Hibernate Panache est parfait pour charger les données (parties sauvegardées, comptes) lors de l'initialisation de la room, mais la simulation en temps réel doit se faire **en mémoire vive (RAM)**. On ne sauvegarde l'état en BDD via Panache que périodiquement (ex: toutes les 5 minutes).
* **Interpolation :** Le client Nuxt reçoit l'état du serveur à 10 Hz mais rend le jeu à 60 Hz. Les entités (porteurs, ressources) doivent glisser visuellement entre leur position précédente et la nouvelle position reçue.

---

## 🗺️ Feuille de route (Roadmap)

* [ ] **Phase 1 : Socle Technique (PoC)**
* Initialisation Nuxt / Quarkus.
* Connexion WebSockets bi-directionnelle.
* Affichage d'une grille/carte basique dans le canvas web.


* [ ] **Phase 2 : Fondations Logistiques**
* Pose de drapeaux et création de routes.
* Implémentation du pathfinding côté Quarkus.
* Mouvement d'un porteur test d'un drapeau à l'autre.


* [ ] **Phase 3 : Bâtiments & Économie primaire**
* Quartier Général.
* Bûcheron, Forestier, Carrière de pierre.
* Transfert visuel des ressources sur les routes.


* [ ] **Phase 4 : Économie avancée & Métallurgie**
* Fermes, nourriture, mines (charbon, fer, or).
* Gestion des stocks globaux et limites de production.


* [ ] **Phase 5 : Militaire & Frontières**
* Algorithme de calcul de frontière.
* Recrutement de soldats et conquête de bâtiments.
* Matchmaking multijoueur finalisé.
