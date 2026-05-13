package assets.Utility;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FontManager {
    private static final Font BASE_FONT;

    static {
        Font loadedFont = loadPreferredFont();
        BASE_FONT = loadedFont != null ? loadedFont : new Font("SansSerif", Font.PLAIN, 12);
    }

    public static Font getFont(float size) {
        return BASE_FONT.deriveFont(size);
    }

    private static Font loadPreferredFont() {
        String[] classpathCandidates = {
                "/fonts/Silver.ttf",
                "/fonts/Sta.Toasty.ttf"
        };

        String[] fileSystemCandidates = {
                "Resources/fonts/Silver.ttf",
                "Resources/fonts/Sta.Toasty.ttf",
                "../Resources/fonts/Silver.ttf",
                "../Resources/fonts/Sta.Toasty.ttf"
        };

        for (String resourcePath : classpathCandidates) {
            Font font = loadFontFromClasspath(resourcePath);
            if (font != null) {
                return font;
            }
        }

        for (String filePath : fileSystemCandidates) {
            Font font = loadFontFromFileSystem(filePath);
            if (font != null) {
                return font;
            }
        }

        return null;
    }

    private static Font loadFontFromClasspath(String resourcePath) {
        try (InputStream is = FontManager.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                return null;
            }

            Font font = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(font);
            return font;
        } catch (FontFormatException | IOException e) {
            return null;
        }
    }

    private static Font loadFontFromFileSystem(String filePath) {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            return null;
        }

        try (InputStream is = Files.newInputStream(path)) {
            Font font = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(font);
            return font;
        } catch (FontFormatException | IOException e) {
            return null;
        }
    }
}
