package fr.opensettlers.persistence;

import fr.opensettlers.entities.Building;
import fr.opensettlers.entities.BuildingFactory;
import fr.opensettlers.entities.ConstructionSite;
import fr.opensettlers.entities.Flag;
import fr.opensettlers.entities.MapTile;
import fr.opensettlers.entities.MilitaryBuilding;
import fr.opensettlers.entities.NaturalResourceNode;
import fr.opensettlers.entities.ProductionBuilding;
import fr.opensettlers.entities.Road;
import fr.opensettlers.entities.Soldier;
import fr.opensettlers.entities.StorageBuilding;
import fr.opensettlers.entities.Worker;
import fr.opensettlers.state.GameState;
import fr.opensettlers.utils.BuildingName;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.ResourceType;
import fr.opensettlers.utils.SoldierRank;
import fr.opensettlers.utils.SoldierState;
import fr.opensettlers.utils.TileType;
import fr.opensettlers.utils.WorkerState;
import fr.opensettlers.utils.WorkerType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Converts a live {@link GameState} to and from a {@link GameSnapshot}.
 *
 * <p>{@link #toSnapshot(GameState)} captures the durable parts of a game; the
 * inverse {@link #toGameState(GameSnapshot, UUID)} rebuilds a playable state from
 * a snapshot. Buildings are recreated complete (any in-progress construction is
 * finished), production buildings get their specialist back so the economy keeps
 * running, and the road network is rewired from the stored flags and roads.</p>
 */
public final class GameSnapshotMapper {

    /** Private constructor to prevent instantiation of this utility class. */
    private GameSnapshotMapper() {}

    /**
     * Captures the durable state of a running game.
     *
     * @param state the live game state
     * @return a serializable snapshot
     */
    public static GameSnapshot toSnapshot(GameState state) {
        GameSnapshot snap = new GameSnapshot();
        snap.tick = state.getCurrentTick();
        snap.playerCount = state.getPlayerCount();
        snap.aiPlayers = new ArrayList<>(state.getAiPlayers());
        snap.eliminatedPlayers = new ArrayList<>(state.getEliminatedPlayers());
        snap.winnerPlayerId = state.getWinnerPlayerId();
        snap.over = state.isOver();

        for (MapTile tile : state.getMapTiles().values()) {
            GameSnapshot.TileSnap t = new GameSnapshot.TileSnap();
            t.x = tile.getCoordinates().getX();
            t.y = tile.getCoordinates().getY();
            t.type = tile.getType().name();
            t.elevation = tile.getElevation();
            NaturalResourceNode node = tile.getNaturalResource();
            if (node != null) {
                t.resourceType = node.getType().name();
                t.resourceQuantity = node.getQuantity();
            }
            snap.tiles.add(t);
        }

        for (Building b : state.getBuildings()) {
            if (b.isDestroyed()) continue;
            snap.buildings.add(toBuildingSnap(b));
        }

        for (Flag flag : state.getRoadNetwork().getAllFlags()) {
            if (flag.isDestroyed() || flag.getBuilding() != null) continue;
            GameSnapshot.FlagSnap f = new GameSnapshot.FlagSnap();
            f.id = flag.getId();
            f.playerId = flag.getPlayerId();
            f.x = flag.getCoordinates().getX();
            f.y = flag.getCoordinates().getY();
            snap.flags.add(f);
        }

        for (Road road : state.getRoadNetwork().getAllRoads()) {
            GameSnapshot.RoadSnap r = new GameSnapshot.RoadSnap();
            r.startFlagId = road.getStartFlag().getId();
            r.endFlagId = road.getEndFlag().getId();
            for (Coordinates c : road.getPath()) {
                r.path.add(new double[]{c.getX(), c.getY()});
            }
            r.level = road.getLevel();
            r.transportCount = road.getTransportCount();
            snap.roads.add(r);
        }

        // Player economy/military preferences.
        state.getDistributionPriorities().forEach((playerId, table) -> {
            Map<String, List<String>> t = new HashMap<>();
            table.forEach((good, order) -> {
                List<String> names = new ArrayList<>();
                for (BuildingName bn : order) names.add(bn.name());
                t.put(good.name(), names);
            });
            snap.distributionPriorities.put(playerId, t);
        });
        snap.militaryOccupation = new HashMap<>(state.getMilitaryOccupation());

        return snap;
    }

    /**
     * Serializes a single building.
     *
     * @param b the building
     * @return its snapshot
     */
    private static GameSnapshot.BuildingSnap toBuildingSnap(Building b) {
        GameSnapshot.BuildingSnap s = new GameSnapshot.BuildingSnap();
        s.type = (b instanceof ConstructionSite site ? site.getTargetBuildingType() : b.getName()).name();
        s.playerId = b.getPlayerId();
        s.x = b.getPosition().getX();
        s.y = b.getPosition().getY();
        s.flagId = b.getAttachedFlag() != null ? b.getAttachedFlag().getId() : UUID.randomUUID();
        if (b instanceof StorageBuilding sb) {
            s.storedResources = new HashMap<>();
            sb.getStoredResources().forEach((type, qty) -> s.storedResources.put(type.name(), qty));
        }
        if (b instanceof MilitaryBuilding mb) {
            s.garrisonRanks = new ArrayList<>();
            for (Soldier soldier : mb.getSoldiers()) {
                s.garrisonRanks.add(soldier.getRank().name());
            }
            s.storedCoins = mb.getStoredCoins();
            s.coinsAllowed = mb.isCoinsAllowed();
        }
        if (b instanceof ProductionBuilding pb) {
            s.productionPaused = pb.isProductionPaused();
        }
        return s;
    }

    /**
     * Rebuilds a playable game state from a snapshot.
     *
     * @param snap   the snapshot to restore
     * @param gameId the identifier to assign the restored game
     * @return the reconstructed game state
     */
    public static GameState toGameState(GameSnapshot snap, UUID gameId) {
        GameState state = new GameState(gameId, new ArrayList<>());
        state.setPlayerCount(snap.playerCount);
        state.getAiPlayers().addAll(snap.aiPlayers);
        state.getEliminatedPlayers().addAll(snap.eliminatedPlayers);
        state.setWinnerPlayerId(snap.winnerPlayerId);
        state.setOver(snap.over);
        state.setCurrentTick(snap.tick);

        // Restore player economy/military preferences (older saves may omit these).
        if (snap.distributionPriorities != null) {
            snap.distributionPriorities.forEach((playerId, table) -> {
                Map<ResourceType, List<BuildingName>> t = new HashMap<>();
                table.forEach((good, order) -> {
                    List<BuildingName> names = new ArrayList<>();
                    for (String n : order) names.add(BuildingName.valueOf(n));
                    t.put(ResourceType.valueOf(good), names);
                });
                state.getDistributionPriorities().put(playerId, t);
            });
        }
        if (snap.militaryOccupation != null) {
            snap.militaryOccupation.forEach(state::setMilitaryOccupationOf);
        }

        Map<Coordinates, MapTile> tiles = new HashMap<>();
        for (GameSnapshot.TileSnap t : snap.tiles) {
            Coordinates coord = new Coordinates(t.x, t.y);
            MapTile tile = new MapTile(coord, TileType.valueOf(t.type), t.elevation);
            if (t.resourceType != null && t.resourceQuantity != null) {
                tile.setNaturalResource(new NaturalResourceNode(
                        ResourceType.valueOf(t.resourceType), t.resourceQuantity));
            }
            tiles.put(coord, tile);
        }
        state.setMapTiles(tiles);

        Map<UUID, Flag> flagMap = new HashMap<>();

        for (GameSnapshot.BuildingSnap bs : snap.buildings) {
            restoreBuilding(state, bs, flagMap);
        }

        for (GameSnapshot.FlagSnap fs : snap.flags) {
            Flag flag = new Flag(fs.id, fs.playerId, new Coordinates(fs.x, fs.y));
            state.getFlags().add(flag);
            state.getRoadNetwork().addFlag(flag);
            flagMap.put(fs.id, flag);
        }

        for (GameSnapshot.RoadSnap rs : snap.roads) {
            Flag start = flagMap.get(rs.startFlagId);
            Flag end = flagMap.get(rs.endFlagId);
            if (start == null || end == null) continue;
            List<Coordinates> path = new ArrayList<>();
            for (double[] p : rs.path) {
                path.add(new Coordinates(p[0], p[1]));
            }
            Road road = state.getRoadNetwork().addRoad(start, end, path);
            road.setLevel(rs.level);
            road.setTransportCount(rs.transportCount);
        }

        state.getTerritoryManager().recalculate(state);
        return state;
    }

    /**
     * Recreates a single building, its attached flag, stock, garrison and
     * specialist occupant, and registers them in the state.
     *
     * @param state   the state being rebuilt
     * @param bs      the building snapshot
     * @param flagMap the id → flag index being populated
     */
    private static void restoreBuilding(GameState state, GameSnapshot.BuildingSnap bs,
                                        Map<UUID, Flag> flagMap) {
        BuildingName type = BuildingName.valueOf(bs.type);
        Coordinates pos = new Coordinates(bs.x, bs.y);
        Building building = BuildingFactory.createBuilding(type, bs.playerId, pos, state);

        Flag flag = new Flag(bs.flagId, bs.playerId, pos);
        flag.setBuilding(building);
        building.setAttachedFlag(flag);
        state.getRoadNetwork().addFlag(flag);
        flagMap.put(bs.flagId, flag);

        if (building instanceof StorageBuilding sb && bs.storedResources != null) {
            sb.getStoredResources().clear();
            bs.storedResources.forEach((name, qty) ->
                    sb.getStoredResources().put(ResourceType.valueOf(name), qty));
        }
        if (building instanceof MilitaryBuilding mb && bs.garrisonRanks != null) {
            for (String rank : bs.garrisonRanks) {
                Soldier soldier = new Soldier(bs.playerId,
                        new Coordinates(pos.getX(), pos.getY()));
                soldier.setRank(SoldierRank.valueOf(rank));
                soldier.setHealth(soldier.getRank().getMaxHealth());
                soldier.setState(SoldierState.GARRISONED);
                soldier.setGarrison(mb);
                mb.addSoldier(soldier);
            }
            mb.setStoredCoins(bs.storedCoins);
            mb.setTerritoryClaimed(!mb.getSoldiers().isEmpty());
            if (bs.coinsAllowed != null) {
                mb.setCoinsAllowed(bs.coinsAllowed);
            }
        }
        if (building instanceof ProductionBuilding pb) {
            WorkerType role = BuildingFactory.occupantRoleFor(type);
            if (role != null) {
                Worker occupant = new Worker(bs.playerId);
                occupant.setType(role);
                occupant.setPosition(new Coordinates(pos.getX(), pos.getY()));
                occupant.setState(WorkerState.WORKING);
                pb.setOccupant(occupant);
                state.getWorkers().add(occupant);
            }
            if (bs.productionPaused != null) {
                pb.setProductionPaused(bs.productionPaused);
            }
        }

        state.getBuildings().add(building);
    }
}
