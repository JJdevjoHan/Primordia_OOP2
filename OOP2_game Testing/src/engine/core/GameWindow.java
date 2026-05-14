package engine.core;


import engine.enums.GameMode;
import engine.enums.GameState;

import javax.swing.*;

import java.awt.*;

import static java.lang.System.exit;

public class GameWindow extends JFrame {

    private final ScreenManager screenManager;
    private final AudioManager audioManager;

    public GameWindow() {
        screenManager = new ScreenManager();
        audioManager  = new AudioManager();
        add(screenManager.getContainer());
        initializeScreens();
        audioManager.playMenuMusic();
    }

    private void initializeScreens() { }

    public void showIntro() {
        audioManager.playMenuMusic();
        screenManager.showScreen(GameState.INTRO);
    }

    public void showCharacterSelection(GameMode mode) {
        screenManager.showScreen(GameState.CHARACTER_SELECTION);
    }

    public void showCredits() {
        screenManager.showScreen(GameState.CREDITS);
    }

    public void showSettings()
    {
        screenManager.showScreen(GameState.SETTINGS);
    }

    public void showArcade()
    {
        screenManager.showScreen(GameState.ARCADE);
    }

    public void showPause()
    {
        screenManager.showScreen(GameState.PAUSE);
    }

    public void showSurvival()
    {
        screenManager.showScreen(GameState.SURVIVAL);
    }

    public void showGame()
    {
        screenManager.showScreen(GameState.GAME);
    }

    public void showMenu()
    {
        screenManager.showScreen(GameState.MENU);
    }


    public void stopGameMusic() {
        audioManager.stopMenuMusic(); // check AudioManager for the exact method name
    }

    public void handleExitButtonClick() {
        showIntro();
    }

    public void closeSettings(GameState previousState) {
        screenManager.showScreen(previousState);

        // Remove the settings screen overlay if it was added dynamically
        Component settingsComp = screenManager.getScreen(GameState.SETTINGS);
        if (settingsComp != null) {
            screenManager.removeScreen(GameState.SETTINGS);
        }

        // Restore focus to the previous panel if it's a JComponent
        Component prev = screenManager.getScreen(previousState);
        if (prev instanceof JComponent) {
            ((JComponent) prev).requestFocusInWindow();
        }
    }
}