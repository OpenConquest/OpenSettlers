package fr.opensettlers.systems;

import fr.opensettlers.entities.BuildingFactory;
import fr.opensettlers.state.GameState;
import fr.opensettlers.entities.MapTile;
import fr.opensettlers.entities.NaturalResourceNode;
import fr.opensettlers.entities.RawExtractor;
import fr.opensettlers.entities.ResourceSlot;
import fr.opensettlers.entities.Worker;
import fr.opensettlers.utils.BuildingName;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.ResourceType;
import fr.opensettlers.utils.TileType;
import fr.opensettlers.utils.WorkerState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests mine behavior: food consumption per extraction and deposit depletion.
 */
class MineProductionTest {

    private final ProductionSystem system = new ProductionSystem();

    private GameState newState() {
        return new GameState(UUID.randomUUID(), new ArrayList<>());
    }

    /** Builds a mine standing on a mountain tile holding an iron deposit. */
    private RawExtractor mineOnIronDeposit(GameState state, int depositQuantity) {
        Coordinates pos = new Coordinates(0, 0);
        MapTile mountain = new MapTile(pos, TileType.MOUNTAIN, 4);
        mountain.setNaturalResource(new NaturalResourceNode(ResourceType.IRON, depositQuantity));
        state.getMapTiles().put(pos, mountain);

        RawExtractor mine = (RawExtractor) BuildingFactory.createBuilding(BuildingName.MINE, 0, pos, state);
        Worker miner = new Worker(0);
        miner.setState(WorkerState.WORKING);
        mine.setOccupant(miner);
        state.getBuildings().add(mine);
        return mine;
    }

    private static void giveFood(RawExtractor mine, ResourceType food, int amount) {
        for (ResourceSlot slot : mine.getInputSlots()) {
            if (slot.getType() == food) {
                slot.setQuantity(amount);
            }
        }
    }

    private static int foodLeft(RawExtractor mine, ResourceType food) {
        return mine.getInputSlots().stream()
                .filter(s -> s.getType() == food)
                .mapToInt(ResourceSlot::getQuantity)
                .sum();
    }

    @Test
    void extractionConsumesOneFoodAndDigsTheDeposit() {
        GameState state = newState();
        RawExtractor mine = mineOnIronDeposit(state, 3);
        giveFood(mine, ResourceType.FISH, 2);

        system.process(state);

        assertEquals(1, mine.getOutputSlot().getQuantity());
        assertEquals(ResourceType.IRON, mine.getOutputSlot().getType());
        assertEquals(1, foodLeft(mine, ResourceType.FISH));
        assertEquals(2, state.getTile(new Coordinates(0, 0)).getNaturalResource().getQuantity());
    }

    @Test
    void mineStarvesWithoutFood() {
        GameState state = newState();
        RawExtractor mine = mineOnIronDeposit(state, 3);

        system.process(state);

        assertEquals(0, mine.getOutputSlot().getQuantity());
        assertEquals(WorkerState.WAITING, mine.getOccupant().getState());
    }

    @Test
    void mineStopsOnceTheDepositIsExhausted() {
        GameState state = newState();
        RawExtractor mine = mineOnIronDeposit(state, 1);
        giveFood(mine, ResourceType.BREAD, 5);

        system.process(state); // digs the last ore unit
        assertEquals(1, mine.getOutputSlot().getQuantity());

        // Burn through the cooldown: no further production may happen
        for (int i = 0; i < 20; i++) {
            system.process(state);
        }
        assertEquals(1, mine.getOutputSlot().getQuantity());
        assertNull(mine.getTargetWorkTile());
        assertEquals(WorkerState.WAITING, mine.getOccupant().getState());
    }
}
