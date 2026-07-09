package fr.opensettlers.entities;

import fr.opensettlers.entities.building.BuildingFactory;
import fr.opensettlers.utils.enums.BuildingName;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.enums.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import fr.opensettlers.entities.resource.ResourceSlot;
import fr.opensettlers.entities.building.ProcessingBuilding;

/**
 * Tests recipe processing, including the armory alternating swords and shields.
 */
class ProcessingBuildingTest {

    private static final Coordinates POS = new Coordinates(0, 0);

    @Test
    void sawmillTurnsLogsIntoPlanks() {
        ProcessingBuilding sawmill = (ProcessingBuilding) BuildingFactory.createBuilding(BuildingName.SAWMILL, 0, POS);
        fill(sawmill, ResourceType.LOG, 1);

        assertTrue(sawmill.canProduce());
        sawmill.produce();

        assertEquals(ResourceType.PLANK, sawmill.getOutputSlot().getType());
        assertEquals(1, sawmill.getOutputSlot().getQuantity());
        assertEquals(0, slotQuantity(sawmill, ResourceType.LOG));
    }

    @Test
    void armoryAlternatesSwordsAndShields() {
        ProcessingBuilding armory = (ProcessingBuilding) BuildingFactory.createBuilding(BuildingName.ARMORY, 0, POS);
        fill(armory, ResourceType.STEEL, 5);
        fill(armory, ResourceType.COAL, 5);

        Set<ResourceType> produced = new HashSet<>();
        for (int i = 0; i < 2; i++) {
            assertTrue(armory.canProduce());
            armory.produce();
            produced.add(armory.getOutputSlot().getType());
            // Simulate the carrier picking up the produced item
            armory.getOutputSlot().removeResource();
        }

        assertEquals(Set.of(ResourceType.SWORD, ResourceType.SHIELD), produced);
    }

    @Test
    void armoryKeepsItsRecipeWhileOutputAwaitsPickup() {
        ProcessingBuilding armory = (ProcessingBuilding) BuildingFactory.createBuilding(BuildingName.ARMORY, 0, POS);
        fill(armory, ResourceType.STEEL, 5);
        fill(armory, ResourceType.COAL, 5);

        assertTrue(armory.canProduce());
        armory.produce();
        ResourceType firstOutput = armory.getOutputSlot().getType();

        // Output not picked up yet: the recipe must not rotate
        assertTrue(armory.canProduce());
        armory.produce();
        assertEquals(firstOutput, armory.getOutputSlot().getType());
        assertEquals(2, armory.getOutputSlot().getQuantity());
    }

    @Test
    void cannotProduceWithoutInputs() {
        ProcessingBuilding brewery = (ProcessingBuilding) BuildingFactory.createBuilding(BuildingName.BREWERY, 0, POS);
        assertFalse(brewery.canProduce());
    }

    private static void fill(ProcessingBuilding building, ResourceType type, int amount) {
        for (ResourceSlot slot : building.getInputSlots()) {
            if (slot.getType() == type) {
                slot.setQuantity(amount);
            }
        }
    }

    private static int slotQuantity(ProcessingBuilding building, ResourceType type) {
        return building.getInputSlots().stream()
                .filter(s -> s.getType() == type)
                .mapToInt(ResourceSlot::getQuantity)
                .sum();
    }
}
