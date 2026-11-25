package com.example.team18;

import org.junit.Test;

import static com.example.team18.FocusTime.Mode.COUNTDOWN;
import static com.example.team18.FocusTime.Mode.COUNT_UP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FocusTimeTest {
    @Test
    public void countUpAndEarlyCountdownPayOneCoinPerWholeMinute() {
        assertEquals(12, FocusTime.earnedCoins(12 * 60 + 59, COUNT_UP, true));
        assertEquals(12, FocusTime.earnedCoins(12 * 60 + 59, COUNTDOWN, false));
        assertEquals(0, FocusTime.earnedCoins(59, COUNT_UP, true));
        assertEquals(0, FocusTime.earnedCoins(59, COUNTDOWN, false));
        assertEquals(1, FocusTime.earnedCoins(60, COUNTDOWN, false));
    }

    @Test
    public void completedCountdownRoundsDownOnlyAfterApplyingTheBonus() {
        assertEquals(37, FocusTime.earnedCoins(25 * 60, COUNTDOWN, true));
        assertEquals(3, FocusTime.earnedCoins(2 * 60, COUNTDOWN, true));
        assertEquals(1, FocusTime.earnedCoins(60, COUNTDOWN, true));
    }

    @Test
    public void separateRoundsHaveNoSharedRemainder() {
        assertEquals(0, FocusTime.earnedCoins(30, COUNT_UP, true)
                + FocusTime.earnedCoins(30, COUNTDOWN, false));
        assertEquals(1, FocusTime.earnedCoins(60, COUNT_UP, true));
    }

    @Test
    public void multiRoundPlansCombineSecondsBeforeRoundingAndPayOnlyOneBonus() {
        assertEquals(0, FocusTime.earnedPlanCoins(30 + 29, true));
        assertEquals(1, FocusTime.earnedPlanCoins(30 + 30, true));
        assertEquals(1, FocusTime.earnedPlanCoins(30 + 30 + 30, true));
        assertEquals(3, FocusTime.earnedPlanCoins(40 + 40 + 40, true));
        assertEquals(2, FocusTime.earnedPlanCoins(45 + 45 + 30, false));
    }

    @Test
    public void oldMilestonesKeepOnlyTheirEarnedItems() {
        assertFalse(PetItem.GREET.unlockedBeforeUpgrade(299));
        assertTrue(PetItem.GREET.unlockedBeforeUpgrade(300));
        assertFalse(PetItem.PET.unlockedBeforeUpgrade(1499));
        assertTrue(PetItem.PET.unlockedBeforeUpgrade(1500));
        assertFalse(PetItem.BLUE_CUSHION.unlockedBeforeUpgrade(1500));
    }
}
