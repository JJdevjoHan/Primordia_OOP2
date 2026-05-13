package engine.ui;

import assets.Utility.ButtonTextRenderer;
import assets.Utility.FontManager;
import engine.core.GameWindow;
import engine.audio.SoundManager;
import engine.enums.GameMode;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class GameModeSelector extends JPanel {

    private Image background;
    private final GameWindow window;
    private JButton backButton;

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

        backButton = createBackButton();
        add(backButton);
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
        if (backButton != null) {
            int btnWidth = 214;
            int btnHeight = 38;
            int centerX = getWidth() / 2 - btnWidth / 2;
            int topMargin = 480;
            int buttonGap = 72;
            backButton.setBounds(centerX, topMargin + (buttonGap * 3) + 4, btnWidth, btnHeight);
        }
    }

    private void createButtons(){

        int btnWidth = 250;
        int btnHeight = 50;
        int centerX = screenWidth/2 - btnWidth/2;
        int topMargin = 480;
        int buttonGap = 72;

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

    private JButton createBackButton() {
        final String label = "Back";
        JButton button = new JButton("") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // subtle drop shadow
                g2.setColor(new Color(8, 6, 4, 100));
                g2.fillRoundRect(4, 4, getWidth(), getHeight(), 12, 12);

                // button background
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                // subtle highlight
                GradientPaint gp = new GradientPaint(0, 0, new Color(160,130,100,40), 0, getHeight(), new Color(0,0,0,0));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight() / 2, 12, 12);
                g2.setPaint(null);

                // border
                g2.setColor(new Color(40, 30, 22, 180));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

                g2.setFont(FontManager.getFont(32).deriveFont(Font.BOLD));
                g2.setColor(new Color(246, 244, 240));
                ButtonTextRenderer.drawCenteredText(g2, this, label, 5);
                g2.dispose();
            }
        };

        button.setFont(FontManager.getFont(32).deriveFont(Font.BOLD));
        button.setForeground(new Color(246, 244, 240));
        button.setBackground(new Color(115, 90, 60));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder());

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(140, 110, 80));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(115, 90, 60));
            }
        });

        button.addActionListener(e -> {
            sfx.setFile(8);
            sfx.play();
            window.showIntro();
        });

        return button;
    }

    private JButton createRPGButton(String text){

        final String label = text;
        JButton btn = new JButton("") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // shadow
                g2.setColor(new Color(8, 6, 4, 100));
                g2.fillRoundRect(4, 4, getWidth(), getHeight(), 12, 12);

                // background
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                // highlight
                GradientPaint gp = new GradientPaint(0, 0, new Color(160,130,100,40), 0, getHeight(), new Color(0,0,0,0));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight() / 2, 12, 12);
                g2.setPaint(null);

                // border
                g2.setColor(new Color(40, 30, 22, 180));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

                g2.setFont(FontManager.getFont(32).deriveFont(Font.BOLD));
                g2.setColor(new Color(246, 244, 240));
                ButtonTextRenderer.drawCenteredText(g2, this, label, 5);
                g2.dispose();
            }
        };
        Font boldFont = FontManager.getFont(32).deriveFont(Font.BOLD);

        btn.setFont(boldFont);
        btn.setForeground(new Color(246,244,240));
        btn.setBackground(new Color(115,90,60));

        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder());

        btn.addMouseListener(new java.awt.event.MouseAdapter(){

            public void mouseEntered(java.awt.event.MouseEvent evt){
                btn.setBackground(new Color(140,110,80));
            }

            public void mouseExited(java.awt.event.MouseEvent evt){
                btn.setBackground(new Color(115,90,60));
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
