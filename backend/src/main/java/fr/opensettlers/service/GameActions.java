package fr.opensettlers.service;

import fr.opensettlers.entities.Building;
import fr.opensettlers.entities.ConstructionSite;
import fr.opensettlers.entities.Flag;
import fr.opensettlers.entities.MapTile;
import fr.opensettlers.entities.MilitaryBuilding;
import fr.opensettlers.entities.ProductionBuilding;
import fr.opensettlers.entities.Soldier;
import fr.opensettlers.entities.StorageBuilding;
import fr.opensettlers.entities.Worker;
import fr.opensettlers.state.GameState;
import fr.opensettlers.utils.BuildingName;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.Direction;
import fr.opensettlers.utils.GameConfig;
import fr.opensettlers.utils.ResourceType;
import fr.opensettlers.utils.SiteSize;
import fr.opensettlers.utils.SoldierState;
import fr.opensettlers.utils.TileType;
import fr.opensettlers.utils.WorkerType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Validated, side-effecting game operations shared by every actor able to play:
 * the human players (through {@link GameEngineService} and the WebSocket
 * protocol) and the built-in computer opponents (through
 * {@link fr.opensettlers.systems.AiSystem}).
 *
 * <p>All methods must be called on the game-loop thread, which guarantees
 * single-threaded access to the {@link GameState}. They validate terrain,
 * ownership and territory rules and return whether the action was carried out,
 * so callers can decide how to report success or failure.</p>
 */
public final class GameActions {

    /** Private constructor to prevent instantiation of this utility class. */
    private GameActions() {}

    /**
     * Places a construction site for a new building, after validating the
     * placement.
     *
     * @param state    the game state to mutate
     * @param playerId the player ordering the construction
     * @param name     the building type to construct
     * @param position the desired position
     * @return {@code true} if a construction site was created
     */
    public static boolean placeBuilding(GameState state, int playerId, BuildingName name, Coordinates position) {
        if (!isPlacementValid(state, name, position, playerId)) {
            return false;
        }
        ConstructionSite site = new ConstructionSite(playerId, position, name);
        state.getBuildings().add(site);
        state.getRoadNetwork().addFlag(site.getAttachedFlag());
        return true;
    }

    /**
     * Checks terrain, ownership and site-grading rules for a building placement,
     * mirroring the Settlers II construction sites:
     * <ul>
     *   <li>Mines must stand on owned mountain tiles bearing their ore (within
     *       miner reach).</li>
     *   <li>Harbors and shipyards must stand on owned coastal grass (adjacent to water).</li>
     *   <li>Every other building needs owned, empty grass whose slope allows its
     *       site grade (hut &lt; house &lt; castle) and enough spacing from the
     *       neighboring buildings and flags.</li>
     * </ul>
     *
     * @param state    the game state
     * @param name     the building type
     * @param position the desired position
     * @param playerId the building owner
     * @return {@code true} if the placement is allowed
     */
    public static boolean isPlacementValid(GameState state, BuildingName name, Coordinates position, int playerId) {
        MapTile tile = state.getTile(position);
        if (tile == null || !state.getTerritoryManager().canBuild(playerId, position)) {
            return false;
        }
        if (isOccupied(state, position) || hasFlagAt(state, position)) {
            return false;
        }

        SiteSize size = name.siteSize();
        if (size == SiteSize.MINE) {
            return tile.getType() == TileType.MOUNTAIN
                    && hasOreForMine(state, name, position)
                    && hasBuildingSpacing(state, position, GameConfig.SITE_MIN_BUILDING_DISTANCE);
        }
        if (!tile.isBuildable()) {
            return false;
        }
        if ((name == BuildingName.HARBOR || name == BuildingName.SHIPYARD)
                && !state.hasWaterInRange(position, 1)) {
            return false;
        }

        int maxSlope = switch (size) {
            case HUT -> GameConfig.SITE_MAX_SLOPE_HUT;
            case HOUSE -> GameConfig.SITE_MAX_SLOPE_HOUSE;
            default -> GameConfig.SITE_MAX_SLOPE_CASTLE;
        };
        for (Direction dir : Direction.values()) {
            MapTile neighbor = state.getTile(position.neighbor(dir));
            if (neighbor != null
                    && Math.abs(neighbor.getElevation() - tile.getElevation()) > maxSlope) {
                return false;
            }
        }

        int minDistance = size == SiteSize.CASTLE
                ? GameConfig.SITE_MIN_BUILDING_DISTANCE_CASTLE
                : GameConfig.SITE_MIN_BUILDING_DISTANCE;
        return hasBuildingSpacing(state, position, minDistance);
    }

    /**
     * Checks that no other building stands closer than the given distance.
     *
     * @param state       the game state
     * @param position    the candidate position
     * @param minDistance the minimum allowed hex distance to any building
     * @return {@code true} if the spacing is respected
     */
    private static boolean hasBuildingSpacing(GameState state, Coordinates position, int minDistance) {
        for (Building b : state.getBuildings()) {
            if (!b.isDestroyed() && b.getPosition().distanceTo(position) < minDistance) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether a mine would have its ore in digging reach: the mountain
     * tile it stands on or a deposit within {@link GameConfig#MINER_MAX_DISTANCE}.
     *
     * @param state    the game state
     * @param name     the mine type (granite, coal, iron or gold)
     * @param position the candidate position
     * @return {@code true} if the matching ore is present
     */
    private static boolean hasOreForMine(GameState state, BuildingName name, Coordinates position) {
        ResourceType ore = name.minedResource();
        MapTile own = state.getTile(position);
        if (own != null && own.getNaturalResource() != null
                && own.getNaturalResource().getType() == ore
                && own.getNaturalResource().isHarvestable()) {
            return true;
        }
        return !state.findResourceTilesInRange(position, GameConfig.MINER_MAX_DISTANCE, ore).isEmpty();
    }

    /**
     * Indicates whether a flag already stands on a tile.
     *
     * @param state    the game state
     * @param position the tile to test
     * @return {@code true} if a live flag occupies the tile
     */
    public static boolean hasFlagAt(GameState state, Coordinates position) {
        for (Flag f : state.getRoadNetwork().getAllFlags()) {
            if (!f.isDestroyed() && f.getCoordinates().equals(position)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Indicates whether a non-destroyed building already stands on a tile.
     *
     * @param state    the game state
     * @param position the tile to test
     * @return {@code true} if the tile is already built on
     */
    public static boolean isOccupied(GameState state, Coordinates position) {
        for (Building b : state.getBuildings()) {
            if (!b.isDestroyed() && b.getPosition().equals(position)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Places a standalone flag inside the player's territory, on a free,
     * walkable tile (no other flag or building).
     *
     * @param state    the game state to mutate
     * @param playerId the player placing the flag
     * @param position the desired position
     * @return the new flag, or {@code null} if the placement was rejected
     */
    public static Flag placeFlag(GameState state, int playerId, Coordinates position) {
        MapTile tile = state.getTile(position);
        if (tile == null || !tile.isWalkable()
                || !state.getTerritoryManager().canBuild(playerId, position)
                || isOccupied(state, position) || hasFlagAt(state, position)) {
            return null;
        }
        Flag flag = new Flag(UUID.randomUUID(), playerId, position);
        state.getFlags().add(flag);
        state.getRoadNetwork().addFlag(flag);
        return flag;
    }

    /**
     * Links two existing flags with a road and its carrier.
     *
     * @param state    the game state to mutate
     * @param flagIdA  one endpoint flag ID
     * @param flagIdB  the other endpoint flag ID
     * @param path     intermediate path coordinates (excluding the endpoints)
     * @return {@code true} if a road was created
     */
    public static boolean linkFlags(GameState state, UUID flagIdA, UUID flagIdB, List<Coordinates> path) {
        Flag flagA = state.getRoadNetwork().getFlagById(flagIdA);
        Flag flagB = state.getRoadNetwork().getFlagById(flagIdB);
        if (flagA == null || flagB == null) {
            return false;
        }
        state.getRoadNetwork().addRoad(flagA, flagB, path);
        return true;
    }

    /**
     * Destroys one of the player's own buildings, recalculating territory when
     * a military building or warehouse is razed.
     *
     * @param state    the game state to mutate
     * @param playerId the player ordering the demolition
     * @param targetId the building to destroy
     * @return {@code true} if a building was destroyed
     */
    public static boolean destroyBuilding(GameState state, int playerId, UUID targetId) {
        Building b = findBuilding(state, targetId);
        if (b == null || b.getPlayerId() != playerId || b.isDestroyed()) {
            return false;
        }
        b.destroy();
        if (b instanceof MilitaryBuilding || b instanceof StorageBuilding) {
            state.getTerritoryManager().recalculate(state);
        }
        return true;
    }

    /**
     * Sends every available soldier at an enemy building (no attacker limit).
     *
     * @param state    the game state to mutate
     * @param playerId the attacking player
     * @param targetId the target building
     * @return the number of soldiers dispatched, or {@code -1} for an invalid order
     */
    public static int attack(GameState state, int playerId, UUID targetId) {
        return attack(state, playerId, targetId, 0);
    }

    /**
     * Sends up to {@code attackerCount} soldiers from the player's military
     * buildings within {@link GameConfig#ATTACK_RADIUS} toward an enemy military
     * building or warehouse, drawing from the closest garrisons first. Each
     * garrison keeps one defender behind, as in Settlers II.
     *
     * @param state         the game state to mutate
     * @param playerId      the attacking player
     * @param targetId      the target building
     * @param attackerCount maximum soldiers to send ({@code <= 0} for no limit)
     * @return the number of soldiers dispatched, or {@code -1} for an invalid order
     */
    public static int attack(GameState state, int playerId, UUID targetId, int attackerCount) {
        Building target = findBuilding(state, targetId);
        if (target == null || target.isDestroyed() || target.getPlayerId() == playerId) {
            return -1;
        }
        if (!(target instanceof MilitaryBuilding) && !(target instanceof StorageBuilding)) {
            return -1;
        }

        List<MilitaryBuilding> sources = new ArrayList<>();
        for (Building b : state.getBuildings()) {
            if (b instanceof MilitaryBuilding mb && !mb.isDestroyed() && mb.getPlayerId() == playerId
                    && mb.getPosition().distanceTo(target.getPosition()) <= GameConfig.ATTACK_RADIUS) {
                sources.add(mb);
            }
        }
        sources.sort(Comparator.comparingInt(mb -> mb.getPosition().distanceTo(target.getPosition())));

        int remaining = attackerCount <= 0 ? Integer.MAX_VALUE : attackerCount;
        int dispatched = 0;
        for (MilitaryBuilding mb : sources) {
            while (mb.getSoldiers().size() > 1 && remaining > 0) {
                Soldier soldier = mb.removeFirstSoldier();
                soldier.setPosition(new Coordinates(mb.getPosition().getX(), mb.getPosition().getY()));
                soldier.setState(SoldierState.MARCHING_TO_ATTACK);
                soldier.setTargetBuilding(target);
                soldier.setGarrison(null);
                state.getSoldiers().add(soldier);
                dispatched++;
                remaining--;
            }
        }
        return dispatched;
    }

    /**
     * Trains a geologist at the nearest able warehouse and sends them to survey
     * the mountains around one of the player's flags.
     *
     * @param state    the game state to mutate
     * @param playerId the player ordering the survey
     * @param flagId   the destination flag to survey around
     * @return {@code true} if a geologist was dispatched
     */
    public static boolean sendGeologist(GameState state, int playerId, UUID flagId) {
        Flag target = state.getRoadNetwork().getFlagById(flagId);
        if (target == null || target.isDestroyed() || target.getPlayerId() != playerId) {
            return false;
        }

        StorageBuilding source = null;
        double minDist = Double.MAX_VALUE;
        for (Building b : state.getBuildings()) {
            if (b instanceof StorageBuilding sb && !sb.isDestroyed()
                    && sb.getPlayerId() == playerId && sb.canSpawnWorker()) {
                double dist = sb.getPosition().distanceTo(target.getCoordinates());
                if (dist < minDist) {
                    minDist = dist;
                    source = sb;
                }
            }
        }
        if (source == null) {
            return false;
        }

        Worker geologist = source.spawnWorker(WorkerType.GEOLOGIST, source.getPosition());
        geologist.setTargetFlagId(target.getId());
        geologist.setSurveysLeft(GameConfig.GEOLOGIST_SURVEYS);
        state.getWorkers().add(geologist);
        return true;
    }

    /**
     * Trains a scout at the nearest able warehouse and sends them to explore
     * the fog of war around one of the player's flags.
     *
     * @param state    the game state to mutate
     * @param playerId the player ordering the exploration
     * @param flagId   the destination flag to explore around
     * @return {@code true} if a scout was dispatched
     */
    public static boolean sendScout(GameState state, int playerId, UUID flagId) {
        Flag target = state.getRoadNetwork().getFlagById(flagId);
        if (target == null || target.isDestroyed() || target.getPlayerId() != playerId) {
            return false;
        }

        StorageBuilding source = null;
        double minDist = Double.MAX_VALUE;
        for (Building b : state.getBuildings()) {
            if (b instanceof StorageBuilding sb && !sb.isDestroyed()
                    && sb.getPlayerId() == playerId && sb.canSpawnWorker()) {
                double dist = sb.getPosition().distanceTo(target.getCoordinates());
                if (dist < minDist) {
                    minDist = dist;
                    source = sb;
                }
            }
        }
        if (source == null) {
            return false;
        }

        Worker scout = source.spawnWorker(WorkerType.SCOUT, source.getPosition());
        scout.setTargetFlagId(target.getId());
        scout.setSurveysLeft(GameConfig.SCOUT_STEPS);
        state.getWorkers().add(scout);
        return true;
    }

    /**
     * Pauses or resumes production at one of the player's own production
     * buildings.
     *
     * @param state    the game state to mutate
     * @param playerId the player owning the building
     * @param targetId the production building to toggle
     * @param enabled  {@code true} to produce, {@code false} to pause
     * @return {@code true} if the building was toggled
     */
    public static boolean setProduction(GameState state, int playerId, UUID targetId, boolean enabled) {
        Building b = findBuilding(state, targetId);
        if (b instanceof ProductionBuilding pb && !b.isDestroyed() && b.getPlayerId() == playerId) {
            pb.setProductionPaused(!enabled);
            return true;
        }
        return false;
    }

    /**
     * Enables or disables gold-coin delivery to one of the player's own military
     * buildings.
     *
     * @param state    the game state to mutate
     * @param playerId the player owning the building
     * @param targetId the military building to toggle
     * @param enabled  {@code true} to accept coins, {@code false} to refuse them
     * @return {@code true} if the building was toggled
     */
    public static boolean setCoinDelivery(GameState state, int playerId, UUID targetId, boolean enabled) {
        Building b = findBuilding(state, targetId);
        if (b instanceof MilitaryBuilding mb && !b.isDestroyed() && b.getPlayerId() == playerId) {
            mb.setCoinsAllowed(enabled);
            return true;
        }
        return false;
    }

    /**
     * Sets the player's distribution priority order of consumer buildings for a
     * contested good.
     *
     * @param state      the game state to mutate
     * @param playerId   the player whose preferences to change
     * @param type       the contested good
     * @param priorities the ordered consumer building types, highest first
     * @return {@code true} if the preferences were updated
     */
    public static boolean setDistribution(GameState state, int playerId, ResourceType type, List<BuildingName> priorities) {
        if (type == null || priorities == null) {
            return false;
        }
        state.getDistributionFor(playerId).put(type, new ArrayList<>(priorities));
        return true;
    }

    /**
     * Sets the player's target garrison occupation (military strength), clamped
     * to [0, 100].
     *
     * @param state    the game state to mutate
     * @param playerId the player whose preference to change
     * @param percent  the desired occupation percentage
     * @return {@code true} (always accepted; the value is clamped)
     */
    public static boolean setMilitaryOccupation(GameState state, int playerId, int percent) {
        state.setMilitaryOccupationOf(playerId, percent);
        return true;
    }

    /**
     * Finds a building by its ID.
     *
     * @param state    the game state
     * @param targetId the building ID
     * @return the building, or {@code null} if not found
     */
    private static Building findBuilding(GameState state, UUID targetId) {
        for (Building b : state.getBuildings()) {
            if (b.getId().equals(targetId)) {
                return b;
            }
        }
        return null;
    }
}
