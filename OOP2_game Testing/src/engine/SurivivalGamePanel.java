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

import assets.Utility.FontManager;
import assets.Utility.GameBar;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SurivivalGamePanel extends JPanel {

    private final int tileSize = 128;
    private final int screenWidth  = tileSize * 12;
    private final int screenHeight = tileSize * 7;
    private static final int DEFAULT_DRAW_WIDTH        = 384;
    private static final int DEFAULT_DRAW_HEIGHT       = 384;
    private static final int DEFAULT_FRAME_SIZE        = 128;
    private static final int DEFAULT_IDLE_DELAY_MS     = 120;
    private static final int DEFAULT_DEAD_DELAY_MS     = 150;
    private static final int DEFAULT_SKILL_DELAY_MS    = 90;
    private static final int DEFAULT_HURT_DELAY_MS     = 90;
    private static final int POST_ATTACK_HURT_MS       = 600;
    private static final int DARK_WIZARD_PROJECTILE_DRAW_SIZE        = 144;
    private static final int DARK_WIZARD_PROJECTILE_VERTICAL_OFFSET  = 50;
    private static final int DARK_WIZARD_PROJECTILE_SPEED            = 44;

    private static final int BOT_TURN_DELAY_MS = 900;

    private static final int SURVIVAL_ROUND_HEAL_AMOUNT = 30;

    private int mapPixelWidth  = screenWidth;
    private int mapPixelHeight = screenHeight;

    public static final List<CharacterDef> ALL_CHARACTERS = loadCharacterDefs();

    private Image backgroundImage;

    private List<BufferedImage> playerFrames     = new ArrayList<>();
    private int                 playerFrameIndex = 0;
    private Timer               playerTimer;

    private List<BufferedImage> enemyFrames     = new ArrayList<>();
    private int                 enemyFrameIndex = 0;
    private Timer               enemyTimer;

    private List<List<BufferedImage>> playerSkillAnimations = new ArrayList<>();
    private List<List<BufferedImage>> enemySkillAnimations  = new ArrayList<>();
    private List<BufferedImage>       activePlayerSkillFrames = List.of();
    private List<BufferedImage>       activeEnemySkillFrames  = List.of();
    private int   playerSkillFrameIndex = 0;
    private int   enemySkillFrameIndex  = 0;
    private Timer playerSkillTimer;
    private Timer enemySkillTimer;
    private boolean isPlayerSkillAnimating = false;
    private boolean isEnemySkillAnimating  = false;
    private int activePlayerSkillOffsetX = 0;
    private int activeEnemySkillOffsetX  = 0;

    private final Map<String, List<BufferedImage>> projectileAnimationCache = new HashMap<>();
    private List<BufferedImage> activeProjectileFrames = List.of();
    private int   projectileFrameIndex   = 0;
    private int   projectileX            = 0;
    private int   projectileY            = 0;
    private int   projectileDirection    = 1;
    private boolean projectileIsPlayerOne = true;
    private Timer projectileTimer;
    private boolean isProjectileAnimating = false;

    private List<BufferedImage> playerHurtFrames     = new ArrayList<>();
    private List<BufferedImage> enemyHurtFrames      = new ArrayList<>();
    private int   playerHurtFrameIndex = 0;
    private int   enemyHurtFrameIndex  = 0;
    private Timer playerHurtTimer;
    private Timer enemyHurtTimer;
    private Timer playerHurtDelayTimer;
    private Timer enemyHurtDelayTimer;
    private Timer playerHurtWindowTimer;
    private Timer enemyHurtWindowTimer;
    private boolean isPlayerHurtAnimating = false;
    private boolean isEnemyHurtAnimating  = false;

    private CharacterDef currentPlayerDef, currentEnemyDef;
    private List<BufferedImage> playerDeadFrames     = new ArrayList<>();
    private List<BufferedImage> enemyDeadFrames      = new ArrayList<>();
    private int   playerDeadFrameIndex = 0;
    private int   enemyDeadFrameIndex  = 0;
    private Timer playerDeadTimer;
    private Timer enemyDeadTimer;

    private int p1SpriteX = 350;
    private int p1SpriteY = 300;
    private int p1SpawnFeetMapX = p1SpriteX + (DEFAULT_DRAW_WIDTH  / 2);
    private int p1SpawnFeetMapY = p1SpriteY + DEFAULT_DRAW_HEIGHT;
    private int p2SpriteX = screenWidth - 350 - DEFAULT_DRAW_WIDTH;
    private int p2SpriteY = 300;
    private int p2SpawnFeetMapX = p2SpriteX + (DEFAULT_DRAW_WIDTH  / 2);
    private int p2SpawnFeetMapY = p2SpriteY + DEFAULT_DRAW_HEIGHT;

    private int     p1HP    = 100, p2HP = 100;
    private boolean isP1Turn = true;

    // [AI/SURVIVAL] Game mode and bot config
    private GameMode         gameMode   = GameMode.PVP;
    private BotAI.Difficulty difficulty = BotAI.Difficulty.NORMAL;

    // [AI/SURVIVAL] Survival mode state
    private int  survivalRound = 1;
    private int  survivalScore = 0;
    private boolean survivalAwaitingNextRound = false;

    private boolean botIsThinking = false;

    private JPanel   p1ButtonPanel, p2ButtonPanel;
    private final List<JButton> p1SkillButtons = new ArrayList<>();
    private final List<JButton> p2SkillButtons = new ArrayList<>();
    private JLabel turnLabel, p1HPLabel, p2HPLabel;

    private JLabel roundLabel;   // shows "Round X" in survival
    private JLabel scoreLabel;   // shows "Score: X" in survival
    private JButton nextRoundBtn; // "Next Round" button shown between survival rounds

    private GameBar p1HealthBar, p2HealthBar;
    int barW = 200;
    int barH = 20;


    public SurivivalGamePanel() {
        this(0, 1, GameMode.PVP, BotAI.Difficulty.NORMAL);
    }


    // [AI/SURVIVAL] Primary constructor — accepts game mode and difficulty.
    public SurivivalGamePanel(int playerCharacterIndex, int enemyCharacterIndex,
                     GameMode mode, BotAI.Difficulty difficulty) {
        this.gameMode   = mode;
        this.difficulty = difficulty;

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

        this.setLayout(null);

        loadMapData("/assets/maps/map1.tmx");

        int safePlayerIndex = sanitizeCharacterIndex(playerCharacterIndex, 0);
        int safeEnemyIndex  = sanitizeCharacterIndex(enemyCharacterIndex, safePlayerIndex == 0 ? 1 : 0);
        setCharacters(safePlayerIndex, safeEnemyIndex);

        Font boldFont = FontManager.getFont(40).deriveFont(Font.BOLD);
        Font noneBold = FontManager.getFont(22).deriveFont(Font.BOLD);

        // Turn indicator
        turnLabel = new JLabel("PLAYER 1'S TURN", SwingConstants.CENTER);
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

        // Skill buttons
        p1ButtonPanel = createSkillUI(p1SkillButtons);
        this.add(p1ButtonPanel);

        p2ButtonPanel = createSkillUI(p2SkillButtons);
        this.add(p2ButtonPanel);

        // Health bars
        p1HealthBar = new GameBar(100, Color.GREEN);
        p2HealthBar = new GameBar(100, Color.RED);
        this.add(p1HealthBar);
        this.add(p2HealthBar);

        // [AI/SURVIVAL] Survival UI labels (hidden in non-survival modes)
        roundLabel = new JLabel("Round 1", SwingConstants.CENTER);
        roundLabel.setFont(FontManager.getFont(26).deriveFont(Font.BOLD));
        roundLabel.setForeground(new Color(255, 220, 80));
        roundLabel.setVisible(gameMode == GameMode.SURVIVAL);
        this.add(roundLabel);

        scoreLabel = new JLabel("Score: 0", SwingConstants.CENTER);
        scoreLabel.setFont(FontManager.getFont(22).deriveFont(Font.BOLD));
        scoreLabel.setForeground(new Color(180, 255, 180));
        scoreLabel.setVisible(gameMode == GameMode.SURVIVAL);
        this.add(scoreLabel);

        // [AI/SURVIVAL] "Next Round" button — hidden until a round ends in survival
        nextRoundBtn = new JButton("⚔  Next Round");
        nextRoundBtn.setFont(FontManager.getFont(28).deriveFont(Font.BOLD));
        nextRoundBtn.setBackground(new Color(60, 180, 80));
        nextRoundBtn.setForeground(Color.WHITE);
        nextRoundBtn.setFocusable(false);
        nextRoundBtn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        nextRoundBtn.setVisible(false);
        nextRoundBtn.addActionListener(e -> advanceSurvivalRound());
        this.add(nextRoundBtn);

        refreshSkillButtonLabels();
        repositionUI();
        updateGameState();
    }

    public void setGameMode(GameMode mode) {
        this.gameMode = mode;
        roundLabel.setVisible(mode == GameMode.SURVIVAL);
        scoreLabel.setVisible(mode == GameMode.SURVIVAL);
        updateGameState();
    }

    public void setDifficulty(BotAI.Difficulty diff) {
        this.difficulty = diff;
    }

    public GameMode getGameMode()         { return gameMode; }
    public int      getSurvivalRound()    { return survivalRound; }
    public int      getSurvivalScore()    { return survivalScore; }

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

        if (backgroundImage != null) {
            g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }

        // Player sprite
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

        // Enemy sprite (mirrored)
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

        // Projectile
        if (isProjectileAnimating && !activeProjectileFrames.isEmpty()) {
            BufferedImage frame = activeProjectileFrames.get(projectileFrameIndex);
            if (frame != null) {
                int drawWidth  = DARK_WIZARD_PROJECTILE_DRAW_SIZE;
                int drawHeight = DARK_WIZARD_PROJECTILE_DRAW_SIZE;
                if (projectileIsPlayerOne) {
                    g2.drawImage(frame, projectileX, projectileY, projectileX + drawWidth, projectileY + drawHeight, 0, 0, frame.getWidth(), frame.getHeight(), this);
                } else {
                    g2.drawImage(frame, projectileX + drawWidth, projectileY, projectileX, projectileY + drawHeight, 0, 0, frame.getWidth(), frame.getHeight(), this);
                }
            }
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
                loadSpawnPoint(document, "spawnPlayer",  true);
                loadSpawnPoint(document, "spawnPlayer2", false);
            }
        } catch (Exception e) {
            System.err.println("Failed to load TMX map data: " + e.getMessage());
        }
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

        playerFrames          = loadFrames(playerDef);
        enemyFrames           = loadFrames(enemyDef);
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
        playerHurtFrames     = loadHurtFrames(playerDef);
        enemyHurtFrames      = loadHurtFrames(enemyDef);
        playerHurtFrameIndex = 0;
        enemyHurtFrameIndex  = 0;
        playerDeadFrames     = loadDeadFrames(playerDef);
        enemyDeadFrames      = loadDeadFrames(enemyDef);
        playerDeadFrameIndex = 0;
        enemyDeadFrameIndex  = 0;
        repaint();
    }


    public void executeSkill(int skillID) {
        if (p1HP <= 0 || p2HP <= 0) return;

        // [AI/SURVIVAL] Block player input while the bot is thinking.
        if (botIsThinking) return;

        boolean actingPlayerOne  = isP1Turn;
        CharacterDef actor       = actingPlayerOne ? currentPlayerDef : currentEnemyDef;
        boolean isDamageSkill    = actor != null && "damage".equalsIgnoreCase(actor.getSkillType(skillID));
        boolean isDarkWizardProj = actor != null
                && "Dark Wizard".equalsIgnoreCase(actor.name)
                && (skillID == 2 || skillID == 3);

        // Apply HP changes
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

        if (isDamageSkill)    scheduleHurtTimeline(actingPlayerOne, skillID);
        if (isDarkWizardProj) startProjectileAnimation(actingPlayerOne, skillID);
        else                  stopProjectileAnimation();

        playSkillAnimation(actingPlayerOne, skillID, null);

        isP1Turn = !isP1Turn;
        updateGameState();
        repaint();

        maybeTriggerBotTurn();
    }

    private void maybeTriggerBotTurn() {
        boolean isBotMode = (gameMode == GameMode.PVB || gameMode == GameMode.SURVIVAL);
        if (!isBotMode) return;

        // It is now P2's turn (the bot) — P1 just acted, so !isP1Turn == true for bot.
        // (isP1Turn was already flipped in executeSkill above.)
        if (isP1Turn) return; // Still player's turn — shouldn't happen, but guard.
        if (p1HP <= 0 || p2HP <= 0) return; // Game already over.

        botIsThinking = true;
        setPlayerButtonsEnabled(false); // disable during bot turn

        Timer botDelay = new Timer(BOT_TURN_DELAY_MS, null);
        botDelay.setRepeats(false);
        botDelay.addActionListener(e -> {
            botIsThinking = false;
            performBotAction();
        });
        botDelay.start();
    }

    /**
     * The bot chooses and executes its skill.
     * Note: executeSkill handles turn flipping, so after this call it will be P1's turn again.
     */
    private void performBotAction() {
        if (p1HP <= 0 || p2HP <= 0) {
            setPlayerButtonsEnabled(true);
            return;
        }

        // [AI/SURVIVAL] Scale difficulty in survival: every 3 rounds, bump up one tier.
        BotAI.Difficulty effectiveDiff = scaledDifficulty();

        int chosenSkill = BotAI.chooseSkill(p2HP, p1HP, currentEnemyDef, effectiveDiff, 100);
        executeSkill(chosenSkill);

        // Re-enable player buttons after bot acts (executeSkill flips back to P1).
        setPlayerButtonsEnabled(true);
    }


    private BotAI.Difficulty scaledDifficulty() {
        if (gameMode != GameMode.SURVIVAL) return difficulty;
        return switch (difficulty) {
            case EASY   -> (survivalRound >= 5) ? BotAI.Difficulty.NORMAL : BotAI.Difficulty.EASY;
            case NORMAL -> (survivalRound >= 6) ? BotAI.Difficulty.HARD   : BotAI.Difficulty.NORMAL;
            case HARD   -> BotAI.Difficulty.HARD;
        };
    }

    /** Enables/disables only the human player's skill buttons. */
    private void setPlayerButtonsEnabled(boolean enabled) {
        for (JButton btn : p1SkillButtons) btn.setEnabled(enabled);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // [AI/SURVIVAL] Survival round progression
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called when the enemy (P2) dies in survival mode.
     * Awards score, shows the "Next Round" button.
     */
    private void handleSurvivalRoundWon() {
        // Score = base 100 per round, bonus for remaining HP, bonus for round number.
        int hpBonus      = p1HP;
        int roundBonus   = survivalRound * 10;
        int roundScore   = 100 + hpBonus + roundBonus;
        survivalScore   += roundScore;
        survivalAwaitingNextRound = true;

        // Update score display
        scoreLabel.setText("Score: " + survivalScore);

        // Show summary in turn label
        turnLabel.setText("Round " + survivalRound + " Clear!  +" + roundScore + " pts");

        // Show the Next Round button, hide skill buttons
        nextRoundBtn.setVisible(true);
        p1ButtonPanel.setVisible(false);
        p2ButtonPanel.setVisible(false);

        repositionUI();
        repaint();
    }

    /**
     * Advances to the next survival round:
     * - Heals the player partially
     * - Picks a new random enemy
     * - Resets enemy HP
     * - Increments round counter
     */
    private void advanceSurvivalRound() {
        if (!survivalAwaitingNextRound) return;

        survivalRound++;
        survivalAwaitingNextRound = false;

        // Partial heal for the player
        p1HP = Math.min(100, p1HP + SURVIVAL_ROUND_HEAL_AMOUNT);
        p2HP = 100;

        // Pick a new random enemy (different from the player's character)
        int playerIdx = ALL_CHARACTERS.indexOf(currentPlayerDef);
        int newEnemyIdx;
        if (ALL_CHARACTERS.size() > 1) {
            do {
                newEnemyIdx = (int) (Math.random() * ALL_CHARACTERS.size());
            } while (newEnemyIdx == playerIdx);
        } else {
            newEnemyIdx = 0;
        }

        // Reload enemy (preserves player)
        setEnemyCharacter(newEnemyIdx);

        // Reset turn to player
        isP1Turn = true;

        // Update survival UI
        roundLabel.setText("Round " + survivalRound);

        // Hide Next Round button, re-show player buttons
        nextRoundBtn.setVisible(false);
        survivalAwaitingNextRound = false;

        updateGameState();
        repositionUI();
        repaint();
    }

    /**
     * Reloads only the enemy character, keeping player state intact.
     * (Lighter than setCharacters() which resets both sides.)
     */
    private void setEnemyCharacter(int enemyIdx) {
        if (enemyTimer != null && enemyTimer.isRunning()) enemyTimer.stop();
        stopSkillAnimation(false);
        stopProjectileAnimation();
        stopHurtTimeline(false);

        CharacterDef enemyDef = ALL_CHARACTERS.get(enemyIdx);
        currentEnemyDef = enemyDef;

        refreshSkillButtonLabels();
        updateP2DrawFromSpawn();

        enemyFrames          = loadFrames(enemyDef);
        enemySkillAnimations = loadSkillAnimations(enemyDef);
        enemyFrameIndex      = 0;

        if (!enemyFrames.isEmpty()) {
            enemyTimer = new Timer(enemyDef.idleAnimation.frameDelayMs, e -> {
                enemyFrameIndex = (enemyFrameIndex + 1) % enemyFrames.size();
                repaint();
            });
            enemyTimer.start();
        }

        if (enemyDeadTimer != null) { enemyDeadTimer.stop(); enemyDeadTimer = null; }
        enemyHurtFrames     = loadHurtFrames(enemyDef);
        enemyHurtFrameIndex = 0;
        enemyDeadFrames     = loadDeadFrames(enemyDef);
        enemyDeadFrameIndex = 0;
        repaint();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Game state update — survival hooks added
    // ─────────────────────────────────────────────────────────────────────────

    private void updateGameState() {
        p1HP = Math.max(0, Math.min(100, p1HP));
        p2HP = Math.max(0, Math.min(100, p2HP));

        p1HPLabel.setText("HP: " + p1HP);
        p2HPLabel.setText("HP: " + p2HP);

        if (p1HealthBar != null) p1HealthBar.updateValue(p1HP);
        if (p2HealthBar != null) p2HealthBar.updateValue(p2HP);

        if (p1HP <= 0 && playerDeadTimer == null) startDeadAnimation(true);
        if (p2HP <= 0 && enemyDeadTimer  == null) startDeadAnimation(false);

        if (p1HP <= 0 || p2HP <= 0) {
            // ── Game / Round over ──────────────────────────────────────────────
            if (gameMode == GameMode.SURVIVAL) {
                if (p2HP <= 0 && p1HP > 0) {
                    // Player won the round
                    handleSurvivalRoundWon();
                } else {
                    // Player died — game over in survival
                    turnLabel.setText("Game Over!  Final Score: " + survivalScore);
                    p1ButtonPanel.setVisible(false);
                    p2ButtonPanel.setVisible(false);
                    nextRoundBtn.setVisible(false);
                }
            } else {
                // PVP / PVB final result
                String winner = (gameMode == GameMode.PVB && p2HP <= 0)
                        ? "YOU WIN!"
                        : (gameMode == GameMode.PVB && p1HP <= 0)
                        ? "BOT WINS!"
                        : (p1HP <= 0 ? "PLAYER 2 WINS!" : "PLAYER 1 WINS!");
                turnLabel.setText(winner);
                p1ButtonPanel.setVisible(false);
                p2ButtonPanel.setVisible(false);
            }
        } else {
            // ── Active round ───────────────────────────────────────────────────
            if (gameMode == GameMode.SURVIVAL) {
                turnLabel.setText("Round " + survivalRound + (botIsThinking ? " — BOT IS THINKING…" : " — YOUR TURN"));
            } else if (gameMode == GameMode.PVB) {
                turnLabel.setText(isP1Turn
                        ? (botIsThinking ? "BOT IS THINKING…" : "YOUR TURN")
                        : "BOT IS THINKING…");
            } else {
                turnLabel.setText(isP1Turn ? "PLAYER 1'S TURN" : "PLAYER 2'S TURN");
            }

            // In PVB/Survival, only show P1 buttons on P1's turn (and not while bot thinks).
            boolean isBotMode = (gameMode == GameMode.PVB || gameMode == GameMode.SURVIVAL);
            if (isBotMode) {
                p1ButtonPanel.setVisible(isP1Turn && !botIsThinking);
                p2ButtonPanel.setVisible(false); // Bot never shows buttons
            } else {
                p1ButtonPanel.setVisible(isP1Turn);
                p2ButtonPanel.setVisible(!isP1Turn);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI positioning  (unchanged logic; survival labels added)
    // ─────────────────────────────────────────────────────────────────────────

    private void repositionUI() {
        int feetX1 = p1SpriteX + getPlayerDrawWidth() / 2;
        int feetY1 = p1SpriteY + getPlayerDrawHeight();
        int feetX2 = p2SpriteX + getEnemyDrawWidth()  / 2;
        int feetY2 = p2SpriteY + getEnemyDrawHeight();

        int labelW = 160, labelH = 30;
        int btnW   = tileSize * 2, btnH = 40;
        int gap    = 5;

        if (p1HPLabel    != null) p1HPLabel   .setBounds(feetX1 - labelW / 2, feetY1 + gap, labelW, labelH);
        if (p2HPLabel    != null) p2HPLabel   .setBounds(feetX2 - labelW / 2, feetY2 + gap, labelW, labelH);
        if (p1ButtonPanel != null) p1ButtonPanel.setBounds(feetX1 - btnW / 2, feetY1 + gap + labelH + gap, btnW, btnH);
        if (p2ButtonPanel != null) p2ButtonPanel.setBounds(feetX2 - btnW / 2, feetY2 + gap + labelH + gap, btnW, btnH);
        if (p1HealthBar  != null) p1HealthBar .setBounds(feetX1 - barW / 2, p1SpriteY - 20, barW, barH);
        if (p2HealthBar  != null) p2HealthBar .setBounds(feetX2 - barW / 2, p2SpriteY - 20, barW, barH);

        // [AI/SURVIVAL] Round / score labels — top-right corner
        if (roundLabel != null) roundLabel.setBounds(screenWidth - 220, 60, 200, 30);
        if (scoreLabel != null) scoreLabel.setBounds(screenWidth - 220, 95, 200, 30);

        // [AI/SURVIVAL] Next Round button — centred mid-screen
        if (nextRoundBtn != null) {
            int nbW = 260, nbH = 50;
            nextRoundBtn.setBounds(screenWidth / 2 - nbW / 2, screenHeight / 2 + 40, nbW, nbH);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // All methods below are UNCHANGED from the original GamePanel.
    // ─────────────────────────────────────────────────────────────────────────

    private List<BufferedImage> loadFrames(CharacterDef def)      { return loadAnimationFrames(def.idleAnimation); }
    private List<BufferedImage> loadDeadFrames(CharacterDef def)  { return loadAnimationFrames(def.deadAnimation); }
    private List<BufferedImage> loadHurtFrames(CharacterDef def)  { return loadAnimationFrames(def.hurtAnimation); }

    private List<List<BufferedImage>> loadSkillAnimations(CharacterDef def) {
        List<List<BufferedImage>> animations = new ArrayList<>();
        for (int skillID = 1; skillID <= 3; skillID++) {
            String path = def.getSkillSpritePath(skillID);
            if (path == null || path.isBlank()) { animations.add(List.of()); continue; }
            animations.add(loadAnimationFrames(new CharacterDef.AnimationDef(path, DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_SKILL_DELAY_MS)));
        }
        return animations;
    }

    private List<BufferedImage> loadAnimationFrames(CharacterDef.AnimationDef animation) {
        List<BufferedImage> frames = new ArrayList<>();
        try {
            URL resource = getClass().getResource(animation.sheetPath);
            if (resource == null) { System.err.println("Missing sprite sheet: " + animation.sheetPath); return frames; }
            BufferedImage sheet = ImageIO.read(resource);
            if (sheet == null)   { System.err.println("Could not decode sprite sheet: " + animation.sheetPath); return frames; }
            int columns = sheet.getWidth()  / animation.frameWidth;
            int rows    = sheet.getHeight() / animation.frameHeight;
            for (int row = 0; row < rows; row++)
                for (int col = 0; col < columns; col++)
                    frames.add(sheet.getSubimage(col * animation.frameWidth, row * animation.frameHeight, animation.frameWidth, animation.frameHeight));
        } catch (Exception e) { System.err.println("Failed to load sprite sheet: " + e.getMessage()); }
        return frames;
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

    private void refreshSkillButtonLabels() {
        applySkillNamesToButtons(currentPlayerDef, p1SkillButtons);
        applySkillNamesToButtons(currentEnemyDef,  p2SkillButtons);
    }

    private void applySkillNamesToButtons(CharacterDef character, List<JButton> buttons) {
        if (character == null || buttons.isEmpty()) return;
        for (int i = 0; i < buttons.size(); i++) buttons.get(i).setText(character.getSkillName(i + 1));
    }

    private int getPlayerDrawWidth()  { return currentPlayerDef != null ? currentPlayerDef.drawWidth  : DEFAULT_DRAW_WIDTH;  }
    private int getPlayerDrawHeight() { return currentPlayerDef != null ? currentPlayerDef.drawHeight : DEFAULT_DRAW_HEIGHT; }
    private int getEnemyDrawWidth()   { return currentEnemyDef  != null ? currentEnemyDef.drawWidth   : DEFAULT_DRAW_WIDTH;  }
    private int getEnemyDrawHeight()  { return currentEnemyDef  != null ? currentEnemyDef.drawHeight  : DEFAULT_DRAW_HEIGHT; }

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
            activePlayerSkillOffsetX = currentPlayerDef != null ? currentPlayerDef.getSkillForwardOffsetX(skillID) : 0;
            playerSkillTimer = new Timer(DEFAULT_SKILL_DELAY_MS, null);
            playerSkillTimer.addActionListener(e -> {
                if (playerSkillFrameIndex < activePlayerSkillFrames.size() - 1) { playerSkillFrameIndex++; }
                else { stopSkillAnimation(true); callback.run(); }
                repaint();
            });
            playerSkillTimer.start();
        } else {
            stopSkillAnimation(false);
            activeEnemySkillFrames  = frames;
            enemySkillFrameIndex    = 0;
            isEnemySkillAnimating   = true;
            int forwardOffset       = currentEnemyDef != null ? currentEnemyDef.getSkillForwardOffsetX(skillID) : 0;
            activeEnemySkillOffsetX = -forwardOffset;
            enemySkillTimer = new Timer(DEFAULT_SKILL_DELAY_MS, null);
            enemySkillTimer.addActionListener(e -> {
                if (enemySkillFrameIndex < activeEnemySkillFrames.size() - 1) { enemySkillFrameIndex++; }
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
            activePlayerSkillOffsetX = 0;  activePlayerSkillFrames = List.of();
        } else {
            if (enemySkillTimer != null)  { enemySkillTimer.stop();  enemySkillTimer  = null; }
            isEnemySkillAnimating = false; enemySkillFrameIndex = 0;
            activeEnemySkillOffsetX = 0;  activeEnemySkillFrames = List.of();
        }
    }

    private void startProjectileAnimation(boolean isPlayerOne, int skillID) {
        if (skillID != 2 && skillID != 3) return;
        CharacterDef actor = isPlayerOne ? currentPlayerDef : currentEnemyDef;
        if (actor == null || !"Dark Wizard".equalsIgnoreCase(actor.name)) return;
        List<BufferedImage> frames = loadDarkWizardProjectileFrames(skillID);
        if (frames.isEmpty()) return;
        stopProjectileAnimation();
        int attackerX = isPlayerOne ? p1SpriteX : p2SpriteX;
        int attackerY = isPlayerOne ? p1SpriteY : p2SpriteY;
        int attackerWidth  = isPlayerOne ? getPlayerDrawWidth()  : getEnemyDrawWidth();
        int attackerHeight = isPlayerOne ? getPlayerDrawHeight() : getEnemyDrawHeight();
        int targetX   = isPlayerOne ? p2SpriteX : p1SpriteX;
        int targetWidth = isPlayerOne ? getEnemyDrawWidth() : getPlayerDrawWidth();
        int projW = DARK_WIZARD_PROJECTILE_DRAW_SIZE;
        int projH = DARK_WIZARD_PROJECTILE_DRAW_SIZE;
        projectileDirection    = attackerX <= targetX ? 1 : -1;
        projectileIsPlayerOne  = isPlayerOne;
        projectileX            = attackerX + (attackerWidth / 2) - (projW / 2);
        projectileY            = attackerY + (attackerHeight / 2) - (projH / 2) + DARK_WIZARD_PROJECTILE_VERTICAL_OFFSET;
        activeProjectileFrames = frames;
        projectileFrameIndex   = 0;
        isProjectileAnimating  = true;
        int stopBoundary = projectileDirection > 0
                ? (targetX + (targetWidth / 2)) - (projW / 2)
                : (targetX + (targetWidth / 2)) + (projW / 2);
        projectileTimer = new Timer(DEFAULT_SKILL_DELAY_MS, null);
        projectileTimer.addActionListener(e -> {
            if (!isProjectileAnimating || activeProjectileFrames.isEmpty()) { stopProjectileAnimation(); return; }
            projectileFrameIndex = (projectileFrameIndex + 1) % activeProjectileFrames.size();
            projectileX += projectileDirection * DARK_WIZARD_PROJECTILE_SPEED;
            if ((projectileDirection > 0 && projectileX >= stopBoundary)
                    || (projectileDirection < 0 && projectileX <= stopBoundary)) stopProjectileAnimation();
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
        List<BufferedImage> frames = loadAnimationFrames(new CharacterDef.AnimationDef(sheetPath, 64, 64, DEFAULT_SKILL_DELAY_MS));
        projectileAnimationCache.put(sheetPath, frames);
        return frames;
    }

    private void stopProjectileAnimation() {
        if (projectileTimer != null) { projectileTimer.stop(); projectileTimer = null; }
        isProjectileAnimating  = false; projectileFrameIndex = 0;
        projectileX = 0; projectileY = 0; projectileDirection = 1;
        projectileIsPlayerOne  = true; activeProjectileFrames = List.of();
    }

    private void scheduleHurtTimeline(boolean attackerIsPlayerOne, int skillID) {
        CharacterDef attacker = attackerIsPlayerOne ? currentPlayerDef : currentEnemyDef;
        if (attacker == null) return;
        int castDurationMs      = getSkillCastDurationMs(attackerIsPlayerOne, skillID);
        int configuredBufferMs  = (int) Math.round(attacker.getSkillHurtTriggerBufferSeconds(skillID) * 1000.0);
        int effectiveBufferMs   = Math.max(0, Math.min(configuredBufferMs, castDurationMs));
        int hurtDurationMs      = Math.max(1, (castDurationMs - effectiveBufferMs) + POST_ATTACK_HURT_MS);
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
                if (!playerHurtFrames.isEmpty()) playerHurtFrameIndex = (playerHurtFrameIndex + 1) % playerHurtFrames.size();
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
                if (!enemyHurtFrames.isEmpty()) enemyHurtFrameIndex = (enemyHurtFrameIndex + 1) % enemyHurtFrames.size();
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
        List<CharacterDataLoader.CharacterConfig> configs = CharacterDataLoader.loadCharacterConfigs("/assets/data/characters.json");
        List<CharacterDef> defs = new ArrayList<>();
        for (CharacterDataLoader.CharacterConfig config : configs) {
            defs.add(new CharacterDef(
                    config.name, config.backstory,
                    config.skill1Name, config.skill2Name, config.skill3Name,
                    config.skill1Description, config.skill2Description, config.skill3Description,
                    config.skill1Type, config.skill2Type, config.skill3Type,
                    config.skill1SpritePath, config.skill2SpritePath, config.skill3SpritePath,
                    config.skill1ForwardOffsetX, config.skill2ForwardOffsetX, config.skill3ForwardOffsetX,
                    config.skill1HurtTriggerBufferSeconds, config.skill2HurtTriggerBufferSeconds, config.skill3HurtTriggerBufferSeconds,
                    new CharacterDef.AnimationDef(config.idleSpritePath,  DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_IDLE_DELAY_MS),
                    new CharacterDef.AnimationDef(config.hurtSpritePath,  DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_HURT_DELAY_MS),
                    new CharacterDef.AnimationDef(config.deathSpritePath, DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_DEAD_DELAY_MS),
                    DEFAULT_DRAW_WIDTH, DEFAULT_DRAW_HEIGHT
            ));
        }
        if (!defs.isEmpty()) return List.copyOf(defs);

        // ── Built-in fallback characters (identical to originals) ─────────────
        return List.of(
                new CharacterDef("Light Mage", "A former temple guardian who forged sacred combat arts to protect frontier villages.",
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
                new CharacterDef("Idk Magician", "A wandering arcane prodigy who channels unstable spellcraft in battle.",
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
                        DEFAULT_DRAW_WIDTH, DEFAULT_DRAW_HEIGHT)
        );
    }
}
