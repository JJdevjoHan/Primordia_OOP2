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

    public static class ProjectileDef {
        public final String sheetPath;
        public final int frameWidth;
        public final int frameHeight;
        public final int drawWidth;
        public final int drawHeight;
        public final int speed;
        public final int verticalOffset;
        public final int spawnOffsetX;
        public final boolean beam;
        public final boolean startDuringCast;
        public final boolean anchorOnTargetCenter;
        public final boolean anchorOnTarget;
        public final int loopStartFrame;
        public final int loopEndFrame;
        public final int impactStartFrame;
        public final int impactEndFrame;
        public final String impactSheetPath;
        public final int impactFrameWidth;
        public final int impactFrameHeight;
        public final int impactDrawWidth;
        public final int impactDrawHeight;
        public final boolean anchorImpactOnTargetCenter;
        public final int animationFrameDelay;

        public ProjectileDef(String sheetPath,
                             int frameWidth,
                             int frameHeight,
                             int drawWidth,
                             int drawHeight,
                             int speed,
                     int verticalOffset) {
                        this(sheetPath, frameWidth, frameHeight, drawWidth, drawHeight, speed, verticalOffset, 0,
                false, false, false, false, 0, 0, 0, 0,
                null, 0, 0, 0, 0, false, 0);
        }

                    public ProjectileDef(String sheetPath,
                                 int frameWidth,
                                 int frameHeight,
                                 int drawWidth,
                                 int drawHeight,
                                 int speed,
                                 int verticalOffset,
                                 int spawnOffsetX) {
                        this(sheetPath, frameWidth, frameHeight, drawWidth, drawHeight, speed, verticalOffset,
                                spawnOffsetX, false, false, false, false, 0, 0, 0, 0,
                                null, 0, 0, 0, 0, false, 0);
                    }

        public ProjectileDef(String sheetPath,
                             int frameWidth,
                             int frameHeight,
                             int drawWidth,
                             int drawHeight,
                             int speed,
                             int verticalOffset,
                             int spawnOffsetX,
                             boolean beam,
                             boolean startDuringCast,
                             boolean anchorOnTargetCenter,
                             boolean anchorOnTarget,
                             int loopStartFrame,
                             int loopEndFrame,
                             int impactStartFrame,
                             int impactEndFrame,
                             String impactSheetPath,
                             int impactFrameWidth,
                             int impactFrameHeight,
                             int impactDrawWidth,
                             int impactDrawHeight) {
            this.sheetPath = sheetPath;
            this.frameWidth = frameWidth;
            this.frameHeight = frameHeight;
            this.drawWidth = drawWidth;
            this.drawHeight = drawHeight;
            this.speed = speed;
            this.verticalOffset = verticalOffset;
            this.spawnOffsetX = spawnOffsetX;
            this.beam = beam;
            this.startDuringCast = startDuringCast;
            this.anchorOnTargetCenter = anchorOnTargetCenter;
            this.anchorOnTarget = anchorOnTarget;
            this.loopStartFrame = loopStartFrame;
            this.loopEndFrame = loopEndFrame;
            this.impactStartFrame = impactStartFrame;
            this.impactEndFrame = impactEndFrame;
            this.impactSheetPath = impactSheetPath;
            this.impactFrameWidth = impactFrameWidth;
            this.impactFrameHeight = impactFrameHeight;
            this.impactDrawWidth = impactDrawWidth;
            this.impactDrawHeight = impactDrawHeight;
            this.anchorImpactOnTargetCenter = false;
            this.animationFrameDelay = 0;
        }

        public ProjectileDef(String sheetPath,
                     int frameWidth,
                     int frameHeight,
                     int drawWidth,
                     int drawHeight,
                     int speed,
                             int verticalOffset,
                                 int spawnOffsetX,
                             boolean beam,
                             boolean startDuringCast,
                             boolean anchorOnTargetCenter,
                             boolean anchorOnTarget,
                             int loopStartFrame,
                             int loopEndFrame,
                             int impactStartFrame,
                             int impactEndFrame,
                             String impactSheetPath,
                             int impactFrameWidth,
                             int impactFrameHeight,
                             int impactDrawWidth,
                             int impactDrawHeight,
                             boolean anchorImpactOnTargetCenter,
                             int animationFrameDelay) {
            this.sheetPath = sheetPath;
            this.frameWidth = frameWidth;
            this.frameHeight = frameHeight;
            this.drawWidth = drawWidth;
            this.drawHeight = drawHeight;
            this.speed = speed;
            this.verticalOffset = verticalOffset;
            this.spawnOffsetX = spawnOffsetX;
            this.beam = beam;
            this.startDuringCast = startDuringCast;
            this.anchorOnTargetCenter = anchorOnTargetCenter;
            this.anchorOnTarget = anchorOnTarget;
            this.loopStartFrame = loopStartFrame;
            this.loopEndFrame = loopEndFrame;
            this.impactStartFrame = impactStartFrame;
            this.impactEndFrame = impactEndFrame;
            this.impactSheetPath = impactSheetPath;
            this.impactFrameWidth = impactFrameWidth;
            this.impactFrameHeight = impactFrameHeight;
            this.impactDrawWidth = impactDrawWidth;
            this.impactDrawHeight = impactDrawHeight;
            this.anchorImpactOnTargetCenter = anchorImpactOnTargetCenter;
            this.animationFrameDelay = animationFrameDelay;
        }

        public ProjectileDef(String sheetPath,
                     int frameWidth,
                     int frameHeight,
                     int drawWidth,
                     int drawHeight,
                     int speed,
                             int verticalOffset,
                                 int spawnOffsetX,
                             boolean beam,
                             boolean startDuringCast,
                             boolean anchorOnTargetCenter,
                             boolean anchorOnTarget,
                             int loopStartFrame,
                             int loopEndFrame,
                             int impactStartFrame,
                             int impactEndFrame) {
            this(sheetPath, frameWidth, frameHeight, drawWidth, drawHeight, speed, verticalOffset, spawnOffsetX,
                    beam, startDuringCast, anchorOnTargetCenter, anchorOnTarget,
                    loopStartFrame, loopEndFrame, impactStartFrame, impactEndFrame,
                    null, 0, 0, 0, 0, false, 0);
        }
    }

    public static class DefenseFormDef {
        public final int toggleSkillSlot;
        public final int altSkillSlot;
        public final int enterFreezeFrame;
        public final int exitStartFrame;
        public final AnimationDef altSkillAnimation;
        public final ProjectileDef altSkillProjectile;

        public DefenseFormDef(int toggleSkillSlot,
                              int altSkillSlot,
                              int enterFreezeFrame,
                              int exitStartFrame,
                              AnimationDef altSkillAnimation,
                              ProjectileDef altSkillProjectile) {
            this.toggleSkillSlot = toggleSkillSlot;
            this.altSkillSlot = altSkillSlot;
            this.enterFreezeFrame = enterFreezeFrame;
            this.exitStartFrame = exitStartFrame;
            this.altSkillAnimation = altSkillAnimation;
            this.altSkillProjectile = altSkillProjectile;
        }
    }

    public final String name;
    public final String backstory;
    public final String skill1Name;
    public final String skill2Name;
    public final String skill3Name;
    public final String skill1Description;
    public final String skill2Description;
    public final String skill3Description;
    public final String skill1Type;
    public final String skill2Type;
    public final String skill3Type;
    public final int skill1Power;
    public final int skill2Power;
    public final int skill3Power;
    public final String skill1SpritePath;
    public final String skill1FollowUpSpritePath;
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
    public final ProjectileDef skill1Projectile;
    public final ProjectileDef skill2Projectile;
    public final ProjectileDef skill3Projectile;
    public final DefenseFormDef defenseForm;
    public final int drawWidth;
    public final int drawHeight;
    // Wind Wizard specific scaling (defaults for other characters)
    public final int skill2Player2OffsetX;
    public final int skill3OffsetX;
    public final int skill3OffsetY;
    public final int shadowOffsetX;
    public final double skill3Scale;
    public final double shadowScale;

    public CharacterDef(String name,
                        String backstory,
                        String skill1Name, String skill2Name, String skill3Name,
                        String skill1Description, String skill2Description, String skill3Description,
                        String skill1Type, String skill2Type, String skill3Type,
                        String skill1SpritePath, String skill1FollowUpSpritePath, String skill2SpritePath, String skill3SpritePath,
                        int skill1ForwardOffsetX, int skill2ForwardOffsetX, int skill3ForwardOffsetX,
                        double skill1HurtTriggerBufferSeconds, double skill2HurtTriggerBufferSeconds, double skill3HurtTriggerBufferSeconds,
                        AnimationDef idleAnimation, AnimationDef hurtAnimation, AnimationDef deadAnimation,
                        int drawWidth, int drawHeight) {
        this(name, backstory,
            skill1Name, skill2Name, skill3Name,
            skill1Description, skill2Description, skill3Description,
            skill1Type, skill2Type, skill3Type,
            10, 10, 25,
            skill1SpritePath, skill1FollowUpSpritePath, skill2SpritePath, skill3SpritePath,
            skill1ForwardOffsetX, skill2ForwardOffsetX, skill3ForwardOffsetX,
            skill1HurtTriggerBufferSeconds, skill2HurtTriggerBufferSeconds, skill3HurtTriggerBufferSeconds,
            idleAnimation, hurtAnimation, deadAnimation,
            null, null, null,
            null,
            drawWidth, drawHeight,
                0, 0, 0, 0, 1.0, 1.0);
        }

    public CharacterDef(String name,
                        String backstory,
                        String skill1Name, String skill2Name, String skill3Name,
                        String skill1Description, String skill2Description, String skill3Description,
                        String skill1Type, String skill2Type, String skill3Type,
                        int skill1Power, int skill2Power, int skill3Power,
                        String skill1SpritePath, String skill1FollowUpSpritePath, String skill2SpritePath, String skill3SpritePath,
                        int skill1ForwardOffsetX, int skill2ForwardOffsetX, int skill3ForwardOffsetX,
                        double skill1HurtTriggerBufferSeconds, double skill2HurtTriggerBufferSeconds, double skill3HurtTriggerBufferSeconds,
                        AnimationDef idleAnimation, AnimationDef hurtAnimation, AnimationDef deadAnimation,
                        int drawWidth, int drawHeight) {
        this(name, backstory,
                skill1Name, skill2Name, skill3Name,
                skill1Description, skill2Description, skill3Description,
                skill1Type, skill2Type, skill3Type,
                skill1Power, skill2Power, skill3Power,
                skill1SpritePath, skill1FollowUpSpritePath, skill2SpritePath, skill3SpritePath,
                skill1ForwardOffsetX, skill2ForwardOffsetX, skill3ForwardOffsetX,
                skill1HurtTriggerBufferSeconds, skill2HurtTriggerBufferSeconds, skill3HurtTriggerBufferSeconds,
                idleAnimation, hurtAnimation, deadAnimation,
                null, null, null,
                null,
                drawWidth, drawHeight,
                        0, 0, 0, 0, 1.0, 1.0);
    }

    public CharacterDef(String name,
                        String backstory,
                        String skill1Name, String skill2Name, String skill3Name,
                        String skill1Description, String skill2Description, String skill3Description,
                        String skill1Type, String skill2Type, String skill3Type,
                        String skill1SpritePath, String skill2SpritePath, String skill3SpritePath,
                        int skill1ForwardOffsetX, int skill2ForwardOffsetX, int skill3ForwardOffsetX,
                        double skill1HurtTriggerBufferSeconds, double skill2HurtTriggerBufferSeconds, double skill3HurtTriggerBufferSeconds,
                        AnimationDef idleAnimation, AnimationDef hurtAnimation, AnimationDef deadAnimation,
                        int drawWidth, int drawHeight) {
        this(name, backstory,
                skill1Name, skill2Name, skill3Name,
                skill1Description, skill2Description, skill3Description,
                skill1Type, skill2Type, skill3Type,
                10, 10, 25,
                skill1SpritePath, null, skill2SpritePath, skill3SpritePath,
                skill1ForwardOffsetX, skill2ForwardOffsetX, skill3ForwardOffsetX,
                skill1HurtTriggerBufferSeconds, skill2HurtTriggerBufferSeconds, skill3HurtTriggerBufferSeconds,
                idleAnimation, hurtAnimation, deadAnimation,
                null, null, null,
                null,
                drawWidth, drawHeight,
                0, 0, 0, 0, 1.0, 1.0);
    }

                public CharacterDef(String name,
                        String backstory,
                        String skill1Name, String skill2Name, String skill3Name,
                        String skill1Description, String skill2Description, String skill3Description,
                        String skill1Type, String skill2Type, String skill3Type,
                        int skill1Power, int skill2Power, int skill3Power,
                        String skill1SpritePath, String skill1FollowUpSpritePath, String skill2SpritePath, String skill3SpritePath,
                        int skill1ForwardOffsetX, int skill2ForwardOffsetX, int skill3ForwardOffsetX,
                        double skill1HurtTriggerBufferSeconds, double skill2HurtTriggerBufferSeconds, double skill3HurtTriggerBufferSeconds,
                        AnimationDef idleAnimation, AnimationDef hurtAnimation, AnimationDef deadAnimation,
                        ProjectileDef skill1Projectile, ProjectileDef skill2Projectile, ProjectileDef skill3Projectile,
                        DefenseFormDef defenseForm,
                        int drawWidth, int drawHeight,
                        int skill2Player2OffsetX, int skill3OffsetX, int skill3OffsetY,
                        int shadowOffsetX,
                        double skill3Scale, double shadowScale) {
        this.name = name;
        this.backstory = backstory == null ? "" : backstory;
        this.skill1Name = skill1Name;
        this.skill2Name = skill2Name;
        this.skill3Name = skill3Name;
        this.skill1Description = skill1Description == null ? "" : skill1Description;
        this.skill2Description = skill2Description == null ? "" : skill2Description;
        this.skill3Description = skill3Description == null ? "" : skill3Description;
        this.skill1Type = skill1Type;
        this.skill2Type = skill2Type;
        this.skill3Type = skill3Type;
        this.skill1Power = skill1Power;
        this.skill2Power = skill2Power;
        this.skill3Power = skill3Power;
        this.skill1SpritePath = skill1SpritePath;
        this.skill1FollowUpSpritePath = skill1FollowUpSpritePath;
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
        this.skill1Projectile = skill1Projectile;
        this.skill2Projectile = skill2Projectile;
        this.skill3Projectile = skill3Projectile;
        this.defenseForm = defenseForm;
        this.drawWidth = drawWidth;
        this.drawHeight = drawHeight;
        this.skill2Player2OffsetX = skill2Player2OffsetX;
        this.skill3OffsetX = skill3OffsetX;
        this.skill3OffsetY = skill3OffsetY;
        this.shadowOffsetX = shadowOffsetX;
        this.skill3Scale = skill3Scale;
        this.shadowScale = shadowScale;
    }

    public CharacterDef(String name,
                        String backstory,
                        String skill1Name, String skill2Name, String skill3Name,
                        String skill1Description, String skill2Description, String skill3Description,
                        String skill1Type, String skill2Type, String skill3Type,
                        int skill1Power, int skill2Power, int skill3Power,
                        String skill1SpritePath, String skill1FollowUpSpritePath, String skill2SpritePath, String skill3SpritePath,
                        int skill1ForwardOffsetX, int skill2ForwardOffsetX, int skill3ForwardOffsetX,
                        double skill1HurtTriggerBufferSeconds, double skill2HurtTriggerBufferSeconds, double skill3HurtTriggerBufferSeconds,
                        AnimationDef idleAnimation, AnimationDef hurtAnimation, AnimationDef deadAnimation,
                        ProjectileDef skill1Projectile, ProjectileDef skill2Projectile, ProjectileDef skill3Projectile,
                        int drawWidth, int drawHeight) {
        this(name, backstory,
                skill1Name, skill2Name, skill3Name,
                skill1Description, skill2Description, skill3Description,
                skill1Type, skill2Type, skill3Type,
                skill1Power, skill2Power, skill3Power,
                    skill1SpritePath, skill1FollowUpSpritePath, skill2SpritePath, skill3SpritePath,
                skill1ForwardOffsetX, skill2ForwardOffsetX, skill3ForwardOffsetX,
                skill1HurtTriggerBufferSeconds, skill2HurtTriggerBufferSeconds, skill3HurtTriggerBufferSeconds,
                idleAnimation, hurtAnimation, deadAnimation,
                skill1Projectile, skill2Projectile, skill3Projectile,
                null,
                drawWidth, drawHeight,
                0, 0, 0, 0, 1.0, 1.0);
    }

    public CharacterDef(String name,
                        String backstory,
                        String skill1Name, String skill2Name, String skill3Name,
                        String skill1Description, String skill2Description, String skill3Description,
                        String skill1Type, String skill2Type, String skill3Type,
                        String skill1SpritePath, String skill1FollowUpSpritePath, String skill2SpritePath, String skill3SpritePath,
                        int skill1ForwardOffsetX, int skill2ForwardOffsetX, int skill3ForwardOffsetX,
                        double skill1HurtTriggerBufferSeconds, double skill2HurtTriggerBufferSeconds, double skill3HurtTriggerBufferSeconds,
                        AnimationDef idleAnimation, AnimationDef hurtAnimation, AnimationDef deadAnimation,
                        ProjectileDef skill1Projectile, ProjectileDef skill2Projectile, ProjectileDef skill3Projectile,
                        DefenseFormDef defenseForm,
                        int drawWidth, int drawHeight) {
        this(name, backstory,
                skill1Name, skill2Name, skill3Name,
                skill1Description, skill2Description, skill3Description,
                skill1Type, skill2Type, skill3Type,
                10, 10, 25,
                skill1SpritePath, skill1FollowUpSpritePath, skill2SpritePath, skill3SpritePath,
                skill1ForwardOffsetX, skill2ForwardOffsetX, skill3ForwardOffsetX,
                skill1HurtTriggerBufferSeconds, skill2HurtTriggerBufferSeconds, skill3HurtTriggerBufferSeconds,
                idleAnimation, hurtAnimation, deadAnimation,
                skill1Projectile, skill2Projectile, skill3Projectile,
                defenseForm,
                drawWidth, drawHeight,
                0, 0, 0, 0, 1.0, 1.0);
    }

    // Constructor for characters with custom Wind Wizard scaling (used by Wind Wizard and other special characters)
    public CharacterDef(String name,
                        String backstory,
                        String skill1Name, String skill2Name, String skill3Name,
                        String skill1Description, String skill2Description, String skill3Description,
                        String skill1Type, String skill2Type, String skill3Type,
                        String skill1SpritePath, String skill1FollowUpSpritePath, String skill2SpritePath, String skill3SpritePath,
                        int skill1ForwardOffsetX, int skill2ForwardOffsetX, int skill3ForwardOffsetX,
                        double skill1HurtTriggerBufferSeconds, double skill2HurtTriggerBufferSeconds, double skill3HurtTriggerBufferSeconds,
                        AnimationDef idleAnimation, AnimationDef hurtAnimation, AnimationDef deadAnimation,
                        int drawWidth, int drawHeight,
                        int skill2Player2OffsetX, int skill3OffsetX, int skill3OffsetY,
                        int shadowOffsetX,
                        double skill3Scale, double shadowScale) {
        this(name, backstory,
                skill1Name, skill2Name, skill3Name,
                skill1Description, skill2Description, skill3Description,
                skill1Type, skill2Type, skill3Type,
                10, 10, 25,  // Default skill powers
                skill1SpritePath, skill1FollowUpSpritePath, skill2SpritePath, skill3SpritePath,
                skill1ForwardOffsetX, skill2ForwardOffsetX, skill3ForwardOffsetX,
                skill1HurtTriggerBufferSeconds, skill2HurtTriggerBufferSeconds, skill3HurtTriggerBufferSeconds,
                idleAnimation, hurtAnimation, deadAnimation,
                null, null, null,  // No projectiles
                null,  // No defense form
                drawWidth, drawHeight,
                skill2Player2OffsetX, skill3OffsetX, skill3OffsetY, shadowOffsetX, skill3Scale, shadowScale);
    }

    public int getSkillPower(int skillID) {
        return switch (skillID) {
            case 1 -> skill1Power;
            case 2 -> skill2Power;
            case 3 -> skill3Power;
            default -> 0;
        };
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

    public String getSkillFollowUpSpritePath(int skillID) {
        return switch (skillID) {
            case 1 -> skill1FollowUpSpritePath;
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

    public String getSkillDescription(int skillID) {
        return switch (skillID) {
            case 1 -> skill1Description;
            case 2 -> skill2Description;
            case 3 -> skill3Description;
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

    public ProjectileDef getSkillProjectile(int skillID) {
        return switch (skillID) {
            case 1 -> skill1Projectile;
            case 2 -> skill2Projectile;
            case 3 -> skill3Projectile;
            default -> null;
        };
    }
}
