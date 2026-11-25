package com.example.team18;

import org.junit.Test;

import static com.example.team18.FocusCycle.NextPhase.FINISHED;
import static com.example.team18.FocusCycle.NextPhase.FOCUS;
import static com.example.team18.FocusCycle.NextPhase.REST;
import static org.junit.Assert.assertEquals;

public class FocusCycleTest {
    @Test public void everyCompletedFocusHasItsRestIncludingTheLastRound() {
        assertEquals(REST, FocusCycle.afterPhase(false, 300, 1, 3));
        assertEquals(REST, FocusCycle.afterPhase(false, 300, 3, 3));
        assertEquals(FOCUS, FocusCycle.afterPhase(true, 300, 1, 3));
        assertEquals(FINISHED, FocusCycle.afterPhase(true, 300, 3, 3));
    }

    @Test public void zeroDurationRestIsSkipped() {
        assertEquals(FOCUS, FocusCycle.afterPhase(false, 0, 1, 2));
        assertEquals(FINISHED, FocusCycle.afterPhase(false, 0, 2, 2));
    }

    @Test public void roundCountStaysBetweenOneAndTen() {
        assertEquals(1, FocusCycle.clampRoundCount(0));
        assertEquals(7, FocusCycle.clampRoundCount(7));
        assertEquals(10, FocusCycle.clampRoundCount(11));
    }
}
