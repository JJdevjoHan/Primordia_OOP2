package engine;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.net.URL;

public class SoundManager implements Runnable {
    Clip clip;
    URL[] soundURL = new URL[20];
    private String action = "play";

    //Constructor para pag assign sa sound | Just follow the current array sa last
    public SoundManager() {
        //BGM for choosing character
        soundURL[0] = getClass().getResource("/assets/SoundManager/BGM/characterSelection_MagicalDefenseTraining.wav");
        //Player 1 Sound
        soundURL[1] = getClass().getResource("/assets/SoundManager/PVPsfx/player_1.wav");
        //Player 2 Sound
        soundURL[2] = getClass().getResource("/assets/SoundManager/PVPsfx/player_2.wav");
        //Arcade Mode Sound
        soundURL[3] = getClass().getResource("/assets/SoundManager/ARCADEsfx/arcade_mode.wav");
        //Survival Mode Sound
        soundURL[4] = getClass().getResource("/assets/SoundManager/SURVIVALsfx/survival_mode.wav");
        //CHOOSE YOUR CHARACTER Sound
        soundURL[5] = getClass().getResource("/assets/SoundManager/SURVIVALsfx/survival_mode.wav");
        //game Menu BGM
        soundURL[6] = getClass().getResource("/assets/SoundManager/BGM/gameMenu_fantasycraft.wav");
        //main Gameplay BGM
        soundURL[7] = getClass().getResource("/assets/SoundManager/BGM/mainGamePlay_DragonSmasher.wav");
        //Click Button
        soundURL[8] = getClass().getResource("/assets/SoundManager/MainMenuSFX/click_003.wav");
        //Survival Mode - Knife to The Throat
        soundURL[9] = getClass().getResource("/assets/SoundManager/BGM/arcadeMode_TheMagicWithin.wav");
        //Arcade Mode - The Magic Within
        soundURL[10] = getClass().getResource("/assets/SoundManager/BGM/survivalMode_KnifeToTheThroat.wav");


    }

    public void setFile(int i) {
        try {
            if (soundURL[i] == null) {
                System.err.println("Index " + i + " kay null icheck imo path name.");
                return;
            }
            AudioInputStream ais = AudioSystem.getAudioInputStream(soundURL[i]);
            clip = AudioSystem.getClip();
            clip.open(ais);
        } catch (Exception e) {
            System.err.println("Error loading file: " + e.getMessage());
        }
    }

    public void play() {
        if (clip != null) {
            this.action = "play";
            Thread t = new Thread(this);
            t.start();
        } else {
            System.err.println("Clip is null! Check index or file path.");
        }
    }

    public void loop() {
        if (clip != null) {
            this.action = "loop";
            Thread t = new Thread(this);
            t.start();
        }
    }

    public void stop() {
        if (clip != null) {
            clip.stop();
        }
    }

    @Override
    public void run() {

        if (clip != null) {
            clip.setFramePosition(0);
            if (action.equals("loop")) {
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                clip.start();
            }
        }
    }
}

