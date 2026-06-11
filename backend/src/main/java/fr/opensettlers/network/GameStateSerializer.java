package fr.opensettlers.network;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.opensettlers.engine.GameConfig;
import fr.opensettlers.engine.GameState;
import fr.opensettlers.engine.state.*;
import fr.opensettlers.network.dto.GameStateDto;
import fr.opensettlers.network.dto.MapDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts the in-memory game state into the JSON payloads sent to clients.
 *
 * <p>The full map is serialized once per connection ({@link #serializeMap}),
 * while the dynamic state is serialized every tick ({@link #serializeState}).
 * All coordinates are double-height hexagonal coordinates.</p>
 */
public final class GameStateSerializer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GameStateSerializer() {}

    /**
     * Serializes the dynamic game state (buildings, flags, roads, units, territory).
     *
     * @param state the game state to serialize
     * @return the JSON payload
     */
    public static String serializeState(GameState state) {
        List<GameStateDto.BuildingDto> buildings = new ArrayList<>();
        for (Building b : state.getBuildings()) {
            buildings.add(toBuildingDto(b));
        }

        List<GameStateDto.FlagDto> flags = new ArrayList<>();
        for (Flag f : state.getRoadNetwork().getAllFlags()) {
            if (f.isDestroyed()) continue;
            List<String> resources = f.getResourceSlots().stream()
                    .map(rs -> rs.getType().name())
                    .toList();
            flags.add(new GameStateDto.FlagDto(
                    f.getId(), f.getPlayerId(),
                    f.getCoordinates().getX(), f.getCoordinates().getY(),
                    resources));
        }

        List<GameStateDto.RoadDto> roads = new ArrayList<>();
        for (Road r : state.getRoadNetwork().getAllRoads()) {
            GameStateDto.CarrierDto carrierDto = null;
            Carrier c = r.getCarrier();
            if (c != null) {
                carrierDto = new GameStateDto.CarrierDto(
                        c.getId(), c.getProgress(), c.getState().name(),
                        c.isCarrying() ? c.getCarriedResource().getType().name() : null);
            }
            List<double[]> path = r.getPath().stream()
                    .map(p -> new double[]{p.getX(), p.getY()})
                    .toList();
            roads.add(new GameStateDto.RoadDto(
                    r.getId(), r.getStartFlag().getId(), r.getEndFlag().getId(), path, carrierDto));
        }

        List<GameStateDto.WorkerDto> workers = new ArrayList<>();
        for (Worker w : state.getWorkers()) {
            workers.add(new GameStateDto.WorkerDto(
                    w.getId(), w.getPlayerId(),
                    w.getType() != null ? w.getType().name() : null,
                    w.getPosition() != null ? w.getPosition().getX() : 0,
                    w.getPosition() != null ? w.getPosition().getY() : 0,
                    w.getState() != null ? w.getState().name() : null));
        }

        List<GameStateDto.SoldierDto> soldiers = new ArrayList<>();
        for (Soldier s : state.getSoldiers()) {
            soldiers.add(new GameStateDto.SoldierDto(
                    s.getId(), s.getPlayerId(),
                    s.getPosition().getX(), s.getPosition().getY(),
                    s.getHealth(), s.getState() != null ? s.getState().name() : null));
        }

        List<int[]> territory = new ArrayList<>();
        for (MapTile tile : state.getMapTiles().values()) {
            int owner = state.getTerritoryManager().getOwnerAt(tile.getCoordinates());
            if (owner >= 0) {
                territory.add(new int[]{
                        (int) tile.getCoordinates().getX(),
                        (int) tile.getCoordinates().getY(),
                        owner});
            }
        }

        GameStateDto dto = new GameStateDto(
                "STATE", state.getCurrentTick(),
                buildings, flags, roads, workers, soldiers, territory);
        return write(dto);
    }

    /**
     * Serializes the static map (terrain, elevation, natural resources).
     *
     * @param state the game state holding the map tiles
     * @return the JSON payload
     */
    public static String serializeMap(GameState state) {
        List<MapDto.TileDto> tiles = new ArrayList<>();
        for (MapTile tile : state.getMapTiles().values()) {
            NaturalResourceNode node = tile.getNaturalResource();
            tiles.add(new MapDto.TileDto(
                    (int) tile.getCoordinates().getX(),
                    (int) tile.getCoordinates().getY(),
                    tile.getType().name(), tile.getElevation(),
                    node != null ? node.getType().name() : null,
                    node != null ? node.getQuantity() : null));
        }
        return write(new MapDto("MAP", GameConfig.MAP_SIZE, tiles));
    }

    private static GameStateDto.BuildingDto toBuildingDto(Building b) {
        Integer groundwork = null;
        Integer progress = null;
        Integer productivity = null;
        Integer outputQuantity = null;
        Map<String, Integer> stored = null;
        Integer garrison = null;
        Integer maxGarrison = null;
        boolean underConstruction = false;

        if (b instanceof ConstructionSite site) {
            underConstruction = true;
            groundwork = site.getGroundworkProgress();
            progress = site.getBuildingProgress();
        } else if (b instanceof ProductionBuilding pb) {
            productivity = pb.getProductivity();
            outputQuantity = pb.getOutputSlot() != null ? pb.getOutputSlot().getQuantity() : null;
        } else if (b instanceof StorageBuilding sb) {
            stored = new HashMap<>();
            for (Map.Entry<fr.opensettlers.engine.state.utils.ResourceType, Integer> e : sb.getStoredResources().entrySet()) {
                stored.put(e.getKey().name(), e.getValue());
            }
        } else if (b instanceof MilitaryBuilding mb) {
            garrison = mb.getSoldiers().size();
            maxGarrison = mb.getMaxCapacity();
        }

        return new GameStateDto.BuildingDto(
                b.getId(),
                b.getName() != null ? b.getName().name() : null,
                b.getPlayerId(),
                b.getPosition().getX(), b.getPosition().getY(),
                underConstruction, groundwork, progress,
                productivity, outputQuantity, stored, garrison, maxGarrison);
    }

    private static String write(Object dto) {
        try {
            return MAPPER.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize game state", e);
        }
    }
}
