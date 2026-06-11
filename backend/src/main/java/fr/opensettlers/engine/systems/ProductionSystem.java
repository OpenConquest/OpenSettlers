package fr.opensettlers.engine.systems;

import fr.opensettlers.engine.GameConfig;
import fr.opensettlers.engine.GameState;
import fr.opensettlers.engine.state.*;
import fr.opensettlers.engine.state.utils.BuildingName;
import fr.opensettlers.engine.state.utils.ResourceType;
import fr.opensettlers.engine.state.utils.TileType;
import fr.opensettlers.engine.state.utils.WorkerState;

import java.util.ArrayList;
import java.util.List;

/**
 * System managing resource transformation and worker productivity in production buildings.
 */
public class ProductionSystem implements ISystem {
    /**
     * Processes production cycles for all active buildings.
     * Manages input resource collection, worker state changes (WORKING / WAITING),
     * productivity value calculation, and production tick cooldowns.
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

            // Check production conditions (slots and, for extractors, map resources)
            if (canProduce(gameState, pb)) {
                occupant.setState(WorkerState.WORKING);
                pb.setWaitingTicks(0);

                int cooldown = pb.getProductionCooldown();
                if (cooldown <= 0) {
                    if (pb instanceof RawExtractor ext) {
                        consumeMapResource(gameState, ext);
                    }
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
     * Checks slot conditions and, for raw extractors, the availability of the
     * natural resource on the map around the building.
     *
     * @param state the current game state
     * @param pb    the production building to check
     * @return {@code true} if the building can produce this cycle
     */
    private boolean canProduce(GameState state, ProductionBuilding pb) {
        if (!pb.canProduce()) {
            return false;
        }
        if (pb instanceof RawExtractor ext) {
            return hasMapResource(state, ext);
        }
        return true;
    }

    /**
     * Checks whether the map holds what the extractor needs: a harvestable node
     * for harvesters, or a free grass tile for the forester. Farms and wells
     * produce without a map node.
     *
     * @param state the current game state
     * @param ext   the raw extractor building
     * @return {@code true} if the required map resource is available
     */
    private boolean hasMapResource(GameState state, RawExtractor ext) {
        GameMap map = state.getMap();
        if (map == null) {
            return true;
        }

        if (ext.getName() == BuildingName.FORESTER) {
            return findPlantableTile(map, ext) != null;
        }
        if (ext.getExtractedResource() == ResourceType.WHEAT
                || ext.getExtractedResource() == ResourceType.WATER) {
            return true;
        }
        return map.findClosestResourceTile(
                ext.getPosition(), workRadius(ext), ext.getExtractedResource()) != null;
    }

    /**
     * Applies the extractor's effect on the map for one production cycle:
     * harvests one unit from the closest matching node, or plants a tree
     * for the forester.
     *
     * @param state the current game state
     * @param ext   the raw extractor building
     */
    private void consumeMapResource(GameState state, RawExtractor ext) {
        GameMap map = state.getMap();
        if (map == null) {
            return;
        }

        if (ext.getName() == BuildingName.FORESTER) {
            MapTile spot = findPlantableTile(map, ext);
            if (spot != null) {
                spot.replantTree(new NaturalResourceNode(ResourceType.LOG, 5));
            }
            return;
        }
        if (ext.getExtractedResource() == ResourceType.WHEAT
                || ext.getExtractedResource() == ResourceType.WATER) {
            return;
        }

        MapTile tile = map.findClosestResourceTile(
                ext.getPosition(), workRadius(ext), ext.getExtractedResource());
        if (tile != null) {
            tile.harvestResource();
        }
    }

    /**
     * Finds a grass tile without a resource where the forester can plant a tree.
     *
     * @param map the game map
     * @param ext the forester building
     * @return a plantable tile, or {@code null} if none nearby
     */
    private MapTile findPlantableTile(GameMap map, RawExtractor ext) {
        return map.findClosestTile(ext.getPosition(), 5, tile ->
                tile.getType() == TileType.GRASS && tile.getNaturalResource() == null);
    }

    /**
     * Returns how far the extractor's specialist works from the building:
     * miners dig right under the mine, fishermen reach nearby shores,
     * woodcutters and quarrymen roam a bit further.
     *
     * @param ext the raw extractor building
     * @return the work radius in tiles
     */
    private int workRadius(RawExtractor ext) {
        if (ext.getName() == BuildingName.MINE) return 3;
        if (ext.getName() == BuildingName.FISHING_HUT) return 5;
        return 6;
    }
}
