package fr.opensettlers.engine.state;

import fr.opensettlers.engine.state.utils.BuildingName;
import fr.opensettlers.engine.state.utils.Coordinates;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Building that garrisons soldiers and expands the owning player's territory.
 */
@Getter
public class MilitaryBuilding extends Building {
    /**
     * Soldiers currently garrisoned in this building.
     */
    private final List<Soldier> soldiers = new ArrayList<>();

    /**
     * Maximum number of soldiers this building can hold.
     */
    private final int maxCapacity;

    /**
     * Radius of territory claimed around this building once occupied.
     */
    private final int territoryRadius;

    /**
     * Whether this building already claimed its territory (done when the first soldier arrives).
     */
    private boolean territoryClaimed = false;

    /**
     * Initializes a new MilitaryBuilding with capacity and territory radius based on its type.
     *
     * @param playerId Unique identifier of the owning player.
     * @param position Coordinates of the building.
     * @param type     The military building type (barracks, guard house, watch tower, castle).
     */
    public MilitaryBuilding(int playerId, Coordinates position, BuildingName type) {
        super(playerId, position);
        this.setName(type);
        this.maxCapacity = switch (type) {
            case BARRACKS -> 2;
            case GUARD_HOUSE -> 3;
            case WATCH_TOWER -> 6;
            case CASTLE -> 9;
            default -> 2;
        };
        this.territoryRadius = switch (type) {
            case BARRACKS -> 4;
            case GUARD_HOUSE -> 5;
            case WATCH_TOWER -> 7;
            case CASTLE -> 9;
            default -> 4;
        };
    }

    /**
     * Garrisons a soldier in this building if there is room.
     *
     * @param soldier the soldier to garrison
     * @return {@code true} if the soldier was garrisoned
     */
    public boolean garrison(Soldier soldier) {
        if (soldiers.size() >= maxCapacity) {
            return false;
        }
        soldiers.add(soldier);
        return true;
    }

    /**
     * Removes a soldier from the garrison (to defend or attack).
     *
     * @return the removed soldier, or {@code null} if the garrison is empty
     */
    public Soldier releaseSoldier() {
        if (soldiers.isEmpty()) {
            return null;
        }
        return soldiers.remove(soldiers.size() - 1);
    }

    /**
     * Checks whether the garrison has free slots.
     *
     * @return {@code true} if at least one slot is free
     */
    public boolean hasFreeSlot() {
        return soldiers.size() < maxCapacity;
    }

    /**
     * Marks the territory around this building as claimed.
     */
    public void markTerritoryClaimed() {
        this.territoryClaimed = true;
    }
}
