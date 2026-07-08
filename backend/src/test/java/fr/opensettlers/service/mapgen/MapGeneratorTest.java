package fr.opensettlers.service.mapgen;

import fr.opensettlers.entities.MapTile;
import fr.opensettlers.utils.ResourceType;
import fr.opensettlers.utils.TileType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke tests for the continental map generator.
 */
class MapGeneratorTest {

    @Test
    void generatesAFullyPopulatedGrid() {
        MapTile[][] grid = new MapGenerator().generateContinentalGrid(64, 64);

        Set<TileType> seenTypes = EnumSet.noneOf(TileType.class);
        for (MapTile[] row : grid) {
            for (MapTile tile : row) {
                assertNotNull(tile);
                seenTypes.add(tile.getType());
            }
        }
        assertTrue(seenTypes.contains(TileType.WATER), "Island maps must have surrounding water");
        assertTrue(seenTypes.contains(TileType.GRASS), "Maps must have buildable grass");
    }

    @Test
    void resourceNodesOnlyAppearOnMatchingTerrain() {
        MapTile[][] grid = new MapGenerator().generateContinentalGrid(64, 64);

        for (MapTile[] row : grid) {
            for (MapTile tile : row) {
                if (tile.getNaturalResource() == null) continue;
                ResourceType res = tile.getNaturalResource().getType();
                switch (tile.getType()) {
                    case WATER -> assertEquals(ResourceType.FISH, res);
                    case MOUNTAIN -> assertTrue(
                            res == ResourceType.IRON || res == ResourceType.COAL
                                    || res == ResourceType.GOLD || res == ResourceType.STONE,
                            "Unexpected mountain resource " + res);
                    case FOREST -> assertEquals(ResourceType.LOG, res);
                    case HILLS, STONE -> assertEquals(ResourceType.STONE, res);
                    case GRASS -> assertEquals(ResourceType.MEAT, res);
                    default -> fail("Resource " + res + " on unexpected terrain " + tile.getType());
                }
            }
        }
    }
}
