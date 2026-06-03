package fr.opensettlers.entities;

import fr.opensettlers.utils.BuildingName;
import fr.opensettlers.utils.ResourceType;
import lombok.*;
import fr.opensettlers.utils.Coordinates;

import java.util.Map;
import java.util.UUID;

/**
 * Base class for all buildings in the game.
 * Each building has a position (x, y) and belongs to a player (playerId).
 * Specific types of buildings will extend this class and add their own properties and behaviors.
 */
@Data
public abstract class Building {
    /**
     * Unique identifier for the building.
     */
    private final UUID id;

    /**
     * Identifier of the player who owns the building.
     */
    private final int playerId;
    /**
     * Coordinates of the building on the game map. The coordinates are represented as a pair of integers (x, y).
     */
    private final Coordinates position;

    /**
     * Flag currently attached to the building, if any. A building can have at most one flag attached at a time.
     */
    private final Flag attachedFlag;

    /**
     * A static map that defines the resource costs for constructing each type of building. The key is the BuildingName enum value, and the value is another map that specifies the required resources and their quantities for that building.
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
     * Boolean defining if the building is destroyed. False means it is active.
     */
    private boolean destroyed = false;

    /**
     * Destroys the building, rendering it inactive.
     */
    public void destroy() {
        this.destroyed = true;
    }

    /**
     * Checks if the building has been destroyed.
     * @return boolean
     */
    public boolean isDestroyed() {
        return this.destroyed;
    }

    /**
     * The function that is triggered every tick by the game loop.
     */
    public abstract void tick();
}
