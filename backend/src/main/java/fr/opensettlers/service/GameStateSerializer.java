package fr.opensettlers.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.opensettlers.utils.BuildingName;
import fr.opensettlers.utils.GameConfig;
import fr.opensettlers.utils.ResourceType;
import fr.opensettlers.state.GameState;
import fr.opensettlers.entities.*;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.controller.dto.GameStateDto;
import fr.opensettlers.controller.dto.MapDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts the in-memory game state into the JSON payloads sent to clients.
 *
 * <p>The full map is serialized once per connection ({@link #serializeMap}),
 * while the dynamic state is serialized every tick ({@link #serializeState}).
 * All coordinates are double-height hexagonal coordinates.</p>
 *
 * <p>When a viewer player is given, the payload is filtered by that player's
 * fog of war: own entities are always included, everything else only when it
 * stands on an explored tile.</p>
 */
public final class GameStateSerializer {

    /** Shared JSON mapper used to serialize every outgoing snapshot. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Private constructor to prevent instantiation of this utility class. */
    private GameStateSerializer() {}

    /**
     * Serializes the full dynamic game state (spectator view, no fog of war).
     *
     * @param state the game state to serialize
     * @return the JSON payload
     */
    public static String serializeState(GameState state) {
        return serializeState(state, null);
    }

    /**
     * Serializes the dynamic game state (buildings, flags, roads, units,
     * territory, geologist signs) as seen by the given player.
     *
     * @param state    the game state to serialize
     * @param viewerId the receiving player, or {@code null} for the full view
     * @return the JSON payload
     */
    public static String serializeState(GameState state, Integer viewerId) {
        Set<Coordinates> explored = viewerId != null
                ? state.getFogOfWar().getExplored(viewerId)
                : null;

        List<GameStateDto.BuildingDto> buildings = new ArrayList<>();
        for (Building b : state.getBuildings()) {
            if (!isVisible(viewerId, explored, b.getPlayerId(), b.getPosition())) continue;
            buildings.add(toBuildingDto(b));
        }

        List<GameStateDto.FlagDto> flags = new ArrayList<>();
        for (Flag f : state.getRoadNetwork().getAllFlags()) {
            if (f.isDestroyed()) continue;
            if (!isVisible(viewerId, explored, f.getPlayerId(), f.getCoordinates())) continue;
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
            if (!isVisible(viewerId, explored, r.getStartFlag().getPlayerId(), r.getStartFlag().getCoordinates())
                    && !isVisible(viewerId, explored, r.getEndFlag().getPlayerId(), r.getEndFlag().getCoordinates())) {
                continue;
            }
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
                    r.getId(), r.getStartFlag().getId(), r.getEndFlag().getId(), path, carrierDto,
                    r.getLevel(), r.isDonkeyAssisted()));
        }

        List<GameStateDto.WorkerDto> workers = new ArrayList<>();
        for (Worker w : state.getWorkers()) {
            if (w.getPosition() != null
                    && !isVisible(viewerId, explored, w.getPlayerId(), w.getPosition())) continue;
            workers.add(new GameStateDto.WorkerDto(
                    w.getId(), w.getPlayerId(),
                    w.getType() != null ? w.getType().name() : null,
                    w.getPosition() != null ? w.getPosition().getX() : 0,
                    w.getPosition() != null ? w.getPosition().getY() : 0,
                    w.getState() != null ? w.getState().name() : null));
        }

        List<GameStateDto.SoldierDto> soldiers = new ArrayList<>();
        for (Soldier s : state.getSoldiers()) {
            if (!isVisible(viewerId, explored, s.getPlayerId(), s.getPosition())) continue;
            soldiers.add(new GameStateDto.SoldierDto(
                    s.getId(), s.getPlayerId(),
                    s.getPosition().getX(), s.getPosition().getY(),
                    s.getHealth(), s.getState() != null ? s.getState().name() : null,
                    s.getRank().name()));
        }

        List<GameStateDto.DonkeyDto> donkeys = new ArrayList<>();
        for (Donkey d : state.getDonkeys()) {
            if (d.getPosition() == null
                    || !isVisible(viewerId, explored, d.getPlayerId(), d.getPosition())) continue;
            donkeys.add(new GameStateDto.DonkeyDto(
                    d.getId(), d.getPlayerId(),
                    d.getPosition().getX(), d.getPosition().getY(),
                    d.getState() != null ? d.getState().name() : null));
        }

        List<GameStateDto.ShipDto> ships = new ArrayList<>();
        for (Ship ship : state.getShips()) {
            if (ship.getPosition() == null
                    || !isVisible(viewerId, explored, ship.getPlayerId(), ship.getPosition())) continue;
            ships.add(new GameStateDto.ShipDto(
                    ship.getId(), ship.getPlayerId(),
                    ship.getPosition().getX(), ship.getPosition().getY(),
                    ship.getState() != null ? ship.getState().name() : null));
        }

        List<int[]> territory = new ArrayList<>();
        List<GameStateDto.SignDto> signs = new ArrayList<>();
        List<GameStateDto.ResourceTileDto> resources = new ArrayList<>();
        for (MapTile tile : state.getMapTiles().values()) {
            boolean tileExplored = explored == null || explored.contains(tile.getCoordinates());
            if (!tileExplored) continue;

            int owner = state.getTerritoryManager().getOwnerAt(tile.getCoordinates());
            if (owner >= 0) {
                territory.add(new int[]{
                        (int) tile.getCoordinates().getX(),
                        (int) tile.getCoordinates().getY(),
                        owner});
            }
            if (tile.isSurveyed()) {
                signs.add(new GameStateDto.SignDto(
                        (int) tile.getCoordinates().getX(),
                        (int) tile.getCoordinates().getY(),
                        tile.getGeologistSign() != null ? tile.getGeologistSign().name() : null));
            }
            NaturalResourceNode node = tile.getNaturalResource();
            if (node != null && node.getQuantity() > 0) {
                resources.add(new GameStateDto.ResourceTileDto(
                        (int) tile.getCoordinates().getX(),
                        (int) tile.getCoordinates().getY(),
                        node.getType().name(), node.getQuantity()));
            }
        }

        List<int[]> exploredList = null;
        if (explored != null) {
            exploredList = new ArrayList<>(explored.size());
            for (Coordinates c : explored) {
                exploredList.add(new int[]{(int) c.getX(), (int) c.getY()});
            }
        }

        // The viewer's own distribution priorities (spectators get none).
        Map<String, List<String>> distribution = null;
        if (viewerId != null) {
            distribution = new HashMap<>();
            for (Map.Entry<ResourceType, List<BuildingName>> e
                    : state.getDistributionFor(viewerId).entrySet()) {
                List<String> order = new ArrayList<>(e.getValue().size());
                for (BuildingName bn : e.getValue()) {
                    order.add(bn.name());
                }
                distribution.put(e.getKey().name(), order);
            }
        }

        Integer militaryOccupation = viewerId != null ? state.getMilitaryOccupationOf(viewerId) : null;

        GameStateDto dto = new GameStateDto(
                "STATE", state.getCurrentTick(),
                buildings, flags, roads, workers, soldiers, donkeys, ships,
                territory, signs, resources, exploredList, distribution, militaryOccupation);
        return write(dto);
    }

    /**
     * Checks whether an entity is visible to the viewer: spectators see all,
     * players always see their own entities, and enemy entities only on
     * explored tiles.
     *
     * @param viewerId      the receiving player, or {@code null} for spectators
     * @param explored      the viewer's explored tiles ({@code null} for spectators)
     * @param ownerPlayerId the entity owner
     * @param position      the entity position
     * @return {@code true} if the entity must be serialized
     */
    private static boolean isVisible(Integer viewerId, Set<Coordinates> explored,
                                     int ownerPlayerId, Coordinates position) {
        if (viewerId == null) return true;
        if (ownerPlayerId == viewerId) return true;
        return explored.contains(position);
    }

    /**
     * Serializes the terminal {@code GAME_OVER} message announcing the winner
     * and the eliminated players.
     *
     * @param state the finished game state
     * @return the JSON payload
     */
    public static String serializeGameOver(GameState state) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("type", "GAME_OVER");
        dto.put("tick", state.getCurrentTick());
        dto.put("winner", state.getWinnerPlayerId());
        dto.put("eliminated", new ArrayList<>(state.getEliminatedPlayers()));
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

    /**
     * Converts a building to its wire DTO, flattening the type-specific state
     * (construction progress, production output, stored resources, garrison and
     * coins) into the nullable fields of {@link GameStateDto.BuildingDto}.
     *
     * @param b the building to serialize
     * @return the building DTO carrying only the fields relevant to its subtype
     */
    private static GameStateDto.BuildingDto toBuildingDto(Building b) {
        Integer groundwork = null;
        Integer progress = null;
        Integer productivity = null;
        Integer outputQuantity = null;
        Map<String, Integer> stored = null;
        Integer garrison = null;
        Integer maxGarrison = null;
        Integer coins = null;
        Boolean productionPaused = null;
        Boolean coinsAllowed = null;
        boolean underConstruction = false;

        if (b instanceof ConstructionSite site) {
            underConstruction = true;
            groundwork = site.getGroundworkProgress();
            progress = site.getBuildingProgress();
        } else if (b instanceof ProductionBuilding pb) {
            productivity = pb.getProductivity();
            outputQuantity = pb.getOutputSlot() != null ? pb.getOutputSlot().getQuantity() : null;
            productionPaused = pb.isProductionPaused();
        } else if (b instanceof StorageBuilding sb) {
            stored = new HashMap<>();
            for (Map.Entry<ResourceType, Integer> e : sb.getStoredResources().entrySet()) {
                stored.put(e.getKey().name(), e.getValue());
            }
            if (b instanceof Garrisoned g) {
                // The headquarters exposes its defending garrison too
                garrison = g.getSoldiers().size();
                maxGarrison = g.getMaxCapacity();
            }
        } else if (b instanceof MilitaryBuilding mb) {
            garrison = mb.getSoldiers().size();
            maxGarrison = mb.getMaxCapacity();
            coins = mb.getStoredCoins();
            coinsAllowed = mb.isCoinsAllowed();
        }

        return new GameStateDto.BuildingDto(
                b.getId(),
                b.getName() != null ? b.getName().name() : null,
                b.getPlayerId(),
                b.getPosition().getX(), b.getPosition().getY(),
                underConstruction, groundwork, progress,
                productivity, outputQuantity, stored, garrison, maxGarrison, coins,
                productionPaused, coinsAllowed);
    }

    /**
     * Serializes a DTO to its JSON string representation.
     *
     * @param dto the object to serialize
     * @return the JSON string
     * @throws IllegalStateException if serialization fails
     */
    private static String write(Object dto) {
        try {
            return MAPPER.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize game state", e);
        }
    }
}
