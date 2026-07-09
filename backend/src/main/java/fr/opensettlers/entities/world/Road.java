package fr.opensettlers.entities.world;

import fr.opensettlers.utils.Coordinates;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.UUID;
import fr.opensettlers.entities.unit.Donkey;
import fr.opensettlers.entities.unit.Carrier;

/**
 * A road connecting exactly two flags via a path of intermediate tile coordinates.
 * Each road has one {@link Carrier} assigned to transport resources between its endpoints.
 */
@Data
@EqualsAndHashCode(of = "id")
public class Road {
    /**
     * Unique identifier.
     */
    private final UUID id;

    /**
     * The flag at one end of the road.
     */
    private final Flag startFlag;

    /**
     * The flag at the other end of the road.
     */
    private final Flag endFlag;

    /**
     * Intermediate tile coordinates forming the road path (excluding the flag positions).
     * The path goes from {@code startFlag} to {@code endFlag}.
     */
    private final List<Coordinates> path;

    /**
     * The carrier assigned to this road, or {@code null} if none yet.
     */
    private Carrier carrier;

    /**
     * Road level: 1 = normal track, 2 = main road (after heavy use).
     * Level-2 roads can be assisted by a donkey.
     */
    private int level = 1;

    /**
     * Number of deliveries completed over this road, used to trigger the
     * upgrade to a level-2 main road.
     */
    private int transportCount = 0;

    /**
     * The donkey assisting the carrier on this road, or {@code null}.
     * Only level-2 roads get a donkey; it doubles the carrier speed.
     */
    private Donkey donkey;

    /**
     * Checks whether a donkey is actively assisting this road's carrier.
     *
     * @return {@code true} if a donkey has arrived and is helping
     */
    public boolean isDonkeyAssisted() {
        return donkey != null;
    }

    /**
     * Returns the length of the road in segments (number of steps to walk).
     * Used as weight for Dijkstra pathfinding.
     *
     * @return the road length (path tiles + 1)
     */
    public int getLength() {
        return path.size() + 1;
    }

    /**
     * Returns the flag at the other end of this road relative to the given flag.
     *
     * @param flag one of the two endpoint flags
     * @return the flag at the other end
     * @throws IllegalArgumentException if the given flag is not an endpoint of this road
     */
    public Flag getOtherFlag(Flag flag) {
        if (flag.getId().equals(startFlag.getId())) {
            return endFlag;
        } else if (flag.getId().equals(endFlag.getId())) {
            return startFlag;
        }
        throw new IllegalArgumentException("Flag is not an endpoint of this road");
    }

    /**
     * Checks whether a carrier has been assigned to this road.
     *
     * @return {@code true} if a carrier is assigned
     */
    public boolean hasCarrier() {
        return carrier != null;
    }
}
