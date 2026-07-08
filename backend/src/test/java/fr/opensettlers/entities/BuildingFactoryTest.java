package fr.opensettlers.entities;

import fr.opensettlers.entities.*;
import fr.opensettlers.utils.BuildingName;
import fr.opensettlers.utils.GameConfig;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks that the factory wires every Settlers II building correctly.
 */
class BuildingFactoryTest {

    private static final Coordinates POS = new Coordinates(0, 0);

    @Test
    void fortressIsTheLargestMilitaryBuilding() {
        MilitaryBuilding fortress = (MilitaryBuilding) BuildingFactory.createBuilding(BuildingName.FORTRESS, 0, POS);
        assertEquals(GameConfig.FORTRESS_CAPACITY, fortress.getMaxCapacity());
        assertEquals(GameConfig.FORTRESS_TERRITORY_RADIUS, fortress.getTerritoryRadius());
        assertTrue(fortress.getMaxCapacity() > GameConfig.WATCHTOWER_CAPACITY);
    }

    @Test
    void mintProducesCoinsFromGoldAndCoal() {
        ProcessingBuilding mint = (ProcessingBuilding) BuildingFactory.createBuilding(BuildingName.MINT, 0, POS);
        assertEquals(ResourceType.COIN, mint.getRecipe().getOutput());
        assertTrue(mint.getRecipe().getInput().containsKey(ResourceType.GOLD));
        assertTrue(mint.getRecipe().getInput().containsKey(ResourceType.COAL));
    }

    @Test
    void metalworksProducesTools() {
        ProcessingBuilding metalworks = (ProcessingBuilding) BuildingFactory.createBuilding(BuildingName.METALWORKS, 0, POS);
        assertEquals(ResourceType.TOOL, metalworks.getRecipe().getOutput());
    }

    @Test
    void pigFarmAndSlaughterhouseFormTheMeatChain() {
        ProcessingBuilding pigFarm = (ProcessingBuilding) BuildingFactory.createBuilding(BuildingName.PIG_FARM, 0, POS);
        ProcessingBuilding slaughterhouse = (ProcessingBuilding) BuildingFactory.createBuilding(BuildingName.SLAUGHTERHOUSE, 0, POS);
        assertEquals(ResourceType.PIG, pigFarm.getRecipe().getOutput());
        assertEquals(ResourceType.MEAT, slaughterhouse.getRecipe().getOutput());
        assertTrue(slaughterhouse.getRecipe().getInput().containsKey(ResourceType.PIG));
    }

    @Test
    void hunterExtractsMeat() {
        RawExtractor hunter = (RawExtractor) BuildingFactory.createBuilding(BuildingName.HUNTERS_HUT, 0, POS);
        assertEquals(ResourceType.MEAT, hunter.getExtractedResource());
    }

    @Test
    void catapultHasAStoneAmmunitionSlot() {
        CatapultBuilding catapult = (CatapultBuilding) BuildingFactory.createBuilding(BuildingName.CATAPULT, 0, POS);
        assertEquals(ResourceType.STONE, catapult.getStoneSlot().getType());
        assertFalse(catapult.canProduce());
    }

    @Test
    void mineHasOneInputSlotPerFoodType() {
        RawExtractor mine = (RawExtractor) BuildingFactory.createBuilding(BuildingName.IRON_MINE, 0, POS);
        long foodSlots = mine.getInputSlots().stream().filter(s -> s.getType().isFood()).count();
        assertEquals(3, foodSlots); // fish, bread, meat
        assertFalse(mine.hasFood());
    }

    @Test
    void eachMineTypeExtractsItsOre() {
        assertEquals(ResourceType.STONE, ((RawExtractor) BuildingFactory
                .createBuilding(BuildingName.GRANITE_MINE, 0, POS)).getExtractedResource());
        assertEquals(ResourceType.COAL, ((RawExtractor) BuildingFactory
                .createBuilding(BuildingName.COAL_MINE, 0, POS)).getExtractedResource());
        assertEquals(ResourceType.IRON, ((RawExtractor) BuildingFactory
                .createBuilding(BuildingName.IRON_MINE, 0, POS)).getExtractedResource());
        assertEquals(ResourceType.GOLD, ((RawExtractor) BuildingFactory
                .createBuilding(BuildingName.GOLD_MINE, 0, POS)).getExtractedResource());
    }

    @Test
    void headquartersStartsWithADefendingGarrison() {
        HeadquartersBuilding hq = (HeadquartersBuilding) BuildingFactory
                .createBuilding(BuildingName.HEADQUARTERS, 0, POS);
        assertEquals(GameConfig.HEADQUARTERS_START_SOLDIERS, hq.getSoldiers().size());
        assertEquals(GameConfig.HEADQUARTERS_CAPACITY, hq.getMaxCapacity());
    }

    @Test
    void headquartersStartsWithTheSettlersKit() {
        StorageBuilding hq = (StorageBuilding) BuildingFactory.createBuilding(BuildingName.HEADQUARTERS, 0, POS);
        assertTrue(hq.getStoredResources().getOrDefault(ResourceType.TOOL, 0) > 0);
        assertTrue(hq.getStoredResources().getOrDefault(ResourceType.SWORD, 0) > 0);
        assertTrue(hq.getStoredResources().getOrDefault(ResourceType.SHIELD, 0) > 0);
        assertTrue(hq.getStoredResources().getOrDefault(ResourceType.BEER, 0) > 0);
    }

    @Test
    void everyBuildingNameCanBeInstantiated() {
        for (BuildingName name : BuildingName.values()) {
            Building b = BuildingFactory.createBuilding(name, 0, POS);
            assertNotNull(b, "Factory returned null for " + name);
            assertEquals(name, b.getName());
        }
    }

    @Test
    void everyConstructibleBuildingHasACost() {
        for (BuildingName name : BuildingName.values()) {
            if (name == BuildingName.HEADQUARTERS) continue; // placed at game start, never built
            assertFalse(Building.buildingCosts.getOrDefault(name, Map.of()).isEmpty(),
                    "Missing construction cost for " + name);
        }
    }
}
