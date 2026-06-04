package fr.opensettlers.engine.systems;

import fr.opensettlers.engine.GameState;
import fr.opensettlers.engine.state.*;
import fr.opensettlers.engine.state.utils.WorkerState;

import java.util.ArrayList;
import java.util.List;

/**
 * System managing resource transformation and worker productivity in production buildings.
 */
public class ProductionSystem implements ISystem {

    /**
     * Default cooldown ticks required for a production building to produce a resource.
     */
    public static final int PRODUCTION_TIME = 5;

    /**
     * Ticks equivalent to a 10-second real-time duration (100ms per tick).
     */
    private static final int TICKS_FOR_10_SECONDS = 100;

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

            // Check production conditions
            if (pb.canProduce()) {
                occupant.setState(WorkerState.WORKING);
                pb.setWaitingTicks(0);

                int cooldown = pb.getProductionCooldown();
                if (cooldown <= 0) {
                    pb.produce();
                    pb.setProductivity(Math.min(100, pb.getProductivity() + 10));
                    pb.setProductionCooldown(PRODUCTION_TIME);
                } else {
                    pb.setProductionCooldown(cooldown - 1);
                }
            } else {
                occupant.setState(WorkerState.WAITING);
                pb.setWaitingTicks(pb.getWaitingTicks() + 1);

                if (pb.getWaitingTicks() >= TICKS_FOR_10_SECONDS) {
                    pb.setProductivity(Math.max(0, pb.getProductivity() - 5));
                    pb.setWaitingTicks(0);
                }
            }
        }
    }
}
