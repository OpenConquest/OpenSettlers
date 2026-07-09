package fr.opensettlers.entities;

import fr.opensettlers.utils.enums.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import fr.opensettlers.entities.resource.ResourceSlot;
import fr.opensettlers.entities.resource.Recipe;

/**
 * Tests the recipe book covering the full Settlers II production chains.
 */
class RecipeTest {

    @Test
    void recipeBookCoversAllProcessedGoods() {
        for (ResourceType output : List.of(
                ResourceType.PLANK, ResourceType.FLOUR, ResourceType.BREAD,
                ResourceType.BEER, ResourceType.PIG, ResourceType.MEAT,
                ResourceType.STEEL, ResourceType.SWORD, ResourceType.SHIELD,
                ResourceType.COIN, ResourceType.TOOL)) {
            assertNotNull(Recipe.RECIPES.get(output), "Missing recipe for " + output);
        }
    }

    @Test
    void coinsAreMintedFromGoldAndCoal() {
        Recipe coin = new Recipe(Recipe.RECIPES.get(ResourceType.COIN), ResourceType.COIN);
        ResourceSlot gold = new ResourceSlot(ResourceType.GOLD);
        ResourceSlot coal = new ResourceSlot(ResourceType.COAL);

        assertFalse(coin.canProcess(List.of(gold, coal)));

        gold.addResource();
        coal.addResource();
        assertTrue(coin.canProcess(List.of(gold, coal)));

        coin.consume(List.of(gold, coal));
        assertEquals(0, gold.getQuantity());
        assertEquals(0, coal.getQuantity());
    }

    @Test
    void onlyFishBreadAndMeatAreFood() {
        for (ResourceType type : ResourceType.values()) {
            boolean expected = type == ResourceType.FISH
                    || type == ResourceType.BREAD
                    || type == ResourceType.MEAT;
            assertEquals(expected, type.isFood(), "isFood mismatch for " + type);
        }
    }

    @Test
    void goldIsAnOre() {
        assertTrue(ResourceType.GOLD.isOre());
        assertFalse(ResourceType.COIN.isOre());
    }
}
