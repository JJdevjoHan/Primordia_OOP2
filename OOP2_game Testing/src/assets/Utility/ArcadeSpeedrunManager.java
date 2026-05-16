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

public final class ArcadeSpeedrunManager {
    private static final int MAX_ENTRIES = 10;
    private static final List<String> SPEEDRUN_FILE_CANDIDATES = List.of(
            "OOP2_game Testing/src/assets/data/arcade_speedrun.txt",
            "src/assets/data/arcade_speedrun.txt",
            "assets/data/arcade_speedrun.txt"
    );

    private ArcadeSpeedrunManager() {
    }

    public static final class Entry {
        private final String playerName;
        private final long timeMs;
        private final long recordedAt;

        public Entry(String playerName, long timeMs, long recordedAt) {
            this.playerName = playerName;
            this.timeMs = timeMs;
            this.recordedAt = recordedAt;
        }

        public String playerName() { return playerName; }
        public long timeMs()       { return timeMs; }
        public long recordedAt()   { return recordedAt; }
    }

    /** Record a completed arcade run time. Entries sorted fastest-first. */
    public static synchronized void recordEntry(String playerName, long timeMs) {
        String trimmed = normalizeName(playerName);
        List<Entry> entries = new ArrayList<>(loadEntries());
        entries.add(new Entry(trimmed, timeMs, System.currentTimeMillis()));
        entries.sort(Comparator.comparingLong(Entry::timeMs).thenComparingLong(Entry::recordedAt));

        if (entries.size() > MAX_ENTRIES) {
            entries = new ArrayList<>(entries.subList(0, MAX_ENTRIES));
        }
        writeEntries(entries);
    }

    public static synchronized List<Entry> loadEntries() {
        Path path = resolveSpeedrunPath();
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

        entries.sort(Comparator.comparingLong(Entry::timeMs).thenComparingLong(Entry::recordedAt));
        if (entries.size() > MAX_ENTRIES) {
            return new ArrayList<>(entries.subList(0, MAX_ENTRIES));
        }
        return entries;
    }

    /**
     * Format a millisecond duration adaptively:
     *   sub-minute  → "ss"s         (e.g. "42s")
     *   sub-hour    → "m:ss"        (e.g. "3:42")
     *   1h+         → "h:mm:ss"     (e.g. "1:03:42")
     */
    public static String formatTime(long timeMs) {
        long totalSeconds = timeMs / 1000;
        long seconds      = totalSeconds % 60;
        long totalMinutes = totalSeconds / 60;
        long minutes      = totalMinutes % 60;
        long hours        = totalMinutes / 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d:%02d", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private static Entry parseEntry(String line) {
        if (line == null || line.isBlank()) return null;
        String[] parts = line.split("\\|", 3);
        if (parts.length != 3) return null;
        try {
            long timeMs     = Long.parseLong(parts[0].trim());
            long recordedAt = Long.parseLong(parts[1].trim());
            String name     = decodeName(parts[2].trim());
            return new Entry(name, timeMs, recordedAt);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void writeEntries(List<Entry> entries) {
        Path path = resolveSpeedrunPath();
        if (path == null) return;
        try {
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);
            StringBuilder sb = new StringBuilder();
            for (Entry e : entries) {
                sb.append(e.timeMs())
                  .append('|')
                  .append(e.recordedAt())
                  .append('|')
                  .append(encodeName(e.playerName()))
                  .append(System.lineSeparator());
            }
            Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static Path resolveSpeedrunPath() {
        for (String candidate : SPEEDRUN_FILE_CANDIDATES) {
            Path path = Paths.get(candidate);
            Path parent = path.getParent();
            if ((parent != null && Files.exists(parent)) || Files.exists(path)) {
                return path;
            }
        }
        return Paths.get(SPEEDRUN_FILE_CANDIDATES.get(0));
    }

    private static String normalizeName(String name) {
        if (name == null) return "Anonymous";
        String t = name.trim();
        return t.isEmpty() ? "Anonymous" : t;
    }

    private static String encodeName(String name) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(name.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeName(String encoded) {
        if (encoded == null || encoded.isBlank()) return "Anonymous";
        try {
            return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return "Anonymous";
        }
    }
}
