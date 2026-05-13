package engine.core;

import engine.audio.SoundManager;

public class AudioManager {

    private final SoundManager menuMusic = new SoundManager();

    public void playMenuMusic() {
        menuMusic.setFile(6);
        menuMusic.loop();
    }

    public void stopMenuMusic() {
        menuMusic.stop();
    }
}