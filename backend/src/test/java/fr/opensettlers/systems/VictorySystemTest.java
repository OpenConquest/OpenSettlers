package fr.opensettlers.systems;

import fr.opensettlers.entities.BuildingFactory;
import fr.opensettlers.entities.StorageBuilding;
import fr.opensettlers.state.GameState;
import fr.opensettlers.utils.BuildingName;
import fr.opensettlers.utils.Coordinates;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests player elimination and winner detection.
 */
class VictorySystemTest {

    private final VictorySystem system = new VictorySystem();

    private GameState twoPlayerState() {
        GameState state = new GameState(UUID.randomUUID(), new ArrayList<>());
        state.setPlayerCount(2);
        state.getBuildings().add(BuildingFactory.createBuilding(
                BuildingName.HEADQUARTERS, 0, new Coordinates(0, 0)));
        state.getBuildings().add(BuildingFactory.createBuilding(
                BuildingName.HEADQUARTERS, 1, new Coordinates(20, 20)));
        return state;
    }

    @Test
    void gameContinuesWhileEveryoneHasAWarehouse() {
        GameState state = twoPlayerState();

        system.process(state);

        assertFalse(state.isOver());
        assertTrue(state.getEliminatedPlayers().isEmpty());
    }

    @Test
    void losingAllStorageEliminatesAPlayerAndDecidesTheWinner() {
        GameState state = twoPlayerState();
        StorageBuilding loserHq = (StorageBuilding) state.getBuildings().stream()
                .filter(b -> b.getPlayerId() == 1).findFirst().orElseThrow();

        loserHq.destroy();
        system.process(state);

        assertTrue(state.getEliminatedPlayers().contains(1));
        assertTrue(state.isOver());
        assertEquals(0, state.getWinnerPlayerId());
    }

    @Test
    void singlePlayerGamesNeverEndOnTheirOwn() {
        GameState state = new GameState(UUID.randomUUID(), new ArrayList<>());
        state.setPlayerCount(1);

        system.process(state);

        assertFalse(state.isOver());
    }
}
