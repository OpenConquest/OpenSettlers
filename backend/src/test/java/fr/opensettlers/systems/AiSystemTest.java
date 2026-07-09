package fr.opensettlers.systems;

import fr.opensettlers.entities.building.BuildingFactory;
import fr.opensettlers.entities.building.ConstructionSite;
import fr.opensettlers.entities.world.MapTile;
import fr.opensettlers.state.GameState;
import fr.opensettlers.utils.enums.BuildingName;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.enums.TileType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that a computer player bootstraps its economy.
 */
class AiSystemTest {

    private final AiSystem system = new AiSystem();

    private GameState aiState() {
        GameState state = new GameState(UUID.randomUUID(), new ArrayList<>());
        Map<Coordinates, MapTile> tiles = new HashMap<>();
        for (int x = 0; x <= 16; x++) {
            for (int y = 0; y <= 32; y++) {
                Coordinates c = new Coordinates(x, y);
                tiles.put(c, new MapTile(c, TileType.GRASS, 0));
            }
        }
        state.setMapTiles(tiles);
        state.setPlayerCount(2);
        state.getAiPlayers().add(0);
        state.getBuildings().add(BuildingFactory.createBuilding(
                BuildingName.HEADQUARTERS, 0, new Coordinates(8, 16)));
        state.getTerritoryManager().recalculate(state);
        return state;
    }

    @Test
    void aiPlacesItsFirstBuilding() {
        GameState state = aiState();
        int before = state.getBuildings().size();

        system.process(state); // tick 0 → decision time

        assertTrue(state.getBuildings().size() > before);
        assertTrue(state.getBuildings().stream().anyMatch(b -> b instanceof ConstructionSite));
    }

    @Test
    void aiIsIdleBetweenDecisionTicks() {
        GameState state = aiState();
        state.setCurrentTick(1); // not a multiple of the decision interval
        int before = state.getBuildings().size();

        system.process(state);

        assertEquals(before, state.getBuildings().size());
    }
}
