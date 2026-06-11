package fr.opensettlers.engine.systems;

import fr.opensettlers.engine.GameConfig;
import fr.opensettlers.engine.GameState;
import fr.opensettlers.engine.state.Building;
import fr.opensettlers.engine.state.MilitaryBuilding;
import fr.opensettlers.engine.state.Soldier;
import fr.opensettlers.engine.state.utils.SoldierState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * System moving soldier units across the map toward their target buildings,
 * one grid step every {@link GameConfig#SOLDIER_MOVE_TICKS} ticks.
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
                    && soldier.getState() != SoldierState.ATTACKING) {
                continue;
            }

            Building target = findBuildingById(gameState, soldier.getTargetBuildingId());
            if (target == null || target.isDestroyed()) {
                retargetNearestGarrison(gameState, soldier);
                target = findBuildingById(gameState, soldier.getTargetBuildingId());
                if (target == null) {
                    continue;
                }
            }

            if (!soldier.isAt(target.getPosition())) {
                soldier.stepToward(target.getPosition());
            }

            if (soldier.isAt(target.getPosition())
                    && soldier.getState() == SoldierState.WALKING_TO_GARRISON
                    && target instanceof MilitaryBuilding mb) {
                if (mb.garrison(soldier)) {
                    soldier.setState(SoldierState.GARRISONED);
                    soldier.setTargetBuildingId(null);
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
        double minDist = Double.MAX_VALUE;
        for (Building b : state.getBuildings()) {
            if (!(b instanceof MilitaryBuilding mb) || mb.isDestroyed()
                    || mb.getPlayerId() != soldier.getPlayerId() || !mb.hasFreeSlot()) {
                continue;
            }
            double dist = Math.hypot(
                    mb.getPosition().getX() - soldier.getPosition().getX(),
                    mb.getPosition().getY() - soldier.getPosition().getY());
            if (dist < minDist) {
                minDist = dist;
                nearest = mb;
            }
        }

        if (nearest != null) {
            soldier.setState(SoldierState.WALKING_TO_GARRISON);
            soldier.setTargetBuildingId(nearest.getId());
        } else {
            soldier.setTargetBuildingId(null);
        }
    }

    /**
     * Finds a building by its unique identifier.
     *
     * @param state the current game state
     * @param id    the building UUID, may be {@code null}
     * @return the matching building, or {@code null}
     */
    private Building findBuildingById(GameState state, UUID id) {
        if (id == null) return null;
        for (Building b : state.getBuildings()) {
            if (b.getId().equals(id)) {
                return b;
            }
        }
        return null;
    }
}
