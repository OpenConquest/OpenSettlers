package fr.opensettlers.entities;

import fr.opensettlers.utils.GameConfig;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.ResourceType;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

/**
 * Siege building that periodically throws stones at nearby enemy military
 * buildings. Each hit kills one garrisoned soldier; a building whose garrison
 * is wiped out burns down.
 * <p>
 * Modeled as a {@link ProductionBuilding} so the economy delivers stones to
 * its input slot and a helper occupies it, but it never "produces" anything:
 * firing is driven by {@link fr.opensettlers.systems.CatapultSystem}.
 */
@Getter
public class CatapultBuilding extends ProductionBuilding {

    /** Ticks remaining before the next shot. */
    @Setter
    private int fireCooldown = GameConfig.CATAPULT_COOLDOWN_TICKS;

    /**
     * Initializes a new CatapultBuilding with a single stone input slot.
     *
     * @param playerId owning player ID
     * @param position map coordinates
     */
    public CatapultBuilding(int playerId, Coordinates position) {
        super(playerId, position);
        this.inputSlots = new ArrayList<>();
        this.inputSlots.add(new ResourceSlot(ResourceType.STONE));
        this.outputSlot = null;
    }

    /**
     * Returns the slot holding the stones used as ammunition.
     *
     * @return the stone input slot
     */
    public ResourceSlot getStoneSlot() {
        return this.inputSlots.getFirst();
    }

    /** Catapults never produce goods; firing is handled by the CatapultSystem. */
    @Override
    public void produce() {
        // No-op: ammunition is consumed by the CatapultSystem when firing.
    }

    /**
     * Always {@code false}: the generic production loop must never fire the catapult.
     *
     * @return {@code false}
     */
    @Override
    public boolean canProduce() {
        return false;
    }
}
