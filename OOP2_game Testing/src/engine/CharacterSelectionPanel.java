package engine;

import assets.Utility.BackButton;
import assets.Utility.CreditsButton;
import assets.Utility.ExitButton;
import assets.Utility.FontManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final GameWindow window;
    private final List<CharacterDef> characters;
    private final GameMode selectedMode;

    private final Map<String, List<BufferedImage>> animationCache = new HashMap<>();
    private final Map<Integer, BufferedImage> thumbnailCache = new HashMap<>();

    private List<BufferedImage> previewFrames = List.of();
    private Rectangle previewBounds = null;
    private int previewFrameIndex = 0;
    private Timer previewTimer;

    private int focusedIndex = 0;
    private int playerOneIndex = -1;
    private boolean selectingPlayerOne = true;

    private JButton exitButton;
    private JButton backbutton;

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

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleInput(e.getKeyCode());
            }
        });

        resetSelectionState();

        backbutton = new BackButton().createBackButton(window, this);
        add(backbutton);

        exitButton = new ExitButton().createExitButton(this);
        add(exitButton);
    }

    @Override
    public void doLayout() {
        super.doLayout();
        int margin = 20;
        for (Component c : getComponents()) {
            if (c instanceof JButton && ((JButton) c).getText().equals("Back")) {
                int fromBottom = 100; // increase this to move button higher
                int fromRight = 120;  // increase this to move button more to the left
                c.setBounds(getWidth() - fromRight, getHeight() - fromBottom, 100, 40);
            }
        }
        if (exitButton != null) {
            exitButton.setBounds(getWidth() - 50 - margin, margin, 40, 40);
        }
    }


    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    public void resetSelectionState() {
        selectingPlayerOne = true;
        playerOneIndex = -1;
        focusedIndex = 0;
        previewFrameIndex = 0;
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
                window.startPvPMatch(playerOneIndex, focusedIndex);
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
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            System.out.println("Audio Thread Interrupted");
        }
        stopMusic();
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

        g2.setColor(new Color(244, 241, 236));
        g2.fillRect(spriteAreaX, spriteAreaY, spriteAreaW, spriteAreaH);
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
        drawFittedSprite(g2, frame, spriteAreaX, spriteAreaY, spriteAreaW, spriteAreaH, 0.70, previewBounds, PREVIEW_INSET_PX, true);
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

            g2.setColor(new Color(250, 249, 246));
            g2.fillRect(cellX, cellY, cellW, cellH);

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
                    drawFittedSprite(g2, thumb, cellX + 6, cellY + 4, cellW - 12, imageAreaH - 2, 0.84, null, GRID_INSET_PX, true);
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
            g2.setColor(new Color(21, 20, 18));
            if (i < characters.size()) {
                String name = characters.get(i).name;
                FontMetrics fm = g2.getFontMetrics();
                int textX = cellX + Math.max(6, (cellW - fm.stringWidth(name)) / 2);
                int textY = cellY + cellH - 8;
                g2.drawString(name, textX, textY);
            }
        }
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

        if (!selectingPlayerOne && playerOneIndex >= 0 && playerOneIndex < characters.size()) {
            g2.setFont(labelFont);
            g2.setColor(new Color(74, 57, 25));
            g2.drawString("Player 1 Locked: " + characters.get(playerOneIndex).name, x, y + 12);
            y += 32;
        }

        if (characters.isEmpty()) {
            return;
        }

        CharacterDef selected = characters.get(focusedIndex);

        y += 20;
        g2.setFont(labelFont);
        g2.setColor(new Color(31, 25, 20));
        g2.drawString("Name", x, y);

        y += 24;
        g2.setFont(bodyFont);
        g2.setColor(new Color(44, 36, 28));
        y = drawWrappedText(g2, selected.name, x, y, textWidth, 24);

        y += 8;
        g2.setFont(labelFont);
        g2.setColor(new Color(31, 25, 20));
        g2.drawString("Backstory", x, y);

        y += 24;
        g2.setFont(bodyFont);
        g2.setColor(new Color(44, 36, 28));
        y = drawWrappedText(g2, selected.backstory, x, y, textWidth, 24);

        y += 10;
        g2.setFont(labelFont);
        g2.setColor(new Color(31, 25, 20));
        g2.drawString("Skills", x, y);

        y += 24;
        g2.setFont(bodyFont);
        g2.setColor(new Color(44, 36, 28));
        y = drawWrappedText(g2, "Skill 1: " + selected.skill1Name, x, y, textWidth, 24);
        y = drawWrappedText(g2, selected.getSkillDescription(1), x + 16, y, textWidth - 16, 22);
        y = drawWrappedText(g2, "Skill 2: " + selected.skill2Name, x, y + 4, textWidth, 24);
        y = drawWrappedText(g2, selected.getSkillDescription(2), x + 16, y, textWidth - 16, 22);
        y = drawWrappedText(g2, "Skill 3: " + selected.skill3Name, x, y + 4, textWidth, 24);
        drawWrappedText(g2, selected.getSkillDescription(3), x + 16, y, textWidth - 16, 22);
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
        bgmCharacterSelection.play();
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
    }
}
