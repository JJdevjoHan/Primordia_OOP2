package engine.interfaces;

//interface
public interface SkillExecutor {

    //tig execute sa skill
    void executeSkill(int skillID);

    default boolean isInputBlocked() {
        return false;
    }
    //stop sa music
    void stopMusic();
}