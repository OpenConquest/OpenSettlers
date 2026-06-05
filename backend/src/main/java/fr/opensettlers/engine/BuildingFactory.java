package fr.opensettlers.engine;

import fr.opensettlers.engine.state.*;
import fr.opensettlers.engine.state.utils.BuildingName;
import fr.opensettlers.engine.state.utils.Coordinates;
import fr.opensettlers.engine.state.utils.ResourceType;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory class to instantiate buildings without creating 13 different subclasses.
 */
public class BuildingFactory {

    public static Building createBuilding(BuildingName type, int playerId, Coordinates position) {
        return switch (type) {
            case HEADQUARTERS -> new StorageBuilding(playerId, position, new HashMap<>(Map.ofEntries(
                    Map.entry(ResourceType.PLANK, 40),
                    Map.entry(ResourceType.STONE, 30),
                    Map.entry(ResourceType.LOG, 10),
                    Map.entry(ResourceType.WHEAT, 5))));
            case WAREHOUSE -> new StorageBuilding(playerId, position, new HashMap<>());

            // --- RAW EXTRACTORS ---
            case WOODCUTTER -> new RawExtractor(playerId, position, ResourceType.LOG, BuildingName.WOODCUTTER);
            case FORESTER -> new RawExtractor(playerId, position, null, BuildingName.FORESTER);
            case QUARRY -> new RawExtractor(playerId, position, ResourceType.STONE, BuildingName.QUARRY);
            case MINE -> new RawExtractor(playerId, position, ResourceType.IRON, BuildingName.MINE);
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
            
            // Storage & Others
            case GUARD_HOUSE, WATCH_TOWER, CASTLE, BARRACKS ->
                throw new UnsupportedOperationException("Factory for " + type + " not fully implemented yet.");
        };
    }

    private static Recipe getRecipe(ResourceType outputType) {
        return new Recipe(Recipe.RECIPES.get(outputType), outputType);
    }
}
