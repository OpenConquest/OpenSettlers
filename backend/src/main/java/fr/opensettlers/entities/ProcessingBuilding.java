package fr.opensettlers.entities;

import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.ResourceType;
import lombok.Getter;

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
        
        this.inputSlots = new java.util.ArrayList<>();
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
    public boolean canProcess() {
        return this.recipe.canProcess(this.inputSlots) && 
               this.outputSlot.getQuantity() < this.outputSlot.getMAX_PER_SLOT();
    }

    /** Calls the production method according to the production frequency. */
    @Override
    public void tick() {
        if (this.productionCooldown <= 0) {
            if (this.canProcess()) {
                this.produce();
                this.productionCooldown = PRODUCTION_TIME;
            }
        } else {
            this.productionCooldown--;
        }
    }
}
