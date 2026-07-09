package fr.opensettlers.entities.unit;

import fr.opensettlers.utils.enums.CarrierState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;

import java.util.UUID;
import fr.opensettlers.entities.world.Road;
import fr.opensettlers.entities.world.Flag;
import fr.opensettlers.entities.resource.ResourceStack;

/**
 * A carrier (settler) assigned to a single {@link Road}, transporting one resource at a time
 * between the road's two endpoint flags.
 *
 * <p>The carrier starts at the midpoint of its road when idle. When a resource needs to be
 * transported, it walks to the source flag, picks up the resource, and delivers it to the
 * other flag.</p>
 */
@Data
@EqualsAndHashCode(of = "id")
public class Carrier {
    /** Unique identifier. */
    private final UUID id;

    /** Owning player ID. */
    private final int playerId;

    /** The road this carrier is assigned to. */
    private final Road assignedRoad;

    /** Current state of the carrier. */
    @Setter
    private CarrierState state = CarrierState.IDLE;

    /** The resource currently being carried, or {@code null} if hands are empty. */
    private ResourceStack carriedResource;

    /**
     * Progress along the road: 0 means at {@code startFlag},
     * {@code road.getLength()} means at {@code endFlag}.
     */
    private int progress;

    /** The flag the carrier is currently walking toward, or {@code null} if idle. */
    @Setter
    private Flag targetFlag;

    /**
     * Creates a carrier at the midpoint of the given road.
     *
     * @param id       unique identifier
     * @param playerId owning player ID
     * @param road     the road to assign this carrier to
     */
    public Carrier(UUID id, int playerId, Road road) {
        this.id = id;
        this.playerId = playerId;
        this.assignedRoad = road;
        this.progress = road.getLength() / 2;
    }

    /**
     * Returns the flag the carrier is currently standing on, or {@code null} if between flags.
     *
     * @return the flag at the carrier's current position, or {@code null}
     */
    public Flag getCurrentFlag() {
        if (progress == 0) {
            return assignedRoad.getStartFlag();
        }
        if (progress == assignedRoad.getLength()) {
            return assignedRoad.getEndFlag();
        }
        return null;
    }

    /**
     * Checks whether the carrier is currently standing on one of its road's flags.
     *
     * @return {@code true} if the carrier is at a flag
     */
    public boolean isAtFlag() {
        return getCurrentFlag() != null;
    }

    /**
     * Checks whether the carrier has arrived at its target flag.
     *
     * @return {@code true} if standing on the target flag
     */
    public boolean isAtTargetFlag() {
        Flag current = getCurrentFlag();
        return current != null && targetFlag != null
                && current.getId().equals(targetFlag.getId());
    }

    /**
     * Moves one step toward the target flag along the road.
     * Does nothing if no target is set.
     */
    public void moveTowardTarget() {
        if (targetFlag == null) return;

        if (targetFlag.getId().equals(assignedRoad.getEndFlag().getId())) {
            progress = Math.min(progress + 1, assignedRoad.getLength());
        } else {
            progress = Math.max(progress - 1, 0);
        }
    }

    /**
     * Sends the carrier to pick up a resource from the given flag.
     *
     * @param pickupFlag the flag to walk toward
     */
    public void assignPickup(Flag pickupFlag) {
        this.targetFlag = pickupFlag;
        this.state = CarrierState.WALKING_TO_PICKUP;
    }

    /**
     * Picks up a resource and sets the carrier to deliver it to the given flag.
     *
     * @param resource     the resource to carry
     * @param deliveryFlag the flag to deliver to
     */
    public void pickupAndDeliver(ResourceStack resource, Flag deliveryFlag) {
        this.carriedResource = resource;
        this.targetFlag = deliveryFlag;
        this.state = CarrierState.WALKING_TO_DELIVER;
    }

    /**
     * Delivers the carried resource and resets the carrier to idle.
     *
     * @return the delivered resource, or {@code null} if nothing was carried
     */
    public ResourceStack deliver() {
        ResourceStack resource = this.carriedResource;
        this.carriedResource = null;
        this.state = CarrierState.IDLE;
        this.targetFlag = null;
        return resource;
    }

    /**
     * Checks whether the carrier is currently carrying a resource.
     *
     * @return {@code true} if carrying a resource
     */
    public boolean isCarrying() {
        return carriedResource != null;
    }
}
