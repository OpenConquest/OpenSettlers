package fr.opensettlers.systems.economy;

import fr.opensettlers.entities.building.Building;
import fr.opensettlers.entities.world.Flag;

/**
 * Represents available supply of a specific resource in the economy.
 */
public class Supply {
    /**
     * The building providing the supply, or {@code null} if the resource is sitting on a flag.
     */
    public final Building provider;

    /**
     * The flag where the supply is located.
     */
    public final Flag flag;

    /**
     * The quantity of resources available.
     */
    public int quantity;

    /**
     * The category source of the supply.
     */
    public final SupplySource source;

    /**
     * Constructs a new Supply.
     *
     * @param provider the building providing the supply (can be null)
     * @param flag     the flag associated with the supply
     * @param quantity the quantity available
     * @param source   the source category of the supply
     */
    public Supply(Building provider, Flag flag, int quantity, SupplySource source) {
        this.provider = provider;
        this.flag = flag;
        this.quantity = quantity;
        this.source = source;
    }
}
