package engine;

/**
 * Describes one playable character's animation data and on-screen draw size.
 * Add a new instance to GamePanel.ALL_CHARACTERS to register a new character.
 */
public class CharacterDef {
    public static class AnimationDef {
        public final String sheetPath;
        public final int frameWidth;
        public final int frameHeight;
        public final int frameDelayMs;

        public AnimationDef(String sheetPath, int frameWidth, int frameHeight, int frameDelayMs) {
            this.sheetPath = sheetPath;
            this.frameWidth = frameWidth;
            this.frameHeight = frameHeight;
            this.frameDelayMs = frameDelayMs;
        }
    }

    public final String name;
    public final AnimationDef idleAnimation;
    public final AnimationDef deadAnimation;
    public final int drawWidth;
    public final int drawHeight;

    public CharacterDef(String name, AnimationDef idleAnimation, AnimationDef deadAnimation,
                        int drawWidth, int drawHeight) {
        this.name = name;
        this.idleAnimation = idleAnimation;
        this.deadAnimation = deadAnimation;
        this.drawWidth = drawWidth;
        this.drawHeight = drawHeight;
    }
}
