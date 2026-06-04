package fr.opensettlers.engine;

import fr.opensettlers.engine.commands.GameCommand;
import io.quarkus.websockets.next.WebSocketConnection;
import lombok.Getter;

import java.util.ArrayList;
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
    
    public GameSession(UUID id) {
        this.id = id;
        this.state = new GameState(id, new ArrayList<>());
    }
    
    public void queueCommand(GameCommand command) {
        commandQueue.add(command);
    }
    
    public void addConnection(WebSocketConnection connection) {
        connections.add(connection);
    }
    
    public void removeConnection(WebSocketConnection connection) {
        connections.remove(connection);
    }
}
