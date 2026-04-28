package engine;


//Mag add rakog comments ari guys ugma duka na
import javax.swing.*;
import java.awt.*;

public class GameWindow extends JFrame {

    private CardLayout layout;
    private JPanel container;
    private final SoundManager menuBGM = new SoundManager();


    public GameWindow() {

        layout = new CardLayout();
        container = new JPanel(layout);

        IntroPanel intro = new IntroPanel(this);
        GameModeSelector menu = new GameModeSelector(this);
        CreditsPanel credits = new CreditsPanel(this);

        container.add(intro, "INTRO");
        container.add(menu, "MENU");
        container.add(credits,"CREDITS");

        container.add(new JPanel(), "CHAR_SELECT");
        container.add(new JPanel(), "GAME");
        container.add(new JPanel(), "SURVIVAL");
        container.add(new JPanel(),"ARCADE");

        add(container);

        setTitle("Primordia");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setUndecorated(true); // removes title bar
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        playMenuMusic(6);
        layout.show(container, "INTRO");
    }

    public void showMenu(){

        layout.show(container,"MENU");
    }

    public void showGamePanel(){
        layout.show(container,"GAME");
    }

    public void showSurivivalGamePanel() {
        layout.show(container,"SURVIVAL");
    }

    public void showArcadeGamePanel(){ layout.show(container,"ARCADE"); }

    public void showCharacterSelection(GameMode mode) {
        stopMenuMusic();
        CharacterSelectionPanel panel = new CharacterSelectionPanel(this, mode);

        container.add(panel, "CHAR_SELECT");
        layout.show(container, "CHAR_SELECT");

        container.revalidate();
        container.repaint();
        panel.requestFocusInWindow();
    }

    public void startPvPMatch(int p1, int p2) {

        GamePanel panel = new GamePanel(this,p1, p2);

        container.add(panel, "GAME");
        layout.show(container, "GAME");

        container.revalidate();
        container.repaint();
        panel.requestFocusInWindow();
    }

    public void startSurvivalMatch(int playerIndex) {

        int botIndex = (int)(Math.random() * GamePanel.ALL_CHARACTERS.size());

        SurivivalGamePanel surivivalGamePanel = new SurivivalGamePanel(
                this,
                playerIndex,
                botIndex,
                GameMode.SURVIVAL,
                BotAI.Difficulty.EASY
        );

        container.add(surivivalGamePanel, "SURVIVAL");
        layout.show(container, "SURVIVAL");

        container.revalidate();
        container.repaint();
        surivivalGamePanel.requestFocusInWindow();
    }

    public void startArcadeMatch(int playerIndex) {
        int botIndex = (int)(Math.random() * GamePanel.ALL_CHARACTERS.size());

        ArcadeGamePanel arcade = new ArcadeGamePanel(
                this,
                playerIndex,
                botIndex,
                GameMode.PVB,
                BotAI.Difficulty.NORMAL
        );

        container.add(arcade, "ARCADE");
        layout.show(container, "ARCADE");

        container.revalidate();
        container.repaint();
        arcade.requestFocusInWindow();
    }

    public void playMenuMusic(int i) {
        menuBGM.setFile(i);
        menuBGM.play();
        menuBGM.loop();
    }

    public void stopMenuMusic() {
        if (menuBGM != null) {
            menuBGM.stop();
        }
    }

    public void showCredits() {
        layout.show(container,"CREDITS");
    }
}