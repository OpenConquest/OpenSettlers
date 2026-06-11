package fr.opensettlers.services;

import fr.opensettlers.engine.GameConfig;
import fr.opensettlers.engine.GameEngine;
import fr.opensettlers.engine.GameSession;
import fr.opensettlers.engine.GameState;
import fr.opensettlers.engine.mapgen.MapGenerator;
import fr.opensettlers.engine.state.*;
import fr.opensettlers.engine.state.utils.BuildingName;
import fr.opensettlers.engine.state.utils.Coordinates;
import fr.opensettlers.engine.state.utils.SoldierState;
import fr.opensettlers.engine.state.utils.TileType;
import fr.opensettlers.network.GameMessage;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.jboss.logging.Logger;

/**
 * Service managing active game sessions and processing player actions.
 */
@ApplicationScoped
public class GameEngineService {

    private static final Logger LOG = Logger.getLogger(GameEngineService.class);

    /** Radius of territory claimed around a headquarters at game start. */
    private static final int HQ_TERRITORY_RADIUS = 10;


    /** Map of active game sessions, indexed by game ID. */
    private final Map<UUID, GameSession> sessions = new ConcurrentHashMap<>();

    /** Map of running game engines, indexed by game ID. */
    private final Map<UUID, GameEngine> engines = new ConcurrentHashMap<>();

    /** Shared executor driving the game loops. */
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);

    /**
     * Creates a new game: generates the map, places one headquarters per player,
     * and starts the game loop.
     *
     * @param playerCount number of players (headquarters placed evenly around the island)
     * @return the created game session
     */
    public GameSession createGame(int playerCount) {
        UUID gameId = UUID.randomUUID();
        GameSession session = new GameSession(gameId);
        GameState state = session.getState();

        GameMap map = new GameMap(new MapGenerator().generateContinentalGrid(GameConfig.MAP_SIZE));
        state.setMap(map);
        placeHeadquarters(state, playerCount);

        sessions.put(gameId, session);
        GameEngine engine = new GameEngine(session, executor);
        engines.put(gameId, engine);
        engine.start();

        LOG.infof("Game %s created with %d players (map %dx%d)",
                gameId, playerCount, map.getSize(), map.getSize());
        return session;
    }

    /**
     * Stops a game loop and removes the session.
     *
     * @param gameId the game to stop
     * @return {@code true} if a game was stopped
     */
    public boolean stopGame(UUID gameId) {
        GameEngine engine = engines.remove(gameId);
        if (engine != null) {
            engine.stop();
        }
        return sessions.remove(gameId) != null;
    }

    /**
     * Retrieves a game session.
     *
     * @param gameId the unique identifier of the game
     * @return the session, or null if not found
     */
    public GameSession getSession(UUID gameId) {
        return sessions.get(gameId);
    }

    /**
     * Retrieves a game state.
     *
     * @param gameId the unique identifier of the game to retrieve
     * @return the game state corresponding to the game ID, or null if not found
     */
    public GameState getGame(UUID gameId) {
        GameSession session = sessions.get(gameId);
        return session != null ? session.getState() : null;
    }

    /**
     * Returns the identifiers of all active games.
     *
     * @return the set of active game IDs
     */
    public java.util.Set<UUID> getActiveGameIds() {
        return sessions.keySet();
    }

    /**
     * Queues an incoming WebSocket message for the game loop. The action is
     * applied at the start of the next tick, on the game loop thread, to avoid
     * concurrent mutation of the game state.
     *
     * @param gameId the unique identifier of the game to process the message for
     * @param message the incoming game message containing the player's action
     */
    public void processMessage(UUID gameId, GameMessage message) {
        GameSession session = sessions.get(gameId);
        if (session == null) {
            LOG.errorf("Game not found: %s", gameId);
            return;
        }

        session.queueCommand(new fr.opensettlers.engine.commands.GameCommand() {
            @Override
            public int getPlayerId() {
                return message.getPlayerId();
            }

            @Override
            public void execute(GameState state) {
                applyMessage(state, message);
            }
        });
    }

    /**
     * Applies a player action to the game state. Must only be called from the
     * game loop thread.
     *
     * @param state   the game state to mutate
     * @param message the player action
     */
    private void applyMessage(GameState state, GameMessage message) {
        switch (message.getType()) {
            case BUILD_BUILDING -> handleBuild(state, message);

            case DESTROY_BUILDING -> {
                Building b = state.getBuildings().stream()
                        .filter(x -> x.getId().equals(message.getTargetId()))
                        .findFirst()
                        .orElse(null);

                if (b != null && b.getPlayerId() == message.getPlayerId()) {
                    b.destroy(); // Flags and building marked as destroyed
                    LOG.infof("Building %s destroyed.", b.getId());
                }
            }

            case PLACE_FLAG -> {
                if (state.getMap() != null
                        && state.getMap().getOwner(message.getPosition()) != message.getPlayerId()) {
                    LOG.warnf("Player %d cannot place a flag outside their territory", message.getPlayerId());
                    return;
                }
                Flag flag = new Flag(UUID.randomUUID(), message.getPlayerId(), message.getPosition());
                state.getFlags().add(flag);
                state.getRoadNetwork().addFlag(flag);
                LOG.infof("Flag placed at %s", message.getPosition());
            }

            case LINK_FLAGS -> {
                Flag flagA = state.getRoadNetwork().getFlagById(message.getFlagIdA());
                Flag flagB = state.getRoadNetwork().getFlagById(message.getFlagIdB());

                if (flagA != null && flagB != null) {
                    state.getRoadNetwork().addRoad(flagA, flagB, message.getPath());
                    LOG.infof("Road created between %s and %s", flagA.getId(), flagB.getId());
                } else {
                    LOG.warn("Failed to link flags: a flag was not found.");
                }
            }

            case ATTACK_BUILDING -> handleAttack(state, message);
        }
    }

    /**
     * Validates terrain and territory, then places a construction site.
     *
     * @param state   the game state
     * @param message the build order
     */
    private void handleBuild(GameState state, GameMessage message) {
        GameMap map = state.getMap();
        if (map != null && !isPlacementValid(map, message.getBuildingName(), message.getPosition(), message.getPlayerId())) {
            LOG.warnf("Invalid placement for %s at %s by player %d",
                    message.getBuildingName(), message.getPosition(), message.getPlayerId());
            return;
        }

        ConstructionSite site = new ConstructionSite(
                message.getPlayerId(),
                message.getPosition(),
                message.getBuildingName()
        );
        state.getBuildings().add(site);
        state.getRoadNetwork().addFlag(site.getAttachedFlag());
        LOG.infof("Construction site for %s placed at %s", message.getBuildingName(), message.getPosition());
    }

    /**
     * Checks terrain and ownership rules for a building placement.
     * Mines must stand on owned mountain tiles; everything else needs owned grass.
     *
     * @param map      the game map
     * @param name     the building type
     * @param position the desired position
     * @param playerId the building player
     * @return {@code true} if the placement is allowed
     */
    private boolean isPlacementValid(GameMap map, BuildingName name, Coordinates position, int playerId) {
        MapTile tile = map.getTile(position);
        if (tile == null || map.getOwner(position) != playerId) {
            return false;
        }
        if (name == BuildingName.MINE) {
            return tile.getType() == TileType.MOUNTAIN;
        }
        return tile.isBuildable();
    }

    /**
     * Sends soldiers from the player's nearby military buildings toward an enemy building.
     * Each garrison keeps one defender behind.
     *
     * @param state   the game state
     * @param message the attack order
     */
    private void handleAttack(GameState state, GameMessage message) {
        Building target = state.getBuildings().stream()
                .filter(x -> x.getId().equals(message.getTargetId()))
                .findFirst()
                .orElse(null);

        if (target == null || target.isDestroyed() || target.getPlayerId() == message.getPlayerId()) {
            LOG.warn("Invalid attack target.");
            return;
        }
        if (!(target instanceof MilitaryBuilding) && !(target instanceof StorageBuilding)) {
            LOG.warn("Only military buildings and warehouses can be attacked.");
            return;
        }

        int dispatched = 0;
        for (Building b : state.getBuildings()) {
            if (!(b instanceof MilitaryBuilding mb) || mb.isDestroyed()
                    || mb.getPlayerId() != message.getPlayerId()) {
                continue;
            }
            double dist = Math.hypot(
                    mb.getPosition().getX() - target.getPosition().getX(),
                    mb.getPosition().getY() - target.getPosition().getY());
            if (dist > GameConfig.ATTACK_RADIUS) {
                continue;
            }

            // Keep one defender in each garrison
            while (mb.getSoldiers().size() > 1) {
                Soldier soldier = mb.releaseSoldier();
                soldier.setPosition(new Coordinates(
                        mb.getPosition().getX(), mb.getPosition().getY()));
                soldier.setState(SoldierState.ATTACKING);
                soldier.setTargetBuildingId(target.getId());
                state.getSoldiers().add(soldier);
                dispatched++;
            }
        }
        LOG.infof("Attack order on %s: %d soldiers dispatched", target.getId(), dispatched);
    }

    /**
     * Places one headquarters per player, spaced evenly around the island,
     * and claims their starting territory.
     *
     * @param state       the game state to populate
     * @param playerCount the number of players
     */
    private void placeHeadquarters(GameState state, int playerCount) {
        GameMap map = state.getMap();
        double center = map.getSize() / 2.0;
        double spawnRadius = map.getSize() * 0.3;

        for (int playerId = 0; playerId < playerCount; playerId++) {
            double angle = 2 * Math.PI * playerId / playerCount;
            Coordinates ideal = new Coordinates(
                    Math.round(center + spawnRadius * Math.cos(angle)),
                    Math.round(center + spawnRadius * Math.sin(angle)));

            MapTile spot = map.findClosestTile(ideal, map.getSize() / 2, MapTile::isBuildable);
            if (spot == null) {
                LOG.errorf("No buildable tile found for player %d headquarters", playerId);
                continue;
            }

            Coordinates position = new Coordinates(
                    spot.getCoordinates().getX(), spot.getCoordinates().getY());
            Building hq = fr.opensettlers.engine.BuildingFactory.createBuilding(
                    BuildingName.HEADQUARTERS, playerId, position, map);
            state.getBuildings().add(hq);
            state.getFlags().add(hq.getAttachedFlag());
            state.getRoadNetwork().addFlag(hq.getAttachedFlag());
            map.claimTerritory(position, HQ_TERRITORY_RADIUS, playerId);
            LOG.infof("Headquarters for player %d placed at %s", playerId, position);
        }
    }

    /** Shuts down all game loops when the application stops. */
    @PreDestroy
    void shutdown() {
        engines.values().forEach(GameEngine::stop);
        executor.shutdownNow();
    }
}
