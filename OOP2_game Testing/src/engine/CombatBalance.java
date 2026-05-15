package engine;

public final class CombatBalance {
    private static final double ELEMENT_ADVANTAGE_MULTIPLIER = 1.24;
    private static final double SETUP_MULTIPLIER = 1.08;
    private static final int MIN_DAMAGE = 16;
    private static final int MAX_DAMAGE = 52;
    private static final int MIN_UTILITY = 8;
    private static final int MAX_UTILITY = 36;

    private CombatBalance() {}

    public static int calculateDamage(CharacterDef attacker,
                                      CharacterDef defender,
                                      int skillID,
                                      double timingRatio,
                                      boolean setupActive) {
        if (attacker == null) return 0;

        int rawPower = Math.max(0, attacker.getSkillPower(skillID));
        if (rawPower <= 0) return 0;
        double normalizedBase = 16.0 + rawPower * 0.48 + skillID * 1.75;
        double executionMultiplier = 0.92 + clamp01(timingRatio) * 0.18;
        double setupMultiplier = setupActive ? SETUP_MULTIPLIER : 1.0;
        double elementalMultiplier = hasElementAdvantage(attacker, defender) ? ELEMENT_ADVANTAGE_MULTIPLIER : 1.0;

        int damage = (int) Math.round(normalizedBase * executionMultiplier * setupMultiplier * elementalMultiplier);
        return clamp(damage, MIN_DAMAGE, MAX_DAMAGE);
    }

    public static int calculateHealing(CharacterDef actor, int skillID, double timingRatio, boolean urgent) {
        if (actor == null) return 10;

        int rawHeal = actor.getSkillHealValue(skillID);
        if (rawHeal <= 0) rawHeal = 10;

        double normalizedBase = 8.0 + rawHeal * 0.56;
        double executionMultiplier = 0.96 + clamp01(timingRatio) * 0.10;
        double urgencyMultiplier = urgent ? 1.08 : 1.0;

        return clamp((int) Math.round(normalizedBase * executionMultiplier * urgencyMultiplier), MIN_UTILITY, MAX_UTILITY);
    }

    public static int calculateShieldHealing(CharacterDef actor, int skillID, double timingRatio, boolean urgent) {
        if (actor == null) return 10;

        int rawShield = Math.max(0, actor.getSkillShieldValue(skillID));
        double normalizedBase = 9.0 + rawShield * 0.62;
        double executionMultiplier = 0.96 + clamp01(timingRatio) * 0.10;
        double urgencyMultiplier = urgent ? 1.06 : 1.0;

        return clamp((int) Math.round(normalizedBase * executionMultiplier * urgencyMultiplier), MIN_UTILITY, MAX_UTILITY);
    }

    public static int calculateSelfHeal(CharacterDef actor, int skillID, double timingRatio, int damageDealt) {
        if (actor == null) return 0;

        int rawSelfHeal = Math.max(0, actor.getSkillSelfHeal(skillID));
        if (rawSelfHeal <= 0) return 0;

        double executionMultiplier = 0.9 + clamp01(timingRatio) * 0.12;
        int healing = (int) Math.round(Math.min(rawSelfHeal, Math.max(6, damageDealt * 0.45)) * executionMultiplier);
        return clamp(healing, 0, 24);
    }

    public static int calculatePoisonDamage(CharacterDef actor, int skillID) {
        if (actor == null) return 0;
        int rawPoison = Math.max(0, actor.getSkillPoisonDamage(skillID));
        if (rawPoison <= 0) return 0;
        return clamp((int) Math.round(4.0 + rawPoison * 0.52), 5, 10);
    }

    public static int calculatePoisonDuration(CharacterDef actor, int skillID) {
        if (actor == null) return 0;
        int duration = actor.getSkillDurationTurns(skillID);
        return clamp(duration, 0, 3);
    }

    public static boolean hasElementAdvantage(CharacterDef attacker, CharacterDef defender) {
        if (attacker == null || defender == null) return false;
        return defender.isWeakTo(attacker.archetype);
    }

    private static double clamp01(double value) {
        if (Double.isNaN(value)) return 0.0;
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
