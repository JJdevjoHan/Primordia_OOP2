package engine;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.net.URL;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class SoundManager implements Runnable {
    private static final List<SoundManager> INSTANCES = Collections.synchronizedList(new ArrayList<>());
    private static final List<Clip> ACTIVE_SFX_CLIPS = Collections.synchronizedList(new ArrayList<>());

    Clip clip;
    URL[] soundURL = new URL[45];
    // Separate master gains for BGM and SFX so BGM can be lowered
    // without making skill SFX too quiet.
    // Default: BGM -6 dB, SFX 0 dB
    private static volatile float masterBgmGainDb = -6.0f;
    private static volatile float masterSfxGainDb = 0.0f;
    private int pendingIndex = -1;
    private int startFrameOffset = 0;  // Offset in frames for playback start position
    private final Map<Integer, byte[]> audioDataCache = new ConcurrentHashMap<>();
    private static final class SfxMappingEntry {
        final String path;
        final float volumeGainDb;

        SfxMappingEntry(String path, float volumeGainDb) {
            this.path = path;
            this.volumeGainDb = volumeGainDb;
        }
    }

    // Logical mapping loaded from data file: "Character Name:SkillKey" -> resource path + default volume
    private final Map<String, SfxMappingEntry> logicalToEntry = new HashMap<>();
    // Resolved map: "Character Name:SkillKey" -> soundURL index
    private final Map<String, Integer> logicalToIndex = new HashMap<>();

    //Constructor para pag assign sa sound | Just follow the current array sa last
    public SoundManager() {
        INSTANCES.add(this);
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
        //Fire Wizard - Skill 1 (Inferno Burst)
        soundURL[17] = getClass().getResource("/assets/SoundManager/SkillSfx/Fire Wizard/Skill1.wav");
        //Fire Wizard - Skill 2 (Flame Strike)
        soundURL[18] = getClass().getResource("/assets/SoundManager/SkillSfx/Fire Wizard/Skill2.wav");
        //Fire Wizard - Skill 3 (Meteor Storm)
        soundURL[19] = getClass().getResource("/assets/SoundManager/SkillSfx/Fire Wizard/Skill3.wav");
        //Steel Wizard - Skill 1.1 (Layered part 1)
        soundURL[20] = getClass().getResource("/assets/SoundManager/SkillSfx/Steel Wizard/Skill1.1.wav");
        //Steel Wizard - Skill 1.2 (Layered part 2)
        soundURL[21] = getClass().getResource("/assets/SoundManager/SkillSfx/Steel Wizard/Skill1.2.wav");
        //Steel Wizard - Skill 2.1 (Event-based sequence part 1)
        soundURL[22] = getClass().getResource("/assets/SoundManager/SkillSfx/Steel Wizard/Skill2.1.wav");
        //Steel Wizard - Skill 2.2 (Event-based sequence part 2 - on collision)
        soundURL[23] = getClass().getResource("/assets/SoundManager/SkillSfx/Steel Wizard/Skill2.2.wav");
        //Steel Wizard - Skill 3.1 (Timed offset part 1)
        soundURL[24] = getClass().getResource("/assets/SoundManager/SkillSfx/Steel Wizard/Skill3.1.wav");
        //Steel Wizard - Skill 3.2 (Timed offset part 2 - delayed)
        soundURL[25] = getClass().getResource("/assets/SoundManager/SkillSfx/Steel Wizard/Skill3.2.wav");
        //Nature Wizard - Skill 1.1 (Normal form)
        soundURL[26] = getClass().getResource("/assets/SoundManager/SkillSfx/Nature Wizard/Skill1.1.wav");
        //Nature Wizard - Skill 1.2 (Defense form)
        soundURL[27] = getClass().getResource("/assets/SoundManager/SkillSfx/Nature Wizard/Skill1.2.wav");
        //Nature Wizard - Skill 2
        soundURL[28] = getClass().getResource("/assets/SoundManager/SkillSfx/Nature Wizard/Skill2.wav");
        //Nature Wizard - Skill 3
        soundURL[29] = getClass().getResource("/assets/SoundManager/SkillSfx/Nature Wizard/Skill3.wav");
        // Dark Wizard - Skill 1
        soundURL[30] = getClass().getResource("/assets/SoundManager/SkillSfx/Dark Wizard/Skill1.wav");
        // Dark Wizard - Skill 2
        soundURL[31] = getClass().getResource("/assets/SoundManager/SkillSfx/Dark Wizard/Skill2.wav");
        // Dark Wizard - Skill 3
        soundURL[32] = getClass().getResource("/assets/SoundManager/SkillSfx/Dark Wizard/Skill3.wav");
        // Water Wizard - Skill Hold
        soundURL[33] = getClass().getResource("/assets/SoundManager/SkillSfx/Water Wizard/SkillHold.wav");
        // Water Wizard - Skill 1
        soundURL[34] = getClass().getResource("/assets/SoundManager/SkillSfx/Water Wizard/Skill1.wav");
        // Water Wizard - Skill 2
        soundURL[35] = getClass().getResource("/assets/SoundManager/SkillSfx/Water Wizard/Skill2.wav");
        // Water Wizard - Skill 3.1
        soundURL[36] = getClass().getResource("/assets/SoundManager/SkillSfx/Water Wizard/Skill3.1.wav");
        // Water Wizard - Skill 3.2
        soundURL[37] = getClass().getResource("/assets/SoundManager/SkillSfx/Water Wizard/Skill3.2.wav");
        // Wind Wizard - Skill 1
        soundURL[38] = getClass().getResource("/assets/SoundManager/SkillSfx/Wind Wizard/Skill1.wav");
        // Wind Wizard - Skill 2.1
        soundURL[39] = getClass().getResource("/assets/SoundManager/SkillSfx/Wind Wizard/Skill2.1.wav");
        // Wind Wizard - Skill 2.2
        soundURL[40] = getClass().getResource("/assets/SoundManager/SkillSfx/Wind Wizard/Skill2.2.wav");
        // Wind Wizard - Skill 3.1
        soundURL[41] = getClass().getResource("/assets/SoundManager/SkillSfx/Wind Wizard/Skill3.1.wav");
        // Wind Wizard - Skill 3.2
        soundURL[42] = getClass().getResource("/assets/SoundManager/SkillSfx/Wind Wizard/Skill3.2.wav");
        // After initializing soundURL table, attempt to load logical mappings
        loadSfxMappings();
    }

    private static void applyGainToActiveClips(boolean applyBgmGain) {
        if (applyBgmGain) {
            synchronized (INSTANCES) {
                for (SoundManager manager : INSTANCES) {
                    Clip activeClip = manager.clip;
                    if (activeClip == null) continue;
                    try {
                        FloatControl gainControl = (FloatControl) activeClip.getControl(FloatControl.Type.MASTER_GAIN);
                        gainControl.setValue(clampGainToControl(gainControl, masterBgmGainDb));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            return;
        }

        synchronized (ACTIVE_SFX_CLIPS) {
            for (Clip activeClip : ACTIVE_SFX_CLIPS) {
                if (activeClip == null) continue;
                try {
                    FloatControl gainControl = (FloatControl) activeClip.getControl(FloatControl.Type.MASTER_GAIN);
                    gainControl.setValue(clampGainToControl(gainControl, masterSfxGainDb));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    private static float clampGainToControl(FloatControl gainControl, float requestedGainDb) {
        float min = gainControl.getMinimum();
        float max = gainControl.getMaximum();
        if (requestedGainDb < min) return min;
        if (requestedGainDb > max) return max;
        return requestedGainDb;
    }

    private void loadSfxMappings() {
        // Prefer JSON mapping placed under /assets/data/sfx_mappings.json
        InputStream mappingStream = getClass().getResourceAsStream("/assets/data/sfx_mappings.json");
        if (mappingStream == null) {
            // fall back to old properties location for compatibility
            mappingStream = getClass().getResourceAsStream("/assets/SoundManager/sfx_mappings.properties");
        }
        if (mappingStream == null) return;

        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(mappingStream))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            String content = sb.toString().trim();
            if (content.startsWith("{")) {
                // Flat JSON parser for the expected mapping shape.
                // Supported entry forms:
                //   "Skill1": "/path.wav"
                //   "Skill1.volumeGainDb": 15.0
                java.util.regex.Pattern blockPat = java.util.regex.Pattern.compile("\"([^\"]+)\"\\s*:\\s*\\{([^}]+)\\}", java.util.regex.Pattern.DOTALL);
                java.util.regex.Matcher blockM = blockPat.matcher(content);
                while (blockM.find()) {
                    String character = blockM.group(1).trim();
                    String body = blockM.group(2);

                    Map<String, String> paths = new HashMap<>();
                    Map<String, Float> volumes = new HashMap<>();

                    java.util.regex.Pattern stringEntryPat = java.util.regex.Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"");
                    java.util.regex.Matcher stringEntryM = stringEntryPat.matcher(body);
                    while (stringEntryM.find()) {
                        String key = stringEntryM.group(1).trim();
                        String path = stringEntryM.group(2).trim();
                        if (!key.endsWith(".volumeGainDb")) {
                            paths.put(key, path);
                        }
                    }

                    java.util.regex.Pattern volumeEntryPat = java.util.regex.Pattern.compile("\"([^\"]+)\\.volumeGainDb\"\\s*:\\s*([-+]?[0-9]*\\.?[0-9]+)");
                    java.util.regex.Matcher volumeEntryM = volumeEntryPat.matcher(body);
                    while (volumeEntryM.find()) {
                        String key = volumeEntryM.group(1).trim();
                        float volumeGainDb = Float.parseFloat(volumeEntryM.group(2));
                        volumes.put(key, volumeGainDb);
                    }

                    for (Map.Entry<String, String> pathEntry : paths.entrySet()) {
                        String key = pathEntry.getKey();
                        float volumeGainDb = volumes.getOrDefault(key, 0.0f);
                        logicalToEntry.put(character + ":" + key, new SfxMappingEntry(pathEntry.getValue(), volumeGainDb));
                    }
                }
            } else {
                // Fallback simple properties-style parser (legacy)
                try (BufferedReader br = new BufferedReader(new InputStreamReader(new java.io.ByteArrayInputStream(content.getBytes())))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        int eq = line.indexOf('=');
                        if (eq <= 0) continue;
                        String key = line.substring(0, eq).trim();
                        String path = line.substring(eq + 1).trim();
                        logicalToEntry.put(key, new SfxMappingEntry(path, 0.0f));
                    }
                }
            }

            // Resolve logical paths to indices by scanning soundURL
            for (Map.Entry<String, SfxMappingEntry> e : logicalToEntry.entrySet()) {
                String logical = e.getKey();
                String relPath = e.getValue().path;
                int lastSlash = relPath.lastIndexOf('/');
                String filename = (lastSlash >= 0) ? relPath.substring(lastSlash + 1) : relPath;

                // Allow direct numeric index in JSON (e.g., "Skill1": "26")
                int forcedIndex = -1;
                try {
                    forcedIndex = Integer.parseInt(relPath);
                } catch (NumberFormatException ignored) {}

                int chosen = -1;
                if (forcedIndex >= 0 && forcedIndex < soundURL.length && soundURL[forcedIndex] != null) {
                    chosen = forcedIndex;
                } else {
                    // Score candidates: higher score means better match
                    List<Integer> candidates = new ArrayList<>();
                    List<Integer> scores = new ArrayList<>();
                    String character = "";
                    int colon = logical.indexOf(':');
                    if (colon > 0) character = logical.substring(0, colon).trim();

                    for (int i = 0; i < soundURL.length; i++) {
                        try {
                            if (soundURL[i] == null) continue;
                            String urlStr = soundURL[i].toString();
                            String decoded = urlStr;
                            try {
                                decoded = java.net.URLDecoder.decode(urlStr, java.nio.charset.StandardCharsets.UTF_8.name());
                            } catch (Exception ignored) {}
                            String ldecoded = decoded.toLowerCase();
                            String lrel = relPath.toLowerCase();
                            String lfilename = filename.toLowerCase();
                            int score = 0;
                            if (ldecoded.endsWith(lrel) || urlStr.endsWith(relPath)) score += 100; // best: endsWith full relPath
                            if (ldecoded.contains(lrel) || urlStr.contains(relPath)) score += 50; // contains relPath
                            if (ldecoded.endsWith(lfilename) || urlStr.endsWith(filename)) score += 30; // endsWith filename
                            if (ldecoded.contains(lfilename) || urlStr.contains(filename)) score += 10; // contains filename
                            if (!character.isEmpty() && ldecoded.contains(character.toLowerCase())) score += 25; // prefer candidate with character folder

                            if (score > 0) {
                                candidates.add(i);
                                scores.add(score);
                            }
                        } catch (Exception ignore) {}
                    }

                    // Pick highest score; tie-break by lowest index
                    int bestScore = -1;
                    for (int j = 0; j < candidates.size(); j++) {
                        int s = scores.get(j);
                        int idx = candidates.get(j);
                        if (s > bestScore || (s == bestScore && idx < chosen)) {
                            bestScore = s;
                            chosen = idx;
                        }
                    }
                }

                if (chosen >= 0) {
                    logicalToIndex.put(logical, chosen);
                } else {
                    System.err.println("Warning: SFX mapping not resolved for " + logical + " -> " + relPath);
                }
            }

        } catch (Exception ex) {
            System.err.println("Error loading SFX mappings: " + ex.getMessage());
        }
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

    /**
     * Set master gain (in decibels). Negative values lower volume, positive increase.
     * This affects clips opened after setting and will try to update the currently playing clip.
     */
    public void setMasterVolume(float db) {
        // Backwards-compatible: set BGM master gain
        setMasterBgmGain(db);
    }

    public float getMasterVolume() {
        return masterBgmGainDb;
    }

    public void setMasterBgmGain(float db) {
        masterBgmGainDb = db;
        applyGainToActiveClips(true);
    }

    public float getMasterBgmGain() {
        return masterBgmGainDb;
    }

    public void setMasterSfxGain(float db) {
        masterSfxGainDb = db;
        applyGainToActiveClips(false);
    }

    public float getMasterSfxGain() {
        return masterSfxGainDb;
    }

    /**
     * Set the global SFX volume using decibels. This value is shared by every SoundManager instance.
     */
    public static void setGlobalSfxGainDb(float db) {
        masterSfxGainDb = db;
        applyGainToActiveClips(false);
    }

    /**
     * Read the shared SFX gain in decibels.
     */
    public static float getGlobalSfxGainDb() {
        return masterSfxGainDb;
    }

    /**
     * Convenience control for users who prefer a 0-100 volume scale.
     * 0 maps to -80 dB, 100 maps to 0 dB.
     */
    public static void setGlobalSfxVolumePercent(int percent) {
        int clamped = Math.max(0, Math.min(100, percent));
        float db = -80.0f + (clamped / 100.0f) * 80.0f;
        setGlobalSfxGainDb(db);
    }

    public static int getGlobalSfxVolumePercent() {
        return Math.round(((masterSfxGainDb + 80.0f) / 80.0f) * 100.0f);
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

    public int getSkillSoundDurationMillis(int soundIndex, double timeOffsetSeconds) {
        if (soundIndex < 0 || soundIndex >= soundURL.length || soundURL[soundIndex] == null) {
            return 0;
        }

        try (AudioInputStream rawStream = AudioSystem.getAudioInputStream(soundURL[soundIndex]);
             AudioInputStream playableStream = toPlayableStream(rawStream)) {
            Clip tempClip = AudioSystem.getClip();
            try {
                tempClip.open(playableStream);
                long lengthMicros = tempClip.getMicrosecondLength();
                long offsetMicros = Math.max(0L, (long) (timeOffsetSeconds * 1_000_000L));
                long remainingMicros = Math.max(0L, lengthMicros - offsetMicros);
                return (int) Math.max(0L, remainingMicros / 1000L);
            } finally {
                tempClip.close();
            }
        } catch (Exception e) {
            System.err.println("Error measuring skill SFX index " + soundIndex + ": " + e.getMessage());
            return 0;
        }
    }

    private AudioInputStream toPlayableStream(AudioInputStream sourceStream) throws Exception {
        AudioFormat sourceFormat = sourceStream.getFormat();
        AudioFormat playableFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sourceFormat.getSampleRate(),
                16,
                sourceFormat.getChannels(),
                Math.max(1, sourceFormat.getChannels()) * 2,
                sourceFormat.getSampleRate(),
                false);

        if (sourceFormat.getEncoding() == AudioFormat.Encoding.PCM_SIGNED
                && sourceFormat.getSampleSizeInBits() == 16
                && !sourceFormat.isBigEndian()) {
            return sourceStream;
        }

        if (AudioSystem.isConversionSupported(playableFormat, sourceFormat)) {
            return AudioSystem.getAudioInputStream(playableFormat, sourceStream);
        }

        return sourceStream;
    }

    public void play() {
        // playback action: play
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
                    ais = toPlayableStream(ais);
                    clip = AudioSystem.getClip();
                    clip.open(ais);
                    // Apply BGM master gain to the current clip if supported
                    try {
                        FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                        gainControl.setValue(masterBgmGainDb);
                    } catch (IllegalArgumentException ignored) {
                        // Not all audio formats provide MASTER_GAIN control
                    }
                    
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
        // playback action: loop
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
                    ais = toPlayableStream(ais);
                    clip = AudioSystem.getClip();
                    clip.open(ais);
                    // Apply BGM master gain to BGM loop
                    try {
                        FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                        gainControl.setValue(masterBgmGainDb);
                    } catch (IllegalArgumentException ignored) {
                        // ignore
                    }
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
        playSkillSFX(soundIndex, timeOffsetSeconds, -1, 0.0f);  // No auto-stop, normal volume
    }

    public void playSkillSFX(int soundIndex, double timeOffsetSeconds, int stopAfterMillis) {
        playSkillSFX(soundIndex, timeOffsetSeconds, stopAfterMillis, 0.0f);  // Normal volume
    }

    public void playSkillSFX(int soundIndex, double timeOffsetSeconds, int stopAfterMillis, float volumeGainDb) {
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
                ais = toPlayableStream(ais);
                
                skillClip = AudioSystem.getClip();
                skillClip.open(ais);

                synchronized (ACTIVE_SFX_CLIPS) {
                    ACTIVE_SFX_CLIPS.add(skillClip);
                }

                final Clip activeSkillClip = skillClip;
                skillClip.addLineListener(event -> {
                    if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP
                            || event.getType() == javax.sound.sampled.LineEvent.Type.CLOSE) {
                        synchronized (ACTIVE_SFX_CLIPS) {
                            ACTIVE_SFX_CLIPS.remove(activeSkillClip);
                        }
                    }
                });
                
                // Apply BGM/SFX separation: use masterSfxGainDb + per-SFX volumeGainDb
                try {
                    FloatControl gainControl = (FloatControl) skillClip.getControl(FloatControl.Type.MASTER_GAIN);
                    float effective = masterSfxGainDb + volumeGainDb;
                    gainControl.setValue(clampGainToControl(gainControl, effective));
                } catch (IllegalArgumentException e) {
                    if (volumeGainDb != 0.0f) {
                        System.err.println("Volume control not available for skill SFX: " + e.getMessage());
                    }
                }
                
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

    /**
     * Play a skill SFX by logical mapping: characterName and skillKey form the mapping key as
     * "Character Name:SkillKey" (matching entries in sfx_mappings.properties).
     */
    public void playMappedSkillSFX(String characterName, String skillKey, double timeOffsetSeconds, int stopAfterMillis, float volumeGainDb) {
        if (characterName == null || skillKey == null) return;
        String logical = characterName + ":" + skillKey;
        Integer idx = logicalToIndex.get(logical);
        if (idx == null) {
            System.err.println("Mapped SFX not found for: " + logical);
            return;
        }
        float mappedVolumeGainDb = 0.0f;
        SfxMappingEntry entry = logicalToEntry.get(logical);
        if (entry != null) {
            mappedVolumeGainDb = entry.volumeGainDb;
        }
        playSkillSFX(idx, timeOffsetSeconds, stopAfterMillis, mappedVolumeGainDb + volumeGainDb);
    }

    

    @Override
    public void run() {
        // Deprecated: playback is handled in play()/loop() threads now.
    }
}

