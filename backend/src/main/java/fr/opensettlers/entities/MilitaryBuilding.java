package fr.opensettlers.entities;

import fr.opensettlers.utils.Coordinates;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/**
 * The MilitaryBuilding is a building that can recruit soldiers and expand the player's territory.
 */
@Getter
public class
MilitaryBuilding extends Building {
    /**
     * List of soldiers currently recruited by this building. The soldiers are represented as a list of Soldier objects, which contain information about the soldier's type, health, attack power, etc.
     */
    private List<Soldier> soldiers;

    /**
     * The maximum number of soldiers that can be recruited by this building. This is determined by the type of the building and can be increased by upgrading the building.
     */
    private int maxCapacity;


    public MilitaryBuilding(UUID id, int playerId, Coordinates position) {
        super(id, playerId, position, new Flag(UUID.randomUUID(), playerId, position));
    }

    @Override
    public void tick() {
        // TODO military building tick behaviour
    }
}
