package fr.opensettlers.systems;

import fr.opensettlers.utils.GameConfig;
import fr.opensettlers.state.GameState;
import fr.opensettlers.entities.Building;
import fr.opensettlers.entities.CatapultBuilding;
import fr.opensettlers.entities.MilitaryBuilding;
import fr.opensettlers.entities.Soldier;
import fr.opensettlers.entities.Worker;
import fr.opensettlers.utils.WorkerState;

import java.util.Random;

/**
 * System driving catapults: every {@link GameConfig#CATAPULT_COOLDOWN_TICKS}
 * ticks, a manned catapult with stones in stock throws one at the nearest
 * enemy military building within {@link GameConfig#CATAPULT_RANGE}. A hit
 * ({@link GameConfig#CATAPULT_HIT_CHANCE}) kills one garrisoned soldier; a
 * building left without a garrison burns down and the territory is
 * recalculated.
 */
public class CatapultSystem implements ISystem {

    /** Random source for projectile hit rolls (injectable for tests). */
    private final Random random;

    /** Creates the system with a default random source. */
    public CatapultSystem() {
        this(new Random());
    }

    /**
     * Creates the system with a custom random source.
     *
     * @param random the random source used for hit rolls
     */
    public CatapultSystem(Random random) {
        this.random = random;
    }

    /**
     * Processes all catapults for the current tick.
     *
     * @param gameState the active game session state
     */
    @Override
    public void process(GameState gameState) {
        for (Building b : gameState.getBuildings()) {
            if (!(b instanceof CatapultBuilding catapult) || catapult.isDestroyed()) {
                continue;
            }
            processCatapult(gameState, catapult);
        }
    }

    /**
     * Counts down the reload timer and fires the catapult when ready.
     *
     * @param state    the current game state
     * @param catapult the catapult to process
     */
    private void processCatapult(GameState state, CatapultBuilding catapult) {
        Worker occupant = catapult.getOccupant();
        if (occupant == null
                || (occupant.getState() != WorkerState.WORKING && occupant.getState() != WorkerState.WAITING)) {
            return; // Unmanned catapults do not fire
        }

        if (catapult.getFireCooldown() > 0) {
            catapult.setFireCooldown(catapult.getFireCooldown() - 1);
            return;
        }
        if (catapult.getStoneSlot().getQuantity() <= 0) {
            return; // Out of ammunition; keep the cooldown at zero until restocked
        }

        MilitaryBuilding target = findNearestEnemyMilitary(state, catapult);
        if (target == null) {
            return; // No target in range; don't waste stones
        }

        catapult.getStoneSlot().removeResource();
        catapult.setFireCooldown(GameConfig.CATAPULT_COOLDOWN_TICKS);

        if (random.nextDouble() < GameConfig.CATAPULT_HIT_CHANCE) {
            applyHit(state, target);
        }
    }

    /**
     * Applies a projectile hit: kills one garrisoned soldier, and burns the
     * building down if its garrison is (or becomes) empty.
     *
     * @param state  the current game state
     * @param target the military building that was hit
     */
    private void applyHit(GameState state, MilitaryBuilding target) {
        Soldier victim = target.removeFirstSoldier();
        if (victim != null) {
            victim.setHealth(0);
        }
        if (target.isGarrisonEmpty()) {
            target.destroy();
            if (target.getAttachedFlag() != null) {
                target.getAttachedFlag().destroy();
            }
            state.getTerritoryManager().recalculate(state);
        }
    }

    /**
     * Finds the nearest enemy military building within catapult range.
     *
     * @param state    the current game state
     * @param catapult the firing catapult
     * @return the nearest enemy military building, or {@code null} if none in range
     */
    private MilitaryBuilding findNearestEnemyMilitary(GameState state, CatapultBuilding catapult) {
        MilitaryBuilding nearest = null;
        int minDist = Integer.MAX_VALUE;
        for (Building b : state.getBuildings()) {
            if (!(b instanceof MilitaryBuilding mb) || mb.isDestroyed()
                    || mb.getPlayerId() == catapult.getPlayerId()) {
                continue;
            }
            int dist = mb.getPosition().distanceTo(catapult.getPosition());
            if (dist <= GameConfig.CATAPULT_RANGE && dist < minDist) {
                minDist = dist;
                nearest = mb;
            }
        }
        return nearest;
    }
}
