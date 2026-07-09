package fr.opensettlers.systems.economy;

import fr.opensettlers.service.GameActions;
import fr.opensettlers.state.GameState;
import fr.opensettlers.utils.enums.BuildingName;
import fr.opensettlers.utils.enums.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the per-player resource distribution mechanic that {@link EconomySystem}
 * uses to order competing demands for a scarce good.
 */
class EconomyDistributionTest {

    private GameState newState() {
        return new GameState(UUID.randomUUID(), new ArrayList<>());
    }

    @Test
    void rankOfReflectsPriorityOrder() {
        List<BuildingName> coal = List.of(BuildingName.FOUNDRY, BuildingName.ARMORY, BuildingName.MINT);
        assertEquals(0, EconomySystem.rankOf(coal, BuildingName.FOUNDRY));
        assertEquals(1, EconomySystem.rankOf(coal, BuildingName.ARMORY));
        assertEquals(2, EconomySystem.rankOf(coal, BuildingName.MINT));
        assertTrue(EconomySystem.rankOf(coal, BuildingName.FOUNDRY)
                < EconomySystem.rankOf(coal, BuildingName.MINT));
    }

    @Test
    void rankOfReturnsMaxForUnlistedOrNull() {
        List<BuildingName> coal = List.of(BuildingName.FOUNDRY, BuildingName.ARMORY);
        assertEquals(Integer.MAX_VALUE, EconomySystem.rankOf(coal, BuildingName.MINT));
        assertEquals(Integer.MAX_VALUE, EconomySystem.rankOf(null, BuildingName.FOUNDRY));
        assertEquals(Integer.MAX_VALUE, EconomySystem.rankOf(coal, null));
    }

    @Test
    void defaultDistributionCoversContestedGoods() {
        Map<ResourceType, List<BuildingName>> d = GameState.defaultDistribution();
        assertEquals(BuildingName.FOUNDRY, d.get(ResourceType.COAL).get(0));
        assertTrue(d.get(ResourceType.COAL).contains(BuildingName.MINT));
        assertEquals(BuildingName.FOUNDRY, d.get(ResourceType.IRON).get(0));
        assertEquals(BuildingName.MILL, d.get(ResourceType.WHEAT).get(0));
        assertEquals(BuildingName.BAKERY, d.get(ResourceType.WATER).get(0));
    }

    @Test
    void setDistributionIsPerPlayerAndReordersPriority() {
        GameState state = newState();
        // Player 0 reprioritises coal to favour the mint over the foundry.
        assertTrue(GameActions.setDistribution(state, 0, ResourceType.COAL,
                List.of(BuildingName.MINT, BuildingName.ARMORY, BuildingName.FOUNDRY)));

        List<BuildingName> p0 = state.getDistributionFor(0).get(ResourceType.COAL);
        assertTrue(EconomySystem.rankOf(p0, BuildingName.MINT)
                < EconomySystem.rankOf(p0, BuildingName.FOUNDRY));

        // Player 1 is untouched and keeps the default (foundry first).
        List<BuildingName> p1 = state.getDistributionFor(1).get(ResourceType.COAL);
        assertTrue(EconomySystem.rankOf(p1, BuildingName.FOUNDRY)
                < EconomySystem.rankOf(p1, BuildingName.MINT));
    }

    @Test
    void setDistributionRejectsNulls() {
        GameState state = newState();
        assertFalse(GameActions.setDistribution(state, 0, null, List.of()));
        assertFalse(GameActions.setDistribution(state, 0, ResourceType.COAL, null));
    }
}
