package fr.opensettlers.engine.systems;

import fr.opensettlers.engine.GameState;
import fr.opensettlers.engine.state.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProductionSystem implements ISystem {
    /** Tick period of production for buildings. */
    public static int PRODUCTION_TIME = 5;

    /**
     * Process function, called every tock by the game loop.
     *
     * @param gameState Game state of the current session.
     */
    @Override
    public void process(GameState gameState) {
        List<ProductionBuilding> productionBuildings = new ArrayList<>();
        for (Building building : gameState.getBuildings()) {
            if (building instanceof ProductionBuilding) {
                productionBuildings.add((ProductionBuilding) building);
            }
        }

        productionBuildings.forEach(productionBuilding -> {
            Flag flag = productionBuilding.getAttachedFlag();
            if (productionBuilding.getInputSlots() != null && flag != null) {
                for (ResourceSlot slot : productionBuilding.getInputSlots()) {
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

            int cooldown = productionBuilding.getProductionCooldown();
            if (cooldown <= 0) {
                boolean hasWorkingOccupant = (productionBuilding.getOccupant() != null 
                        && productionBuilding.getOccupant().getState() == fr.opensettlers.engine.state.utils.WorkerState.WORKING);
                if (hasWorkingOccupant && productionBuilding.canProduce()) {
                    productionBuilding.produce();
                    productionBuilding.setProductionCooldown(PRODUCTION_TIME);
                }
            } else {
                productionBuilding.setProductionCooldown(cooldown - 1);
            }
        });
    }
}
