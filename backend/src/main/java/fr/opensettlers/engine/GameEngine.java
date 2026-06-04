package fr.opensettlers.engine;

import fr.opensettlers.engine.systems.*;
import fr.opensettlers.network.GameWebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import lombok.Data;
import org.jboss.logging.Logger;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Implements the game loop. */
@Data
public class GameEngine implements Runnable {
    /**  The logger. */
    private static final Logger LOG = Logger.getLogger(GameWebSocket.class);

    /** Time interval in milliseconds separating each tick. */
    private static final int TICK_PERIOD_MS = 100;

    /** The current game session. */
    private final GameSession session;

    /** Secheduled executor used for ticks. */
    private final ScheduledExecutorService executor;

    /** Currently scheduled process. */
    private ScheduledFuture<?> scheduledTask;

    /** True if the game is currently running. */
    private volatile boolean running = false;

    /** System managing combat mechanisms. */
    private final CombatSystem combatSystem = new CombatSystem();

    /** System managing soldier movement mechanisms. */
    private final MovementSystem movementSystem = new MovementSystem();

    /** System managing resource production via buildings. */
    private final ProductionSystem productionSystem = new ProductionSystem();

    /** System managing resource transport mechanisms. */
    private final TransportSystem transportSystem = new TransportSystem();

    /** System managing physical worker movements and state transitions. */
    private final WorkerSystem workerSystem = new fr.opensettlers.engine.systems.WorkerSystem();

    /** System managing global resource allocation. */
    private final EconomySystem economySystem = new fr.opensettlers.engine.systems.EconomySystem();

    /** System managing construction site progress and worker assignments. */
    private final ConstructionSystem constructionSystem = new fr.opensettlers.engine.systems.ConstructionSystem();

    /** Starts the game and launches the game loop. */
    public synchronized void start() {
        if (running) return;
        running = true;
        scheduledTask = executor.scheduleAtFixedRate(this, 0, TICK_PERIOD_MS, TimeUnit.MILLISECONDS);
    }

    /** Stops the game loop. */
    public synchronized void stop() {
        if (!running) return;
        running = false;
        if (scheduledTask != null) {
            scheduledTask.cancel(true);
        }
    }

    /** The function periodically called by the scheduler. */
    @Override
    public void run() {
        try {
            tick();
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
    }

    /** Triggers the tick acrossd the entire game. */
    private void tick() {
        GameState state = session.getState();

        state.tick();

        combatSystem.process(state);
        movementSystem.process(state);
        workerSystem.process(state);
        economySystem.process(state);
        constructionSystem.process(state);
        productionSystem.process(state);
        transportSystem.process(state);

        broadcastState();
    }

    /** Sends the game state information to all connected clients. */
    private void broadcastState() {
        String stateJson = serializeState(session.getState());
        for (WebSocketConnection conn : session.getConnections()) {
            if (conn.isOpen()) {
                conn.sendTextAndAwait(stateJson);
            }
        }
    }
    
    private String serializeState(GameState state) {
        return "{\"tick\":" + state.getCurrentTick() + "}";
        // TODO add all information relevasnt to the clients
    }
}
