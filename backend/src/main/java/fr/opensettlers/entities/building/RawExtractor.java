package fr.opensettlers.entities.building;

import fr.opensettlers.utils.enums.BuildingName;
import fr.opensettlers.utils.enums.ResourceType;
import fr.opensettlers.utils.Coordinates;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import fr.opensettlers.entities.world.MapTile;
import fr.opensettlers.entities.resource.ResourceSlot;

/**
 * Building that extracts raw resources from a map resource node.
 * Mines additionally consume one unit of food (fish, bread or meat) per
 * extraction, as in The Settlers II.
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
     * Initializes a new RawExtractor. Mines receive one input slot per food
     * type so that the economy delivers them provisions.
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
        if (buildingName.isMine()) {
            for (ResourceType type : ResourceType.values()) {
                if (type.isFood()) {
                    this.inputSlots.add(new ResourceSlot(type));
                }
            }
        }
        if (extractedResource != null) {
            this.outputSlot = new ResourceSlot(extractedResource);
        }
    }

    /**
     * Extracts resources from the target work tile.
     * For extractors with a targetWorkTile, harvests the natural resource.
     * For extractors without a targetWorkTile (like WATER_WELL), simply adds to output.
     * Mines eat one unit of food per extraction.
     */
    @Override
    public void produce() {
        if (this.outputSlot == null) {
            return;
        }
        if (this.buildingName.isMine() && !consumeFood()) {
            return;
        }
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

    /**
     * Checks if extraction conditions are met.
     * For Forester (no output slot): returns false (handled directly by ProductionSystem).
     * For WATER_WELL: only checks output slot capacity (water is infinite).
     * For MINE: needs space, a non-depleted deposit assigned, and food in stock.
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
        boolean outputHasSpace = this.outputSlot.getQuantity() < this.outputSlot.getMAX_PER_SLOT();
        // WATER_WELL doesn't need a targetWorkTile (water is infinite)
        if (this.buildingName == BuildingName.WATER_WELL) {
            return outputHasSpace;
        }
        // Mines need an assigned deposit and food to feed the miner
        if (this.buildingName.isMine()) {
            return outputHasSpace && this.targetWorkTile != null && hasFood();
        }
        // All other extractors need a targetWorkTile
        return outputHasSpace && this.targetWorkTile != null;
    }

    /**
     * Checks whether at least one unit of food is available in the input slots.
     *
     * @return {@code true} if the mine has provisions
     */
    public boolean hasFood() {
        for (ResourceSlot slot : this.inputSlots) {
            if (slot.getType().isFood() && slot.getQuantity() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Consumes one unit of food from the first non-empty food slot.
     *
     * @return {@code true} if a unit was consumed
     */
    private boolean consumeFood() {
        for (ResourceSlot slot : this.inputSlots) {
            if (slot.getType().isFood() && slot.removeResource()) {
                return true;
            }
        }
        return false;
    }
}
