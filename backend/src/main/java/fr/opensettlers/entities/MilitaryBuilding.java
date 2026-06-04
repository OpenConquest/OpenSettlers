package fr.opensettlers.entities;

import fr.opensettlers.utils.Coordinates;
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


    public MilitaryBuilding(UUID id, int playerId, Coordinates position) {
        super(id, playerId, position);
    }

    /** The function that is triggered every tick by the game loop. */
    @Override
    public void tick() {
        // TODO military building tick behaviour
    }
}
