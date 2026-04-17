package engine;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;

import assets.Utility.BackButton;
import assets.Utility.FontManager;
import assets.Utility.GameBar;
import assets.Utility.RoundManager;
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
    private static final int BARS_TOP_Y = 84;
    private static final int WIND_SKILL2_P2_LEFT_NUDGE_X = 400;
    private static final int WIND_SKILL3_FEET_OFFSET_X = 80;
    private static final int WIND_SKILL3_FEET_OFFSET_Y = 0;
    private static final double WIND_SKILL3_SCALE = 2.2;
    private static final int DARK_WIZARD_PROJECTILE_DRAW_SIZE = 144;
    private static final int DARK_WIZARD_PROJECTILE_VERTICAL_OFFSET = 50;
    private static final int DARK_WIZARD_PROJECTILE_SPEED = 44;

    private static final int MAX_MP            = 100;
    private static final int MP_REGEN_PER_TURN = 10;
    /** MP costs: index 0 = skill 1, 1 = skill 2, 2 = skill 3 */
    private static final int[] SKILL_MP_COST = { 10, 20, 30 };


    private static final int TURN_TIME_SECONDS = 10;
    private static final int TIMER_WARN_THRESHOLD = 3;

    private int mapPixelWidth = screenWidth;
    private int mapPixelHeight = screenHeight;

    public static final List<CharacterDef> ALL_CHARACTERS = loadCharacterDefs();

    // Assets
    private Image backgroundImage;

    // Player animation state
    private List<BufferedImage> playerFrames = new ArrayList<>();
    private int playerFrameIndex = 0;
    private Timer playerTimer;

    // Enemy animation state
    private List<BufferedImage> enemyFrames = new ArrayList<>();
    private int enemyFrameIndex = 0;
    private Timer enemyTimer;

    // Skill animation state
    private List<List<BufferedImage>> playerSkillAnimations = new ArrayList<>();
    private List<List<BufferedImage>> enemySkillAnimations = new ArrayList<>();
    private List<BufferedImage> activePlayerSkillFrames = List.of();
    private List<BufferedImage> activeEnemySkillFrames = List.of();
    private int playerSkillFrameIndex = 0;
    private int enemySkillFrameIndex = 0;
    private int activePlayerSkillID = 0;
    private int activeEnemySkillID = 0;
    private Timer playerSkillTimer;
    private Timer enemySkillTimer;
    private boolean isPlayerSkillAnimating = false;
    private boolean isEnemySkillAnimating = false;
    private int activePlayerSkillOffsetX = 0;
    private int activeEnemySkillOffsetX = 0;

    // Projectile animation state
    private final Map<String, List<BufferedImage>> projectileAnimationCache = new HashMap<>();
    private List<BufferedImage> activeProjectileFrames = List.of();
    private int projectileFrameIndex = 0;
    private int projectileX = 0;
    private int projectileY = 0;
    private int projectileDirection = 1;
    private boolean projectileIsPlayerOne = true;
    private Timer projectileTimer;
    private boolean isProjectileAnimating = false;

    // Hurt animation state
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

    // Dead animation state
    private CharacterDef currentPlayerDef, currentEnemyDef;
    private List<BufferedImage> playerDeadFrames = new ArrayList<>();
    private List<BufferedImage> enemyDeadFrames  = new ArrayList<>();
    private int playerDeadFrameIndex = 0;
    private int enemyDeadFrameIndex  = 0;
    private Timer playerDeadTimer;
    private Timer enemyDeadTimer;

    // Sprite placement
    private int p1SpriteX = 350;
    private int p1SpriteY = 300;
    private int p1SpawnFeetMapX = p1SpriteX + (DEFAULT_DRAW_WIDTH / 2);
    private int p1SpawnFeetMapY = p1SpriteY + DEFAULT_DRAW_HEIGHT;
    private int p2SpriteX = screenWidth - 350 - DEFAULT_DRAW_WIDTH;
    private int p2SpriteY = 300;
    private int p2SpawnFeetMapX = p2SpriteX + (DEFAULT_DRAW_WIDTH / 2);
    private int p2SpawnFeetMapY = p2SpriteY + DEFAULT_DRAW_HEIGHT;

    private int p1HP = 100, p2HP = 100;
    private int p1MP = MAX_MP, p2MP = MAX_MP;
    private boolean isP1Turn = true;

    private int    turnSecondsLeft = TURN_TIME_SECONDS;
    private Timer  countdownTimer;
    private JLabel countdownLabel;

    // UI Elements
    private JPanel p1ButtonPanel, p2ButtonPanel;
    private final List<JButton> p1SkillButtons = new ArrayList<>();
    private final List<JButton> p2SkillButtons = new ArrayList<>();
    private JLabel turnLabel, p1HPLabel, p2HPLabel;

    // HP bars
    private GameBar p1HealthBar, p2HealthBar;
    // MP bars
    private GameBar p1MpBar, p2MpBar;

    int barW = 220;
    int barH = 18;

    private GameWindow window;
    private RoundManager roundManager;

    public GamePanel() {
        this(new GameWindow(), 0, 1);
    }

    public GamePanel(GameWindow window, int playerCharacterIndex, int enemyCharacterIndex) {

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

        this.window = window;
        this.setLayout(null);

        JButton backBtn = new BackButton().createBackButton(window, this);
        add(backBtn);

        loadMapData("/assets/maps/map1.tmx");

        int safePlayerIndex = sanitizeCharacterIndex(playerCharacterIndex, 0);
        int safeEnemyIndex = sanitizeCharacterIndex(enemyCharacterIndex, safePlayerIndex == 0 ? 1 : 0);
        setCharacters(safePlayerIndex, safeEnemyIndex);

        Font boldFont = FontManager.getFont(40).deriveFont(Font.BOLD);
        Font noneBold = FontManager.getFont(22).deriveFont(Font.BOLD);

        // Turn Indicator Label
        turnLabel = new JLabel("PLAYER 1'S TURN", SwingConstants.CENTER);
        turnLabel.setFont(boldFont);
        turnLabel.setBounds(0, 10, screenWidth, 50);
        this.add(turnLabel);

        // HP labels — kept invisible; values are shown inside the bars
        p1HPLabel = new JLabel("", SwingConstants.CENTER);
        p1HPLabel.setFont(noneBold);
        p1HPLabel.setForeground(Color.black);
        p1HPLabel.setVisible(false);
        this.add(p1HPLabel);

        p2HPLabel = new JLabel("", SwingConstants.CENTER);
        p2HPLabel.setFont(noneBold);
        p2HPLabel.setForeground(Color.black);
        p2HPLabel.setVisible(false);
        this.add(p2HPLabel);

        // Skill button panels
        p1ButtonPanel = createSkillUI(p1SkillButtons);
        this.add(p1ButtonPanel);

        p2ButtonPanel = createSkillUI(p2SkillButtons);
        this.add(p2ButtonPanel);

        // ── HP bars ───────────────────────────────────────────────────────────
        p1HealthBar = new GameBar(100, Color.GREEN, GameBar.BarType.HP);
        p2HealthBar = new GameBar(100, Color.RED,   GameBar.BarType.HP);
        this.add(p1HealthBar);
        this.add(p2HealthBar);

        // ── MP bars ───────────────────────────────────────────────────────────
        p1MpBar = new GameBar(MAX_MP, new Color(80, 160, 255), GameBar.BarType.MP);
        p2MpBar = new GameBar(MAX_MP, new Color(80, 160, 255), GameBar.BarType.MP);
        this.add(p1MpBar);
        this.add(p2MpBar);

        // ── Countdown label ───────────────────────────────────────────────────
        countdownLabel = new JLabel("10", SwingConstants.CENTER);
        countdownLabel.setFont(FontManager.getFont(32).deriveFont(Font.BOLD));
        countdownLabel.setForeground(Color.WHITE);
        countdownLabel.setOpaque(true);
        countdownLabel.setBackground(new Color(40, 40, 40, 180));
        countdownLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
        this.add(countdownLabel);

        roundManager = new RoundManager(
                GameMode.PVP,

                // ROUND START
                (round, p1Wins, p2Wins) -> {
                    System.out.println("Round " + round + " Start!");

                    p1HP = 100;
                    p2HP = 100;
                    // Restore MP fully at the start of every round
                    p1MP = MAX_MP;
                    p2MP = MAX_MP;

                    isP1Turn = true;
                    updateGameState();
                    repaint();
                },

                // ROUND END
                (p1WonRound, round) -> {
                    String msg = p1WonRound ? "PLAYER 1 WINS ROUND " : "PLAYER 2 WINS ROUND ";
                    turnLabel.setText(msg + round);

                    new Timer(2000, e -> {
                        ((Timer) e.getSource()).stop();
                        roundManager.advanceRound();
                    }).start();
                },

                // MATCH END
                (p1WonMatch, p1Wins, p2Wins, totalRounds) -> {
                    stopTurnTimer();

                    String msg = p1WonMatch ? "PLAYER 1 WINS MATCH!" : "PLAYER 2 WINS MATCH!";
                    turnLabel.setText(msg);

                    p1ButtonPanel.setVisible(false);
                    p2ButtonPanel.setVisible(false);
                });

        refreshSkillButtonLabels();
        repositionUI();
        updateGameState();
        roundManager.startMatch();
    }

    @Override
    public void doLayout() {
        super.doLayout();
        for (Component c : getComponents()) {
            if (c instanceof JButton && ((JButton) c).getText().equals("Back")) {
                c.setBounds(getWidth() - 120, getHeight() - 70, 100, 40);
            }
        }
    }

    private static int mpCostFor(int skillID) {
        if (skillID < 1 || skillID > SKILL_MP_COST.length) return 0;
        return SKILL_MP_COST[skillID - 1];
    }

    private boolean hasEnoughMP(boolean actingPlayerOne, int skillID) {
        int available = actingPlayerOne ? p1MP : p2MP;
        return available >= mpCostFor(skillID);
    }

    /**
     * Deduct MP from the acting player and regenerate MP for the waiting player.
     */
    private void spendAndRegenMP(boolean actingPlayerOne, int skillID) {
        int cost = mpCostFor(skillID);
        if (actingPlayerOne) {
            p1MP = Math.max(0, p1MP - cost);
            p2MP = Math.min(MAX_MP, p2MP + MP_REGEN_PER_TURN);
        } else {
            p2MP = Math.max(0, p2MP - cost);
            p1MP = Math.min(MAX_MP, p1MP + MP_REGEN_PER_TURN);
        }
    }

    /** Grey out skill buttons the active player can't currently afford. */
    private void refreshSkillButtonMPState() {
        boolean actingP1 = isP1Turn;
        List<JButton> activeButtons = actingP1 ? p1SkillButtons : p2SkillButtons;
        int currentMP = actingP1 ? p1MP : p2MP;

        for (int i = 0; i < activeButtons.size(); i++) {
            int cost = mpCostFor(i + 1);
            boolean canAfford = currentMP >= cost;
            activeButtons.get(i).setEnabled(canAfford);
            activeButtons.get(i).setForeground(canAfford ? Color.BLACK : new Color(140, 140, 140));
        }
    }

    private void startTurnTimer() {
        stopTurnTimer();

        turnSecondsLeft = TURN_TIME_SECONDS;
        refreshCountdownLabel();

        countdownTimer = new Timer(1000, null);
        countdownTimer.addActionListener(e -> {
            turnSecondsLeft--;
            refreshCountdownLabel();
            if (turnSecondsLeft <= 0) skipTurn();
        });
        countdownTimer.start();
    }

    private void stopTurnTimer() {
        if (countdownTimer != null) {
            countdownTimer.stop();
            countdownTimer = null;
        }
    }

    private void skipTurn() {
        stopTurnTimer();
        if (p1HP <= 0 || p2HP <= 0) return;

        String skippedPlayer = isP1Turn ? "PLAYER 1" : "PLAYER 2";
        // Still grant regen to the player whose turn was skipped
        if (isP1Turn) p1MP = Math.min(MAX_MP, p1MP + MP_REGEN_PER_TURN);
        else          p2MP = Math.min(MAX_MP, p2MP + MP_REGEN_PER_TURN);

        isP1Turn = !isP1Turn;
        updateGameState();

        turnLabel.setText(skippedPlayer + " took too long — turn skipped!");
        repaint();

        Timer restoreLabel = new Timer(1500, e2 -> {
            if (p1HP > 0 && p2HP > 0) {
                turnLabel.setText(isP1Turn ? "PLAYER 1'S TURN" : "PLAYER 2'S TURN");
                repaint();
            }
        });
        restoreLabel.setRepeats(false);
        restoreLabel.start();
    }

    private void refreshCountdownLabel() {
        if (countdownLabel == null) return;
        countdownLabel.setText(String.valueOf(Math.max(0, turnSecondsLeft)));
        boolean urgent = turnSecondsLeft <= TIMER_WARN_THRESHOLD;
        countdownLabel.setForeground(Color.WHITE);
        countdownLabel.setBackground(urgent
                ? new Color(180, 20, 20, 210)
                : new Color(40, 40, 40, 180));
        countdownLabel.setBorder(BorderFactory.createLineBorder(
                urgent ? Color.RED : Color.WHITE, urgent ? 2 : 1));
    }

    private int sanitizeCharacterIndex(int requestedIndex, int fallbackIndex) {
        if (ALL_CHARACTERS.isEmpty()) return 0;
        if (requestedIndex >= 0 && requestedIndex < ALL_CHARACTERS.size()) return requestedIndex;
        if (fallbackIndex  >= 0 && fallbackIndex  < ALL_CHARACTERS.size()) return fallbackIndex;
        return 0;
    }

    private JPanel createSkillUI(List<JButton> buttonStore) {
        JPanel panel = new JPanel(new GridLayout(1, 3, 4, 4));
        panel.setOpaque(false);

        for (int i = 0; i < 3; i++) {
            JButton btn = new JButton("Skill " + (i + 1));
            btn.setFocusable(false);
            btn.setFont(new Font("Arial", Font.BOLD, 10));
            btn.setBackground(new Color(255, 255, 255, 180));
            btn.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
            btn.setToolTipText("MP Cost: " + SKILL_MP_COST[i]);

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

        if (backgroundImage != null)
            g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);

        // Player sprite
        if (p1HP <= 0) {
            if (!playerDeadFrames.isEmpty())
                g2.drawImage(playerDeadFrames.get(playerDeadFrameIndex),
                        p1SpriteX, p1SpriteY, getPlayerDrawWidth(), getPlayerDrawHeight(), this);
        } else if (isPlayerHurtAnimating && !playerHurtFrames.isEmpty()) {
            g2.drawImage(playerHurtFrames.get(playerHurtFrameIndex),
                    p1SpriteX, p1SpriteY, getPlayerDrawWidth(), getPlayerDrawHeight(), this);
        } else if (isWindWizardAttack3(true)) {
            // Wind Wizard skill 3 hides the caster while the overlay animation plays.
        } else if (isPlayerSkillAnimating && !activePlayerSkillFrames.isEmpty()) {
            BufferedImage skillFrame = activePlayerSkillFrames.get(playerSkillFrameIndex);
            int skillDrawHeight = getPlayerDrawHeight();
            int skillDrawWidth = getSkillDrawWidth(skillFrame, skillDrawHeight, getPlayerDrawWidth());
            g2.drawImage(skillFrame,
                p1SpriteX + activePlayerSkillOffsetX, p1SpriteY, skillDrawWidth, skillDrawHeight, this);
        } else if (!playerFrames.isEmpty()) {
            g2.drawImage(playerFrames.get(playerFrameIndex),
                    p1SpriteX, p1SpriteY, getPlayerDrawWidth(), getPlayerDrawHeight(), this);
        }

        // Enemy sprite (mirrored)
        if (p2HP <= 0) {
            if (!enemyDeadFrames.isEmpty())
                g2.drawImage(enemyDeadFrames.get(enemyDeadFrameIndex),
                        p2SpriteX + getEnemyDrawWidth(), p2SpriteY, -getEnemyDrawWidth(), getEnemyDrawHeight(), this);
        } else if (isEnemyHurtAnimating && !enemyHurtFrames.isEmpty()) {
            g2.drawImage(enemyHurtFrames.get(enemyHurtFrameIndex),
                    p2SpriteX + getEnemyDrawWidth(), p2SpriteY, -getEnemyDrawWidth(), getEnemyDrawHeight(), this);
        } else if (isWindWizardAttack3(false)) {
            // Wind Wizard skill 3 hides the caster while the overlay animation plays.
        } else if (isEnemySkillAnimating && !activeEnemySkillFrames.isEmpty()) {
            BufferedImage skillFrame = activeEnemySkillFrames.get(enemySkillFrameIndex);
            int skillDrawHeight = getEnemyDrawHeight();
            int skillDrawWidth = getSkillDrawWidth(skillFrame, skillDrawHeight, getEnemyDrawWidth());
            int enemySkillDrawX = p2SpriteX + skillDrawWidth + activeEnemySkillOffsetX;
            if (isWindWizardSkill2(false)) {
                enemySkillDrawX -= WIND_SKILL2_P2_LEFT_NUDGE_X;
            }
            g2.drawImage(skillFrame,
                enemySkillDrawX, p2SpriteY,
                -skillDrawWidth, skillDrawHeight, this);
        } else if (!enemyFrames.isEmpty()) {
            g2.drawImage(enemyFrames.get(enemyFrameIndex),
                    p2SpriteX + getEnemyDrawWidth(), p2SpriteY, -getEnemyDrawWidth(), getEnemyDrawHeight(), this);
        }

        // Wind skill 3 overlay is drawn last so it appears above both characters.
        if (isWindWizardAttack3(true) && !activePlayerSkillFrames.isEmpty()) {
            drawAnchoredSkillFrame(g2, activePlayerSkillFrames.get(playerSkillFrameIndex),
                getEnemyFeetAnchorX(), getEnemyFeetAnchorY(),
                getPlayerDrawWidth(), getPlayerDrawHeight(), true);
        }
        if (isWindWizardAttack3(false) && !activeEnemySkillFrames.isEmpty()) {
            drawAnchoredSkillFrame(g2, activeEnemySkillFrames.get(enemySkillFrameIndex),
                getPlayerFeetAnchorX(), getPlayerFeetAnchorY(),
                getEnemyDrawWidth(), getEnemyDrawHeight(), false);
        }

        // Projectile
        if (isProjectileAnimating && !activeProjectileFrames.isEmpty()) {
            BufferedImage frame = activeProjectileFrames.get(projectileFrameIndex);
            if (frame != null) {
                int dw = DARK_WIZARD_PROJECTILE_DRAW_SIZE, dh = DARK_WIZARD_PROJECTILE_DRAW_SIZE;
                if (projectileIsPlayerOne)
                    g2.drawImage(frame, projectileX, projectileY, projectileX + dw, projectileY + dh,
                            0, 0, frame.getWidth(), frame.getHeight(), this);
                else
                    g2.drawImage(frame, projectileX + dw, projectileY, projectileX, projectileY + dh,
                            0, 0, frame.getWidth(), frame.getHeight(), this);
            }
        }
    }

    private void loadMapData(String tmxResourcePath) {
        try {
            URL tmxUrl = getClass().getResource(tmxResourcePath);
            if (tmxUrl == null) { System.err.println("TMX not found: " + tmxResourcePath); return; }
            try (InputStream stream = tmxUrl.openStream()) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setIgnoringComments(true);
                factory.setNamespaceAware(false);
                Document document = factory.newDocumentBuilder().parse(stream);
                document.getDocumentElement().normalize();
                loadMapDimensions(document);
                loadMapBackground(document, tmxResourcePath, tmxUrl);
                loadSpawnPoint(document, "spawnPlayer",  true);
                loadSpawnPoint(document, "spawnPlayer2", false);
            }
        } catch (Exception e) { System.err.println("Failed to load TMX map data: " + e.getMessage()); }
    }

    public void setCharacters(int playerIdx, int enemyIdx) {
        if (playerTimer != null && playerTimer.isRunning()) playerTimer.stop();
        if (enemyTimer  != null && enemyTimer.isRunning())  enemyTimer.stop();
        stopSkillAnimation(true);
        stopSkillAnimation(false);
        stopProjectileAnimation();
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
        enemySkillAnimations  = loadSkillAnimations(enemyDef);
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


    public void executeSkill(int skillID) {
        if (!roundManager.isRoundInProgress()) return;
        if (p1HP <= 0 || p2HP <= 0) return;

        // ── MP check ─────────────────────────────────────────────────────────
        if (!hasEnoughMP(isP1Turn, skillID)) {
            String original = turnLabel.getText();
            turnLabel.setText("Not enough MP! (Need " + mpCostFor(skillID) + " MP)");
            Timer restore = new Timer(1200, e -> turnLabel.setText(original));
            restore.setRepeats(false);
            restore.start();
            return;
        }

        stopTurnTimer();

        boolean actingPlayerOne = isP1Turn;
        CharacterDef actor = actingPlayerOne ? currentPlayerDef : currentEnemyDef;
        boolean isDamageSkill = actor != null && "damage".equalsIgnoreCase(actor.getSkillType(skillID));
        boolean isDarkWizardProjectile = actor != null
                && "Dark Wizard".equalsIgnoreCase(actor.name)
                && (skillID == 2 || skillID == 3);

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

        // Spend MP for attacker, regen for defender
        spendAndRegenMP(actingPlayerOne, skillID);

        if (isDamageSkill)          scheduleHurtTimeline(actingPlayerOne, skillID);
        if (isDarkWizardProjectile) startProjectileAnimation(actingPlayerOne, skillID);
        else                        stopProjectileAnimation();

        playSkillAnimation(actingPlayerOne, skillID, null);

        isP1Turn = !isP1Turn;
        updateGameState();
        repaint();
    }

    private void updateGameState() {
        p1HP = Math.max(0, Math.min(100,    p1HP));
        p2HP = Math.max(0, Math.min(100,    p2HP));
        p1MP = Math.max(0, Math.min(MAX_MP, p1MP));
        p2MP = Math.max(0, Math.min(MAX_MP, p2MP));

        // HP label text (hidden — kept for safety)
        p1HPLabel.setText("HP: " + p1HP);
        p2HPLabel.setText("HP: " + p2HP);

        if (p1HealthBar != null) p1HealthBar.updateValue(p1HP);
        if (p2HealthBar != null) p2HealthBar.updateValue(p2HP);
        if (p1MpBar     != null) p1MpBar.updateValue(p1MP);
        if (p2MpBar     != null) p2MpBar.updateValue(p2MP);

        if (p1HP <= 0 && playerDeadTimer == null) startDeadAnimation(true);
        if (p2HP <= 0 && enemyDeadTimer  == null) startDeadAnimation(false);

        if (p1HP <= 0 || p2HP <= 0) {
            stopTurnTimer();
            if (!roundManager.isMatchOver()) {
                boolean p1WonRound = p2HP <= 0;
                roundManager.recordRoundResult(p1WonRound);
            }
            return;
        }

        turnLabel.setText(
                (isP1Turn ? "PLAYER 1'S TURN" : "PLAYER 2'S TURN") +
                        " | " + roundManager.getScoreDisplay("P1", "P2")
        );

        // Grey out unaffordable skill buttons
        refreshSkillButtonMPState();

        if (countdownLabel != null) {
            countdownLabel.setVisible(true);
            repositionUI();
        }
        startTurnTimer();

        System.out.println("P1: " + roundManager.getWinPips(true));
        System.out.println("P2: " + roundManager.getWinPips(false));
    }


    private void repositionUI() {
        int feetX1 = p1SpriteX + getPlayerDrawWidth() / 2;
        int feetY1 = p1SpriteY + getPlayerDrawHeight();
        int feetX2 = p2SpriteX + getEnemyDrawWidth()  / 2;
        int feetY2 = p2SpriteY + getEnemyDrawHeight();

        int btnW = tileSize * 2, btnH = 40;
        int gap  = 5;

        // Hidden HP labels — zero-sized so they take no space
        if (p1HPLabel != null) p1HPLabel.setBounds(0, 0, 0, 0);
        if (p2HPLabel != null) p2HPLabel.setBounds(0, 0, 0, 0);

        int panelH = Math.max(getHeight(), screenHeight);
        int hpY = Math.max(0, Math.min(BARS_TOP_Y, panelH - ((barH * 2) + gap + 10)));
        int mpY = hpY + barH + gap;

        // HP bars above the character
        if (p1HealthBar != null) p1HealthBar.setBounds(feetX1 - barW / 2, hpY, barW, barH);
        if (p2HealthBar != null) p2HealthBar.setBounds(feetX2 - barW / 2, hpY, barW, barH);

        // MP bars directly below HP bars
        if (p1MpBar != null) p1MpBar.setBounds(feetX1 - barW / 2, mpY, barW, barH);
        if (p2MpBar != null) p2MpBar.setBounds(feetX2 - barW / 2, mpY, barW, barH);

        // Skill buttons below feet (below bars stack)
        if (p1ButtonPanel != null) p1ButtonPanel.setBounds(feetX1 - btnW / 2, feetY1 + gap + barH + gap + barH + gap, btnW, btnH);
        if (p2ButtonPanel != null) p2ButtonPanel.setBounds(feetX2 - btnW / 2, feetY2 + gap + barH + gap + barH + gap, btnW, btnH);

        // Countdown above the active player's button panel
        if (countdownLabel != null) {
            int cdW = 56, cdH = 40;
            int activeFeetX = isP1Turn ? feetX1 : feetX2;
            int activeFeetY = isP1Turn
                    ? (feetY1 + gap + barH + gap + barH + gap)
                    : (feetY2 + gap + barH + gap + barH + gap);
            countdownLabel.setBounds(activeFeetX - cdW / 2, activeFeetY - cdH - 4, cdW, cdH);
        }
    }

    private void refreshSkillButtonLabels() {
        applySkillNamesToButtons(currentPlayerDef, p1SkillButtons);
        applySkillNamesToButtons(currentEnemyDef,  p2SkillButtons);
    }

    private void applySkillNamesToButtons(CharacterDef character, List<JButton> buttons) {
        if (character == null || buttons.isEmpty()) return;
        for (int i = 0; i < buttons.size(); i++) {
            String name = character.getSkillName(i + 1);
            int cost    = SKILL_MP_COST[i];
            buttons.get(i).setText("<html><center>" + name + "<br/><font color='#4488ff'>" + cost + " MP</font></center></html>");
            buttons.get(i).setToolTipText("MP Cost: " + cost);
        }
    }


    private int getPlayerDrawWidth()  { return currentPlayerDef != null ? currentPlayerDef.drawWidth  : DEFAULT_DRAW_WIDTH;  }
    private int getPlayerDrawHeight() { return currentPlayerDef != null ? currentPlayerDef.drawHeight : DEFAULT_DRAW_HEIGHT; }
    private int getEnemyDrawWidth()   { return currentEnemyDef  != null ? currentEnemyDef.drawWidth   : DEFAULT_DRAW_WIDTH;  }
    private int getEnemyDrawHeight()  { return currentEnemyDef  != null ? currentEnemyDef.drawHeight  : DEFAULT_DRAW_HEIGHT; }

    private int getSkillDrawWidth(BufferedImage frame, int targetHeight, int fallbackWidth) {
        if (frame == null || frame.getHeight() <= 0 || targetHeight <= 0) return fallbackWidth;
        return Math.max(1, Math.round((float) frame.getWidth() * targetHeight / frame.getHeight()));
    }

    private void loadMapBackground(Document document, String tmxResourcePath, URL tmxUrl) {
        NodeList imageNodes = document.getElementsByTagName("image");
        if (imageNodes.getLength() == 0) return;
        Element imageElement = (Element) imageNodes.item(0);
        String source = imageElement.getAttribute("source");
        if (source == null || source.isEmpty()) return;
        int imageWidth  = parseNumber(imageElement.getAttribute("width"));
        int imageHeight = parseNumber(imageElement.getAttribute("height"));
        if (imageWidth  > 0) mapPixelWidth  = imageWidth;
        if (imageHeight > 0) mapPixelHeight = imageHeight;
        Image loaded = tryLoadMapImage(source, tmxResourcePath, tmxUrl);
        if (loaded != null) backgroundImage = loaded;
        else System.err.println("TMX background image not found: " + source);
    }

    private Image tryLoadMapImage(String source, String tmxResourcePath, URL tmxUrl) {
        String resolvedResourcePath = resolveResourcePath(tmxResourcePath, source);
        URL resourceUrl = getClass().getResource(resolvedResourcePath);
        if (resourceUrl != null) return new ImageIcon(resourceUrl).getImage();
        try {
            Path tmxFilePath = Paths.get(tmxUrl.toURI());
            Path tmxDir = tmxFilePath.getParent();
            if (tmxDir != null) {
                Path imagePath = tmxDir.resolve(source.replace('\\', File.separatorChar)).normalize();
                if (Files.exists(imagePath)) return new ImageIcon(imagePath.toString()).getImage();
            }
        } catch (Exception ignored) {}
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
            if (isPlayerOne) { p1SpawnFeetMapX = spawnX; p1SpawnFeetMapY = spawnY; updateP1DrawFromSpawn(); }
            else             { p2SpawnFeetMapX = spawnX; p2SpawnFeetMapY = spawnY; updateP2DrawFromSpawn(); }
            return;
        }
    }

    private void loadMapDimensions(Document document) {
        Element map = document.getDocumentElement();
        if (map == null || !"map".equals(map.getTagName())) return;
        int widthInTiles  = parseNumber(map.getAttribute("width"));
        int heightInTiles = parseNumber(map.getAttribute("height"));
        int tileWidth     = parseNumber(map.getAttribute("tilewidth"));
        int tileHeight    = parseNumber(map.getAttribute("tileheight"));
        if (widthInTiles  > 0 && tileWidth  > 0) mapPixelWidth  = widthInTiles  * tileWidth;
        if (heightInTiles > 0 && tileHeight > 0) mapPixelHeight = heightInTiles * tileHeight;
    }

    private int scaleMapXToPanel(int mapX) {
        if (mapPixelWidth <= 0) return mapX;
        int panelWidth = Math.max(getWidth(), screenWidth);
        return (int) Math.round(mapX * ((double) panelWidth / mapPixelWidth));
    }

    private int scaleMapYToPanel(int mapY) {
        if (mapPixelHeight <= 0) return mapY;
        int panelHeight = Math.max(getHeight(), screenHeight);
        return (int) Math.round(mapY * ((double) panelHeight / mapPixelHeight));
    }

    private void updateP1DrawFromSpawn() {
        int feetX = scaleMapXToPanel(p1SpawnFeetMapX);
        int feetY = scaleMapYToPanel(p1SpawnFeetMapY);
        p1SpriteX = feetX - (getPlayerDrawWidth()  / 2);
        p1SpriteY = feetY -  getPlayerDrawHeight();
    }

    private void updateP2DrawFromSpawn() {
        int feetX = scaleMapXToPanel(p2SpawnFeetMapX);
        int feetY = scaleMapYToPanel(p2SpawnFeetMapY);
        p2SpriteX = feetX - (getEnemyDrawWidth()  / 2);
        p2SpriteY = feetY -  getEnemyDrawHeight();
    }

    private int parseNumber(String value) {
        if (value == null || value.isBlank()) return 0;
        return (int) Math.round(Double.parseDouble(value));
    }

    private String resolveResourcePath(String basePath, String relativePath) {
        String normalizedRelative = relativePath.replace('\\', '/');
        Path base     = Paths.get(basePath).getParent();
        Path resolved = (base == null ? Paths.get(normalizedRelative) : base.resolve(normalizedRelative)).normalize();
        String resourcePath = resolved.toString().replace('\\', '/');
        return resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
    }

    private List<BufferedImage> loadFrames(CharacterDef def)     { return loadAnimationFrames(def.idleAnimation); }
    private List<BufferedImage> loadDeadFrames(CharacterDef def) { return loadAnimationFrames(def.deadAnimation); }
    private List<BufferedImage> loadHurtFrames(CharacterDef def) { return loadAnimationFrames(def.hurtAnimation); }

    private List<List<BufferedImage>> loadSkillAnimations(CharacterDef def) {
        List<List<BufferedImage>> animations = new ArrayList<>();
        for (int skillID = 1; skillID <= 3; skillID++) {
            String path = def.getSkillSpritePath(skillID);
            if (path == null || path.isBlank()) { animations.add(List.of()); continue; }
            int frameWidth = getSkillFrameWidth(def, skillID);
            int frameHeight = getSkillFrameHeight(def, skillID);
            animations.add(loadAnimationFrames(new CharacterDef.AnimationDef(
                    path, frameWidth, frameHeight, DEFAULT_SKILL_DELAY_MS)));
        }
        return animations;
    }

    private int getSkillFrameWidth(CharacterDef def, int skillID) {
        if (def != null && "Wind Wizard".equals(def.name)) {
            if (skillID == 2) return 200;
            if (skillID == 3) return 288;
        }
        return DEFAULT_FRAME_SIZE;
    }

    private int getSkillFrameHeight(CharacterDef def, int skillID) {
        if (def != null && "Wind Wizard".equals(def.name) && (skillID == 2 || skillID == 3)) {
            return 128;
        }
        return DEFAULT_FRAME_SIZE;
    }

    private List<BufferedImage> loadAnimationFrames(CharacterDef.AnimationDef animation) {
        List<BufferedImage> frames = new ArrayList<>();
        try {
            URL resource = getClass().getResource(animation.sheetPath);
            if (resource == null) { System.err.println("Missing sprite sheet: " + animation.sheetPath); return frames; }
            BufferedImage sheet = ImageIO.read(resource);
            if (sheet == null)   { System.err.println("Could not decode sprite sheet: " + animation.sheetPath); return frames; }
            int frameWidth = animation.frameWidth;
            int frameHeight = animation.frameHeight;
            int columns = sheet.getWidth()  / frameWidth;
            int rows    = sheet.getHeight() / frameHeight;

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

            for (int row = 0; row < rows; row++)
                for (int col = 0; col < columns; col++)
                    frames.add(sheet.getSubimage(
                            col * frameWidth, row * frameHeight,
                            frameWidth, frameHeight));

            if (frames.isEmpty()) {
                frames.add(sheet);
            }
        } catch (Exception e) { System.err.println("Failed to load sprite sheet: " + e.getMessage()); }
        return frames;
    }

    private void playSkillAnimation(boolean isPlayerOne, int skillID, Runnable onCastFinished) {
        if (skillID < 1 || skillID > 3) return;
        Runnable callback = onCastFinished != null ? onCastFinished : () -> {};
        List<List<BufferedImage>> source = isPlayerOne ? playerSkillAnimations : enemySkillAnimations;
        if (source.isEmpty() || source.size() < skillID) { callback.run(); return; }
        List<BufferedImage> frames = source.get(skillID - 1);
        if (frames == null || frames.isEmpty()) { callback.run(); return; }

        if (isPlayerOne) {
            stopSkillAnimation(true);
            activePlayerSkillFrames  = frames;
            playerSkillFrameIndex    = 0;
            isPlayerSkillAnimating   = true;
            activePlayerSkillID      = skillID;
            activePlayerSkillOffsetX = currentPlayerDef != null ? currentPlayerDef.getSkillForwardOffsetX(skillID) : 0;
            playerSkillTimer = new Timer(DEFAULT_SKILL_DELAY_MS, null);
            playerSkillTimer.addActionListener(e -> {
                if (playerSkillFrameIndex < activePlayerSkillFrames.size() - 1) playerSkillFrameIndex++;
                else { stopSkillAnimation(true); callback.run(); }
                repaint();
            });
            playerSkillTimer.start();
        } else {
            stopSkillAnimation(false);
            activeEnemySkillFrames  = frames;
            enemySkillFrameIndex    = 0;
            isEnemySkillAnimating   = true;
            activeEnemySkillID      = skillID;
            activeEnemySkillOffsetX = -(currentEnemyDef != null ? currentEnemyDef.getSkillForwardOffsetX(skillID) : 0);
            enemySkillTimer = new Timer(DEFAULT_SKILL_DELAY_MS, null);
            enemySkillTimer.addActionListener(e -> {
                if (enemySkillFrameIndex < activeEnemySkillFrames.size() - 1) enemySkillFrameIndex++;
                else { stopSkillAnimation(false); callback.run(); }
                repaint();
            });
            enemySkillTimer.start();
        }
    }

    private void stopSkillAnimation(boolean isPlayerOne) {
        if (isPlayerOne) {
            if (playerSkillTimer != null) { playerSkillTimer.stop(); playerSkillTimer = null; }
            isPlayerSkillAnimating = false; playerSkillFrameIndex = 0;
            activePlayerSkillID = 0;        activePlayerSkillOffsetX = 0;   activePlayerSkillFrames = List.of();
        } else {
            if (enemySkillTimer != null)  { enemySkillTimer.stop();  enemySkillTimer  = null; }
            isEnemySkillAnimating = false;  enemySkillFrameIndex = 0;
            activeEnemySkillID = 0;         activeEnemySkillOffsetX = 0;    activeEnemySkillFrames = List.of();
        }
    }

    private boolean isWindWizardAttack3(boolean isPlayerOne) {
        CharacterDef actor = isPlayerOne ? currentPlayerDef : currentEnemyDef;
        int activeSkillID = isPlayerOne ? activePlayerSkillID : activeEnemySkillID;
        List<BufferedImage> frames = isPlayerOne ? activePlayerSkillFrames : activeEnemySkillFrames;
        return actor != null && "Wind Wizard".equalsIgnoreCase(actor.name) && activeSkillID == 3 && !frames.isEmpty();
    }

    private boolean isWindWizardSkill2(boolean isPlayerOne) {
        CharacterDef actor = isPlayerOne ? currentPlayerDef : currentEnemyDef;
        int activeSkillID = isPlayerOne ? activePlayerSkillID : activeEnemySkillID;
        return actor != null && "Wind Wizard".equalsIgnoreCase(actor.name) && activeSkillID == 2;
    }

    private void drawAnchoredSkillFrame(Graphics2D g2,
                                        BufferedImage frame,
                                        int targetFeetX,
                                        int targetFeetY,
                        int skillMaxDrawWidth,
                                        int skillDrawHeight,
                                        boolean mirror) {
        if (frame == null) return;
        int sourceX = 0;
        int sourceY = 0;
        int sourceW = Math.max(1, frame.getWidth());
        int sourceH = Math.max(1, frame.getHeight());
        double scale = Math.min(
            Math.max(1, skillMaxDrawWidth) / (double) sourceW,
            Math.max(1, skillDrawHeight) / (double) sourceH
        ) * WIND_SKILL3_SCALE;
        int drawWidth = Math.max(1, (int) Math.round(sourceW * scale));
        int drawHeight = Math.max(1, (int) Math.round(sourceH * scale));
        int directionalOffsetX = mirror ? WIND_SKILL3_FEET_OFFSET_X : -WIND_SKILL3_FEET_OFFSET_X;
        int x = targetFeetX - (drawWidth / 2) + directionalOffsetX;
        int y = targetFeetY - drawHeight + WIND_SKILL3_FEET_OFFSET_Y;
        if (mirror) {
            g2.drawImage(frame, x + drawWidth, y, x, y + drawHeight,
                    sourceX, sourceY, sourceX + sourceW, sourceY + sourceH, this);
        } else {
            g2.drawImage(frame, x, y, x + drawWidth, y + drawHeight,
                    sourceX, sourceY, sourceX + sourceW, sourceY + sourceH, this);
        }
    }

    private int getPlayerFeetAnchorX() {
        return scaleMapXToPanel(p1SpawnFeetMapX);
    }

    private int getPlayerFeetAnchorY() {
        return scaleMapYToPanel(p1SpawnFeetMapY);
    }

    private int getEnemyFeetAnchorX() {
        return scaleMapXToPanel(p2SpawnFeetMapX);
    }

    private int getEnemyFeetAnchorY() {
        return scaleMapYToPanel(p2SpawnFeetMapY);
    }

    private Rectangle getOpaqueBounds(BufferedImage image) {
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = (image.getRGB(x, y) >>> 24) & 0xff;
                if (alpha == 0) continue;
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

    private void startProjectileAnimation(boolean isPlayerOne, int skillID) {
        if (skillID != 2 && skillID != 3) return;
        CharacterDef actor = isPlayerOne ? currentPlayerDef : currentEnemyDef;
        if (actor == null || !"Dark Wizard".equalsIgnoreCase(actor.name)) return;
        List<BufferedImage> frames = loadDarkWizardProjectileFrames(skillID);
        if (frames.isEmpty()) return;
        stopProjectileAnimation();
        int attackerX      = isPlayerOne ? p1SpriteX : p2SpriteX;
        int attackerY      = isPlayerOne ? p1SpriteY : p2SpriteY;
        int attackerWidth  = isPlayerOne ? getPlayerDrawWidth()  : getEnemyDrawWidth();
        int attackerHeight = isPlayerOne ? getPlayerDrawHeight() : getEnemyDrawHeight();
        int targetX        = isPlayerOne ? p2SpriteX : p1SpriteX;
        int targetWidth    = isPlayerOne ? getEnemyDrawWidth()   : getPlayerDrawWidth();
        int projW = DARK_WIZARD_PROJECTILE_DRAW_SIZE;
        int projH = DARK_WIZARD_PROJECTILE_DRAW_SIZE;
        projectileDirection   = attackerX <= targetX ? 1 : -1;
        projectileIsPlayerOne = isPlayerOne;
        projectileX           = attackerX + (attackerWidth  / 2) - (projW / 2);
        projectileY           = attackerY + (attackerHeight / 2) - (projH / 2) + DARK_WIZARD_PROJECTILE_VERTICAL_OFFSET;
        activeProjectileFrames = frames;
        projectileFrameIndex  = 0;
        isProjectileAnimating = true;
        int stopBoundary = projectileDirection > 0
                ? (targetX + (targetWidth / 2)) - (projW / 2)
                : (targetX + (targetWidth / 2)) + (projW / 2);
        projectileTimer = new Timer(DEFAULT_SKILL_DELAY_MS, null);
        projectileTimer.addActionListener(e -> {
            if (!isProjectileAnimating || activeProjectileFrames.isEmpty()) { stopProjectileAnimation(); return; }
            projectileFrameIndex = (projectileFrameIndex + 1) % activeProjectileFrames.size();
            projectileX += projectileDirection * DARK_WIZARD_PROJECTILE_SPEED;
            if ((projectileDirection > 0 && projectileX >= stopBoundary)
                    || (projectileDirection < 0 && projectileX <= stopBoundary))
                stopProjectileAnimation();
            repaint();
        });
        projectileTimer.start();
    }

    private List<BufferedImage> loadDarkWizardProjectileFrames(int skillID) {
        String sheetPath = switch (skillID) {
            case 2 -> "/assets/spritesheet/Dark Wizard/Fire_1-Sheet.png";
            case 3 -> "/assets/spritesheet/Dark Wizard/Fire_2-Sheet.png";
            default -> null;
        };
        if (sheetPath == null) return List.of();
        List<BufferedImage> cached = projectileAnimationCache.get(sheetPath);
        if (cached != null) return cached;
        List<BufferedImage> frames = loadAnimationFrames(
                new CharacterDef.AnimationDef(sheetPath, 64, 64, DEFAULT_SKILL_DELAY_MS));
        projectileAnimationCache.put(sheetPath, frames);
        return frames;
    }

    private void stopProjectileAnimation() {
        if (projectileTimer != null) { projectileTimer.stop(); projectileTimer = null; }
        isProjectileAnimating = false; projectileFrameIndex = 0;
        projectileX = 0; projectileY = 0; projectileDirection = 1;
        projectileIsPlayerOne = true;   activeProjectileFrames = List.of();
    }

    private void scheduleHurtTimeline(boolean attackerIsPlayerOne, int skillID) {
        CharacterDef attacker = attackerIsPlayerOne ? currentPlayerDef : currentEnemyDef;
        if (attacker == null) return;
        int castDurationMs     = getSkillCastDurationMs(attackerIsPlayerOne, skillID);
        int configuredBufferMs = (int) Math.round(attacker.getSkillHurtTriggerBufferSeconds(skillID) * 1000.0);
        int effectiveBufferMs  = Math.max(0, Math.min(configuredBufferMs, castDurationMs));
        int hurtDurationMs     = Math.max(1, (castDurationMs - effectiveBufferMs) + POST_ATTACK_HURT_MS);
        startDelayedHurt(!attackerIsPlayerOne, effectiveBufferMs, hurtDurationMs);
    }

    private int getSkillCastDurationMs(boolean attackerIsPlayerOne, int skillID) {
        List<List<BufferedImage>> source = attackerIsPlayerOne ? playerSkillAnimations : enemySkillAnimations;
        if (skillID < 1 || source.isEmpty() || source.size() < skillID) return DEFAULT_SKILL_DELAY_MS;
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
        if (targetIsPlayer) playerHurtDelayTimer = delayTimer;
        else                enemyHurtDelayTimer  = delayTimer;
    }

    private void startTimedHurt(boolean isPlayer, int durationMs) {
        if (isPlayer) {
            if (p1HP <= 0 || playerHurtFrames.isEmpty()) return;
            if (playerHurtTimer       != null) playerHurtTimer.stop();
            if (playerHurtWindowTimer != null) playerHurtWindowTimer.stop();
            isPlayerHurtAnimating = true; playerHurtFrameIndex = 0;
            playerHurtTimer = new Timer(DEFAULT_HURT_DELAY_MS, null);
            playerHurtTimer.addActionListener(e -> {
                if (!playerHurtFrames.isEmpty())
                    playerHurtFrameIndex = (playerHurtFrameIndex + 1) % playerHurtFrames.size();
                repaint();
            });
            playerHurtTimer.start();
            playerHurtWindowTimer = new Timer(durationMs, null);
            playerHurtWindowTimer.setRepeats(false);
            playerHurtWindowTimer.addActionListener(e -> stopHurtTimeline(true));
            playerHurtWindowTimer.start();
        } else {
            if (p2HP <= 0 || enemyHurtFrames.isEmpty()) return;
            if (enemyHurtTimer       != null) enemyHurtTimer.stop();
            if (enemyHurtWindowTimer != null) enemyHurtWindowTimer.stop();
            isEnemyHurtAnimating = true; enemyHurtFrameIndex = 0;
            enemyHurtTimer = new Timer(DEFAULT_HURT_DELAY_MS, null);
            enemyHurtTimer.addActionListener(e -> {
                if (!enemyHurtFrames.isEmpty())
                    enemyHurtFrameIndex = (enemyHurtFrameIndex + 1) % enemyHurtFrames.size();
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
            if (playerHurtDelayTimer  != null) { playerHurtDelayTimer.stop();  playerHurtDelayTimer  = null; }
            if (playerHurtWindowTimer != null) { playerHurtWindowTimer.stop(); playerHurtWindowTimer = null; }
            if (playerHurtTimer       != null) { playerHurtTimer.stop();       playerHurtTimer       = null; }
            isPlayerHurtAnimating = false; playerHurtFrameIndex = 0;
        } else {
            if (enemyHurtDelayTimer  != null) { enemyHurtDelayTimer.stop();  enemyHurtDelayTimer  = null; }
            if (enemyHurtWindowTimer != null) { enemyHurtWindowTimer.stop(); enemyHurtWindowTimer = null; }
            if (enemyHurtTimer       != null) { enemyHurtTimer.stop();       enemyHurtTimer       = null; }
            isEnemyHurtAnimating = false; enemyHurtFrameIndex = 0;
        }
    }

    private void startDeadAnimation(boolean isPlayer) {
        if (isPlayer) {
            if (playerTimer != null) playerTimer.stop();
            stopSkillAnimation(true); stopHurtTimeline(true);
            if (playerDeadFrames.isEmpty()) return;
            playerDeadFrameIndex = 0;
            int delay = currentPlayerDef != null ? currentPlayerDef.deadAnimation.frameDelayMs : 150;
            playerDeadTimer = new Timer(delay, null);
            playerDeadTimer.addActionListener(e -> {
                if (playerDeadFrameIndex < playerDeadFrames.size() - 1) playerDeadFrameIndex++;
                else playerDeadTimer.stop();
                repaint();
            });
            playerDeadTimer.start();
        } else {
            if (enemyTimer != null) enemyTimer.stop();
            stopSkillAnimation(false); stopHurtTimeline(false);
            if (enemyDeadFrames.isEmpty()) return;
            enemyDeadFrameIndex = 0;
            int delay = currentEnemyDef != null ? currentEnemyDef.deadAnimation.frameDelayMs : 150;
            enemyDeadTimer = new Timer(delay, null);
            enemyDeadTimer.addActionListener(e -> {
                if (enemyDeadFrameIndex < enemyDeadFrames.size() - 1) enemyDeadFrameIndex++;
                else enemyDeadTimer.stop();
                repaint();
            });
            enemyDeadTimer.start();
        }
    }

    private static List<CharacterDef> loadCharacterDefs() {
        List<CharacterDataLoader.CharacterConfig> configs =
                CharacterDataLoader.loadCharacterConfigs("/assets/data/characters.json");
        List<CharacterDef> defs = new ArrayList<>();
        for (CharacterDataLoader.CharacterConfig config : configs) {
            int drawWidth = config.drawWidth > 0 ? config.drawWidth : DEFAULT_DRAW_WIDTH;
            int drawHeight = config.drawHeight > 0 ? config.drawHeight : DEFAULT_DRAW_HEIGHT;
            defs.add(new CharacterDef(
                    config.name, config.backstory,
                    config.skill1Name, config.skill2Name, config.skill3Name,
                    config.skill1Description, config.skill2Description, config.skill3Description,
                    config.skill1Type, config.skill2Type, config.skill3Type,
                    config.skill1SpritePath, config.skill2SpritePath, config.skill3SpritePath,
                    config.skill1ForwardOffsetX, config.skill2ForwardOffsetX, config.skill3ForwardOffsetX,
                    config.skill1HurtTriggerBufferSeconds, config.skill2HurtTriggerBufferSeconds,
                    config.skill3HurtTriggerBufferSeconds,
                    new CharacterDef.AnimationDef(config.idleSpritePath,  DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_IDLE_DELAY_MS),
                    new CharacterDef.AnimationDef(config.hurtSpritePath,  DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_HURT_DELAY_MS),
                    new CharacterDef.AnimationDef(config.deathSpritePath, DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_DEAD_DELAY_MS),
                    drawWidth, drawHeight));
        }
        if (!defs.isEmpty()) return List.copyOf(defs);

        return List.of(
                new CharacterDef("Light Mage",
                        "A former temple guardian who forged sacred combat arts to protect frontier villages.",
                        "Light Sword", "Halo of Aegis", "Dawn Piercer",
                        "Unleashes a radiant slash that deals high single-target light damage.",
                        "Creates a protective halo that grants a shield and minor regeneration to herself.",
                        "Calls down a focused beam that burns one enemy with concentrated light damage.",
                        "damage", "defense", "damage",
                        "/assets/spritesheet/Light Mage/LightSword-Sheet.png",
                        "/assets/spritesheet/Light Mage/HaloOfAegis-Sheet.png",
                        "/assets/spritesheet/Light Mage/DawnPiercer-Sheet.png",
                        0, 0, 0, 0.0, 0.0, 0.0,
                        new CharacterDef.AnimationDef("/assets/spritesheet/Light Mage/Idle-Sheet.png",  DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_IDLE_DELAY_MS),
                        new CharacterDef.AnimationDef("/assets/spritesheet/Light Mage/Hurt-Sheet.png",  DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_HURT_DELAY_MS),
                        new CharacterDef.AnimationDef("/assets/spritesheet/Light Mage/Dead-Sheet.png",  DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_DEAD_DELAY_MS),
                        DEFAULT_DRAW_WIDTH, DEFAULT_DRAW_HEIGHT),
                new CharacterDef("Idk Magician",
                        "A wandering arcane prodigy who channels unstable spellcraft in battle.",
                        "Magic Arrow", "Arcane Charge", "Magic Sphere",
                        "Fires a condensed arcane bolt that pierces one target and deals medium magic damage.",
                        "Wraps the caster in a rune shield, reducing incoming damage for 2 turns.",
                        "Summons a rotating sphere that shocks enemies and lowers their attack.",
                        "damage", "defense", "debuff",
                        "/assets/spritesheet/Idk Magician/Magic_arrow.png",
                        "/assets/spritesheet/Idk Magician/Charge_1.png",
                        "/assets/spritesheet/Idk Magician/Magic_sphere.png",
                        0, 0, 0, 0.0, 0.0, 0.0,
                        new CharacterDef.AnimationDef("/assets/spritesheet/Idk Magician/Idle.png", DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_IDLE_DELAY_MS),
                        new CharacterDef.AnimationDef("/assets/spritesheet/Idk Magician/Hurt.png", DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_HURT_DELAY_MS),
                        new CharacterDef.AnimationDef("/assets/spritesheet/Idk Magician/Dead.png", DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_DEAD_DELAY_MS),
                        DEFAULT_DRAW_WIDTH, DEFAULT_DRAW_HEIGHT),
                    new CharacterDef("Water Wizard",
                        "A tidebound mage who shapes pressure, mist, and currents into disciplined battlefield control. He wears foes down with forceful strikes and relentless flow.",
                        "Tidal Blade", "Barrier Surge", "Undertow Hex",
                        "Slices one enemy with a pressurized water blade that deals steady damage.",
                        "Raises a flowing shield that softens incoming damage for 2 turns.",
                        "Whips up a dragging current that lowers all enemies' attack for 2 turns.",
                        "damage", "defense", "debuff",
                        "/assets/spritesheet/Water Wizard/Attack-Sheet.png",
                        "/assets/spritesheet/Water Wizard/Charge-Sheet.png",
                        "/assets/spritesheet/Water Wizard/Attack2-Sheet.png",
                        0, 0, 0, 0.14, 0.0, 0.0,
                        new CharacterDef.AnimationDef("/assets/spritesheet/Water Wizard/Idle-Sheet.png", DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_IDLE_DELAY_MS),
                        new CharacterDef.AnimationDef("/assets/spritesheet/Water Wizard/Hurt-Sheet.png", DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_HURT_DELAY_MS),
                        new CharacterDef.AnimationDef("/assets/spritesheet/Water Wizard/Dead-Sheet.png", DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_DEAD_DELAY_MS),
                        DEFAULT_DRAW_WIDTH, DEFAULT_DRAW_HEIGHT),
                    new CharacterDef("Wind Wizard",
                        "A sky runner who bends pressure and razor gusts into elegant battlefield control. She dances through combat, striking fast and breaking enemy rhythm.",
                        "Gale Cut", "Cyclone Guard", "Tempest Lash",
                        "Cuts through one enemy with a razor gust that deals quick damage.",
                        "Wraps the caster in a spinning wind barrier that reduces incoming damage for 2 turns.",
                        "Unleashes a sweeping gale that lowers all enemies' attack for 2 turns.",
                        "damage", "defense", "debuff",
                        "/assets/spritesheet/Wind WIzard/Attack1-Sheet.png",
                        "/assets/spritesheet/Wind WIzard/Attack2-Sheet.png",
                        "/assets/spritesheet/Wind WIzard/Attack3-Sheet.png",
                        0, 0, 0, 0.12, 0.0, 0.0,
                        new CharacterDef.AnimationDef("/assets/spritesheet/Wind WIzard/Idle-Sheet.png", DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_IDLE_DELAY_MS),
                        new CharacterDef.AnimationDef("/assets/spritesheet/Wind WIzard/Hurt-Sheet.png", DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_HURT_DELAY_MS),
                        new CharacterDef.AnimationDef("/assets/spritesheet/Wind WIzard/Dead-Sheet.png", DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_DEAD_DELAY_MS),
                        DEFAULT_DRAW_WIDTH, DEFAULT_DRAW_HEIGHT));
    }
}