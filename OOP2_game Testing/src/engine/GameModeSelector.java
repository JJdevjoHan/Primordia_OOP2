package engine;

import assets.Utility.ButtonTextRenderer;
import assets.Utility.CreditsButton;
import assets.Utility.ExitButton;
import assets.Utility.FontManager;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class GameModeSelector extends JPanel {

    private Image background;
    private final GameWindow window;
    private JButton exitButton;

    private final int screenWidth = 1536;

    SoundManager gameModeAudio = new SoundManager();
    SoundManager sfx = new SoundManager();

    public GameModeSelector(GameWindow window) {

        this.window = window;

        int screenHeight = 896;
        setPreferredSize(new Dimension(screenWidth, screenHeight));
        setLayout(null);

        loadBackground();

        //createTitle();
        createButtons();

        exitButton = new ExitButton().createExitButton(this);
        add(exitButton);
    }

    private void loadBackground() {

        URL resource = getClass().getResource("/assets/maps/menu2.png");

        if(resource != null){
            background = new ImageIcon(resource).getImage();
        }
    }

    @Override
    public void doLayout() {
        super.doLayout();
        int margin = 20;
        if (exitButton != null) {
            exitButton.setBounds(getWidth() - 50 - margin, margin, 40, 40);
        }
    }

    private void createButtons(){

        int btnWidth = 250;
        int btnHeight = 50;
        int centerX = screenWidth/2 - btnWidth/2;
        int topMargin = 480;
        int buttonGap = 78;

        JButton pvp = createRPGButton("Versus");
        JButton survival = createRPGButton("Survival");
        JButton arcade = createRPGButton("Arcade");

        pvp.setBounds(centerX, topMargin, btnWidth, btnHeight);
        survival.setBounds(centerX, topMargin + buttonGap, btnWidth, btnHeight);
        arcade.setBounds(centerX, topMargin + (buttonGap * 2), btnWidth, btnHeight);

        add(pvp);
        add(survival);
        add(arcade);

        pvp.addActionListener(e -> {
            sfx.setFile(8);
            sfx.play();
            window.showCharacterSelection(GameMode.PVP);
        });
        survival.addActionListener(e -> {
            sfx.setFile(8);
            sfx.play();
            window.showCharacterSelection(GameMode.SURVIVAL);
        });
        arcade.addActionListener(e -> {
            sfx.setFile(8);
            sfx.play();
            window.showCharacterSelection(GameMode.ARCADE);
        });
    }

    private JButton createRPGButton(String text){

        final String label = text;
        JButton btn = new JButton("") {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                ButtonTextRenderer.drawCenteredText(g2, this, label, 5);
                g2.dispose();
            }
        };
        Font boldFont = FontManager.getFont(32).deriveFont(Font.BOLD);

        btn.setFont(boldFont);
        btn.setForeground(new Color(255,215,0));
        btn.setBackground(new Color(30,30,50));

        btn.setFocusPainted(false);

        btn.setBorder(BorderFactory.createLineBorder(new Color(200,160,40),3));

        btn.addMouseListener(new java.awt.event.MouseAdapter(){

            public void mouseEntered(java.awt.event.MouseEvent evt){
                btn.setBackground(new Color(70,70,100));
            }

            public void mouseExited(java.awt.event.MouseEvent evt){
                btn.setBackground(new Color(30,30,50));
            }
        });

        return btn;
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        if(background != null){
            g.drawImage(background,0,0,getWidth(),getHeight(),this);
        }
    }


}
