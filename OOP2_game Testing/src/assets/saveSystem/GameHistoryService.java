package assets.saveSystem;

import java.util.Collections;
import java.util.List;

/**
 * High-level service the game panels interact with.
 *
 * Hides repository details (file paths, exception handling) behind
 * simple, intention-revealing methods. Errors are logged to stderr
 * rather than propagated, so a save failure never crashes the game.
 *
 * ── How to use from GamePanel / ArcadeGamePanel / SurivivalGamePanel ──────
 *
 *  // At match end (inside your match-complete lambda):
 *  GameHistoryService.getInstance().recordPvpMatch(
 *      p1WonMatch, p1Wins, p2Wins, totalRounds,
 *      ALL_CHARACTERS.get(safePlayerIndex).name,
 *      ALL_CHARACTERS.get(safeEnemyIndex).name,
 *      selectedBattleground
 *  );
 *
 *  // For Survival mode:
 *  GameHistoryService.getInstance().recordSurvivalMatch(
 *      p1WonMatch, survivalScore,
 *      ALL_CHARACTERS.get(safePlayerIndex).name,
 *      selectedBattleground
 *  );
 *
 * OOP Principles applied:
 *  - Singleton     : one shared service instance across the whole application
 *  - Facade        : wraps repository + exception handling behind a clean API
 *  - Dependency Inversion : internally uses GameHistoryRepository (interface)
 *  - Encapsulation : singleton instance and repo are private
 */
public final class GameHistoryService {

    // ── Singleton ──────────────────────────────────────────────────────────
    private static GameHistoryService instance;

    private final GameHistoryRepository repository;

    /** Returns the application-wide singleton, creating it on first call. */
    public static synchronized GameHistoryService getInstance() {
        if (instance == null) {
            instance = new GameHistoryService(new Filegamehistoryrepository());
        }
        return instance;
    }

    /**
     * Allows injection of a custom repository (useful for unit tests).
     * Call this BEFORE the first {@link #getInstance()} if you want to swap
     * the implementation.
     */
    public static synchronized void setRepository(GameHistoryRepository repo) {
        instance = new GameHistoryService(repo);
    }

    private GameHistoryService(GameHistoryRepository repository) {
        this.repository = repository;
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Records the result of a PVP or ARCADE match.
     *
     * @param player1Won      true if Player 1 / the human won
     * @param player1Wins     rounds won by P1
     * @param player2Wins     rounds won by P2
     * @param totalRounds     total rounds played
     * @param player1CharName character name of P1
     * @param player2CharName character name of P2 (or "BOT" for arcade)
     * @param battleground    map/battleground name
     * @param mode            {@link GameMode#PVP} or {@link GameMode#ARCADE}
     */
    public void recordMatch(boolean player1Won,
                            int player1Wins,
                            int player2Wins,
                            int totalRounds,
                            String player1CharName,
                            String player2CharName,
                            String battleground,
                            GameMode mode) {

        String winner = player1Won ? player1CharName : player2CharName;

        GameRecord record = new GameRecord.Builder()
                .timestamp(GameRecord.nowTimestamp())
                .gameMode(mode)
                .player1Name(player1CharName)
                .player2Name(player2CharName)
                .winnerName(winner)
                .player1Wins(player1Wins)
                .player2Wins(player2Wins)
                .totalRounds(totalRounds)
                .survivalScore(0)
                .battleground(sanitize(battleground))
                .build();

        persist(record);
    }

    /**
     * Convenience overload for PVP matches (most common case).
     */
    public void recordPvpMatch(boolean player1Won,
                               int player1Wins,
                               int player2Wins,
                               int totalRounds,
                               String player1CharName,
                               String player2CharName,
                               String battleground) {

        recordMatch(player1Won, player1Wins, player2Wins, totalRounds,
                player1CharName, player2CharName, battleground, GameMode.PVP);
    }

    /**
     * Convenience overload for Arcade matches (player vs BOT).
     */
    public void recordArcadeMatch(boolean playerWon,
                                  int playerWins,
                                  int botWins,
                                  int totalRounds,
                                  String playerCharName,
                                  String opponentCharName,
                                  String battleground) {

        recordMatch(playerWon, playerWins, botWins, totalRounds,
                playerCharName, opponentCharName, battleground, GameMode.ARCADE);
    }

    /**
     * Records a Survival mode session.
     *
     * @param playerWon       true if the player survived all encounters
     * @param survivalScore   final score
     * @param playerCharName  character name
     * @param battleground    map/battleground name
     */
    public void recordSurvivalMatch(boolean playerWon,
                                    int survivalScore,
                                    String playerCharName,
                                    String battleground) {

        GameRecord record = new GameRecord.Builder()
                .timestamp(GameRecord.nowTimestamp())
                .gameMode(GameMode.SURVIVAL)
                .player1Name(playerCharName)
                .player2Name("BOT")
                .winnerName(playerWon ? playerCharName : "BOT")
                .player1Wins(playerWon ? 1 : 0)
                .player2Wins(playerWon ? 0 : 1)
                .totalRounds(1)
                .survivalScore(survivalScore)
                .battleground(sanitize(battleground))
                .build();

        persist(record);
    }

    // ── Query helpers ──────────────────────────────────────────────────────

    /**
     * Returns all records, oldest first. Returns an empty list on error.
     */
    public List<GameRecord> getHistory() {
        try {
            return repository.loadAll();
        } catch (SaveSystemException e) {
            System.err.println("[SaveSystem] Could not load history: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Returns the N most recent records, newest first.
     */
    public List<GameRecord> getRecentHistory(int limit) {
        try {
            return repository.loadRecent(limit);
        } catch (SaveSystemException e) {
            System.err.println("[SaveSystem] Could not load recent history: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Returns all records for a specific mode.
     */
    public List<GameRecord> getHistoryByMode(GameMode mode) {
        try {
            return repository.loadByMode(mode);
        } catch (SaveSystemException e) {
            System.err.println("[SaveSystem] Could not filter history: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Returns the highest survival score ever recorded.
     * Returns 0 if there are no survival records.
     */
    public int getHighScore() {
        return getHistoryByMode(GameMode.SURVIVAL).stream()
                .mapToInt(GameRecord::getSurvivalScore)
                .max()
                .orElse(0);
    }

    /**
     * Clears all saved history. Errors are logged, not thrown.
     */
    public void clearHistory() {
        try {
            repository.clearAll();
        } catch (SaveSystemException e) {
            System.err.println("[SaveSystem] Could not clear history: " + e.getMessage());
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────

    /** Persists a record and logs any error without crashing the game. */
    private void persist(GameRecord record) {
        try {
            repository.save(record);
            System.out.println("[SaveSystem] Saved: " + record);
        } catch (SaveSystemException e) {
            System.err.println("[SaveSystem] ERROR – could not save record: " + e.getMessage());
        }
    }

    /** Removes pipe characters and trims whitespace from user-facing strings. */
    private static String sanitize(String value) {
        if (value == null || value.isBlank()) return "Unknown";
        return value.trim().replace("|", "-");
    }
}