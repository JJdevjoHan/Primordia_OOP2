import re

file_path = r"c:\Users\User\Desktop\Primordia\Primordia_OOP2\OOP2_game Testing\src\engine\ArcadeGamePanel.java"

with open(file_path, 'r') as f:
    content = f.read()

# Find the location to insert the method
pattern = r'(    private boolean shouldHideCasterDuringOverlaySkill3\(boolean isPlayerOne\) \{[\s\S]*?return isWindWizardAttack3\(isPlayerOne\);[\s\S]*?\})\n\n(    private void drawAnchoredSkillFrame)'

replacement = r'''\1

    private boolean isWindWizardSkill2(boolean isPlayerOne) {
        CharacterDef actor = isPlayerOne ? currentPlayerDef : currentEnemyDef;
        int activeSkillID = isPlayerOne ? activePlayerSkillID : activeEnemySkillID;
        return actor != null && "Wind Wizard".equalsIgnoreCase(actor.name) && activeSkillID == 2;
    }

\2'''

new_content = re.sub(pattern, replacement, content, flags=re.MULTILINE)

with open(file_path, 'w') as f:
    f.write(new_content)

print("Fixed ArcadeGamePanel.java - added isWindWizardSkill2() method")
