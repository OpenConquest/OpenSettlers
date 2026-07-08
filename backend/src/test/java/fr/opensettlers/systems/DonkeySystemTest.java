package fr.opensettlers.systems;

import fr.opensettlers.entities.BuildingFactory;
import fr.opensettlers.utils.GameConfig;
import fr.opensettlers.state.GameState;
import fr.opensettlers.systems.TransportManager;
import fr.opensettlers.entities.Carrier;
import fr.opensettlers.entities.Donkey;
import fr.opensettlers.entities.Flag;
import fr.opensettlers.entities.ProcessingBuilding;
import fr.opensettlers.entities.Road;
import fr.opensettlers.utils.BuildingName;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.DonkeyState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests road upgrades to level 2 and the donkey lifecycle.
 */
class DonkeySystemTest {

    private final DonkeySystem system = new DonkeySystem();

    private GameState newState() {
        return new GameState(UUID.randomUUID(), new ArrayList<>());
    }

    /** Creates two connected flags and returns the road between them. */
    private Road buildRoad(GameState state, int playerId) {
        Flag a = new Flag(UUID.randomUUID(), playerId, new Coordinates(0, 0));
        Flag b = new Flag(UUID.randomUUID(), playerId, new Coordinates(4, 0));
        state.getRoadNetwork().addFlag(a);
        state.getRoadNetwork().addFlag(b);
        return state.getRoadNetwork().addRoad(a, b,
                List.of(new Coordinates(1, 1), new Coordinates(2, 0), new Coordinates(3, 1)));
    }

    @Test
    void heavilyUsedRoadsBecomeMainRoads() {
        GameState state = newState();
        Road road = buildRoad(state, 0);
        road.setTransportCount(GameConfig.ROAD_UPGRADE_DELIVERIES);

        system.process(state);

        assertEquals(2, road.getLevel());
    }

    @Test
    void lightlyUsedRoadsStayLevelOne() {
        GameState state = newState();
        Road road = buildRoad(state, 0);
        road.setTransportCount(GameConfig.ROAD_UPGRADE_DELIVERIES - 1);

        system.process(state);

        assertEquals(1, road.getLevel());
    }

    @Test
    void aBredDonkeyWalksToTheMainRoadAndAssistsIt() {
        GameState state = newState();
        Road road = buildRoad(state, 0);
        road.setLevel(2);

        ProcessingBuilding breeder = (ProcessingBuilding) BuildingFactory.createBuilding(
                BuildingName.DONKEY_BREEDER, 0, new Coordinates(0, 2));
        breeder.getOutputSlot().addResource();
        state.getBuildings().add(breeder);

        // Several ticks: breed, walk along the flags, arrive
        for (int i = 0; i < 10; i++) {
            system.process(state);
        }

        assertEquals(1, state.getDonkeys().size());
        Donkey donkey = state.getDonkeys().getFirst();
        assertEquals(DonkeyState.ASSISTING, donkey.getState());
        assertTrue(road.isDonkeyAssisted());
        assertEquals(0, breeder.getOutputSlot().getQuantity());
    }

    @Test
    void onlyOneDonkeyIsAssignedPerRoad() {
        GameState state = newState();
        Road road = buildRoad(state, 0);
        road.setLevel(2);

        ProcessingBuilding breeder = (ProcessingBuilding) BuildingFactory.createBuilding(
                BuildingName.DONKEY_BREEDER, 0, new Coordinates(0, 2));
        breeder.getOutputSlot().setQuantity(5);
        state.getBuildings().add(breeder);

        for (int i = 0; i < 10; i++) {
            system.process(state);
        }

        assertEquals(1, state.getDonkeys().size());
        assertEquals(4, breeder.getOutputSlot().getQuantity());
    }

    @Test
    void donkeysAreFreedWhenTheirRoadIsRemoved() {
        GameState state = newState();
        Road road = buildRoad(state, 0);
        road.setLevel(2);
        Donkey donkey = new Donkey(0, road.getStartFlag().getCoordinates());
        donkey.setAssignedRoad(road);
        donkey.setState(DonkeyState.ASSISTING);
        road.setDonkey(donkey);
        state.getDonkeys().add(donkey);

        state.getRoadNetwork().removeRoad(road.getId());
        system.process(state);

        assertEquals(DonkeyState.IDLE, donkey.getState());
        assertNull(donkey.getAssignedRoad());
    }

    @Test
    void idleDonkeysAreReassignedBeforeBreedingNewOnes() {
        GameState state = newState();
        Road road = buildRoad(state, 0);
        road.setLevel(2);

        Donkey idle = new Donkey(0, new Coordinates(0, 0));
        idle.setState(DonkeyState.IDLE);
        state.getDonkeys().add(idle);

        for (int i = 0; i < 10; i++) {
            system.process(state);
        }

        assertEquals(1, state.getDonkeys().size());
        assertEquals(DonkeyState.ASSISTING, idle.getState());
        assertSame(road, idle.getAssignedRoad());
    }

    @Test
    void donkeyAssistedRoadsDoubleCarrierSpeed() {
        GameState state = newState();
        Road road = buildRoad(state, 0);
        road.setLevel(2);
        Donkey donkey = new Donkey(0, road.getStartFlag().getCoordinates());
        donkey.setState(DonkeyState.ASSISTING);
        road.setDonkey(donkey);

        Carrier carrier = road.getCarrier();
        carrier.assignPickup(road.getStartFlag()); // walk toward progress 0
        int before = carrier.getProgress();

        new TransportManager(state.getRoadNetwork()).processCarrier(carrier, road);

        assertEquals(before - GameConfig.DONKEY_ROAD_SPEED, carrier.getProgress());
    }
}
