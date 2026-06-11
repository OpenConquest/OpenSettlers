package fr.opensettlers.network.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Snapshot of the dynamic game state broadcast to clients every tick.
 *
 * @param type      message discriminator, always {@code "STATE"}
 * @param tick      current game tick
 * @param buildings all buildings (including construction sites)
 * @param flags     all flags of the road network
 * @param roads     all roads with their carrier
 * @param workers   all worker units on the map
 * @param soldiers  all soldier units on the map
 * @param territory owned tiles as [x, y, playerId] triples (unowned tiles omitted)
 */
public record GameStateDto(
        String type,
        long tick,
        List<BuildingDto> buildings,
        List<FlagDto> flags,
        List<RoadDto> roads,
        List<WorkerDto> workers,
        List<SoldierDto> soldiers,
        List<int[]> territory
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
            Integer maxGarrison
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
     */
    public record RoadDto(UUID id, UUID startFlagId, UUID endFlagId, List<double[]> path, CarrierDto carrier) {}

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
     */
    public record SoldierDto(UUID id, int playerId, double x, double y, int health, String state) {}
}
