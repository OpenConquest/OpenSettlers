package fr.opensettlers.network;

import fr.opensettlers.services.GameEngineService;
import io.quarkus.websockets.next.OnMessage;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.inject.Inject;

import java.util.UUID;

/**
 * WebSocket endpoint for game clients.
 * Expected URL: ws://localhost:8080/game/{gameId}
 */
@WebSocket(path = "/game/{gameId}")
public class GameWebSocket {

    @Inject
    GameEngineService gameEngineService;

    @OnOpen
    public void onOpen(WebSocketConnection connection) {
        String gameId = connection.pathParam("gameId");
        System.out.println("New WebSocket connection to game: " + gameId);
        // Add player to broadcast list here
    }

    @OnMessage
    public void onMessage(GameMessage message, WebSocketConnection connection) {
        String gameIdStr = connection.pathParam("gameId");
        try {
            UUID gameId = UUID.fromString(gameIdStr);
            // Delegate message processing to the engine service
            gameEngineService.processMessage(gameId, message);
            
            // Broadcast: Use connection.broadcast() later to
            // send the updated GameState to all connected clients.
            
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid Game ID provided via WebSocket.");
        }
    }
}
