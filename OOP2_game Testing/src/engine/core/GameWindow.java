package engine.core;


import javax.swing.*;

public class GameWindow extends JFrame {

    private final ScreenManager screenManager;
    private final AudioManager audioManager;

    public GameWindow() {

        screenManager = new ScreenManager();
        audioManager = new AudioManager();

        add(screenManager.getContainer());

        initializeScreens();

        audioManager.playMenuMusic();
    }

    private void initializeScreens() {

    }
}