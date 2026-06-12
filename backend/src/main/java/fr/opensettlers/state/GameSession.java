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

@Getter
public class GameSession {
    private final UUID id;
    private final GameState state;
    private final Queue<GameCommand> commandQueue = new ConcurrentLinkedQueue<>();
    private final Set<WebSocketConnection> connections = ConcurrentHashMap.newKeySet();

    /**
     * Player ID associated with each connection. Connections without an entry
     * are spectators and receive the unfiltered game state.
     */
    private final Map<WebSocketConnection, Integer> connectionPlayers = new ConcurrentHashMap<>();

    public GameSession(UUID id) {
        this.id = id;
        this.state = new GameState(id, new ArrayList<>());
    }

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

    public void addConnection(WebSocketConnection connection) {
        addConnection(connection, null);
    }

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
