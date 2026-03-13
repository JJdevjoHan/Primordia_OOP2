package engine;

import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel {
    private final int tileSize = 128;
    private final int screenWidth = tileSize * 12;
    private final int screenHeight = tileSize * 7;

    // Assets
    private Image backgroundImage;
    private ImageIcon p1Sprite, p2Sprite;

    // Game State
    private int p1HP = 100, p2HP = 100;
    private boolean isP1Turn = true;

    // UI Elements - These now ONLY hold buttons
    private JPanel p1ButtonPanel, p2ButtonPanel;
    private JLabel turnLabel, p1HPLabel, p2HPLabel;

    public GamePanel() {

        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setFocusable(true);
        this.addKeyListener(new KeyInputs(this));

        // Use Null Layout to place buttons at exact X/Y coordinate below the sprites
        this.setLayout(null);


        try {
            backgroundImage = new ImageIcon(getClass().getResource("/assets/plains background.png")).getImage();
            p1Sprite = new ImageIcon(getClass().getResource("/assets/Stickman.gif"));
            p2Sprite = new ImageIcon(getClass().getResource("/assets/Stickman mirrored.gif"));
        } catch (Exception e) {
            System.err.println("Error: wala ang assesst nakitan!");
        }

        // Turn Indicator Label
        turnLabel = new JLabel("PLAYER 1'S TURN", SwingConstants.CENTER);
        turnLabel.setFont(new Font("Monospaced", Font.BOLD, 40));
        turnLabel.setForeground(Color.BLACK);
        turnLabel.setBounds(-50, 500, screenWidth, 50);
        this.add(turnLabel);

        // HP ni player 1
        p1HPLabel = new JLabel("HP: 100", SwingConstants.CENTER);
        p1HPLabel.setFont(new Font("Arial", Font.BOLD, 22));
        p1HPLabel.setForeground(Color.black);
        p1HPLabel.setBounds(340, 450, 150, 30);
        this.add(p1HPLabel);


        //HP ni player 2
        p2HPLabel = new JLabel("HP: 100", SwingConstants.CENTER);
        p2HPLabel.setFont(new Font("Arial", Font.BOLD, 22));
        p2HPLabel.setForeground(Color.black);
        p2HPLabel.setBounds(screenWidth - 600, 450, 150, 30);
        this.add(p2HPLabel);

        // Button Panels ( naa sa Y = 340, which is below the Y=200 sprites)
        //buttons ni player 1
        p1ButtonPanel = createSkillUI();
        p1ButtonPanel.setBounds(350, 500, tileSize, 80);
        this.add(p1ButtonPanel);

        //buttons ni player 2
        p2ButtonPanel = createSkillUI();
        p2ButtonPanel.setBounds(screenWidth - 590, 500, tileSize, 80);
        this.add(p2ButtonPanel);

        updateGameState();
    }

    private JPanel createSkillUI() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 4, 4));
        panel.setOpaque(false); // Transparent so background image shows through gaps

        String[] skills = {"Atk", "Heal", "S.Atk", "S.Heal"};
        for (int i = 0; i < 4; i++) {
            JButton btn = new JButton(skills[i]);
            btn.setFocusable(false);
            btn.setFont(new Font("Arial", Font.BOLD, 10));
            btn.setBackground(new Color(255, 255, 255, 180));
            btn.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

            int skillID = i + 1;
            btn.addActionListener(e -> executeSkill(skillID));
            panel.add(btn);
        }
        return panel;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        //Background
        if (backgroundImage != null) {
            g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }

        //player 1 sprite
        if (p1HP > 0 && p1Sprite != null) {
            g2.drawImage(p1Sprite.getImage(), 350, 300, tileSize, tileSize, this);
        }

        //player 2 sprite
        if (p2HP > 0 && p2Sprite != null) {
            g2.drawImage(p2Sprite.getImage(), screenWidth - 580, 300, tileSize, tileSize, this);
        }
    }

    public void executeSkill(int skillID) {
        if (p1HP <= 0 || p2HP <= 0) return;

        if (isP1Turn) {
            switch (skillID) {
                case 1 -> p2HP -= 10;
                case 2 -> p1HP = Math.min(100, p1HP + 10);
                case 3 -> p2HP -= 25;
                case 4 -> p1HP = Math.min(100, p1HP + 30);
            }
        } else {
            switch (skillID) {
                case 1 -> p1HP -= 10;
                case 2 -> p2HP = Math.min(100, p2HP + 10);
                case 3 -> p1HP -= 25;
                case 4 -> p2HP = Math.min(100, p2HP + 30);
            }
        }

        isP1Turn = !isP1Turn;
        updateGameState();
        repaint();
    }

    private void updateGameState() {
        p1HPLabel.setText("HP: " + p1HP);
        p2HPLabel.setText("HP: " + p2HP);

        if (p1HP <= 0 || p2HP <= 0) {
            turnLabel.setText(p1HP <= 0 ? "PLAYER 2 WINS!" : "PLAYER 1 WINS!");
            p1ButtonPanel.setVisible(false);
            p2ButtonPanel.setVisible(false);
        } else {
            turnLabel.setText(isP1Turn ? "PLAYER 1'S TURN" : "PLAYER 2'S TURN");

            // Toggle visibility of BUTTONS only
            p1ButtonPanel.setVisible(isP1Turn);
            p2ButtonPanel.setVisible(!isP1Turn);
        }
    }
}
