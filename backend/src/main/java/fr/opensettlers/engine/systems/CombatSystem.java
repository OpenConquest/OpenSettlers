package fr.opensettlers.engine.systems;

import fr.opensettlers.engine.GameState;
import fr.opensettlers.engine.state.Building;
import fr.opensettlers.engine.state.MilitaryBuilding;
import fr.opensettlers.engine.state.Soldier;
import fr.opensettlers.engine.state.StorageBuilding;
import fr.opensettlers.engine.state.utils.SoldierState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * System resolving combat: duels between soldiers sharing a tile, defender
 * sorties from attacked garrisons, and capture or destruction of buildings
 * once their defense is exhausted.
 */
public class CombatSystem implements ISystem {

    /**
     * Resolves all ongoing fights and sieges for the current tick.
     *
     * @param gameState Game state of the current session.
     */
    @Override
    public void process(GameState gameState) {
        engageArrivedAttackers(gameState);
        resolveDuels(gameState);
        resolveSieges(gameState);
        releaseVictoriousDefenders(gameState);
    }

    /**
     * Switches attackers that reached their target building into fighting mode.
     *
     * @param state the current game state
     */
    private void engageArrivedAttackers(GameState state) {
        for (Soldier soldier : state.getSoldiers()) {
            if (soldier.getState() != SoldierState.ATTACKING) continue;
            Building target = findBuildingById(state, soldier.getTargetBuildingId());
            if (target != null && !target.isDestroyed() && soldier.isAt(target.getPosition())) {
                soldier.setState(SoldierState.FIGHTING);
            }
        }
    }

    /**
     * Makes every pair of enemy soldiers sharing a tile exchange blows.
     * Dead soldiers are swept by the game state at the next tick.
     *
     * @param state the current game state
     */
    private void resolveDuels(GameState state) {
        List<Soldier> soldiers = state.getSoldiers();
        for (int i = 0; i < soldiers.size(); i++) {
            for (int j = i + 1; j < soldiers.size(); j++) {
                Soldier a = soldiers.get(i);
                Soldier b = soldiers.get(j);
                if (a.getPlayerId() == b.getPlayerId() || a.isDead() || b.isDead()
                        || !a.isSamePosition(b)) {
                    continue;
                }
                a.setState(SoldierState.FIGHTING);
                b.setState(SoldierState.FIGHTING);
                a.attack(b);
                if (!b.isDead()) {
                    b.attack(a);
                }
            }
        }
    }

    /**
     * For each building under siege: sends out a defender if the garrison still
     * holds one, otherwise resolves the capture or destruction of the building.
     *
     * @param state the current game state
     */
    private void resolveSieges(GameState state) {
        List<Building> buildings = new ArrayList<>(state.getBuildings());
        for (Building building : buildings) {
            if (building.isDestroyed()) continue;

            List<Soldier> attackers = livingSoldiersAt(state, building, true);
            if (attackers.isEmpty()) continue;

            List<Soldier> defenders = livingSoldiersAt(state, building, false);
            if (!defenders.isEmpty()) continue; // A duel is already in progress

            if (building instanceof MilitaryBuilding mb && !mb.getSoldiers().isEmpty()) {
                Soldier defender = mb.releaseSoldier();
                defender.setPosition(new fr.opensettlers.engine.state.utils.Coordinates(
                        mb.getPosition().getX(), mb.getPosition().getY()));
                defender.setState(SoldierState.FIGHTING);
                defender.setTargetBuildingId(mb.getId());
                state.getSoldiers().add(defender);
            } else if (building instanceof MilitaryBuilding mb) {
                captureBuilding(state, mb, attackers.get(0));
            } else if (building instanceof StorageBuilding) {
                building.destroy();
                if (building.getAttachedFlag() != null) {
                    building.getAttachedFlag().destroy();
                }
            }
        }
    }

    /**
     * Transfers a conquered military building (and its flag) to the attacker,
     * garrisons the winning soldier inside, and claims the surrounding territory.
     *
     * @param state    the current game state
     * @param building the conquered military building
     * @param winner   the attacking soldier taking possession
     */
    private void captureBuilding(GameState state, MilitaryBuilding building, Soldier winner) {
        building.setPlayerId(winner.getPlayerId());
        if (building.getAttachedFlag() != null) {
            building.getAttachedFlag().setPlayerId(winner.getPlayerId());
        }

        winner.setState(SoldierState.GARRISONED);
        winner.setTargetBuildingId(null);
        building.garrison(winner);
        state.getSoldiers().remove(winner);

        if (state.getMap() != null) {
            state.getMap().claimTerritory(
                    building.getPosition(), building.getTerritoryRadius(), winner.getPlayerId());
        }
    }

    /**
     * Sends fighting soldiers with no remaining enemy on their tile back to
     * their garrison (defenders) or marching again (attackers whose duel ended).
     *
     * @param state the current game state
     */
    private void releaseVictoriousDefenders(GameState state) {
        for (Soldier soldier : state.getSoldiers()) {
            if (soldier.getState() != SoldierState.FIGHTING || soldier.isDead()) continue;

            boolean enemyPresent = state.getSoldiers().stream()
                    .anyMatch(other -> !other.isDead()
                            && other.getPlayerId() != soldier.getPlayerId()
                            && other.isSamePosition(soldier));
            if (enemyPresent) continue;

            Building target = findBuildingById(state, soldier.getTargetBuildingId());
            if (target == null || target.isDestroyed()) {
                soldier.setState(SoldierState.WALKING_TO_GARRISON);
            } else if (target.getPlayerId() == soldier.getPlayerId()) {
                soldier.setState(SoldierState.WALKING_TO_GARRISON);
            } else {
                soldier.setState(SoldierState.ATTACKING);
            }
        }
    }

    /**
     * Lists the living soldiers standing on a building's tile, filtered by side.
     *
     * @param state     the current game state
     * @param building  the building whose tile is inspected
     * @param attackers {@code true} to list enemies of the building, {@code false} for its owner's soldiers
     * @return the matching soldiers
     */
    private List<Soldier> livingSoldiersAt(GameState state, Building building, boolean attackers) {
        List<Soldier> result = new ArrayList<>();
        for (Soldier s : state.getSoldiers()) {
            if (s.isDead() || !s.isAt(building.getPosition())) continue;
            boolean isEnemy = s.getPlayerId() != building.getPlayerId();
            if (isEnemy == attackers) {
                result.add(s);
            }
        }
        return result;
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
