package assets.saveSystem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Immutable value object representing a single completed game session.
 *
 * OOP Principles applied:
 *  - Encapsulation  : all fields are private final; exposed only via getters
 *  - Abstraction    : callers deal with a clean GameRecord, not raw strings/ints
 *  - Single Responsibility : this class only HOLDS data, nothing else
 */
public final class GameRecord {

    // ── Date format used when serialising to / from the save file ──────────
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── Core fields ────────────────────────────────────────────────────────
    private final String        timestamp;      // when the match ended
    private final GameMode      gameMode;       // PVP / ARCADE / SURVIVAL
    private final String        player1Name;    // character name
    private final String        player2Name;    // character name (or "BOT")
    private final String        winnerName;     // character name of winner
    private final int           player1Wins;    // rounds won by P1
    private final int           player2Wins;    // rounds won by P2
    private final int           totalRounds;    // rounds played
    private final int           survivalScore;  // 0 unless SURVIVAL mode
    private final String        battleground;   // map name

    // ── Private constructor – force use of the Builder ─────────────────────
    private GameRecord(Builder b) {
        this.timestamp     = b.timestamp;
        this.gameMode      = b.gameMode;
        this.player1Name   = b.player1Name;
        this.player2Name   = b.player2Name;
        this.winnerName    = b.winnerName;
        this.player1Wins   = b.player1Wins;
        this.player2Wins   = b.player2Wins;
        this.totalRounds   = b.totalRounds;
        this.survivalScore = b.survivalScore;
        this.battleground  = b.battleground;
    }

    // ── Getters ────────────────────────────────────────────────────────────
    public String   getTimestamp()     { return timestamp;     }
    public GameMode getGameMode()      { return gameMode;      }
    public String   getPlayer1Name()   { return player1Name;   }
    public String   getPlayer2Name()   { return player2Name;   }
    public String   getWinnerName()    { return winnerName;    }
    public int      getPlayer1Wins()   { return player1Wins;   }
    public int      getPlayer2Wins()   { return player2Wins;   }
    public int      getTotalRounds()   { return totalRounds;   }
    public int      getSurvivalScore() { return survivalScore; }
    public String   getBattleground()  { return battleground;  }

    /**
     * Serialises this record to a single pipe-delimited line so it can be
     * appended directly to the history file without any extra library.
     *
     * Format:
     *   timestamp|gameMode|p1Name|p2Name|winnerName|p1Wins|p2Wins|totalRounds|survivalScore|battleground
     */
    public String toFileLine() {
        return String.join("|",
                timestamp,
                gameMode.name(),
                player1Name,
                player2Name,
                winnerName,
                String.valueOf(player1Wins),
                String.valueOf(player2Wins),
                String.valueOf(totalRounds),
                String.valueOf(survivalScore),
                battleground
        );
    }

    /**
     * Deserialises one pipe-delimited line back into a GameRecord.
     * Returns null if the line is malformed (lets the loader skip bad lines).
     */
    public static GameRecord fromFileLine(String line) {
        if (line == null || line.isBlank()) return null;
        String[] parts = line.split("\\|", -1);
        if (parts.length < 10) return null;

        try {
            return new Builder()
                    .timestamp(parts[0])
                    .gameMode(GameMode.valueOf(parts[1]))
                    .player1Name(parts[2])
                    .player2Name(parts[3])
                    .winnerName(parts[4])
                    .player1Wins(Integer.parseInt(parts[5]))
                    .player2Wins(Integer.parseInt(parts[6]))
                    .totalRounds(Integer.parseInt(parts[7]))
                    .survivalScore(Integer.parseInt(parts[8]))
                    .battleground(parts[9])
                    .build();
        } catch (IllegalArgumentException e) {
            return null; // unknown GameMode or bad integer – skip
        }
    }

    /** Convenience: creates a timestamp string for right now. */
    public static String nowTimestamp() {
        return LocalDateTime.now().format(FORMATTER);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | %s vs %s | Winner: %s | Rounds: %d-%d/%d | Score: %d | Map: %s",
                timestamp, gameMode, player1Name, player2Name,
                winnerName, player1Wins, player2Wins, totalRounds,
                survivalScore, battleground);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Builder – keeps construction readable and avoids a huge constructor
    // ══════════════════════════════════════════════════════════════════════
    public static final class Builder {
        private String   timestamp     = GameRecord.nowTimestamp();
        private GameMode gameMode      = GameMode.PVP;
        private String   player1Name   = "Unknown";
        private String   player2Name   = "Unknown";
        private String   winnerName    = "Unknown";
        private int      player1Wins   = 0;
        private int      player2Wins   = 0;
        private int      totalRounds   = 0;
        private int      survivalScore = 0;
        private String   battleground  = "Unknown";

        public Builder timestamp(String v)     { this.timestamp     = v; return this; }
        public Builder gameMode(GameMode v)    { this.gameMode      = v; return this; }
        public Builder player1Name(String v)   { this.player1Name   = v; return this; }
        public Builder player2Name(String v)   { this.player2Name   = v; return this; }
        public Builder winnerName(String v)    { this.winnerName    = v; return this; }
        public Builder player1Wins(int v)      { this.player1Wins   = v; return this; }
        public Builder player2Wins(int v)      { this.player2Wins   = v; return this; }
        public Builder totalRounds(int v)      { this.totalRounds   = v; return this; }
        public Builder survivalScore(int v)    { this.survivalScore = v; return this; }
        public Builder battleground(String v)  { this.battleground  = v; return this; }

        public GameRecord build() {
            return new GameRecord(this);
        }
    }
}