package fr.opensettlers.entities.building;

import fr.opensettlers.utils.enums.BuildingName;
import fr.opensettlers.utils.enums.ResourceType;
import lombok.*;
import fr.opensettlers.utils.Coordinates;

import java.util.Map;
import java.util.UUID;
import fr.opensettlers.entities.world.Flag;

/**
 * Abstract base class for all buildings, defined by a position and an owning player.
 */
@Data
public abstract class Building {
    /** Unique identifier. */
    private final UUID id;

    /** Owning player ID. Mutable to support capture by another player. */
    private int playerId;

    /** Position on the game map. */
    private final Coordinates position;

    /** Boolean defining if the building is destroyed. False means it is active. */
    private boolean destroyed = false;

    /** The building type name, set at creation by the factory. */
    @Setter
    private BuildingName name;

    /** Flag attached to this building, or {@code null}. */
    @Setter
    private Flag attachedFlag;

    /**
     * Initializes the building and attaches it bidirectionally to a new Flag.
     *
     * @param playerId owning player ID
     * @param position map coordinates
     */
    public Building(int playerId, Coordinates position) {
        this.id = UUID.randomUUID();
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
            Map.entry(BuildingName.GRANITE_MINE, Map.of(ResourceType.PLANK, 4)),
            Map.entry(BuildingName.COAL_MINE, Map.of(ResourceType.PLANK, 4)),
            Map.entry(BuildingName.IRON_MINE, Map.of(ResourceType.PLANK, 4)),
            Map.entry(BuildingName.GOLD_MINE, Map.of(ResourceType.PLANK, 4)),
            Map.entry(BuildingName.LOOKOUT_TOWER, Map.of(ResourceType.PLANK, 4)),
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

            Map.entry(BuildingName.HUNTERS_HUT, Map.of(ResourceType.PLANK, 2)),
            Map.entry(BuildingName.PIG_FARM, Map.of(ResourceType.PLANK, 3, ResourceType.STONE, 3)),
            Map.entry(BuildingName.SLAUGHTERHOUSE, Map.of(ResourceType.PLANK, 2, ResourceType.STONE, 2)),
            Map.entry(BuildingName.DONKEY_BREEDER, Map.of(ResourceType.PLANK, 3, ResourceType.STONE, 3)),
            Map.entry(BuildingName.MINT, Map.of(ResourceType.PLANK, 2, ResourceType.STONE, 2)),
            Map.entry(BuildingName.METALWORKS, Map.of(ResourceType.PLANK, 2, ResourceType.STONE, 2)),
            Map.entry(BuildingName.CATAPULT, Map.of(ResourceType.PLANK, 4, ResourceType.STONE, 2)),
            Map.entry(BuildingName.FORTRESS, Map.of(ResourceType.PLANK, 4, ResourceType.STONE, 7)),
            Map.entry(BuildingName.HARBOR, Map.of(ResourceType.PLANK, 4, ResourceType.STONE, 6)),
            Map.entry(BuildingName.SHIPYARD, Map.of(ResourceType.PLANK, 4, ResourceType.STONE, 3))
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
