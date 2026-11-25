package com.example.team18;

import android.content.SharedPreferences;

import java.util.Locale;

final class TimerDurations {
    static final int MAX_SECONDS = 24 * 60 * 60 - 1;

    enum Unit {
        HOURS(3600, 23, R.string.timer_hours),
        MINUTES(60, 59, R.string.timer_minutes),
        SECONDS(1, 59, R.string.timer_seconds);

        final int secondsPerUnit;
        final int maximum;
        final int label;

        Unit(int secondsPerUnit, int maximum, int label) {
            this.secondsPerUnit = secondsPerUnit;
            this.maximum = maximum;
            this.label = label;
        }

        int read(int totalSeconds) {
            return totalSeconds / secondsPerUnit % (maximum + 1);
        }

        int step(int totalSeconds, int amount) {
            int current = read(totalSeconds);
            int next = wrap(current + amount, 0, maximum);
            return totalSeconds + (next - current) * secondsPerUnit;
        }
    }

    static int wrap(int value, int minimum, int maximum) {
        return minimum + Math.floorMod(value - minimum, maximum - minimum + 1);
    }

    static int read(SharedPreferences preferences, boolean rest) {
        int minutes = preferences.getInt(rest ? "break_minutes" : "focus_minutes", rest ? 5 : 25);
        int legacySeconds = (int) Math.min(MAX_SECONDS, Math.max(0, minutes * 60L));
        return Math.max(0, Math.min(MAX_SECONDS, preferences.getInt(key(rest), legacySeconds)));
    }

    static void write(SharedPreferences.Editor editor, boolean rest, int seconds) {
        editor.putInt(key(rest), seconds);
    }

    static boolean hasActiveRound(SharedPreferences preferences) {
        boolean rest = preferences.getBoolean("break_mode", false);
        long durationMs = read(preferences, rest) * 1000L;
        return preferences.getBoolean("running", false)
                || preferences.contains(MainActivity.ROUND_ID_KEY)
                || preferences.getLong("remaining_ms", durationMs) < durationMs;
    }

    private static String key(boolean rest) {
        return rest ? "break_duration_seconds" : "focus_duration_seconds";
    }

    static String formatClock(long seconds) {
        return String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600,
                seconds / 60 % 60, seconds % 60);
    }

}
