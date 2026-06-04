package fr.opensettlers.engine.state;

import fr.opensettlers.engine.state.utils.BuildingName;
import fr.opensettlers.engine.state.utils.ResourceType;
import lombok.*;
import fr.opensettlers.engine.state.utils.Coordinates;

import java.util.Map;
import java.util.UUID;

/**
 * Abstract base class for all buildings, defined by a position and an owning player.
 */
@Data
public abstract class Building {
    /** Unique identifier. */
    private final UUID id;

    /** Owning player ID. */
    private final int playerId;

    /** Position on the game map. */
    private final Coordinates position;

    /** Boolean defining if the building is destroyed. False means it is active. */
    private boolean destroyed = false;

    /** Flag attached to this building, or {@code null}. */
    private final Flag attachedFlag;

    /**
     * Initializes the building and attaches it bidirectionally to a new Flag.
     * 
     * @param id       unique identifier
     * @param playerId owning player ID
     * @param position map coordinates
     */
    public Building(UUID id, int playerId, Coordinates position) {
        this.id = id;
        this.playerId = playerId;
        this.position = position;
        this.attachedFlag = new Flag(UUID.randomUUID(), playerId, position);
        this.attachedFlag.setBuilding(this);
    }

    /**
     * Resource costs for constructing each building type.
     */
    public static final Map<BuildingName, Map<ResourceType, Integer>> buildingCosts = Map.ofEntries(
            Map.entry(BuildingName.WOODCUTTER, Map.of(ResourceType.PLANK, 2)),
            Map.entry(BuildingName.FORESTER, Map.of(ResourceType.PLANK, 2)),
            Map.entry(BuildingName.QUARRY, Map.of(ResourceType.PLANK, 2)),
            Map.entry(BuildingName.MINE, Map.of(ResourceType.PLANK, 4)),
            Map.entry(BuildingName.FISHING_HUT, Map.of(ResourceType.PLANK, 2)),
            Map.entry(BuildingName.WATER_WELL, Map.of(ResourceType.PLANK, 2)),
            Map.entry(BuildingName.BARRACKS, Map.of(ResourceType.PLANK, 2 )),
            Map.entry(BuildingName.GUARD_HOUSE, Map.of(ResourceType.PLANK, 2, ResourceType.STONE, 3)),

            Map.entry(BuildingName.BREWERY, Map.of(ResourceType.PLANK, 2, ResourceType.STONE, 2)),
            Map.entry(BuildingName.FOUNDRY, Map.of(ResourceType.PLANK, 2, ResourceType.STONE, 2)),
            Map.entry(BuildingName.MILL, Map.of(ResourceType.PLANK, 2, ResourceType.STONE, 2)),
            Map.entry(BuildingName.ARMORY, Map.of(ResourceType.PLANK, 2, ResourceType.STONE, 2)),
            Map.entry(BuildingName.BAKERY, Map.of(ResourceType.PLANK, 2, ResourceType.STONE, 2)),
            Map.entry(BuildingName.SAWMILL, Map.of(ResourceType.PLANK, 2, ResourceType.STONE, 2)),
            Map.entry(BuildingName.WATCH_TOWER, Map.of(ResourceType.PLANK, 3, ResourceType.STONE, 5)),
            Map.entry(BuildingName.WAREHOUSE, Map.of(
                    ResourceType.PLANK, 4,
                    ResourceType.STONE, 3
            )),

            Map.entry(BuildingName.FARM, Map.of(ResourceType.PLANK, 3, ResourceType.STONE,3)),
            Map.entry(BuildingName.CASTLE, Map.of(ResourceType.PLANK, 4, ResourceType.STONE, 7))
    );

    /**
     * Destroys the building, rendering it inactive.
     */
    public void destroy() {
        this.destroyed = true;
    }

    /**
     * Checks if the building has been destroyed.
     *
     * @return boolean indicating if the building is destroyed
     */
    public boolean isDestroyed() {
        return this.destroyed;
    }
}
