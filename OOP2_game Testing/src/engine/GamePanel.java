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

    // TMX map pixel size (used to convert TMX object coordinates to panel coordinates)
    private int mapPixelWidth = screenWidth;
    private int mapPixelHeight = screenHeight;

    // All available characters – add new CharacterDef entries here to register more.
    public static final List<CharacterDef> ALL_CHARACTERS = List.of(
        new CharacterDef(
            "Light Mage",
            new CharacterDef.AnimationDef("/assets/spritesheet/Light Mage/Idle-Sheet.png", 128, 128, 120),
            new CharacterDef.AnimationDef("/assets/spritesheet/Light Mage/Dead-Sheet.png", 128, 128, 150),
            384,
            384
        ),
        new CharacterDef(
            "Idk Magician",
            new CharacterDef.AnimationDef("/assets/spritesheet/Idk Magician/Idle.png", 128, 128, 120),
            new CharacterDef.AnimationDef("/assets/spritesheet/Idk Magician/Dead.png", 128, 128, 150),
            384,
            384
        )
    );

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

        // Turn Indicator Label
        turnLabel = new JLabel("PLAYER 1'S TURN", SwingConstants.CENTER);
        turnLabel.setFont(new Font("Monospaced", Font.BOLD, 40));
        turnLabel.setForeground(Color.BLACK);
        turnLabel.setBounds(0, 10, screenWidth, 50);
        this.add(turnLabel);

        // HP labels
        p1HPLabel = new JLabel("HP: 100", SwingConstants.CENTER);
        p1HPLabel.setFont(new Font("Arial", Font.BOLD, 22));
        p1HPLabel.setForeground(Color.black);
        this.add(p1HPLabel);

        p2HPLabel = new JLabel("HP: 100", SwingConstants.CENTER);
        p2HPLabel.setFont(new Font("Arial", Font.BOLD, 22));
        p2HPLabel.setForeground(Color.black);
        this.add(p2HPLabel);

        // Skill button panels
        p1ButtonPanel = createSkillUI();
        this.add(p1ButtonPanel);

        p2ButtonPanel = createSkillUI();
        this.add(p2ButtonPanel);

        repositionUI();
        updateGameState();
    }

    private JPanel createSkillUI() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 4, 4));
        panel.setOpaque(false); // Transparent so background image shows through gaps

        String[] skills = {"Atk", "Heal", "S.Atk", "S.Heal"};
        for (int i = 0; i < 4; i++) {
            JButton btn = new JButton(skills[i]);
            btn.setFocusable(false);
            btn.setFont(new Font("Arial", Font.BOLD, 10));
            btn.setBackground(new Color(255, 255, 255, 180));
            btn.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

            int skillID = i + 1;
            btn.addActionListener(e -> executeSkill(skillID));
            panel.add(btn);
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

        CharacterDef playerDef = ALL_CHARACTERS.get(playerIdx);
        CharacterDef enemyDef  = ALL_CHARACTERS.get(enemyIdx);

        currentPlayerDef = playerDef;
        currentEnemyDef  = enemyDef;

        updateP1DrawFromSpawn();
        updateP2DrawFromSpawn();
        repositionUI();

        playerFrames = loadFrames(playerDef);
        enemyFrames  = loadFrames(enemyDef);
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
        int labelW = 160, labelH = 30, btnW = tileSize, btnH = 80, gap = 5;
        if (p1HPLabel    != null) p1HPLabel   .setBounds(feetX1 - labelW / 2, feetY1 + gap,                   labelW, labelH);
        if (p2HPLabel    != null) p2HPLabel   .setBounds(feetX2 - labelW / 2, feetY2 + gap,                   labelW, labelH);
        if (p1ButtonPanel != null) p1ButtonPanel.setBounds(feetX1 - btnW / 2, feetY1 + gap + labelH + gap, btnW, btnH);
        if (p2ButtonPanel != null) p2ButtonPanel.setBounds(feetX2 - btnW / 2, feetY2 + gap + labelH + gap, btnW, btnH);
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

        if (isP1Turn) {
            switch (skillID) {
                case 1 -> p2HP = Math.max(0, p2HP - 10);
                case 2 -> p1HP = Math.min(100, p1HP + 10);
                case 3 -> p2HP = Math.max(0, p2HP - 25);
                case 4 -> p1HP = Math.min(100, p1HP + 30);
            }
        } else {
            switch (skillID) {
                case 1 -> p1HP = Math.max(0, p1HP - 10);
                case 2 -> p2HP = Math.min(100, p2HP + 10);
                case 3 -> p1HP = Math.max(0, p1HP - 25);
                case 4 -> p2HP = Math.min(100, p2HP + 30);
            }
        }

        isP1Turn = !isP1Turn;
        updateGameState();
        repaint();
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
}
