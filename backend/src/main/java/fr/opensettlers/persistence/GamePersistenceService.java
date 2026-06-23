package fr.opensettlers.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.opensettlers.state.GameState;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Application service persisting and restoring games through
 * {@link GameSaveEntity}.
 *
 * <p>It bridges the in-memory simulation and the database: it serializes a live
 * {@link GameState} into a {@link GameSnapshot}, stores that snapshot as JSON,
 * and later rebuilds a playable state from a saved row. The simulation loop never
 * touches the database directly, so disk I/O never blocks a game tick.</p>
 */
@ApplicationScoped
public class GamePersistenceService {

    /** JSON mapper used to (de)serialize the snapshot payload stored on each row. */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Saves the durable state of a running game.
     *
     * @param state the live game state to persist
     * @param name  a human-readable name for the save
     * @return the database identifier of the new save
     */
    @Transactional
    public Long save(GameState state, String name) {
        GameSnapshot snapshot = GameSnapshotMapper.toSnapshot(state);
        GameSaveEntity entity = new GameSaveEntity();
        entity.name = name;
        entity.gameId = state.getGameId();
        entity.savedAt = Instant.now();
        entity.playerCount = state.getPlayerCount();
        entity.tick = state.getCurrentTick();
        entity.snapshotJson = writeJson(snapshot);
        entity.persist();
        return entity.id;
    }

    /**
     * Lists all saves, most recent first, without deserializing their snapshots.
     *
     * @return the saved-game metadata
     */
    public List<SaveSummary> list() {
        return GameSaveEntity.<GameSaveEntity>listAll().stream()
                .map(e -> new SaveSummary(e.id, e.name, e.gameId, e.savedAt, e.playerCount, e.tick))
                .sorted((a, b) -> b.savedAt().compareTo(a.savedAt()))
                .toList();
    }

    /**
     * Rebuilds a playable game state from a saved row.
     *
     * @param saveId the save identifier
     * @return the reconstructed game state under a fresh game ID, or {@code null}
     *         if no save with that ID exists
     */
    public GameState restore(Long saveId) {
        GameSaveEntity entity = GameSaveEntity.findById(saveId);
        if (entity == null) {
            return null;
        }
        GameSnapshot snapshot = readJson(entity.snapshotJson);
        return GameSnapshotMapper.toGameState(snapshot, UUID.randomUUID());
    }

    /**
     * Serializes a snapshot to JSON.
     *
     * @param snapshot the snapshot
     * @return the JSON string
     */
    private String writeJson(GameSnapshot snapshot) {
        try {
            return mapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize game snapshot", e);
        }
    }

    /**
     * Deserializes a snapshot from JSON.
     *
     * @param json the JSON string
     * @return the snapshot
     */
    private GameSnapshot readJson(String json) {
        try {
            return mapper.readValue(json, GameSnapshot.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize game snapshot", e);
        }
    }

    /**
     * Lightweight metadata about a saved game.
     *
     * @param id          database identifier
     * @param name        human-readable name
     * @param gameId      the game the save came from
     * @param savedAt     when the save was taken
     * @param playerCount number of players
     * @param tick        game tick at the save
     */
    public record SaveSummary(Long id, String name, UUID gameId, Instant savedAt,
                              int playerCount, long tick) {}
}
