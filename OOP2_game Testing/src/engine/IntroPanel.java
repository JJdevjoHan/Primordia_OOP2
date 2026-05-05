package engine;

import assets.Utility.ButtonTextRenderer;
import assets.Utility.CreditsButton;
import assets.Utility.ExitButton;
import assets.Utility.FontManager;
import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class IntroPanel extends JPanel {

    private Image backgroundImage;
    private GameWindow window;
    private JButton creditsButton;
    private JButton exitButton;

    private final int tileSize = 128;
    private final int screenWidth = tileSize * 12;
    private final int screenHeight = tileSize * 7;

    private final SoundManager sound = new SoundManager();
    public IntroPanel(GameWindow window) {
        this.window = window;
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setLayout(null);
        loadBackground("/assets/maps/splashscreen.png");
        createStartButton();

        creditsButton = new CreditsButton().createCreditsButton(window, sound);
        exitButton = new ExitButton().createExitButton(this);
        add(exitButton);
        add(creditsButton);
    }

    private void createStartButton() {
        final String startLabel = "START";
        JButton startButton = new JButton("") {
            @Override
            protected void paintComponent(Graphics g) {

                /*

                label.setFont(FontManager.getFont(24f));
                title.setFont(FontManager.getFont(32f));

                 */

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Rounded background
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);

                // Rounded border
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(4));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 30, 30);

                super.paintComponent(g2);

                ButtonTextRenderer.drawCenteredText(g2, this, startLabel, 5);
                g2.dispose();
            }
        };

        int btnWidth = 220;
        int btnHeight = 50;

        startButton.setBounds(
                screenWidth / 2 - btnWidth / 2,
                screenHeight / 2 - btnHeight / 2,
                btnWidth,
                btnHeight
        );

        startButton.setFont(FontManager.getFont(35).deriveFont(Font.BOLD));
        startButton.setForeground(Color.WHITE);
        startButton.setBackground(new Color(30,30,50));
        startButton.setFocusPainted(false);
        startButton.setBorder(BorderFactory.createLineBorder(new Color(200,160,40),1));

        // Hover effects
        startButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                startButton.setBackground(new Color(70, 70, 100));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                startButton.setBackground(new Color(30, 30, 50));
            }
        });

        Toolkit.getDefaultToolkit().beep();
        startButton.addActionListener(e -> {

            sound.setFile(8);
            sound.play();
            window.showMenu();
        });

        add(startButton);
    }


    @Override
    public void doLayout() {
        super.doLayout();
        int margin = 20;
        if (creditsButton != null) {
            int leftMargin = 40;
            int bottomMargin = 60;
            creditsButton.setBounds(leftMargin, getHeight() - 40 - bottomMargin, 150, 40);
        }
        if (exitButton != null) {
            exitButton.setBounds(getWidth() - 50 - margin, margin, 40, 40);
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