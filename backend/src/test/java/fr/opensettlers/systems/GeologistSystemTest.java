package fr.opensettlers.systems;

import fr.opensettlers.utils.GameConfig;
import fr.opensettlers.state.GameState;
import fr.opensettlers.entities.Flag;
import fr.opensettlers.entities.MapTile;
import fr.opensettlers.entities.NaturalResourceNode;
import fr.opensettlers.entities.Worker;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.ResourceType;
import fr.opensettlers.utils.TileType;
import fr.opensettlers.utils.WorkerState;
import fr.opensettlers.utils.WorkerType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests geologists surveying mountains and planting ore signs.
 */
class GeologistSystemTest {

    private final GeologistSystem system = new GeologistSystem();

    private GameState newState() {
        return new GameState(UUID.randomUUID(), new ArrayList<>());
    }

    /** Adds a tile to the map at the given double-height coordinates. */
    private MapTile addTile(GameState state, int x, int y, TileType type) {
        MapTile tile = new MapTile(new Coordinates(x, y), type, 2);
        state.getMapTiles().put(tile.getCoordinates(), tile);
        return tile;
    }

    /** Creates a geologist standing on his target flag, ready to survey. */
    private Worker geologistAt(GameState state, Coordinates position, int surveys) {
        Worker geologist = new Worker(0);
        geologist.setType(WorkerType.GEOLOGIST);
        geologist.setPosition(position);
        geologist.setState(WorkerState.WORKING);
        geologist.setSurveysLeft(surveys);
        state.getWorkers().add(geologist);
        return geologist;
    }

    @Test
    void surveysPlantOreSignsOnMountainTiles() {
        GameState state = newState();
        addTile(state, 0, 0, TileType.GRASS);
        MapTile mountain = addTile(state, 1, 1, TileType.MOUNTAIN);
        mountain.setNaturalResource(new NaturalResourceNode(ResourceType.GOLD, 5));

        Worker geologist = geologistAt(state, new Coordinates(0, 0), 3);
        system.process(state);

        assertTrue(mountain.isSurveyed());
        assertEquals(ResourceType.GOLD, mountain.getGeologistSign());
        assertEquals(2, geologist.getSurveysLeft());
        assertEquals(GameConfig.GEOLOGIST_SURVEY_TICKS, geologist.getSurveyCooldown());
    }

    @Test
    void barrenMountainsGetAnEmptySign() {
        GameState state = newState();
        addTile(state, 0, 0, TileType.GRASS);
        MapTile mountain = addTile(state, 1, 1, TileType.MOUNTAIN);

        geologistAt(state, new Coordinates(0, 0), 3);
        system.process(state);

        assertTrue(mountain.isSurveyed());
        assertNull(mountain.getGeologistSign());
    }

    @Test
    void eachTileIsOnlySurveyedOnce() {
        GameState state = newState();
        addTile(state, 0, 0, TileType.GRASS);
        MapTile first = addTile(state, 1, 1, TileType.MOUNTAIN);
        MapTile second = addTile(state, 0, 2, TileType.MOUNTAIN);
        second.setNaturalResource(new NaturalResourceNode(ResourceType.IRON, 5));

        Worker geologist = geologistAt(state, new Coordinates(0, 0), 5);
        // Two surveys with the cooldown in between
        for (int i = 0; i < 2 * (GameConfig.GEOLOGIST_SURVEY_TICKS + 1); i++) {
            system.process(state);
        }

        assertTrue(first.isSurveyed());
        assertTrue(second.isSurveyed());
        assertEquals(3, geologist.getSurveysLeft());
    }

    @Test
    void geologistHeadsHomeWhenNothingIsLeftToSurvey() {
        GameState state = newState();
        addTile(state, 0, 0, TileType.GRASS); // no mountain anywhere

        Worker geologist = geologistAt(state, new Coordinates(0, 0), 5);
        system.process(state);

        assertNull(geologist.getType());
        assertNull(geologist.getState()); // no warehouse to walk back to
    }

    @Test
    void geologistStopsAfterHisSurveyQuota() {
        GameState state = newState();
        addTile(state, 0, 0, TileType.GRASS);
        addTile(state, 1, 1, TileType.MOUNTAIN);
        addTile(state, 0, 2, TileType.MOUNTAIN);

        Worker geologist = geologistAt(state, new Coordinates(0, 0), 1);
        // First survey, cooldown, then quota exhausted → dismissal
        for (int i = 0; i < GameConfig.GEOLOGIST_SURVEY_TICKS + 2; i++) {
            system.process(state);
            if (geologist.getType() == null) break;
        }

        assertNull(geologist.getType());
    }
}
