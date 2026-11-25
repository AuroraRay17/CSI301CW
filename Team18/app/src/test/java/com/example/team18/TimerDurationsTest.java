package com.example.team18;

import org.junit.Test;

import static com.example.team18.TimerDurations.Unit.HOURS;
import static com.example.team18.TimerDurations.Unit.MINUTES;
import static com.example.team18.TimerDurations.Unit.SECONDS;
import static org.junit.Assert.assertEquals;

public class TimerDurationsTest {
    @Test
    public void steppingWrapsOnlyTheSelectedPosition() {
        assertEquals(3600 + 24 * 60 + 45, MINUTES.step(3600 + 23 * 60 + 45, 1));
        assertEquals(23 * 60 + 45, HOURS.step(3600 + 23 * 60 + 45, -1));
        assertEquals(3600 + 45, MINUTES.step(3600 + 59 * 60 + 45, 1));
        assertEquals(3600 + 23 * 60 + 59, SECONDS.step(3600 + 23 * 60, -1));
        assertEquals(23 * 3600, HOURS.step(0, -1));
        assertEquals(3599, HOURS.step(TimerDurations.MAX_SECONDS, 1));
    }

    @Test
    public void roundPickerWrapsInBothDirectionsAndAcrossMultipleSteps() {
        assertEquals(10, TimerDurations.wrap(0, 1, 10));
        assertEquals(1, TimerDurations.wrap(11, 1, 10));
        assertEquals(3, TimerDurations.wrap(13, 1, 10));
        assertEquals(8, TimerDurations.wrap(-2, 1, 10));
    }

    @Test
    public void formatsExactSeconds() {
        assertEquals("00:00:00", TimerDurations.formatClock(0));
        assertEquals("01:23:45", TimerDurations.formatClock(5025));
        assertEquals("23:59:59", TimerDurations.formatClock(TimerDurations.MAX_SECONDS));
    }
}
