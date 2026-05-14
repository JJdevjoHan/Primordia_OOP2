package engine.core;

import engine.enums.GameMode;
import engine.gameplay.BotAI;
import engine.gameplay.GamePanel;
import engine.gameplay.SurivivalGamePanel;

public class GameFactory {

    public static GamePanel createPvPGame(
            GameWindow window,
            int p1,
            int p2
    ) {
        return new GamePanel(window, p1, p2);
    }

    public static SurivivalGamePanel createSurvivalGame(
            GameWindow window,
            int player,
            int bot
    ) {
        return new SurivivalGamePanel(
                window,
                player,
                bot,
                GameMode.SURVIVAL,
                BotAI.Difficulty.EASY
        );
    }
}