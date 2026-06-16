package fr.opensettlers.service;

import fr.opensettlers.entities.MilitaryBuilding;
import fr.opensettlers.entities.ProcessingBuilding;
import fr.opensettlers.entities.Recipe;
import fr.opensettlers.state.GameState;
import fr.opensettlers.utils.BuildingName;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the per-building player controls added on top of the engine: pausing
 * production and toggling gold-coin delivery, including ownership/type guards.
 */
class GameControlsTest {

    private GameState newState() {
        return new GameState(UUID.randomUUID(), new ArrayList<>());
    }

    @Test
    void setProductionPausesAndResumesOwnBuilding() {
        GameState state = newState();
        ProcessingBuilding sawmill = new ProcessingBuilding(
                0, new Coordinates(0, 0), new Recipe(Map.of(ResourceType.LOG, 1), ResourceType.PLANK));
        state.getBuildings().add(sawmill);

        assertFalse(sawmill.isProductionPaused());
        assertTrue(GameActions.setProduction(state, 0, sawmill.getId(), false)); // pause
        assertTrue(sawmill.isProductionPaused());
        assertTrue(GameActions.setProduction(state, 0, sawmill.getId(), true)); // resume
        assertFalse(sawmill.isProductionPaused());
    }

    @Test
    void setProductionRejectsOtherPlayers() {
        GameState state = newState();
        ProcessingBuilding sawmill = new ProcessingBuilding(
                0, new Coordinates(0, 0), new Recipe(Map.of(ResourceType.LOG, 1), ResourceType.PLANK));
        state.getBuildings().add(sawmill);

        assertFalse(GameActions.setProduction(state, 1, sawmill.getId(), false));
        assertFalse(sawmill.isProductionPaused());
    }

    @Test
    void setCoinDeliveryTogglesOwnMilitaryBuilding() {
        GameState state = newState();
        MilitaryBuilding guard = new MilitaryBuilding(
                0, new Coordinates(2, 0), BuildingName.GUARD_HOUSE, 3, 2);
        state.getBuildings().add(guard);

        assertTrue(guard.isCoinsAllowed());
        assertTrue(GameActions.setCoinDelivery(state, 0, guard.getId(), false));
        assertFalse(guard.isCoinsAllowed());
        assertFalse(GameActions.setCoinDelivery(state, 1, guard.getId(), true)); // wrong owner
        assertFalse(guard.isCoinsAllowed());
    }

    @Test
    void controlsRejectMismatchedBuildingTypes() {
        GameState state = newState();
        MilitaryBuilding guard = new MilitaryBuilding(
                0, new Coordinates(2, 0), BuildingName.GUARD_HOUSE, 3, 2);
        state.getBuildings().add(guard);

        // A military building has no production to pause.
        assertFalse(GameActions.setProduction(state, 0, guard.getId(), false));
        // A non-existent target is rejected.
        assertFalse(GameActions.setCoinDelivery(state, 0, UUID.randomUUID(), false));
    }
}
