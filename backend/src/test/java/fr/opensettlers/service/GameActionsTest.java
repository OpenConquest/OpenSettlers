package fr.opensettlers.service;

import fr.opensettlers.entities.BuildingFactory;
import fr.opensettlers.entities.ConstructionSite;
import fr.opensettlers.entities.Flag;
import fr.opensettlers.entities.MapTile;
import fr.opensettlers.entities.NaturalResourceNode;
import fr.opensettlers.state.GameState;
import fr.opensettlers.utils.BuildingName;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.ResourceType;
import fr.opensettlers.utils.TileType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    void minesRequireMountainTerrainBearingTheirOre() {
        GameState state = grassStateWithHq();
        MapTile mountain = state.getTile(new Coordinates(8, 20));
        mountain.setType(TileType.MOUNTAIN);
        mountain.setNaturalResource(new NaturalResourceNode(ResourceType.IRON, 5));

        assertTrue(GameActions.isPlacementValid(state, BuildingName.IRON_MINE, new Coordinates(8, 20), 0));
        // The wrong mine type for the deposit is rejected
        assertFalse(GameActions.isPlacementValid(state, BuildingName.COAL_MINE, new Coordinates(8, 20), 0));
        assertFalse(GameActions.isPlacementValid(state, BuildingName.WOODCUTTER, new Coordinates(8, 20), 0));
    }

    @Test
    void harborsRequireAdjacentWater() {
        GameState state = grassStateWithHq();

        assertFalse(GameActions.isPlacementValid(state, BuildingName.HARBOR, new Coordinates(8, 24), 0));

        state.getTile(new Coordinates(8, 26)).setType(TileType.WATER); // south neighbor of (8,24)
        assertTrue(GameActions.isPlacementValid(state, BuildingName.HARBOR, new Coordinates(8, 24), 0));
    }

    @Test
    void castleSizedSitesNeedMoreSpacingThanHuts() {
        GameState state = grassStateWithHq();
        // Distance 2 from the HQ: fine for a hut, too close for a castle-size farm
        assertTrue(GameActions.isPlacementValid(state, BuildingName.WOODCUTTER, new Coordinates(8, 20), 0));
        assertFalse(GameActions.isPlacementValid(state, BuildingName.FARM, new Coordinates(8, 20), 0));
        // Distance 4: both are fine
        assertTrue(GameActions.isPlacementValid(state, BuildingName.FARM, new Coordinates(8, 24), 0));
    }

    @Test
    void steepSlopesOnlyAllowSmallSites() {
        GameState state = grassStateWithHq();
        // Raise a neighbor of (8,24) by 2 elevation steps
        state.getTile(new Coordinates(8, 26)).setElevation(2);

        assertTrue(GameActions.isPlacementValid(state, BuildingName.WOODCUTTER, new Coordinates(8, 24), 0));
        assertTrue(GameActions.isPlacementValid(state, BuildingName.SAWMILL, new Coordinates(8, 24), 0));
        assertFalse(GameActions.isPlacementValid(state, BuildingName.FARM, new Coordinates(8, 24), 0));
    }

    @Test
    void rejectsFlagOnAnOccupiedTile() {
        GameState state = grassStateWithHq();

        assertNotNull(GameActions.placeFlag(state, 0, new Coordinates(8, 20)));
        // A second flag on the same tile is rejected
        assertNull(GameActions.placeFlag(state, 0, new Coordinates(8, 20)));
        // A flag on the headquarters tile is rejected too
        assertNull(GameActions.placeFlag(state, 0, new Coordinates(8, 16)));
    }

    @Test
    void rejectsBuildingOnAnOccupiedTile() {
        GameState state = grassStateWithHq();
        GameActions.placeBuilding(state, 0, BuildingName.WOODCUTTER, new Coordinates(8, 20));

        boolean second = GameActions.placeBuilding(state, 0, BuildingName.QUARRY, new Coordinates(8, 20));

        assertFalse(second);
    }

    @Test
    void linksTwoFlagsWithARoadOverGrass() {
        GameState state = grassStateWithHq();
        Flag a = GameActions.placeFlag(state, 0, new Coordinates(8, 20));
        Flag b = GameActions.placeFlag(state, 0, new Coordinates(8, 24));
        assertNotNull(a);
        assertNotNull(b);

        boolean linked = GameActions.linkFlags(state, a.getId(), b.getId(),
                List.of(new Coordinates(8, 22)));

        assertTrue(linked);
        assertEquals(1, state.getRoadNetwork().getAllRoads().size());
    }

    @Test
    void rejectsARoadCrossingAForestTile() {
        GameState state = grassStateWithHq();
        state.getTile(new Coordinates(8, 22)).setType(TileType.FOREST);
        Flag a = GameActions.placeFlag(state, 0, new Coordinates(8, 20));
        Flag b = GameActions.placeFlag(state, 0, new Coordinates(8, 24));

        boolean linked = GameActions.linkFlags(state, a.getId(), b.getId(),
                List.of(new Coordinates(8, 22)));

        assertFalse(linked);
        assertTrue(state.getRoadNetwork().getAllRoads().isEmpty());
    }

    @Test
    void rejectsARoadWhosePathIsNotAContiguousChain() {
        GameState state = grassStateWithHq();
        Flag a = GameActions.placeFlag(state, 0, new Coordinates(8, 20));
        Flag b = GameActions.placeFlag(state, 0, new Coordinates(8, 24));

        // (8,28) is adjacent to neither endpoint: the chain has a gap.
        boolean linked = GameActions.linkFlags(state, a.getId(), b.getId(),
                List.of(new Coordinates(8, 28)));

        assertFalse(linked);
        assertTrue(state.getRoadNetwork().getAllRoads().isEmpty());
    }
}
