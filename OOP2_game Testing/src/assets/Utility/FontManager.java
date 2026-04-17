package assets.Utility;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FontManager {
    private static Font baseFont;

    static {
        baseFont = loadFontFromClasspath("/fonts/Silver.ttf");

        if (baseFont == null) {
            baseFont = loadFontFromClasspath("/fonts/Sta.Toasty.ttf");
        }

        if (baseFont == null) {
            baseFont = loadFontFromFileSystem(
                    "C:/Users/User/Desktop/Primordia/Primordia_OOP2/Resources/fonts/Silver.ttf");
        }

        if (baseFont == null) {
            baseFont = loadFontFromFileSystem(
                    "C:/Users/User/Desktop/Primordia/Primordia_OOP2/Resources/fonts/Sta.Toasty.ttf");
        }

        if (baseFont == null) {
            baseFont = new Font("SansSerif", Font.PLAIN, 12);
        }
    }

    public static Font getFont(float size) {
        return baseFont.deriveFont(size);
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
