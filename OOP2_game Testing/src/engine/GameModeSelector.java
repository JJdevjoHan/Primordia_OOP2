package engine;

import assets.Utility.FontManager;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class GameModeSelector extends JPanel {

    private Image background;
    private final GameWindow window;

    private final int screenWidth = 1536;

    public GameModeSelector(GameWindow window) {

        this.window = window;

        int screenHeight = 896;
        setPreferredSize(new Dimension(screenWidth, screenHeight));
        setLayout(null);

        loadBackground();

        //createTitle();
        createButtons();
    }

    private void loadBackground() {

        URL resource = getClass().getResource("/assets/maps/menu2.png");

        if(resource != null){
            background = new ImageIcon(resource).getImage();
        }
    }


    private void createButtons(){

        int btnWidth = 250;
        int btnHeight = 50;
        int centerX = screenWidth/2 - btnWidth/2;

        JButton pvp = createRPGButton("Versus");
        JButton survival = createRPGButton("Survival");
        JButton arcade = createRPGButton("Arcade");

        pvp.setBounds(centerX, 450, btnWidth, btnHeight);
        survival.setBounds(centerX, 540, btnWidth, btnHeight);
        arcade.setBounds(centerX, 630, btnWidth, btnHeight);

        add(pvp);
        add(survival);
        add(arcade);

        pvp.addActionListener(ActionEvent_ -> window.showCharacterSelection());

    }

    private JButton createRPGButton(String text){

        JButton btn = new JButton(text);
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
