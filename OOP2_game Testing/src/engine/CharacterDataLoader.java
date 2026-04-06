package engine;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
        public final String skill1SpritePath;
        public final String skill2SpritePath;
        public final String skill3SpritePath;
        public final int skill1ForwardOffsetX;
        public final int skill2ForwardOffsetX;
        public final int skill3ForwardOffsetX;
        public final double skill1HurtTriggerBufferSeconds;
        public final double skill2HurtTriggerBufferSeconds;
        public final double skill3HurtTriggerBufferSeconds;
        public final String idleSpritePath;
        public final String hurtSpritePath;
        public final String deathSpritePath;

        public CharacterConfig(
            String name,
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
            String skill1SpritePath,
            String skill2SpritePath,
            String skill3SpritePath,
            int skill1ForwardOffsetX,
            int skill2ForwardOffsetX,
            int skill3ForwardOffsetX,
            double skill1HurtTriggerBufferSeconds,
            double skill2HurtTriggerBufferSeconds,
            double skill3HurtTriggerBufferSeconds,
            String idleSpritePath,
            String hurtSpritePath,
            String deathSpritePath
        ) {
            this.name = name;
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
            this.skill1SpritePath = skill1SpritePath;
            this.skill2SpritePath = skill2SpritePath;
            this.skill3SpritePath = skill3SpritePath;
            this.skill1ForwardOffsetX = skill1ForwardOffsetX;
            this.skill2ForwardOffsetX = skill2ForwardOffsetX;
            this.skill3ForwardOffsetX = skill3ForwardOffsetX;
            this.skill1HurtTriggerBufferSeconds = skill1HurtTriggerBufferSeconds;
            this.skill2HurtTriggerBufferSeconds = skill2HurtTriggerBufferSeconds;
            this.skill3HurtTriggerBufferSeconds = skill3HurtTriggerBufferSeconds;
            this.idleSpritePath = idleSpritePath;
            this.hurtSpritePath = hurtSpritePath;
            this.deathSpritePath = deathSpritePath;
        }
    }

    public static List<CharacterConfig> loadCharacterConfigs(String resourcePath) {
        try (InputStream stream = CharacterDataLoader.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                System.err.println("Character JSON not found: " + resourcePath);
                return List.of();
            }

            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
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

                String skill1SpritePath = normalizeResourcePath(toStringValue(sprites.get("skill1")));
                String skill2SpritePath = normalizeResourcePath(toStringValue(sprites.get("skill2")));
                String skill3SpritePath = normalizeResourcePath(toStringValue(sprites.get("skill3")));
                int skill1ForwardOffsetX = getSkillForwardOffsetX(skillOffsets, "skill1");
                int skill2ForwardOffsetX = getSkillForwardOffsetX(skillOffsets, "skill2");
                int skill3ForwardOffsetX = getSkillForwardOffsetX(skillOffsets, "skill3");
                double skill1HurtTriggerBufferSeconds = getSkillHurtTriggerBufferSeconds(skills, 0);
                double skill2HurtTriggerBufferSeconds = getSkillHurtTriggerBufferSeconds(skills, 1);
                double skill3HurtTriggerBufferSeconds = getSkillHurtTriggerBufferSeconds(skills, 2);
                String idleSpritePath = normalizeResourcePath(toStringValue(sprites.get("idle")));
                String hurtSpritePath = normalizeResourcePath(toStringValue(sprites.get("hurt")));
                String deathSpritePath = normalizeResourcePath(toStringValue(sprites.get("death")));

                if (name == null || name.isBlank() || idleSpritePath == null || deathSpritePath == null) {
                    continue;
                }

                result.add(new CharacterConfig(
                    name,
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
                    skill1SpritePath,
                    skill2SpritePath,
                    skill3SpritePath,
                    skill1ForwardOffsetX,
                    skill2ForwardOffsetX,
                    skill3ForwardOffsetX,
                    skill1HurtTriggerBufferSeconds,
                    skill2HurtTriggerBufferSeconds,
                    skill3HurtTriggerBufferSeconds,
                    idleSpritePath,
                    hurtSpritePath,
                    deathSpritePath
                ));
            }

            return result;
        } catch (Exception e) {
            System.err.println("Failed to load character JSON: " + e.getMessage());
            return List.of();
        }
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

    private static List<?> asList(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }

    private static Map<?, ?> asMap(Object value) {
        return value instanceof Map<?, ?> map ? map : Map.of();
    }

    private static String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
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
