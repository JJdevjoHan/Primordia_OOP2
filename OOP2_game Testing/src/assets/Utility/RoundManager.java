package assets.Utility;

import engine.GameMode;

public class RoundManager {

    public static final int MAX_ROUNDS = 3;

    public static final int WINS_TO_WIN = 2;

    @FunctionalInterface
    public interface OnRoundStart {
        void onRoundStart(int roundNumber, int p1Wins, int p2Wins);
    }

    @FunctionalInterface
    public interface OnRoundEnd {
        void onRoundEnd(boolean p1WonRound, int roundNumber);
    }

    @FunctionalInterface
    public interface OnMatchEnd {
        void onMatchEnd(boolean p1WonMatch, int p1Wins, int p2Wins, int totalRounds);
    }

    private final GameMode gameMode;
    private final OnRoundStart  onRoundStart;
    private final OnRoundEnd    onRoundEnd;
    private final OnMatchEnd    onMatchEnd;

    private int  currentRound = 1;   // 1-based, never exceeds MAX_ROUNDS
    private int  p1Wins       = 0;
    private int  p2Wins       = 0;
    private boolean matchOver = false;
    private boolean roundInProgress = false;

    public RoundManager(
            GameMode     gameMode,
            OnRoundStart onRoundStart,
            OnRoundEnd   onRoundEnd,
            OnMatchEnd   onMatchEnd) {

        this.gameMode     = gameMode;
        this.onRoundStart = onRoundStart;
        this.onRoundEnd   = onRoundEnd;
        this.onMatchEnd   = onMatchEnd;
    }

    public void startMatch() {
        currentRound    = 1;
        p1Wins          = 0;
        p2Wins          = 0;
        matchOver       = false;
        roundInProgress = true;
        onRoundStart.onRoundStart(currentRound, p1Wins, p2Wins);
    }


    public void recordRoundResult(boolean p1WonRound) {
        if (matchOver || !roundInProgress) return;

        roundInProgress = false;

        if (p1WonRound) p1Wins++; else p2Wins++;

        onRoundEnd.onRoundEnd(p1WonRound, currentRound);

        if (p1Wins >= WINS_TO_WIN || p2Wins >= WINS_TO_WIN || currentRound >= MAX_ROUNDS) {
            matchOver = true;
            boolean p1WonMatch = p1Wins > p2Wins;
            onMatchEnd.onMatchEnd(p1WonMatch, p1Wins, p2Wins, currentRound);
        }
    }

    public void advanceRound() {
        if (matchOver) return;
        currentRound++;
        roundInProgress = true;
        onRoundStart.onRoundStart(currentRound, p1Wins, p2Wins);
    }

    public void reset() {
        currentRound    = 1;
        p1Wins          = 0;
        p2Wins          = 0;
        matchOver       = false;
        roundInProgress = false;
    }

    public int getCurrentRound()   { return currentRound; }

    public int getP1Wins()         { return p1Wins; }

    public int getP2Wins()         { return p2Wins; }

    public boolean isMatchOver()   { return matchOver; }

    public boolean isRoundInProgress() { return roundInProgress; }

    public GameMode getGameMode()  { return gameMode; }

    public String getScoreDisplay(String p1Label, String p2Label) {
        return p1Label + "  " + p1Wins + " \u2013 " + p2Wins + "  " + p2Label;
    }

    public String getWinPips(boolean forP1) {
        int wins = forP1 ? p1Wins : p2Wins;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < WINS_TO_WIN; i++) {
            if (i > 0) sb.append(' ');
            sb.append(i < wins ? '\u25CF' : '\u25CB'); // ● or ○
        }
        return sb.toString();
    }
}
