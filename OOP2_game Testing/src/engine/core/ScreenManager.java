package engine.core;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ScreenManager {

    private final JPanel container;
    private final CardLayout layout;

    private final Map<GameState, Component> screens = new HashMap<>();

    private GameState currentState;

    public ScreenManager() {
        layout = new CardLayout();
        container = new JPanel(layout);
    }

    public JPanel getContainer() {
        return container;
    }

    public void addScreen(GameState state, Component panel) {
        screens.put(state, panel);
        container.add(panel, state.name());
    }

    public void showScreen(GameState state) {
        currentState = state;
        layout.show(container, state.name());
    }

    public void removeScreen(GameState state) {
        Component comp = screens.remove(state);

        if (comp != null) {
            container.remove(comp);
        }
    }

    public Component getScreen(GameState state) {
        return screens.get(state);
    }

    public GameState getCurrentState() {
        return currentState;
    }
}