package fr.opensettlers.utils;

import fr.opensettlers.entities.unit.Soldier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import fr.opensettlers.utils.enums.SoldierRank;

/**
 * Tests the rank ladder and its effect on soldier health.
 */
class SoldierRankTest {

    @Test
    void ranksFormAnAscendingLadder() {
        assertEquals(SoldierRank.PRIVATE_FIRST_CLASS, SoldierRank.PRIVATE.next());
        assertEquals(SoldierRank.SERGEANT, SoldierRank.PRIVATE_FIRST_CLASS.next());
        assertEquals(SoldierRank.OFFICER, SoldierRank.SERGEANT.next());
        assertEquals(SoldierRank.GENERAL, SoldierRank.OFFICER.next());
        assertEquals(SoldierRank.GENERAL, SoldierRank.GENERAL.next());
        assertFalse(SoldierRank.GENERAL.isPromotable());
    }

    @Test
    void healthGrowsWithRank() {
        int previous = 0;
        for (SoldierRank rank : SoldierRank.values()) {
            assertTrue(rank.getMaxHealth() > previous);
            previous = rank.getMaxHealth();
        }
    }

    @Test
    void promotionHealsTheSoldierToTheNewMaximum() {
        Soldier soldier = new Soldier(0, new Coordinates(0, 0));
        assertEquals(SoldierRank.PRIVATE, soldier.getRank());
        assertEquals(SoldierRank.PRIVATE.getMaxHealth(), soldier.getHealth());

        soldier.setHealth(1);
        soldier.promote();

        assertEquals(SoldierRank.PRIVATE_FIRST_CLASS, soldier.getRank());
        assertEquals(SoldierRank.PRIVATE_FIRST_CLASS.getMaxHealth(), soldier.getHealth());
    }
}
