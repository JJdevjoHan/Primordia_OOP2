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

import assets.Utility.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SurivivalGamePanel extends JPanel {

    private final int tileSize     = 128;
    private final int screenWidth  = tileSize * 12;
    private final int screenHeight = tileSize * 7;
    private static final int DEFAULT_DRAW_WIDTH       = 480;
    private static final int DEFAULT_DRAW_HEIGHT      = 480;
    private static final int DEFAULT_FRAME_SIZE       = 128;
    private static final int DEFAULT_IDLE_DELAY_MS    = 120;
    private static final int DEFAULT_DEAD_DELAY_MS    = 150;
    private static final int DEFAULT_SKILL_DELAY_MS   = 90;
    private static final int DEFAULT_HURT_DELAY_MS    = 90;
    private static final int POST_ATTACK_HURT_MS      = 600;
    private static final int BARS_TOP_Y               = 84;
    private static final int WIND_SKILL2_P2_LEFT_NUDGE_X = 60;
    private static final int WIND_SKILL3_FEET_OFFSET_Y   = 0;
    private static final double WIND_SKILL3_SCALE        = 1.18;
    private static final int NATURE_FORM_FREEZE_FRAME_ONE_BASED = 5;
    private static final int NATURE_FORM_RELEASE_START_FRAME_ONE_BASED = 6;
    private static final String NATURE_SHOT_SHEET_PATH = "/src/assets/spritesheet/Nature Wizard/Shot-Sheet.png";
    private static final String NATURE_DART_SHEET_PATH = "/src/assets/spritesheet/Nature Wizard/Dart.png";
    private static final int NATURE_ALT_FRAME_SIZE = 128;
    private static final int NATURE_DART_DRAW_SIZE = 144;
    private static final int NATURE_DART_SPAWN_OFFSET_X = 80;
    private static final int DEFAULT_PROJECTILE_DRAW_SIZE       = 144;
    private static final int DEFAULT_PROJECTILE_VERTICAL_OFFSET = 50;
    private static final int DEFAULT_PROJECTILE_SPEED           = 44;

    private static final int BOT_TURN_DELAY_MS = 900;

    private static final int SURVIVAL_ROUND_HEAL_AMOUNT = 30;

    private static final int MAX_MP              = 100;
    private static final int MP_REGEN_PER_TURN   = 10;   // MP regained at the start of each turn

    private static final int[] SKILL_MP_COST = { 10, 20, 30 };

    private static final int TURN_TIME_SECONDS    = 10;
    private static final int TIMER_WARN_THRESHOLD = 3;
    private static final int SKILL_PANEL_MIN_WIDTH = 928;
    private static final int SKILL_PANEL_MAX_WIDTH = 1216;
    private static final int SKILL_PANEL_HEIGHT = 192;
    private static final int SKILL_PANEL_BOTTOM_MARGIN = 64;
    private static final int SKILL_PANEL_SIDE_MARGIN = 32;
    private static final String BATTLE_UI_BOX_PATH = "/assets/BattleUI/BattleUI_Box.png";

    private int mapPixelWidth  = screenWidth;
    private int mapPixelHeight = screenHeight;

    public static final List<CharacterDef> ALL_CHARACTERS = loadCharacterDefs();

    private Image backgroundImage;
    private Image battleUiBoxImage;
    private long battleUiBoxLastModified = -1;

    private List<BufferedImage> playerFrames     = new ArrayList<>();
    private int                 playerFrameIndex = 0;
    private Timer               playerTimer;

    private List<BufferedImage> enemyFrames     = new ArrayList<>();
    private int                 enemyFrameIndex = 0;
    private Timer               enemyTimer;

    private List<List<BufferedImage>> playerSkillAnimations   = new ArrayList<>();
    private List<List<BufferedImage>> enemySkillAnimations    = new ArrayList<>();
    private List<BufferedImage>       activePlayerSkillFrames = List.of();
    private List<BufferedImage>       activeEnemySkillFrames  = List.of();
    private int   playerSkillFrameIndex = 0;
    private int   enemySkillFrameIndex  = 0;
    private int   activePlayerSkillID   = 0;
    private int   activeEnemySkillID    = 0;
    private Timer playerSkillTimer;
    private Timer enemySkillTimer;
    private boolean isPlayerSkillAnimating = false;
    private boolean isEnemySkillAnimating  = false;
    private int activePlayerSkillOffsetX = 0;
    private int activeEnemySkillOffsetX  = 0;
    private boolean isPlayerNatureDefenseForm = false;
    private boolean isEnemyNatureDefenseForm = false;

    private final Map<String, List<BufferedImage>> projectileAnimationCache = new HashMap<>();
    private List<BufferedImage> activeProjectileFrames = List.of();
    private List<BufferedImage> projectileImpactFrames = List.of();
    private int     projectileFrameIndex  = 0;
    private int     projectileX           = 0;
    private int     projectileY           = 0;
    private int     projectileDrawWidth   = DEFAULT_PROJECTILE_DRAW_SIZE;
    private int     projectileDrawHeight  = DEFAULT_PROJECTILE_DRAW_SIZE;
    private int     projectileSpeed       = DEFAULT_PROJECTILE_SPEED;
    private int     projectileDirection   = 1;
    private boolean projectileInImpactPhase = false;
    private int     projectileLoopStartIndex = 0;
    private int     projectileLoopEndIndex = 0;
    private int     projectileImpactStartIndex = -1;
    private int     projectileImpactEndIndex = -1;
    private boolean projectileIsPlayerOne = true;
    private Timer   projectileTimer;
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
    private Timer playerHurtFlashTimer;
    private Timer enemyHurtFlashTimer;
    private boolean isPlayerHurtAnimating = false;
    private boolean isEnemyHurtAnimating  = false;
    private boolean playerHurtFlashing = false;
    private boolean enemyHurtFlashing = false;

    private CharacterDef        currentPlayerDef, currentEnemyDef;
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

    //HP
    private int p1HP = 100, p2HP = 100;
    private int p1MP = MAX_MP, p2MP = MAX_MP;
    private boolean isP1Turn = true;

    private GameMode         gameMode   = GameMode.PVP;
    private BotAI.Difficulty difficulty = BotAI.Difficulty.NORMAL;

    // Survival score (rounds use RoundManager just like GamePanel)
    private int survivalScore = 0;

    private boolean botIsThinking = false;


    private int   turnSecondsLeft = TURN_TIME_SECONDS;
    private Timer countdownTimer;
    private CircularTimerLabel countdownLabel;

    private JPanel              skillButtonPanel;
    private final List<JButton> skillButtons = new ArrayList<>();
    private JLabel              skillPanelTitle;
    private JLabel              turnLabel, p1HPLabel, p2HPLabel;

    private JLabel  scoreLabel;

    // HP bars
    private GameBar p1HealthBar, p2HealthBar;
    // MP bars
    private GameBar p1MpBar, p2MpBar;
    // Rounds won indicators
    private RoundsWonIndicator p1RoundsWon, p2RoundsWon;

    int barW = 220;
    int barH = 18;

    private GameWindow   window;
    private RoundManager roundManager;
    private JButton backbutton;
    private JButton exitButton;

    private final SoundManager survivalBGM = new SoundManager();

    public SurivivalGamePanel() {
        this(new GameWindow(), 0, 1, GameMode.PVP, BotAI.Difficulty.NORMAL);
    }

    public SurivivalGamePanel(GameWindow window, int playerCharacterIndex, int enemyCharacterIndex,
                              GameMode mode, BotAI.Difficulty difficulty) {
        this.gameMode   = mode;
        this.difficulty = difficulty;
        this.window     = window;

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

        backbutton = new BackButton().createBackButton(window, this);
        add(backbutton);

        exitButton = new ExitButton().createExitButton(this);
        add(exitButton);


        loadMapData("/assets/maps/map1.tmx");

        int safePlayerIndex = sanitizeCharacterIndex(playerCharacterIndex, 0);
        int safeEnemyIndex  = sanitizeCharacterIndex(enemyCharacterIndex, safePlayerIndex == 0 ? 1 : 0);
        setCharacters(safePlayerIndex, safeEnemyIndex);

        Font boldFont = FontManager.getFont(40).deriveFont(Font.BOLD);
        Font noneBold = FontManager.getFont(22).deriveFont(Font.BOLD);

        turnLabel = new JLabel("PLAYER 1'S TURN", SwingConstants.CENTER);
        turnLabel.setFont(boldFont);
        turnLabel.setBounds(0, 10, screenWidth, 50);
        turnLabel.setVisible(false);
        this.add(turnLabel);

        //LABEL SA GAMEBARS
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

        skillButtonPanel = createSkillUI();
        this.add(skillButtonPanel);

        //GAMEBARRR
        p1HealthBar = new GameBar(100, Color.GREEN,  GameBar.BarType.HP);
        p2HealthBar = new GameBar(100, Color.RED,    GameBar.BarType.HP);
        this.add(p1HealthBar);
        this.add(p2HealthBar);
        p2HealthBar.setFillFromRight(true);

        p1MpBar = new GameBar(MAX_MP, new Color(40, 100, 180), GameBar.BarType.MP);
        p2MpBar = new GameBar(MAX_MP, new Color(40, 100, 180), GameBar.BarType.MP);
        this.add(p1MpBar);
        this.add(p2MpBar);
        p2MpBar.setFillFromRight(true);

        // ── Rounds Won Indicators ─────────────────────────────────────────────
        p1RoundsWon = new RoundsWonIndicator(Color.GREEN, true);
        p2RoundsWon = new RoundsWonIndicator(Color.RED);
        this.add(p1RoundsWon);
        this.add(p2RoundsWon);


        scoreLabel = new JLabel("Score: 0", SwingConstants.CENTER);
        scoreLabel.setFont(FontManager.getFont(22).deriveFont(Font.BOLD));
        scoreLabel.setForeground(new Color(180, 255, 180));
        scoreLabel.setVisible(gameMode == GameMode.SURVIVAL);
        this.add(scoreLabel);

        //10secondcountdown
        countdownLabel = new CircularTimerLabel("10");
        countdownLabel.setFont(FontManager.getFont(38).deriveFont(Font.BOLD));
        this.add(countdownLabel);

        //roundmanager
        roundManager = new RoundManager(
                mode,

                // ROUND START
                (round, p1Wins, p2Wins) -> {
                    System.out.println("Round " + round + " Start!");

                    resetNatureDefenseForms();
                    if (gameMode == GameMode.SURVIVAL && round > 1) {
                        p1HP = Math.min(100, p1HP + SURVIVAL_ROUND_HEAL_AMOUNT);
                        pickNewSurvivalEnemy();
                    } else {
                        p1HP = 100;
                    }
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
                    if (gameMode == GameMode.SURVIVAL && p1WonRound) {
                        int hpBonus    = p1HP;
                        int roundBonus = round * 10;
                        survivalScore += 100 + hpBonus + roundBonus;
                        scoreLabel.setText("Score: " + survivalScore);
                    }

                    String msg = p1WonRound ? "PLAYER 1 WINS ROUND " : "PLAYER 2 WINS ROUND ";
                    turnLabel.setText(msg + round);
                    if (p1WonRound) {
                        p1RoundsWon.setWins(roundManager.getP1Wins());
                    } else {
                        p2RoundsWon.setWins(roundManager.getP2Wins());
                    }
                    repaint();

                    new Timer(2000, e -> {
                        ((Timer) e.getSource()).stop();
                        roundManager.advanceRound();
                    }).start();
                },

                // MATCH END
                (p1WonMatch, p1Wins, p2Wins, totalRounds) -> {
                    stopTurnTimer();

                    String msg;
                    if (gameMode == GameMode.SURVIVAL) {
                        msg = p1WonMatch
                                ? "YOU WIN!  Final Score: " + survivalScore
                                : "Game Over!  Final Score: " + survivalScore;
                    } else if (gameMode == GameMode.PVB) {
                        msg = p1WonMatch ? "YOU WIN!" : "BOT WINS!";
                    } else {
                        msg = p1WonMatch ? "PLAYER 1 WINS MATCH!" : "PLAYER 2 WINS MATCH!";
                    }
                    turnLabel.setText(msg);

                    skillButtonPanel.setVisible(false);
                });

        refreshSkillButtons();
        repositionUI();
        updateGameState();
        roundManager.startMatch();
        survivalBGM.setFile(9);
        survivalBGM.loop();
    }

    private static int mpCostFor(int skillID) {
        if (skillID < 1 || skillID > SKILL_MP_COST.length) return 0;
        return SKILL_MP_COST[skillID - 1];
    }


    private boolean hasEnoughMP(boolean actingPlayerOne, int skillID) {
        int available = actingPlayerOne ? p1MP : p2MP;
        return available >= mpCostFor(skillID);
    }


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

    /** Refresh which skill buttons are enabled based on current MP. */
    private void refreshSkillButtonMPState() {
        boolean actingP1 = isP1Turn;
        int currentMP = actingP1 ? p1MP : p2MP;

        for (int i = 0; i < skillButtons.size(); i++) {
            int cost = mpCostFor(i + 1);
            boolean canAfford = currentMP >= cost;
            skillButtons.get(i).setEnabled(canAfford);
            // Visual cue: dim the button text when unaffordable
            skillButtons.get(i).setForeground(canAfford ? Color.BLACK : new Color(140, 140, 140));
        }
    }

    private void startTurnTimer() {
        stopTurnTimer();

        boolean isBotTurn = (gameMode == GameMode.PVB || gameMode == GameMode.SURVIVAL) && !isP1Turn;
        if (isBotTurn) return;

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
        if (!roundManager.isRoundInProgress()) return;

        String skippedPlayer = isP1Turn ? "PLAYER 1" : "PLAYER 2";
        // Still grant regen to the player whose turn was skipped
        if (isP1Turn) {
            p1MP = Math.min(MAX_MP, p1MP + MP_REGEN_PER_TURN);
        } else {
            p2MP = Math.min(MAX_MP, p2MP + MP_REGEN_PER_TURN);
        }

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

        maybeTriggerBotTurn();
    }

    private void refreshCountdownLabel() {
        if (countdownLabel == null) return;
        countdownLabel.setText(String.valueOf(Math.max(0, turnSecondsLeft)));
        boolean urgent = turnSecondsLeft <= TIMER_WARN_THRESHOLD;
        countdownLabel.setForeground(urgent ? new Color(190, 20, 20) : new Color(20, 20, 20));
        countdownLabel.repaint();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        // Force a full UI refresh on first attach to prevent initial HUD misplacement.
        SwingUtilities.invokeLater(() -> {
            updateP1DrawFromSpawn();
            updateP2DrawFromSpawn();
            repositionUI();
            revalidate();
            repaint();
        });
    }

    @Override
    public void doLayout() {
        super.doLayout();
        int margin = 20;
        for (Component c : getComponents()) {
            if (c instanceof JButton && ((JButton) c).getText().equals("Back")) {
                c.setBounds(getWidth() - 120, getHeight() - 70, 100, 40);
            }
        }

        if (exitButton != null) {
            exitButton.setBounds(getWidth() - 50 - margin, margin, 40, 40);
        }
    }


    public void setGameMode(GameMode mode) {
        this.gameMode = mode;
        scoreLabel.setVisible(mode == GameMode.SURVIVAL);
        updateGameState();
    }

    public void setDifficulty(BotAI.Difficulty diff) { this.difficulty = diff; }

    public GameMode getGameMode()      { return gameMode; }
    public int      getSurvivalScore() { return survivalScore; }


    private int sanitizeCharacterIndex(int requestedIndex, int fallbackIndex) {
        if (ALL_CHARACTERS.isEmpty()) return 0;
        if (requestedIndex >= 0 && requestedIndex < ALL_CHARACTERS.size()) return requestedIndex;
        if (fallbackIndex  >= 0 && fallbackIndex  < ALL_CHARACTERS.size()) return fallbackIndex;
        return 0;
    }

    private JPanel createSkillUI() {
        JPanel panel = new JPanel(new BorderLayout(4, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (battleUiBoxImage != null) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.drawImage(battleUiBoxImage, 0, 0, getWidth(), getHeight(), null);
                    g2.dispose();
                }
            }
        };
        battleUiBoxImage = getBattleUiBoxImage();
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));

        skillPanelTitle = new JLabel("", SwingConstants.CENTER);
        skillPanelTitle.setFont(FontManager.getFont(34).deriveFont(Font.BOLD));
        skillPanelTitle.setForeground(new Color(0x52, 0x33, 0x3F));
        skillPanelTitle.setOpaque(false);
        skillPanelTitle.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        panel.add(skillPanelTitle, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(1, 3, 4, 4));
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        for (int i = 0; i < 3; i++) {
            JButton btn = new BattleUISkillButton("Skill " + (i + 1));
            btn.setToolTipText("MP Cost: " + SKILL_MP_COST[i]);
            int skillID = i + 1;
            btn.addActionListener(e -> executeSkill(skillID));
            grid.add(btn);
            skillButtons.add(btn);
        }

        panel.add(grid, BorderLayout.CENTER);
        return panel;
    }

    private Image getBattleUiBoxImage() {
        try {
            Path imagePath = Paths.get("OOP2_game Testing", "src", "assets", "BattleUI", "BattleUI_Box.png");
            if (Files.exists(imagePath)) {
                long lm = Files.getLastModifiedTime(imagePath).toMillis();
                if (lm != battleUiBoxLastModified || battleUiBoxImage == null) {
                    battleUiBoxLastModified = lm;
                    battleUiBoxImage = new ImageIcon(imagePath.toString()).getImage();
                }
                return battleUiBoxImage;
            }
        } catch (Exception ignored) {}
        try {
            URL resourceUrl = getClass().getResource(BATTLE_UI_BOX_PATH);
            if (resourceUrl != null) return new ImageIcon(resourceUrl).getImage();
        } catch (Exception ignored) {}
        return null;
    }

    private void refreshSkillButtons() {
        CharacterDef activeCharacter = isP1Turn ? currentPlayerDef : currentEnemyDef;
        refreshSkillPanelTitle(activeCharacter);
        applySkillNamesToButtons(activeCharacter, skillButtons);
        refreshSkillButtonMPState();
    }

    private void refreshSkillPanelTitle(CharacterDef activeCharacter) {
        if (skillPanelTitle == null) return;
        String actor = isP1Turn ? (gameMode == GameMode.SURVIVAL ? "YOUR" : "PLAYER 1") : "BOT";
        if (activeCharacter != null && activeCharacter.name != null && !activeCharacter.name.isBlank()) {
            skillPanelTitle.setText(actor + " MOVE - " + activeCharacter.name);
        } else {
            skillPanelTitle.setText(actor + " MOVE");
        }
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        if (backgroundImage != null)
            g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);

        // Player
        if (p1HP <= 0) {
            if (!playerDeadFrames.isEmpty())
                g2.drawImage(playerDeadFrames.get(playerDeadFrameIndex),
                        p1SpriteX, p1SpriteY, getPlayerDrawWidth(), getPlayerDrawHeight(), this);
        } else if (isPlayerHurtAnimating && !playerHurtFrames.isEmpty()) {
            BufferedImage hurtFrame = playerHurtFrames.get(playerHurtFrameIndex);
            if (playerHurtFlashing) {
                hurtFrame = applyRedTintToNonTransparent(hurtFrame);
            }
            g2.drawImage(hurtFrame,
                    p1SpriteX, p1SpriteY, getPlayerDrawWidth(), getPlayerDrawHeight(), this);
        } else if (isTargetOverlayAttack3(true) && shouldHideCasterDuringOverlaySkill3(true)) {
            // Wind Wizard skill 3 hides the caster while the overlay animation plays.
        } else if (isPlayerSkillAnimating && !activePlayerSkillFrames.isEmpty() && !isTargetOverlayAttack3(true)) {
            BufferedImage skillFrame = activePlayerSkillFrames.get(playerSkillFrameIndex);
            int skillDrawHeight = getPlayerDrawHeight();
            int skillDrawWidth = getSkillDrawWidth(skillFrame, skillDrawHeight, getPlayerDrawWidth());
            g2.drawImage(skillFrame,
                p1SpriteX + activePlayerSkillOffsetX, p1SpriteY, skillDrawWidth, skillDrawHeight, this);
        } else if (isPlayerNatureDefenseForm) {
            BufferedImage stanceFrame = getNatureDefensePoseFrame(true);
            if (stanceFrame != null) {
                int stanceDrawHeight = getPlayerDrawHeight();
                int stanceDrawWidth = getSkillDrawWidth(stanceFrame, stanceDrawHeight, getPlayerDrawWidth());
                g2.drawImage(stanceFrame,
                        p1SpriteX, p1SpriteY, stanceDrawWidth, stanceDrawHeight, this);
            }
        } else if (!playerFrames.isEmpty()) {
            g2.drawImage(playerFrames.get(playerFrameIndex),
                    p1SpriteX, p1SpriteY, getPlayerDrawWidth(), getPlayerDrawHeight(), this);
        }

        // Enemy (mirrored)
        if (p2HP <= 0) {
            if (!enemyDeadFrames.isEmpty())
                g2.drawImage(enemyDeadFrames.get(enemyDeadFrameIndex),
                        p2SpriteX + getEnemyDrawWidth(), p2SpriteY, -getEnemyDrawWidth(), getEnemyDrawHeight(), this);
        } else if (isEnemyHurtAnimating && !enemyHurtFrames.isEmpty()) {
            BufferedImage hurtFrame = enemyHurtFrames.get(enemyHurtFrameIndex);
            if (enemyHurtFlashing) {
                hurtFrame = applyRedTintToNonTransparent(hurtFrame);
            }
            g2.drawImage(hurtFrame,
                    p2SpriteX + getEnemyDrawWidth(), p2SpriteY, -getEnemyDrawWidth(), getEnemyDrawHeight(), this);
        } else if (isTargetOverlayAttack3(false) && shouldHideCasterDuringOverlaySkill3(false)) {
            // Wind Wizard skill 3 hides the caster while the overlay animation plays.
        } else if (isEnemySkillAnimating && !activeEnemySkillFrames.isEmpty() && !isTargetOverlayAttack3(false)) {
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
        } else if (isEnemyNatureDefenseForm) {
            BufferedImage stanceFrame = getNatureDefensePoseFrame(false);
            if (stanceFrame != null) {
                int stanceDrawHeight = getEnemyDrawHeight();
                int stanceDrawWidth = getSkillDrawWidth(stanceFrame, stanceDrawHeight, getEnemyDrawWidth());
                g2.drawImage(stanceFrame,
                        p2SpriteX + stanceDrawWidth, p2SpriteY,
                        -stanceDrawWidth, stanceDrawHeight, this);
            }
        } else if (!enemyFrames.isEmpty()) {
            g2.drawImage(enemyFrames.get(enemyFrameIndex),
                    p2SpriteX + getEnemyDrawWidth(), p2SpriteY, -getEnemyDrawWidth(), getEnemyDrawHeight(), this);
        }

        // Wind skill 3 overlay is drawn last so it appears above both characters.
        if (isTargetOverlayAttack3(true) && !activePlayerSkillFrames.isEmpty()) {
            drawAnchoredSkillFrame(g2, activePlayerSkillFrames.get(playerSkillFrameIndex),
                getEnemyFeetAnchorX(), getEnemyFeetAnchorY(),
                getPlayerDrawWidth(), getPlayerDrawHeight(), true);
        }
        if (isTargetOverlayAttack3(false) && !activeEnemySkillFrames.isEmpty()) {
            drawAnchoredSkillFrame(g2, activeEnemySkillFrames.get(enemySkillFrameIndex),
                getPlayerFeetAnchorX(), getPlayerFeetAnchorY(),
                getEnemyDrawWidth(), getEnemyDrawHeight(), false);
        }

        // Projectile
        if (isProjectileAnimating && !activeProjectileFrames.isEmpty()) {
            BufferedImage frame = activeProjectileFrames.get(projectileFrameIndex);
            if (frame != null) {
                int dw = projectileDrawWidth, dh = projectileDrawHeight;
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
                loadSpawnPoint(document, "Spawn_Player1",  true);
                loadSpawnPoint(document, "Spawn_Player2", false);
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
        isPlayerNatureDefenseForm = false;
        isEnemyNatureDefenseForm = false;

        refreshSkillButtons();
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

    private void setEnemyCharacter(int enemyIdx) {
        if (enemyTimer != null && enemyTimer.isRunning()) enemyTimer.stop();
        resetNatureDefenseForms();
        stopProjectileAnimation();
        stopHurtTimeline(false);

        CharacterDef enemyDef = ALL_CHARACTERS.get(enemyIdx);
        currentEnemyDef = enemyDef;
        isEnemyNatureDefenseForm = false;

        refreshSkillButtons();
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

    private void pickNewSurvivalEnemy() {
        int playerIdx = ALL_CHARACTERS.indexOf(currentPlayerDef);
        int newEnemyIdx;
        if (ALL_CHARACTERS.size() > 1) {
            do {
                newEnemyIdx = (int) (Math.random() * ALL_CHARACTERS.size());
            }
            while (newEnemyIdx == playerIdx);
        } else {
            newEnemyIdx = 0;
        }
        setEnemyCharacter(newEnemyIdx);
    }

    public void executeSkill(int skillID) {
        if (!roundManager.isRoundInProgress()) return;
        if (p1HP <= 0 || p2HP <= 0) return;
        if (botIsThinking) return;

        // ── Prevent casting while opponent is animating ────────────────────────────────────────
        if (isP1Turn && (isEnemySkillAnimating || isProjectileAnimating)) {
            return;
        }

        if (!hasEnoughMP(isP1Turn, skillID)) {
            // Flash the turn label to tell the player they can't afford it
            String original = turnLabel.getText();
            turnLabel.setText("Not enough MP! (Need " + mpCostFor(skillID) + " MP)");
            Timer restore = new Timer(1200, e -> turnLabel.setText(original));
            restore.setRepeats(false);
            restore.start();
            return;
        }

        // Stop the countdown so it cannot fire mid-animation
        stopTurnTimer();

        boolean actingPlayerOne  = isP1Turn;
        CharacterDef actor       = actingPlayerOne ? currentPlayerDef : currentEnemyDef;
        CharacterDef.DefenseFormDef defenseForm = actor != null ? actor.defenseForm : null;
        boolean isDefenseFormToggleSkill = defenseForm != null && skillID == defenseForm.toggleSkillSlot;
        boolean isDefenseFormAltSkill = defenseForm != null
                && skillID == defenseForm.altSkillSlot
                && isNatureDefenseFormActive(actingPlayerOne);
        boolean isDamageSkill    = actor != null && "damage".equalsIgnoreCase(actor.getSkillType(skillID));
        CharacterDef.ProjectileDef projectileDef = actor != null ? actor.getSkillProjectile(skillID) : null;
        if (isDefenseFormAltSkill) {
            projectileDef = buildDefenseFormAltProjectileDef(defenseForm);
        }
        boolean hasProjectileAnimation = projectileDef != null;
        boolean startProjectileDuringCast = hasProjectileAnimation && projectileDef.startDuringCast;
        boolean holdCastUntilProjectileDone = hasProjectileAnimation && projectileDef.beam && !startProjectileDuringCast;

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

        // Spend MP for the attacker and regen for the defender
        spendAndRegenMP(actingPlayerOne, skillID);

        if (isDamageSkill && !hasProjectileAnimation) {
            scheduleHurtTimeline(actingPlayerOne, skillID);
        }

        CharacterDef.ProjectileDef activeProjectileDef = projectileDef;
        if (isDefenseFormToggleSkill) {
            stopProjectileAnimation();
            if (isNatureDefenseFormActive(actingPlayerOne)) {
                playNatureDefenseFormExitAnimation(actingPlayerOne, defenseForm);
            } else {
                playNatureDefenseFormEnterAnimation(actingPlayerOne, defenseForm);
            }
        } else if (hasProjectileAnimation) {
            int projectileSpawnFrame = getProjectileSpawnFrame(actor, skillID);
            if (startProjectileDuringCast) {
                Runnable spawnProjectile = () -> startProjectileAnimation(
                        actingPlayerOne,
                        skillID,
                        activeProjectileDef,
                        isDamageSkill ? () -> startDelayedHurt(!actingPlayerOne, 0, POST_ATTACK_HURT_MS) : null,
                        null);
                if (projectileSpawnFrame > 1) {
                    scheduleProjectileSpawnAtCastFrame(projectileSpawnFrame, spawnProjectile);
                } else {
                    spawnProjectile.run();
                }
                if (isDefenseFormAltSkill) {
                    playNatureFormSkill1CastAnimation(actingPlayerOne, defenseForm, null);
                } else {
                    playSkillAnimation(actingPlayerOne, skillID, false, null);
                }
            } else {
                Runnable spawnProjectile = () -> startProjectileAnimation(
                        actingPlayerOne,
                        skillID,
                        activeProjectileDef,
                        isDamageSkill ? () -> startDelayedHurt(!actingPlayerOne, 0, POST_ATTACK_HURT_MS) : null,
                        holdCastUntilProjectileDone ? () -> stopSkillAnimation(actingPlayerOne) : null);
                if (isDefenseFormAltSkill) {
                    playNatureFormSkill1CastAnimation(actingPlayerOne, defenseForm, spawnProjectile);
                } else {
                    if (projectileSpawnFrame > 1) {
                        playSkillAnimation(actingPlayerOne, skillID, holdCastUntilProjectileDone, null);
                        scheduleProjectileSpawnAtCastFrame(projectileSpawnFrame, spawnProjectile);
                    } else {
                        playSkillAnimation(actingPlayerOne, skillID, holdCastUntilProjectileDone, spawnProjectile);
                    }
                }
            }
        } else {
            stopProjectileAnimation();
            playSkillAnimation(actingPlayerOne, skillID, false, null);
        }

        isP1Turn = !isP1Turn;
        updateGameState(); // restarts timer for the next player
        repaint();

        maybeTriggerBotTurn();
    }

    private void maybeTriggerBotTurn() {
        boolean isBotMode = (gameMode == GameMode.PVB || gameMode == GameMode.SURVIVAL);
        if (!isBotMode || isP1Turn) return;
        if (p1HP <= 0 || p2HP <= 0) return;

        botIsThinking = true;
        setPlayerButtonsEnabled(false);

        Timer botDelay = new Timer(BOT_TURN_DELAY_MS, null);
        botDelay.setRepeats(false);
        botDelay.addActionListener(e -> {
            botIsThinking = false;
            performBotAction();
        });
        botDelay.start();
    }

    private void performBotAction() {
        if (p1HP <= 0 || p2HP <= 0) { setPlayerButtonsEnabled(true); return; }

        // Bot also respects MP — try its preferred skill; fall back if unaffordable
        int preferred = BotAI.chooseSkill(p2HP, p1HP, currentEnemyDef, scaledDifficulty(), 100);
        int chosenSkill = preferred;
        if (!hasEnoughMP(false, preferred)) {
            // Fall back to the cheapest affordable skill
            chosenSkill = 0;
            for (int s = 1; s <= 3; s++) {
                if (hasEnoughMP(false, s)) { chosenSkill = s; break; }
            }
            if (chosenSkill == 0) {
                // No affordable skill — skip and regen
                p2MP = Math.min(MAX_MP, p2MP + MP_REGEN_PER_TURN);
                isP1Turn = true;
                updateGameState();
                setPlayerButtonsEnabled(true);
                return;
            }
        }

        executeSkill(chosenSkill);
        setPlayerButtonsEnabled(true);
    }

    private BotAI.Difficulty scaledDifficulty() {
        if (gameMode != GameMode.SURVIVAL) return difficulty;
        int round = roundManager.getCurrentRound();
        return switch (difficulty) {
            case EASY   -> (round >= 5) ? BotAI.Difficulty.NORMAL : BotAI.Difficulty.EASY;
            case NORMAL -> (round >= 6) ? BotAI.Difficulty.HARD   : BotAI.Difficulty.NORMAL;
            case HARD   -> BotAI.Difficulty.HARD;
        };
    }

    private void setPlayerButtonsEnabled(boolean enabled) {
        for (JButton btn : skillButtons) btn.setEnabled(enabled);
    }

    private void updateGameState() {
        p1HP = Math.max(0, Math.min(100,    p1HP));
        p2HP = Math.max(0, Math.min(100,    p2HP));
        p1MP = Math.max(0, Math.min(MAX_MP, p1MP));
        p2MP = Math.max(0, Math.min(MAX_MP, p2MP));

        // HP labels (hidden — values shown inside bars)
        p1HPLabel.setText("HP: " + p1HP);
        p2HPLabel.setText("HP: " + p2HP);

        // Update bars
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

        // Update turn label
        if (gameMode == GameMode.SURVIVAL) {
            String roundInfo = "Round " + roundManager.getCurrentRound()
                    + " | Score: " + survivalScore;
            turnLabel.setText(roundInfo + (botIsThinking ? " — BOT IS THINKING…"
                    : isP1Turn ? " — YOUR TURN" : ""));
        } else if (gameMode == GameMode.PVB) {
            turnLabel.setText(isP1Turn
                    ? (botIsThinking ? "BOT IS THINKING…" : "YOUR TURN")
                    : "BOT IS THINKING…");
        } else {
            turnLabel.setText("PLAYER 1'S TURN".equals(isP1Turn ? "PLAYER 1'S TURN" : "PLAYER 2'S TURN")
                    ? "PLAYER 1'S TURN | " + roundManager.getScoreDisplay("P1", "P2")
                    : "PLAYER 2'S TURN | " + roundManager.getScoreDisplay("P1", "P2"));
        }

        // Button visibility
        boolean isBotMode = (gameMode == GameMode.PVB || gameMode == GameMode.SURVIVAL);
        if (skillButtonPanel != null) {
            skillButtonPanel.setVisible(true);
        }
        refreshSkillButtons();
        if (isBotMode && !isP1Turn) {
            setPlayerButtonsEnabled(false);
        }

        // Reposition countdown above active player then restart timer
        if (countdownLabel != null) {
            countdownLabel.setVisible(true);
            repositionUI();
        }
        startTurnTimer();
    }

    private void repositionUI() {
        // HP labels (invisible — kept so no NPE)
        if (p1HPLabel != null) p1HPLabel.setBounds(0, 0, 0, 0);
        if (p2HPLabel != null) p2HPLabel.setBounds(0, 0, 0, 0);

        int panelW = getWidth();
        int topMargin = 84;
        int barTop = topMargin;
        int hpHeight = 28;
        int manaHeight = 18;
        int barSpacing = 6;
        int sideMargin = 30;
        int centerDiameter = 60;
        int indicatorWidth = 56;
        int indicatorGap = 8;

        int leftMaxWidth = (panelW / 2) - sideMargin - (centerDiameter / 2);
        int hpWidth = Math.max(260, leftMaxWidth - 20);
        int manaWidth = Math.max(180, hpWidth - indicatorWidth - indicatorGap);

        int leftX = sideMargin;
        int rightX = panelW - sideMargin - hpWidth;
        int centerX = (panelW - centerDiameter) / 2;

        if (p1HealthBar != null) p1HealthBar.setBounds(leftX, barTop, hpWidth, hpHeight);
        if (p1MpBar != null) p1MpBar.setBounds(leftX, barTop + hpHeight + barSpacing, manaWidth, manaHeight);
        if (p1RoundsWon != null) p1RoundsWon.setBounds(leftX + manaWidth + indicatorGap, barTop + hpHeight + barSpacing - 2, indicatorWidth, 28);

        if (p2HealthBar != null) p2HealthBar.setBounds(rightX, barTop, hpWidth, hpHeight);
        if (p2MpBar != null) p2MpBar.setBounds(rightX + indicatorWidth + indicatorGap, barTop + hpHeight + barSpacing, manaWidth, manaHeight);
        if (p2RoundsWon != null) p2RoundsWon.setBounds(rightX, barTop + hpHeight + barSpacing - 2, indicatorWidth, 28);

        if (countdownLabel != null) countdownLabel.setBounds(centerX, barTop - 4, centerDiameter, centerDiameter);

        if (skillButtonPanel != null) {
            int panelW2 = Math.max(SKILL_PANEL_MIN_WIDTH, Math.min(SKILL_PANEL_MAX_WIDTH, getWidth() - SKILL_PANEL_SIDE_MARGIN * 2));
            int panelX = (getWidth() - panelW2) / 2;
            int panelY = getHeight() - SKILL_PANEL_HEIGHT - SKILL_PANEL_BOTTOM_MARGIN;
            skillButtonPanel.setBounds(panelX, panelY, panelW2, SKILL_PANEL_HEIGHT);
        }

        if (scoreLabel != null && gameMode == GameMode.SURVIVAL)
            scoreLabel.setBounds(screenWidth - 220, 60, 200, 30);
    }

    private void refreshSkillButtonLabels() {
        refreshSkillButtons();
    }

    private void applySkillNamesToButtons(CharacterDef character, List<JButton> buttons) {
        if (character == null || buttons.isEmpty()) return;
        for (int i = 0; i < buttons.size(); i++) {
            String name = character.getSkillName(i + 1);
            String type = getSkillType(character, i + 1);
            int cost = SKILL_MP_COST[i];
            buttons.get(i).setText(name);
            buttons.get(i).putClientProperty("skillType", type != null ? type.toLowerCase() : null);
            buttons.get(i).putClientProperty("mpCost", cost);
            buttons.get(i).setToolTipText("<html>Type: " + type + "<br/>MP Cost: " + cost + "</html>");
        }
    }

    private String getSkillType(CharacterDef character, int skillID) {
        if (character == null) return "";
        return switch (skillID) {
            case 1 -> character.skill1Type;
            case 2 -> character.skill2Type;
            case 3 -> character.skill3Type;
            default -> "";
        };
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
            animations.add(loadSkillAnimationFrames(def, skillID));
        }
        return animations;
    }

    private List<BufferedImage> loadSkillAnimationFrames(CharacterDef def, int skillID) {
        String path = def.getSkillSpritePath(skillID);
        if (path == null || path.isBlank()) {
            return List.of();
        }

        int frameWidth = getSkillFrameWidth(def, skillID);
        int frameHeight = getSkillFrameHeight(def, skillID);
        List<BufferedImage> frames = new ArrayList<>(loadAnimationFrames(
                new CharacterDef.AnimationDef(path, frameWidth, frameHeight, DEFAULT_SKILL_DELAY_MS)));

        String followUpPath = def.getSkillFollowUpSpritePath(skillID);
        if (skillID == 1 && followUpPath != null && !followUpPath.isBlank() && !followUpPath.equals(path)) {
            frames.addAll(loadAnimationFrames(
                    new CharacterDef.AnimationDef(followUpPath, frameWidth, frameHeight, DEFAULT_SKILL_DELAY_MS)));
        }

        return frames;
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
        playSkillAnimation(isPlayerOne, skillID, false, onCastFinished);
    }

    private void playSkillAnimation(boolean isPlayerOne, int skillID, boolean holdLastFrame, Runnable onCastFinished) {
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
                else {
                    if (holdLastFrame) {
                        if (playerSkillTimer != null) { playerSkillTimer.stop(); playerSkillTimer = null; }
                        callback.run();
                    } else {
                        stopSkillAnimation(true);
                        callback.run();
                    }
                }
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
                else {
                    if (holdLastFrame) {
                        if (enemySkillTimer != null) { enemySkillTimer.stop(); enemySkillTimer = null; }
                        callback.run();
                    } else {
                        stopSkillAnimation(false);
                        callback.run();
                    }
                }
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

    private boolean isNatureDefenseFormActive(boolean isPlayerOne) {
        return isPlayerOne ? isPlayerNatureDefenseForm : isEnemyNatureDefenseForm;
    }

    private void setNatureDefenseFormActive(boolean isPlayerOne, boolean active) {
        if (isPlayerOne) {
            isPlayerNatureDefenseForm = active;
        } else {
            isEnemyNatureDefenseForm = active;
        }
    }

    private void resetNatureDefenseForms() {
        stopSkillAnimation(true);
        stopSkillAnimation(false);
        isPlayerNatureDefenseForm = false;
        isEnemyNatureDefenseForm = false;
    }

    private CharacterDef.DefenseFormDef getDefenseFormConfig(boolean isPlayerOne) {
        CharacterDef actor = isPlayerOne ? currentPlayerDef : currentEnemyDef;
        return actor != null ? actor.defenseForm : null;
    }

    private BufferedImage getNatureDefensePoseFrame(boolean isPlayerOne) {
        CharacterDef.DefenseFormDef defenseForm = getDefenseFormConfig(isPlayerOne);
        if (defenseForm == null) return null;
        List<List<BufferedImage>> source = isPlayerOne ? playerSkillAnimations : enemySkillAnimations;
        if (defenseForm.toggleSkillSlot < 1 || defenseForm.toggleSkillSlot > source.size()) return null;
        List<BufferedImage> frames = source.get(defenseForm.toggleSkillSlot - 1);
        if (frames == null || frames.isEmpty()) return null;
        int freezeIndex = Math.max(0, Math.min(frames.size() - 1, defenseForm.enterFreezeFrame - 1));
        return frames.get(freezeIndex);
    }

    private void playNatureDefenseFormEnterAnimation(boolean isPlayerOne, CharacterDef.DefenseFormDef defenseForm) {
        if (defenseForm == null) return;
        List<List<BufferedImage>> source = isPlayerOne ? playerSkillAnimations : enemySkillAnimations;
        if (defenseForm.toggleSkillSlot < 1 || defenseForm.toggleSkillSlot > source.size()) {
            setNatureDefenseFormActive(isPlayerOne, true);
            repaint();
            return;
        }
        List<BufferedImage> frames = source.get(defenseForm.toggleSkillSlot - 1);
        if (frames == null || frames.isEmpty()) {
            setNatureDefenseFormActive(isPlayerOne, true);
            repaint();
            return;
        }

        stopSkillAnimation(isPlayerOne);
        int freezeIndex = Math.max(0, Math.min(frames.size() - 1, defenseForm.enterFreezeFrame - 1));

        if (isPlayerOne) {
            activePlayerSkillFrames = frames;
            playerSkillFrameIndex = 0;
            isPlayerSkillAnimating = true;
            activePlayerSkillID = defenseForm.toggleSkillSlot;
            activePlayerSkillOffsetX = currentPlayerDef != null ? currentPlayerDef.getSkillForwardOffsetX(defenseForm.toggleSkillSlot) : 0;
            playerSkillTimer = new Timer(DEFAULT_SKILL_DELAY_MS, null);
            playerSkillTimer.addActionListener(e -> {
                if (playerSkillFrameIndex < freezeIndex) {
                    playerSkillFrameIndex++;
                } else {
                    stopSkillAnimation(true);
                    setNatureDefenseFormActive(true, true);
                }
                repaint();
            });
            playerSkillTimer.start();
        } else {
            activeEnemySkillFrames = frames;
            enemySkillFrameIndex = 0;
            isEnemySkillAnimating = true;
            activeEnemySkillID = defenseForm.toggleSkillSlot;
            activeEnemySkillOffsetX = -(currentEnemyDef != null ? currentEnemyDef.getSkillForwardOffsetX(defenseForm.toggleSkillSlot) : 0);
            enemySkillTimer = new Timer(DEFAULT_SKILL_DELAY_MS, null);
            enemySkillTimer.addActionListener(e -> {
                if (enemySkillFrameIndex < freezeIndex) {
                    enemySkillFrameIndex++;
                } else {
                    stopSkillAnimation(false);
                    setNatureDefenseFormActive(false, true);
                }
                repaint();
            });
            enemySkillTimer.start();
        }
    }

    private void playNatureDefenseFormExitAnimation(boolean isPlayerOne, CharacterDef.DefenseFormDef defenseForm) {
        if (defenseForm == null) return;
        List<List<BufferedImage>> source = isPlayerOne ? playerSkillAnimations : enemySkillAnimations;
        if (defenseForm.toggleSkillSlot < 1 || defenseForm.toggleSkillSlot > source.size()) {
            setNatureDefenseFormActive(isPlayerOne, false);
            repaint();
            return;
        }
        List<BufferedImage> frames = source.get(defenseForm.toggleSkillSlot - 1);
        if (frames == null || frames.isEmpty()) {
            setNatureDefenseFormActive(isPlayerOne, false);
            repaint();
            return;
        }

        stopSkillAnimation(isPlayerOne);
        int startIndex = Math.max(0, Math.min(frames.size() - 1, defenseForm.exitStartFrame - 1));

        if (isPlayerOne) {
            activePlayerSkillFrames = frames;
            playerSkillFrameIndex = startIndex;
            isPlayerSkillAnimating = true;
            activePlayerSkillID = defenseForm.toggleSkillSlot;
            activePlayerSkillOffsetX = currentPlayerDef != null ? currentPlayerDef.getSkillForwardOffsetX(defenseForm.toggleSkillSlot) : 0;
            playerSkillTimer = new Timer(DEFAULT_SKILL_DELAY_MS, null);
            playerSkillTimer.addActionListener(e -> {
                if (playerSkillFrameIndex < activePlayerSkillFrames.size() - 1) {
                    playerSkillFrameIndex++;
                } else {
                    stopSkillAnimation(true);
                    setNatureDefenseFormActive(true, false);
                }
                repaint();
            });
            playerSkillTimer.start();
        } else {
            activeEnemySkillFrames = frames;
            enemySkillFrameIndex = startIndex;
            isEnemySkillAnimating = true;
            activeEnemySkillID = defenseForm.toggleSkillSlot;
            activeEnemySkillOffsetX = -(currentEnemyDef != null ? currentEnemyDef.getSkillForwardOffsetX(defenseForm.toggleSkillSlot) : 0);
            enemySkillTimer = new Timer(DEFAULT_SKILL_DELAY_MS, null);
            enemySkillTimer.addActionListener(e -> {
                if (enemySkillFrameIndex < activeEnemySkillFrames.size() - 1) {
                    enemySkillFrameIndex++;
                } else {
                    stopSkillAnimation(false);
                    setNatureDefenseFormActive(false, false);
                }
                repaint();
            });
            enemySkillTimer.start();
        }
    }

    private void playNatureFormSkill1CastAnimation(boolean isPlayerOne, CharacterDef.DefenseFormDef defenseForm, Runnable onCastFinished) {
        List<BufferedImage> frames = loadNatureShotFrames(defenseForm);
        if (frames.isEmpty()) {
            int fallbackSkillSlot = defenseForm != null ? defenseForm.altSkillSlot : 1;
            playSkillAnimation(isPlayerOne, fallbackSkillSlot, false, onCastFinished);
            return;
        }

        Runnable callback = onCastFinished != null ? onCastFinished : () -> {};
        if (isPlayerOne) {
            stopSkillAnimation(true);
            activePlayerSkillFrames = frames;
            playerSkillFrameIndex = 0;
            isPlayerSkillAnimating = true;
            int altSkillSlot = defenseForm != null ? defenseForm.altSkillSlot : 1;
            activePlayerSkillID = altSkillSlot;
            activePlayerSkillOffsetX = currentPlayerDef != null ? currentPlayerDef.getSkillForwardOffsetX(altSkillSlot) : 0;
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
            int altSkillSlot = defenseForm != null ? defenseForm.altSkillSlot : 1;
            activeEnemySkillID = altSkillSlot;
            activeEnemySkillOffsetX = -(currentEnemyDef != null ? currentEnemyDef.getSkillForwardOffsetX(altSkillSlot) : 0);
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

    private List<BufferedImage> loadNatureShotFrames(CharacterDef.DefenseFormDef defenseForm) {
        if (defenseForm == null || defenseForm.altSkillAnimation == null) return List.of();
        String sheetPath = defenseForm.altSkillAnimation.sheetPath;
        int frameWidth = Math.max(1, defenseForm.altSkillAnimation.frameWidth);
        int frameHeight = Math.max(1, defenseForm.altSkillAnimation.frameHeight);
        List<BufferedImage> frames = loadAnimationFrames(
                new CharacterDef.AnimationDef(
                        sheetPath,
                        frameWidth,
                        frameHeight,
                        DEFAULT_SKILL_DELAY_MS));
        if (!frames.isEmpty()) return frames;

        frames = loadAnimationFrames(
                new CharacterDef.AnimationDef(
                        sheetPath,
                        64,
                        64,
                        DEFAULT_SKILL_DELAY_MS));
        return frames;
    }

    private CharacterDef.ProjectileDef buildDefenseFormAltProjectileDef(CharacterDef.DefenseFormDef defenseForm) {
        if (defenseForm == null) return null;
        return defenseForm.altSkillProjectile;
    }

    private boolean isWindWizardAttack3(boolean isPlayerOne) {
        CharacterDef actor = isPlayerOne ? currentPlayerDef : currentEnemyDef;
        int activeSkillID = isPlayerOne ? activePlayerSkillID : activeEnemySkillID;
        List<BufferedImage> frames = isPlayerOne ? activePlayerSkillFrames : activeEnemySkillFrames;
        return actor != null && "Wind Wizard".equalsIgnoreCase(actor.name) && activeSkillID == 3 && !frames.isEmpty();
    }

    private boolean isTargetOverlayAttack3(boolean isPlayerOne) {
        return isWindWizardAttack3(isPlayerOne);
    }

    private boolean shouldHideCasterDuringOverlaySkill3(boolean isPlayerOne) {
        return isWindWizardAttack3(isPlayerOne);
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
        int x = targetFeetX - (drawWidth / 2);
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


    private void startProjectileAnimation(boolean isPlayerOne, int skillID, CharacterDef.ProjectileDef overrideProjectileDef, Runnable onImpactStart, Runnable onComplete) {
        CharacterDef actor = isPlayerOne ? currentPlayerDef : currentEnemyDef;
        if (actor == null) { if (onComplete != null) onComplete.run(); return; }
        CharacterDef.ProjectileDef projectileDef = overrideProjectileDef != null ? overrideProjectileDef : actor.getSkillProjectile(skillID);
        if (projectileDef == null) { if (onComplete != null) onComplete.run(); return; }
        List<BufferedImage> frames = loadProjectileFrames(projectileDef);
        if (frames.isEmpty()) { if (onComplete != null) onComplete.run(); return; }
        List<BufferedImage> impactFrames = loadProjectileImpactFrames(projectileDef);
        stopProjectileAnimation();
        int attackerX      = isPlayerOne ? p1SpriteX : p2SpriteX;
        int attackerY      = isPlayerOne ? p1SpriteY : p2SpriteY;
        int attackerWidth  = isPlayerOne ? getPlayerDrawWidth()  : getEnemyDrawWidth();
        int attackerHeight = isPlayerOne ? getPlayerDrawHeight() : getEnemyDrawHeight();
        int targetX        = isPlayerOne ? p2SpriteX : p1SpriteX;
        int targetWidth    = isPlayerOne ? getEnemyDrawWidth()   : getPlayerDrawWidth();
        int projW = Math.max(1, projectileDef.drawWidth);
        int projH = Math.max(1, projectileDef.drawHeight);
        int verticalOffset = projectileDef.verticalOffset;
        projectileDirection    = attackerX <= targetX ? 1 : -1;
        int horizontalSpawnOffset = projectileDef.spawnOffsetX * projectileDirection;
        projectileIsPlayerOne  = isPlayerOne;
        projectileDrawWidth    = projW;
        projectileDrawHeight   = projH;
        projectileSpeed        = Math.max(1, projectileDef.speed);
        projectileImpactFrames = impactFrames;
        if (projectileDef.anchorOnTargetCenter) {
            int targetCenterX = targetX + (targetWidth / 2);
            int targetCenterY = isPlayerOne ? p2SpriteY + (getEnemyDrawHeight() / 2) : p1SpriteY + (getPlayerDrawHeight() / 2);
            projectileX = targetCenterX - (projW / 2) + projectileDef.spawnOffsetX;
            projectileY = targetCenterY - (projH / 2) + verticalOffset;
        } else if (projectileDef.anchorOnTarget) {
            int targetFeetX = isPlayerOne ? getEnemyFeetAnchorX() : getPlayerFeetAnchorX();
            int targetFeetY = isPlayerOne ? getEnemyFeetAnchorY() : getPlayerFeetAnchorY();
            projectileX = targetFeetX - (projW / 2) + projectileDef.spawnOffsetX;
            projectileY = targetFeetY - projH + verticalOffset;
        } else {
            projectileX = attackerX + (attackerWidth / 2) - (projW / 2) + horizontalSpawnOffset;
            projectileY = attackerY + (attackerHeight / 2) - (projH / 2) + verticalOffset;
        }
        activeProjectileFrames = frames;
        int totalFrames = frames.size();
        projectileLoopStartIndex = toValidFrameIndex(projectileDef.loopStartFrame, totalFrames, 0);
        projectileLoopEndIndex = toValidFrameIndex(projectileDef.loopEndFrame, totalFrames, totalFrames - 1);
        if (projectileLoopEndIndex < projectileLoopStartIndex) {
            projectileLoopEndIndex = projectileLoopStartIndex;
        }
        projectileImpactStartIndex = toValidFrameIndex(projectileDef.impactStartFrame, totalFrames, -1);
        projectileImpactEndIndex = toValidFrameIndex(projectileDef.impactEndFrame, totalFrames, -1);
        if (projectileImpactStartIndex >= 0 && projectileImpactEndIndex >= 0
                && projectileImpactEndIndex < projectileImpactStartIndex) {
            projectileImpactEndIndex = projectileImpactStartIndex;
        }
        projectileInImpactPhase = false;
        projectileFrameIndex   = 0;
        isProjectileAnimating  = true;
        if (projectileDef.beam && onImpactStart != null) {
            onImpactStart.run();
        }
        int stopBoundary = projectileDirection > 0
                ? (targetX + (targetWidth / 2)) - (projW / 2)
                : (targetX + (targetWidth / 2)) + (projW / 2);
        int animationDelay = projectileDef.animationFrameDelay > 0 ? projectileDef.animationFrameDelay : DEFAULT_SKILL_DELAY_MS;
        projectileTimer = new Timer(animationDelay, null);
        projectileTimer.addActionListener(e -> {
            if (!isProjectileAnimating || activeProjectileFrames.isEmpty()) {
                stopProjectileAnimation();
                if (onComplete != null) onComplete.run();
                return;
            }

            if (projectileDef.beam) {
                if (projectileFrameIndex < activeProjectileFrames.size() - 1) {
                    projectileFrameIndex++;
                } else {
                    stopProjectileAnimation();
                    if (onComplete != null) onComplete.run();
                }
                repaint();
                return;
            }

            if (!projectileInImpactPhase) {
                if (projectileFrameIndex < projectileLoopStartIndex) {
                    projectileFrameIndex++;
                } else {
                    if (projectileFrameIndex < projectileLoopEndIndex) {
                        projectileFrameIndex++;
                    } else {
                        projectileFrameIndex = projectileLoopStartIndex;
                    }
                }

                projectileX += projectileDirection * projectileSpeed;
                boolean reachedTarget = (projectileDirection > 0 && projectileX >= stopBoundary)
                        || (projectileDirection < 0 && projectileX <= stopBoundary);
                if (reachedTarget) {
                    projectileX = stopBoundary;
                    if (!projectileImpactFrames.isEmpty()) {
                        if (onImpactStart != null) {
                            onImpactStart.run();
                        }
                        projectileInImpactPhase = true;
                        activeProjectileFrames = projectileImpactFrames;
                        projectileFrameIndex = 0;
                        projectileDrawWidth = projectileDef.impactDrawWidth > 0 ? projectileDef.impactDrawWidth : projW;
                        projectileDrawHeight = projectileDef.impactDrawHeight > 0 ? projectileDef.impactDrawHeight : projH;
                        
                        // If impact should be anchored on target center, reposition it
                        if (projectileDef.anchorImpactOnTargetCenter) {
                            int targetCenterX = stopBoundary;
                            int targetCenterY = isPlayerOne ? p2SpriteY + (getEnemyDrawHeight() / 2) : p1SpriteY + (getPlayerDrawHeight() / 2);
                            projectileX = targetCenterX - (projectileDrawWidth / 2);
                            projectileY = targetCenterY - (projectileDrawHeight / 2);
                        }
                    } else if (projectileImpactStartIndex >= 0) {
                        if (onImpactStart != null) {
                            onImpactStart.run();
                        }
                        projectileInImpactPhase = true;
                        int startIndex = Math.max(0, Math.min(activeProjectileFrames.size() - 1, projectileImpactStartIndex));
                        projectileFrameIndex = startIndex;
                        int impactEnd = projectileImpactEndIndex >= startIndex
                                ? projectileImpactEndIndex
                                : activeProjectileFrames.size() - 1;
                        projectileImpactEndIndex = Math.min(activeProjectileFrames.size() - 1, impactEnd);
                    } else {
                        if (onImpactStart != null) {
                            onImpactStart.run();
                        }
                        stopProjectileAnimation();
                        if (onComplete != null) onComplete.run();
                    }
                }
            } else {
                int impactEndIndex = projectileImpactFrames.isEmpty() && projectileImpactEndIndex >= 0
                        ? Math.min(projectileImpactEndIndex, activeProjectileFrames.size() - 1)
                        : activeProjectileFrames.size() - 1;
                if (projectileFrameIndex < impactEndIndex) {
                    projectileFrameIndex++;
                } else {
                    stopProjectileAnimation();
                    if (onComplete != null) onComplete.run();
                }
            }
            repaint();
        });
        projectileTimer.start();
    }

    private int toValidFrameIndex(int oneBasedFrame, int totalFrames, int fallback) {
        if (oneBasedFrame <= 0) return fallback;
        int index = oneBasedFrame - 1;
        return (index >= 0 && index < totalFrames) ? index : fallback;
    }

    private List<BufferedImage> loadProjectileFrames(CharacterDef.ProjectileDef projectileDef) {
        String sheetPath = projectileDef.sheetPath;
        if (sheetPath == null || sheetPath.isBlank()) return List.of();

        String cacheKey = sheetPath + "|" + projectileDef.frameWidth + "x" + projectileDef.frameHeight;
        List<BufferedImage> cached = projectileAnimationCache.get(cacheKey);
        if (cached != null) return cached;

        List<BufferedImage> frames = loadAnimationFrames(
                new CharacterDef.AnimationDef(
                        sheetPath,
                        Math.max(1, projectileDef.frameWidth),
                        Math.max(1, projectileDef.frameHeight),
                        DEFAULT_SKILL_DELAY_MS));
        projectileAnimationCache.put(cacheKey, frames);
        return frames;
    }

    private List<BufferedImage> loadProjectileImpactFrames(CharacterDef.ProjectileDef projectileDef) {
        String impactSheetPath = projectileDef.impactSheetPath;
        if (impactSheetPath == null || impactSheetPath.isBlank()) return List.of();

        int frameWidth = projectileDef.impactFrameWidth > 0 ? projectileDef.impactFrameWidth : projectileDef.frameWidth;
        int frameHeight = projectileDef.impactFrameHeight > 0 ? projectileDef.impactFrameHeight : projectileDef.frameHeight;
        String cacheKey = impactSheetPath + "|" + frameWidth + "x" + frameHeight;
        List<BufferedImage> cached = projectileAnimationCache.get(cacheKey);
        if (cached != null) return cached;

        List<BufferedImage> frames = loadAnimationFrames(
                new CharacterDef.AnimationDef(
                        impactSheetPath,
                        Math.max(1, frameWidth),
                        Math.max(1, frameHeight),
                        DEFAULT_SKILL_DELAY_MS));
        projectileAnimationCache.put(cacheKey, frames);
        return frames;
    }

    private void stopProjectileAnimation() {
        if (projectileTimer != null) { projectileTimer.stop(); projectileTimer = null; }
        isProjectileAnimating  = false; projectileFrameIndex = 0;
        projectileDrawWidth = DEFAULT_PROJECTILE_DRAW_SIZE;
        projectileDrawHeight = DEFAULT_PROJECTILE_DRAW_SIZE;
        projectileSpeed = DEFAULT_PROJECTILE_SPEED;
        projectileInImpactPhase = false;
        projectileImpactFrames = List.of();
        projectileLoopStartIndex = 0;
        projectileLoopEndIndex = 0;
        projectileImpactStartIndex = -1;
        projectileImpactEndIndex = -1;
        projectileX = 0; projectileY = 0; projectileDirection = 1;
        projectileIsPlayerOne  = true;   activeProjectileFrames = List.of();
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

    private int getProjectileSpawnFrame(CharacterDef actor, int skillID) {
        if (actor == null) return -1;
        if ("Dark Wizard".equalsIgnoreCase(actor.name)) {
            if (skillID == 1) return 9;
            if (skillID == 2) return 7;
        }
        return -1;
    }

    private void scheduleProjectileSpawnAtCastFrame(int frameOneBased, Runnable spawnProjectile) {
        if (spawnProjectile == null) return;
        int delayMs = Math.max(0, frameOneBased - 1) * DEFAULT_SKILL_DELAY_MS;
        Timer spawnDelayTimer = new Timer(delayMs, e -> spawnProjectile.run());
        spawnDelayTimer.setRepeats(false);
        spawnDelayTimer.start();
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
            if (playerHurtFlashTimer  != null) playerHurtFlashTimer.stop();
            isPlayerHurtAnimating = true; playerHurtFrameIndex = 0;
            playerHurtFlashing = false;
            playerHurtTimer = new Timer(DEFAULT_HURT_DELAY_MS, null);
            playerHurtTimer.addActionListener(e -> {
                if (!playerHurtFrames.isEmpty())
                    playerHurtFrameIndex = (playerHurtFrameIndex + 1) % playerHurtFrames.size();
                repaint();
            });
            playerHurtTimer.start();
            // Flash timer: toggle red flash every 150ms
            playerHurtFlashTimer = new Timer(150, null);
            playerHurtFlashTimer.addActionListener(e -> {
                playerHurtFlashing = !playerHurtFlashing;
                repaint();
            });
            playerHurtFlashTimer.start();
            playerHurtWindowTimer = new Timer(durationMs, null);
            playerHurtWindowTimer.setRepeats(false);
            playerHurtWindowTimer.addActionListener(e -> stopHurtTimeline(true));
            playerHurtWindowTimer.start();
        } else {
            if (p2HP <= 0 || enemyHurtFrames.isEmpty()) return;
            if (enemyHurtTimer       != null) enemyHurtTimer.stop();
            if (enemyHurtWindowTimer != null) enemyHurtWindowTimer.stop();
            if (enemyHurtFlashTimer  != null) enemyHurtFlashTimer.stop();
            isEnemyHurtAnimating = true; enemyHurtFrameIndex = 0;
            enemyHurtFlashing = false;
            enemyHurtTimer = new Timer(DEFAULT_HURT_DELAY_MS, null);
            enemyHurtTimer.addActionListener(e -> {
                if (!enemyHurtFrames.isEmpty())
                    enemyHurtFrameIndex = (enemyHurtFrameIndex + 1) % enemyHurtFrames.size();
                repaint();
            });
            enemyHurtTimer.start();
            // Flash timer: toggle red flash every 150ms
            enemyHurtFlashTimer = new Timer(150, null);
            enemyHurtFlashTimer.addActionListener(e -> {
                enemyHurtFlashing = !enemyHurtFlashing;
                repaint();
            });
            enemyHurtFlashTimer.start();
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
            if (playerHurtFlashTimer  != null) { playerHurtFlashTimer.stop();  playerHurtFlashTimer  = null; }
            isPlayerHurtAnimating = false; playerHurtFrameIndex = 0;
            playerHurtFlashing = false;
        } else {
            if (enemyHurtDelayTimer  != null) { enemyHurtDelayTimer.stop();  enemyHurtDelayTimer  = null; }
            if (enemyHurtWindowTimer != null) { enemyHurtWindowTimer.stop(); enemyHurtWindowTimer = null; }
            if (enemyHurtTimer       != null) { enemyHurtTimer.stop();       enemyHurtTimer       = null; }
            if (enemyHurtFlashTimer  != null) { enemyHurtFlashTimer.stop();  enemyHurtFlashTimer  = null; }
            isEnemyHurtAnimating = false; enemyHurtFrameIndex = 0;
            enemyHurtFlashing = false;
        }
    }

    private BufferedImage applyRedTintToNonTransparent(BufferedImage original) {
        BufferedImage tinted = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                int argb = original.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha > 0) {
                    // Non-transparent pixel - apply red tint with reduced opacity
                    int r = 255;
                    int g = 0;
                    int b = 0;
                    int reducedAlpha = (int)(alpha * 0.5); // 50% opacity
                    tinted.setRGB(x, y, (reducedAlpha << 24) | (r << 16) | (g << 8) | b);
                } else {
                    // Transparent pixel - keep as is
                    tinted.setRGB(x, y, argb);
                }
            }
        }
        return tinted;
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

    //PLAYS THE MUSIC
    public void playMusic(int i) {
        survivalBGM.setFile(i);
        survivalBGM.loop();
    }

    //STOPS THE MUSIC
    public void stopMusic() {
        if (survivalBGM != null) {
            survivalBGM.stop();
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
                    config.skill1SpritePath, config.skill1FollowUpSpritePath, config.skill2SpritePath, config.skill3SpritePath,
                    config.skill1ForwardOffsetX, config.skill2ForwardOffsetX, config.skill3ForwardOffsetX,
                    config.skill1HurtTriggerBufferSeconds, config.skill2HurtTriggerBufferSeconds,
                    config.skill3HurtTriggerBufferSeconds,
                    new CharacterDef.AnimationDef(config.idleSpritePath,  DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_IDLE_DELAY_MS),
                    new CharacterDef.AnimationDef(config.hurtSpritePath,  DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_HURT_DELAY_MS),
                    new CharacterDef.AnimationDef(config.deathSpritePath, DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_DEAD_DELAY_MS),
                    config.skill1Projectile, config.skill2Projectile, config.skill3Projectile,
                    config.defenseForm,
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
                        "/assets/spritesheet/Water Wizard/Attack-Sheet.png", "",
                        "/assets/spritesheet/Water Wizard/Attack2-Sheet.png",
                        "/assets/spritesheet/Water Wizard/Attack3-Sheet.png",
                        0, 0, 0, 0.14, 0.0, 0.0,
                        new CharacterDef.AnimationDef("/assets/spritesheet/Water Wizard/Idle-Sheet.png", DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_IDLE_DELAY_MS),
                        new CharacterDef.AnimationDef("/assets/spritesheet/Water Wizard/Hurt-Sheet.png", DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_HURT_DELAY_MS),
                        new CharacterDef.AnimationDef("/assets/spritesheet/Water Wizard/Dead-Sheet.png", DEFAULT_FRAME_SIZE, DEFAULT_FRAME_SIZE, DEFAULT_DEAD_DELAY_MS),
                        new CharacterDef.ProjectileDef("/assets/spritesheet/Water Wizard/Charge-Sheet.png", 128, 128, 144, 144, 44, 50),
                        new CharacterDef.ProjectileDef("/assets/spritesheet/Water Wizard/Charge2-Sheet.png", 72, 72, 144, 144, 44, 50),
                        null,
                        null,
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