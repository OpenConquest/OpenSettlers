package fr.opensettlers.systems;

import fr.opensettlers.entities.Building;
import fr.opensettlers.entities.BuildingFactory;
import fr.opensettlers.entities.MapTile;
import fr.opensettlers.entities.StorageBuilding;
import fr.opensettlers.state.GameState;
import fr.opensettlers.utils.BuildingName;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.ResourceType;
import fr.opensettlers.utils.TileType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests sea expeditions: a harbor with materials and a shipyard launches a ship
 * that founds a new harbor on an unclaimed shore.
 */
class NavalSystemTest {

    private final NavalSystem system = new NavalSystem();

    /**
     * Builds a coast: north land (y ≤ 6), a sea band (7 ≤ y ≤ 19), south land
     * (y ≥ 20), with a player-0 harbor and shipyard on the north shore.
     */
    private GameState coastalState() {
        GameState state = new GameState(UUID.randomUUID(), new ArrayList<>());
        Map<Coordinates, MapTile> tiles = new HashMap<>();
        for (int x = 0; x <= 24; x++) {
            for (int y = 0; y <= 40; y++) {
                TileType type = (y >= 7 && y <= 19) ? TileType.WATER : TileType.GRASS;
                Coordinates c = new Coordinates(x, y);
                tiles.put(c, new MapTile(c, type, 0));
            }
        }
        state.setMapTiles(tiles);
        state.setPlayerCount(1);

        StorageBuilding harbor = (StorageBuilding) BuildingFactory.createBuilding(
                BuildingName.HARBOR, 0, new Coordinates(10, 6), state);
        harbor.getStoredResources().put(ResourceType.PLANK, 10);
        harbor.getStoredResources().put(ResourceType.STONE, 10);
        state.getBuildings().add(harbor);
        state.getRoadNetwork().addFlag(harbor.getAttachedFlag());

        Building shipyard = BuildingFactory.createBuilding(
                BuildingName.SHIPYARD, 0, new Coordinates(8, 4), state);
        state.getBuildings().add(shipyard);
        return state;
    }

    private long harborCount(GameState state) {
        return state.getBuildings().stream()
                .filter(b -> b instanceof StorageBuilding && !b.isDestroyed()
                        && b.getName() == BuildingName.HARBOR)
                .count();
    }

    @Test
    void anExpeditionFoundsANewHarbor() {
        GameState state = coastalState();
        assertEquals(1, harborCount(state));

        for (int i = 0; i < 400 && harborCount(state) < 2; i++) {
            system.process(state);
        }

        assertEquals(2, harborCount(state), "a new harbor should be founded by the expedition");
        assertFalse(state.getShips().isEmpty(), "the expedition ship should exist");
    }

    @Test
    void launchConsumesTheExpeditionMaterials() {
        GameState state = coastalState();
        StorageBuilding harbor = (StorageBuilding) state.getBuildings().get(0);

        system.process(state); // launches the expedition

        assertEquals(1, state.getShips().size());
        assertEquals(6, harbor.getStoredResources().get(ResourceType.PLANK));
        assertEquals(6, harbor.getStoredResources().get(ResourceType.STONE));
    }

    @Test
    void noExpeditionWithoutAShipyard() {
        GameState state = coastalState();
        state.getBuildings().removeIf(b -> b.getName() == BuildingName.SHIPYARD);

        system.process(state);

        assertTrue(state.getShips().isEmpty());
    }
}
