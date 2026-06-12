package fr.opensettlers.controller;

import fr.opensettlers.service.GameStateSerializer;
import fr.opensettlers.state.GameSession;
import fr.opensettlers.service.GameEngineService;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.inject.Inject;

import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * WebSocket endpoint for game clients.
 * Expected URL: ws://localhost:8080/game/{gameId}
 *
 */
@WebSocket(path = "/game/{gameId}")
public class GameWebSocket {

    private static final Logger LOG = Logger.getLogger(GameWebSocket.class);

    @Inject
    GameEngineService gameEngineService;

    /**
     * Handles the opening of a new WebSocket connection: registers it on the
     * game session for state broadcasts and sends the static map once.
     *
     * @param connection the WebSocket connection that was opened
     */
    @OnOpen
    public void onOpen(WebSocketConnection connection) {
        GameSession session = resolveSession(connection);
        if (session == null) {
            LOG.warnf("Connection refused: unknown game %s", connection.pathParam("gameId"));
            connection.closeAndAwait();
            return;
        }

        Integer playerId = resolvePlayerId(connection);
        session.addConnection(connection, playerId);
        LOG.infof("New WebSocket connection to game %s (player: %s)",
                session.getId(), playerId != null ? playerId : "spectator");

        if (!session.getState().getMapTiles().isEmpty()) {
            connection.sendTextAndAwait(GameStateSerializer.serializeMap(session.getState()));
        }
    }

    /**
     * Extracts the player ID from the connection's query string
     * (e.g. {@code ws://host/game/{gameId}?playerId=0}).
     *
     * @param connection the WebSocket connection
     * @return the player ID, or {@code null} if absent or malformed (spectator)
     */
    private Integer resolvePlayerId(WebSocketConnection connection) {
        String query = connection.handshakeRequest().query();
        if (query == null || query.isEmpty()) {
            return null;
        }
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && kv[0].equals("playerId")) {
                try {
                    return Integer.parseInt(kv[1]);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Handles a client disconnecting: removes it from the broadcast list.
     *
     * @param connection the WebSocket connection that was closed
     */
    @OnClose
    public void onClose(WebSocketConnection connection) {
        GameSession session = resolveSession(connection);
        if (session != null) {
            session.removeConnection(connection);
            LOG.infof("Connection closed for game: %s", session.getId());
        }
    }

    /**
     * Handles incoming text messages from the WebSocket connection.
     *
     * @param message the parsed game message received from the client
     * @param connection the WebSocket connection the message was received on
     */
    @OnTextMessage
    public void onMessage(GameMessage message, WebSocketConnection connection) {
        String gameIdStr = connection.pathParam("gameId");
        try {
            UUID gameId = UUID.fromString(gameIdStr);
            // Delegate message processing to the engine service
            gameEngineService.processMessage(gameId, message);
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid Game ID provided via WebSocket.");
        }
    }

    /**
     * Resolves the game session targeted by a connection's path parameter.
     *
     * @param connection the WebSocket connection
     * @return the session, or {@code null} if the ID is invalid or unknown
     */
    private GameSession resolveSession(WebSocketConnection connection) {
        try {
            return gameEngineService.getSession(UUID.fromString(connection.pathParam("gameId")));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
