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
    public final String skill1Name;
    public final String skill2Name;
    public final String skill3Name;
    public final String skill1Type;
    public final String skill2Type;
    public final String skill3Type;
    public final String skill1SpritePath;
    public final String skill2SpritePath;
    public final String skill3SpritePath;
    public final int skill1ForwardOffsetX;
    public final int skill2ForwardOffsetX;
    public final int skill3ForwardOffsetX;
    public final double skill1HurtTriggerBufferSeconds;
    public final double skill2HurtTriggerBufferSeconds;
    public final double skill3HurtTriggerBufferSeconds;
    public final AnimationDef idleAnimation;
    public final AnimationDef hurtAnimation;
    public final AnimationDef deadAnimation;
    public final int drawWidth;
    public final int drawHeight;

    public CharacterDef(String name,
                        String skill1Name, String skill2Name, String skill3Name,
                        String skill1Type, String skill2Type, String skill3Type,
                        String skill1SpritePath, String skill2SpritePath, String skill3SpritePath,
                        int skill1ForwardOffsetX, int skill2ForwardOffsetX, int skill3ForwardOffsetX,
                        double skill1HurtTriggerBufferSeconds, double skill2HurtTriggerBufferSeconds, double skill3HurtTriggerBufferSeconds,
                        AnimationDef idleAnimation, AnimationDef hurtAnimation, AnimationDef deadAnimation,
                        int drawWidth, int drawHeight) {
        this.name = name;
        this.skill1Name = skill1Name;
        this.skill2Name = skill2Name;
        this.skill3Name = skill3Name;
        this.skill1Type = skill1Type;
        this.skill2Type = skill2Type;
        this.skill3Type = skill3Type;
        this.skill1SpritePath = skill1SpritePath;
        this.skill2SpritePath = skill2SpritePath;
        this.skill3SpritePath = skill3SpritePath;
        this.skill1ForwardOffsetX = skill1ForwardOffsetX;
        this.skill2ForwardOffsetX = skill2ForwardOffsetX;
        this.skill3ForwardOffsetX = skill3ForwardOffsetX;
        this.skill1HurtTriggerBufferSeconds = skill1HurtTriggerBufferSeconds;
        this.skill2HurtTriggerBufferSeconds = skill2HurtTriggerBufferSeconds;
        this.skill3HurtTriggerBufferSeconds = skill3HurtTriggerBufferSeconds;
        this.idleAnimation = idleAnimation;
        this.hurtAnimation = hurtAnimation;
        this.deadAnimation = deadAnimation;
        this.drawWidth = drawWidth;
        this.drawHeight = drawHeight;
    }

    public String getSkillName(int skillID) {
        return switch (skillID) {
            case 1 -> skill1Name;
            case 2 -> skill2Name;
            case 3 -> skill3Name;
            default -> "Skill";
        };
    }

    public String getSkillSpritePath(int skillID) {
        return switch (skillID) {
            case 1 -> skill1SpritePath;
            case 2 -> skill2SpritePath;
            case 3 -> skill3SpritePath;
            default -> null;
        };
    }

    public String getSkillType(int skillID) {
        return switch (skillID) {
            case 1 -> skill1Type;
            case 2 -> skill2Type;
            case 3 -> skill3Type;
            default -> "";
        };
    }

    public int getSkillForwardOffsetX(int skillID) {
        return switch (skillID) {
            case 1 -> skill1ForwardOffsetX;
            case 2 -> skill2ForwardOffsetX;
            case 3 -> skill3ForwardOffsetX;
            default -> 0;
        };
    }

    public double getSkillHurtTriggerBufferSeconds(int skillID) {
        return switch (skillID) {
            case 1 -> skill1HurtTriggerBufferSeconds;
            case 2 -> skill2HurtTriggerBufferSeconds;
            case 3 -> skill3HurtTriggerBufferSeconds;
            default -> 0.0;
        };
    }
}
