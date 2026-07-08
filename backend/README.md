# opensettlers

Backend of OpenSettlers, a clone of The Settlers II (10th Anniversary): map
generation, economy (roads, carriers, supply/demand), construction, production
chains tied to map resources, military (recruitment, attack, capture, catapults),
sea expeditions, computer opponents, win conditions, and save/load.

See [CLAUDE.md](CLAUDE.md) for the architecture and developer conventions.

## Game API

REST (lobby & saves):

- `POST /games` with `{"playerCount": 2, "aiPlayers": 1}` → creates a game (map
  generated, one headquarters per player, game loop started) and returns
  `gameId`. `aiPlayers` (optional) makes the last slots computer-controlled.
- `GET /games` → lists active game IDs.
- `DELETE /games/{gameId}` → stops a game.
- `POST /games/{gameId}/save` with `{"name": "..."}` → persists a snapshot,
  returns a `saveId`.
- `GET /saves` → lists saved games.
- `POST /saves/{saveId}/load` → restores a save as a new running game.

WebSocket (real time): `ws://localhost:8080/game/{gameId}?playerId=N`
(omit `playerId` for a spectator).

- On connect the server sends a `MAP` message (terrain, elevation, natural
  resources), then a `STATE` message every tick (buildings, flags, roads,
  carriers, workers, soldiers, ships, territory owners), and a final `GAME_OVER`.
- Client actions are JSON messages with a `type` of `BUILD_BUILDING`,
  `DESTROY_BUILDING`, `PLACE_FLAG`, `LINK_FLAGS`, `ATTACK_BUILDING` or
  `SEND_GEOLOGIST` (see `fr.opensettlers.controller.GameMessage`).

Engine tuning (tick rate, map size, production time, soldier speed, attack
radius, AI cadence, naval expeditions, …) lives as constants in
`fr.opensettlers.utils.GameConfig`.

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/opensettlers-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): Build RESTful web services and APIs using Jakarta REST (formerly JAX-RS)

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
