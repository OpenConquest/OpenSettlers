package fr.opensettlers.persistence;

import fr.opensettlers.entities.building.Building;
import fr.opensettlers.entities.building.BuildingFactory;
import fr.opensettlers.entities.building.MilitaryBuilding;
import fr.opensettlers.entities.building.ProductionBuilding;
import fr.opensettlers.service.GameActions;
import fr.opensettlers.state.GameState;
import fr.opensettlers.utils.enums.BuildingName;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.enums.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that player preferences (distribution priorities, military occupation)
 * and per-building controls (production pause, coin delivery) survive a
 * save/load round-trip through the {@link GameSnapshotMapper}.
 */
class SettingsPersistenceTest {

    private Building findByType(GameState state, BuildingName name) {
        return state.getBuildings().stream()
                .filter(b -> b.getName() == name)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void settingsSurviveSaveAndLoad() {
        GameState state = new GameState(UUID.randomUUID(), new ArrayList<>());
        state.setPlayerCount(1);

        // Player preferences.
        GameActions.setDistribution(state, 0, ResourceType.COAL,
                List.of(BuildingName.MINT, BuildingName.ARMORY, BuildingName.FOUNDRY));
        GameActions.setMilitaryOccupation(state, 0, 50);

        // Per-building controls.
        Building sawmill = BuildingFactory.createBuilding(BuildingName.SAWMILL, 0, new Coordinates(0, 0), state);
        ((ProductionBuilding) sawmill).setProductionPaused(true);
        state.getBuildings().add(sawmill);

        Building guard = BuildingFactory.createBuilding(BuildingName.GUARD_HOUSE, 0, new Coordinates(2, 0), state);
        ((MilitaryBuilding) guard).setCoinsAllowed(false);
        state.getBuildings().add(guard);

        // Round-trip through a snapshot.
        GameSnapshot snap = GameSnapshotMapper.toSnapshot(state);
        GameState restored = GameSnapshotMapper.toGameState(snap, UUID.randomUUID());

        // Distribution order preserved.
        List<BuildingName> coal = restored.getDistributionFor(0).get(ResourceType.COAL);
        assertEquals(BuildingName.MINT, coal.get(0));
        assertEquals(BuildingName.FOUNDRY, coal.get(2));

        // Military occupation preserved.
        assertEquals(50, restored.getMilitaryOccupationOf(0));

        // Per-building controls preserved.
        assertTrue(((ProductionBuilding) findByType(restored, BuildingName.SAWMILL)).isProductionPaused());
        assertFalse(((MilitaryBuilding) findByType(restored, BuildingName.GUARD_HOUSE)).isCoinsAllowed());
    }
}
