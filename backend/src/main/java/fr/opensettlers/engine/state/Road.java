package fr.opensettlers.engine.state;

import fr.opensettlers.engine.state.utils.Coordinates;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.UUID;

/**
 * A road connecting exactly two flags via a path of intermediate tile coordinates.
 * Each road has one {@link Carrier} assigned to transport resources between its endpoints.
 */
@Data
@EqualsAndHashCode(of = "id")
public class Road {
    /** Unique identifier. */
    private final UUID id;

    /** The flag at one end of the road. */
    private final Flag startFlag;

    /** The flag at the other end of the road. */
    private final Flag endFlag;

    /**
     * Intermediate tile coordinates forming the road path (excluding the flag positions).
     * The path goes from {@code startFlag} to {@code endFlag}.
     */
    private final List<Coordinates> path;

    /** The carrier assigned to this road, or {@code null} if none yet. */
    private Carrier carrier;

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
