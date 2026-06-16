package fr.opensettlers.systems;

import fr.opensettlers.utils.GameConfig;
import fr.opensettlers.state.GameState;
import fr.opensettlers.entities.*;
import fr.opensettlers.utils.BuildingName;
import fr.opensettlers.utils.ResourceType;
import fr.opensettlers.utils.WorkerState;

import java.util.ArrayList;
import java.util.List;

/**
 * System managing resource transformation and worker productivity in production buildings.
 * Handles both ProcessingBuildings (transform inputs → outputs) and RawExtractors
 * (extract resources from the map using specialized worker behaviors).
 */
public class ProductionSystem implements ISystem {

    /**
     * Processes production cycles for all active buildings.
     * For RawExtractors, runs specialized worker behavior (resource finding, planting, etc.)
     * before the generic cooldown/production logic.
     *
     * @param gameState the active game session state
     */
    @Override
    public void process(GameState gameState) {
        List<ProductionBuilding> productionBuildings = new ArrayList<>();
        for (Building building : gameState.getBuildings()) {
            if (building instanceof ProductionBuilding pb && !pb.isDestroyed()) {
                productionBuildings.add(pb);
            }
        }

        for (ProductionBuilding pb : productionBuildings) {
            // Paused by the player: keep the occupant but produce nothing.
            if (pb.isProductionPaused()) {
                pb.setProductivity(0);
                pb.setWaitingTicks(0);
                Worker occ = pb.getOccupant();
                if (occ != null && occ.getState() == WorkerState.WORKING) {
                    occ.setState(WorkerState.WAITING);
                }
                continue;
            }

            Worker occupant = pb.getOccupant();
            if (occupant == null || (occupant.getState() != WorkerState.WORKING && occupant.getState() != WorkerState.WAITING)) {
                pb.setProductivity(0);
                pb.setWaitingTicks(0);
                continue;
            }

            // Ingest input resources from attached flag if slots have space
            Flag flag = pb.getAttachedFlag();
            if (pb.getInputSlots() != null && flag != null) {
                for (ResourceSlot slot : pb.getInputSlots()) {
                    if (slot.getQuantity() < slot.getMAX_PER_SLOT()) {
                        for (int i = 0; i < flag.getResourceSlots().size(); i++) {
                            ResourceStack rs = flag.getResourceSlots().get(i);
                            if (rs.getType() == slot.getType() && flag.getId().equals(rs.getTargetFlagId())) {
                                flag.getResourceSlots().remove(i);
                                slot.addResource();
                                break;
                            }
                        }
                    }
                }
            }

            // Run specialized RawExtractor behavior BEFORE generic production
            if (pb instanceof RawExtractor extractor) {
                processRawExtractor(extractor, gameState, occupant);
                // Forester is fully handled inside processRawExtractor — skip generic logic
                if (extractor.getBuildingName() == BuildingName.FORESTER) {
                    continue;
                }
            }

            // Generic production cooldown logic
            if (pb.canProduce()) {
                occupant.setState(WorkerState.WORKING);
                pb.setWaitingTicks(0);

                int cooldown = pb.getProductionCooldown();
                if (cooldown <= 0) {
                    pb.produce();
                    pb.setProductivity(Math.min(100, pb.getProductivity() + 10));
                    pb.setProductionCooldown(GameConfig.PRODUCTION_TIME);
                } else {
                    pb.setProductionCooldown(cooldown - 1);
                }
            } else {
                occupant.setState(WorkerState.WAITING);
                pb.setWaitingTicks(pb.getWaitingTicks() + 1);

                // Ticks for 10s
                if (pb.getWaitingTicks() >= 10000 / GameConfig.TICK_PERIOD_MS) {
                    pb.setProductivity(Math.max(0, pb.getProductivity() - 5));
                    pb.setWaitingTicks(0);
                }
            }
        }
    }

    /**
     * Dispatches to the correct worker behavior handler based on the building type.
     * Called before the generic production cooldown logic each tick.
     *
     * @param extractor the RawExtractor building to process
     * @param state     the active game state
     * @param occupant  the worker occupying this building
     */
    private void processRawExtractor(RawExtractor extractor, GameState state, Worker occupant) {
        switch (extractor.getBuildingName()) {
            case WOODCUTTER   -> handleWoodcutter(extractor, state);
            case FORESTER     -> handleForester(extractor, state, occupant);
            case QUARRY       -> handleQuarry(extractor, state);
            case FARM         -> handleFarmer(extractor, state);
            case FISHING_HUT  -> handleFisherman(extractor, state, occupant);
            case HUNTERS_HUT  -> handleHunter(extractor, state);
            case MINE         -> handleMiner(extractor, state);
            default -> {} // WATER_WELL — basic behavior, no special handling
        }
    }

    /**
     * Handles miner behavior: assigns the closest non-depleted deposit of the
     * mine's ore within {@link GameConfig#MINER_MAX_DISTANCE} (including the
     * tile the mine stands on) as the target work tile. Once every deposit in
     * range is dug out, the mine is exhausted and stops producing.
     *
     * @param extractor the mine building
     * @param state     the game state for map access
     */
    private void handleMiner(RawExtractor extractor, GameState state) {
        if (extractor.getTargetWorkTile() == null) {
            MapTile own = state.getTile(extractor.getPosition());
            if (own != null && own.getNaturalResource() != null
                    && own.getNaturalResource().getType() == extractor.getExtractedResource()
                    && !own.getNaturalResource().isDepleted()) {
                extractor.setTargetWorkTile(own);
            } else {
                List<MapTile> deposits = state.findResourceTilesInRange(
                        extractor.getPosition(),
                        GameConfig.MINER_MAX_DISTANCE,
                        extractor.getExtractedResource()
                );
                if (!deposits.isEmpty()) {
                    extractor.setTargetWorkTile(deposits.getFirst()); // closest first
                }
            }
        }

        // Validate existing target — clear if depleted
        validateTargetWorkTile(extractor);
    }

    /**
     * Handles hunter behavior: tracks the closest wild game (MEAT resource
     * node) within {@link GameConfig#HUNTER_MAX_DISTANCE} and assigns it as
     * the target work tile. Game does not respawn once hunted out.
     *
     * @param extractor the hunter's hut building
     * @param state     the game state for map access
     */
    private void handleHunter(RawExtractor extractor, GameState state) {
        if (extractor.getTargetWorkTile() == null) {
            List<MapTile> game = state.findResourceTilesInRange(
                    extractor.getPosition(),
                    GameConfig.HUNTER_MAX_DISTANCE,
                    ResourceType.MEAT
            );
            if (!game.isEmpty()) {
                extractor.setTargetWorkTile(game.getFirst()); // closest first
            }
        }

        // Validate existing target — clear if depleted
        validateTargetWorkTile(extractor);
    }

    /**
     * Handles woodcutter behavior: searches for the closest tree (FOREST tile with LOG resource)
     * within {@link GameConfig#WOODCUTTER_MAX_DISTANCE} and assigns it as the target work tile.
     * When the tree is cut (produce()), it is removed from the map and the woodcutter searches
     * for the next tree.
     *
     * @param extractor the woodcutter building
     * @param state     the game state for map access
     */
    private void handleWoodcutter(RawExtractor extractor, GameState state) {
        // Search for a target tree if none assigned
        if (extractor.getTargetWorkTile() == null) {
            List<MapTile> trees = state.findResourceTilesInRange(
                    extractor.getPosition(),
                    GameConfig.WOODCUTTER_MAX_DISTANCE,
                    ResourceType.LOG
            );
            if (!trees.isEmpty()) {
                extractor.setTargetWorkTile(trees.getFirst()); // closest first (BFS order)
            }
        }

        // Validate existing target — clear if depleted
        validateTargetWorkTile(extractor);
    }

    /**
     * Handles forester behavior: searches for empty GRASS tiles within
     * {@link GameConfig#FORESTER_MAX_DISTANCE} and plants new trees on them.
     * The forester has no output slot — it "produces" by modifying the map directly.
     * This method fully manages the production cycle (cooldown + planting), so the
     * generic production logic is skipped for foresters.
     *
     * @param extractor the forester building
     * @param state     the game state for map access
     * @param occupant  the worker occupying this building
     */
    private void handleForester(RawExtractor extractor, GameState state, Worker occupant) {
        int cooldown = extractor.getProductionCooldown();
        if (cooldown > 0) {
            extractor.setProductionCooldown(cooldown - 1);
            occupant.setState(WorkerState.WORKING);
            return;
        }

        // Find an empty grass tile to plant a tree on
        List<MapTile> emptyGrass = state.findEmptyGrassTilesInRange(
                extractor.getPosition(),
                GameConfig.FORESTER_MAX_DISTANCE
        );

        if (!emptyGrass.isEmpty()) {
            MapTile target = emptyGrass.getFirst();
            boolean planted = target.replantTree(new NaturalResourceNode(ResourceType.LOG, 5));
            if (planted) {
                extractor.setProductionCooldown(GameConfig.PRODUCTION_TIME);
                extractor.setProductivity(Math.min(100, extractor.getProductivity() + 10));
                extractor.setWaitingTicks(0);
                occupant.setState(WorkerState.WORKING);
            }
        } else {
            // No space to plant — waiting
            occupant.setState(WorkerState.WAITING);
            extractor.setWaitingTicks(extractor.getWaitingTicks() + 1);
            if (extractor.getWaitingTicks() >= 10000 / GameConfig.TICK_PERIOD_MS) {
                extractor.setProductivity(Math.max(0, extractor.getProductivity() - 5));
                extractor.setWaitingTicks(0);
            }
        }
    }

    /**
     * Handles quarry behavior: searches for the closest STONE resource tile
     * within {@link GameConfig#QUARRYMAN_MAX_DISTANCE}. Stone does not regrow.
     *
     * @param extractor the quarry building
     * @param state     the game state for map access
     */
    private void handleQuarry(RawExtractor extractor, GameState state) {
        // Search for a target stone deposit if none assigned
        if (extractor.getTargetWorkTile() == null) {
            List<MapTile> stones = state.findResourceTilesInRange(
                    extractor.getPosition(),
                    GameConfig.QUARRYMAN_MAX_DISTANCE,
                    ResourceType.STONE
            );
            if (!stones.isEmpty()) {
                extractor.setTargetWorkTile(stones.getFirst()); // closest first
            }
        }

        // Validate existing target — clear if depleted
        validateTargetWorkTile(extractor);
    }

    /**
     * Handles farmer behavior:
     * <ol>
     *   <li>On first ticks: plants up to {@link GameConfig#FARMER_MAX_FIELDS} wheat fields
     *       on nearby empty GRASS tiles (within distance 3 of the farm).</li>
     *   <li>Searches managed fields for a harvestable one and assigns it as targetWorkTile.</li>
     *   <li>After a field is fully harvested (depleted), replants it with fresh wheat.</li>
     * </ol>
     *
     * @param extractor the farm building
     * @param state     the game state for map access
     */
    private void handleFarmer(RawExtractor extractor, GameState state) {
        // Phase 1: Setup fields if not yet at capacity
        if (extractor.getManagedFields().size() < GameConfig.FARMER_MAX_FIELDS) {
            List<MapTile> emptyGrass = state.findEmptyGrassTilesInRange(
                    extractor.getPosition(),
                    3  // Fields must be very close to the farm
            );
            for (MapTile tile : emptyGrass) {
                if (extractor.getManagedFields().size() >= GameConfig.FARMER_MAX_FIELDS) break;
                if (tile.plantField(new NaturalResourceNode(ResourceType.WHEAT, 3))) {
                    extractor.getManagedFields().add(tile);
                }
            }
        }

        // Phase 2: Find a harvestable field if no target assigned
        if (extractor.getTargetWorkTile() == null) {
            for (MapTile field : extractor.getManagedFields()) {
                if (field.getNaturalResource() != null
                        && field.getNaturalResource().getQuantity() > 0) {
                    extractor.setTargetWorkTile(field);
                    break;
                }
            }
        }

        // Phase 3: After harvest completion, replant the field
        if (extractor.getTargetWorkTile() != null) {
            MapTile target = extractor.getTargetWorkTile();
            if (target.getNaturalResource() == null || target.getNaturalResource().isDepleted()) {
                // Replant the field with fresh wheat
                target.setNaturalResource(new NaturalResourceNode(ResourceType.WHEAT, 3));
                extractor.setTargetWorkTile(null); // Will pick a new field next tick
            }
        }
    }

    /**
     * Handles fisherman behavior: the fishing hut must be within
     * {@link GameConfig#FISHERMAN_MAX_DISTANCE} tiles of water to function.
     * Searches for WATER tiles with FISH resources and assigns them as targets.
     *
     * @param extractor the fishing hut building
     * @param state     the game state for map access
     * @param occupant  the worker occupying this building
     */
    private void handleFisherman(RawExtractor extractor, GameState state, Worker occupant) {
        // Verify water proximity — if no water within range, fisherman can't work
        if (!state.hasWaterInRange(extractor.getPosition(), GameConfig.FISHERMAN_MAX_DISTANCE)) {
            occupant.setState(WorkerState.WAITING);
            return; // Can't fish without water nearby
        }

        // Find a fish resource tile if none assigned
        if (extractor.getTargetWorkTile() == null) {
            List<MapTile> fishTiles = state.findResourceTilesInRange(
                    extractor.getPosition(),
                    GameConfig.FISHERMAN_MAX_DISTANCE,
                    ResourceType.FISH
            );
            if (!fishTiles.isEmpty()) {
                extractor.setTargetWorkTile(fishTiles.getFirst()); // closest first
            }
        }

        // Validate existing target — clear if depleted
        validateTargetWorkTile(extractor);
    }

    /**
     * Validates the current targetWorkTile of a RawExtractor.
     * Clears the target if the resource is null or depleted.
     *
     * @param extractor the RawExtractor to validate
     */
    private void validateTargetWorkTile(RawExtractor extractor) {
        if (extractor.getTargetWorkTile() != null) {
            MapTile target = extractor.getTargetWorkTile();
            if (target.getNaturalResource() == null || target.getNaturalResource().isDepleted()) {
                extractor.setTargetWorkTile(null);
            }
        }
    }
}
