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
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntSupplier;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class MapGenerator {
    private MapGenerator() {}

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
            if (Files.exists(backgroundsRoot) && Files.isDirectory(backgroundsRoot)) {
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