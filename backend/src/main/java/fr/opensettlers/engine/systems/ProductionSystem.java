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

    /** Used to ensure all buildings respect the production rate. */
    private int productionCooldown = 0;

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

            if (this.productionCooldown <= 0) {
                if (productionBuilding.canProduce()) {
                    productionBuilding.produce();
                    this.productionCooldown = PRODUCTION_TIME;
                }
            } else {
                this.productionCooldown--;
            }
        });
    }
}
