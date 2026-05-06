package engine;


//Mag add rakog comments ari guys ugma duka na
import javax.swing.*;
import javax.swing.event.AncestorListener;
import javax.swing.event.AncestorEvent;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class GameWindow extends JFrame {

    private CardLayout layout;
    private JPanel container;
    private final SoundManager menuBGM = new SoundManager();
    private CharacterSelectionPanel currentCharacterSelectionPanel;  // Track character selection panel
    private JPanel currentGamePanel;  // Track current game/panel being played
    private String currentPanelCard = "INTRO";  // Track the current card name
    private final java.util.Map<String, Component> cardMap = new java.util.HashMap<>();
    private final java.util.Map<Component, java.util.List<Timer>> pausedTimers = new java.util.HashMap<>();


    public GameWindow() {

        layout = new CardLayout();
        container = new JPanel(layout);

        IntroPanel intro = new IntroPanel(this);
        GameModeSelector menu = new GameModeSelector(this);
        CreditsPanel credits = new CreditsPanel(this);

        container.add(intro, "INTRO");
        cardMap.put("INTRO", intro);
        container.add(menu, "MENU");
        cardMap.put("MENU", menu);
        container.add(credits,"CREDITS");
        cardMap.put("CREDITS", credits);

        add(container);

        setTitle("Primordia");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        setUndecorated(true); // removes title bar
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        
        // Add window listener BEFORE making visible
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowClose();
            }
        });
        
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        
        playMenuMusic(6);
        currentPanelCard = "INTRO";
        layout.show(container, "INTRO");
    }
    
    private void handleWindowClose() {
        // Debug: print current panel to verify state
        System.out.println("Window close requested. Current panel: " + currentPanelCard);
        
        // If in a game or menu/selection, show pause menu; otherwise quit
        // Check if we're in an active screen by looking at the current panel card
        if ("GAME".equals(currentPanelCard) || "SURVIVAL".equals(currentPanelCard) || "ARCADE".equals(currentPanelCard)
            || "CHAR_SELECT".equals(currentPanelCard) || "MENU".equals(currentPanelCard) || "INTRO".equals(currentPanelCard)) {
            System.out.println("In game - showing pause menu");
            showPauseMenu();
        } else {
            System.out.println("Not in game - exiting");
            System.exit(0);
        }
    }
    
    public void handleExitButtonClick() {
        // This is called when the exit button is clicked - same logic as window close
        handleWindowClose();
    }
    
    public void showPauseMenu() {
        System.out.println("showPauseMenu() called");
        // Always show pause menu when requested
        Component prev = cardMap.get(currentPanelCard);
        PauseMenuPanel pauseMenu = new PauseMenuPanel(this, currentPanelCard, prev);
        container.add(pauseMenu, "PAUSE");
        cardMap.put("PAUSE", pauseMenu);
        // pause underlying timers for the previous component
        pauseTimersForComponent(prev);
        layout.show(container, "PAUSE");
        container.revalidate();
        container.repaint();
        pauseMenu.requestFocusInWindow();
        System.out.println("Pause menu shown");
    }
    
    public void resumeFromPause(String previousPanelCard) {
        // show the previous card and resume its timers
        layout.show(container, previousPanelCard);
        Component prev = cardMap.get(previousPanelCard);
        resumeTimersForComponent(prev);
        if (prev instanceof JComponent) {
            ((JComponent) prev).requestFocusInWindow();
        }
        // remove pause overlay component if present
        Component pauseComp = cardMap.remove("PAUSE");
        if (pauseComp != null) {
            container.remove(pauseComp);
            container.revalidate();
            container.repaint();
        }
    }

    public void showMenu(){
        currentGamePanel = null;
        currentPanelCard = "MENU";
        layout.show(container,"MENU");
        playMenuMusic(6);
    }

    public void showGamePanel(){
        currentPanelCard = "GAME";
        layout.show(container,"GAME");
    }

    public void showSurivivalGamePanel() {
        currentPanelCard = "SURVIVAL";
        layout.show(container,"SURVIVAL");
    }

    public void showArcadeGamePanel(){
        currentPanelCard = "ARCADE";
        layout.show(container,"ARCADE");
    }
    public void showCharacterSelection(GameMode mode) {
        stopMenuMusic();
        GamePanel.reloadCharacterDefs();
        currentCharacterSelectionPanel = new CharacterSelectionPanel(this, mode);
        currentGamePanel = null;  // Character selection is not a game
        currentPanelCard = "CHAR_SELECT";

        container.add(currentCharacterSelectionPanel, "CHAR_SELECT");
        cardMap.put("CHAR_SELECT", currentCharacterSelectionPanel);
        layout.show(container, "CHAR_SELECT");

        container.revalidate();
        container.repaint();
        SwingUtilities.invokeLater(currentCharacterSelectionPanel::requestFocusInWindow);
    }

    public void startPvPMatch(int p1, int p2) {
        stopCharacterSelectionMusic();
        GamePanel.reloadCharacterDefs();

        GamePanel panel = new GamePanel(this,p1, p2);
        currentGamePanel = panel;
        currentPanelCard = "GAME";

        container.add(panel, "GAME");
        cardMap.put("GAME", panel);
        layout.show(container, "GAME");

        container.revalidate();
        container.repaint();
        SwingUtilities.invokeLater(panel::requestFocusInWindow);
    }

    public void startSurvivalMatch(int playerIndex) {
        stopCharacterSelectionMusic();
        GamePanel.reloadCharacterDefs();

        int botIndex = (int)(Math.random() * GamePanel.ALL_CHARACTERS.size());

        SurivivalGamePanel surivivalGamePanel = new SurivivalGamePanel(
                this,
                playerIndex,
                botIndex,
                GameMode.SURVIVAL,
                BotAI.Difficulty.EASY
        );
        currentGamePanel = surivivalGamePanel;
        currentPanelCard = "SURVIVAL";

        container.add(surivivalGamePanel, "SURVIVAL");
        cardMap.put("SURVIVAL", surivivalGamePanel);
        layout.show(container, "SURVIVAL");

        container.revalidate();
        container.repaint();
        SwingUtilities.invokeLater(surivivalGamePanel::requestFocusInWindow);
    }

    public void startArcadeMatch(int playerIndex) {
        stopCharacterSelectionMusic();
        GamePanel.reloadCharacterDefs();
        List<Integer> arcadeOpponents = new ArrayList<>();        for (int i = 0; i < GamePanel.ALL_CHARACTERS.size(); i++) {
            if (i != playerIndex) {
                arcadeOpponents.add(i);
            }
        }

        ArcadeGamePanel arcade = new ArcadeGamePanel(
                this,
                playerIndex,
                arcadeOpponents,
                BotAI.Difficulty.NORMAL
        );
        currentGamePanel = arcade;
        currentPanelCard = "ARCADE";

        container.add(arcade, "ARCADE");
        cardMap.put("ARCADE", arcade);
        layout.show(container, "ARCADE");

        container.revalidate();
        container.repaint();
        SwingUtilities.invokeLater(arcade::requestFocusInWindow);
    }

    public void playMenuMusic(int i) {
        menuBGM.setFile(i);
        menuBGM.loop();
    }

    public void stopMenuMusic() {
        if (menuBGM != null) {
            menuBGM.stop();
        }
    }

    public void stopCharacterSelectionMusic() {
        if (currentCharacterSelectionPanel != null) {
            currentCharacterSelectionPanel.stopMusic();
        }
    }

    public void showCredits() {
        currentGamePanel = null;
        currentPanelCard = "CREDITS";
        layout.show(container,"CREDITS");
    }

    // Pause all javax.swing.Timers declared as fields on the component instance
    private void pauseTimersForComponent(Component comp) {
        if (comp == null) return;
        try {
            java.util.List<Timer> stopped = new java.util.ArrayList<>();
            Class<?> cls = comp.getClass();
            while (cls != null) {
                java.lang.reflect.Field[] fields = cls.getDeclaredFields();
                for (java.lang.reflect.Field f : fields) {
                    if (javax.swing.Timer.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        Object val = f.get(comp);
                        if (val instanceof Timer) {
                            Timer t = (Timer) val;
                            if (t.isRunning()) {
                                t.stop();
                                stopped.add(t);
                            }
                        }
                    }
                }
                cls = cls.getSuperclass();
            }
            if (!stopped.isEmpty()) {
                pausedTimers.put(comp, stopped);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void resumeTimersForComponent(Component comp) {
        if (comp == null) return;
        java.util.List<Timer> list = pausedTimers.remove(comp);
        if (list == null) return;
        for (Timer t : list) {
            try {
                t.start();
            } catch (Exception ignored) {}
        }
    }
}