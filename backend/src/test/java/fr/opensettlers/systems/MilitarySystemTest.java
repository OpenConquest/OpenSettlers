package fr.opensettlers.systems;

import fr.opensettlers.entities.BuildingFactory;
import fr.opensettlers.state.GameState;
import fr.opensettlers.entities.MilitaryBuilding;
import fr.opensettlers.entities.Soldier;
import fr.opensettlers.entities.StorageBuilding;
import fr.opensettlers.utils.BuildingName;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.ResourceType;
import fr.opensettlers.utils.SoldierRank;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests soldier recruitment (sword + shield + beer) and gold coin promotions.
 */
class MilitarySystemTest {

    private final MilitarySystem system = new MilitarySystem();

    private GameState newState() {
        return new GameState(UUID.randomUUID(), new ArrayList<>());
    }

    @Test
    void recruitsSoldiersWithSwordShieldAndBeer() {
        GameState state = newState();
        StorageBuilding hq = (StorageBuilding) BuildingFactory.createBuilding(
                BuildingName.HEADQUARTERS, 0, new Coordinates(0, 0));
        MilitaryBuilding barracks = (MilitaryBuilding) BuildingFactory.createBuilding(
                BuildingName.BARRACKS, 0, new Coordinates(2, 2));
        state.getBuildings().add(hq);
        state.getBuildings().add(barracks);

        int swords = hq.getStoredResources().get(ResourceType.SWORD);
        int shields = hq.getStoredResources().get(ResourceType.SHIELD);
        int beers = hq.getStoredResources().get(ResourceType.BEER);

        system.process(state);

        assertEquals(barracks.getMaxCapacity(), state.getSoldiers().size());
        assertEquals(swords - barracks.getMaxCapacity(), hq.getStoredResources().get(ResourceType.SWORD));
        assertEquals(shields - barracks.getMaxCapacity(), hq.getStoredResources().get(ResourceType.SHIELD));
        assertEquals(beers - barracks.getMaxCapacity(), hq.getStoredResources().get(ResourceType.BEER));
    }

    @Test
    void doesNotRecruitWithoutBeer() {
        GameState state = newState();
        StorageBuilding hq = (StorageBuilding) BuildingFactory.createBuilding(
                BuildingName.HEADQUARTERS, 0, new Coordinates(0, 0));
        hq.getStoredResources().put(ResourceType.BEER, 0);
        MilitaryBuilding barracks = (MilitaryBuilding) BuildingFactory.createBuilding(
                BuildingName.BARRACKS, 0, new Coordinates(2, 2));
        state.getBuildings().add(hq);
        state.getBuildings().add(barracks);

        system.process(state);

        assertTrue(state.getSoldiers().isEmpty());
    }

    @Test
    void promotesTheLowestRankedSoldierWithACoin() {
        GameState state = newState();
        MilitaryBuilding tower = (MilitaryBuilding) BuildingFactory.createBuilding(
                BuildingName.WATCH_TOWER, 0, new Coordinates(0, 0));
        Soldier veteran = new Soldier(0, new Coordinates(0, 0));
        veteran.promote(); // PRIVATE_FIRST_CLASS
        Soldier rookie = new Soldier(0, new Coordinates(0, 0));
        tower.addSoldier(veteran);
        tower.addSoldier(rookie);
        tower.setStoredCoins(1);
        state.getBuildings().add(tower);

        system.process(state);

        assertEquals(SoldierRank.PRIVATE_FIRST_CLASS, rookie.getRank());
        assertEquals(SoldierRank.PRIVATE_FIRST_CLASS, veteran.getRank());
        assertEquals(0, tower.getStoredCoins());
        assertTrue(tower.getPromotionCooldown() > 0);
    }

    @Test
    void promotionWaitsForTheCooldown() {
        GameState state = newState();
        MilitaryBuilding tower = (MilitaryBuilding) BuildingFactory.createBuilding(
                BuildingName.WATCH_TOWER, 0, new Coordinates(0, 0));
        Soldier rookie = new Soldier(0, new Coordinates(0, 0));
        tower.addSoldier(rookie);
        tower.setStoredCoins(2);
        state.getBuildings().add(tower);

        system.process(state); // first promotion
        system.process(state); // cooldown active, coin must not be spent

        assertEquals(SoldierRank.PRIVATE_FIRST_CLASS, rookie.getRank());
        assertEquals(1, tower.getStoredCoins());
    }

    @Test
    void generalsAreNeverPromotedFurther() {
        GameState state = newState();
        MilitaryBuilding tower = (MilitaryBuilding) BuildingFactory.createBuilding(
                BuildingName.WATCH_TOWER, 0, new Coordinates(0, 0));
        Soldier general = new Soldier(0, new Coordinates(0, 0));
        while (general.getRank().isPromotable()) {
            general.promote();
        }
        tower.addSoldier(general);
        tower.setStoredCoins(1);
        state.getBuildings().add(tower);

        system.process(state);

        assertEquals(SoldierRank.GENERAL, general.getRank());
        assertEquals(1, tower.getStoredCoins()); // coin kept, nobody to promote
    }
}
