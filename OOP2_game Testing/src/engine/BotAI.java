package engine;

import java.util.Random;


public final class BotAI {

    public enum Difficulty { EASY, NORMAL, HARD }

    /** On EASY the bot picks randomly ~40 % of the time, ignoring strategy. */
    private static final double EASY_RANDOM_CHANCE   = 0.40;
    /** On NORMAL the bot occasionally (15 %) makes a sub-optimal choice. */
    private static final double NORMAL_RANDOM_CHANCE = 0.15;
    /** On HARD the bot always plays optimally. */
    private static final double HARD_RANDOM_CHANCE   = 0.00;

    private static final double DESPERATE_THRESHOLD  = 0.20;   // ≤ 20 % own HP
    private static final double AGGRESSIVE_THRESHOLD = 0.30;   // ≤ 30 % enemy HP

    private static final Random RNG = new Random();

    private BotAI() {}

    public static int chooseSkill(int botHP, int playerHP,
                                  CharacterDef botDef,
                                  Difficulty difficulty,
                                  int maxHP) {

        double noiseChance = switch (difficulty) {
            case EASY   -> EASY_RANDOM_CHANCE;
            case NORMAL -> NORMAL_RANDOM_CHANCE;
            case HARD   -> HARD_RANDOM_CHANCE;
        };
        if (RNG.nextDouble() < noiseChance) {
            return randomSkill();
        }

        double botRatio    = (double) botHP    / maxHP;
        double playerRatio = (double) playerHP / maxHP;

        // ── Tier 1: Desperate self-preservation ─────────────────────────────
        if (botRatio <= DESPERATE_THRESHOLD) {
            int healSkill = findSkillOfType(botDef, "heal", "defense");
            if (healSkill != -1) return healSkill;
            // No heal available — try to eliminate the threat instead
            int dmgSkill = findStrongestDamageSkill(botDef);
            if (dmgSkill != -1) return dmgSkill;
        }

        // ── Tier 2: Aggressive finishing blow ────────────────────────────────
        if (playerRatio <= AGGRESSIVE_THRESHOLD) {
            int dmgSkill = findStrongestDamageSkill(botDef);
            if (dmgSkill != -1) return dmgSkill;
        }

        // ── Tier 3: Tactical mid-game ────────────────────────────────────────
        // If bot is moderately hurt and has a heal, use it ~60 % of the time
        if (botRatio < 0.55 && RNG.nextDouble() < 0.60) {
            int healSkill = findSkillOfType(botDef, "heal", "defense");
            if (healSkill != -1) return healSkill;
        }

        // Otherwise prefer dealing damage / debuffing
        int dmgSkill = findStrongestDamageSkill(botDef);
        if (dmgSkill != -1) return dmgSkill;

        // ── Tier 4: Random fallback ──────────────────────────────────────────
        return randomSkill();
    }

    public static int chooseSkill(int botHP, int playerHP, CharacterDef botDef) {
        return chooseSkill(botHP, playerHP, botDef, Difficulty.NORMAL, 100);
    }

    private static int findSkillOfType(CharacterDef def, String... types) {
        if (def == null) return -1;
        for (int id = 1; id <= 3; id++) {
            String t = def.getSkillType(id);
            if (t == null) continue;
            for (String wanted : types) {
                if (wanted.equalsIgnoreCase(t)) return id;
            }
        }
        return -1;
    }

    private static int findStrongestDamageSkill(CharacterDef def) {
        if (def == null) return -1;

        int[] preferenceOrder = {3, 1, 2};
        for (int id : preferenceOrder) {
            String t = def.getSkillType(id);
            if (t != null && ("damage".equalsIgnoreCase(t) || "debuff".equalsIgnoreCase(t))) {
                return id;
            }
        }
        return -1;
    }

    private static int randomSkill() {
        return RNG.nextInt(3) + 1;  // 1, 2, or 3
    }
}