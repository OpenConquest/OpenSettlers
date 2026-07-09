package fr.opensettlers.entities.building;

import java.util.List;
import fr.opensettlers.entities.unit.Soldier;

/**
 * A building holding a soldier garrison able to defend it: military buildings
 * and the headquarters. The garrison staffing ({@code MilitarySystem}) and the
 * defender sorties ({@code CombatSystem}) operate on this contract.
 */
public interface Garrisoned {

    /**
     * Returns the soldiers currently garrisoned in this building.
     *
     * @return the mutable garrison list
     */
    List<Soldier> getSoldiers();

    /**
     * Returns the maximum number of soldiers this building can hold.
     *
     * @return the garrison capacity
     */
    int getMaxCapacity();

    /**
     * Adds a soldier to the garrison if there is room.
     *
     * @param soldier the soldier to add
     * @return {@code true} if the soldier was added
     */
    default boolean addSoldier(Soldier soldier) {
        if (getSoldiers().size() >= getMaxCapacity()) {
            return false;
        }
        getSoldiers().add(soldier);
        return true;
    }

    /**
     * Removes and returns the first soldier from the garrison (weakest/oldest).
     *
     * @return the removed soldier, or {@code null} if empty
     */
    default Soldier removeFirstSoldier() {
        if (getSoldiers().isEmpty()) {
            return null;
        }
        return getSoldiers().removeFirst();
    }

    /**
     * Checks if the garrison is completely empty.
     *
     * @return {@code true} if no soldiers are garrisoned
     */
    default boolean isGarrisonEmpty() {
        return getSoldiers().isEmpty();
    }

    /**
     * Checks if there is room for another soldier.
     *
     * @return {@code true} if the garrison is not full
     */
    default boolean hasRoom() {
        return getSoldiers().size() < getMaxCapacity();
    }
}
