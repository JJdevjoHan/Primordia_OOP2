package engine.ui;

import assets.Utility.FontManager;
import engine.core.GameWindow;
import engine.audio.SoundManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

public class SettingsMenuPanel extends JPanel {
    private static final int SCREEN_WIDTH = 1536;
    private static final int SCREEN_HEIGHT = 896;

    private final GameWindow window;
    private final String previousPanelCard;
    private final Component previousComponent;
    private final SoundManager soundManager = new SoundManager();
    
    private boolean hoverBackButton = false;
    private boolean hoverBgmToggle = false;
    private boolean hoverSfxToggle = false;
    private boolean draggingBgmSlider = false;
    private boolean draggingSfxSlider = false;
    private boolean hoverBgmSlider = false;
    private boolean hoverSfxSlider = false;

    // UI positioning
    private Rectangle backButton;
    private Rectangle bgmToggle;
    private Rectangle sfxToggle;
    private Rectangle bgmSliderTrack;
    private Rectangle bgmSliderThumb;
    private Rectangle sfxSliderTrack;
    private Rectangle sfxSliderThumb;

    private final Font titleFont = FontManager.getFont(42f).deriveFont(Font.BOLD);
    private final Font subtitleFont = FontManager.getFont(26f).deriveFont(Font.BOLD);
    private final Font labelFont = FontManager.getFont(24f).deriveFont(Font.BOLD);

    public SettingsMenuPanel(GameWindow window, String previousPanelCard, Component previousComponent) {
        this.window = window;
        this.previousPanelCard = previousPanelCard;
        this.previousComponent = previousComponent;
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e.getX(), e.getY());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (bgmSliderThumb != null && bgmSliderThumb.contains(e.getX(), e.getY())) {
                    draggingBgmSlider = true;
                }
                if (sfxSliderThumb != null && sfxSliderThumb.contains(e.getX(), e.getY())) {
                    draggingSfxSlider = true;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                draggingBgmSlider = false;
                draggingSfxSlider = false;
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                handleMouseMove(e.getX(), e.getY());
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleMouseMove(e.getX(), e.getY());
            }
        });

        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                    closeSettings();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Draw blurred and darkened background
        paintBackground(g2);

        // Draw settings menu panel
        paintSettingsMenu(g2);

        g2.dispose();
    }

    private void paintBackground(Graphics2D g2) {
        try {
            if (previousComponent != null) {
                int w = getWidth();
                int h = getHeight();
                BufferedImage buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D ig = buf.createGraphics();
                ig.setColor(new Color(0,0,0,0));
                ig.fillRect(0,0,w,h);
                // paint previous component into image
                previousComponent.paint(ig);
                ig.dispose();

                // small 5x5 box blur
                int size = 5;
                float weight = 1.0f / (size * size);
                float[] data = new float[size * size];
                for (int i = 0; i < data.length; i++) data[i] = weight;
                ConvolveOp op = new ConvolveOp(new Kernel(size, size, data), ConvolveOp.EDGE_NO_OP, null);
                BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                op.filter(buf, dst);

                // draw blurred background with slight darken
                Composite old = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
                g2.drawImage(dst, 0, 0, null);
                g2.setComposite(old);

                // dark overlay to improve contrast
                g2.setColor(new Color(0, 0, 0, 120));
                g2.fillRect(0, 0, w, h);
                return;
            }
        } catch (Exception ignored) {}

        // fallback dark overlay
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, getWidth(), getHeight());
    }

    private void paintSettingsMenu(Graphics2D g2) {
        String title = "Settings";
        String backText = "Back";
        String bgmLabel = "Background Music";
        String sfxLabel = "Sound Effects";

        FontMetrics titleFm = g2.getFontMetrics(titleFont);
        FontMetrics btnFm = g2.getFontMetrics(subtitleFont);
        FontMetrics labelFm = g2.getFontMetrics(labelFont);

        int padX = 42;
        int padY = 26;
        int btnPadX = 22;
        int btnPadY = 10;
        int btnGap = 16;
        int contentGap = 26;

        int titleW = titleFm.stringWidth(title);
        int labelW1 = labelFm.stringWidth(bgmLabel);
        int labelW2 = labelFm.stringWidth(sfxLabel);
        int maxLabelW = Math.max(labelW1, labelW2);
        
        // Slider dimensions
        int sliderWidth = 280;
        int sliderHeight = 10;
        int thumbSize = 20;
        int toggleSize = 52;
        
        int controlWidth = maxLabelW + 36 + toggleSize + 24 + sliderWidth;
        int b1 = btnFm.stringWidth(backText.toUpperCase()) + btnPadX * 2;
        int buttonWidth = Math.max(b1, controlWidth);
        int buttonHeight = btnFm.getHeight() + btnPadY * 2;

        int panelWidth = Math.max(titleW, buttonWidth) + padX * 2;
        int panelHeight = padY * 2 + titleFm.getHeight() + btnGap + 
                 (labelFm.getHeight() + 26) * 2 + contentGap + buttonHeight + btnGap;

        int panelX = (getWidth() - panelWidth) / 2;
        int panelY = (getHeight() - panelHeight) / 2;

        // Draw shadow
        g2.setColor(new Color(8, 6, 4, 100));
        g2.fillRoundRect(panelX + 6, panelY + 8, panelWidth, panelHeight, 14, 14);

        // Draw panel background
        Color panelBg = new Color(70, 55, 40, 230);
        g2.setColor(panelBg);
        g2.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 14, 14);

        // Draw border
        g2.setColor(new Color(30, 24, 18, 170));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 14, 14);

        // Draw title
        int titleX = panelX + (panelWidth - titleW) / 2;
        int titleY = panelY + padY + titleFm.getAscent();
        g2.setFont(titleFont);
        g2.setColor(new Color(245, 242, 238));
        g2.drawString(title, titleX, titleY);

        // Calculate control areas
        int controlStartY = titleY + padY + btnGap;
        int controlX = panelX + padX;

        // BGM Control Row
        int bgmLabelX = controlX;
        int bgmLabelY = controlStartY + (toggleSize + labelFm.getAscent() - labelFm.getDescent()) / 2;
        Color controlTextColor = new Color(245, 242, 238);
        
        g2.setFont(labelFont);
        g2.setColor(controlTextColor);
        g2.drawString(bgmLabel, bgmLabelX, bgmLabelY);

        int bgmToggleX = bgmLabelX + maxLabelW + 28;
        int bgmToggleY = controlStartY;
        bgmToggle = new Rectangle(bgmToggleX, bgmToggleY, toggleSize, toggleSize);
        paintToggleButton(g2, bgmToggle, isBgmEnabled(), hoverBgmToggle);

        int bgmSliderX = bgmToggleX + toggleSize + 24;
        int bgmSliderY = controlStartY + (toggleSize - sliderHeight) / 2;
        bgmSliderTrack = new Rectangle(bgmSliderX, bgmSliderY, sliderWidth, sliderHeight);
        
        float bgmGain = soundManager.getMasterBgmGain();
        float bgmPercent = (bgmGain + 80.0f) / 80.0f; // -80 to 0 dB range mapped to 0-1
        bgmPercent = Math.max(0, Math.min(1, bgmPercent));
        int bgmThumbX = bgmSliderX + (int)(bgmPercent * (sliderWidth - thumbSize));
        int bgmThumbY = bgmSliderY - (thumbSize - sliderHeight) / 2;
        bgmSliderThumb = new Rectangle(bgmThumbX, bgmThumbY, thumbSize, thumbSize);
        paintSlider(g2, bgmSliderTrack, bgmSliderThumb, hoverBgmSlider);

        // SFX Control Row
        int sfxControlY = controlStartY + labelFm.getHeight() + 42;
        int sfxLabelY = sfxControlY + (toggleSize + labelFm.getAscent() - labelFm.getDescent()) / 2;
        
        g2.setFont(labelFont);
        g2.setColor(controlTextColor);
        g2.drawString(sfxLabel, bgmLabelX, sfxLabelY);

        int sfxToggleY = sfxControlY;
        sfxToggle = new Rectangle(bgmToggleX, sfxToggleY, toggleSize, toggleSize);
        paintToggleButton(g2, sfxToggle, isSfxEnabled(), hoverSfxToggle);

        int sfxSliderY = sfxControlY + (toggleSize - sliderHeight) / 2;
        sfxSliderTrack = new Rectangle(bgmSliderX, sfxSliderY, sliderWidth, sliderHeight);
        
        float sfxGain = soundManager.getMasterSfxGain();
        float sfxPercent = (sfxGain + 80.0f) / 80.0f; // -80 to 0 dB range mapped to 0-1
        sfxPercent = Math.max(0, Math.min(1, sfxPercent));
        int sfxThumbX = bgmSliderX + (int)(sfxPercent * (sliderWidth - thumbSize));
        int sfxThumbY = sfxSliderY - (thumbSize - sliderHeight) / 2;
        sfxSliderThumb = new Rectangle(sfxThumbX, sfxThumbY, thumbSize, thumbSize);
        paintSlider(g2, sfxSliderTrack, sfxSliderThumb, hoverSfxSlider);

        // Back button
        int buttonX = panelX + (panelWidth - buttonWidth) / 2;
        int backButtonY = sfxControlY + labelFm.getHeight() + contentGap + 20;
        int backButtonWidth = Math.min(buttonWidth, 120);
        backButton = new Rectangle(panelX + (panelWidth - backButtonWidth) / 2, backButtonY, backButtonWidth, buttonHeight);
        paintButton(g2, backButton, backText.toUpperCase(), hoverBackButton);
    }

    private boolean isBgmEnabled() {
        return soundManager.getMasterBgmGain() > -79.0f;
    }

    private boolean isSfxEnabled() {
        return soundManager.getMasterSfxGain() > -79.0f;
    }

    private void paintToggleButton(Graphics2D g2, Rectangle bounds, boolean enabled, boolean hovered) {
        // Background
        Color bgColor = enabled ? new Color(96, 150, 108) : new Color(154, 98, 88);
        if (hovered) {
            bgColor = enabled ? new Color(122, 176, 132) : new Color(182, 120, 108);
        }
        g2.setColor(bgColor);
        g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);

        // Border
        g2.setColor(new Color(40, 30, 22, 180));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);

        // Text
        String text = enabled ? "ON" : "OFF";
        g2.setFont(labelFont);
        g2.setColor(new Color(246, 244, 240));
        FontMetrics fm = g2.getFontMetrics();
        int textX = bounds.x + (bounds.width - fm.stringWidth(text)) / 2;
        int textY = bounds.y + (bounds.height + fm.getAscent()) / 2 - 3;
        g2.drawString(text, textX, textY);
    }

    private void paintSlider(Graphics2D g2, Rectangle track, Rectangle thumb, boolean hovered) {
        // Draw track background
        g2.setColor(new Color(56, 48, 40));
        g2.fillRoundRect(track.x, track.y, track.width, track.height, 4, 4);

        // Draw track border
        g2.setColor(new Color(30, 24, 18, 180));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(track.x, track.y, track.width, track.height, 4, 4);

        // Draw filled portion
        g2.setColor(new Color(132, 104, 72));
        int filledWidth = thumb.x - track.x + thumb.width / 2;
        filledWidth = Math.max(0, Math.min(filledWidth, track.width));
        g2.fillRoundRect(track.x, track.y, filledWidth, track.height, 4, 4);

        // Draw thumb
        Color thumbColor = hovered ? new Color(158, 126, 92) : new Color(132, 104, 72);
        g2.setColor(thumbColor);
        g2.fillRoundRect(thumb.x, thumb.y, thumb.width, thumb.height, 4, 4);

        // Draw thumb border
        g2.setColor(new Color(40, 30, 22, 180));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(thumb.x, thumb.y, thumb.width, thumb.height, 4, 4);
    }

    private void paintButton(Graphics2D g2, Rectangle bounds, String text, boolean hovered) {
        // Draw button background - earthy palette
        Color base = new Color(115, 90, 60);
        Color hover = new Color(140, 110, 80);
        Color bgColor = hovered ? hover : base;
        g2.setColor(bgColor);
        g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 10, 10);

        // subtle inner highlight
        GradientPaint gp = new GradientPaint(bounds.x, bounds.y, new Color(160,130,100,40), bounds.x, bounds.y + bounds.height, new Color(0,0,0,0));
        g2.setPaint(gp);
        g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height / 2, 10, 10);
        g2.setPaint(null);

        // Draw button border
        g2.setColor(new Color(40, 30, 22, 180));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 10, 10);

        // Draw centered button text
        g2.setFont(subtitleFont);
        g2.setColor(new Color(246, 244, 240));
        FontMetrics fm = g2.getFontMetrics();
        int textX = bounds.x + (bounds.width - fm.stringWidth(text)) / 2;
        int textY = bounds.y + (bounds.height + fm.getAscent()) / 2 - 3;
        g2.drawString(text, textX, textY);
    }

    private void handleMouseClick(int x, int y) {
        if (backButton != null && backButton.contains(x, y)) {
            closeSettings();
        } else if (bgmToggle != null && bgmToggle.contains(x, y)) {
            toggleBgm();
        } else if (sfxToggle != null && sfxToggle.contains(x, y)) {
            toggleSfx();
        } else if (bgmSliderTrack != null && bgmSliderTrack.contains(x, y)) {
            // Click on slider track to set position
            int sliderX = x - bgmSliderTrack.x;
            float percent = sliderX / (float) bgmSliderTrack.width;
            percent = Math.max(0, Math.min(1, percent));
            float gainDb = (percent * 80.0f) - 80.0f;
            soundManager.setMasterBgmGain(gainDb);
            repaint();
        } else if (sfxSliderTrack != null && sfxSliderTrack.contains(x, y)) {
            // Click on slider track to set position
            int sliderX = x - sfxSliderTrack.x;
            float percent = sliderX / (float) sfxSliderTrack.width;
            percent = Math.max(0, Math.min(1, percent));
            float gainDb = (percent * 80.0f) - 80.0f;
            soundManager.setMasterSfxGain(gainDb);
            repaint();
        }
    }

    private void handleMouseMove(int x, int y) {
        boolean wasBackHovered = hoverBackButton;
        boolean wasBgmToggleHovered = hoverBgmToggle;
        boolean wasSfxToggleHovered = hoverSfxToggle;
        boolean wasBgmSliderHovered = hoverBgmSlider;
        boolean wasSfxSliderHovered = hoverSfxSlider;

        hoverBackButton = backButton != null && backButton.contains(x, y);
        hoverBgmToggle = bgmToggle != null && bgmToggle.contains(x, y);
        hoverSfxToggle = sfxToggle != null && sfxToggle.contains(x, y);
        hoverBgmSlider = bgmSliderThumb != null && bgmSliderThumb.contains(x, y);
        hoverSfxSlider = sfxSliderThumb != null && sfxSliderThumb.contains(x, y);

        if (draggingBgmSlider && bgmSliderTrack != null) {
            int sliderX = x - bgmSliderTrack.x;
            float percent = sliderX / (float) bgmSliderTrack.width;
            percent = Math.max(0, Math.min(1, percent));
            float gainDb = (percent * 80.0f) - 80.0f;
            soundManager.setMasterBgmGain(gainDb);
            repaint();
            return;
        }

        if (draggingSfxSlider && sfxSliderTrack != null) {
            int sliderX = x - sfxSliderTrack.x;
            float percent = sliderX / (float) sfxSliderTrack.width;
            percent = Math.max(0, Math.min(1, percent));
            float gainDb = (percent * 80.0f) - 80.0f;
            soundManager.setMasterSfxGain(gainDb);
            repaint();
            return;
        }

        // Repaint if hover state changed
        if (wasBackHovered != hoverBackButton || wasBgmToggleHovered != hoverBgmToggle || 
            wasSfxToggleHovered != hoverSfxToggle || wasBgmSliderHovered != hoverBgmSlider || 
            wasSfxSliderHovered != hoverSfxSlider) {
            repaint();
        }
    }

    private void toggleBgm() {
        float currentGain = soundManager.getMasterBgmGain();
        if (isBgmEnabled()) {
            soundManager.setMasterBgmGain(-80.0f); // Mute
        } else {
            soundManager.setMasterBgmGain(0.0f); // Unmute
        }
        repaint();
    }

    private void toggleSfx() {
        float currentGain = soundManager.getMasterSfxGain();
        if (isSfxEnabled()) {
            soundManager.setMasterSfxGain(-80.0f); // Mute
        } else {
            soundManager.setMasterSfxGain(0.0f); // Unmute
        }
        repaint();
    }

    private void closeSettings() {
        window.closeSettings(previousPanelCard);
    }
}
