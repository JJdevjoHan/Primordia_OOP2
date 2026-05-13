package assets.Utility;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class SurvivalLeaderboardManager {
    private static final int MAX_ENTRIES = 10;
    private static final List<String> LEADERBOARD_FILE_CANDIDATES = List.of(
            "OOP2_game Testing/src/assets/data/survival_leaderboard.txt",
            "src/assets/data/survival_leaderboard.txt",
            "assets/data/survival_leaderboard.txt"
    );

    private SurvivalLeaderboardManager() {
    }

    public static final class Entry {
        private final String playerName;
        private final int score;
        private final long recordedAt;

        public Entry(String playerName, int score, long recordedAt) {
            this.playerName = playerName;
            this.score = score;
            this.recordedAt = recordedAt;
        }

        public String playerName() {
            return playerName;
        }

        public int score() {
            return score;
        }

        public long recordedAt() {
            return recordedAt;
        }
    }

    public static synchronized void recordEntry(String playerName, int score) {
        String trimmedName = normalizeName(playerName);
        List<Entry> entries = new ArrayList<>(loadEntries());
        entries.add(new Entry(trimmedName, score, System.currentTimeMillis()));
        entries.sort(Comparator.comparingInt(Entry::score).reversed().thenComparingLong(Entry::recordedAt));

        if (entries.size() > MAX_ENTRIES) {
            entries = new ArrayList<>(entries.subList(0, MAX_ENTRIES));
        }

        writeEntries(entries);
    }

    public static synchronized List<Entry> loadEntries() {
        Path path = resolveLeaderboardPath();
        if (path == null || !Files.exists(path)) {
            return Collections.emptyList();
        }

        List<Entry> entries = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                Entry entry = parseEntry(line);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        } catch (IOException ignored) {
            return Collections.emptyList();
        }

        entries.sort(Comparator.comparingInt(Entry::score).reversed().thenComparingLong(Entry::recordedAt));
        if (entries.size() > MAX_ENTRIES) {
            return new ArrayList<>(entries.subList(0, MAX_ENTRIES));
        }
        return entries;
    }

    public static synchronized String formatEntries() {
        List<Entry> entries = loadEntries();
        if (entries.isEmpty()) {
            return "No survival scores yet.";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            if (i > 0) {
                builder.append(System.lineSeparator());
            }
            builder.append(i + 1)
                    .append(". ")
                    .append(entry.playerName())
                    .append(" - ")
                    .append(entry.score());
        }
        return builder.toString();
    }

    private static Entry parseEntry(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }

        String[] parts = line.split("\\|", 3);
        if (parts.length != 3) {
            return null;
        }

        try {
            int score = Integer.parseInt(parts[0].trim());
            long recordedAt = Long.parseLong(parts[1].trim());
            String playerName = decodeName(parts[2].trim());
            return new Entry(playerName, score, recordedAt);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void writeEntries(List<Entry> entries) {
        Path path = resolveLeaderboardPath();
        if (path == null) {
            return;
        }

        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            StringBuilder builder = new StringBuilder();
            for (Entry entry : entries) {
                builder.append(entry.score())
                        .append('|')
                        .append(entry.recordedAt())
                        .append('|')
                        .append(encodeName(entry.playerName()))
                        .append(System.lineSeparator());
            }

            Files.writeString(path, builder.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static Path resolveLeaderboardPath() {
        for (String candidate : LEADERBOARD_FILE_CANDIDATES) {
            Path path = Paths.get(candidate);
            Path parent = path.getParent();
            if ((parent != null && Files.exists(parent)) || Files.exists(path)) {
                return path;
            }
        }
        return Paths.get(LEADERBOARD_FILE_CANDIDATES.get(0));
    }

    private static String normalizeName(String playerName) {
        if (playerName == null) {
            return "Anonymous";
        }

        String trimmed = playerName.trim();
        return trimmed.isEmpty() ? "Anonymous" : trimmed;
    }

    private static String encodeName(String playerName) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(playerName.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeName(String encodedName) {
        if (encodedName == null || encodedName.isBlank()) {
            return "Anonymous";
        }

        try {
            return new String(Base64.getUrlDecoder().decode(encodedName), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return "Anonymous";
        }
    }
}