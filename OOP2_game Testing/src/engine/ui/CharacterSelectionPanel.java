package engine.ui;

import assets.Utility.ExitButton;
import assets.Utility.FontManager;
import engine.gameplay.GamePanel;
import engine.core.GameWindow;
import engine.audio.SoundManager;
import engine.character.CharacterDef;
import engine.enums.GameMode;

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

        int topHeight = (int) (height * 0.48);
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
        String selectedName = (focusedIndex >= 0 && focusedIndex < characters.size()) ? characters.get(focusedIndex).name : null;
        PreviewTweak tweak = getPreviewTweak(selectedName);
        double fillRatio = PREVIEW_BASE_FILL_RATIO * tweak.scaleMultiplier;
        drawPreviewShadow(g2, frame, spriteAreaX, spriteAreaY, spriteAreaW, spriteAreaH, fillRatio, previewBounds, PREVIEW_INSET_PX, tweak.offsetRatioX, tweak.offsetRatioY);
        drawFittedSprite(g2, frame, spriteAreaX, spriteAreaY, spriteAreaW, spriteAreaH, fillRatio, previewBounds, PREVIEW_INSET_PX, true, tweak.offsetRatioX, tweak.offsetRatioY);

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

        // Draw a subtle content card to separate text from background
        int cardX = panel.x + 12;
        int cardY = y + 12;
        int cardW = panel.width - 24;
        int cardH = panel.height - (cardY - panel.y) - 18;
        g2.setColor(new Color(245, 242, 238, 200));
        g2.fillRoundRect(cardX, cardY, cardW, cardH, 14, 14);
        g2.setColor(new Color(60, 50, 40, 160));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(cardX, cardY, cardW, cardH, 14, 14);

        int innerX = cardX + 18;
        int innerY = cardY + 18;
        int innerW = cardW - 36;

        g2.setFont(labelFont);
        int headerH = 30;
        // Place Backstory header near top of the content card to reduce empty space
        int backHeaderY = innerY;
        g2.setFont(labelFont);
        g2.setColor(new Color(75, 57, 30));
        g2.fillRoundRect(innerX, backHeaderY, innerW, headerH, 10, 10);
        g2.setColor(new Color(245, 242, 238));
        g2.setFont(labelFont.deriveFont(Font.BOLD, 26f));
        g2.drawString("Backstory", innerX + 8, backHeaderY + headerH - 6);

        // Backstory text area (height sized to wrapped content)
        int backstoryY = backHeaderY + headerH + 8;
        g2.setFont(bodyFont.deriveFont(Font.PLAIN, 24f));
        g2.setColor(new Color(44, 36, 28));
        int descTextWidth = innerW - 24;
        FontMetrics backFm = g2.getFontMetrics();
        int backLineH = Math.max(1, (int) Math.round(backFm.getHeight() * 0.9));
        int descLines = countWrappedLines(g2, selected.backstory == null ? "" : selected.backstory, descTextWidth);
        // Add a bit more top padding and size box to content; ensure text starts at top
        int backBoxH = Math.max(backLineH, descLines * backLineH + 12);
        g2.setColor(new Color(255, 255, 255, 200));
        g2.fillRoundRect(innerX, backstoryY - 6, innerW, backBoxH + 8, 10, 10);
        g2.setColor(new Color(50, 41, 32));
        int descX = innerX + 12;
        int descY = backstoryY + 12; // start from top padding
        drawWrappedText(g2, selected.backstory == null ? "" : selected.backstory, descX, descY, descTextWidth, backLineH);

        // Skills header strip (colored) and table (preserve layout)
        int skillsHeaderY = backstoryY + backBoxH + 12;
        g2.setFont(labelFont.deriveFont(Font.BOLD, 24f));
        g2.setColor(new Color(75, 57, 30));
        g2.fillRoundRect(innerX, skillsHeaderY, innerW, headerH, 10, 10);
        g2.setColor(new Color(245, 242, 238));
        g2.drawString("Skills", innerX + 8, skillsHeaderY + headerH - 6);

        int skillsStartY = skillsHeaderY + headerH + 12;
        int bottomY = drawSkillsTable(g2, selected, innerX, skillsStartY, innerW);

        // Done drawing details
        return;
    }

    private int drawSkillsTable(Graphics2D g2, CharacterDef selected, int x, int y, int width) {
        // Render three skills horizontally to avoid vertical cutoff.
        int boxes = 3;
        int boxGap = 12;
        int minBoxHeight = 120;
        int nameBoxInnerH = 56;
        int tableX = x + 0;
        int totalGap = boxGap * (boxes - 1);
        int boxW = (width - 16 - totalGap) / boxes;
        int maxBoxH = minBoxHeight;
        int[] boxHeights = new int[boxes];

        // Description font used for measuring and rendering (larger for readability)
        Font descFont = bodyFont.deriveFont(Font.PLAIN, 26f);
        g2.setFont(descFont);
        FontMetrics descFm = g2.getFontMetrics();
        int descLineH = Math.max(1, (int) Math.round(descFm.getHeight() * 0.9));

        // Compute required height for each box based on wrapped description lines
        for (int i = 0; i < boxes; i++) {
            String desc = selected.getSkillDescription(i + 1);
            int descTextWidth = Math.max(80, boxW - 24);
            int descLines = countWrappedLines(g2, desc == null ? "" : desc, descTextWidth);
            int h = Math.max(minBoxHeight, nameBoxInnerH + (descLines * descLineH) + 32);
            boxHeights[i] = h;
            maxBoxH = Math.max(maxBoxH, h);
        }

        // Draw each skill box
        for (int i = 0; i < boxes; i++) {
            int bx = tableX + i * (boxW + boxGap);
            int by = y;

            g2.setColor(new Color(248, 245, 239));
            g2.fillRoundRect(bx, by, boxW, maxBoxH, 12, 12);
            g2.setColor(new Color(53, 44, 34));
            g2.drawRoundRect(bx, by, boxW, maxBoxH, 12, 12);

            // Name header area
            int nameBoxX = bx + 10;
            int nameBoxY = by + 10;
            int nameBoxW = boxW - 20;
            int nameBoxH = nameBoxInnerH;
            g2.setColor(new Color(86, 63, 35));
            g2.fillRoundRect(nameBoxX, nameBoxY, nameBoxW, nameBoxH, 10, 10);
            g2.setColor(new Color(247, 241, 228));
            g2.drawRoundRect(nameBoxX, nameBoxY, nameBoxW, nameBoxH, 10, 10);

            // Skill name
            String skillName = selected.getSkillName(i + 1);
            g2.setColor(new Color(252, 248, 242));
            g2.setFont(bodyFont.deriveFont(Font.BOLD, 30f));
            FontMetrics nameMetrics = g2.getFontMetrics();
            int nameTextX = nameBoxX + 12;
            int nameTextY = nameBoxY + (nameBoxH + nameMetrics.getAscent()) / 2 - 2;
            g2.drawString(skillName == null ? "" : skillName, nameTextX, nameTextY);

            // Skill description
            String skillDesc = selected.getSkillDescription(i + 1);
            g2.setColor(new Color(50, 41, 32));
            g2.setFont(descFont);
            int descX = bx + 12;
            int descY = nameBoxY + nameBoxH + 18;
            int descTextWidth = Math.max(80, boxW - 24);
            drawWrappedText(g2, skillDesc == null ? "" : skillDesc, descX, descY, descTextWidth, descLineH);
        }

        return y + maxBoxH + 12;
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
