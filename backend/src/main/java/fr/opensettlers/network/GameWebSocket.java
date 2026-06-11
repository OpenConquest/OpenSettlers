package fr.opensettlers.network;

import fr.opensettlers.engine.GameSession;
import fr.opensettlers.services.GameEngineService;
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

        session.addConnection(connection);
        LOG.infof("New WebSocket connection to game: %s", session.getId());

        if (session.getState().getMap() != null) {
            connection.sendTextAndAwait(GameStateSerializer.serializeMap(session.getState().getMap()));
        }
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
