package fr.opensettlers.entities.building;

import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.GameConfig;
import fr.opensettlers.utils.enums.ResourceType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import fr.opensettlers.entities.unit.Soldier;

/**
 * The headquarters: the player's main storage hub, which also holds a soldier
 * garrison and defends itself when attacked, as in The Settlers II. Losing it
 * without another warehouse means elimination.
 */
@Getter
public class HeadquartersBuilding extends StorageBuilding implements Garrisoned {

    /** Soldiers garrisoned in the headquarters, defending it when attacked. */
    private final List<Soldier> soldiers = new ArrayList<>();

    /**
     * Initializes the headquarters with its starting stock.
     *
     * @param playerId        owning player ID
     * @param position        map coordinates
     * @param storedResources initial resource stock
     */
    public HeadquartersBuilding(int playerId, Coordinates position, Map<ResourceType, Integer> storedResources) {
        super(playerId, position, storedResources);
    }

    /**
     * Returns the garrison capacity of the headquarters.
     *
     * @return the maximum number of defending soldiers
     */
    @Override
    public int getMaxCapacity() {
        return GameConfig.HEADQUARTERS_CAPACITY;
    }
}
