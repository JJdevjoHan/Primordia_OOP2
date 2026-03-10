package main;

import javax.swing.*;
import java.awt.*;

// subclass na nag inherit sa JPanel

public class GamePanel extends JPanel {

    int orig_TileSize = 32; // 16x16
    int scaling = 3;   // tile_size multiplayier
    int TileSize = orig_TileSize * scaling; // actual size of the tile

    int TileperRow = 19;
    int TileperCol = 10;

    int ScreenWidth = TileSize * TileperRow;
    int ScreenHeight= TileSize * TileperCol;

    public GamePanel(){
        this.setPreferredSize(new Dimension(ScreenWidth, ScreenHeight));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
    }





}
