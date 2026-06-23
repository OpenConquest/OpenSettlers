package fr.opensettlers.state;

import fr.opensettlers.service.commands.GameCommand;
import io.quarkus.websockets.next.WebSocketConnection;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A live game: its {@link GameState}, the queue of player commands awaiting the
 * next tick, and the set of WebSocket connections (players and spectators) that
 * receive state broadcasts. One session exists per running game.
 */
@Getter
public class GameSession {
    /** Unique session identifier, shared with the hosted {@link GameState}. */
    private final UUID id;

    /** The authoritative game state advanced by the engine each tick. */
    private final GameState state;

    /** Player commands enqueued by WebSocket handlers, drained at the start of each tick. */
    private final Queue<GameCommand> commandQueue = new ConcurrentLinkedQueue<>();

    /** Open WebSocket connections (players and spectators) receiving state broadcasts. */
    private final Set<WebSocketConnection> connections = ConcurrentHashMap.newKeySet();

    /**
     * Player ID associated with each connection. Connections without an entry
     * are spectators and receive the unfiltered game state.
     */
    private final Map<WebSocketConnection, Integer> connectionPlayers = new ConcurrentHashMap<>();

    /**
     * Creates a session hosting a fresh, empty game state.
     *
     * @param id the session identifier
     */
    public GameSession(UUID id) {
        this.id = id;
        this.state = new GameState(id, new ArrayList<>());
    }

    /**
     * Wraps a pre-built game state in a session, used when restoring a saved
     * game from persistence.
     *
     * @param id    the session identifier
     * @param state the game state to host
     */
    public GameSession(UUID id, GameState state) {
        this.id = id;
        this.state = state;
    }

    /**
     * Enqueues a player command to be applied at the start of the next tick.
     *
     * @param command the command to queue
     */
    public void queueCommand(GameCommand command) {
        commandQueue.add(command);
    }

    /**
     * Registers a connection, optionally bound to a player for fog-of-war
     * filtered broadcasts.
     *
     * @param connection the WebSocket connection
     * @param playerId   the player this connection plays as, or {@code null} for a spectator
     */
    public void addConnection(WebSocketConnection connection, Integer playerId) {
        connections.add(connection);
        if (playerId != null) {
            connectionPlayers.put(connection, playerId);
        }
    }

    /**
     * Registers a spectator connection not bound to any player.
     *
     * @param connection the WebSocket connection
     */
    public void addConnection(WebSocketConnection connection) {
        addConnection(connection, null);
    }

    /**
     * Removes a connection and any player binding it held.
     *
     * @param connection the WebSocket connection to drop
     */
    public void removeConnection(WebSocketConnection connection) {
        connections.remove(connection);
        connectionPlayers.remove(connection);
    }

    /**
     * Returns the player a connection is bound to.
     *
     * @param connection the WebSocket connection
     * @return the player ID, or {@code null} for spectators
     */
    public Integer getPlayerFor(WebSocketConnection connection) {
        return connectionPlayers.get(connection);
    }
}
