package com.example.team18;

import android.content.Context;

final class FocusTime {
    enum Mode {
        COUNTDOWN,
        COUNT_UP;

        static Mode fromStoredValue(String value) {
            return COUNT_UP.name().equals(value) ? COUNT_UP : COUNTDOWN;
        }
    }

    static int earnedCoins(int seconds, Mode mode, boolean completed) {
        // One coin per 40 seconds is the integer equivalent of 1.5 coins per minute.
        return seconds / (mode == Mode.COUNTDOWN && completed ? 40 : 60);
    }

    static int earnedPlanCoins(int totalSeconds, boolean completed) {
        int wholeMinutes = totalSeconds / 60;
        return completed ? wholeMinutes * 3 / 2 : wholeMinutes;
    }

    static String format(Context context, long seconds) {
        long minutes = seconds / 60;
        long remainder = seconds % 60;
        if (minutes >= 60) {
            return remainder == 0
                    ? context.getString(R.string.focus_duration_hours_minutes, minutes / 60, minutes % 60)
                    : context.getString(R.string.focus_duration_hours_seconds,
                            minutes / 60, minutes % 60, remainder);
        }
        if (remainder == 0) return context.getString(R.string.focus_duration_minutes, minutes);
        if (minutes == 0) return context.getString(R.string.focus_duration_seconds, remainder);
        return context.getString(R.string.focus_duration_minutes_seconds, minutes, remainder);
    }
}
