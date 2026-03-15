package engine;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;

import assets.Utility.FontManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class GamePanel extends JPanel {
    private final int tileSize = 128;
    private final int screenWidth = tileSize * 12;
    private final int screenHeight = tileSize * 7;
    private static final int DEFAULT_DRAW_WIDTH = 384;
    private static final int DEFAULT_DRAW_HEIGHT = 384;
    private static final int DEFAULT_FRAME_SIZE = 128;
    private static final int DEFAULT_IDLE_DELAY_MS = 120;
    private static final int DEFAULT_DEAD_DELAY_MS = 150;
    private static final int DEFAULT_SKILL_DELAY_MS = 90;
    private static final int DEFAULT_HURT_DELAY_MS = 90;
    private static final int POST_ATTACK_HURT_MS = 600;

    // TMX map pixel size (used to convert TMX object coordinates to panel coordinates)
    private int mapPixelWidth = screenWidth;
    private int mapPixelHeight = screenHeight;

    // Characters are loaded from assets/data/characters.json with fallback defaults.
    public static final List<CharacterDef> ALL_CHARACTERS = loadCharacterDefs();

    // Assets
    private Image backgroundImage;

    // Player (first sprite) animation state
    private List<BufferedImage> playerFrames = new ArrayList<>();
    private int playerFrameIndex = 0;
    private Timer playerTimer;

    // Enemy (second sprite, always mirrored to face the player) animation state
    private List<BufferedImage> enemyFrames = new ArrayList<>();
    private int enemyFrameIndex = 0;
    private Timer enemyTimer;

    // Skill animation state (plays once, then returns to idle)
    private List<List<BufferedImage>> playerSkillAnimations = new ArrayList<>();
    private List<List<BufferedImage>> enemySkillAnimations = new ArrayList<>();
    private List<BufferedImage> activePlayerSkillFrames = List.of();
    private List<BufferedImage> activeEnemySkillFrames = List.of();
    private int playerSkillFrameIndex = 0;
    private int enemySkillFrameIndex = 0;
    private Timer playerSkillTimer;
    private Timer enemySkillTimer;
    private boolean isPlayerSkillAnimating = false;
    private boolean isEnemySkillAnimating = false;
    private int activePlayerSkillOffsetX = 0;
    private int activeEnemySkillOffsetX = 0;

    // Hurt animation state — plays once when the target takes a damage-type skill
    private List<BufferedImage> playerHurtFrames = new ArrayList<>();
    private List<BufferedImage> enemyHurtFrames  = new ArrayList<>();
    private int playerHurtFrameIndex = 0;
    private int enemyHurtFrameIndex  = 0;
    private Timer playerHurtTimer;
    private Timer enemyHurtTimer;
    private Timer playerHurtDelayTimer;
    private Timer enemyHurtDelayTimer;
    private Timer playerHurtWindowTimer;
    private Timer enemyHurtWindowTimer;
    private boolean isPlayerHurtAnimating = false;
    private boolean isEnemyHurtAnimating = false;

    // Dead animation state — plays once when HP reaches 0, then holds on last frame
    private CharacterDef currentPlayerDef, currentEnemyDef;
    private List<BufferedImage> playerDeadFrames = new ArrayList<>();
    private List<BufferedImage> enemyDeadFrames  = new ArrayList<>();
    private int playerDeadFrameIndex = 0;
    private int enemyDeadFrameIndex  = 0;
    private Timer playerDeadTimer;
    private Timer enemyDeadTimer;

    // Sprite placement (read from map object layers when available)
    private int p1SpriteX = 350;
    private int p1SpriteY = 300;
    private int p1SpawnFeetMapX = p1SpriteX + (DEFAULT_DRAW_WIDTH / 2);
    private int p1SpawnFeetMapY = p1SpriteY + DEFAULT_DRAW_HEIGHT;
    private int p2SpriteX = screenWidth - 350 - DEFAULT_DRAW_WIDTH;
    private int p2SpriteY = 300;
    private int p2SpawnFeetMapX = p2SpriteX + (DEFAULT_DRAW_WIDTH / 2);
    private int p2SpawnFeetMapY = p2SpriteY + DEFAULT_DRAW_HEIGHT;

    // Game State
    private int p1HP = 100, p2HP = 100;
    private boolean isP1Turn = true;

    // UI Elements - These now ONLY hold buttons
    private JPanel p1ButtonPanel, p2ButtonPanel;
    private final List<JButton> p1SkillButtons = new ArrayList<>();
    private final List<JButton> p2SkillButtons = new ArrayList<>();
    private JLabel turnLabel, p1HPLabel, p2HPLabel;

    public GamePanel() {

        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setFocusable(true);
        this.addKeyListener(new KeyInputs(this));
        this.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                updateP1DrawFromSpawn();
                updateP2DrawFromSpawn();
                repositionUI();
                repaint();
            }
        });

        // Use Null Layout to place buttons at exact X/Y coordinate below the sprites
        this.setLayout(null);

        // Load map background and spawn points from TMX.
        loadMapData("/assets/maps/map1.tmx");

        // Default: player = Light Mage (index 0), enemy = Idk Magician (index 1).
        // Call setCharacters(playerIdx, enemyIdx) anytime to swap them.
        setCharacters(0, 1);

        Font boldFont = FontManager.getFont(40).deriveFont(Font.BOLD);
        Font noneBold = FontManager.getFont(22).deriveFont(Font.BOLD);

        // Turn Indicator Label
        turnLabel = new JLabel("PLAYER 1'S TURN", SwingConstants.CENTER);

        /*
        turnLabel.setFont(FontManager.getFont(40));
        turnLabel.setForeground(Color.BLACK);
         */
        turnLabel.setFont(boldFont);
        turnLabel.setBounds(0, 10, screenWidth, 50);
        this.add(turnLabel);

        // HP labels
        p1HPLabel = new JLabel("HP: 100", SwingConstants.CENTER);
        p1HPLabel.setFont(noneBold);
        p1HPLabel.setForeground(Color.black);
        this.add(p1HPLabel);

        p2HPLabel = new JLabel("HP: 100", SwingConstants.CENTER);
        p2HPLabel.setFont(noneBold);
        p2HPLabel.setForeground(Color.black);
        this.add(p2HPLabel);

        // Skill button panels
        p1ButtonPanel = createSkillUI(p1SkillButtons);
        this.add(p1ButtonPanel);

        p2ButtonPanel = createSkillUI(p2SkillButtons);
        this.add(p2ButtonPanel);

        refreshSkillButtonLabels();

        repositionUI();
        updateGameState();
    }

    private JPanel createSkillUI(List<JButton> buttonStore) {
        JPanel panel = new JPanel(new GridLayout(1, 3, 4, 4));
        panel.setOpaque(false); // Transparent so background image shows through gaps

        for (int i = 0; i < 3; i++) {
            JButton btn = new JButton("Skill " + (i + 1));
            btn.setFocusable(false);
            btn.setFont(new Font("Arial", Font.BOLD, 10));
            btn.setBackground(new Color(255, 255, 255, 180));
            btn.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

            int skillID = i + 1;
            btn.addActionListener(e -> executeSkill(skillID));
            panel.add(btn);
            buttonStore.add(btn);
        }
        return panel;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        //Background
        if (backgroundImage != null) {
            g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }

        // Player sprite (faces right)
        if (p1HP <= 0) {
            if (!playerDeadFrames.isEmpty()) {
                BufferedImage frame = playerDeadFrames.get(playerDeadFrameIndex);
                g2.drawImage(frame, p1SpriteX, p1SpriteY, getPlayerDrawWidth(), getPlayerDrawHeight(), this);
            }
        } else if (isPlayerHurtAnimating && !playerHurtFrames.isEmpty()) {
            BufferedImage frame = playerHurtFrames.get(playerHurtFrameIndex);
            g2.drawImage(frame, p1SpriteX, p1SpriteY, getPlayerDrawWidth(), getPlayerDrawHeight(), this);
        } else if (isPlayerSkillAnimating && !activePlayerSkillFrames.isEmpty()) {
            BufferedImage frame = activePlayerSkillFrames.get(playerSkillFrameIndex);
            g2.drawImage(frame, p1SpriteX + activePlayerSkillOffsetX, p1SpriteY, getPlayerDrawWidth(), getPlayerDrawHeight(), this);
        } else if (!playerFrames.isEmpty()) {
            BufferedImage frame = playerFrames.get(playerFrameIndex);
            g2.drawImage(frame, p1SpriteX, p1SpriteY, getPlayerDrawWidth(), getPlayerDrawHeight(), this);
        }

        // Enemy sprite (always mirrored so it faces the player)
        if (p2HP <= 0) {
            if (!enemyDeadFrames.isEmpty()) {
                BufferedImage frame = enemyDeadFrames.get(enemyDeadFrameIndex);
                g2.drawImage(frame, p2SpriteX + getEnemyDrawWidth(), p2SpriteY, -getEnemyDrawWidth(), getEnemyDrawHeight(), this);
            }
        } else if (isEnemyHurtAnimating && !enemyHurtFrames.isEmpty()) {
            BufferedImage frame = enemyHurtFrames.get(enemyHurtFrameIndex);
            g2.drawImage(frame, p2SpriteX + getEnemyDrawWidth(), p2SpriteY, -getEnemyDrawWidth(), getEnemyDrawHeight(), this);
        } else if (isEnemySkillAnimating && !activeEnemySkillFrames.isEmpty()) {
            BufferedImage frame = activeEnemySkillFrames.get(enemySkillFrameIndex);
            g2.drawImage(frame, p2SpriteX + getEnemyDrawWidth() + activeEnemySkillOffsetX, p2SpriteY, -getEnemyDrawWidth(), getEnemyDrawHeight(), this);
        } else if (!enemyFrames.isEmpty()) {
            BufferedImage frame = enemyFrames.get(enemyFrameIndex);
            g2.drawImage(frame, p2SpriteX + getEnemyDrawWidth(), p2SpriteY, -getEnemyDrawWidth(), getEnemyDrawHeight(), this);
        }
    }

    private void loadMapData(String tmxResourcePath) {
        try {
            URL tmxUrl = getClass().getResource(tmxResourcePath);
            if (tmxUrl == null) {
                System.err.println("TMX not found: " + tmxResourcePath);
                return;
            }

            try (InputStream stream = tmxUrl.openStream()) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(true);
            factory.setNamespaceAware(false);
            Document document = factory.newDocumentBuilder().parse(stream);
            document.getDocumentElement().normalize();

            loadMapDimensions(document);

            loadMapBackground(document, tmxResourcePath, tmxUrl);
            loadSpawnPoint(document, "spawnPlayer", true);
            loadSpawnPoint(document, "spawnPlayer2", false);
            }
        } catch (Exception e) {
            System.err.println("Failed to load TMX map data: " + e.getMessage());
        }
    }

    /**
     * Switches the player and enemy characters.
     * playerIdx / enemyIdx are indices into ALL_CHARACTERS.
     * The enemy is always drawn mirrored so it faces the player.
     * Call this anytime – from a character select screen, for example.
     */
    public void setCharacters(int playerIdx, int enemyIdx) {
        if (playerTimer != null && playerTimer.isRunning()) playerTimer.stop();
        if (enemyTimer  != null && enemyTimer.isRunning())  enemyTimer.stop();
        stopSkillAnimation(true);
        stopSkillAnimation(false);
        stopHurtTimeline(true);
        stopHurtTimeline(false);

        CharacterDef playerDef = ALL_CHARACTERS.get(playerIdx);
        CharacterDef enemyDef  = ALL_CHARACTERS.get(enemyIdx);

        currentPlayerDef = playerDef;
        currentEnemyDef  = enemyDef;

        refreshSkillButtonLabels();

        updateP1DrawFromSpawn();
        updateP2DrawFromSpawn();
        repositionUI();

        playerFrames = loadFrames(playerDef);
        enemyFrames  = loadFrames(enemyDef);
        playerSkillAnimations = loadSkillAnimations(playerDef);
        enemySkillAnimations = loadSkillAnimations(enemyDef);
        playerFrameIndex = 0;
        enemyFrameIndex  = 0;

        if (!playerFrames.isEmpty()) {
            playerTimer = new Timer(playerDef.idleAnimation.frameDelayMs, e -> {
                playerFrameIndex = (playerFrameIndex + 1) % playerFrames.size();
                repaint();
            });
            playerTimer.start();
        }

        if (!enemyFrames.isEmpty()) {
            enemyTimer = new Timer(enemyDef.idleAnimation.frameDelayMs, e -> {
                enemyFrameIndex = (enemyFrameIndex + 1) % enemyFrames.size();
                repaint();
            });
            enemyTimer.start();
        }

        if (playerDeadTimer != null) { playerDeadTimer.stop(); playerDeadTimer = null; }
        if (enemyDeadTimer  != null) { enemyDeadTimer.stop();  enemyDeadTimer  = null; }
        playerHurtFrames = loadHurtFrames(playerDef);
        enemyHurtFrames  = loadHurtFrames(enemyDef);
        playerHurtFrameIndex = 0;
        enemyHurtFrameIndex  = 0;
        playerDeadFrames = loadDeadFrames(playerDef);
        enemyDeadFrames  = loadDeadFrames(enemyDef);
        playerDeadFrameIndex = 0;
        enemyDeadFrameIndex  = 0;
        repaint();
    }

    private List<BufferedImage> loadFrames(CharacterDef def) {
        return loadAnimationFrames(def.idleAnimation);
    }

    private List<BufferedImage> loadDeadFrames(CharacterDef def) {
        return loadAnimationFrames(def.deadAnimation);
    }

    private List<BufferedImage> loadHurtFrames(CharacterDef def) {
        return loadAnimationFrames(def.hurtAnimation);
    }

    private List<List<BufferedImage>> loadSkillAnimations(CharacterDef def) {
        List<List<BufferedImage>> animations = new ArrayList<>();
        for (int skillID = 1; skillID <= 3; skillID++) {
            String skillSpritePath = def.getSkillSpritePath(skillID);
            if (skillSpritePath == null || skillSpritePath.isBlank()) {
                animations.add(List.of());
                continue;
            }
            animations.add(loadAnimationFrames(new CharacterDef.AnimationDef(
                skillSpritePath,
                DEFAULT_FRAME_SIZE,
                DEFAULT_FRAME_SIZE,
                DEFAULT_SKILL_DELAY_MS
            )));
        }
        return animations;
    }

    private List<BufferedImage> loadAnimationFrames(CharacterDef.AnimationDef animation) {
        List<BufferedImage> frames = new ArrayList<>();
        try {
            URL resource = getClass().getResource(animation.sheetPath);
            if (resource == null) {
                System.err.println("Missing sprite sheet: " + animation.sheetPath);
                return frames;
            }
            BufferedImage sheet = ImageIO.read(resource);
            if (sheet == null) {
                System.err.println("Could not decode sprite sheet: " + animation.sheetPath);
                return frames;
            }
            int columns = sheet.getWidth()  / animation.frameWidth;
            int rows    = sheet.getHeight() / animation.frameHeight;
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < columns; col++) {
                    frames.add(sheet.getSubimage(
                        col * animation.frameWidth,
                        row * animation.frameHeight,
                        animation.frameWidth,
                        animation.frameHeight
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load sprite sheet: " + e.getMessage());
        }
        return frames;
    }

    private void loadMapBackground(Document document, String tmxResourcePath, URL tmxUrl) {
        NodeList imageNodes = document.getElementsByTagName("image");
        if (imageNodes.getLength() == 0) return;

        Element imageElement = (Element) imageNodes.item(0);
        String source = imageElement.getAttribute("source");
        if (source == null || source.isEmpty()) return;

        // Prefer image layer dimensions for coordinate scaling.
        int imageWidth = parseNumber(imageElement.getAttribute("width"));
        int imageHeight = parseNumber(imageElement.getAttribute("height"));
        if (imageWidth > 0) mapPixelWidth = imageWidth;
        if (imageHeight > 0) mapPixelHeight = imageHeight;

        Image loaded = tryLoadMapImage(source, tmxResourcePath, tmxUrl);
        if (loaded != null) {
            backgroundImage = loaded;
        } else {
            System.err.println("TMX background image not found: " + source);
        }
    }

    private Image tryLoadMapImage(String source, String tmxResourcePath, URL tmxUrl) {
        // 1) Try classpath resource first.
        String resolvedResourcePath = resolveResourcePath(tmxResourcePath, source);
        URL resourceUrl = getClass().getResource(resolvedResourcePath);
        if (resourceUrl != null) {
            return new ImageIcon(resourceUrl).getImage();
        }

        // 2) Try filesystem path relative to TMX file location (for Tiled absolute/relative exports).
        try {
            Path tmxFilePath = Paths.get(tmxUrl.toURI());
            Path tmxDir = tmxFilePath.getParent();
            if (tmxDir != null) {
                Path imagePath = tmxDir.resolve(source.replace('\\', File.separatorChar)).normalize();
                if (Files.exists(imagePath)) {
                    return new ImageIcon(imagePath.toString()).getImage();
                }
            }
        } catch (Exception ignored) {
            // Fall through to null when URL cannot be converted to local file path.
        }

        return null;
    }

    private void loadSpawnPoint(Document document, String groupName, boolean isPlayerOne) {
        NodeList groups = document.getElementsByTagName("objectgroup");
        for (int i = 0; i < groups.getLength(); i++) {
            Node node = groups.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;

            Element group = (Element) node;
            if (!groupName.equals(group.getAttribute("name"))) continue;

            int offsetX = parseNumber(group.getAttribute("offsetx"));
            int offsetY = parseNumber(group.getAttribute("offsety"));

            NodeList objects = group.getElementsByTagName("object");
            if (objects.getLength() == 0) return;

            Element object = (Element) objects.item(0);
            int spawnX = parseNumber(object.getAttribute("x")) + offsetX;
            int spawnY = parseNumber(object.getAttribute("y")) + offsetY;

            if (isPlayerOne) {
                p1SpawnFeetMapX = spawnX;
                p1SpawnFeetMapY = spawnY;
                updateP1DrawFromSpawn();
            } else {
                p2SpawnFeetMapX = spawnX;
                p2SpawnFeetMapY = spawnY;
                updateP2DrawFromSpawn();
            }
            return;
        }
    }

    private void loadMapDimensions(Document document) {
        Element map = document.getDocumentElement();
        if (map == null || !"map".equals(map.getTagName())) return;

        int widthInTiles = parseNumber(map.getAttribute("width"));
        int heightInTiles = parseNumber(map.getAttribute("height"));
        int tileWidth = parseNumber(map.getAttribute("tilewidth"));
        int tileHeight = parseNumber(map.getAttribute("tileheight"));

        if (widthInTiles > 0 && tileWidth > 0) {
            mapPixelWidth = widthInTiles * tileWidth;
        }
        if (heightInTiles > 0 && tileHeight > 0) {
            mapPixelHeight = heightInTiles * tileHeight;
        }
    }

    private int scaleMapXToPanel(int mapX) {
        if (mapPixelWidth <= 0) return mapX;
        int panelWidth = Math.max(getWidth(), screenWidth);
        double scaleX = (double) panelWidth / mapPixelWidth;
        return (int) Math.round(mapX * scaleX);
    }

    private int scaleMapYToPanel(int mapY) {
        if (mapPixelHeight <= 0) return mapY;
        int panelHeight = Math.max(getHeight(), screenHeight);
        double scaleY = (double) panelHeight / mapPixelHeight;
        return (int) Math.round(mapY * scaleY);
    }

    private void updateP1DrawFromSpawn() {
        int feetX = scaleMapXToPanel(p1SpawnFeetMapX);
        int feetY = scaleMapYToPanel(p1SpawnFeetMapY);
        p1SpriteX = feetX - (getPlayerDrawWidth() / 2);
        p1SpriteY = feetY - getPlayerDrawHeight();
    }

    private void updateP2DrawFromSpawn() {
        int feetX = scaleMapXToPanel(p2SpawnFeetMapX);
        int feetY = scaleMapYToPanel(p2SpawnFeetMapY);
        p2SpriteX = feetX - (getEnemyDrawWidth() / 2);
        p2SpriteY = feetY - getEnemyDrawHeight();
    }

    private void repositionUI() {
        int feetX1 = p1SpriteX + getPlayerDrawWidth() / 2;
        int feetY1 = p1SpriteY + getPlayerDrawHeight();
        int feetX2 = p2SpriteX + getEnemyDrawWidth() / 2;
        int feetY2 = p2SpriteY + getEnemyDrawHeight();
        int labelW = 160, labelH = 30, btnW = tileSize * 2, btnH = 40, gap = 5;
        if (p1HPLabel    != null) p1HPLabel   .setBounds(feetX1 - labelW / 2, feetY1 + gap,                   labelW, labelH);
        if (p2HPLabel    != null) p2HPLabel   .setBounds(feetX2 - labelW / 2, feetY2 + gap,                   labelW, labelH);
        if (p1ButtonPanel != null) p1ButtonPanel.setBounds(feetX1 - btnW / 2, feetY1 + gap + labelH + gap, btnW, btnH);
        if (p2ButtonPanel != null) p2ButtonPanel.setBounds(feetX2 - btnW / 2, feetY2 + gap + labelH + gap, btnW, btnH);
    }

    private void refreshSkillButtonLabels() {
        applySkillNamesToButtons(currentPlayerDef, p1SkillButtons);
        applySkillNamesToButtons(currentEnemyDef, p2SkillButtons);
    }

    private void applySkillNamesToButtons(CharacterDef character, List<JButton> buttons) {
        if (character == null || buttons.isEmpty()) return;
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setText(character.getSkillName(i + 1));
        }
    }

    private int getPlayerDrawWidth() {
        return currentPlayerDef != null ? currentPlayerDef.drawWidth : DEFAULT_DRAW_WIDTH;
    }

    private int getPlayerDrawHeight() {
        return currentPlayerDef != null ? currentPlayerDef.drawHeight : DEFAULT_DRAW_HEIGHT;
    }

    private int getEnemyDrawWidth() {
        return currentEnemyDef != null ? currentEnemyDef.drawWidth : DEFAULT_DRAW_WIDTH;
    }

    private int getEnemyDrawHeight() {
        return currentEnemyDef != null ? currentEnemyDef.drawHeight : DEFAULT_DRAW_HEIGHT;
    }

    private int parseNumber(String value) {
        if (value == null || value.isBlank()) return 0;
        return (int) Math.round(Double.parseDouble(value));
    }

    private String resolveResourcePath(String basePath, String relativePath) {
        String normalizedRelative = relativePath.replace('\\', '/');
        Path base = Paths.get(basePath).getParent();
        Path resolved = (base == null ? Paths.get(normalizedRelative) : base.resolve(normalizedRelative)).normalize();
        String resourcePath = resolved.toString().replace('\\', '/');
        return resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
    }

    public void executeSkill(int skillID) {
        if (p1HP <= 0 || p2HP <= 0) return;

        boolean actingPlayerOne = isP1Turn;
        CharacterDef actor = actingPlayerOne ? currentPlayerDef : currentEnemyDef;
        boolean isDamageSkill = actor != null && "damage".equalsIgnoreCase(actor.getSkillType(skillID));

        if (isP1Turn) {
            switch (skillID) {
                case 1 -> p2HP = Math.max(0, p2HP - 10);
                case 2 -> p1HP = Math.min(100, p1HP + 10);
                case 3 -> p2HP = Math.max(0, p2HP - 25);
            }
        } else {
            switch (skillID) {
                case 1 -> p1HP = Math.max(0, p1HP - 10);
                case 2 -> p2HP = Math.min(100, p2HP + 10);
                case 3 -> p1HP = Math.max(0, p1HP - 25);
            }
        }

        if (isDamageSkill) {
            scheduleHurtTimeline(actingPlayerOne, skillID);
        }
        playSkillAnimation(actingPlayerOne, skillID, null);

        isP1Turn = !isP1Turn;
        updateGameState();
        repaint();
    }

    private void playSkillAnimation(boolean isPlayerOne, int skillID, Runnable onCastFinished) {
        if (skillID < 1 || skillID > 3) return;
        Runnable callback = onCastFinished != null ? onCastFinished : () -> {};

        List<List<BufferedImage>> source = isPlayerOne ? playerSkillAnimations : enemySkillAnimations;
        if (source.isEmpty() || source.size() < skillID) {
            callback.run();
            return;
        }

        List<BufferedImage> frames = source.get(skillID - 1);
        if (frames == null || frames.isEmpty()) {
            callback.run();
            return;
        }

        if (isPlayerOne) {
            stopSkillAnimation(true);
            activePlayerSkillFrames = frames;
            playerSkillFrameIndex = 0;
            isPlayerSkillAnimating = true;
            activePlayerSkillOffsetX = currentPlayerDef != null ? currentPlayerDef.getSkillForwardOffsetX(skillID) : 0;

            playerSkillTimer = new Timer(DEFAULT_SKILL_DELAY_MS, null);
            playerSkillTimer.addActionListener(e -> {
                if (playerSkillFrameIndex < activePlayerSkillFrames.size() - 1) {
                    playerSkillFrameIndex++;
                } else {
                    stopSkillAnimation(true);
                    callback.run();
                }
                repaint();
            });
            playerSkillTimer.start();
        } else {
            stopSkillAnimation(false);
            activeEnemySkillFrames = frames;
            enemySkillFrameIndex = 0;
            isEnemySkillAnimating = true;
            int forwardOffset = currentEnemyDef != null ? currentEnemyDef.getSkillForwardOffsetX(skillID) : 0;
            activeEnemySkillOffsetX = -forwardOffset;

            enemySkillTimer = new Timer(DEFAULT_SKILL_DELAY_MS, null);
            enemySkillTimer.addActionListener(e -> {
                if (enemySkillFrameIndex < activeEnemySkillFrames.size() - 1) {
                    enemySkillFrameIndex++;
                } else {
                    stopSkillAnimation(false);
                    callback.run();
                }
                repaint();
            });
            enemySkillTimer.start();
        }
    }

    private void stopSkillAnimation(boolean isPlayerOne) {
        if (isPlayerOne) {
            if (playerSkillTimer != null) {
                playerSkillTimer.stop();
                playerSkillTimer = null;
            }
            isPlayerSkillAnimating = false;
            playerSkillFrameIndex = 0;
            activePlayerSkillOffsetX = 0;
            activePlayerSkillFrames = List.of();
        } else {
            if (enemySkillTimer != null) {
                enemySkillTimer.stop();
                enemySkillTimer = null;
            }
            isEnemySkillAnimating = false;
            enemySkillFrameIndex = 0;
            activeEnemySkillOffsetX = 0;
            activeEnemySkillFrames = List.of();
        }
    }

    private void scheduleHurtTimeline(boolean attackerIsPlayerOne, int skillID) {
        CharacterDef attacker = attackerIsPlayerOne ? currentPlayerDef : currentEnemyDef;
        if (attacker == null) return;

        int castDurationMs = getSkillCastDurationMs(attackerIsPlayerOne, skillID);
        int configuredBufferMs = (int) Math.round(attacker.getSkillHurtTriggerBufferSeconds(skillID) * 1000.0);
        int effectiveBufferMs = Math.max(0, Math.min(configuredBufferMs, castDurationMs));
        int hurtDurationMs = Math.max(1, (castDurationMs - effectiveBufferMs) + POST_ATTACK_HURT_MS);

        startDelayedHurt(attackerIsPlayerOne ? false : true, effectiveBufferMs, hurtDurationMs);
    }

    private int getSkillCastDurationMs(boolean attackerIsPlayerOne, int skillID) {
        List<List<BufferedImage>> source = attackerIsPlayerOne ? playerSkillAnimations : enemySkillAnimations;
        if (skillID < 1 || source.isEmpty() || source.size() < skillID) {
            return DEFAULT_SKILL_DELAY_MS;
        }
        List<BufferedImage> frames = source.get(skillID - 1);
        int frameCount = (frames == null || frames.isEmpty()) ? 1 : frames.size();
        return frameCount * DEFAULT_SKILL_DELAY_MS;
    }

    private void startDelayedHurt(boolean targetIsPlayer, int delayMs, int durationMs) {
        stopHurtTimeline(targetIsPlayer);

        Timer delayTimer = new Timer(delayMs, null);
        delayTimer.setRepeats(false);
        delayTimer.addActionListener(e -> startTimedHurt(targetIsPlayer, durationMs));
        delayTimer.start();

        if (targetIsPlayer) {
            playerHurtDelayTimer = delayTimer;
        } else {
            enemyHurtDelayTimer = delayTimer;
        }
    }

    private void startTimedHurt(boolean isPlayer, int durationMs) {
        if (isPlayer) {
            if (p1HP <= 0 || playerHurtFrames.isEmpty()) return;
            if (playerHurtTimer != null) playerHurtTimer.stop();
            if (playerHurtWindowTimer != null) playerHurtWindowTimer.stop();

            isPlayerHurtAnimating = true;
            playerHurtFrameIndex = 0;
            playerHurtTimer = new Timer(DEFAULT_HURT_DELAY_MS, null);
            playerHurtTimer.addActionListener(e -> {
                if (!playerHurtFrames.isEmpty()) {
                    playerHurtFrameIndex = (playerHurtFrameIndex + 1) % playerHurtFrames.size();
                }
                repaint();
            });
            playerHurtTimer.start();

            playerHurtWindowTimer = new Timer(durationMs, null);
            playerHurtWindowTimer.setRepeats(false);
            playerHurtWindowTimer.addActionListener(e -> stopHurtTimeline(true));
            playerHurtWindowTimer.start();
        } else {
            if (p2HP <= 0 || enemyHurtFrames.isEmpty()) return;
            if (enemyHurtTimer != null) enemyHurtTimer.stop();
            if (enemyHurtWindowTimer != null) enemyHurtWindowTimer.stop();

            isEnemyHurtAnimating = true;
            enemyHurtFrameIndex = 0;
            enemyHurtTimer = new Timer(DEFAULT_HURT_DELAY_MS, null);
            enemyHurtTimer.addActionListener(e -> {
                if (!enemyHurtFrames.isEmpty()) {
                    enemyHurtFrameIndex = (enemyHurtFrameIndex + 1) % enemyHurtFrames.size();
                }
                repaint();
            });
            enemyHurtTimer.start();

            enemyHurtWindowTimer = new Timer(durationMs, null);
            enemyHurtWindowTimer.setRepeats(false);
            enemyHurtWindowTimer.addActionListener(e -> stopHurtTimeline(false));
            enemyHurtWindowTimer.start();
        }
    }

    private void stopHurtTimeline(boolean isPlayer) {
        if (isPlayer) {
            if (playerHurtDelayTimer != null) {
                playerHurtDelayTimer.stop();
                playerHurtDelayTimer = null;
            }
            if (playerHurtWindowTimer != null) {
                playerHurtWindowTimer.stop();
                playerHurtWindowTimer = null;
            }
            if (playerHurtTimer != null) {
                playerHurtTimer.stop();
                playerHurtTimer = null;
            }
            isPlayerHurtAnimating = false;
            playerHurtFrameIndex = 0;
        } else {
            if (enemyHurtDelayTimer != null) {
                enemyHurtDelayTimer.stop();
                enemyHurtDelayTimer = null;
            }
            if (enemyHurtWindowTimer != null) {
                enemyHurtWindowTimer.stop();
                enemyHurtWindowTimer = null;
            }
            if (enemyHurtTimer != null) {
                enemyHurtTimer.stop();
                enemyHurtTimer = null;
            }
            isEnemyHurtAnimating = false;
            enemyHurtFrameIndex = 0;
        }
    }

    private void updateGameState() {
        p1HPLabel.setText("HP: " + p1HP);
        p2HPLabel.setText("HP: " + p2HP);

        if (p1HP <= 0 && playerDeadTimer == null) startDeadAnimation(true);
        if (p2HP <= 0 && enemyDeadTimer  == null) startDeadAnimation(false);

        if (p1HP <= 0 || p2HP <= 0) {
            turnLabel.setText(p1HP <= 0 ? "PLAYER 2 WINS!" : "PLAYER 1 WINS!");
            p1ButtonPanel.setVisible(false);
            p2ButtonPanel.setVisible(false);
        } else {
            turnLabel.setText(isP1Turn ? "PLAYER 1'S TURN" : "PLAYER 2'S TURN");

            // Toggle visibility of BUTTONS only
            p1ButtonPanel.setVisible(isP1Turn);
            p2ButtonPanel.setVisible(!isP1Turn);
        }
    }

    private void startDeadAnimation(boolean isPlayer) {
        if (isPlayer) {
            if (playerTimer != null) playerTimer.stop();
            stopSkillAnimation(true);
            stopHurtTimeline(true);
            if (playerDeadFrames.isEmpty()) return;
            playerDeadFrameIndex = 0;
            int delay = currentPlayerDef != null ? currentPlayerDef.deadAnimation.frameDelayMs : 150;
            playerDeadTimer = new Timer(delay, null);
            playerDeadTimer.addActionListener(e -> {
                if (playerDeadFrameIndex < playerDeadFrames.size() - 1) {
                    playerDeadFrameIndex++;
                } else {
                    playerDeadTimer.stop();
                }
                repaint();
            });
            playerDeadTimer.start();
        } else {
            if (enemyTimer != null) enemyTimer.stop();
            stopSkillAnimation(false);
            stopHurtTimeline(false);
            if (enemyDeadFrames.isEmpty()) return;
            enemyDeadFrameIndex = 0;
            int delay = currentEnemyDef != null ? currentEnemyDef.deadAnimation.frameDelayMs : 150;
            enemyDeadTimer = new Timer(delay, null);
            enemyDeadTimer.addActionListener(e -> {
                if (enemyDeadFrameIndex < enemyDeadFrames.size() - 1) {
                    enemyDeadFrameIndex++;
                } else {
                    enemyDeadTimer.stop();
                }
                repaint();
            });
            enemyDeadTimer.start();
        }
    }

    private static List<CharacterDef> loadCharacterDefs() {
        List<CharacterDataLoader.CharacterConfig> configs = CharacterDataLoader.loadCharacterConfigs("/assets/data/characters.json");
        List<CharacterDef> defs = new ArrayList<>();

        for (CharacterDataLoader.CharacterConfig config : configs) {
            defs.add(new CharacterDef(
                config.name,
                config.skill1Name,
                config.skill2Name,
                config.skill3Name,
                config.skill1Type,
                config.skill2Type,
                config.skill3Type,
                config.skill1SpritePath,
                config.skill2SpritePath,
                config.skill3SpritePath,
                config.skill1ForwardOffsetX,
                config.skill2ForwardOffsetX,
                config.skill3ForwardOffsetX,
                config.skill1HurtTriggerBufferSeconds,
                config.skill2HurtTriggerBufferSeconds,
                config.skill3HurtTriggerBufferSeconds,
                new CharacterDef.AnimationDef(config.idleSpritePath, DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_IDLE_DELAY_MS),
                new CharacterDef.AnimationDef(config.hurtSpritePath, DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_HURT_DELAY_MS),
                new CharacterDef.AnimationDef(config.deathSpritePath, DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_DEAD_DELAY_MS),
                DEFAULT_DRAW_WIDTH,
                DEFAULT_DRAW_HEIGHT
            ));
        }

        if (!defs.isEmpty()) {
            return List.copyOf(defs);
        }

        return List.of(
            new CharacterDef(
                "Light Mage",
                "Light Sword",
                "Halo of Aegis",
                "Dawn Piercer",
                "damage",
                "defense",
                "damage",
                "/assets/spritesheet/Light Mage/LightSword-Sheet.png",
                "/assets/spritesheet/Light Mage/HaloOfAegis-Sheet.png",
                "/assets/spritesheet/Light Mage/DawnPiercer-Sheet.png",
                0,
                0,
                0,
                0.0,
                0.0,
                0.0,
                new CharacterDef.AnimationDef("/assets/spritesheet/Light Mage/Idle-Sheet.png", DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_IDLE_DELAY_MS),
                new CharacterDef.AnimationDef("/assets/spritesheet/Light Mage/Hurt-Sheet.png", DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_HURT_DELAY_MS),
                new CharacterDef.AnimationDef("/assets/spritesheet/Light Mage/Dead-Sheet.png", DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_DEAD_DELAY_MS),
                DEFAULT_DRAW_WIDTH,
                DEFAULT_DRAW_HEIGHT
            ),
            new CharacterDef(
                "Idk Magician",
                "Magic Arrow",
                "Arcane Charge",
                "Magic Sphere",
                "damage",
                "defense",
                "debuff",
                "/assets/spritesheet/Idk Magician/Magic_arrow.png",
                "/assets/spritesheet/Idk Magician/Charge_1.png",
                "/assets/spritesheet/Idk Magician/Magic_sphere.png",
                0,
                0,
                0,
                0.0,
                0.0,
                0.0,
                new CharacterDef.AnimationDef("/assets/spritesheet/Idk Magician/Idle.png", DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_IDLE_DELAY_MS),
                new CharacterDef.AnimationDef("/assets/spritesheet/Idk Magician/Hurt.png", DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_HURT_DELAY_MS),
                new CharacterDef.AnimationDef("/assets/spritesheet/Idk Magician/Dead.png", DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_DEAD_DELAY_MS),
                DEFAULT_DRAW_WIDTH,
                DEFAULT_DRAW_HEIGHT
            )
        );
    }
}
