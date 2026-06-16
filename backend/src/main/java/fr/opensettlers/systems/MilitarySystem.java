package fr.opensettlers.systems;

import fr.opensettlers.utils.GameConfig;
import fr.opensettlers.state.GameState;
import fr.opensettlers.entities.*;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.ResourceType;
import fr.opensettlers.utils.SoldierState;

/**
 * System managing garrisons: recruits soldiers for military buildings with free
 * slots (one neutral settler + one sword + one shield + one beer per soldier,
 * as in Settlers II), recalculates the territory when a building becomes
 * occupied for the first time, and promotes garrisoned soldiers with the gold
 * coins delivered to the building.
 */
public class MilitarySystem implements ISystem {

    /**
     * Recruits soldiers toward under-staffed military buildings, triggers the
     * territory claim of newly occupied ones, ingests delivered gold coins,
     * and promotes garrisoned soldiers.
     *
     * @param gameState the active game session state
     */
    @Override
    public void process(GameState gameState) {
        for (Building b : gameState.getBuildings()) {
            if (!(b instanceof MilitaryBuilding mb) || mb.isDestroyed()) {
                continue;
            }

            if (!mb.isTerritoryClaimed() && !mb.getSoldiers().isEmpty()) {
                mb.setTerritoryClaimed(true);
                gameState.getTerritoryManager().recalculate(gameState);
            }

            ingestCoins(mb);
            promoteSoldiers(mb);

            // Staff toward the player's chosen occupation (always at least one
            // soldier, so the building keeps holding its territory).
            int occupation = gameState.getMilitaryOccupationOf(mb.getPlayerId());
            int target = Math.max(1, (int) Math.ceil(mb.getMaxCapacity() * occupation / 100.0));
            int reserved = countReservedSlots(gameState, mb);
            int missing = target - mb.getSoldiers().size() - reserved;
            for (int i = 0; i < missing; i++) {
                if (!recruitSoldier(gameState, mb)) {
                    break;
                }
            }
        }
    }

    /**
     * Moves gold coins that arrived on the building's flag into its coin store,
     * up to the building's coin capacity.
     *
     * @param mb the military building
     */
    private void ingestCoins(MilitaryBuilding mb) {
        Flag flag = mb.getAttachedFlag();
        if (flag == null) return;

        int capacity = GameConfig.coinCapacity(mb.getBuildingName());
        for (int i = 0; i < flag.getResourceSlots().size() && mb.getStoredCoins() < capacity; i++) {
            ResourceStack rs = flag.getResourceSlots().get(i);
            if (rs.getType() == ResourceType.COIN && flag.getId().equals(rs.getTargetFlagId())) {
                flag.getResourceSlots().remove(i);
                mb.setStoredCoins(mb.getStoredCoins() + 1);
                i--;
            }
        }
    }

    /**
     * Spends one stored gold coin to promote the lowest-ranked garrisoned
     * soldier, at most once every {@link GameConfig#PROMOTION_TICKS} ticks.
     *
     * @param mb the military building
     */
    private void promoteSoldiers(MilitaryBuilding mb) {
        if (mb.getPromotionCooldown() > 0) {
            mb.setPromotionCooldown(mb.getPromotionCooldown() - 1);
            return;
        }
        if (mb.getStoredCoins() <= 0) return;

        Soldier candidate = null;
        for (Soldier s : mb.getSoldiers()) {
            if (!s.getRank().isPromotable()) continue;
            if (candidate == null || s.getRank().ordinal() < candidate.getRank().ordinal()) {
                candidate = s;
            }
        }
        if (candidate == null) return;

        candidate.promote();
        mb.setStoredCoins(mb.getStoredCoins() - 1);
        mb.setPromotionCooldown(GameConfig.PROMOTION_TICKS);
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
                    && mb.equals(s.getTargetBuilding())
                    && (s.getState() == SoldierState.WALKING_TO_GARRISON
                        || s.getState() == SoldierState.FIGHTING
                        || s.getState() == SoldierState.WALKING_TO_DEFEND)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Recruits one soldier from the nearest storage building holding a neutral
     * settler plus the full Settlers II kit: sword, shield and beer. The
     * soldier spawns at the storage and walks to the military building.
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
        source.retrieveResource(ResourceType.SHIELD);
        source.retrieveResource(ResourceType.BEER);

        Soldier soldier = new Soldier(mb.getPlayerId(),
                new Coordinates(source.getPosition().getX(), source.getPosition().getY()));
        soldier.setState(SoldierState.WALKING_TO_GARRISON);
        soldier.setTargetBuilding(mb);
        state.getSoldiers().add(soldier);
        return true;
    }

    /**
     * Finds the nearest storage building of the same player able to recruit
     * (at least one settler, one sword, one shield and one beer in stock).
     *
     * @param state the current game state
     * @param mb    the military building to staff
     * @return the nearest eligible storage, or {@code null} if none
     */
    private StorageBuilding findNearestRecruitingStorage(GameState state, MilitaryBuilding mb) {
        StorageBuilding nearest = null;
        int minDist = Integer.MAX_VALUE;
        for (Building b : state.getBuildings()) {
            if (!(b instanceof StorageBuilding sb) || sb.isDestroyed()
                    || sb.getPlayerId() != mb.getPlayerId()
                    || sb.getStoredNeutralSettlers() < 1
                    || sb.getStoredResources().getOrDefault(ResourceType.SWORD, 0) < 1
                    || sb.getStoredResources().getOrDefault(ResourceType.SHIELD, 0) < 1
                    || sb.getStoredResources().getOrDefault(ResourceType.BEER, 0) < 1) {
                continue;
            }
            int dist = sb.getPosition().distanceTo(mb.getPosition());
            if (dist < minDist) {
                minDist = dist;
                nearest = sb;
            }
        }
        return nearest;
    }
}
