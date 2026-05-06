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
    private int startFrameOffset = 0;  // Offset in frames for playback start position
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
        //Idk Magician - Skill 1 (Lighting Burst)
        soundURL[11] = getClass().getResource("/assets/SoundManager/SkillSfx/Idk Magician/Skill1.wav");
        //Idk Magician - Skill 2 (Thunder Call)
        soundURL[12] = getClass().getResource("/assets/SoundManager/SkillSfx/Idk Magician/Skill2.wav");
        //Idk Magician - Skill 3 (Plasma Bolt)
        soundURL[13] = getClass().getResource("/assets/SoundManager/SkillSfx/Idk Magician/Skill3.wav");
        //Light Mage - Skill 1 (Light Sword)
        soundURL[14] = getClass().getResource("/assets/SoundManager/SkillSfx/Light Mage/Skill1.wav");
        //Light Mage - Skill 2 (Halo of Aegis)
        soundURL[15] = getClass().getResource("/assets/SoundManager/SkillSfx/Light Mage/Skill2.wav");
        //Light Mage - Skill 3 (Dawn Piercer)
        soundURL[16] = getClass().getResource("/assets/SoundManager/SkillSfx/Light Mage/Skill3.wav");


    }

    public void setFile(int i) {
        // Defer heavy audio loading to the play/loop thread to avoid blocking caller
        pendingIndex = i;
        startFrameOffset = 0;  // Reset offset when setting new file
    }

    public void setTimeOffset(double offsetSeconds) {
        // Will be converted to frames based on audio format when playing
        // This is a temporary storage; will be calculated in play() when we know the audio format
        if (offsetSeconds < 0) offsetSeconds = 0;
        this.startFrameOffset = (int)(offsetSeconds * 44100);  // Assume 44.1kHz; will be adjusted based on actual format
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
                    
                    // Calculate frame position based on actual audio format if offset is set
                    int framePos = 0;
                    if (startFrameOffset > 0) {
                        float sampleRate = ais.getFormat().getSampleRate();
                        framePos = (int)(startFrameOffset / 44100.0 * sampleRate);
                        framePos = Math.min(framePos, clip.getFrameLength() - 1);
                        startFrameOffset = 0;  // Reset for next play
                    }
                    
                    clip.setFramePosition(framePos);
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
            try {
                if (clip.isRunning()) {
                    clip.stop();
                }
                clip.close();
            } catch (Exception e) {
                System.err.println("Error stopping clip: " + e.getMessage());
            } finally {
                clip = null;
            }
        }
    }

    public void playSkillSFX(int soundIndex, double timeOffsetSeconds) {
        playSkillSFX(soundIndex, timeOffsetSeconds, -1);  // No auto-stop
    }

    public void playSkillSFX(int soundIndex, double timeOffsetSeconds, int stopAfterMillis) {
        if (soundIndex < 0 || soundIndex >= soundURL.length || soundURL[soundIndex] == null) {
            System.err.println("Invalid skill sound index: " + soundIndex);
            return;
        }
        
        Thread t = new Thread(() -> {
            Clip skillClip = null;
            try {
                AudioInputStream ais;
                byte[] cached = audioDataCache.get(soundIndex);
                if (cached != null) {
                    ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(cached));
                } else {
                    ais = AudioSystem.getAudioInputStream(soundURL[soundIndex]);
                }
                
                skillClip = AudioSystem.getClip();
                skillClip.open(ais);
                
                // Calculate frame position based on time offset
                int framePos = 0;
                if (timeOffsetSeconds > 0) {
                    float sampleRate = ais.getFormat().getSampleRate();
                    int frameOffset = (int)(timeOffsetSeconds * 44100);  // Assume 44.1kHz; will be adjusted
                    framePos = (int)(frameOffset / 44100.0 * sampleRate);
                    framePos = Math.min(framePos, skillClip.getFrameLength() - 1);
                }
                
                skillClip.setFramePosition(framePos);
                skillClip.start();
                
                // Auto-stop after specified duration if requested
                if (stopAfterMillis > 0) {
                    final Clip clipToStop = skillClip;
                    Thread stopThread = new Thread(() -> {
                        try {
                            Thread.sleep(stopAfterMillis);
                            try {
                                if (clipToStop != null && clipToStop.isRunning()) {
                                    clipToStop.stop();
                                }
                                if (clipToStop != null) {
                                    clipToStop.close();
                                }
                            } catch (Exception stopException) {
                                System.err.println("Error during auto-stop: " + stopException.getMessage());
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                    stopThread.setDaemon(true);
                    stopThread.start();
                }
            } catch (Exception e) {
                System.err.println("Error playing skill SFX index " + soundIndex + ": " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void run() {
        // Deprecated: playback is handled in play()/loop() threads now.
    }
}

