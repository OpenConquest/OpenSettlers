package fr.opensettlers.service;

import fr.opensettlers.entities.BuildingFactory;
import fr.opensettlers.entities.ConstructionSite;
import fr.opensettlers.entities.MapTile;
import fr.opensettlers.state.GameState;
import fr.opensettlers.utils.BuildingName;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.TileType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the shared, validated player/AI operations: terrain, territory and the
 * coastal rule for naval buildings.
 */
class GameActionsTest {

    /**
     * Builds a state with a grass map and a player-0 headquarters projecting
     * territory over the whole map.
     */
    private GameState grassStateWithHq() {
        GameState state = new GameState(UUID.randomUUID(), new ArrayList<>());
        Map<Coordinates, MapTile> tiles = new HashMap<>();
        for (int x = 0; x <= 16; x++) {
            for (int y = 0; y <= 32; y++) {
                Coordinates c = new Coordinates(x, y);
                tiles.put(c, new MapTile(c, TileType.GRASS, 0));
            }
        }
        state.setMapTiles(tiles);
        state.setPlayerCount(1);
        state.getBuildings().add(BuildingFactory.createBuilding(
                BuildingName.HEADQUARTERS, 0, new Coordinates(8, 16)));
        state.getTerritoryManager().recalculate(state);
        return state;
    }

    @Test
    void placesABuildingOnEmptyOwnedGrass() {
        GameState state = grassStateWithHq();

        boolean placed = GameActions.placeBuilding(state, 0, BuildingName.WOODCUTTER, new Coordinates(8, 20));

        assertTrue(placed);
        assertTrue(state.getBuildings().stream()
                .anyMatch(b -> b instanceof ConstructionSite cs
                        && cs.getTargetBuildingType() == BuildingName.WOODCUTTER));
    }

    @Test
    void rejectsBuildingOutsideTerritory() {
        GameState state = grassStateWithHq();

        boolean placed = GameActions.placeBuilding(state, 1, BuildingName.WOODCUTTER, new Coordinates(8, 20));

        assertFalse(placed);
    }

    @Test
    void minesRequireMountainTerrain() {
        GameState state = grassStateWithHq();
        state.getTile(new Coordinates(8, 20)).setType(TileType.MOUNTAIN);

        assertTrue(GameActions.isPlacementValid(state, BuildingName.MINE, new Coordinates(8, 20), 0));
        assertFalse(GameActions.isPlacementValid(state, BuildingName.WOODCUTTER, new Coordinates(8, 20), 0));
    }

    @Test
    void harborsRequireAdjacentWater() {
        GameState state = grassStateWithHq();

        assertFalse(GameActions.isPlacementValid(state, BuildingName.HARBOR, new Coordinates(8, 20), 0));

        state.getTile(new Coordinates(8, 22)).setType(TileType.WATER); // south neighbor of (8,20)
        assertTrue(GameActions.isPlacementValid(state, BuildingName.HARBOR, new Coordinates(8, 20), 0));
    }

    @Test
    void rejectsBuildingOnAnOccupiedTile() {
        GameState state = grassStateWithHq();
        GameActions.placeBuilding(state, 0, BuildingName.WOODCUTTER, new Coordinates(8, 20));

        boolean second = GameActions.placeBuilding(state, 0, BuildingName.QUARRY, new Coordinates(8, 20));

        assertFalse(second);
    }
}
