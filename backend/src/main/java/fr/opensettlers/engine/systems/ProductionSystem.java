package fr.opensettlers.engine.systems;

import fr.opensettlers.engine.GameState;
import fr.opensettlers.engine.state.Building;
import fr.opensettlers.engine.state.ProductionBuilding;
import fr.opensettlers.engine.state.RawExtractor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProductionSystem implements ISystem {
    @Override
    public void process(GameState gameState) {
        List<ProductionBuilding> productionBuildings = new ArrayList<>();
        for (Building building : gameState.getBuildings()) {
            if (building instanceof ProductionBuilding) {
                productionBuildings.add((ProductionBuilding) building);
            }
        }

        productionBuildings.forEach(ProductionBuilding::produce);
    }
}
