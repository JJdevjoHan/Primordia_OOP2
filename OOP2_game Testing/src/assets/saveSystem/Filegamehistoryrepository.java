package assets.saveSystem;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * File-based implementation of {@link GameHistoryRepository}.
 *
 * Stores one record per line in a plain-text file inside the user's home
 * directory so it survives across runs and works on Windows, macOS, and Linux.
 *
 * Save file location:  ~/.primordia/game_history.txt
 *
 * OOP Principles applied:
 *  - Encapsulation         : file path details are private; callers never see them
 *  - Single Responsibility : this class only handles file I/O, nothing else
 *  - Liskov Substitution   : can be swapped with any other GameHistoryRepository
 *  - Inheritance/Interface : implements GameHistoryRepository
 */
public class Filegamehistoryrepository implements GameHistoryRepository {

    // ── Save file configuration ────────────────────────────────────────────
    private static final String GAME_FOLDER  = ".primordia";
    private static final String HISTORY_FILE = "game_history.txt";
    private static final String COMMENT_PREFIX = "#"; // lines starting with # are skipped

    private final Path saveFilePath;

    // ── Constructor ────────────────────────────────────────────────────────

    /**
     * Creates the repository and ensures the save directory exists.
     * Uses the default location: {@code <user.home>/.primordia/game_history.txt}
     */
    public Filegamehistoryrepository() {
        this(Paths.get(System.getProperty("user.home"), GAME_FOLDER, HISTORY_FILE));
    }

    /**
     * Creates the repository pointing at a custom path (useful for tests).
     *
     * @param saveFilePath full path to the history file
     */
    public Filegamehistoryrepository(Path saveFilePath) {
        this.saveFilePath = saveFilePath;
        initialiseFile();
    }

    // ── GameHistoryRepository implementation ──────────────────────────────

    @Override
    public void save(GameRecord record) throws SaveSystemException {
        if (record == null) throw new SaveSystemException("Cannot save a null record.");
        try {
            // CREATE parent directories if they don't exist yet
            Files.createDirectories(saveFilePath.getParent());

            // APPEND one line to the file (creates the file if it doesn't exist)
            try (BufferedWriter writer = Files.newBufferedWriter(
                    saveFilePath,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND)) {

                writer.write(record.toFileLine());
                writer.newLine();
            }

        } catch (IOException e) {
            throw new SaveSystemException("Failed to save game record: " + e.getMessage(), e);
        }
    }

    @Override
    public List<GameRecord> loadAll() throws SaveSystemException {
        if (!Files.exists(saveFilePath)) return Collections.emptyList();

        try (BufferedReader reader = Files.newBufferedReader(saveFilePath, StandardCharsets.UTF_8)) {

            List<GameRecord> records = new ArrayList<>();
            String line;

            while ((line = reader.readLine()) != null) {
                // Skip blank lines and comment lines
                if (line.isBlank() || line.startsWith(COMMENT_PREFIX)) continue;

                GameRecord record = GameRecord.fromFileLine(line);
                if (record != null) {
                    records.add(record);
                }
                // Silently skip malformed lines so one bad line doesn't kill everything
            }

            return Collections.unmodifiableList(records);

        } catch (IOException e) {
            throw new SaveSystemException("Failed to load game history: " + e.getMessage(), e);
        }
    }

    @Override
    public List<GameRecord> loadRecent(int limit) throws SaveSystemException {
        if (limit <= 0) return Collections.emptyList();

        List<GameRecord> all = new ArrayList<>(loadAll());

        // Reverse to get newest first, then take up to `limit`
        Collections.reverse(all);
        return Collections.unmodifiableList(
                all.subList(0, Math.min(limit, all.size()))
        );
    }

    @Override
    public List<GameRecord> loadByMode(GameMode mode) throws SaveSystemException {
        if (mode == null) return Collections.emptyList();

        return loadAll().stream()
                .filter(r -> r.getGameMode() == mode)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public void clearAll() throws SaveSystemException {
        try {
            if (Files.exists(saveFilePath)) {
                // Overwrite with an empty file (keeps the file itself)
                Files.writeString(saveFilePath, "", StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new SaveSystemException("Failed to clear game history: " + e.getMessage(), e);
        }
    }

    // ── Package-visible helpers ────────────────────────────────────────────

    /** Returns the absolute path where records are stored. */
    public Path getSaveFilePath() {
        return saveFilePath;
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /**
     * Creates the save directory and a header comment in the history file on
     * first run so the file is human-readable if someone opens it.
     */
    private void initialiseFile() {
        try {
            if (saveFilePath.getParent() != null) {
                Files.createDirectories(saveFilePath.getParent());
            }

            if (!Files.exists(saveFilePath)) {
                try (BufferedWriter w = Files.newBufferedWriter(
                        saveFilePath,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW)) {
                    w.write("# Primordia – Game History");
                    w.newLine();
                    w.write("# Format: timestamp|gameMode|p1Name|p2Name|winnerName|p1Wins|p2Wins|totalRounds|survivalScore|battleground");
                    w.newLine();
                }
            }
        } catch (IOException e) {
            // Non-fatal: log and continue. The save() call will report
            // the error properly if writing later also fails.
            System.err.println("[SaveSystem] Warning: could not initialise save file: " + e.getMessage());
        }
    }
}