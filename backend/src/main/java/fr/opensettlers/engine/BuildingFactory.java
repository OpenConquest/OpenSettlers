package fr.opensettlers.engine;

import fr.opensettlers.engine.state.*;
import fr.opensettlers.engine.state.utils.BuildingName;
import fr.opensettlers.engine.state.utils.Coordinates;
import fr.opensettlers.engine.state.utils.ResourceType;
import fr.opensettlers.engine.state.utils.TileType;

import java.util.HashMap;
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
     * Creates a building, using the map to resolve context-dependent details
     * (e.g. which ore a mine extracts based on the underlying mountain deposit).
     *
     * @param type     the building type to create
     * @param playerId owning player ID
     * @param position map coordinates
     * @param map      the game map, or {@code null} if unavailable
     * @return the created building
     */
    public static Building createBuilding(BuildingName type, int playerId, Coordinates position, GameMap map) {
        Building building = switch (type) {
            case HEADQUARTERS -> new StorageBuilding(playerId, position, new HashMap<>(Map.ofEntries(
                    Map.entry(ResourceType.PLANK, 40),
                    Map.entry(ResourceType.STONE, 30),
                    Map.entry(ResourceType.LOG, 10),
                    Map.entry(ResourceType.WHEAT, 5),
                    Map.entry(ResourceType.SWORD, 6))));
            case WAREHOUSE -> new StorageBuilding(playerId, position, new HashMap<>());

            // --- RAW EXTRACTORS ---
            case WOODCUTTER -> new RawExtractor(playerId, position, ResourceType.LOG);
            case FORESTER -> new RawExtractor(playerId, position, null); // No output inventory
            case QUARRY -> new RawExtractor(playerId, position, ResourceType.STONE);
            case MINE -> new RawExtractor(playerId, position, resolveMineOre(position, map));
            case FISHING_HUT -> new RawExtractor(playerId, position, ResourceType.FISH);
            case FARM -> new RawExtractor(playerId, position, ResourceType.WHEAT);
            case WATER_WELL -> new RawExtractor(playerId, position, ResourceType.WATER);

            // --- PROCESSING BUILDINGS ---
            case SAWMILL -> new ProcessingBuilding(playerId, position, getRecipe(ResourceType.PLANK));
            case MILL -> new ProcessingBuilding(playerId, position, getRecipe(ResourceType.FLOUR));
            case BAKERY -> new ProcessingBuilding(playerId, position, getRecipe(ResourceType.BREAD));
            case FOUNDRY -> new ProcessingBuilding(playerId, position, getRecipe(ResourceType.STEEL));
            case ARMORY -> new ProcessingBuilding(playerId, position, getRecipe(ResourceType.SWORD));
            case BREWERY -> new ProcessingBuilding(playerId, position, getRecipe(ResourceType.BEER));

            // --- MILITARY BUILDINGS ---
            case GUARD_HOUSE, WATCH_TOWER, CASTLE, BARRACKS ->
                    new MilitaryBuilding(playerId, position, type);
        };
        building.setName(type);
        return building;
    }

    /**
     * Determines which ore a mine extracts from the closest mountain deposit around it.
     * Defaults to iron if the map is unavailable or no deposit is found.
     *
     * @param position the mine position
     * @param map      the game map, or {@code null}
     * @return the ore resource type the mine will extract
     */
    private static ResourceType resolveMineOre(Coordinates position, GameMap map) {
        if (map == null) {
            return ResourceType.IRON;
        }
        MapTile deposit = map.findClosestTile(position, 3, tile ->
                tile.getType() == TileType.MOUNTAIN
                        && tile.getNaturalResource() != null
                        && !tile.getNaturalResource().isDepleted());
        if (deposit == null) {
            return ResourceType.IRON;
        }
        return deposit.getNaturalResource().getType();
    }

    private static Recipe getRecipe(ResourceType outputType) {
        return new Recipe(Recipe.RECIPES.get(outputType), outputType);
    }
}
