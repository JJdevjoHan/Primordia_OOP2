package engine.core;

import engine.audio.SoundManager;
import engine.enums.GameMode;
import engine.enums.GameState;
import engine.gameplay.*;
import assets.Utility.*;
import engine.ui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class GameWindow extends JFrame {

    private CardLayout layout;
    private JPanel container;
    private final SoundManager menuBGM = new SoundManager();
    private CharacterSelectionPanel currentCharacterSelectionPanel;
    private JPanel currentGamePanel;
    private String currentPanelCard = "INTRO";
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
        container.add(credits, "CREDITS");
        cardMap.put("CREDITS", credits);

        add(container);

        setTitle("Primordia");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

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
        System.out.println("Window close requested. Current panel: " + currentPanelCard);
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
        handleWindowClose();
    }

    public void showPauseMenu() {
        System.out.println("showPauseMenu() called");
        Component prev = cardMap.get(currentPanelCard);
        PauseMenuPanel pauseMenu = new PauseMenuPanel(this, currentPanelCard, prev);
        container.add(pauseMenu, "PAUSE");
        cardMap.put("PAUSE", pauseMenu);
        pauseTimersForComponent(prev);
        layout.show(container, "PAUSE");
        container.revalidate();
        container.repaint();
        pauseMenu.requestFocusInWindow();
        System.out.println("Pause menu shown");
    }

    public void resumeFromPause(String previousPanelCard) {
        layout.show(container, previousPanelCard);
        Component prev = cardMap.get(previousPanelCard);
        resumeTimersForComponent(prev);
        if (prev instanceof JComponent) {
            ((JComponent) prev).requestFocusInWindow();
        }
        Component pauseComp = cardMap.remove("PAUSE");
        if (pauseComp != null) {
            container.remove(pauseComp);
            container.revalidate();
            container.repaint();
        }
    }

    public void showSettingsMenu() {
        System.out.println("showSettingsMenu() called");
        Component prev = cardMap.get(currentPanelCard);
        SettingsMenuPanel settingsMenu = new SettingsMenuPanel(this, currentPanelCard, prev);
        container.add(settingsMenu, "SETTINGS");
        cardMap.put("SETTINGS", settingsMenu);
        layout.show(container, "SETTINGS");
        container.revalidate();
        container.repaint();
        settingsMenu.requestFocusInWindow();
        System.out.println("Settings menu shown");
    }

    public void closeSettings(String previousPanelCard) {
        layout.show(container, previousPanelCard);
        Component settingsComp = cardMap.remove("SETTINGS");
        if (settingsComp != null) {
            container.remove(settingsComp);
            container.revalidate();
            container.repaint();
        }
        if (previousPanelCard.equals("INTRO")) {
            Component prev = cardMap.get("INTRO");
            if (prev instanceof JComponent) {
                ((JComponent) prev).requestFocusInWindow();
            }
        }
    }


    public void showIntro() {
        stopGameMusic();
        detachCurrentGamePanel();
        currentPanelCard = "INTRO";
        layout.show(container, "INTRO");
        playMenuMusic(6);
    }

    public void showMenu() {
        stopGameMusic();
        detachCurrentGamePanel();
        currentPanelCard = "MENU";
        layout.show(container, "MENU");
        playMenuMusic(6);
    }

    public void showCredits() {
        stopGameMusic();
        detachCurrentGamePanel();
        currentPanelCard = "CREDITS";
        layout.show(container, "CREDITS");
    }

    public void showGamePanel() {
        currentPanelCard = "GAME";
        layout.show(container, "GAME");
    }

    public void showSurivivalGamePanel() {
        currentPanelCard = "SURVIVAL";
        layout.show(container, "SURVIVAL");
    }

    public void showArcadeGamePanel() {
        currentPanelCard = "ARCADE";
        layout.show(container, "ARCADE");
    }

    public void showCharacterSelection(GameMode mode) {
        stopMenuMusic();
        GamePanel.reloadCharacterDefs();
        detachCurrentGamePanel();

        Component previousSelection = cardMap.remove("CHAR_SELECT");
        if (previousSelection != null) {
            container.remove(previousSelection);
            container.revalidate();
            container.repaint();
        }

        currentCharacterSelectionPanel = new CharacterSelectionPanel(this, mode);
        currentGamePanel = null;
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

        Component previousGame = cardMap.remove("GAME");
        if (previousGame != null) container.remove(previousGame);

        GamePanel panel = new GamePanel(this, p1, p2);
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

        Component previousSurvival = cardMap.remove("SURVIVAL");
        if (previousSurvival != null) container.remove(previousSurvival);

        int botIndex = (int) (Math.random() * GamePanel.ALL_CHARACTERS.size());

        SurivivalGamePanel surivivalGamePanel = new SurivivalGamePanel(
                this, playerIndex, botIndex, GameMode.SURVIVAL, BotAI.Difficulty.EASY);
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

        Component previousArcade = cardMap.remove("ARCADE");
        if (previousArcade != null) container.remove(previousArcade);

        List<Integer> arcadeOpponents = new ArrayList<>();
        for (int i = 0; i < GamePanel.ALL_CHARACTERS.size(); i++) {
            if (i != playerIndex) arcadeOpponents.add(i);
        }

        ArcadeGamePanel arcade = new ArcadeGamePanel(
                this, playerIndex, arcadeOpponents, BotAI.Difficulty.NORMAL);
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
        if (menuBGM != null) menuBGM.stop();
    }

    public void stopCharacterSelectionMusic() {
        if (currentCharacterSelectionPanel != null) {
            currentCharacterSelectionPanel.stopMusic();
        }
    }

    public void stopGameMusic() {
        if (currentGamePanel instanceof GamePanel) {
            ((GamePanel) currentGamePanel).stopMusic();
        } else if (currentGamePanel instanceof SurivivalGamePanel) {
            ((SurivivalGamePanel) currentGamePanel).stopMusic();
        } else if (currentGamePanel instanceof ArcadeGamePanel) {
            ((ArcadeGamePanel) currentGamePanel).stopMusic();
        }
    }

    private void detachCurrentGamePanel() {
        if (currentGamePanel == null) return;

        Component current = currentGamePanel;
        pauseTimersForComponent(current);

        if (currentPanelCard != null) {
            Component cardComponent = cardMap.remove(currentPanelCard);
            if (cardComponent != null) container.remove(cardComponent);
            else container.remove(current);
        } else {
            container.remove(current);
        }

        currentGamePanel = null;
        container.revalidate();
        container.repaint();
    }

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
                        if (val instanceof Timer t && t.isRunning()) {
                            t.stop();
                            stopped.add(t);
                        }
                    }
                }
                cls = cls.getSuperclass();
            }
            if (!stopped.isEmpty()) pausedTimers.put(comp, stopped);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void resumeTimersForComponent(Component comp) {
        if (comp == null) return;
        java.util.List<Timer> list = pausedTimers.remove(comp);
        if (list == null) return;
        for (Timer t : list) {
            try { t.start(); } catch (Exception ignored) {}
        }
    }
}