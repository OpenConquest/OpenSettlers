package fr.opensettlers.engine;

import fr.opensettlers.engine.state.*;
import fr.opensettlers.engine.state.utils.BuildingName;
import fr.opensettlers.engine.state.utils.Coordinates;
import fr.opensettlers.engine.state.utils.ResourceType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory class to instantiate buildings without creating 13 different subclasses.
 */
public class BuildingFactory {

    /**
     * Creates a building without map context. Mines default to iron extraction.
     *
     * @param type     the building type to create
     * @param playerId owning player ID
     * @param position map coordinates
     * @return the created building
     */
    public static Building createBuilding(BuildingName type, int playerId, Coordinates position) {
        return createBuilding(type, playerId, position, null);
    }

    /**
     * Creates a building, using the game state to resolve context-dependent details
     * (e.g. which ore a mine extracts based on the closest mountain deposit).
     *
     * @param type     the building type to create
     * @param playerId owning player ID
     * @param position map coordinates
     * @param state    the game state for map access, or {@code null} if unavailable
     * @return the created building
     */
    public static Building createBuilding(BuildingName type, int playerId, Coordinates position, GameState state) {
        Building building = switch (type) {
            case HEADQUARTERS -> new StorageBuilding(playerId, position, new HashMap<>(Map.ofEntries(
                    Map.entry(ResourceType.PLANK, 40),
                    Map.entry(ResourceType.STONE, 30),
                    Map.entry(ResourceType.LOG, 10),
                    Map.entry(ResourceType.WHEAT, 5),
                    Map.entry(ResourceType.SWORD, 6))));
            case WAREHOUSE -> new StorageBuilding(playerId, position, new HashMap<>());

            // --- RAW EXTRACTORS ---
            case WOODCUTTER -> new RawExtractor(playerId, position, ResourceType.LOG, BuildingName.WOODCUTTER);
            case FORESTER -> new RawExtractor(playerId, position, null, BuildingName.FORESTER);
            case QUARRY -> new RawExtractor(playerId, position, ResourceType.STONE, BuildingName.QUARRY);
            case MINE -> new RawExtractor(playerId, position, resolveMineOre(position, state), BuildingName.MINE);
            case FISHING_HUT -> new RawExtractor(playerId, position, ResourceType.FISH, BuildingName.FISHING_HUT);
            case FARM -> new RawExtractor(playerId, position, ResourceType.WHEAT, BuildingName.FARM);
            case WATER_WELL -> new RawExtractor(playerId, position, ResourceType.WATER, BuildingName.WATER_WELL);

            // --- PROCESSING BUILDINGS ---
            case SAWMILL -> new ProcessingBuilding(playerId, position, getRecipe(ResourceType.PLANK));
            case MILL -> new ProcessingBuilding(playerId, position, getRecipe(ResourceType.FLOUR));
            case BAKERY -> new ProcessingBuilding(playerId, position, getRecipe(ResourceType.BREAD));
            case FOUNDRY -> new ProcessingBuilding(playerId, position, getRecipe(ResourceType.STEEL));
            case ARMORY -> new ProcessingBuilding(playerId, position, getRecipe(ResourceType.SWORD));
            case BREWERY -> new ProcessingBuilding(playerId, position, getRecipe(ResourceType.BEER));

            // --- MILITARY BUILDINGS ---
            case BARRACKS, GUARD_HOUSE, WATCH_TOWER, CASTLE -> new MilitaryBuilding(playerId, position, type,
                    GameConfig.militaryCapacity(type),
                    GameConfig.militaryRadius(type));
        };
        building.setName(type);
        return building;
    }

    /**
     * Determines which ore a mine extracts from the closest non-depleted deposit
     * around it. Defaults to iron if the map is unavailable or no deposit is found.
     *
     * @param position the mine position
     * @param state    the game state, or {@code null}
     * @return the ore resource type the mine will extract
     */
    private static ResourceType resolveMineOre(Coordinates position, GameState state) {
        if (state == null) {
            return ResourceType.IRON;
        }
        ResourceType bestOre = ResourceType.IRON;
        int bestDist = Integer.MAX_VALUE;
        for (ResourceType ore : List.of(ResourceType.IRON, ResourceType.COAL, ResourceType.STONE)) {
            List<MapTile> deposits = state.findResourceTilesInRange(position, 3, ore);
            if (!deposits.isEmpty()) {
                int dist = position.distanceTo(deposits.getFirst().getCoordinates());
                if (dist < bestDist) {
                    bestDist = dist;
                    bestOre = ore;
                }
            }
        }
        return bestOre;
    }

    private static Recipe getRecipe(ResourceType outputType) {
        return new Recipe(Recipe.RECIPES.get(outputType), outputType);
    }
}
