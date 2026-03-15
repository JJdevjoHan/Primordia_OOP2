package assets.Utility;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

public class FontManager {
    private static Font baseFont;

    static {
        try (InputStream is = FontManager.class.getResourceAsStream("/fonts/Silver.ttf")) {
            if (is == null) {
                throw new IOException("Font resource not found!");
            }
            baseFont = Font.createFont(Font.TRUETYPE_FONT, is);

            // Register the base font once
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(baseFont);

        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
            baseFont = new Font("SansSerif", Font.PLAIN, 12); // fallback
        }
    }

    public static Font getFont(float size) {
        return baseFont.deriveFont(size);
    }
}
