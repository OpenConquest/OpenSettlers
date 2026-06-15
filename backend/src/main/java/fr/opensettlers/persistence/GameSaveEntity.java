package fr.opensettlers.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistent record of a saved game, stored in PostgreSQL through Hibernate
 * Panache (active-record pattern).
 *
 * <p>The bulk of the data is the JSON-serialized {@link GameSnapshot} held in
 * {@link #snapshotJson}; the other columns are lightweight metadata so saves can
 * be listed without deserializing every blob.</p>
 */
@Entity
@Table(name = "game_save")
public class GameSaveEntity extends PanacheEntity {

    /** Human-readable name chosen when saving. */
    @Column(nullable = false)
    public String name;

    /** Identifier of the game this save was taken from. */
    @Column(nullable = false)
    public UUID gameId;

    /** Instant the save was created. */
    @Column(nullable = false)
    public Instant savedAt;

    /** Number of players in the saved game. */
    public int playerCount;

    /** Game tick at the moment of the save. */
    public long tick;

    /** The full {@link GameSnapshot} serialized as JSON. */
    @Column(columnDefinition = "text", nullable = false)
    public String snapshotJson;
}
