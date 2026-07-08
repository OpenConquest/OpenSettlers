package fr.opensettlers.entities;

import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.ResourceType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Building that transforms input resources into output resources via one or
 * several recipes. Multi-recipe buildings (e.g. the Armory producing swords
 * and shields) cycle to the next recipe each time their output slot empties.
 */
@Getter
public class ProcessingBuilding extends ProductionBuilding {
    /**
     * Recipes this building can process, cycled in order.
     */
    private final List<Recipe> recipes;

    /**
     * Index of the recipe currently being processed.
     */
    private int recipeIndex = 0;

    /**
     * Initializes a new single-recipe ProcessingBuilding.
     *
     * @param playerId owning player ID
     * @param position map coordinates
     * @param recipe   conversion recipe
     */
    public ProcessingBuilding(int playerId, Coordinates position, Recipe recipe) {
        this(playerId, position, List.of(recipe));
    }

    /**
     * Initializes a new ProcessingBuilding cycling over several recipes.
     * Input slots cover the union of all recipe inputs; the output slot is
     * retyped when the building rotates to a recipe with a different output.
     *
     * @param playerId owning player ID
     * @param position map coordinates
     * @param recipes  conversion recipes, processed in rotation
     */
    public ProcessingBuilding(int playerId, Coordinates position, List<Recipe> recipes) {
        super(playerId, position);
        if (recipes.isEmpty()) {
            throw new IllegalArgumentException("A processing building needs at least one recipe");
        }
        this.recipes = List.copyOf(recipes);

        this.inputSlots = new ArrayList<>();
        Set<ResourceType> slotTypes = new HashSet<>();
        for (Recipe recipe : this.recipes) {
            if (recipe.getInput() == null) continue;
            for (ResourceType type : recipe.getInput().keySet()) {
                if (slotTypes.add(type)) {
                    this.inputSlots.add(new ResourceSlot(type));
                }
            }
        }
        this.outputSlot = new ResourceSlot(this.recipes.getFirst().getOutput());
    }

    /**
     * Returns the recipe currently being processed.
     *
     * @return the active recipe
     */
    public Recipe getRecipe() {
        return recipes.get(recipeIndex);
    }

    /**
     * Processes input resources and produces the output according to the active recipe.
     */
    @Override
    public void produce() {
        getRecipe().consume(this.inputSlots);
        this.outputSlot.addResource();
    }

    /**
     * Checks if the building can produce. Multi-recipe buildings rotate to the
     * next recipe once their output slot has been fully picked up.
     *
     * @return {@code true} if processing conditions are met.
     */
    @Override
    public boolean canProduce() {
        rotateRecipeIfOutputEmpty();
        return getRecipe().canProcess(this.inputSlots)
                && this.outputSlot.getQuantity() < this.outputSlot.getMAX_PER_SLOT();
    }

    /**
     * Rotates to the next recipe when the output slot is empty and no produced
     * item is still awaiting pickup, retyping the output slot if needed.
     */
    private void rotateRecipeIfOutputEmpty() {
        if (recipes.size() <= 1
                || this.outputSlot.getQuantity() > 0
                || !this.outputDestinations.isEmpty()) {
            return;
        }
        this.recipeIndex = (this.recipeIndex + 1) % recipes.size();
        if (this.outputSlot.getType() != getRecipe().getOutput()) {
            this.outputSlot = new ResourceSlot(getRecipe().getOutput());
        }
    }
}
