package fr.opensettlers.engine.state;

import fr.opensettlers.engine.state.utils.Coordinates;
import fr.opensettlers.engine.state.utils.ResourceType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.UUID;

/** Building that transforms input resources into output resources via a recipe. */
@Getter
public class ProcessingBuilding extends ProductionBuilding {
    /** Recipe defining the input/output resource conversion. */
    private final Recipe recipe;

    /**
     * @param id       unique identifier
     * @param playerId owning player ID
     * @param position map coordinates
     * @param recipe   conversion recipe
     */
    public ProcessingBuilding(UUID id, int playerId, Coordinates position, Recipe recipe) {
        super(id, playerId, position);
        this.recipe = recipe;

        this.inputSlots = new ArrayList<>();
        if (recipe.getInput() != null) {
            for (ResourceType type : recipe.getInput().keySet()) {
                this.inputSlots.add(new ResourceSlot(type));
            }
        }
        this.outputSlot = new ResourceSlot(recipe.getOutput());
    }

    /** Processes input resources and produces the output according to the recipe. */
    @Override
    public void produce() {
        this.recipe.consume(this.inputSlots);
        this.outputSlot.addResource();
    }

    /** @return {@code true} if processing conditions are met. */
    @Override
    public boolean canProduce() {
        return this.recipe.canProcess(this.inputSlots) && 
               this.outputSlot.getQuantity() < this.outputSlot.getMAX_PER_SLOT();
    }
}
