package fr.opensettlers.systems;

import fr.opensettlers.utils.GameConfig;
import fr.opensettlers.state.GameState;
import fr.opensettlers.entities.Building;
import fr.opensettlers.entities.MilitaryBuilding;
import fr.opensettlers.entities.Soldier;
import fr.opensettlers.utils.SoldierState;

import java.util.ArrayList;
import java.util.List;

/**
 * System moving soldier units across the map toward their target buildings,
 * one hex step every {@link GameConfig#SOLDIER_MOVE_TICKS} ticks. Follows the
 * soldier's precomputed path when set, otherwise walks straight toward the target.
 */
public class MovementSystem implements ISystem {

    /**
     * Moves walking soldiers toward their targets and garrisons those that arrive
     * at a friendly military building. Soldiers whose target disappeared are
     * re-routed to the nearest friendly garrison with a free slot.
     *
     * @param gameState Game state of the current session.
     */
    @Override
    public void process(GameState gameState) {
        if (gameState.getCurrentTick() % GameConfig.SOLDIER_MOVE_TICKS != 0) {
            return;
        }

        List<Soldier> arrived = new ArrayList<>();
        for (Soldier soldier : gameState.getSoldiers()) {
            if (soldier.getState() != SoldierState.WALKING_TO_GARRISON
                    && soldier.getState() != SoldierState.MARCHING_TO_ATTACK
                    && soldier.getState() != SoldierState.WALKING_TO_DEFEND) {
                continue;
            }

            Building target = soldier.getTargetBuilding();
            if (target == null || target.isDestroyed()) {
                retargetNearestGarrison(gameState, soldier);
                target = soldier.getTargetBuilding();
                if (target == null) {
                    continue;
                }
            }

            if (!soldier.isAt(target.getPosition())) {
                if (!soldier.hasReachedDestination()) {
                    soldier.advanceOnPath();
                } else {
                    soldier.stepToward(target.getPosition());
                }
            }

            if (soldier.isAt(target.getPosition())
                    && (soldier.getState() == SoldierState.WALKING_TO_GARRISON
                        || soldier.getState() == SoldierState.WALKING_TO_DEFEND)
                    && target instanceof MilitaryBuilding mb) {
                if (mb.addSoldier(soldier)) {
                    soldier.setState(SoldierState.GARRISONED);
                    soldier.setGarrison(mb);
                    soldier.setTargetBuilding(null);
                    arrived.add(soldier);
                } else {
                    retargetNearestGarrison(gameState, soldier);
                }
            }
            // Attackers arriving at their target are handled by the CombatSystem.
        }

        // Garrisoned soldiers now live inside their building, not on the map
        gameState.getSoldiers().removeAll(arrived);
    }

    /**
     * Re-routes a soldier to the nearest friendly military building with a free slot.
     * If none exists, the soldier keeps standing where they are.
     *
     * @param state   the current game state
     * @param soldier the soldier to re-route
     */
    private void retargetNearestGarrison(GameState state, Soldier soldier) {
        MilitaryBuilding nearest = null;
        int minDist = Integer.MAX_VALUE;
        for (Building b : state.getBuildings()) {
            if (!(b instanceof MilitaryBuilding mb) || mb.isDestroyed()
                    || mb.getPlayerId() != soldier.getPlayerId() || !mb.hasRoom()) {
                continue;
            }
            int dist = mb.getPosition().distanceTo(soldier.getPosition());
            if (dist < minDist) {
                minDist = dist;
                nearest = mb;
            }
        }

        soldier.setPath(new ArrayList<>());
        soldier.setPathIndex(0);
        if (nearest != null) {
            soldier.setState(SoldierState.WALKING_TO_GARRISON);
            soldier.setTargetBuilding(nearest);
        } else {
            soldier.setTargetBuilding(null);
        }
    }
}
