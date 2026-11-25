package com.example.team18;

final class FocusCycle {
    static final int MIN_ROUNDS = 1;
    static final int MAX_ROUNDS = 10;

    enum NextPhase { REST, FOCUS, FINISHED }

    static int clampRoundCount(int value) {
        return Math.max(MIN_ROUNDS, Math.min(MAX_ROUNDS, value));
    }

    static NextPhase afterPhase(boolean rest, int restSeconds, int currentRound, int totalRounds) {
        if (!rest && restSeconds > 0) return NextPhase.REST;
        return currentRound < totalRounds ? NextPhase.FOCUS : NextPhase.FINISHED;
    }
}
