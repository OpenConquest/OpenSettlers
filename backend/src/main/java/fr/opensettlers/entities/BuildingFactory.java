package fr.opensettlers.entities;

import fr.opensettlers.entities.*;
import fr.opensettlers.state.GameState;
import fr.opensettlers.utils.BuildingName;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.GameConfig;
import fr.opensettlers.utils.ResourceType;
import fr.opensettlers.utils.WorkerType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory class to instantiate buildings without creating one subclass per type.
 */
public class BuildingFactory {

    /**
     * Initial stock of a headquarters, mirroring a Settlers II starting supply:
     * construction materials, weapons and provisions to bootstrap the economy.
     */
    private static final Map<ResourceType, Integer> HEADQUARTERS_STOCK = Map.ofEntries(
            Map.entry(ResourceType.PLANK, 40),
            Map.entry(ResourceType.STONE, 30),
            Map.entry(ResourceType.LOG, 10),
            Map.entry(ResourceType.WHEAT, 5),
            Map.entry(ResourceType.IRON, 8),
            Map.entry(ResourceType.COAL, 8),
            Map.entry(ResourceType.FISH, 10),
            Map.entry(ResourceType.BREAD, 5),
            Map.entry(ResourceType.TOOL, 30),
            Map.entry(ResourceType.SWORD, 6),
            Map.entry(ResourceType.SHIELD, 6),
            Map.entry(ResourceType.BEER, 6));

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
            case HEADQUARTERS -> new StorageBuilding(playerId, position, new HashMap<>(HEADQUARTERS_STOCK));
            case WAREHOUSE -> new StorageBuilding(playerId, position, new HashMap<>());

            // --- NAVAL ---
            // A harbor is a coastal warehouse that can launch sea expeditions.
            case HARBOR -> new StorageBuilding(playerId, position, new HashMap<>());
            case SHIPYARD -> new ShipyardBuilding(playerId, position);

            // --- RAW EXTRACTORS ---
            case WOODCUTTER -> new RawExtractor(playerId, position, ResourceType.LOG, BuildingName.WOODCUTTER);
            case FORESTER -> new RawExtractor(playerId, position, null, BuildingName.FORESTER);
            case QUARRY -> new RawExtractor(playerId, position, ResourceType.STONE, BuildingName.QUARRY);
            case MINE -> new RawExtractor(playerId, position, resolveMineOre(position, state), BuildingName.MINE);
            case FISHING_HUT -> new RawExtractor(playerId, position, ResourceType.FISH, BuildingName.FISHING_HUT);
            case HUNTERS_HUT -> new RawExtractor(playerId, position, ResourceType.MEAT, BuildingName.HUNTERS_HUT);
            case FARM -> new RawExtractor(playerId, position, ResourceType.WHEAT, BuildingName.FARM);
            case WATER_WELL -> new RawExtractor(playerId, position, ResourceType.WATER, BuildingName.WATER_WELL);

            // --- PROCESSING BUILDINGS ---
            case SAWMILL -> new ProcessingBuilding(playerId, position, getRecipe(ResourceType.PLANK));
            case MILL -> new ProcessingBuilding(playerId, position, getRecipe(ResourceType.FLOUR));
            case BAKERY -> new ProcessingBuilding(playerId, position, getRecipe(ResourceType.BREAD));
            case PIG_FARM -> new ProcessingBuilding(playerId, position, getRecipe(ResourceType.PIG));
            case SLAUGHTERHOUSE -> new ProcessingBuilding(playerId, position, getRecipe(ResourceType.MEAT));
            case DONKEY_BREEDER -> new ProcessingBuilding(playerId, position, getRecipe(ResourceType.DONKEY));
            case FOUNDRY -> new ProcessingBuilding(playerId, position, getRecipe(ResourceType.STEEL));
            case MINT -> new ProcessingBuilding(playerId, position, getRecipe(ResourceType.COIN));
            case METALWORKS -> new ProcessingBuilding(playerId, position, getRecipe(ResourceType.TOOL));
            case BREWERY -> new ProcessingBuilding(playerId, position, getRecipe(ResourceType.BEER));
            // The armory alternates between swords and shields, as in Settlers II
            case ARMORY -> new ProcessingBuilding(playerId, position,
                    List.of(getRecipe(ResourceType.SWORD), getRecipe(ResourceType.SHIELD)));

            // --- MILITARY BUILDINGS ---
            case BARRACKS, GUARD_HOUSE, WATCH_TOWER, CASTLE, FORTRESS -> new MilitaryBuilding(playerId, position, type,
                    GameConfig.militaryCapacity(type),
                    GameConfig.militaryRadius(type));

            // --- SIEGE ---
            case CATAPULT -> new CatapultBuilding(playerId, position);
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
        for (ResourceType ore : List.of(ResourceType.IRON, ResourceType.COAL, ResourceType.GOLD, ResourceType.STONE)) {
            // The mine may sit directly on the deposit
            MapTile own = state.getTile(position);
            if (own != null && own.getNaturalResource() != null
                    && own.getNaturalResource().getType() == ore
                    && !own.getNaturalResource().isDepleted()) {
                return ore;
            }
            List<MapTile> deposits = state.findResourceTilesInRange(position, GameConfig.MINER_MAX_DISTANCE, ore);
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

    /**
     * Builds the production recipe yielding the given output resource, pairing
     * its input requirements (from {@link Recipe#RECIPES}) with the output type.
     *
     * @param outputType the resource the processing building produces
     * @return the recipe for that output
     */
    private static Recipe getRecipe(ResourceType outputType) {
        return new Recipe(Recipe.RECIPES.get(outputType), outputType);
    }

    /**
     * Maps a building type to the specialist worker that occupies it once built.
     *
     * @param name the building type
     * @return the occupying worker role, or {@code null} for buildings without a
     *         specialist occupant (storage, military, naval)
     */
    public static WorkerType occupantRoleFor(BuildingName name) {
        return switch (name) {
            case WOODCUTTER -> WorkerType.WOODCUTTER;
            case FORESTER -> WorkerType.FORESTER;
            case QUARRY -> WorkerType.QUARRYMAN;
            case MINE -> WorkerType.MINER;
            case FISHING_HUT -> WorkerType.FISHERMAN;
            case HUNTERS_HUT -> WorkerType.HUNTER;
            case FARM -> WorkerType.FARMER;
            case SAWMILL -> WorkerType.CARPENTER;
            case MILL -> WorkerType.MILLER;
            case BAKERY -> WorkerType.BAKER;
            case PIG_FARM -> WorkerType.PIG_BREEDER;
            case SLAUGHTERHOUSE -> WorkerType.BUTCHER;
            case DONKEY_BREEDER -> WorkerType.DONKEY_BREEDER;
            case BREWERY -> WorkerType.BREWER;
            case FOUNDRY -> WorkerType.SMELTER;
            case ARMORY -> WorkerType.SMITH;
            case MINT -> WorkerType.MINTER;
            case METALWORKS -> WorkerType.METALWORKER;
            case CATAPULT -> WorkerType.HELPER;
            default -> null;
        };
    }
}
