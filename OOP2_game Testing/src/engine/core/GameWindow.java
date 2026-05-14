package engine.core;


import engine.enums.GameMode;
import engine.enums.GameState;

import javax.swing.*;

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

    public void closeSettings(){
        
    }
}