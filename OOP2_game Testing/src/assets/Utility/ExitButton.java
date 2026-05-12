package assets.Utility;
import engine.GameWindow;
import javax.swing.*;
import java.awt.*;

public class ExitButton {

    public JButton createExitButton(JPanel panel) {
        final String label = "X";
        JButton exitButton = new JButton("") {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                assets.Utility.ButtonTextRenderer.drawCenteredText(g2, this, label, 5);
                g2.dispose();
            }
        };

        exitButton.setFont(assets.Utility.FontManager.getFont(30f));
        //exitButton.setFont(new Font("Arial", Font.BOLD, 16));
        exitButton.setForeground(Color.WHITE);
        exitButton.setBackground(new Color(180, 40, 40));
        exitButton.setOpaque(true);
        exitButton.setContentAreaFilled(true);
        exitButton.setFocusPainted(false);
        exitButton.setBorder(BorderFactory.createEmptyBorder());

        exitButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exitButton.setBackground(new Color(220, 60, 60));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exitButton.setBackground(new Color(180, 40, 40));
            }
        });

        // Try to get the GameWindow from the panel and call its close handler
        exitButton.addActionListener(e -> {
            GameWindow window = getGameWindow(panel);
            if (window != null) {
                window.handleExitButtonClick();
            } else {
                System.exit(0);
            }
        });

        return exitButton;
    }
    
    private GameWindow getGameWindow(JPanel panel) {
        Component comp = panel;
        while (comp != null) {
            if (comp instanceof JFrame) {
                JFrame frame = (JFrame) comp;
                if (frame instanceof GameWindow) {
                    return (GameWindow) frame;
                }
            }
            comp = comp.getParent();
        }
        return null;
    }
}
