package fr.opensettlers.systems;

import fr.opensettlers.entities.building.BuildingFactory;
import fr.opensettlers.state.GameState;
import fr.opensettlers.entities.building.CatapultBuilding;
import fr.opensettlers.entities.building.MilitaryBuilding;
import fr.opensettlers.entities.unit.Soldier;
import fr.opensettlers.entities.unit.Worker;
import fr.opensettlers.utils.enums.BuildingName;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.enums.WorkerState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import fr.opensettlers.systems.military.CatapultSystem;

/**
 * Tests catapult sieges: ammunition, hits killing garrisoned soldiers, and
 * the destruction of emptied military buildings.
 */
class CatapultSystemTest {

    /** Random stub forcing every projectile to hit. */
    private static final Random ALWAYS_HIT = new Random() {
        @Override public double nextDouble() { return 0.0; }
    };

    /** Random stub forcing every projectile to miss. */
    private static final Random ALWAYS_MISS = new Random() {
        @Override public double nextDouble() { return 1.0; }
    };

    private GameState newState() {
        return new GameState(UUID.randomUUID(), new ArrayList<>());
    }

    private CatapultBuilding mannedCatapult(GameState state, int playerId) {
        CatapultBuilding catapult = (CatapultBuilding) BuildingFactory.createBuilding(
                BuildingName.CATAPULT, playerId, new Coordinates(0, 0));
        Worker helper = new Worker(playerId);
        helper.setState(WorkerState.WAITING);
        catapult.setOccupant(helper);
        catapult.setFireCooldown(0);
        catapult.getStoneSlot().setQuantity(2);
        state.getBuildings().add(catapult);
        return catapult;
    }

    private MilitaryBuilding enemyBarracks(GameState state, int playerId, int garrison) {
        MilitaryBuilding barracks = (MilitaryBuilding) BuildingFactory.createBuilding(
                BuildingName.BARRACKS, playerId, new Coordinates(3, 3));
        for (int i = 0; i < garrison; i++) {
            barracks.addSoldier(new Soldier(playerId, new Coordinates(3, 3)));
        }
        state.getBuildings().add(barracks);
        return barracks;
    }

    @Test
    void aHitKillsOneGarrisonedSoldier() {
        GameState state = newState();
        CatapultBuilding catapult = mannedCatapult(state, 0);
        MilitaryBuilding barracks = enemyBarracks(state, 1, 2);

        new CatapultSystem(ALWAYS_HIT).process(state);

        assertEquals(1, barracks.getSoldiers().size());
        assertFalse(barracks.isDestroyed());
        assertEquals(1, catapult.getStoneSlot().getQuantity());
        assertTrue(catapult.getFireCooldown() > 0);
    }

    @Test
    void wipingOutTheGarrisonBurnsTheBuildingDown() {
        GameState state = newState();
        mannedCatapult(state, 0);
        MilitaryBuilding barracks = enemyBarracks(state, 1, 1);

        new CatapultSystem(ALWAYS_HIT).process(state);

        assertTrue(barracks.isDestroyed());
        assertTrue(barracks.getAttachedFlag().isDestroyed());
    }

    @Test
    void aMissOnlyConsumesAStone() {
        GameState state = newState();
        CatapultBuilding catapult = mannedCatapult(state, 0);
        MilitaryBuilding barracks = enemyBarracks(state, 1, 1);

        new CatapultSystem(ALWAYS_MISS).process(state);

        assertEquals(1, barracks.getSoldiers().size());
        assertFalse(barracks.isDestroyed());
        assertEquals(1, catapult.getStoneSlot().getQuantity());
    }

    @Test
    void unmannedCatapultsDoNotFire() {
        GameState state = newState();
        CatapultBuilding catapult = mannedCatapult(state, 0);
        catapult.setOccupant(null);
        MilitaryBuilding barracks = enemyBarracks(state, 1, 1);

        new CatapultSystem(ALWAYS_HIT).process(state);

        assertEquals(1, barracks.getSoldiers().size());
        assertEquals(2, catapult.getStoneSlot().getQuantity());
    }

    @Test
    void doesNotWasteStonesWithoutATargetInRange() {
        GameState state = newState();
        CatapultBuilding catapult = mannedCatapult(state, 0);
        MilitaryBuilding farAway = (MilitaryBuilding) BuildingFactory.createBuilding(
                BuildingName.BARRACKS, 1, new Coordinates(50, 50));
        farAway.addSoldier(new Soldier(1, new Coordinates(50, 50)));
        state.getBuildings().add(farAway);

        new CatapultSystem(ALWAYS_HIT).process(state);

        assertEquals(2, catapult.getStoneSlot().getQuantity());
        assertEquals(1, farAway.getSoldiers().size());
    }

    @Test
    void friendlyBuildingsAreNeverTargeted() {
        GameState state = newState();
        CatapultBuilding catapult = mannedCatapult(state, 0);
        MilitaryBuilding own = enemyBarracks(state, 0, 1); // same player

        new CatapultSystem(ALWAYS_HIT).process(state);

        assertEquals(1, own.getSoldiers().size());
        assertEquals(2, catapult.getStoneSlot().getQuantity());
    }
}
