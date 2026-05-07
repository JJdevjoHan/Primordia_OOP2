package engine;

import javax.swing.ImageIcon;
import javax.swing.Timer;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.IntSupplier;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class MapGenerator {
    private MapGenerator() {}

    // Edit these layer numbers to change which numbered PNGs stay static per battleground.
    // Layer numbers are 1-based and match the numbered files after render-order reversal.
    private static final Map<String, int[]> STATIC_BACKGROUND_LAYERS_BY_BACKGROUND = Map.ofEntries(
            Map.entry("battleground1", new int[] { 1, 2, 3 }),
            Map.entry("battleground2", new int[] { 1, 4, 5 }),
            Map.entry("battleground3", new int[] { 1, 4, 5, 6 })
    );

    // Track how many times each battleground has been loaded for weighted fairness
    private static final Map<String, Integer> BATTLEGROUND_LOAD_COUNT = new HashMap<>();
    static {
        BATTLEGROUND_LOAD_COUNT.put("battleground1", 0);
        BATTLEGROUND_LOAD_COUNT.put("battleground2", 0);
        BATTLEGROUND_LOAD_COUNT.put("battleground3", 0);
    }

    public static final class BackgroundState {
        private final List<Image> layers;
        private final double[] offsets;
        private final double[] speeds;
        private final Timer timer;

        private BackgroundState(List<Image> layers, double[] offsets, double[] speeds, Timer timer) {
            this.layers = layers;
            this.offsets = offsets;
            this.speeds = speeds;
            this.timer = timer;
        }

        public static BackgroundState empty() {
            return new BackgroundState(new ArrayList<>(), new double[0], new double[0], null);
        }

        public boolean isEmpty() {
            return layers.isEmpty();
        }

        public void stop() {
            if (timer != null) {
                timer.stop();
            }
        }

        public void draw(Graphics2D g2, Component component) {
            if (layers.isEmpty()) {
                return;
            }

            int width = component.getWidth();
            int height = component.getHeight();
            for (int i = 0; i < layers.size(); i++) {
                Image layer = layers.get(i);
                if (layer == null) {
                    continue;
                }
                int offset = (int) Math.round(i < offsets.length ? offsets[i] : 0.0);
                g2.drawImage(layer, -offset, 0, width, height, component);
                g2.drawImage(layer, -offset + width, 0, width, height, component);
            }
        }
    }

    public static BackgroundState loadBackground(Document document, String tmxResourcePath, URL tmxUrl, IntSupplier widthSupplier, Runnable repaintAction, BiConsumer<Integer, Integer> sizeConsumer) {
        List<Image> backgroundLayers = new ArrayList<>();
        NodeList imageNodes = document.getElementsByTagName("imagelayer");
        if (imageNodes.getLength() == 0) {
            imageNodes = document.getElementsByTagName("image");
        }

        for (int i = 0; i < imageNodes.getLength(); i++) {
            Node node = imageNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element imageElement = (Element) node;
            Element img = imageElement.getTagName().equals("image")
                    ? imageElement
                    : (Element) imageElement.getElementsByTagName("image").item(0);
            if (img == null) {
                continue;
            }

            String source = img.getAttribute("source");
            if (source == null || source.isEmpty()) {
                continue;
            }

            int imageWidth = parseNumber(img.getAttribute("width"));
            int imageHeight = parseNumber(img.getAttribute("height"));
            if (sizeConsumer != null && imageWidth > 0 && imageHeight > 0) {
                sizeConsumer.accept(imageWidth, imageHeight);
            }

            List<Image> layers = loadLayerImages(source, tmxResourcePath, tmxUrl);
            if (!layers.isEmpty()) {
                backgroundLayers.addAll(layers);
                continue;
            }

            Image loaded = loadSingleImage(source, tmxResourcePath, tmxUrl);
            if (loaded != null) {
                backgroundLayers.add(loaded);
            }
        }

        if (backgroundLayers.isEmpty()) {
            return BackgroundState.empty();
        }

        int count = backgroundLayers.size();
        double[] offsets = new double[count];
        double[] speeds = new double[count];
        for (int i = 0; i < count; i++) {
            double t = count > 1 ? i / (double) (count - 1) : 0.0;
            speeds[i] = 0.5 + t * 2.0;
            offsets[i] = 0.0;
        }

        Timer timer = new Timer(40, e -> {
            double dt = 40.0 / 1000.0;
            int width = Math.max(1, widthSupplier != null ? widthSupplier.getAsInt() : 0);
            for (int i = 0; i < offsets.length; i++) {
                offsets[i] = (offsets[i] + speeds[i] * dt) % width;
            }
            if (repaintAction != null) {
                repaintAction.run();
            }
        });
        timer.start();

        return new BackgroundState(backgroundLayers, offsets, speeds, timer);
    }

    public static boolean shouldAnimateBackgroundLayer(String source, int layerIndex, int layerCount) {
        if (layerCount <= 0) {
            return true;
        }

        String backgroundKey = getBackgroundKey(source);
        int[] staticLayerNumbers = STATIC_BACKGROUND_LAYERS_BY_BACKGROUND.get(backgroundKey);
        if (staticLayerNumbers == null || staticLayerNumbers.length == 0) {
            return true;
        }

        int layerNumber = layerCount - layerIndex;
        for (int staticLayerNumber : staticLayerNumbers) {
            if (staticLayerNumber == layerNumber) {
                return false;
            }
        }
        return true;
    }

    private static Image loadSingleImage(String source, String tmxResourcePath, URL tmxUrl) {
        URL resourceUrl = MapGenerator.class.getResource(resolveResourcePath(tmxResourcePath, source));
        if (resourceUrl != null) {
            return new ImageIcon(resourceUrl).getImage();
        }

        try {
            Path tmxFilePath = Paths.get(tmxUrl.toURI());
            Path tmxDir = tmxFilePath.getParent();
            if (tmxDir != null) {
                Path imagePath = tmxDir.resolve(source.replace('\\', File.separatorChar)).normalize();
                if (Files.exists(imagePath)) {
                    return new ImageIcon(imagePath.toString()).getImage();
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    private static List<Image> loadLayerImages(String source, String tmxResourcePath, URL tmxUrl) {
        List<Image> layers = new ArrayList<>();
        String baseName = source != null ? source.replaceAll("\\\\", "/") : "";
        baseName = baseName.contains(".") ? baseName.substring(0, baseName.lastIndexOf('.')) : baseName;

        try {
            URL tmxx = MapGenerator.class.getResource(tmxResourcePath);
            if (tmxx == null) {
                return layers;
            }

            Path tmxFilePath = Paths.get(tmxx.toURI());
            Path mapsDir = tmxFilePath.getParent();
            if (mapsDir == null) {
                return layers;
            }

            Path bgFolder = null;
            Path backgroundsRoot = mapsDir.resolve("backgrounds");
            
            // First, try to find an exact folder match for baseName (e.g., "Battleground1")
            if (Files.exists(backgroundsRoot) && Files.isDirectory(backgroundsRoot)) {
                Path exactMatch = backgroundsRoot.resolve(baseName);
                if (Files.exists(exactMatch) && Files.isDirectory(exactMatch)) {
                    bgFolder = exactMatch;
                }
            }
            
            // If no exact match, try random selection (fallback)
            if (bgFolder == null && Files.exists(backgroundsRoot) && Files.isDirectory(backgroundsRoot)) {
                List<Path> candidates = new ArrayList<>();
                try (java.util.stream.Stream<Path> stream = Files.list(backgroundsRoot)) {
                    stream.filter(Files::isDirectory).forEach(candidates::add);
                }
                if (!candidates.isEmpty()) {
                    Path chosen = candidates.get((int) (Math.random() * candidates.size()));
                    Path chosenSpecific = chosen.resolve(baseName);
                    bgFolder = Files.exists(chosenSpecific) && Files.isDirectory(chosenSpecific) ? chosenSpecific : chosen;
                }
            }

            if (bgFolder == null) {
                bgFolder = mapsDir.resolve("background").resolve(baseName);
            }

            if (!Files.exists(bgFolder) || !Files.isDirectory(bgFolder)) {
                Path alt = mapsDir.resolve("backgrounds").resolve(baseName);
                if (Files.exists(alt) && Files.isDirectory(alt)) {
                    bgFolder = alt;
                } else {
                    return layers;
                }
            }

            List<Path> imgs = new ArrayList<>();
            try (java.util.stream.Stream<Path> stream = Files.list(bgFolder)) {
                stream.filter(p -> Files.isRegularFile(p) && isImageFile(p)).forEach(imgs::add);
            }

            if (imgs.isEmpty()) {
                try (java.util.stream.Stream<Path> stream = Files.list(bgFolder)) {
                    stream.filter(Files::isDirectory).forEach(dir -> {
                        try (java.util.stream.Stream<Path> nested = Files.list(dir)) {
                            nested.filter(p -> Files.isRegularFile(p) && isImageFile(p)).forEach(imgs::add);
                        } catch (Exception ignored) {}
                    });
                }
            }

            if (imgs.isEmpty()) {
                return layers;
            }

            imgs.sort((a, b) -> {
                String an = a.getFileName().toString();
                String bn = b.getFileName().toString();
                Integer ai = extractTrailingNumber(an);
                Integer bi = extractTrailingNumber(bn);
                if (ai != null && bi != null) {
                    return ai.compareTo(bi);
                }
                return an.compareTo(bn);
            });
            Collections.reverse(imgs);

            for (Path imgPath : imgs) {
                try {
                    layers.add(new ImageIcon(imgPath.toString()).getImage());
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        return layers;
    }

    private static boolean isImageFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
    }

    private static String getBackgroundKey(String source) {
        if (source == null || source.isBlank()) {
            return "";
        }

        String normalized = source.replace('\\', '/').toLowerCase(Locale.ROOT);
        
        // Check the full path (not just filename) for battleground directories
        if (normalized.contains("battleground1")) {
            return "battleground1";
        }
        if (normalized.contains("battleground2")) {
            return "battleground2";
        }
        if (normalized.contains("battleground3")) {
            return "battleground3";
        }
        
        // Fallback: extract filename without extension
        int slashIndex = normalized.lastIndexOf('/');
        String fileName = slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex >= 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    /**
     * Selects a battleground from candidates using weighted fairness.
     * Maps that haven't been loaded recently get higher selection chances.
     * Guarantees fair distribution over time.
     */
    public static Path selectWeightedBattleground(List<Path> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            String bgName = candidates.get(0).getFileName().toString().toLowerCase();
            recordBattlegroundLoad(bgName);
            return candidates.get(0);
        }

        // Calculate max load count to determine bias
        int maxCount = BATTLEGROUND_LOAD_COUNT.values().stream().mapToInt(Integer::intValue).max().orElse(0);

        // Build weighted list: candidates with lower load counts get higher weights
        List<Path> weightedList = new ArrayList<>();
        for (Path candidate : candidates) {
            String bgName = candidate.getFileName().toString().toLowerCase();
            Integer currentCount = BATTLEGROUND_LOAD_COUNT.getOrDefault(bgName, 0);
            // Weight = (maxCount - currentCount) + 1, so least-loaded gets highest weight
            int weight = (maxCount - currentCount) + 1;
            for (int i = 0; i < weight; i++) {
                weightedList.add(candidate);
            }
        }

        // Pick randomly from weighted list
        Path selected = weightedList.get((int) (Math.random() * weightedList.size()));
        recordBattlegroundLoad(selected.getFileName().toString().toLowerCase());
        return selected;
    }

    /**
     * Records that a battleground was loaded and increments its counter.
     */
    private static void recordBattlegroundLoad(String battlegroundName) {
        String normalized = battlegroundName.toLowerCase();
        if (normalized.contains("battleground1")) {
            BATTLEGROUND_LOAD_COUNT.put("battleground1", BATTLEGROUND_LOAD_COUNT.getOrDefault("battleground1", 0) + 1);
        } else if (normalized.contains("battleground2")) {
            BATTLEGROUND_LOAD_COUNT.put("battleground2", BATTLEGROUND_LOAD_COUNT.getOrDefault("battleground2", 0) + 1);
        } else if (normalized.contains("battleground3")) {
            BATTLEGROUND_LOAD_COUNT.put("battleground3", BATTLEGROUND_LOAD_COUNT.getOrDefault("battleground3", 0) + 1);
        }
    }

    private static Integer extractTrailingNumber(String name) {
        String digits = "";
        for (int i = name.length() - 1; i >= 0; i--) {
            char c = name.charAt(i);
            if (Character.isDigit(c)) {
                digits = c + digits;
            } else {
                break;
            }
        }
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(digits);
        } catch (Exception e) {
            return null;
        }
    }

    private static String resolveResourcePath(String basePath, String relativePath) {
        String normalizedRelative = relativePath.replace('\\', '/');
        Path base = Paths.get(basePath).getParent();
        Path resolved = (base == null ? Paths.get(normalizedRelative) : base.resolve(normalizedRelative)).normalize();
        String result = resolved.toString().replace('\\', '/');
        return result.startsWith("/") ? result : "/" + result;
    }

    private static int parseNumber(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return (int) Math.round(Double.parseDouble(value));
    }
}