package fr.opensettlers.engine.state;

import fr.opensettlers.engine.state.utils.BuildingName;
import fr.opensettlers.engine.state.utils.ResourceType;
import fr.opensettlers.engine.state.utils.Coordinates;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Building that extracts raw resources from a map resource node.
 */
@Getter
public class RawExtractor extends ProductionBuilding {

    /**
     * The type of resource this building extracts. Null for buildings like Forester that do not produce inventory.
     */
    private final ResourceType extractedResource;

    /**
     * The building type name, used to determine worker behavior in ProductionSystem.
     */
    private final BuildingName buildingName;

    /**
     * The tile currently being worked on (tree being cut, stone being mined, fish spot, etc.).
     * Null when idle/searching. Set by ProductionSystem when finding a target.
     */
    @Setter
    private MapTile targetWorkTile;

    /**
     * For FARM: list of field tiles managed by this farm (up to FARMER_MAX_FIELDS).
     */
    private final List<MapTile> managedFields = new ArrayList<>();

    /**
     * Initializes a new RawExtractor.
     *
     * @param playerId          owning player ID
     * @param position          map coordinates
     * @param extractedResource the resource it produces (null for Forester)
     * @param buildingName      the building type name
     */
    public RawExtractor(int playerId, Coordinates position, ResourceType extractedResource, BuildingName buildingName) {
        super(playerId, position);
        this.extractedResource = extractedResource;
        this.buildingName = buildingName;
        this.inputSlots = new ArrayList<>();
        if (extractedResource != null) {
            this.outputSlot = new ResourceSlot(extractedResource);
        }
    }

    /**
     * Extracts resources from the target work tile.
     * For extractors with a targetWorkTile, harvests the natural resource.
     * For extractors without a targetWorkTile (like WATER_WELL), simply adds to output.
     */
    @Override
    public void produce() {
        if (this.outputSlot != null) {
            if (this.targetWorkTile != null) {
                boolean harvested = this.targetWorkTile.harvestResource();
                if (harvested) {
                    this.outputSlot.addResource();
                }
                // If resource is depleted, clear the target
                if (this.targetWorkTile.getNaturalResource() == null
                        || this.targetWorkTile.getNaturalResource().isDepleted()) {
                    this.targetWorkTile = null;
                }
            } else {
                // Fallback for extractors that don't use targetWorkTile (e.g. WATER_WELL)
                this.outputSlot.addResource();
            }
        }
    }

    /**
     * Checks if extraction conditions are met.
     * For Forester (no output slot): returns false (handled directly by ProductionSystem).
     * For WATER_WELL/MINE: only checks output slot capacity.
     * For others: checks output slot has space AND a target tile is assigned.
     *
     * @return {@code true} if extraction conditions are met.
     */
    @Override
    public boolean canProduce() {
        // Forester (no output slot) is fully managed by ProductionSystem
        if (this.outputSlot == null) {
            return false;
        }
        // WATER_WELL and MINE don't need targetWorkTile
        if (this.buildingName == BuildingName.WATER_WELL || this.buildingName == BuildingName.MINE) {
            return this.outputSlot.getQuantity() < this.outputSlot.getMAX_PER_SLOT();
        }
        // All other extractors need a targetWorkTile
        return this.outputSlot.getQuantity() < this.outputSlot.getMAX_PER_SLOT()
                && this.targetWorkTile != null;
    }
}
