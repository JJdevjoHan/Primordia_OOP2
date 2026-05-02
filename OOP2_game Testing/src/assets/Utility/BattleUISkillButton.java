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
    private static final String TYPE_SPRITE_PATH = "/assets/BattleUI/BattleUI_Type.png";
    private static final BufferedImage TYPE_SPRITE_SHEET = loadTypeSpriteSheet();

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

            // Draw icon (if any) and text manually in white
            String text = getText();
            String skillType = null;
            Object prop = getClientProperty("skillType");
            if (prop instanceof String) skillType = (String) prop;

            int iconWidth = 0;
            int iconHeight = 0;
            BufferedImage iconImage = null;
            if (skillType != null && TYPE_SPRITE_SHEET != null) {
                int typeFrame = mapTypeToFrame(skillType);
                int availableFrames = TYPE_SPRITE_SHEET.getWidth() / 48;
                if (typeFrame >= 0 && typeFrame < availableFrames) {
                    iconImage = TYPE_SPRITE_SHEET.getSubimage(typeFrame * 48, 0, 48, TYPE_SPRITE_SHEET.getHeight());
                    iconHeight = Math.max(16, Math.min(48, getHeight() - 12));
                    // scale icon down a bit for visual balance
                    iconHeight = (int) (iconHeight * 0.8);
                    iconWidth = (int) (iconImage.getWidth() * (iconHeight / (double) iconImage.getHeight()));
                }
            }

            // read mpCost from client property (set by panels)
            int mpCost = -1;
            Object mpProp = getClientProperty("mpCost");
            if (mpProp instanceof Integer) mpCost = (Integer) mpProp;
            else if (mpProp instanceof String) {
                try { mpCost = Integer.parseInt((String) mpProp); } catch (NumberFormatException ignored) {}
            }

            g2.setColor(Color.WHITE);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int textW = text != null ? fm.stringWidth(text) : 0;
            int textX = (getWidth() - textW) / 2;
            int textY = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();

            // Draw icon to the left of the centered text without shifting the text
            if (iconImage != null) {
                int spacing = 8;
                int iconX = textX - spacing - iconWidth;
                int iconY = (getHeight() - iconHeight) / 2;
                g2.drawImage(iconImage, iconX, iconY, iconWidth, iconHeight, null);
            }

            if (text != null && !text.isEmpty()) {
                g2.drawString(text, textX, textY);
            }

            // Draw mana icon (use BattleUI_Type frame 3) on the right side and render MP cost centered inside
            int iconRightSize = Math.min((int) (getHeight() * 0.9), 44);
            int rightSpacing = 4;
            int rightX = textX + textW + rightSpacing;
            int rightY = (getHeight() - iconRightSize) / 2;
            if (rightX + iconRightSize > getWidth() - 4) rightX = getWidth() - iconRightSize - 4;

            BufferedImage manaIcon = null;
            if (TYPE_SPRITE_SHEET != null) {
                int frames = TYPE_SPRITE_SHEET.getWidth() / 48;
                int manaFrameIndex = 2; // frame 3 (0-based index)
                if (manaFrameIndex >= 0 && manaFrameIndex < frames) {
                    manaIcon = TYPE_SPRITE_SHEET.getSubimage(manaFrameIndex * 48, 0, 48, TYPE_SPRITE_SHEET.getHeight());
                }
            }

            if (manaIcon != null) {
                int manaH = iconRightSize;
                int manaW = (int) (manaIcon.getWidth() * (manaH / (double) manaIcon.getHeight()));
                int drawX = rightX;
                int drawY = rightY;
                g2.drawImage(manaIcon, drawX, drawY, manaW, manaH, null);

                if (mpCost >= 0) {
                    float mpFontSize = Math.max(10f, iconRightSize * 0.6f);
                    Font mpFont = FontManager.getFont(mpFontSize).deriveFont(Font.BOLD);
                    g2.setFont(mpFont);
                    FontMetrics mfm = g2.getFontMetrics();
                    String mpStr = String.valueOf(mpCost);
                    int mpW = mfm.stringWidth(mpStr);
                    int centerX = drawX + manaW / 2;
                    int centerY = drawY + manaH / 2;
                    int mpX = centerX - (mpW / 2);
                    int mpY = centerY + (mfm.getAscent() - mfm.getDescent()) / 2 + 3; // 3px top margin
                    g2.setColor(new Color(26, 58, 110));
                    g2.drawString(mpStr, mpX, mpY);
                    g2.setFont(getFont());
                }
            } else {
                // fallback: draw simple droplet shape if sprite not available
                int dropletSize = iconRightSize;
                int dropletX = rightX;
                int dropletY = rightY;
                Color dropletColor = new Color(64, 164, 223);
                g2.setColor(dropletColor);
                int ovalH = (int) (dropletSize * 0.7);
                g2.fillOval(dropletX, dropletY, dropletSize, ovalH);
                int triTop = dropletY + (int) (dropletSize * 0.55);
                int tipX = dropletX + dropletSize / 2;
                int tipY = dropletY + dropletSize;
                int[] xs = {dropletX, dropletX + dropletSize, tipX};
                int[] ys = {triTop, triTop, tipY};
                g2.fillPolygon(xs, ys, 3);

                if (mpCost >= 0) {
                    float mpFontSize = Math.max(10f, dropletSize * 0.6f);
                    Font mpFont = FontManager.getFont(mpFontSize).deriveFont(Font.BOLD);
                    g2.setFont(mpFont);
                    FontMetrics mfm = g2.getFontMetrics();
                    String mpStr = String.valueOf(mpCost);
                    int mpW = mfm.stringWidth(mpStr);
                    int centerX = dropletX + dropletSize / 2;
                    int centerY = dropletY + dropletSize / 2;
                    int mpX = centerX - (mpW / 2);
                    int mpY = centerY + (mfm.getAscent() - mfm.getDescent()) / 2 + 3; // 3px top margin
                    g2.setColor(new Color(26, 58, 110));
                    g2.drawString(mpStr, mpX, mpY);
                    g2.setFont(getFont());
                }
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

    private static int mapTypeToFrame(String type) {
        if (type == null) return -1;
        type = type.toLowerCase().trim();
        return switch (type) {
            case "debuff" -> 0; // frame 1
            case "attack", "damage" -> 1; // frame 2
            case "defense" -> 3; // frame 4
            case "healing" -> 4; // frame 5
            default -> -1;
        };
    }

    private static BufferedImage loadTypeSpriteSheet() {
        Path localPath = Paths.get("OOP2_game Testing", "src", "assets", "BattleUI", "BattleUI_Type.png");
        try {
            if (Files.exists(localPath)) {
                return ImageIO.read(localPath.toFile());
            }
        } catch (IOException ignored) {
        }

        try {
            URL resourceUrl = BattleUISkillButton.class.getResource(TYPE_SPRITE_PATH);
            if (resourceUrl != null) return ImageIO.read(resourceUrl);
        } catch (IOException ignored) {}

        try (InputStream is = BattleUISkillButton.class.getResourceAsStream(TYPE_SPRITE_PATH)) {
            if (is != null) return ImageIO.read(is);
        } catch (IOException ignored) {}

        return null;
    }
}