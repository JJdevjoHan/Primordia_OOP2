package engine;

import assets.Utility.CreditsButton;
import assets.Utility.ExitButton;
import assets.Utility.FontManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.geom.Point2D;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CharacterSelectionPanel extends JPanel implements Runnable{
    private static final int SCREEN_WIDTH = 1536;
    private static final int SCREEN_HEIGHT = 896;
    private static final int GRID_COLUMNS = 4;
    private static final int GRID_ROWS = 2;
    private static final int GRID_SLOTS = GRID_COLUMNS * GRID_ROWS;
    private static final int DEFAULT_FRAME_DELAY_MS = 120;
    private static final int PREVIEW_INSET_PX = 12;
    private static final int GRID_INSET_PX = 2;
    private static final int FEET_MARGIN_PX = 3;
    private static final double PREVIEW_BASE_FILL_RATIO = 0.70;
    private static final double GALLERY_PORTRAIT_FILL_RATIO = 1.14;
    private static final double GALLERY_PORTRAIT_HEIGHT_RATIO = 0.72;
    private static final double GALLERY_PORTRAIT_TOP_PADDING_RATIO = 0.06;
    private static final float GALLERY_BG_ALPHA = 0.75f;
    private static final float PREVIEW_BG_ALPHA = 0.85f;

    private static final class PreviewTweak {
        final double offsetRatioX;
        final double offsetRatioY;
        final double scaleMultiplier;

        PreviewTweak(double offsetRatioX, double offsetRatioY, double scaleMultiplier) {
            this.offsetRatioX = offsetRatioX;
            this.offsetRatioY = offsetRatioY;
            this.scaleMultiplier = scaleMultiplier;
        }
    }

    // Preview-only per-champion tuning.
    // offsetRatioX/offsetRatioY are ratios of the drawn sprite size.
    // scaleMultiplier multiplies PREVIEW_BASE_FILL_RATIO.
    private static final Map<String, PreviewTweak> PREVIEW_TWEAKS = Map.of(
        "idk magician", new PreviewTweak(-0.07, 0.0, 1.30),
        "light mage", new PreviewTweak(-0.07, 0.0, 1.30),
        "fire wizard", new PreviewTweak(-0.07, 0.0, 1.30),
        "steel wizard", new PreviewTweak(-0.07, 0.0, 1.30),
        "nature wizard", new PreviewTweak(-0.07, 0.0, 1.30),
        "water wizard", new PreviewTweak(-0.10, -0.07, 1.30),
        "wind wizard", new PreviewTweak(0.04, 0.015, 1.20),
        "dark wizard", new PreviewTweak(-0.12, 0.0, 1.30)
    );

    private static final List<String> GALLERY_BG_IMAGE_PATHS = List.of(
        "/assets/maps/backgrounds/Battleground1/Bright/Battleground1.png",
        "/assets/maps/backgrounds/Battleground2/Bright/Battleground2.png",
        "/assets/maps/backgrounds/Battleground3/Bright/Battleground3.png"
    );

    // Per-champion portrait nudges (ratio of the drawn sprite size).
    // +x = right, -x = left, +y = down, -y = up.
    private static final Map<String, Point2D.Double> PORTRAIT_OFFSET_RATIOS = Map.of(
        "water wizard", new Point2D.Double(-0.10, -0.07),
        "wind wizard", new Point2D.Double(0.10, 0.015),
        "dark wizard", new Point2D.Double(-0.08, -0.035)
    );

    private final GameWindow window;
    private final List<CharacterDef> characters;
    private final GameMode selectedMode;

    private final Map<String, List<BufferedImage>> animationCache = new HashMap<>();
    private final Map<Integer, BufferedImage> thumbnailCache = new HashMap<>();

    private final List<BufferedImage> galleryBackgrounds = new ArrayList<>();
    private final List<BufferedImage> galleryBackgroundsBlurred = new ArrayList<>();
    private final int[] galleryBackgroundIndexBySlot = new int[GRID_SLOTS];

    private List<BufferedImage> previewFrames = List.of();
    private Rectangle previewBounds = null;
    private int previewFrameIndex = 0;
    private Timer previewTimer;
    private Timer pvpStartTimer;

    private int focusedIndex = 0;
    private int playerOneIndex = -1;
    private boolean selectingPlayerOne = true;

    private JButton exitButton;
    private JRootPane boundRootPane;

    private final Font titleFont = FontManager.getFont(38f).deriveFont(Font.BOLD);
    private final Font subtitleFont = FontManager.getFont(26f).deriveFont(Font.BOLD);
    private final Font bodyFont = FontManager.getFont(22f).deriveFont(Font.PLAIN);
    private final Font labelFont = FontManager.getFont(24f).deriveFont(Font.BOLD);

    //variables for sound effects and bgm
    private int selectionSoundIndex = 2;
    private final SoundManager bgmCharacterSelection = new SoundManager();
    private final SoundManager playerSelection = new SoundManager();

    public CharacterSelectionPanel(GameWindow window, GameMode mode) {
        this.window = window;
        this.selectedMode = mode;
        this.characters = GamePanel.ALL_CHARACTERS;
        playMusic(0);
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);

        resetSelectionState();
        loadGalleryBackgrounds();
        randomizeGalleryBackgroundSelection();

        exitButton = new ExitButton().createExitButton(this);
        add(exitButton);
        // Preload player selection SFX so playback is immediate when Enter is pressed
        new Thread(() -> {
            try {
                playerSelection.preload(1); // player 1
                playerSelection.preload(2); // player 2
            } catch (Exception ignored) {
            }
        }).start();
    }

    private void loadGalleryBackgrounds() {
        if (!galleryBackgrounds.isEmpty()) {
            return;
        }

        for (String path : GALLERY_BG_IMAGE_PATHS) {
            try {
                URL resource = getClass().getResource(path);
                if (resource == null) {
                    continue;
                }
                BufferedImage image = ImageIO.read(resource);
                if (image != null) {
                    galleryBackgrounds.add(image);
                    galleryBackgroundsBlurred.add(blurImage(image));
                }
            } catch (Exception ignored) {
            }
        }
    }

    private BufferedImage blurImage(BufferedImage source) {
        if (source == null) {
            return null;
        }

        // Small, subtle blur (5x5 box blur). Cached per background.
        int size = 5;
        float weight = 1.0f / (size * size);
        float[] data = new float[size * size];
        for (int i = 0; i < data.length; i++) {
            data[i] = weight;
        }

        try {
            ConvolveOp op = new ConvolveOp(new Kernel(size, size, data), ConvolveOp.EDGE_NO_OP, null);
            BufferedImage dest = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
            op.filter(source, dest);
            return dest;
        } catch (Exception ignored) {
            return source;
        }
    }

    private void randomizeGalleryBackgroundSelection() {
        if (galleryBackgrounds.isEmpty()) {
            return;
        }

        Random random = new Random();
        for (int i = 0; i < galleryBackgroundIndexBySlot.length; i++) {
            galleryBackgroundIndexBySlot[i] = random.nextInt(galleryBackgrounds.size());
        }
    }

    @Override
    public void doLayout() {
        super.doLayout();
        int margin = 20;
        if (exitButton != null) {
            exitButton.setBounds(getWidth() - 50 - margin, margin, 40, 40);
        }
    }


    @Override
    public void addNotify() {
        super.addNotify();
        installKeyBindings();
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    @Override
    public void removeNotify() {
        uninstallKeyBindings();
        super.removeNotify();
    }

    public void resetSelectionState() {
        stopPvpStartTimer();
        selectingPlayerOne = true;
        playerOneIndex = -1;
        focusedIndex = 0;
        previewFrameIndex = 0;
        randomizeGalleryBackgroundSelection();
        if (!characters.isEmpty()) {
            setFocusedIndex(0);
        } else {
            previewBounds = null;
        }
        SwingUtilities.invokeLater(this::requestFocusInWindow);
        repaint();
    }

    private void handleInput(int keyCode) {
        if (characters.isEmpty()) {
            return;
        }

        switch (keyCode) {
            case KeyEvent.VK_LEFT -> moveFocus(-1, 0);
            case KeyEvent.VK_RIGHT -> moveFocus(1, 0);
            case KeyEvent.VK_UP -> moveFocus(0, -1);
            case KeyEvent.VK_DOWN -> moveFocus(0, 1);
            case KeyEvent.VK_ENTER -> confirmSelection();
            default -> {
                return;
            }
        }

        repaint();
    }

    private void installKeyBindings() {
        JRootPane rootPane = SwingUtilities.getRootPane(this);
        if (rootPane == null) {
            return;
        }

        if (boundRootPane == rootPane) {
            return;
        }

        uninstallKeyBindings();
        boundRootPane = rootPane;

        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "moveLeft");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "moveRight");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "moveUp");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "moveDown");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "confirmSelection");

        actionMap.put("moveLeft", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { handleInput(KeyEvent.VK_LEFT); }
        });
        actionMap.put("moveRight", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { handleInput(KeyEvent.VK_RIGHT); }
        });
        actionMap.put("moveUp", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { handleInput(KeyEvent.VK_UP); }
        });
        actionMap.put("moveDown", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { handleInput(KeyEvent.VK_DOWN); }
        });
        actionMap.put("confirmSelection", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { handleInput(KeyEvent.VK_ENTER); }
        });
    }

    private void uninstallKeyBindings() {
        if (boundRootPane == null) {
            return;
        }

        InputMap inputMap = boundRootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = boundRootPane.getActionMap();

        inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0));
        inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0));
        inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0));
        inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0));
        inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));

        actionMap.remove("moveLeft");
        actionMap.remove("moveRight");
        actionMap.remove("moveUp");
        actionMap.remove("moveDown");
        actionMap.remove("confirmSelection");
        boundRootPane = null;
    }

    private void moveFocus(int dx, int dy) {
        if (characters.size() == 1) {
            setFocusedIndex(0);
            return;
        }

        int nextIndex = focusedIndex;
        if (dx < 0 || dy < 0) {
            nextIndex = Math.max(0, focusedIndex - 1);
        } else if (dx > 0 || dy > 0) {
            nextIndex = Math.min(characters.size() - 1, focusedIndex + 1);
        }

        if (nextIndex != focusedIndex) {
            setFocusedIndex(nextIndex);
        }
    }

    private void setFocusedIndex(int newIndex) {
        if (newIndex < 0 || newIndex >= characters.size()) {
            return;
        }
        focusedIndex = newIndex;
        CharacterDef selected = characters.get(focusedIndex);
        previewFrames = loadAnimationFrames(selected.idleAnimation);
        previewBounds = getCombinedOpaqueBounds(previewFrames);
        previewFrameIndex = 0;
        restartPreviewTimer(selected.idleAnimation.frameDelayMs);
    }

    private void confirmSelection() {
        switch (selectedMode) {
            case PVP -> {
                if (selectingPlayerOne) {
                    playerOneIndex = focusedIndex;
                    selectingPlayerOne = false;
                    playSE(1);
                    return;
                }
                selectionSoundIndex = 2;
                startPlay();
                // Delay starting the PvP match by 1 second after playing selection SFX
                stopPvpStartTimer();
                pvpStartTimer = new Timer(1000, e -> {
                    ((Timer) e.getSource()).stop();
                    pvpStartTimer = null;
                    window.startPvPMatch(playerOneIndex, focusedIndex);
                });
                pvpStartTimer.setRepeats(false);
                pvpStartTimer.start();
            }

            case SURVIVAL -> {
                selectionSoundIndex = 4;
                startPlay();
                window.startSurvivalMatch(focusedIndex);
            }

            case ARCADE -> {
                selectionSoundIndex = 3;
                startPlay();
                window.startArcadeMatch(focusedIndex);
            }
        }
    }

    private void startPlay() {
        Thread t = new Thread(this);
        t.setDaemon(true);
        t.start();
    }

    private void restartPreviewTimer(int frameDelayMs) {
        if (previewTimer != null) {
            previewTimer.stop();
            previewTimer = null;
        }

        if (previewFrames.size() <= 1) {
            return;
        }

        int safeDelay = frameDelayMs > 0 ? frameDelayMs : DEFAULT_FRAME_DELAY_MS;
        previewTimer = new Timer(safeDelay, e -> {
            previewFrameIndex = (previewFrameIndex + 1) % previewFrames.size();
            repaint();
        });
        previewTimer.start();
    }

    private List<BufferedImage> loadAnimationFrames(CharacterDef.AnimationDef animation) {
        if (animation == null || animation.sheetPath == null || animation.sheetPath.isBlank()) {
            return List.of();
        }

        List<BufferedImage> cachedFrames = animationCache.get(animation.sheetPath);
        if (cachedFrames != null) {
            return cachedFrames;
        }

        List<BufferedImage> frames = new ArrayList<>();
        try {
            URL resource = getClass().getResource(animation.sheetPath);
            if (resource == null) {
                animationCache.put(animation.sheetPath, List.of());
                return List.of();
            }

            BufferedImage sheet = ImageIO.read(resource);
            if (sheet == null || animation.frameWidth <= 0 || animation.frameHeight <= 0) {
                animationCache.put(animation.sheetPath, List.of());
                return List.of();
            }

            int frameWidth = animation.frameWidth;
            int frameHeight = animation.frameHeight;
            int columns = sheet.getWidth() / frameWidth;
            int rows = sheet.getHeight() / frameHeight;

            if (columns <= 0 || rows <= 0) {
                // Support sheets that use a different square frame size (e.g., 96x96).
                int inferredSize = sheet.getHeight();
                if (inferredSize > 0 && sheet.getWidth() % inferredSize == 0) {
                    frameWidth = inferredSize;
                    frameHeight = inferredSize;
                    columns = sheet.getWidth() / frameWidth;
                    rows = sheet.getHeight() / frameHeight;
                }
            }

            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < columns; col++) {
                    frames.add(sheet.getSubimage(
                        col * frameWidth,
                        row * frameHeight,
                        frameWidth,
                        frameHeight
                    ));
                }
            }

            if (frames.isEmpty()) {
                frames.add(sheet);
            }
        } catch (Exception ignored) {
            frames = List.of();
        }

        List<BufferedImage> immutableFrames = List.copyOf(frames);
        animationCache.put(animation.sheetPath, immutableFrames);
        return immutableFrames;
    }

    private BufferedImage getThumbnail(int index) {
        if (index < 0 || index >= characters.size()) {
            return null;
        }
        if (thumbnailCache.containsKey(index)) {
            return thumbnailCache.get(index);
        }

        CharacterDef def = characters.get(index);
        List<BufferedImage> frames = loadAnimationFrames(def.idleAnimation);
        BufferedImage thumb = frames.isEmpty() ? null : frames.get(0);
        thumbnailCache.put(index, thumb);
        return thumb;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        paintBackground(g2);

        int width = getWidth();
        int height = getHeight();
        int margin = 24;
        int sectionGap = 14;

        int topHeight = (int) (height * 0.40);
        int leftWidth = (int) ((width - margin * 2 - sectionGap) * 0.30);

        Rectangle leftPanel = new Rectangle(margin, margin, leftWidth, topHeight);
        Rectangle rightPanel = new Rectangle(margin + leftWidth + sectionGap, margin, width - (margin * 2 + leftWidth + sectionGap), topHeight);
        Rectangle bottomPanel = new Rectangle(margin, margin + topHeight + sectionGap, width - margin * 2, height - (margin * 2 + topHeight + sectionGap));

        drawSectionFrame(g2, leftPanel, new Color(226, 220, 203));
        drawSectionFrame(g2, rightPanel, new Color(219, 216, 209));
        drawSectionFrame(g2, bottomPanel, new Color(232, 228, 216));

        paintPreviewPanel(g2, leftPanel);
        paintGridPanel(g2, rightPanel);
        paintDetailsPanel(g2, bottomPanel);

        g2.dispose();
    }

    private void paintBackground(Graphics2D g2) {
        GradientPaint gradient = new GradientPaint(
            0,
            0,
            new Color(37, 31, 25),
            0,
            getHeight(),
            new Color(70, 55, 39)
        );
        g2.setPaint(gradient);
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setColor(new Color(140, 112, 76, 40));
        for (int i = 0; i < getHeight(); i += 28) {
            g2.fillRect(0, i, getWidth(), 1);
        }
    }

    private void drawSectionFrame(Graphics2D g2, Rectangle rect, Color fill) {
        g2.setColor(fill);
        g2.fillRect(rect.x, rect.y, rect.width, rect.height);

        g2.setColor(new Color(27, 22, 18));
        g2.setStroke(new BasicStroke(6f));
        g2.drawRect(rect.x, rect.y, rect.width, rect.height);
    }

    private void paintPreviewPanel(Graphics2D g2, Rectangle panel) {
        g2.setFont(subtitleFont);
        g2.setColor(new Color(28, 24, 20));
        String title = selectingPlayerOne ? "Player 1 Preview" : "Player 2 Preview";
        g2.drawString(title, panel.x + 18, panel.y + 36);

        int spriteAreaX = panel.x + 24;
        int spriteAreaY = panel.y + 52;
        int spriteAreaW = panel.width - 48;
        int spriteAreaH = panel.height - 76;

        paintPreviewBackground(g2, new Rectangle(spriteAreaX, spriteAreaY, spriteAreaW, spriteAreaH));
        g2.setColor(new Color(24, 20, 17));
        g2.setStroke(new BasicStroke(4f));
        g2.drawRect(spriteAreaX, spriteAreaY, spriteAreaW, spriteAreaH);

        if (characters.isEmpty()) {
            return;
        }

        BufferedImage frame = previewFrames.isEmpty() ? null : previewFrames.get(previewFrameIndex);
        if (frame == null) {
            return;
        }
        CharacterDef selected = (focusedIndex >= 0 && focusedIndex < characters.size()) ? characters.get(focusedIndex) : null;
        String selectedName = selected != null ? selected.name : null;
        PreviewTweak tweak = getPreviewTweak(selectedName);
        double fillRatio = PREVIEW_BASE_FILL_RATIO * tweak.scaleMultiplier;
        drawPreviewShadow(g2, frame, spriteAreaX, spriteAreaY, spriteAreaW, spriteAreaH, fillRatio, previewBounds, PREVIEW_INSET_PX, tweak.offsetRatioX, tweak.offsetRatioY);
        drawFittedSprite(g2, frame, spriteAreaX, spriteAreaY, spriteAreaW, spriteAreaH, fillRatio, previewBounds, PREVIEW_INSET_PX, true, tweak.offsetRatioX, tweak.offsetRatioY);
        drawElementInfoBadge(g2, selected, new Rectangle(spriteAreaX, spriteAreaY, spriteAreaW, spriteAreaH));
        drawHpMpBadge(g2, selected, new Rectangle(spriteAreaX, spriteAreaY, spriteAreaW, spriteAreaH));

        // Draw character name overlay inside the preview area (centered near bottom)
        if (selectedName != null && !selectedName.isBlank()) {
            BufferedImage nameBg = getBlurredGalleryBackgroundForSlot(focusedIndex);
            int overlayW = Math.min(spriteAreaW - 40, 420);
            int overlayH = 44;
            int overlayX = spriteAreaX + (spriteAreaW - overlayW) / 2;
            int overlayY = spriteAreaY + spriteAreaH - overlayH - 12;

            Composite oldComp = g2.getComposite();
            Shape oldClip = g2.getClip();

            // Draw blurred background behind the name with low opacity
            if (nameBg != null) {
                g2.setClip(new Rectangle(overlayX, overlayY, overlayW, overlayH));
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
                drawCoverImage(g2, nameBg, new Rectangle(overlayX, overlayY, overlayW, overlayH));
                g2.setComposite(oldComp);
                g2.setClip(oldClip);
            }

            // Dark translucent strip to improve readability
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
            g2.setColor(new Color(24, 20, 17));
            g2.fillRoundRect(overlayX, overlayY, overlayW, overlayH, 10, 10);
            g2.setComposite(oldComp);

            // Draw name text centered — larger, bold, ALL CAPS for readability
            String nameUpper = selectedName.toUpperCase();
            g2.setFont(bodyFont.deriveFont(Font.BOLD, 24f));
            g2.setColor(new Color(245, 242, 238));
            FontMetrics fm = g2.getFontMetrics();
            int textX = overlayX + (overlayW - fm.stringWidth(nameUpper)) / 2;
            int textY = overlayY + (overlayH + fm.getAscent()) / 2 - 4;
            g2.drawString(nameUpper, textX, textY);
        }
    }

    private void drawElementInfoBadge(Graphics2D g2, CharacterDef selected, Rectangle bounds) {
        if (selected == null) return;

        String typeText = selected.archetype == null || selected.archetype.isBlank()
                ? "Type: Unknown"
                : "Type: " + selected.archetype;
        String weakText = "Weak: " + formatWeaknesses(selected.weaknesses);

        Font badgeFont = bodyFont.deriveFont(Font.BOLD, 18f);
        g2.setFont(badgeFont);
        FontMetrics fm = g2.getFontMetrics();

        int padX = 12;
        int padY = 8;
        int lineGap = 4;
        int textW = Math.max(fm.stringWidth(typeText), fm.stringWidth(weakText));
        int badgeW = Math.min(bounds.width - 24, textW + padX * 2);
        int badgeH = fm.getHeight() * 2 + lineGap + padY * 2;
        int badgeX = bounds.x + 12;
        int badgeY = bounds.y + 12;

        Composite oldComposite = g2.getComposite();
        Stroke oldStroke = g2.getStroke();
        Shape oldClip = g2.getClip();
        g2.setClip(bounds);

        g2.setComposite(AlphaComposite.SrcOver.derive(0.82f));
        g2.setColor(new Color(22, 27, 34));
        g2.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 10, 10);
        g2.setComposite(oldComposite);

        g2.setColor(new Color(215, 185, 95));
        g2.setStroke(new BasicStroke(1.6f));
        g2.drawRoundRect(badgeX, badgeY, badgeW, badgeH, 10, 10);

        int textX = badgeX + padX;
        int textY = badgeY + padY + fm.getAscent();
        g2.setColor(new Color(238, 246, 255));
        g2.drawString(typeText, textX, textY);
        g2.setColor(new Color(255, 220, 160));
        g2.drawString(weakText, textX, textY + fm.getHeight() + lineGap);

        g2.setClip(oldClip);
        g2.setStroke(oldStroke);
        g2.setComposite(oldComposite);
    }

    private void drawHpMpBadge(Graphics2D g2, CharacterDef selected, Rectangle bounds) {
        // Read HP and MP directly from the selected character definition
        int displayHp = selected != null ? selected.getMaxHp() : 100;
        int displayMp = selected != null ? selected.getMaxMp() : 100;

        Font badgeFont = bodyFont.deriveFont(Font.BOLD, 18f);
        g2.setFont(badgeFont);
        FontMetrics fm = g2.getFontMetrics();

        String hpText = "HP: " + displayHp;
        String mpText = "MP: " + displayMp;

        int padX = 12;
        int padY = 8;
        int lineGap = 4;
        int textW = Math.max(fm.stringWidth(hpText), fm.stringWidth(mpText));
        int badgeW = Math.min(bounds.width - 24, textW + padX * 2);
        int badgeH = fm.getHeight() * 2 + lineGap + padY * 2;

        // Position at top-right (mirroring the element badge at top-left)
        int badgeX = bounds.x + bounds.width - badgeW - 12;
        int badgeY = bounds.y + 12;

        Composite oldComposite = g2.getComposite();
        Stroke oldStroke = g2.getStroke();
        Shape oldClip = g2.getClip();
        g2.setClip(bounds);

        // Dark translucent background
        g2.setComposite(AlphaComposite.SrcOver.derive(0.82f));
        g2.setColor(new Color(22, 27, 34));
        g2.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 10, 10);
        g2.setComposite(oldComposite);

        // Golden border (same as element badge)
        g2.setColor(new Color(215, 185, 95));
        g2.setStroke(new BasicStroke(1.6f));
        g2.drawRoundRect(badgeX, badgeY, badgeW, badgeH, 10, 10);

        g2.setFont(badgeFont);
        int textX = badgeX + padX;
        int textY = badgeY + padY + fm.getAscent();

        // HP line — soft green
        g2.setColor(new Color(120, 220, 130));
        g2.drawString(hpText, textX, textY);

        // MP line — blue
        g2.setColor(new Color(100, 180, 255));
        g2.drawString(mpText, textX, textY + fm.getHeight() + lineGap);

        g2.setClip(oldClip);
        g2.setStroke(oldStroke);
        g2.setComposite(oldComposite);
    }

    private String formatWeaknesses(String[] weaknesses) {
        if (weaknesses == null || weaknesses.length == 0) {
            return "None";
        }

        StringBuilder sb = new StringBuilder();
        for (String weakness : weaknesses) {
            if (weakness == null || weakness.isBlank()) continue;
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(weakness);
        }
        return sb.isEmpty() ? "None" : sb.toString();
    }

    private PreviewTweak getPreviewTweak(String characterName) {
        if (characterName == null) {
            return new PreviewTweak(0.0, 0.0, 1.0);
        }
        PreviewTweak tweak = PREVIEW_TWEAKS.get(characterName.trim().toLowerCase());
        return tweak != null ? tweak : new PreviewTweak(0.0, 0.0, 1.0);
    }

    private void paintPreviewBackground(Graphics2D g2, Rectangle bounds) {
        BufferedImage bg = getBlurredGalleryBackgroundForSlot(focusedIndex);
        if (bg == null) {
            g2.setColor(new Color(244, 241, 236));
            g2.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            return;
        }

        Composite oldComposite = g2.getComposite();
        Shape oldClip = g2.getClip();

        g2.setClip(bounds);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, PREVIEW_BG_ALPHA));
        drawCoverImage(g2, bg, bounds);

        g2.setComposite(oldComposite);
        g2.setClip(oldClip);
    }

    private void paintGridPanel(Graphics2D g2, Rectangle panel) {
        g2.setFont(subtitleFont);
        g2.setColor(new Color(28, 24, 20));
        g2.drawString("Champion Gallery", panel.x + 18, panel.y + 36);

        if (characters.isEmpty()) {
            return;
        }

        int gridX = panel.x + 18;
        int gridY = panel.y + 52;
        int gridW = panel.width - 36;
        int gridH = panel.height - 70;

        int cols = GRID_COLUMNS;
        int rows = GRID_ROWS;
        int hGap = 10;
        int vGap = 12;

        int cellW = (gridW - hGap * (cols - 1)) / cols;
        int cellH = (gridH - vGap * (rows - 1)) / rows;

        for (int i = 0; i < GRID_SLOTS; i++) {
            int row = i / cols;
            int col = i % cols;

            int cellX = gridX + col * (cellW + hGap);
            int cellY = gridY + row * (cellH + vGap);

            paintGalleryCellBackground(g2, i, new Rectangle(cellX, cellY, cellW, cellH));

            g2.setColor(new Color(20, 20, 20));
            g2.setStroke(new BasicStroke(3f));
            g2.drawRect(cellX, cellY, cellW, cellH);

            if (i < characters.size() && i == playerOneIndex && !selectingPlayerOne) {
                g2.setColor(new Color(196, 147, 35));
                g2.setStroke(new BasicStroke(4f));
                g2.drawRect(cellX + 2, cellY + 2, cellW - 4, cellH - 4);
            }

            if (i < characters.size() && i == focusedIndex) {
                g2.setColor(new Color(42, 101, 214));
                g2.setStroke(new BasicStroke(5f));
                g2.drawRect(cellX + 1, cellY + 1, cellW - 2, cellH - 2);
            }

            if (i < characters.size()) {
                BufferedImage thumb = getThumbnail(i);
                if (thumb != null) {
                    int imageAreaH = cellH - 30;
                    Rectangle portraitBounds = getPortraitBounds(thumb);
                    Point2D.Double offset = getPortraitOffsetRatio(characters.get(i).name);
                    drawFittedSprite(g2, thumb, cellX + 6, cellY + 4, cellW - 12, imageAreaH - 2, GALLERY_PORTRAIT_FILL_RATIO, portraitBounds, GRID_INSET_PX, false, offset.x, offset.y);
                }
            } else {
                g2.setFont(bodyFont.deriveFont(Font.BOLD, 18f));
                g2.setColor(new Color(120, 110, 98));
                String emptyLabel = "Empty";
                FontMetrics fm = g2.getFontMetrics();
                int textX = cellX + Math.max(6, (cellW - fm.stringWidth(emptyLabel)) / 2);
                int textY = cellY + cellH / 2 + 6;
                g2.drawString(emptyLabel, textX, textY);
            }

            g2.setFont(bodyFont.deriveFont(Font.BOLD, 18f));
            if (i < characters.size()) {
                String name = characters.get(i).name;
                FontMetrics fm = g2.getFontMetrics();
                int labelPaddingX = 10;
                int labelPaddingY = 4;
                int labelW = Math.min(cellW - 12, fm.stringWidth(name) + labelPaddingX * 2);
                int labelX = cellX + (cellW - labelW) / 2;
                int labelH = fm.getHeight() + labelPaddingY * 2 - 2;
                int labelY = cellY + cellH - labelH - 6;

                g2.setColor(new Color(52, 35, 22, 210));
                g2.fillRoundRect(labelX, labelY, labelW, labelH, 12, 12);
                g2.setColor(new Color(241, 233, 218));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(labelX, labelY, labelW, labelH, 12, 12);

                g2.setColor(new Color(249, 244, 236));
                int textX = labelX + (labelW - fm.stringWidth(name)) / 2;
                int textY = labelY + labelH - labelPaddingY - 2;
                g2.drawString(name, textX, textY);
            }
        }
    }

    private void paintGalleryCellBackground(Graphics2D g2, int slotIndex, Rectangle cellBounds) {
        BufferedImage bg = getGalleryBackgroundForSlot(slotIndex);
        if (bg == null) {
            g2.setColor(new Color(250, 249, 246));
            g2.fillRect(cellBounds.x, cellBounds.y, cellBounds.width, cellBounds.height);
            return;
        }

        Composite oldComposite = g2.getComposite();
        Shape oldClip = g2.getClip();
        g2.setClip(cellBounds);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, GALLERY_BG_ALPHA));
        drawCoverImage(g2, bg, cellBounds);
        g2.setComposite(oldComposite);
        g2.setClip(oldClip);
    }

    private BufferedImage getGalleryBackgroundForSlot(int slotIndex) {
        if (galleryBackgrounds.isEmpty()) {
            return null;
        }

        int safeSlot = Math.floorMod(slotIndex, galleryBackgroundIndexBySlot.length);
        int bgIndex = galleryBackgroundIndexBySlot[safeSlot];
        if (bgIndex < 0 || bgIndex >= galleryBackgrounds.size()) {
            bgIndex = 0;
        }
        return galleryBackgrounds.get(bgIndex);
    }

    private BufferedImage getBlurredGalleryBackgroundForSlot(int slotIndex) {
        if (galleryBackgroundsBlurred.isEmpty()) {
            return null;
        }

        int safeSlot = Math.floorMod(slotIndex, galleryBackgroundIndexBySlot.length);
        int bgIndex = galleryBackgroundIndexBySlot[safeSlot];
        if (bgIndex < 0 || bgIndex >= galleryBackgroundsBlurred.size()) {
            bgIndex = 0;
        }
        return galleryBackgroundsBlurred.get(bgIndex);
    }

    private void drawCoverImage(Graphics2D g2, BufferedImage image, Rectangle dest) {
        if (image == null || dest.width <= 0 || dest.height <= 0) {
            return;
        }

        double scale = Math.max(
            dest.getWidth() / image.getWidth(),
            dest.getHeight() / image.getHeight()
        );

        int drawW = (int) Math.ceil(image.getWidth() * scale);
        int drawH = (int) Math.ceil(image.getHeight() * scale);
        int drawX = dest.x + (dest.width - drawW) / 2;
        int drawY = dest.y + (dest.height - drawH) / 2;

        g2.drawImage(image, drawX, drawY, drawW, drawH, this);
    }

    private void paintDetailsPanel(Graphics2D g2, Rectangle panel) {
        int x = panel.x + 20;
        int y = panel.y + 34;
        int textWidth = panel.width - 40;

        g2.setFont(titleFont);
        g2.setColor(new Color(24, 20, 16));

        String phaseText = selectingPlayerOne ? "Player 1: Choose Your Champion" : "Player 2: Choose Your Champion";
        g2.drawString(phaseText, x, y);
        y += 28;

        if (characters.isEmpty()) {
            return;
        }

        CharacterDef selected = characters.get(focusedIndex);
        // Add immersive blurred background using the character's gallery background
        BufferedImage detailBg = getBlurredGalleryBackgroundForSlot(focusedIndex);
        if (detailBg != null) {
            Composite oldComp = g2.getComposite();
            Shape oldClip = g2.getClip();
            g2.setClip(panel);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.14f));
            drawCoverImage(g2, detailBg, panel);
            g2.setComposite(oldComp);
            g2.setClip(oldClip);
        }

        // Content card with gradient
        int cardX = panel.x + 12;
        int cardY = y + 12;
        int cardW = panel.width - 24;
        int cardH = panel.height - (cardY - panel.y) - 18;
        GradientPaint cardGrad = new GradientPaint(
            cardX, cardY,           new Color(250, 247, 243, 215),
            cardX, cardY + cardH,   new Color(232, 226, 217, 215));
        g2.setPaint(cardGrad);
        g2.fillRoundRect(cardX, cardY, cardW, cardH, 14, 14);
        g2.setPaint(null);
        g2.setColor(new Color(130, 100, 60, 180));
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawRoundRect(cardX, cardY, cardW, cardH, 14, 14);

        int innerX = cardX + 18;
        int innerY = cardY + 16;
        int innerW = cardW - 36;

        int headerH = 32;

        // --- Backstory header (gradient) ---
        int backHeaderY = innerY;
        GradientPaint bsHeaderGrad = new GradientPaint(
            innerX, backHeaderY,           new Color(95, 72, 40),
            innerX, backHeaderY + headerH, new Color(62, 46, 22));
        g2.setPaint(bsHeaderGrad);
        g2.fillRoundRect(innerX, backHeaderY, innerW, headerH, 10, 10);
        g2.setPaint(null);
        g2.setColor(new Color(215, 185, 95, 180));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(innerX, backHeaderY, innerW, headerH, 10, 10);
        g2.setColor(new Color(255, 248, 230));
        g2.setFont(labelFont.deriveFont(Font.BOLD, 23f));
        FontMetrics bhFm = g2.getFontMetrics();
        g2.drawString("Backstory", innerX + 12, backHeaderY + (headerH + bhFm.getAscent()) / 2 - 3);

        // --- Backstory text area ---
        int backstoryY = backHeaderY + headerH + 6;
        g2.setFont(bodyFont.deriveFont(Font.PLAIN, 22f));
        g2.setColor(new Color(44, 36, 28));
        int descTextWidth = innerW - 24;
        FontMetrics backFm = g2.getFontMetrics();
        int backLineH = Math.max(1, (int) Math.round(backFm.getHeight() * 0.9));
        int descLines = countWrappedLines(g2, selected.backstory == null ? "" : selected.backstory, descTextWidth);
        int backBoxH = Math.max(backLineH, descLines * backLineH + 12);
        g2.setColor(new Color(250, 247, 243, 185));
        g2.fillRoundRect(innerX, backstoryY - 4, innerW, backBoxH + 8, 8, 8);
        g2.setColor(new Color(160, 130, 80, 80));
        g2.setStroke(new BasicStroke(0.8f));
        g2.drawRoundRect(innerX, backstoryY - 4, innerW, backBoxH + 8, 8, 8);
        g2.setColor(new Color(50, 41, 32));
        int descX = innerX + 12;
        int descY = backstoryY + 10;
        drawWrappedText(g2, selected.backstory == null ? "" : selected.backstory, descX, descY, descTextWidth, backLineH);

        // --- Skills header (gradient) ---
        int skillsHeaderY = backstoryY + backBoxH + 12;
        GradientPaint skHeaderGrad = new GradientPaint(
            innerX, skillsHeaderY,           new Color(95, 72, 40),
            innerX, skillsHeaderY + headerH, new Color(62, 46, 22));
        g2.setPaint(skHeaderGrad);
        g2.fillRoundRect(innerX, skillsHeaderY, innerW, headerH, 10, 10);
        g2.setPaint(null);
        g2.setColor(new Color(215, 185, 95, 180));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(innerX, skillsHeaderY, innerW, headerH, 10, 10);
        g2.setColor(new Color(255, 248, 230));
        g2.setFont(labelFont.deriveFont(Font.BOLD, 23f));
        FontMetrics shFm = g2.getFontMetrics();
        g2.drawString("Skills", innerX + 12, skillsHeaderY + (headerH + shFm.getAscent()) / 2 - 3);

        int skillsStartY = skillsHeaderY + headerH + 12;
        drawSkillsTable(g2, selected, innerX, skillsStartY, innerW);
    }

    private int drawSkillsTable(Graphics2D g2, CharacterDef selected, int x, int y, int width) {
        int boxes        = 3;
        int boxGap       = 14;
        int nameBoxH     = 56;
        int tableX       = x;
        int totalGap     = boxGap * (boxes - 1);
        int boxW         = (width - 16 - totalGap) / boxes;
        int maxBoxH      = 0;
        int chipRowH     = 32;
        int chipGap      = 6;
        int accentW      = 5;

        Font descFont = bodyFont.deriveFont(Font.PLAIN, 23f);
        g2.setFont(descFont);
        FontMetrics descFm = g2.getFontMetrics();
        int descLineH = Math.max(1, (int) Math.round(descFm.getHeight() * 0.92));

        for (int i = 0; i < boxes; i++) {
            String desc = selected.getSkillDescription(i + 1);
            g2.setFont(descFont);
            int descLines = countWrappedLines(g2, desc == null ? "" : desc, Math.max(80, boxW - 28));
            boolean hasEffects = hasSkillEffectChips(selected, i + 1);
            int h = nameBoxH + 14
                  + Math.max(descLineH, descLines * descLineH) + 12
                  + chipRowH + chipGap
                  + (hasEffects ? chipRowH + chipGap : 0)
                  + 14;
            maxBoxH = Math.max(maxBoxH, h);
        }

        for (int i = 0; i < boxes; i++) {
            int bx = tableX + i * (boxW + boxGap);
            int by = y;
            String skillType = selected.getSkillType(i + 1);
            Color[] accent   = accentForSkillType(skillType);
            Color accentDark = accent[0];
            Color accentLight = accent[1];

            // --- Card background: subtle gradient ---
            GradientPaint cardGrad = new GradientPaint(
                bx, by,           new Color(252, 250, 247),
                bx, by + maxBoxH, new Color(235, 230, 221));
            g2.setPaint(cardGrad);
            g2.fillRoundRect(bx, by, boxW, maxBoxH, 14, 14);

            // --- Colored left accent strip (clipped to card bounds) ---
            Shape prevClip = g2.getClip();
            g2.setClip(new Rectangle(bx, by, boxW, maxBoxH));
            GradientPaint accentGrad = new GradientPaint(
                bx, by,           accentDark,
                bx, by + maxBoxH, accentDark.darker());
            g2.setPaint(accentGrad);
            g2.fillRect(bx, by, accentW, maxBoxH);
            g2.setClip(prevClip);

            // --- Card border (type-tinted) ---
            g2.setPaint(null);
            g2.setColor(accentDark);
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawRoundRect(bx, by, boxW, maxBoxH, 14, 14);

            // --- Name header with gradient ---
            int nameBoxX = bx + accentW + 5;
            int nameBoxY = by + 8;
            int nameBoxW = boxW - accentW - 13;
            GradientPaint nameGrad = new GradientPaint(
                nameBoxX, nameBoxY,           new Color(98, 72, 42),
                nameBoxX, nameBoxY + nameBoxH, new Color(65, 46, 22));
            g2.setPaint(nameGrad);
            g2.fillRoundRect(nameBoxX, nameBoxY, nameBoxW, nameBoxH, 10, 10);
            g2.setPaint(null);
            g2.setColor(new Color(accentLight.getRed(), accentLight.getGreen(), accentLight.getBlue(), 120));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(nameBoxX, nameBoxY, nameBoxW, nameBoxH, 10, 10);

            // --- Skill name ---
            String skillName = selected.getSkillName(i + 1);
            g2.setColor(new Color(255, 250, 240));
            g2.setFont(bodyFont.deriveFont(Font.BOLD, 25f));
            FontMetrics nameFm = g2.getFontMetrics();
            int nameTextY = nameBoxY + (nameBoxH / 2) + (nameFm.getAscent() / 2) - 3;
            g2.drawString(skillName == null ? "" : skillName, nameBoxX + 10, nameTextY);

            // --- MP badge (top-right inside name header) ---
            int mp = selected.getSkillManaCost(i + 1);
            String mpStr = "MP " + mp;
            g2.setFont(bodyFont.deriveFont(Font.BOLD, 16f));
            FontMetrics mpFm = g2.getFontMetrics();
            int mpBadgeW = mpFm.stringWidth(mpStr) + 16;
            int mpBadgeH = 21;
            int mpBadgeX = nameBoxX + nameBoxW - mpBadgeW - 6;
            int mpBadgeY = nameBoxY + (nameBoxH - mpBadgeH) / 2;
            g2.setColor(new Color(18, 50, 120, 235));
            g2.fillRoundRect(mpBadgeX, mpBadgeY, mpBadgeW, mpBadgeH, 8, 8);
            g2.setColor(new Color(180, 215, 255));
            g2.setStroke(new BasicStroke(0.8f));
            g2.drawRoundRect(mpBadgeX, mpBadgeY, mpBadgeW, mpBadgeH, 8, 8);
            g2.setColor(new Color(210, 235, 255));
            g2.drawString(mpStr, mpBadgeX + 8, mpBadgeY + mpBadgeH - mpFm.getDescent() + 1);

            // --- Type pill (left of MP badge) ---
            String typeLabel = skillType != null ? skillType.toUpperCase() : "SKILL";
            g2.setFont(bodyFont.deriveFont(Font.BOLD, 14f));
            FontMetrics typeFm = g2.getFontMetrics();
            int typePillW = typeFm.stringWidth(typeLabel) + 12;
            int typePillH = 17;
            int typePillX = mpBadgeX - typePillW - 5;
            int typePillY = mpBadgeY + (mpBadgeH - typePillH) / 2;
            g2.setColor(new Color(accentDark.getRed(), accentDark.getGreen(), accentDark.getBlue(), 210));
            g2.fillRoundRect(typePillX, typePillY, typePillW, typePillH, 6, 6);
            g2.setColor(accentLight);
            g2.drawString(typeLabel, typePillX + 6, typePillY + typePillH - typeFm.getDescent() + 2);

            // --- Description ---
            String skillDesc = selected.getSkillDescription(i + 1);
            g2.setColor(new Color(45, 36, 26));
            g2.setFont(descFont);
            int descX = bx + accentW + 10;
            int descY = nameBoxY + nameBoxH + 19;
            int afterDescY = drawWrappedText(g2, skillDesc == null ? "" : skillDesc,
                                             descX, descY, Math.max(80, boxW - accentW - 20), descLineH);

            // --- Chips pinned to card bottom (consistent across all 3 cards) ---
            boolean hasEffects = hasSkillEffectChips(selected, i + 1);
            int effectChipY  = by + maxBoxH - 14 - chipRowH;
            int damageChipY  = hasEffects ? (effectChipY - chipGap - chipRowH) : effectChipY;
            int sepY         = damageChipY - 8;

            // Separator line
            g2.setColor(new Color(accentDark.getRed(), accentDark.getGreen(), accentDark.getBlue(), 60));
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(bx + accentW + 6, sepY, bx + boxW - 8, sepY);

            // Damage chips
            drawSkillDamageChips(g2, selected, i + 1, bx + accentW + 8, damageChipY, boxW - accentW - 16, chipRowH);

            // Effect chips
            if (hasEffects) {
                drawSkillEffectChips(g2, selected, i + 1, bx + accentW + 8,
                                     effectChipY, boxW - accentW - 16, chipRowH);
            }
        }
        return y + maxBoxH + 12;
    }

    private Color[] accentForSkillType(String type) {
        if (type == null) return new Color[]{new Color(80, 70, 60), new Color(215, 210, 200)};
        return switch (type.toLowerCase()) {
            case "damage"  -> new Color[]{new Color(155, 48, 28),  new Color(255, 185, 165)};
            case "debuff"  -> new Color[]{new Color(80, 38, 140),  new Color(215, 170, 255)};
            case "defense" -> new Color[]{new Color(24, 82, 128),  new Color(155, 215, 255)};
            case "heal"    -> new Color[]{new Color(38, 100, 45),  new Color(165, 245, 175)};
            default        -> new Color[]{new Color(75, 65, 50),   new Color(215, 205, 190)};
        };
    }

    private boolean hasSkillEffectChips(CharacterDef def, int skillID) {
        if (def == null) return false;
        return CombatBalance.calculatePoisonDamage(def, skillID) > 0
            || def.getSkillHealValue(skillID) > 0
            || def.getSkillShieldValue(skillID) > 0
            || def.getSkillSelfHeal(skillID) > 0;
    }

    private int drawChip(Graphics2D g2, String text, int x, int y, int h, Color bg, Color border, Color fg) {
        g2.setFont(bodyFont.deriveFont(Font.BOLD, 16f));
        FontMetrics fm = g2.getFontMetrics();
        int padX = 16;
        int w = fm.stringWidth(text) + padX * 2;
        // Fill
        g2.setColor(bg);
        g2.fillRoundRect(x, y, w, h, 8, 8);
        // Border
        g2.setColor(border);
        g2.setStroke(new BasicStroke(0.9f));
        g2.drawRoundRect(x, y, w, h, 8, 8);
        // Text
        g2.setColor(fg);
        g2.drawString(text, x + padX, y + (h + fm.getAscent() - fm.getDescent()) / 2 + 2);
        return w + 6;
    }

    private void drawSkillDamageChips(Graphics2D g2, CharacterDef def, int skillID,
                                      int x, int y, int maxW, int chipH) {
        int neutral = CombatBalance.previewNeutralDamage(def, skillID);
        if (neutral <= 0) return;
        int cx = x;
        cx += drawChip(g2, "DMG " + neutral, cx, y, chipH,
                       new Color(55, 45, 35, 215), new Color(120, 100, 75, 180), new Color(240, 228, 210));
        int adv = CombatBalance.previewAdvantageDamage(def, skillID);
        if (adv != neutral) {
            drawChip(g2, "ADV " + adv, cx, y, chipH,
                     new Color(22, 85, 28, 215), new Color(80, 165, 80, 180), new Color(150, 240, 155));
        }
    }

    private void drawSkillEffectChips(Graphics2D g2, CharacterDef def, int skillID,
                                      int x, int y, int maxW, int chipH) {
        int cx = x;
        int poisonDmg = CombatBalance.calculatePoisonDamage(def, skillID);
        int poisonDur = CombatBalance.calculatePoisonDuration(def, skillID);
        if (poisonDmg > 0 && poisonDur > 0) {
            cx += drawChip(g2, "PSN " + poisonDmg + "x" + poisonDur, cx, y, chipH,
                           new Color(22, 82, 28, 215), new Color(75, 160, 75, 180), new Color(155, 248, 160));
        }
        if (def.getSkillHealValue(skillID) > 0) {
            int heal = CombatBalance.calculateHealing(def, skillID, 1.0, false);
            cx += drawChip(g2, "HEAL +" + heal, cx, y, chipH,
                           new Color(35, 105, 42, 215), new Color(90, 175, 90, 180), new Color(175, 248, 178));
        }
        if (def.getSkillShieldValue(skillID) > 0) {
            int shield = CombatBalance.calculateShieldHealing(def, skillID, 1.0, false);
            cx += drawChip(g2, "SHLD +" + shield, cx, y, chipH,
                           new Color(22, 62, 138, 215), new Color(80, 145, 220, 180), new Color(145, 205, 255));
        }
        if (def.getSkillSelfHeal(skillID) > 0) {
            drawChip(g2, "REGEN +" + def.getSkillSelfHeal(skillID), cx, y, chipH,
                     new Color(118, 86, 16, 215), new Color(195, 155, 60, 180), new Color(248, 205, 105));
        }
    }
    private int countWrappedLines(Graphics2D g2, String text, int maxWidth) {
        if (text == null || text.isBlank()) {
            return 1;
        }

        FontMetrics metrics = g2.getFontMetrics();
        String[] words = text.trim().split("\\s+");
        int lines = 1;
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (metrics.stringWidth(candidate) <= maxWidth) {
                line = new StringBuilder(candidate);
            } else {
                lines++;
                line = new StringBuilder(word);
            }
        }

        return lines;
    }

    private int drawWrappedText(Graphics2D g2, String text, int x, int y, int maxWidth, int lineHeight) {
        if (text == null || text.isBlank()) {
            return y + lineHeight;
        }

        FontMetrics metrics = g2.getFontMetrics();
        String[] words = text.trim().split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (metrics.stringWidth(candidate) <= maxWidth) {
                line = new StringBuilder(candidate);
            } else {
                g2.drawString(line.toString(), x, y);
                y += lineHeight;
                line = new StringBuilder(word);
            }
        }

        if (!line.isEmpty()) {
            g2.drawString(line.toString(), x, y);
            y += lineHeight;
        }

        return y;
    }

    private void drawFittedSprite(Graphics2D g2,
                                  BufferedImage image,
                                  int areaX,
                                  int areaY,
                                  int areaW,
                                  int areaH,
                                  double fillRatio) {
        drawFittedSprite(g2, image, areaX, areaY, areaW, areaH, fillRatio, null, 0, false);
    }

    private void drawFittedSprite(Graphics2D g2,
                                  BufferedImage image,
                                  int areaX,
                                  int areaY,
                                  int areaW,
                                  int areaH,
                                  double fillRatio,
                                  Rectangle sourceBounds,
                                  int insetPx,
                                  boolean alignFeetToBottom) {
        drawFittedSprite(g2, image, areaX, areaY, areaW, areaH, fillRatio, sourceBounds, insetPx, alignFeetToBottom, 0.0, 0.0);
    }

    private void drawFittedSprite(Graphics2D g2,
                                  BufferedImage image,
                                  int areaX,
                                  int areaY,
                                  int areaW,
                                  int areaH,
                                  double fillRatio,
                                  Rectangle sourceBounds,
                                  int insetPx,
                                  boolean alignFeetToBottom,
                                  double offsetRatioX,
                                  double offsetRatioY) {
        if (image == null || areaW <= 0 || areaH <= 0) {
            return;
        }

        int safeInset = Math.max(0, insetPx);
        int innerX = areaX + safeInset;
        int innerY = areaY + safeInset;
        int innerW = Math.max(1, areaW - (safeInset * 2));
        int innerH = Math.max(1, areaH - (safeInset * 2));

        Rectangle bounds = sourceBounds != null ? sourceBounds : getOpaqueBounds(image);
        int sourceX = bounds.x;
        int sourceY = bounds.y;
        int sourceW = Math.max(1, bounds.width);
        int sourceH = Math.max(1, bounds.height);

        double scale = Math.min(
            (innerW * fillRatio) / sourceW,
            (innerH * fillRatio) / sourceH
        );

        int targetW = Math.max(1, (int) Math.round(sourceW * scale));
        int targetH = Math.max(1, (int) Math.round(sourceH * scale));
        int targetX = innerX + (innerW - targetW) / 2;
        int targetY = alignFeetToBottom
            ? (innerY + innerH - targetH - FEET_MARGIN_PX)
            : (innerY + (innerH - targetH) / 2);

        int offsetPxX = (int) Math.round(targetW * offsetRatioX);
        int offsetPxY = (int) Math.round(targetH * offsetRatioY);
        targetX += offsetPxX;
        targetY += offsetPxY;

        g2.drawImage(
            image,
            targetX,
            targetY,
            targetX + targetW,
            targetY + targetH,
            sourceX,
            sourceY,
            sourceX + sourceW,
            sourceY + sourceH,
            this
        );
    }

    private void drawPreviewShadow(Graphics2D g2,
                                   BufferedImage image,
                                   int areaX,
                                   int areaY,
                                   int areaW,
                                   int areaH,
                                   double fillRatio,
                                   Rectangle sourceBounds,
                                   int insetPx,
                                   double offsetRatioX,
                                   double offsetRatioY) {
        Rectangle bounds = getFittedSpriteTargetBounds(image, areaX, areaY, areaW, areaH, fillRatio, sourceBounds, insetPx, true, offsetRatioX, offsetRatioY);
        if (bounds == null) {
            return;
        }

        int shadowW = Math.max(22, (int) Math.round(bounds.width * 0.48));
        int shadowH = Math.max(8, (int) Math.round(bounds.height * 0.06));
        int shadowX = bounds.x + (bounds.width - shadowW) / 2;
        int shadowY = Math.min(bounds.y + bounds.height + 14, areaY + areaH - shadowH - 6);

        Color oldColor = g2.getColor();
        Composite oldComposite = g2.getComposite();
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Shape oldClip = g2.getClip();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setClip(new Rectangle(areaX, areaY, areaW, areaH));
        for (int i = 4; i >= 0; i--) {
            float alpha = switch (i) {
                case 4 -> 0.04f;
                case 3 -> 0.07f;
                case 2 -> 0.12f;
                case 1 -> 0.18f;
                default -> 0.24f;
            };
            int padX = i * 5;
            int padY = i * 3;
            g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
            g2.setColor(Color.BLACK);
            g2.fillOval(shadowX - padX, shadowY - padY, shadowW + padX * 2, shadowH + padY * 2);
        }
        g2.setComposite(oldComposite);
        g2.setColor(oldColor);
        g2.setClip(oldClip);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    private Rectangle getFittedSpriteTargetBounds(BufferedImage image,
                                                  int areaX,
                                                  int areaY,
                                                  int areaW,
                                                  int areaH,
                                                  double fillRatio,
                                                  Rectangle sourceBounds,
                                                  int insetPx,
                                                  boolean alignFeetToBottom,
                                                  double offsetRatioX,
                                                  double offsetRatioY) {
        if (image == null || areaW <= 0 || areaH <= 0) {
            return null;
        }

        int safeInset = Math.max(0, insetPx);
        int innerX = areaX + safeInset;
        int innerY = areaY + safeInset;
        int innerW = Math.max(1, areaW - (safeInset * 2));
        int innerH = Math.max(1, areaH - (safeInset * 2));

        Rectangle bounds = sourceBounds != null ? sourceBounds : getOpaqueBounds(image);
        int sourceW = Math.max(1, bounds.width);
        int sourceH = Math.max(1, bounds.height);

        double scale = Math.min(
            (innerW * fillRatio) / sourceW,
            (innerH * fillRatio) / sourceH
        );

        int targetW = Math.max(1, (int) Math.round(sourceW * scale));
        int targetH = Math.max(1, (int) Math.round(sourceH * scale));
        int targetX = innerX + (innerW - targetW) / 2;
        int targetY = alignFeetToBottom
            ? (innerY + innerH - targetH - FEET_MARGIN_PX)
            : (innerY + (innerH - targetH) / 2);

        targetX += (int) Math.round(targetW * offsetRatioX);
        targetY += (int) Math.round(targetH * offsetRatioY);
        return new Rectangle(targetX, targetY, targetW, targetH);
    }

    private Point2D.Double getPortraitOffsetRatio(String characterName) {
        if (characterName == null) {
            return new Point2D.Double(0.0, 0.0);
        }
        Point2D.Double offset = PORTRAIT_OFFSET_RATIOS.get(characterName.trim().toLowerCase());
        return offset != null ? offset : new Point2D.Double(0.0, 0.0);
    }

    private Rectangle getCombinedOpaqueBounds(List<BufferedImage> frames) {
        if (frames == null || frames.isEmpty()) {
            return null;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (BufferedImage frame : frames) {
            if (frame == null) {
                continue;
            }
            Rectangle bounds = getOpaqueBounds(frame);
            minX = Math.min(minX, bounds.x);
            minY = Math.min(minY, bounds.y);
            maxX = Math.max(maxX, bounds.x + bounds.width - 1);
            maxY = Math.max(maxY, bounds.y + bounds.height - 1);
        }

        if (maxX < minX || maxY < minY) {
            BufferedImage first = frames.get(0);
            if (first == null) {
                return null;
            }
            return new Rectangle(0, 0, first.getWidth(), first.getHeight());
        }

        return new Rectangle(minX, minY, (maxX - minX) + 1, (maxY - minY) + 1);
    }

    private Rectangle getPortraitBounds(BufferedImage image) {
        Rectangle bounds = getOpaqueBounds(image);
        int topPad = (int) Math.round(bounds.height * GALLERY_PORTRAIT_TOP_PADDING_RATIO);
        int portraitY = Math.max(0, bounds.y - topPad);
        int portraitHeight = Math.max(1, (int) Math.round(bounds.height * GALLERY_PORTRAIT_HEIGHT_RATIO));
        return new Rectangle(bounds.x, portraitY, bounds.width, portraitHeight);
    }

    private Rectangle getOpaqueBounds(BufferedImage image) {
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = (image.getRGB(x, y) >>> 24) & 0xff;
                if (alpha == 0) {
                    continue;
                }
                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
            }
        }

        if (maxX < minX || maxY < minY) {
            return new Rectangle(0, 0, image.getWidth(), image.getHeight());
        }

        return new Rectangle(minX, minY, (maxX - minX) + 1, (maxY - minY) + 1);
    }

    //PLAYS THE MUSIC
    public void playMusic(int i) {
        bgmCharacterSelection.setFile(i);
        bgmCharacterSelection.loop();
    }

    //STOPS THE MUSIC
    public void stopMusic() {
        if(bgmCharacterSelection!=null){
            bgmCharacterSelection.stop();
        }

    }

    public void playSE(int i) {
        playerSelection.setFile(i);
        playerSelection.play();
    }


    //The purpose of this is if di ta mo gamit og pause ang "Player 2" audio kay di pahuman naa na sa gameplay, which is why na ato ipause para walay molapas sa gameplay na audio
    @Override
    public void run() {
        playerSelection.setFile(selectionSoundIndex);
        playerSelection.play();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Stop background music after the selection sound finishes
        stopMusic();
    }

    private void stopPvpStartTimer() {
        if (pvpStartTimer != null) {
            pvpStartTimer.stop();
            pvpStartTimer = null;
        }
    }
}
