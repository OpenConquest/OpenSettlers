package fr.opensettlers.state;

import fr.opensettlers.entities.building.Building;
import fr.opensettlers.entities.building.BuildingFactory;
import fr.opensettlers.entities.world.MapTile;
import fr.opensettlers.entities.unit.Worker;
import fr.opensettlers.utils.enums.BuildingName;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.enums.TileType;
import fr.opensettlers.service.GameStateSerializer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the per-player fog of war and its effect on serialization.
 */
class FogOfWarManagerTest {

    private GameState newStateWithMap(int size) {
        GameState state = new GameState(UUID.randomUUID(), new ArrayList<>());
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                Coordinates c = new Coordinates(x, 2.0 * y + (x % 2));
                state.getMapTiles().put(c, new MapTile(c, TileType.GRASS, 1));
            }
        }
        return state;
    }

    @Test
    void buildingsRevealTheirSurroundings() {
        GameState state = newStateWithMap(16);
        Building barracks = BuildingFactory.createBuilding(
                BuildingName.BARRACKS, 0, new Coordinates(0, 0));
        state.getBuildings().add(barracks);

        state.getFogOfWar().update(state);

        // Barracks vision = territory radius (4) + margin (2) = 6
        assertTrue(state.getFogOfWar().isExplored(0, new Coordinates(0, 0)));
        assertTrue(state.getFogOfWar().isExplored(0, new Coordinates(3, 1)));
        assertFalse(state.getFogOfWar().isExplored(0, new Coordinates(15, 29)));
    }

    @Test
    void explorationIsPerPlayer() {
        GameState state = newStateWithMap(16);
        Building barracks = BuildingFactory.createBuilding(
                BuildingName.BARRACKS, 0, new Coordinates(0, 0));
        state.getBuildings().add(barracks);

        state.getFogOfWar().update(state);

        assertTrue(state.getFogOfWar().isExplored(0, new Coordinates(0, 0)));
        assertFalse(state.getFogOfWar().isExplored(1, new Coordinates(0, 0)));
        assertTrue(state.getFogOfWar().getExplored(1).isEmpty());
    }

    @Test
    void explorationIsPermanent() {
        GameState state = newStateWithMap(16);
        Worker scout = new Worker(0);
        scout.setPosition(new Coordinates(8, 8));
        state.getWorkers().add(scout);

        state.getFogOfWar().update(state);
        state.getWorkers().clear();
        state.getFogOfWar().update(state);

        assertTrue(state.getFogOfWar().isExplored(0, new Coordinates(8, 8)));
    }

    @Test
    void serializationHidesEnemyBuildingsInTheFog() {
        GameState state = newStateWithMap(16);
        Building enemyHq = BuildingFactory.createBuilding(
                BuildingName.HEADQUARTERS, 0, new Coordinates(0, 0));
        state.getBuildings().add(enemyHq);
        state.getFogOfWar().update(state);

        String viewOfPlayer1 = GameStateSerializer.serializeState(state, 1);
        String spectatorView = GameStateSerializer.serializeState(state);

        assertFalse(viewOfPlayer1.contains(enemyHq.getId().toString()),
                "Player 1 must not see the unexplored enemy HQ");
        assertTrue(spectatorView.contains(enemyHq.getId().toString()),
                "Spectators see everything");
    }

    @Test
    void serializationShowsEnemyBuildingsOnExploredTiles() {
        GameState state = newStateWithMap(16);
        Building enemyHq = BuildingFactory.createBuilding(
                BuildingName.HEADQUARTERS, 0, new Coordinates(0, 0));
        state.getBuildings().add(enemyHq);

        // Player 1 scouts right next to the enemy HQ
        Worker scout = new Worker(1);
        scout.setPosition(new Coordinates(1, 1));
        state.getWorkers().add(scout);
        state.getFogOfWar().update(state);

        String viewOfPlayer1 = GameStateSerializer.serializeState(state, 1);

        assertTrue(viewOfPlayer1.contains(enemyHq.getId().toString()),
                "Player 1 explored the enemy HQ tile and must see it");
    }
}
