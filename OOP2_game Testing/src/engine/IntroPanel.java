package engine;

import main.Main;
import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class IntroPanel extends JPanel {

    private Image backgroundImage;
    private GameWindow window;

    private final int tileSize = 128;
    private final int screenWidth = tileSize * 12;
    private final int screenHeight = tileSize * 7;

    public IntroPanel(GameWindow window) {

        this.window = window;

        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setLayout(null);

        loadBackground("/assets/maps/splashscreen.png");

        createStartButton();
    }

    private void createStartButton() {
        JButton startButton = new JButton("START") {
            @Override
            protected void paintComponent(Graphics g) {
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
                g2.dispose();
            }
        };

        int btnWidth = 200;
        int btnHeight = 50;

        startButton.setBounds(
                screenWidth / 2 - btnWidth / 2,
                screenHeight / 2 - btnHeight / 2,
                btnWidth,
                btnHeight
        );
        startButton.setFont(new Font("Monospaced", Font.BOLD, 32));
        startButton.setForeground(Color.WHITE);
        startButton.setBackground(new Color(40, 40, 40));
        startButton.setFocusPainted(false);

        // Hover effects
        startButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                startButton.setBackground(new Color(70, 70, 70));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                startButton.setBackground(new Color(40, 40, 40));
            }
        });

        Toolkit.getDefaultToolkit().beep();
        startButton.addActionListener(e -> window.showGamePanel());

        add(startButton);
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