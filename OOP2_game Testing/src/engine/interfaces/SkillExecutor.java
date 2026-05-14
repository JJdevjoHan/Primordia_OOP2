package engine.interfaces;

/**
 * OOP Principle: Interface Segregation + Dependency Inversion
 *
 * Any game panel that handles player skill input implements this.
 * KeyInputs depends only on this interface — never on a concrete panel class.
 * Adding a new game mode means implementing this interface, not editing KeyInputs.
 */
public interface SkillExecutor {

    /** Execute the skill in slot 1, 2, or 3. */
    void executeSkill(int skillID);

    /**
     * Returns true while an overlay/popup is blocking input.
     * Panels with no such concept use the default (always false).
     */
    default boolean isInputBlocked() {
        return false;
    }

    /** Stop background music (called when navigating away). */
    void stopMusic();
}