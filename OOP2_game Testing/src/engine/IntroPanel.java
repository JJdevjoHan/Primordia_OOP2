package engine;

import assets.Utility.ButtonTextRenderer;
import assets.Utility.FontManager;
import assets.Utility.SurvivalLeaderboardPopupDialog;
import java.awt.*;
import java.net.URL;
import javax.swing.*;

public class IntroPanel extends JPanel {

    private Image backgroundImage;
    private GameWindow window;
    private JButton startButton;
    private JButton creditsButton;
    private JButton settingsButton;
    private JButton quitButton;
    private JButton leaderboardButton;

    private final int tileSize = 128;
    private final int screenWidth = tileSize * 12;
    private final int screenHeight = tileSize * 7;

    private final SoundManager sound = new SoundManager();
    public IntroPanel(GameWindow window) {
        this.window = window;
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setLayout(null);
        loadBackground("/assets/maps/splashscreen.png");
        createButtons();
    }

    private void createButtons() {
        startButton = createStyledButton("START");
        creditsButton = createStyledButton("CREDITS");
        settingsButton = createStyledButton("SETTINGS");
        quitButton = createStyledButton("QUIT");
        leaderboardButton = createStyledButton("LEADERBOARD");

        startButton.addActionListener(e -> {
            sound.setFile(8);
            sound.play();
            window.showMenu();
        });

        creditsButton.addActionListener(e -> {
            sound.setFile(8);
            sound.play();
            window.showCredits();
        });

        settingsButton.addActionListener(e -> {
            sound.setFile(8);
            sound.play();
            window.showSettingsMenu();
        });

        quitButton.addActionListener(e -> {
            sound.setFile(8);
            sound.play();
            System.exit(0);
        });

        leaderboardButton.addActionListener(e -> {
            sound.setFile(8);
            sound.play();
            SurvivalLeaderboardPopupDialog popup = new SurvivalLeaderboardPopupDialog(window);
            popup.setLocationRelativeTo(window);
            popup.setVisible(true);
        });

        add(startButton);
        add(creditsButton);
        add(settingsButton);
        add(quitButton);
        add(leaderboardButton);
    }

    private JButton createStyledButton(String label) {
        JButton button = new JButton("") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                GradientPaint gp = new GradientPaint(0, 0, new Color(160, 130, 100, 40), 0, getHeight(), new Color(0, 0, 0, 0));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight() / 2, 12, 12);
                g2.setPaint(null);

                g2.setColor(new Color(40, 30, 22, 180));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

                g2.setFont(FontManager.getFont(35).deriveFont(Font.BOLD));
                g2.setColor(new Color(246, 244, 240));
                ButtonTextRenderer.drawCenteredText(g2, this, label, 5);
                g2.dispose();
            }
        };

        button.setFont(FontManager.getFont(35).deriveFont(Font.BOLD));
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

        return button;
    }


    @Override
    public void doLayout() {
        super.doLayout();
        int btnWidth = 220;
        int btnHeight = 60;
        int centerX = getWidth() / 2 - btnWidth / 2;
        int centerY = getHeight() / 2 - btnHeight / 2;
        int gap = 20;

        if (startButton != null) {
            startButton.setBounds(centerX, centerY, btnWidth, btnHeight);
        }
        if (settingsButton != null) {
            settingsButton.setBounds(centerX, centerY + btnHeight + gap, btnWidth, btnHeight);
        }
        if (creditsButton != null) {
            creditsButton.setBounds(centerX, centerY + (btnHeight + gap) * 2, btnWidth, btnHeight);
        }
        if (quitButton != null) {
            quitButton.setBounds(centerX, centerY + (btnHeight + gap) * 3, btnWidth, btnHeight);
        }
        if (leaderboardButton != null) {
            int leaderboardWidth = 180;
            int leaderboardHeight = 44;
            int bottomLeftX = 24;
            int bottomLeftY = getHeight() - leaderboardHeight - 24;
            leaderboardButton.setBounds(bottomLeftX, bottomLeftY, leaderboardWidth, leaderboardHeight);
        }
    }

    private void loadBackground(String path) {

        URL resource = getClass().getResource(path);

        if(resource != null){
            backgroundImage = new ImageIcon(resource).getImage();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if(backgroundImage != null){
            g.drawImage(backgroundImage,0,0,getWidth(),getHeight(),this);
        }
    }


}