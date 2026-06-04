package fr.opensettlers.engine.state;

import fr.opensettlers.engine.state.utils.Coordinates;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Building that recruits soldiers and expands territory. */
@Getter
public class MilitaryBuilding extends Building{
    /** Soldiers currently garrisoned in this building. */
    private final List<Soldier> soldiers = new ArrayList<>();

    /** Maximum number of soldiers this building can hold. */
    private int maxCapacity;

    /**
     * @param id Unique identifier of the building.
     * @param playerId Unique identifier of the owning player.
     * @param position Coordinates of the building.
     */
    public MilitaryBuilding(UUID id, int playerId, Coordinates position) {
        super(id, playerId, position);
    }
}
