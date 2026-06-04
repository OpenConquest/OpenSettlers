package fr.opensettlers.network;

import fr.opensettlers.services.GameEngineService;
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
     * Handles the opening of a new WebSocket connection.
     *
     * @param connection the WebSocket connection that was opened
     */
    @OnOpen
    public void onOpen(WebSocketConnection connection) {
        String gameId = connection.pathParam("gameId");
        LOG.infof("New WebSocket connection to game: %s", gameId);
        // Add player to broadcast list here
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
            
            // Broadcast: Use connection.broadcast() later to
            // send the updated GameState to all connected clients.
            
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid Game ID provided via WebSocket.");
        }
    }
}
