package main;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {

        // object declarations
        JFrame window = new JFrame();
        GamePanel gamePanel = new GamePanel();

        //features sa window
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(true);
        window.setTitle("Game");

        window.add(gamePanel);
        window.pack();

        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }
}