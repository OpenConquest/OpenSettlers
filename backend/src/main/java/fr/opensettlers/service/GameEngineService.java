package fr.opensettlers.service;

import fr.opensettlers.utils.GameConfig;
import fr.opensettlers.state.GameSession;
import fr.opensettlers.state.GameState;
import fr.opensettlers.service.commands.GameCommand;
import fr.opensettlers.service.mapgen.MapGenerator;
import fr.opensettlers.entities.Building;
import fr.opensettlers.entities.BuildingFactory;
import fr.opensettlers.entities.MapTile;
import fr.opensettlers.utils.BuildingName;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.controller.GameMessage;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Set;
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

    /** The logger. */
    private static final Logger LOG = Logger.getLogger(GameEngineService.class);

    /** Map of active game sessions, indexed by game ID. */
    private final Map<UUID, GameSession> sessions = new ConcurrentHashMap<>();

    /** Map of running game engines, indexed by game ID. */
    private final Map<UUID, GameEngine> engines = new ConcurrentHashMap<>();

    /** Shared executor driving the game loops. */
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);

    /**
     * Creates a new game: generates the map, places one headquarters per player,
     * computes the initial territories, and starts the game loop.
     *
     * @param playerCount number of players (headquarters placed evenly around the island)
     * @return the created game session
     */
    public GameSession createGame(int playerCount) {
        return createGame(playerCount, 0);
    }

    /**
     * Creates a new game with a number of computer-controlled opponents. The
     * last {@code aiPlayers} player slots are driven by the built-in AI; the
     * remaining slots are reserved for human players.
     *
     * @param playerCount total number of players (human and AI)
     * @param aiPlayers   how many of those players are computer-controlled
     * @return the created game session
     */
    public GameSession createGame(int playerCount, int aiPlayers) {
        UUID gameId = UUID.randomUUID();
        GameSession session = new GameSession(gameId);
        GameState state = session.getState();
        state.setPlayerCount(playerCount);

        int aiCount = Math.max(0, Math.min(aiPlayers, playerCount));
        for (int playerId = playerCount - aiCount; playerId < playerCount; playerId++) {
            state.getAiPlayers().add(playerId);
        }

        MapTile[][] grid = new MapGenerator()
                .generateContinentalGrid(GameConfig.MAP_SIZE, GameConfig.MAP_SIZE);
        state.setMapTilesFromGrid(grid);
        placeHeadquarters(state, playerCount);
        state.getTerritoryManager().recalculate(state);

        sessions.put(gameId, session);
        GameEngine engine = new GameEngine(session, executor);
        engines.put(gameId, engine);
        engine.start();

        LOG.infof("Game %s created with %d players (%d AI, map %dx%d)",
                gameId, playerCount, aiCount, GameConfig.MAP_SIZE, GameConfig.MAP_SIZE);
        return session;
    }

    /**
     * Registers a game restored from a snapshot and starts its loop.
     *
     * @param state the reconstructed game state (its game ID becomes the new game ID)
     * @return the created game session
     */
    public GameSession loadGame(GameState state) {
        UUID gameId = state.getGameId();
        GameSession session = new GameSession(gameId, state);
        sessions.put(gameId, session);
        GameEngine engine = new GameEngine(session, executor);
        engines.put(gameId, engine);
        engine.start();
        LOG.infof("Game %s restored from save (%d players, tick %d)",
                gameId, state.getPlayerCount(), state.getCurrentTick());
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
    public Set<UUID> getActiveGameIds() {
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

        session.queueCommand(new GameCommand() {
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
        int playerId = message.getPlayerId();
        switch (message.getType()) {
            case BUILD_BUILDING -> {
                if (GameActions.placeBuilding(state, playerId, message.getBuildingName(), message.getPosition())) {
                    LOG.infof("Construction site for %s placed at %s",
                            message.getBuildingName(), message.getPosition());
                } else {
                    LOG.warnf("Invalid placement for %s at %s by player %d",
                            message.getBuildingName(), message.getPosition(), playerId);
                }
            }

            case DESTROY_BUILDING -> {
                if (GameActions.destroyBuilding(state, playerId, message.getTargetId())) {
                    LOG.infof("Building %s destroyed.", message.getTargetId());
                }
            }

            case PLACE_FLAG -> {
                if (GameActions.placeFlag(state, playerId, message.getPosition()) != null) {
                    LOG.infof("Flag placed at %s", message.getPosition());
                } else {
                    LOG.warnf("Player %d cannot place a flag outside their territory", playerId);
                }
            }

            case LINK_FLAGS -> {
                if (GameActions.linkFlags(state, message.getFlagIdA(), message.getFlagIdB(), message.getPath())) {
                    LOG.infof("Road created between %s and %s", message.getFlagIdA(), message.getFlagIdB());
                } else {
                    LOG.warn("Failed to link flags: a flag was not found.");
                }
            }

            case ATTACK_BUILDING -> {
                int dispatched = GameActions.attack(state, playerId, message.getTargetId());
                if (dispatched < 0) {
                    LOG.warn("Invalid attack target.");
                } else {
                    LOG.infof("Attack order on %s: %d soldiers dispatched",
                            message.getTargetId(), dispatched);
                }
            }

            case SEND_GEOLOGIST -> {
                if (GameActions.sendGeologist(state, playerId, message.getTargetId())) {
                    LOG.infof("Geologist dispatched to flag %s", message.getTargetId());
                } else {
                    LOG.warnf("Player %d cannot dispatch a geologist to flag %s",
                            playerId, message.getTargetId());
                }
            }

            case SET_PRODUCTION -> {
                if (GameActions.setProduction(state, playerId, message.getTargetId(), message.isEnabled())) {
                    LOG.infof("Production %s for building %s",
                            message.isEnabled() ? "resumed" : "paused", message.getTargetId());
                }
            }

            case SET_COIN_DELIVERY -> {
                if (GameActions.setCoinDelivery(state, playerId, message.getTargetId(), message.isEnabled())) {
                    LOG.infof("Coin delivery %s for building %s",
                            message.isEnabled() ? "enabled" : "disabled", message.getTargetId());
                }
            }

            case SET_DISTRIBUTION -> {
                if (GameActions.setDistribution(state, playerId, message.getResourceType(), message.getPriorities())) {
                    LOG.infof("Distribution priorities updated for %s by player %d",
                            message.getResourceType(), playerId);
                }
            }

            case SET_MILITARY -> {
                GameActions.setMilitaryOccupation(state, playerId, message.getMilitaryOccupation());
                LOG.infof("Military occupation set to %d%% by player %d",
                        message.getMilitaryOccupation(), playerId);
            }
        }
    }

    /**
     * Places one headquarters per player, spaced evenly around the island.
     *
     * @param state       the game state to populate
     * @param playerCount the number of players
     */
    private void placeHeadquarters(GameState state, int playerCount) {
        double center = GameConfig.MAP_SIZE / 2.0;
        double spawnRadius = GameConfig.MAP_SIZE * 0.3;

        for (int playerId = 0; playerId < playerCount; playerId++) {
            double angle = 2 * Math.PI * playerId / playerCount;
            int idealX = (int) Math.round(center + spawnRadius * Math.cos(angle));
            int idealY = (int) Math.round(center + spawnRadius * Math.sin(angle));
            // Convert array position to double-height coordinates
            Coordinates ideal = new Coordinates(idealX, 2.0 * idealY + (idealX % 2));

            MapTile spot = findClosestBuildableTile(state, ideal);
            if (spot == null) {
                LOG.errorf("No buildable tile found for player %d headquarters", playerId);
                continue;
            }

            Coordinates position = new Coordinates(
                    spot.getCoordinates().getX(), spot.getCoordinates().getY());
            Building hq = BuildingFactory.createBuilding(
                    BuildingName.HEADQUARTERS, playerId, position, state);
            state.getBuildings().add(hq);
            state.getFlags().add(hq.getAttachedFlag());
            state.getRoadNetwork().addFlag(hq.getAttachedFlag());
            LOG.infof("Headquarters for player %d placed at %s", playerId, position);
        }
    }

    /**
     * Finds the buildable tile closest to the given coordinates.
     *
     * @param state the game state
     * @param ideal the preferred coordinates
     * @return the closest buildable tile, or {@code null} if the map has none
     */
    private MapTile findClosestBuildableTile(GameState state, Coordinates ideal) {
        MapTile best = null;
        int bestDist = Integer.MAX_VALUE;
        for (MapTile tile : state.getMapTiles().values()) {
            if (!tile.isBuildable()) continue;
            int dist = tile.getCoordinates().distanceTo(ideal);
            if (dist < bestDist) {
                bestDist = dist;
                best = tile;
            }
        }
        return best;
    }

    /** Shuts down all game loops when the application stops. */
    @PreDestroy
    void shutdown() {
        engines.values().forEach(GameEngine::stop);
        executor.shutdownNow();
    }
}
