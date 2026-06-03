package fr.opensettlers.engine;

import fr.opensettlers.entities.Building;
import fr.opensettlers.entities.ProcessingBuilding;
import fr.opensettlers.entities.RawExtractor;
import fr.opensettlers.entities.Recipe;
import fr.opensettlers.utils.BuildingName;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.ResourceType;

import java.util.UUID;

/**
 * Factory class to instantiate buildings without creating 13 different subclasses.
 */
public class BuildingFactory {

    public static Building createBuilding(BuildingName type, int playerId, Coordinates position) {
        UUID id = UUID.randomUUID();
        
        return switch (type) {
            // --- RAW EXTRACTORS ---
            case WOODCUTTER -> new RawExtractor(id, playerId, position, ResourceType.LOG);
            case FORESTER -> new RawExtractor(id, playerId, position, null); // No output inventory
            case QUARRY -> new RawExtractor(id, playerId, position, ResourceType.STONE);
            case MINE -> new RawExtractor(id, playerId, position, ResourceType.IRON); // TODO: Determine ore dynamically based on map tile
            case FISHING_HUT -> new RawExtractor(id, playerId, position, ResourceType.FISH);
            case FARM -> new RawExtractor(id, playerId, position, ResourceType.WHEAT);
            case WATER_WELL -> new RawExtractor(id, playerId, position, ResourceType.WATER);
            
            // --- PROCESSING BUILDINGS ---
            case SAWMILL -> new ProcessingBuilding(id, playerId, position, getRecipe(ResourceType.PLANK));
            case MILL -> new ProcessingBuilding(id, playerId, position, getRecipe(ResourceType.FLOUR));
            case BAKERY -> new ProcessingBuilding(id, playerId, position, getRecipe(ResourceType.BREAD));
            case FOUNDRY -> new ProcessingBuilding(id, playerId, position, getRecipe(ResourceType.STEEL));
            case ARMORY -> new ProcessingBuilding(id, playerId, position, getRecipe(ResourceType.SWORD));
            case BREWERY -> new ProcessingBuilding(id, playerId, position, getRecipe(ResourceType.BEER));
            
            // Storage & Others
            case HEADQUARTERS, WAREHOUSE, GUARD_HOUSE, WATCH_TOWER, CASTLE, BARRACKS -> 
                throw new UnsupportedOperationException("Factory for " + type + " not fully implemented yet.");
        };
    }

    private static Recipe getRecipe(ResourceType outputType) {
        return new Recipe(Recipe.RECIPES.get(outputType), outputType);
    }
}
