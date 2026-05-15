package engine;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads character battle metadata from characters.json without external dependencies.
 */
public final class CharacterDataLoader {

    private CharacterDataLoader() {}

    public static class CharacterConfig {
        public final String name;
        public final String archetype;
        public final String[] weaknesses;
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
        public final int skill2Player2OffsetX;
        public final int skill3OffsetX;
        public final int skill3OffsetY;
        public final int shadowOffsetX;
        public final double skill3Scale;
        public final double shadowScale;
        public final int skill1DurationTurns;
        public final int skill2DurationTurns;
        public final int skill3DurationTurns;
        public final int skill1PoisonDamage;
        public final int skill2PoisonDamage;
        public final int skill3PoisonDamage;
        public final int skill1ShieldValue;
        public final int skill2ShieldValue;
        public final int skill3ShieldValue;
        public final int skill1HealValue;
        public final int skill2HealValue;
        public final int skill3HealValue;
        public final int skill1SelfHeal;
        public final int skill2SelfHeal;
        public final int skill3SelfHeal;
        public final int drawWidth;
        public final int drawHeight;
        public final String idleSpritePath;
        public final String hurtSpritePath;
        public final String deathSpritePath;
        public final CharacterDef.ProjectileDef skill1Projectile;
        public final CharacterDef.ProjectileDef skill2Projectile;
        public final CharacterDef.ProjectileDef skill3Projectile;
        public final CharacterDef.DefenseFormDef defenseForm;

        public CharacterConfig(
            String name,
            String archetype,
            String[] weaknesses,
            String backstory,
            String skill1Name,
            String skill2Name,
            String skill3Name,
            String skill1Description,
            String skill2Description,
            String skill3Description,
            String skill1Type,
            String skill2Type,
            String skill3Type,
            int skill1Power,
            int skill2Power,
            int skill3Power,
            String skill1SpritePath,
            String skill1FollowUpSpritePath,
            String skill2SpritePath,
            String skill3SpritePath,
            int skill1ForwardOffsetX,
            int skill2ForwardOffsetX,
            int skill3ForwardOffsetX,
            double skill1HurtTriggerBufferSeconds,
            double skill2HurtTriggerBufferSeconds,
            double skill3HurtTriggerBufferSeconds,
            int skill2Player2OffsetX,
            int skill3OffsetX,
            int skill3OffsetY,
            int shadowOffsetX,
            double skill3Scale,
            double shadowScale,
            int skill1DurationTurns,
            int skill2DurationTurns,
            int skill3DurationTurns,
            int skill1PoisonDamage,
            int skill2PoisonDamage,
            int skill3PoisonDamage,
            int skill1ShieldValue,
            int skill2ShieldValue,
            int skill3ShieldValue,
            int skill1HealValue,
            int skill2HealValue,
            int skill3HealValue,
            int skill1SelfHeal,
            int skill2SelfHeal,
            int skill3SelfHeal,
            int drawWidth,
            int drawHeight,
            String idleSpritePath,
            String hurtSpritePath,
            String deathSpritePath,
            CharacterDef.ProjectileDef skill1Projectile,
            CharacterDef.ProjectileDef skill2Projectile,
            CharacterDef.ProjectileDef skill3Projectile,
            CharacterDef.DefenseFormDef defenseForm
        ) {
            this.name = name;
            this.archetype = archetype;
            this.weaknesses = weaknesses == null ? new String[0] : weaknesses;
            this.backstory = backstory;
            this.skill1Name = skill1Name;
            this.skill2Name = skill2Name;
            this.skill3Name = skill3Name;
            this.skill1Description = skill1Description;
            this.skill2Description = skill2Description;
            this.skill3Description = skill3Description;
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
            this.skill2Player2OffsetX = skill2Player2OffsetX;
            this.skill3OffsetX = skill3OffsetX;
            this.skill3OffsetY = skill3OffsetY;
            this.shadowOffsetX = shadowOffsetX;
            this.skill3Scale = skill3Scale;
            this.shadowScale = shadowScale;
            this.skill1DurationTurns = skill1DurationTurns;
            this.skill2DurationTurns = skill2DurationTurns;
            this.skill3DurationTurns = skill3DurationTurns;
            this.skill1PoisonDamage = skill1PoisonDamage;
            this.skill2PoisonDamage = skill2PoisonDamage;
            this.skill3PoisonDamage = skill3PoisonDamage;
            this.skill1ShieldValue = skill1ShieldValue;
            this.skill2ShieldValue = skill2ShieldValue;
            this.skill3ShieldValue = skill3ShieldValue;
            this.skill1HealValue = skill1HealValue;
            this.skill2HealValue = skill2HealValue;
            this.skill3HealValue = skill3HealValue;
            this.skill1SelfHeal = skill1SelfHeal;
            this.skill2SelfHeal = skill2SelfHeal;
            this.skill3SelfHeal = skill3SelfHeal;
            this.drawWidth = drawWidth;
            this.drawHeight = drawHeight;
            this.idleSpritePath = idleSpritePath;
            this.hurtSpritePath = hurtSpritePath;
            this.deathSpritePath = deathSpritePath;
            this.skill1Projectile = skill1Projectile;
            this.skill2Projectile = skill2Projectile;
            this.skill3Projectile = skill3Projectile;
            this.defenseForm = defenseForm;
        }
    }

    public static List<CharacterConfig> loadCharacterConfigs(String resourcePath) {
        try (InputStream stream = CharacterDataLoader.class.getResourceAsStream(resourcePath)) {
            String json = readJsonFromFileSystem();
            if (json == null) {
                if (stream == null) {
                    System.err.println("Character JSON not found: " + resourcePath);
                    return List.of();
                }
                json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
            Object rootValue = new JsonParser(json).parseValue();
            if (!(rootValue instanceof Map<?, ?> rootMap)) {
                return List.of();
            }

            Object charactersValue = rootMap.get("characters");
            if (!(charactersValue instanceof List<?> characterList)) {
                return List.of();
            }

            List<CharacterConfig> result = new ArrayList<>();
            for (Object entry : characterList) {
                if (!(entry instanceof Map<?, ?> characterMap)) continue;

                String name = toStringValue(characterMap.get("name"));
                String archetype = toStringValue(characterMap.get("elementType"));
                String[] weaknesses = getStringArray(characterMap.get("weakness"));
                String backstory = toStringValue(characterMap.get("backstory"));
                List<?> skills = asList(characterMap.get("skills"));
                Map<?, ?> sprites = asMap(characterMap.get("sprites"));
                Map<?, ?> skillOffsets = asMap(characterMap.get("skillOffsets"));

                String skill1Name = getSkillName(skills, 0, "Skill 1");
                String skill2Name = getSkillName(skills, 1, "Skill 2");
                String skill3Name = getSkillName(skills, 2, "Skill 3");
                String skill1Description = getSkillDescription(skills, 0, "No description available.");
                String skill2Description = getSkillDescription(skills, 1, "No description available.");
                String skill3Description = getSkillDescription(skills, 2, "No description available.");
                String skill1Type = getSkillType(skills, 0, "damage");
                String skill2Type = getSkillType(skills, 1, "defense");
                String skill3Type = getSkillType(skills, 2, "damage");
                int skill1Power = getSkillPower(skills, 0, 10);
                int skill2Power = getSkillPower(skills, 1, 10);
                int skill3Power = getSkillPower(skills, 2, 25);
                CharacterDef.ProjectileDef skill1Projectile = getSkillProjectile(skills, 0);
                CharacterDef.ProjectileDef skill2Projectile = getSkillProjectile(skills, 1);
                CharacterDef.ProjectileDef skill3Projectile = getSkillProjectile(skills, 2);
                CharacterDef.DefenseFormDef defenseForm = getDefenseForm(characterMap);

                String skill1SpritePath = normalizeResourcePath(toStringValue(sprites.get("skill1")));
                String skill1FollowUpSpritePath = normalizeResourcePath(toStringValue(sprites.get("skill1FollowUp")));
                String skill2SpritePath = normalizeResourcePath(toStringValue(sprites.get("skill2")));
                String skill3SpritePath = normalizeResourcePath(toStringValue(sprites.get("skill3")));
                int skill1ForwardOffsetX = getSkillForwardOffsetX(skillOffsets, "skill1");
                int skill2ForwardOffsetX = getSkillForwardOffsetX(skillOffsets, "skill2");
                int skill3ForwardOffsetX = getSkillForwardOffsetX(skillOffsets, "skill3");
                double skill1HurtTriggerBufferSeconds = getSkillHurtTriggerBufferSeconds(skills, 0);
                double skill2HurtTriggerBufferSeconds = getSkillHurtTriggerBufferSeconds(skills, 1);
                double skill3HurtTriggerBufferSeconds = getSkillHurtTriggerBufferSeconds(skills, 2);
                int skill2Player2OffsetX = getOptionalInt(characterMap, "skill2Player2OffsetX");
                int skill3OffsetX = getOptionalInt(characterMap, "skill3OffsetX");
                int skill3OffsetY = getOptionalInt(characterMap, "skill3OffsetY");
                int shadowOffsetX = getOptionalInt(characterMap, "shadowOffsetX");
                double skill3Scale = getOptionalDouble(characterMap, "skill3Scale", 1.0);
                double shadowScale = getOptionalDouble(characterMap, "shadowScale", 1.0);
                int drawWidth = getOptionalInt(characterMap, "drawWidth");
                int drawHeight = getOptionalInt(characterMap, "drawHeight");
                String idleSpritePath = normalizeResourcePath(toStringValue(sprites.get("idle")));
                String hurtSpritePath = normalizeResourcePath(toStringValue(sprites.get("hurt")));
                 String deathSpritePath = normalizeResourcePath(toStringValue(sprites.get("death")));

                 // Extract durationTurns and poisonDamage from skills (new fields)
                 int skill1DurationTurns = getSkillDurationTurns(skills, 0);
                 int skill2DurationTurns = getSkillDurationTurns(skills, 1);
                 int skill3DurationTurns = getSkillDurationTurns(skills, 2);
                 int skill1PoisonDamage = getSkillPoisonDamage(skills, 0);
                 int skill2PoisonDamage = getSkillPoisonDamage(skills, 1);
                 int skill3PoisonDamage = getSkillPoisonDamage(skills, 2);
                 int skill1ShieldValue = getSkillShieldValue(skills, 0);
                 int skill2ShieldValue = getSkillShieldValue(skills, 1);
                 int skill3ShieldValue = getSkillShieldValue(skills, 2);
                 int skill1HealValue = getSkillHealValue(skills, 0);
                 int skill2HealValue = getSkillHealValue(skills, 1);
                 int skill3HealValue = getSkillHealValue(skills, 2);
                 int skill1SelfHeal = getSkillSelfHeal(skills, 0);
                 int skill2SelfHeal = getSkillSelfHeal(skills, 1);
                 int skill3SelfHeal = getSkillSelfHeal(skills, 2);

                 if (name == null || name.isBlank() || idleSpritePath == null || deathSpritePath == null) {
                    continue;
                }

                result.add(new CharacterConfig(
                    name,
                    archetype,
                    weaknesses,
                    backstory,
                    skill1Name,
                    skill2Name,
                    skill3Name,
                    skill1Description,
                    skill2Description,
                    skill3Description,
                    skill1Type,
                    skill2Type,
                    skill3Type,
                    skill1Power,
                    skill2Power,
                    skill3Power,
                    skill1SpritePath,
                    skill1FollowUpSpritePath,
                    skill2SpritePath,
                    skill3SpritePath,
                    skill1ForwardOffsetX,
                    skill2ForwardOffsetX,
                    skill3ForwardOffsetX,
                    skill1HurtTriggerBufferSeconds,
                    skill2HurtTriggerBufferSeconds,
                    skill3HurtTriggerBufferSeconds,
                    skill2Player2OffsetX,
                    skill3OffsetX,
                    skill3OffsetY,
                     shadowOffsetX,
                     skill3Scale,
                     shadowScale,
                     skill1DurationTurns,
                     skill2DurationTurns,
                     skill3DurationTurns,
                     skill1PoisonDamage,
                     skill2PoisonDamage,
                     skill3PoisonDamage,
                     skill1ShieldValue,
                     skill2ShieldValue,
                     skill3ShieldValue,
                     skill1HealValue,
                     skill2HealValue,
                     skill3HealValue,
                     skill1SelfHeal,
                     skill2SelfHeal,
                     skill3SelfHeal,
                     drawWidth,
                    drawHeight,
                    idleSpritePath,
                    hurtSpritePath,
                    deathSpritePath,
                    skill1Projectile,
                    skill2Projectile,
                    skill3Projectile,
                    defenseForm
                ));
            }

            return result;
        } catch (Exception e) {
            System.err.println("Failed to load character JSON: " + e.getMessage());
            return List.of();
        }
    }

    private static String readJsonFromFileSystem() {
        String[] candidates = {
                "OOP2_game Testing/src/assets/data/characters.json",
                "src/assets/data/characters.json",
                "assets/data/characters.json"
        };
        for (String candidate : candidates) {
            Path path = Paths.get(candidate);
            if (Files.exists(path)) {
                try {
                    return Files.readString(path, StandardCharsets.UTF_8);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private static String getSkillName(List<?> skills, int index, String fallback) {
        if (skills.size() <= index || !(skills.get(index) instanceof Map<?, ?> skillMap)) return fallback;
        String value = toStringValue(skillMap.get("name"));
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private static String getSkillType(List<?> skills, int index, String fallback) {
        if (skills.size() <= index || !(skills.get(index) instanceof Map<?, ?> skillMap)) return fallback;
        String value = toStringValue(skillMap.get("type"));
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private static String getSkillDescription(List<?> skills, int index, String fallback) {
        if (skills.size() <= index || !(skills.get(index) instanceof Map<?, ?> skillMap)) return fallback;
        String value = toStringValue(skillMap.get("description"));
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private static CharacterDef.ProjectileDef getSkillProjectile(List<?> skills, int index) {
        if (skills.size() <= index || !(skills.get(index) instanceof Map<?, ?> skillMap)) return null;
        Map<?, ?> projectileMap = asMap(skillMap.get("projectile"));
        if (projectileMap.isEmpty()) return null;

        return parseProjectileDef(projectileMap);
    }

    private static CharacterDef.ProjectileDef parseProjectileDef(Map<?, ?> projectileMap) {
        if (projectileMap.isEmpty()) return null;

        String sheetPath = normalizeResourcePath(toStringValue(projectileMap.get("sprite")));
        if (sheetPath == null || sheetPath.isBlank()) return null;

        int frameWidth = getMapInt(projectileMap, "frameWidth", 64);
        int frameHeight = getMapInt(projectileMap, "frameHeight", 64);
        int drawWidth = getMapInt(projectileMap, "drawWidth", frameWidth);
        int drawHeight = getMapInt(projectileMap, "drawHeight", frameHeight);
        int speed = getMapInt(projectileMap, "speed", 44);
        int verticalOffset = getMapInt(projectileMap, "verticalOffset", 0);
        int spawnOffsetX = getMapInt(projectileMap, "spawnOffsetX", 0);
        boolean beam = getMapBoolean(projectileMap, "beam", false);
        boolean startDuringCast = getMapBoolean(projectileMap, "startDuringCast", false);
        boolean anchorOnTargetCenter = getMapBoolean(projectileMap, "anchorOnTargetCenter", false);
        boolean anchorOnTarget = getMapBoolean(projectileMap, "anchorOnTarget", false);
        int loopStartFrame = getMapInt(projectileMap, "loopStartFrame", 0);
        int loopEndFrame = getMapInt(projectileMap, "loopEndFrame", 0);
        int impactStartFrame = getMapInt(projectileMap, "impactStartFrame", 0);
        int impactEndFrame = getMapInt(projectileMap, "impactEndFrame", 0);
        String impactSheetPath = normalizeResourcePath(toStringValue(projectileMap.get("impactSprite")));
        int impactFrameWidth = getMapInt(projectileMap, "impactFrameWidth", 0);
        int impactFrameHeight = getMapInt(projectileMap, "impactFrameHeight", 0);
        int impactDrawWidth = getMapInt(projectileMap, "impactDrawWidth", 0);
        int impactDrawHeight = getMapInt(projectileMap, "impactDrawHeight", 0);
        boolean anchorImpactOnTargetCenter = getMapBoolean(projectileMap, "anchorImpactOnTargetCenter", false);
        int animationFrameDelay = getMapInt(projectileMap, "animationFrameDelay", 0);

        return new CharacterDef.ProjectileDef(
                sheetPath,
                Math.max(1, frameWidth),
                Math.max(1, frameHeight),
                Math.max(1, drawWidth),
                Math.max(1, drawHeight),
                Math.max(1, speed),
            verticalOffset,
            spawnOffsetX,
            beam,
            startDuringCast,
            anchorOnTargetCenter,
            anchorOnTarget,
            Math.max(0, loopStartFrame),
            Math.max(0, loopEndFrame),
            Math.max(0, impactStartFrame),
                Math.max(0, impactEndFrame),
                impactSheetPath,
                Math.max(0, impactFrameWidth),
                Math.max(0, impactFrameHeight),
                Math.max(0, impactDrawWidth),
                Math.max(0, impactDrawHeight),
                anchorImpactOnTargetCenter,
                Math.max(0, animationFrameDelay));
    }

            private static CharacterDef.DefenseFormDef getDefenseForm(Map<?, ?> characterMap) {
            Map<?, ?> defenseFormMap = asMap(characterMap.get("defenseForm"));
            if (defenseFormMap.isEmpty()) return null;

            int toggleSkillSlot = Math.max(1, getMapInt(defenseFormMap, "toggleSkillSlot", 3));
            int altSkillSlot = Math.max(1, getMapInt(defenseFormMap, "altSkillSlot", 1));
            int enterFreezeFrame = Math.max(1, getMapInt(defenseFormMap, "enterFreezeFrame", 5));
            int exitStartFrame = Math.max(1, getMapInt(defenseFormMap, "exitStartFrame", 6));

            String altSkillSprite = normalizeResourcePath(toStringValue(defenseFormMap.get("altSkillSprite")));
            int altSkillFrameWidth = Math.max(1, getMapInt(defenseFormMap, "altSkillFrameWidth", 128));
            int altSkillFrameHeight = Math.max(1, getMapInt(defenseFormMap, "altSkillFrameHeight", 128));
            CharacterDef.AnimationDef altSkillAnimation = null;
            if (altSkillSprite != null && !altSkillSprite.isBlank()) {
                altSkillAnimation = new CharacterDef.AnimationDef(
                    altSkillSprite,
                    altSkillFrameWidth,
                    altSkillFrameHeight,
                    90);
            }

            CharacterDef.ProjectileDef altSkillProjectile = parseProjectileDef(asMap(defenseFormMap.get("altSkillProjectile")));
            if (altSkillAnimation == null && altSkillProjectile == null) return null;

            return new CharacterDef.DefenseFormDef(
                toggleSkillSlot,
                altSkillSlot,
                enterFreezeFrame,
                exitStartFrame,
                altSkillAnimation,
                altSkillProjectile);
            }

    private static List<?> asList(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }

    private static String[] getStringArray(Object value) {
        List<?> list = asList(value);
        List<String> strings = new ArrayList<>();
        for (Object item : list) {
            String text = toStringValue(item);
            if (text != null && !text.isBlank()) strings.add(text);
        }
        return strings.toArray(new String[0]);
    }

    private static Map<?, ?> asMap(Object value) {
        return value instanceof Map<?, ?> map ? map : Map.of();
    }

    private static String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static int getOptionalInt(Map<?, ?> map, String key) {
        Object raw = map.get(key);
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw != null) {
            try {
                return Integer.parseInt(String.valueOf(raw));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static double getOptionalDouble(Map<?, ?> map, String key, double fallback) {
        Object raw = map.get(key);
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        if (raw != null) {
            try {
                return Double.parseDouble(String.valueOf(raw));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static int getMapInt(Map<?, ?> map, String key, int fallback) {
        Object raw = map.get(key);
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw != null) {
            try {
                return Integer.parseInt(String.valueOf(raw));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static boolean getMapBoolean(Map<?, ?> map, String key, boolean fallback) {
        Object raw = map.get(key);
        if (raw instanceof Boolean b) {
            return b;
        }
        if (raw != null) {
            String text = String.valueOf(raw).trim();
            if ("true".equalsIgnoreCase(text)) return true;
            if ("false".equalsIgnoreCase(text)) return false;
        }
        return fallback;
    }

    private static int getSkillForwardOffsetX(Map<?, ?> skillOffsets, String skillKey) {
        Map<?, ?> skillOffsetEntry = asMap(skillOffsets.get(skillKey));
        Object raw = skillOffsetEntry.get("forwardX");
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw != null) {
            try {
                return Integer.parseInt(String.valueOf(raw));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static double getSkillHurtTriggerBufferSeconds(List<?> skills, int index) {
        if (skills.size() <= index || !(skills.get(index) instanceof Map<?, ?> skillMap)) return 0.0;
        Object raw = skillMap.get("hurtTriggerBufferSeconds");
        if (raw instanceof Number number) {
            return Math.max(0.0, number.doubleValue());
        }
        if (raw != null) {
            try {
                return Math.max(0.0, Double.parseDouble(String.valueOf(raw)));
            } catch (NumberFormatException ignored) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private static int getSkillPower(List<?> skills, int index, int fallback) {
        if (skills.size() <= index || !(skills.get(index) instanceof Map<?, ?> skillMap)) return fallback;
        Object raw = skillMap.get("power");
        if (raw instanceof Number number) {
            return Math.max(0, number.intValue());
        }
        if (raw != null) {
            try {
                return Math.max(0, Integer.parseInt(String.valueOf(raw)));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return 0;
    }

    private static int getSkillDurationTurns(List<?> skills, int index) {
        if (skills.size() <= index || !(skills.get(index) instanceof Map<?, ?> skillMap)) return 0;
        Object raw = skillMap.get("durationTurns");
        if (raw == null) raw = skillMap.get("poisonDuration");
        if (raw instanceof Number number) return Math.max(0, number.intValue());
        if (raw != null) {
            try { return Math.max(0, Integer.parseInt(String.valueOf(raw))); } catch (Exception e) { return 0; }
        }
        return 0;
    }

    private static int getSkillPoisonDamage(List<?> skills, int index) {
        if (skills.size() <= index || !(skills.get(index) instanceof Map<?, ?> skillMap)) return 0;
        Object raw = skillMap.get("poisonDamage");
        if (raw instanceof Number number) return Math.max(0, number.intValue());
        if (raw != null) {
            try { return Math.max(0, Integer.parseInt(String.valueOf(raw))); } catch (Exception e) { return 0; }
        }
        return 0;
    }

    private static int getSkillShieldValue(List<?> skills, int index) {
        if (skills.size() <= index || !(skills.get(index) instanceof Map<?, ?> skillMap)) return 0;
        Object raw = skillMap.get("shieldValue");
        if (raw instanceof Number number) return Math.max(0, number.intValue());
        if (raw != null) {
            try { return Math.max(0, Integer.parseInt(String.valueOf(raw))); } catch (Exception e) { return 0; }
        }
        return 0;
    }

    private static int getSkillHealValue(List<?> skills, int index) {
        if (skills.size() <= index || !(skills.get(index) instanceof Map<?, ?> skillMap)) return 0;
        Object raw = skillMap.get("healValue");
        if (raw instanceof Number number) return Math.max(0, number.intValue());
        if (raw != null) {
            try { return Math.max(0, Integer.parseInt(String.valueOf(raw))); } catch (Exception e) { return 0; }
        }
        return 0;
    }

    private static int getSkillSelfHeal(List<?> skills, int index) {
        if (skills.size() <= index || !(skills.get(index) instanceof Map<?, ?> skillMap)) return 0;
        Object raw = skillMap.get("selfHeal");
        if (raw instanceof Number number) return Math.max(0, number.intValue());
        if (raw != null) {
            try { return Math.max(0, Integer.parseInt(String.valueOf(raw))); } catch (Exception e) { return 0; }
        }
        return 0;
    }

    private static String normalizeResourcePath(String path) {
        if (path == null || path.isBlank()) return null;
        String normalized = path.replace('\\', '/');
        if (normalized.startsWith("src/")) {
            normalized = normalized.substring(3);
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }

    private static final class JsonParser {
        private final String input;
        private int index;

        JsonParser(String input) {
            this.input = input;
        }

        Object parseValue() {
            skipWhitespace();
            if (index >= input.length()) {
                throw new IllegalStateException("Unexpected end of JSON");
            }

            char c = input.charAt(index);
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> result = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                expect('}');
                return result;
            }

            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                result.put(key, value);
                skipWhitespace();
                if (peek('}')) {
                    expect('}');
                    break;
                }
                expect(',');
            }
            return result;
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> result = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                expect(']');
                return result;
            }

            while (true) {
                result.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    expect(']');
                    break;
                }
                expect(',');
            }
            return result;
        }

        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (index < input.length()) {
                char c = input.charAt(index++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (index >= input.length()) {
                        throw new IllegalStateException("Invalid escape sequence");
                    }
                    char esc = input.charAt(index++);
                    switch (esc) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (index + 4 > input.length()) {
                                throw new IllegalStateException("Invalid unicode escape");
                            }
                            String hex = input.substring(index, index + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            index += 4;
                        }
                        default -> throw new IllegalStateException("Unknown escape: " + esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new IllegalStateException("Unterminated string");
        }

        private Object parseNumber() {
            int start = index;
            if (peek('-')) index++;
            while (index < input.length() && Character.isDigit(input.charAt(index))) index++;
            if (peek('.')) {
                index++;
                while (index < input.length() && Character.isDigit(input.charAt(index))) index++;
            }
            if (peek('e') || peek('E')) {
                index++;
                if (peek('+') || peek('-')) index++;
                while (index < input.length() && Character.isDigit(input.charAt(index))) index++;
            }

            String token = input.substring(start, index);
            if (token.isBlank()) {
                throw new IllegalStateException("Invalid number token");
            }

            if (token.contains(".") || token.contains("e") || token.contains("E")) {
                return Double.parseDouble(token);
            }
            try {
                return Integer.parseInt(token);
            } catch (NumberFormatException ignored) {
                return Long.parseLong(token);
            }
        }

        private Object parseLiteral(String literal, Object value) {
            if (input.startsWith(literal, index)) {
                index += literal.length();
                return value;
            }
            throw new IllegalStateException("Expected literal: " + literal);
        }

        private void expect(char expected) {
            skipWhitespace();
            if (index >= input.length() || input.charAt(index) != expected) {
                throw new IllegalStateException("Expected '" + expected + "' at index " + index);
            }
            index++;
        }

        private boolean peek(char c) {
            return index < input.length() && input.charAt(index) == c;
        }

        private void skipWhitespace() {
            while (index < input.length()) {
                char c = input.charAt(index);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                    index++;
                } else {
                    break;
                }
            }
        }
    }
}
