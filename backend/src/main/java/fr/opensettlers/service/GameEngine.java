package fr.opensettlers.service;

import fr.opensettlers.service.commands.GameCommand;
import fr.opensettlers.state.GameSession;
import fr.opensettlers.state.GameState;
import fr.opensettlers.utils.GameConfig;
import fr.opensettlers.systems.*;
import fr.opensettlers.service.GameStateSerializer;
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
    private static final Logger LOG = Logger.getLogger(GameEngine.class);

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

    /** System managing catapult sieges. */
    private final CatapultSystem catapultSystem = new CatapultSystem();

    /** System managing garrison recruitment and territory expansion. */
    private final MilitarySystem militarySystem = new MilitarySystem();

    /** System managing soldier movement mechanisms. */
    private final MovementSystem movementSystem = new MovementSystem();

    /** System managing resource production via buildings. */
    private final ProductionSystem productionSystem = new ProductionSystem();

    /** System managing resource transport mechanisms. */
    private final TransportSystem transportSystem = new TransportSystem();

    /** System managing physical worker movements and state transitions. */
    private final WorkerSystem workerSystem = new fr.opensettlers.systems.WorkerSystem();

    /** System managing global resource allocation. */
    private final EconomySystem economySystem = new fr.opensettlers.systems.EconomySystem();

    /** System managing construction site progress and worker assignments. */
    private final ConstructionSystem constructionSystem = new fr.opensettlers.systems.ConstructionSystem();

    /** System managing road upgrades and donkey assistance. */
    private final DonkeySystem donkeySystem = new DonkeySystem();

    /** System managing geologists surveying the mountains. */
    private final GeologistSystem geologistSystem = new GeologistSystem();

    /** System managing sea expeditions launched from harbors. */
    private final NavalSystem navalSystem = new NavalSystem();

    /** System driving the built-in computer opponents. */
    private final AiSystem aiSystem = new AiSystem();

    /** System refreshing the per-player fog of war. */
    private final VisionSystem visionSystem = new VisionSystem();

    /** System detecting player elimination and deciding the winner. */
    private final VictorySystem victorySystem = new VictorySystem();

    /** Whether the terminal GAME_OVER message has already been broadcast. */
    private boolean gameOverBroadcast = false;

    /** Starts the game and launches the game loop. */
    public synchronized void start() {
        if (running) return;
        running = true;
        scheduledTask = executor.scheduleAtFixedRate(this, 0, GameConfig.TICK_PERIOD_MS, TimeUnit.MILLISECONDS);
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
            LOG.error("Game tick failed", e);
        }
    }

    /** Triggers the tick acrossd the entire game. */
    private void tick() {
        GameState state = session.getState();

        // Apply queued player commands on the loop thread before simulating
        GameCommand command;
        while ((command = session.getCommandQueue().poll()) != null) {
            try {
                command.execute(state);
            } catch (Exception e) {
                LOG.error("Failed to apply player command", e);
            }
        }

        state.tick();

        aiSystem.process(state);
        militarySystem.process(state);
        combatSystem.process(state);
        catapultSystem.process(state);
        movementSystem.process(state);
        geologistSystem.process(state);
        workerSystem.process(state);
        economySystem.process(state);
        constructionSystem.process(state);
        productionSystem.process(state);
        transportSystem.process(state);
        donkeySystem.process(state);
        navalSystem.process(state);
        visionSystem.process(state);
        victorySystem.process(state);

        broadcastState();

        if (state.isOver() && !gameOverBroadcast) {
            gameOverBroadcast = true;
            broadcastGameOver();
            stop();
        }
    }

    /** Broadcasts the terminal GAME_OVER message to every connected client. */
    private void broadcastGameOver() {
        String payload = GameStateSerializer.serializeGameOver(session.getState());
        for (WebSocketConnection conn : session.getConnections()) {
            if (conn.isOpen()) {
                conn.sendTextAndAwait(payload);
            }
        }
        LOG.infof("Game %s over — winner: %s", session.getId(),
                session.getState().getWinnerPlayerId());
    }

    /**
     * Sends the game state to all connected clients. Player-bound connections
     * receive a state filtered by their fog of war; spectators receive the
     * full state. Payloads are serialized once per distinct viewer.
     */
    private void broadcastState() {
        if (session.getConnections().isEmpty()) return;

        java.util.Map<Integer, String> payloadCache = new java.util.HashMap<>();
        String spectatorPayload = null;

        for (WebSocketConnection conn : session.getConnections()) {
            if (!conn.isOpen()) continue;

            Integer playerId = session.getPlayerFor(conn);
            String payload;
            if (playerId == null) {
                if (spectatorPayload == null) {
                    spectatorPayload = GameStateSerializer.serializeState(session.getState());
                }
                payload = spectatorPayload;
            } else {
                payload = payloadCache.computeIfAbsent(playerId,
                        pid -> GameStateSerializer.serializeState(session.getState(), pid));
            }
            conn.sendTextAndAwait(payload);
        }
    }
}
