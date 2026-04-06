package assets.Utility;

import engine.GameWindow;

import javax.swing.*;
import java.awt.*;

public class BackButton {

    public JButton createBackButton(GameWindow window, JPanel panel) {

        JButton backBtn = new JButton("Back");

        backBtn.setFocusPainted(false);
        backBtn.setFont(new Font("Arial", Font.BOLD, 14));
        backBtn.setForeground(Color.WHITE);
        backBtn.setBackground(new Color(40, 40, 60));
        backBtn.setBorder(BorderFactory.createLineBorder(new Color(200, 160, 40), 2));

        backBtn.addActionListener(e -> {
            System.out.println("Back button pressed!"); // <- see if this prints
            int confirm = JOptionPane.showConfirmDialog(
                    panel,
                    "Return to menu?",
                    "Confirm",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {
                System.out.println("Switching to menu...");
                window.showMenu(); // this should switch
            }
        });

        return backBtn;
    }
}