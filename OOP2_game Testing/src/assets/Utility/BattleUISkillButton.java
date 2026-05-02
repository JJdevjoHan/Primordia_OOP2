package assets.Utility;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BattleUISkillButton extends JButton {
    private static final String SPRITE_PATH = "/assets/BattleUI/BattleUI_Button.png";
    private static final int FRAME_COUNT = 3;
    private static final BufferedImage SPRITE_SHEET = loadSpriteSheet();

    public BattleUISkillButton(String text) {
        super(text);
        setFont(FontManager.getFont(26f).deriveFont(Font.BOLD));
        setForeground(Color.WHITE);
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
        setHorizontalTextPosition(SwingConstants.CENTER);
        setVerticalTextPosition(SwingConstants.CENTER);
        setFocusable(false);
        setFocusPainted(false);
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        setContentAreaFilled(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(0, 0, 0, 0));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (SPRITE_SHEET != null) {
                int frameIndex = getFrameIndex();
                int frameWidth = SPRITE_SHEET.getWidth() / FRAME_COUNT;
                if (frameWidth > 0) {
                    BufferedImage frame = SPRITE_SHEET.getSubimage(frameIndex * frameWidth, 0, frameWidth, SPRITE_SHEET.getHeight());
                    if (!isEnabled()) {
                        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.65f));
                    }
                    g2.drawImage(frame, 0, 0, getWidth(), getHeight(), null);
                }
            } else {
                paintFallback(g2);
            }

            // Draw text manually in white
            String text = getText();
            if (text != null && !text.isEmpty()) {
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int textX = (getWidth() - fm.stringWidth(text)) / 2;
                int textY = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(text, textX, textY);
            }
        } finally {
            g2.dispose();
        }
    }

    private int getFrameIndex() {
        ButtonModel model = getModel();
        if (model.isPressed() && model.isArmed()) {
            return 2;
        }
        if (model.isRollover()) {
            return 1;
        }
        return 0;
    }

    private void paintFallback(Graphics2D g2) {
        Color base = new Color(90, 72, 50);
        Color hover = new Color(110, 88, 60);
        Color pressed = new Color(70, 54, 38);
        Color fill = base;

        ButtonModel model = getModel();
        if (model.isPressed() && model.isArmed()) {
            fill = pressed;
        } else if (model.isRollover()) {
            fill = hover;
        }

        if (!isEnabled()) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.65f));
        }

        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
        g2.setColor(new Color(25, 18, 12));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 16, 16);
    }

    private static BufferedImage loadSpriteSheet() {
        Path localPath = Paths.get("OOP2_game Testing", "src", "assets", "BattleUI", "BattleUI_Button.png");
        try {
            if (Files.exists(localPath)) {
                return ImageIO.read(localPath.toFile());
            }
        } catch (IOException ignored) {
        }

        try {
            URL resourceUrl = BattleUISkillButton.class.getResource(SPRITE_PATH);
            if (resourceUrl != null) {
                return ImageIO.read(resourceUrl);
            }
        } catch (IOException ignored) {
        }

        try (InputStream inputStream = BattleUISkillButton.class.getResourceAsStream(SPRITE_PATH)) {
            if (inputStream != null) {
                return ImageIO.read(inputStream);
            }
        } catch (IOException ignored) {
        }

        return null;
    }
}