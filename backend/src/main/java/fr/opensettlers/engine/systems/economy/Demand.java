package fr.opensettlers.engine.systems.economy;

import fr.opensettlers.engine.state.Building;
import fr.opensettlers.engine.state.Flag;
import fr.opensettlers.engine.state.utils.ResourceType;

/**
 * Represents a resource demand from a building or construction site in the economy.
 */
public class Demand {
    /**
     * The building requesting the resource.
     */
    public final Building building;

    /**
     * The flag attached to the requesting building.
     */
    public final Flag flag;

    /**
     * The type of resource requested.
     */
    public final ResourceType type;

    /**
     * The quantity of resources requested.
     */
    public int quantity;

    /**
     * Constructs a new Demand.
     *
     * @param building the building requesting the resource
     * @param flag     the attached flag where the resource should be routed
     * @param type     the type of resource requested
     * @param quantity the quantity needed
     */
    public Demand(Building building, Flag flag, ResourceType type, int quantity) {
        this.building = building;
        this.flag = flag;
        this.type = type;
        this.quantity = quantity;
    }
}
