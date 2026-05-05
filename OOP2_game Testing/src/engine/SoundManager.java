package engine;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.net.URL;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SoundManager implements Runnable {
    Clip clip;
    URL[] soundURL = new URL[20];
    private String action = "play";
    private int pendingIndex = -1;
    private final Map<Integer, byte[]> audioDataCache = new ConcurrentHashMap<>();

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
        // Defer heavy audio loading to the play/loop thread to avoid blocking caller
        pendingIndex = i;
    }

    public void preload(int i) {
        if (i < 0 || i >= soundURL.length || soundURL[i] == null) return;
        // Load into memory on a daemon thread so it doesn't block the caller
        Thread t = new Thread(() -> {
            try (InputStream is = soundURL[i].openStream()) {
                byte[] data = is.readAllBytes();
                audioDataCache.put(i, data);
            } catch (Exception e) {
                System.err.println("Error preloading audio index " + i + ": " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void play() {
        this.action = "play";
        Thread t = new Thread(() -> {
            synchronized (this) {
                try {
                    // stop existing clip if any
                    stop();
                    if (pendingIndex < 0 || soundURL[pendingIndex] == null) {
                        System.err.println("No sound file set or invalid index: " + pendingIndex);
                        return;
                    }
                    AudioInputStream ais;
                    byte[] cached = audioDataCache.get(pendingIndex);
                    if (cached != null) {
                        ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(cached));
                    } else {
                        ais = AudioSystem.getAudioInputStream(soundURL[pendingIndex]);
                    }
                    clip = AudioSystem.getClip();
                    clip.open(ais);
                    clip.setFramePosition(0);
                    clip.start();
                } catch (Exception e) {
                    System.err.println("Error playing file: " + e.getMessage());
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void loop() {
        this.action = "loop";
        Thread t = new Thread(() -> {
            synchronized (this) {
                try {
                    stop();
                    if (pendingIndex < 0 || soundURL[pendingIndex] == null) {
                        System.err.println("No sound file set or invalid index: " + pendingIndex);
                        return;
                    }
                    AudioInputStream ais;
                    byte[] cached = audioDataCache.get(pendingIndex);
                    if (cached != null) {
                        ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(cached));
                    } else {
                        ais = AudioSystem.getAudioInputStream(soundURL[pendingIndex]);
                    }
                    clip = AudioSystem.getClip();
                    clip.open(ais);
                    clip.setFramePosition(0);
                    clip.loop(Clip.LOOP_CONTINUOUSLY);
                } catch (Exception e) {
                    System.err.println("Error looping file: " + e.getMessage());
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void stop() {
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
    }

    @Override
    public void run() {
        // Deprecated: playback is handled in play()/loop() threads now.
    }
}

