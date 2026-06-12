package fr.opensettlers.entities;

import fr.opensettlers.utils.BuildingName;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.ResourceType;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a building under construction. Needs materials, a terrassier to flatten the ground,
 * and a builder to perform masonry.
 */
@Getter
@Setter
public class ConstructionSite extends Building {
    /**
     * The final building type that will be created upon completion.
     */
    private final BuildingName targetBuildingType;

    /**
     * Map of total materials required for construction.
     */
    private final Map<ResourceType, Integer> requiredMaterials;

    /**
     * Map of materials that have already been delivered to the site.
     */
    private final Map<ResourceType, Integer> deliveredMaterials = new HashMap<>();

    /**
     * Progress of groundwork flattening (0 to 100).
     */
    private int groundworkProgress = 0;

    /**
     * Progress of construction masonry (0 to 100).
     */
    private int buildingProgress = 0;

    /**
     * Worker assigned to perform groundwork/leveling.
     */
    private Worker assignedTerrassier;

    /**
     * Worker assigned to perform building/masonry.
     */
    private Worker assignedBuilder;

    /**
     * Initializes a new ConstructionSite.
     *
     * @param playerId           owning player ID
     * @param position           map position
     * @param targetBuildingType target building type to construct
     */
    public ConstructionSite(int playerId, Coordinates position, BuildingName targetBuildingType) {
        super(playerId, position);
        this.targetBuildingType = targetBuildingType;
        this.setName(targetBuildingType);
        this.requiredMaterials = new HashMap<>(Building.buildingCosts.getOrDefault(targetBuildingType, Map.of()));
        for (ResourceType rt : requiredMaterials.keySet()) {
            deliveredMaterials.put(rt, 0);
        }
    }
}
