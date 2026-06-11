package fr.opensettlers.engine.systems;

import fr.opensettlers.engine.GameState;
import fr.opensettlers.engine.state.*;
import fr.opensettlers.engine.state.utils.Coordinates;
import fr.opensettlers.engine.state.utils.ResourceType;
import fr.opensettlers.engine.state.utils.SoldierState;

import java.util.UUID;

/**
 * System managing garrisons: recruits soldiers for military buildings with free
 * slots (one neutral settler + one sword per soldier) and claims territory
 * around newly occupied buildings.
 */
public class MilitarySystem implements ISystem {

    /**
     * Recruits soldiers toward under-staffed military buildings and claims territory
     * for occupied ones.
     *
     * @param gameState the active game session state
     */
    @Override
    public void process(GameState gameState) {
        for (Building b : gameState.getBuildings()) {
            if (!(b instanceof MilitaryBuilding mb) || mb.isDestroyed()) {
                continue;
            }

            if (!mb.isTerritoryClaimed() && !mb.getSoldiers().isEmpty() && gameState.getMap() != null) {
                gameState.getMap().claimTerritory(mb.getPosition(), mb.getTerritoryRadius(), mb.getPlayerId());
                mb.markTerritoryClaimed();
            }

            int reserved = countReservedSlots(gameState, mb);
            int missing = mb.getMaxCapacity() - mb.getSoldiers().size() - reserved;
            for (int i = 0; i < missing; i++) {
                if (!recruitSoldier(gameState, mb)) {
                    break;
                }
            }
        }
    }

    /**
     * Counts soldiers whose garrison slot must stay reserved: recruits walking
     * toward the building and defenders out on a sortie (fighting in front of it).
     *
     * @param state the current game state
     * @param mb    the military building
     * @return the number of reserved slots
     */
    private int countReservedSlots(GameState state, MilitaryBuilding mb) {
        int count = 0;
        for (Soldier s : state.getSoldiers()) {
            if (s.getPlayerId() == mb.getPlayerId()
                    && mb.getId().equals(s.getTargetBuildingId())
                    && (s.getState() == SoldierState.WALKING_TO_GARRISON
                        || s.getState() == SoldierState.FIGHTING)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Recruits one soldier from the nearest storage building holding both a
     * neutral settler and a sword. The soldier spawns at the storage and walks
     * to the military building.
     *
     * @param state the current game state
     * @param mb    the military building to staff
     * @return {@code true} if a soldier was recruited
     */
    private boolean recruitSoldier(GameState state, MilitaryBuilding mb) {
        StorageBuilding source = findNearestRecruitingStorage(state, mb);
        if (source == null) {
            return false;
        }

        source.setStoredNeutralSettlers(source.getStoredNeutralSettlers() - 1);
        source.retrieveResource(ResourceType.SWORD);

        Soldier soldier = new Soldier(UUID.randomUUID(), mb.getPlayerId(),
                new Coordinates(source.getPosition().getX(), source.getPosition().getY()));
        soldier.setState(SoldierState.WALKING_TO_GARRISON);
        soldier.setTargetBuildingId(mb.getId());
        state.getSoldiers().add(soldier);
        return true;
    }

    /**
     * Finds the nearest storage building of the same player able to recruit
     * (at least one settler and one sword in stock).
     *
     * @param state the current game state
     * @param mb    the military building to staff
     * @return the nearest eligible storage, or {@code null} if none
     */
    private StorageBuilding findNearestRecruitingStorage(GameState state, MilitaryBuilding mb) {
        StorageBuilding nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Building b : state.getBuildings()) {
            if (!(b instanceof StorageBuilding sb) || sb.isDestroyed()
                    || sb.getPlayerId() != mb.getPlayerId()
                    || sb.getStoredNeutralSettlers() < 1
                    || sb.getStoredResources().getOrDefault(ResourceType.SWORD, 0) < 1) {
                continue;
            }
            double dist = Math.hypot(
                    sb.getPosition().getX() - mb.getPosition().getX(),
                    sb.getPosition().getY() - mb.getPosition().getY());
            if (dist < minDist) {
                minDist = dist;
                nearest = sb;
            }
        }
        return nearest;
    }
}
