package fr.opensettlers.entities.world;

import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.enums.ResourceType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import fr.opensettlers.entities.resource.ResourceStack;
import fr.opensettlers.entities.building.Building;

/**
 * A flag that holds resources for transport between buildings.
 *
 * <p>Each flag can hold up to {@value MAX_CAPACITY} resources in its slots.
 * Each slot is a {@link ResourceStack} containing the resource type and
 * the UUID of the final destination flag for routing.</p>
 *
 * <p>Roads are connected to flags, and carriers transport resources
 * between adjacent flags along these roads.</p>
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode(of = "id")
public class Flag {
    /** Unique identifier. */
    private final UUID id;

    /** Owning player ID. Mutable to support capture by another player. */
    private int playerId;

    /** Position on the game map. */
    private final Coordinates coordinates;

    /** Boolean defining if the building is destroyed. False means it is active. */
    private boolean destroyed = false;

    /**
     * Resources currently held on this flag, each with routing info.
     * Maximum size is {@value MAX_CAPACITY}.
     */
    private final List<ResourceStack> resourceSlots = new ArrayList<>();

    /** Roads connected to this flag. */
    private final List<Road> connectedRoads = new ArrayList<>();

    /** Maximum total resource capacity. */
    private static final int MAX_CAPACITY = 5;

    /** The building attached to this flag, if any. */
    private Building building;

    /**
     * Creates a new flag.
     *
     * @param id          unique identifier
     * @param playerId    owning player ID
     * @param coordinates position on the game map
     */
    public Flag(UUID id, int playerId, Coordinates coordinates) {
        this.id = id;
        this.playerId = playerId;
        this.coordinates = coordinates;
    }

    /**
     * Adds one resource with a specific target destination.
     *
     * @param resourceType the resource type to add
     * @param targetFlagId the final destination flag ID (may be {@code null})
     * @throws IllegalStateException if the flag is full
     */
    public void addResource(ResourceType resourceType, UUID targetFlagId) {
        if (resourceSlots.size() >= MAX_CAPACITY) {
            throw new IllegalStateException("Flag is at full capacity");
        }
        resourceSlots.add(new ResourceStack(resourceType, targetFlagId));
    }

    /**
     * Adds one unit of the given resource without a specific destination.
     * The destination will be assigned later by the transport system.
     *
     * @param resourceType the resource type to add
     * @throws IllegalStateException if the flag is full
     */
    public void addResource(ResourceType resourceType) {
        addResource(resourceType, null);
    }

    /**
     * Removes and returns the first resource of the given type.
     *
     * @param resourceType the resource type to remove
     * @return the removed resource stack
     * @throws IllegalArgumentException if no resource of that type is available
     */
    public ResourceStack popResource(ResourceType resourceType) {
        for (int i = 0; i < resourceSlots.size(); i++) {
            if (resourceSlots.get(i).getType() == resourceType) {
                return resourceSlots.remove(i);
            }
        }
        throw new IllegalArgumentException("No resources of this type to pop");
    }

    /**
     * Removes and returns the specified resource stack from this flag.
     *
     * @param resource the exact resource stack to remove
     * @return the removed resource stack
     * @throws IllegalArgumentException if the resource is not on this flag
     */
    public ResourceStack popResource(ResourceStack resource) {
        if (!resourceSlots.remove(resource)) {
            throw new IllegalArgumentException("Resource not found on this flag");
        }
        return resource;
    }

    /**
     * Checks whether the flag is at full capacity.
     *
     * @return {@code true} if the flag holds {@value MAX_CAPACITY} resources
     */
    public boolean isFull() {
        return resourceSlots.size() >= MAX_CAPACITY;
    }

    /**
     * Checks whether the flag holds no resources.
     *
     * @return {@code true} if the flag is empty
     */
    public boolean isEmpty() {
        return resourceSlots.isEmpty();
    }

    /**
     * Checks whether the flag has any resource without an assigned destination.
     *
     * @return {@code true} if at least one resource has a {@code null} target
     */
    public boolean hasUnroutedResources() {
        return resourceSlots.stream().anyMatch(rs -> rs.getTargetFlagId() == null);
    }

    /**
     * Returns the number of resources of the given type on this flag.
     *
     * @param resourceType the resource type to count
     * @return the count
     */
    public int getResourceCount(ResourceType resourceType) {
        return (int) resourceSlots.stream()
                .filter(rs -> rs.getType() == resourceType)
                .count();
    }

    /**
     * Returns the total number of resources on this flag.
     *
     * @return the total resource count
     */
    public int getTotalResourceCount() {
        return resourceSlots.size();
    }

    /**
     * Connects a road to this flag.
     *
     * @param road the road to connect
     */
    public void connectRoad(Road road) {
        if (!connectedRoads.contains(road)) {
            connectedRoads.add(road);
        }
    }

    /**
     * Disconnects a road from this flag.
     *
     * @param road the road to disconnect
     */
    public void disconnectRoad(Road road) {
        connectedRoads.remove(road);
    }

    /**
     * Destroys the flag, rendering it inactive.
     */
    public void destroy() {
        this.destroyed = true;
    }

    /**
     * Checks if the flag has been destroyed.
     *
     * @return {@code true} if destroyed
     */
    public boolean isDestroyed() {
        return this.destroyed;
    }
}
