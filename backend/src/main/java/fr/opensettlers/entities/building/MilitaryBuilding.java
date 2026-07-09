package fr.opensettlers.entities.building;

import fr.opensettlers.utils.enums.BuildingName;
import fr.opensettlers.utils.Coordinates;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import fr.opensettlers.entities.unit.Soldier;

/**
 * Building that garrisons {@link Soldier}s and projects territorial control.
 * <p>
 * Examples: Barracks (2 slots), Guard House (3), Watch Tower (6), Fortress (9).
 * The territory radius and max capacity are set from {@link fr.opensettlers.utils.GameConfig}.
 */
@Getter
@Setter
public class MilitaryBuilding extends Building implements Garrisoned {
    /** Soldiers currently garrisoned in this building. */
    private final List<Soldier> soldiers = new ArrayList<>();

    /** Maximum number of soldiers this building can hold. */
    private int maxCapacity;

    /** Territory radius in hex distance (set from GameConfig). */
    private int territoryRadius;

    /** The building type name, for configuration lookup. */
    private final BuildingName buildingName;

    /** Whether the first soldier already arrived and triggered the territory claim. */
    private boolean territoryClaimed = false;

    /** Gold coins stored for soldier promotions (max set by GameConfig.coinCapacity). */
    private int storedCoins = 0;

    /**
     * Whether this building accepts gold-coin deliveries for promotions. When the
     * player switches coins off, the economy stops routing coins here, mirroring
     * the per-building coin toggle of the original game.
     */
    private boolean coinsAllowed = true;

    /** Ticks remaining before the next promotion can happen in this building. */
    private int promotionCooldown = 0;

    /**
     * Initializes a new MilitaryBuilding.
     *
     * @param playerId        owning player ID
     * @param position        coordinates on the map
     * @param buildingName    the type of military building
     * @param maxCapacity     max garrison size
     * @param territoryRadius hex radius of territory projection
     */
    public MilitaryBuilding(int playerId, Coordinates position,
                            BuildingName buildingName,
                            int maxCapacity, int territoryRadius) {
        super(playerId, position);
        this.buildingName = buildingName;
        this.maxCapacity = maxCapacity;
        this.territoryRadius = territoryRadius;
    }

    /**
     * Checks if there is room for another soldier.
     *
     * @return {@code true} if the garrison is not full
     */
    public boolean hasRoom() {
        return soldiers.size() < maxCapacity;
    }

    /**
     * Adds a soldier to the garrison if there is room.
     *
     * @param soldier the soldier to add
     * @return {@code true} if the soldier was added
     */
    public boolean addSoldier(Soldier soldier) {
        if (!hasRoom()) return false;
        soldiers.add(soldier);
        return true;
    }

    /**
     * Removes and returns the first soldier from the garrison (weakest/oldest).
     * Used when a defender needs to go fight an attacker.
     *
     * @return the removed soldier, or {@code null} if empty
     */
    public Soldier removeFirstSoldier() {
        if (soldiers.isEmpty()) return null;
        return soldiers.removeFirst();
    }

    /**
     * Removes a specific soldier from the garrison.
     *
     * @param soldier the soldier to remove
     */
    public void removeSoldier(Soldier soldier) {
        soldiers.remove(soldier);
    }

    /**
     * Returns the number of free slots in the garrison.
     *
     * @return available slots
     */
    public int freeSlots() {
        return maxCapacity - soldiers.size();
    }

    /**
     * Checks if the garrison is completely empty.
     *
     * @return {@code true} if no soldiers are garrisoned
     */
    public boolean isGarrisonEmpty() {
        return soldiers.isEmpty();
    }
}
