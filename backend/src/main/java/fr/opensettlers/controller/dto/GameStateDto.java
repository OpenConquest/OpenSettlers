package fr.opensettlers.controller.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Snapshot of the dynamic game state broadcast to clients every tick.
 * When sent to a player-bound connection, entities and tiles outside the
 * player's explored area are omitted (fog of war).
 *
 * @param type      message discriminator, always {@code "STATE"}
 * @param tick      current game tick
 * @param buildings all buildings (including construction sites)
 * @param flags     all flags of the road network
 * @param roads     all roads with their carrier
 * @param workers   all worker units on the map
 * @param soldiers  all soldier units on the map
 * @param donkeys   all donkey units on the map
 * @param ships     all ships sailing the seas
 * @param territory owned tiles as [x, y, playerId] triples (unowned tiles omitted)
 * @param signs     geologist signs as one entry per surveyed tile
 * @param resources live natural-resource nodes on explored tiles (trees, stone,
 *                  ore, fish, water) with their remaining quantity
 * @param explored  tiles explored by the receiving player as [x, y] pairs
 *                  ({@code null} for spectators, who see everything)
 * @param distribution the receiving player's distribution priority order per
 *                  contested good (good name → ordered consumer building names);
 *                  {@code null} for spectators
 * @param militaryOccupation the receiving player's target garrison occupation
 *                  percentage (0–100); {@code null} for spectators
 */
public record GameStateDto(
        String type,
        long tick,
        List<BuildingDto> buildings,
        List<FlagDto> flags,
        List<RoadDto> roads,
        List<WorkerDto> workers,
        List<SoldierDto> soldiers,
        List<DonkeyDto> donkeys,
        List<ShipDto> ships,
        List<int[]> territory,
        List<SignDto> signs,
        List<ResourceTileDto> resources,
        List<int[]> explored,
        Map<String, List<String>> distribution,
        Integer militaryOccupation
) {
    /**
     * A building as seen by the client.
     *
     * @param id                unique identifier
     * @param name              building type name
     * @param playerId          owning player
     * @param x                 horizontal position
     * @param y                 vertical position
     * @param underConstruction whether this is still a construction site
     * @param groundworkProgress groundwork progress (construction sites only)
     * @param buildingProgress  masonry progress (construction sites only)
     * @param productivity      productivity percent (production buildings only)
     * @param outputQuantity    output slot stock (production buildings only)
     * @param storedResources   stored stock (storage buildings only)
     * @param garrison          garrisoned soldier count (military buildings only)
     * @param maxGarrison       garrison capacity (military buildings only)
     * @param coins             stored gold coins (military buildings only)
     * @param productionPaused  whether production is paused (production buildings only)
     * @param coinsAllowed      whether coin delivery is enabled (military buildings only)
     */
    public record BuildingDto(
            UUID id,
            String name,
            int playerId,
            double x,
            double y,
            boolean underConstruction,
            Integer groundworkProgress,
            Integer buildingProgress,
            Integer productivity,
            Integer outputQuantity,
            Map<String, Integer> storedResources,
            Integer garrison,
            Integer maxGarrison,
            Integer coins,
            Boolean productionPaused,
            Boolean coinsAllowed
    ) {}

    /**
     * A flag of the road network.
     *
     * @param id        unique identifier
     * @param playerId  owning player
     * @param x         horizontal position
     * @param y         vertical position
     * @param resources resource types currently waiting on the flag
     */
    public record FlagDto(UUID id, int playerId, double x, double y, List<String> resources) {}

    /**
     * A road between two flags, with its carrier.
     *
     * @param id          unique identifier
     * @param startFlagId flag at one end
     * @param endFlagId   flag at the other end
     * @param path        intermediate path coordinates as [x, y] pairs
     * @param carrier     the carrier working this road, or {@code null}
     * @param level       road level (1 = track, 2 = main road)
     * @param hasDonkey   whether a donkey is assisting the carrier
     */
    public record RoadDto(UUID id, UUID startFlagId, UUID endFlagId, List<double[]> path, CarrierDto carrier,
                          int level, boolean hasDonkey) {}

    /**
     * A carrier working a road.
     *
     * @param id           unique identifier
     * @param progress     progress along the road (0 = start flag)
     * @param state        carrier state
     * @param carriedType  carried resource type, or {@code null} if hands are empty
     */
    public record CarrierDto(UUID id, int progress, String state, String carriedType) {}

    /**
     * A worker unit moving on the map.
     *
     * @param id       unique identifier
     * @param playerId owning player
     * @param workerType worker role, or {@code null} for a neutral settler
     * @param x        horizontal position
     * @param y        vertical position
     * @param state    worker state
     */
    public record WorkerDto(UUID id, int playerId, String workerType, double x, double y, String state) {}

    /**
     * A soldier unit.
     *
     * @param id       unique identifier
     * @param playerId owning player
     * @param x        horizontal position
     * @param y        vertical position
     * @param health   remaining health points
     * @param state    soldier state
     * @param rank     military rank name
     */
    public record SoldierDto(UUID id, int playerId, double x, double y, int health, String state, String rank) {}

    /**
     * A donkey unit.
     *
     * @param id       unique identifier
     * @param playerId owning player
     * @param x        horizontal position
     * @param y        vertical position
     * @param state    donkey state
     */
    public record DonkeyDto(UUID id, int playerId, double x, double y, String state) {}

    /**
     * A ship sailing the seas.
     *
     * @param id       unique identifier
     * @param playerId owning player
     * @param x        horizontal position
     * @param y        vertical position
     * @param state    ship state
     */
    public record ShipDto(UUID id, int playerId, double x, double y, String state) {}

    /**
     * A geologist sign on a surveyed tile.
     *
     * @param x        horizontal position
     * @param y        vertical position
     * @param resource ore found on this tile, or {@code null} for an empty sign
     */
    public record SignDto(int x, int y, String resource) {}

    /**
     * A live natural-resource node on an explored tile.
     *
     * @param x        horizontal position
     * @param y        vertical position
     * @param resource resource type provided by the node (e.g. {@code LOG}, {@code STONE})
     * @param quantity remaining harvestable quantity
     */
    public record ResourceTileDto(int x, int y, String resource, int quantity) {}
}
