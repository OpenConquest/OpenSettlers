package fr.opensettlers.persistence;

import fr.opensettlers.entities.building.BuildingFactory;
import fr.opensettlers.entities.world.Flag;
import fr.opensettlers.entities.world.MapTile;
import fr.opensettlers.entities.building.MilitaryBuilding;
import fr.opensettlers.entities.world.NaturalResourceNode;
import fr.opensettlers.entities.unit.Soldier;
import fr.opensettlers.entities.building.StorageBuilding;
import fr.opensettlers.state.GameState;
import fr.opensettlers.utils.enums.BuildingName;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.enums.ResourceType;
import fr.opensettlers.utils.enums.SoldierRank;
import fr.opensettlers.utils.enums.TileType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that a game state survives a save/restore round-trip: terrain,
 * buildings, stock, garrisons, flags and roads are all reconstructed.
 */
class GameSnapshotMapperTest {

    private GameState sampleState() {
        GameState state = new GameState(UUID.randomUUID(), new ArrayList<>());
        state.setPlayerCount(2);
        state.getAiPlayers().add(1);
        state.setCurrentTick(123);

        Map<Coordinates, MapTile> tiles = new HashMap<>();
        MapTile forest = new MapTile(new Coordinates(2, 0), TileType.FOREST, 1);
        forest.setNaturalResource(new NaturalResourceNode(ResourceType.LOG, 7));
        tiles.put(forest.getCoordinates(), forest);
        tiles.put(new Coordinates(0, 0), new MapTile(new Coordinates(0, 0), TileType.GRASS, 0));
        state.setMapTiles(tiles);

        StorageBuilding hq = (StorageBuilding) BuildingFactory.createBuilding(
                BuildingName.HEADQUARTERS, 0, new Coordinates(0, 0));
        hq.getStoredResources().put(ResourceType.PLANK, 17);
        state.getBuildings().add(hq);
        state.getRoadNetwork().addFlag(hq.getAttachedFlag());

        MilitaryBuilding barracks = (MilitaryBuilding) BuildingFactory.createBuilding(
                BuildingName.BARRACKS, 0, new Coordinates(4, 4));
        Soldier soldier = new Soldier(0, new Coordinates(4, 4));
        soldier.setRank(SoldierRank.SERGEANT);
        barracks.addSoldier(soldier);
        state.getBuildings().add(barracks);

        Flag flag = new Flag(UUID.randomUUID(), 0, new Coordinates(2, 0));
        state.getFlags().add(flag);
        state.getRoadNetwork().addFlag(flag);
        state.getRoadNetwork().addRoad(hq.getAttachedFlag(), flag,
                new ArrayList<>(List.of(new Coordinates(1, 0))));

        return state;
    }

    @Test
    void restoresTheFullGame() {
        GameState original = sampleState();

        GameSnapshot snapshot = GameSnapshotMapper.toSnapshot(original);
        GameState restored = GameSnapshotMapper.toGameState(snapshot, UUID.randomUUID());

        assertEquals(123, restored.getCurrentTick());
        assertEquals(2, restored.getPlayerCount());
        assertTrue(restored.getAiPlayers().contains(1));
        assertEquals(2, restored.getMapTiles().size());

        StorageBuilding hq = restored.getBuildings().stream()
                .filter(b -> b instanceof StorageBuilding).map(b -> (StorageBuilding) b)
                .findFirst().orElseThrow();
        assertEquals(17, hq.getStoredResources().get(ResourceType.PLANK));

        MilitaryBuilding barracks = restored.getBuildings().stream()
                .filter(b -> b instanceof MilitaryBuilding).map(b -> (MilitaryBuilding) b)
                .findFirst().orElseThrow();
        assertEquals(1, barracks.getSoldiers().size());
        assertEquals(SoldierRank.SERGEANT, barracks.getSoldiers().get(0).getRank());

        assertEquals(1, restored.getFlags().size());
        assertEquals(1, restored.getRoadNetwork().getAllRoads().size());
    }

    @Test
    void preservesTerrainResources() {
        GameState restored = GameSnapshotMapper.toGameState(
                GameSnapshotMapper.toSnapshot(sampleState()), UUID.randomUUID());

        MapTile forest = restored.getTile(new Coordinates(2, 0));
        assertNotNull(forest);
        assertEquals(TileType.FOREST, forest.getType());
        assertNotNull(forest.getNaturalResource());
        assertEquals(ResourceType.LOG, forest.getNaturalResource().getType());
        assertEquals(7, forest.getNaturalResource().getQuantity());
    }
}
