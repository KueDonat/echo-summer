package com.echosummer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Align;
import java.util.Random;

/**
 * Handles the 4-lane rhythm game mechanics during the performance.
 * Synchronizes note movement and hit detection with the Music position.
 */
public class RhythmGame {
    public static class Note {
        public float targetTime; // Time in seconds when this note should be hit
        public int lane;         // 0: D, 1: F, 2: J, 3: K
        public boolean hit = false;
        public boolean missed = false;
        public boolean isSlider = false;
        public float duration = 0f;
        public boolean isHeld = false;

        public Note(float targetTime, int lane) {
            this.targetTime = targetTime;
            this.lane = lane;
        }
    }

    private final Array<Note> notes = new Array<>();
    private float scrollSpeed = 350f; // Pixels per second

    public int score = 0;
    public int perfects = 0;
    public int goods = 0;
    public int misses = 0;
    public int combo = 0;
    public int maxCombo = 0;

    public String feedback = "";
    public float feedbackTimer = 0f;
    public Color feedbackColor = Color.WHITE;

    private final float hitZoneY = 150f;

    // Precision thresholds in seconds
    private float thresholdPerfect = 0.05f;
    private float thresholdGood = 0.12f;
    private float thresholdOk = 0.20f;

    private Music music;
    private Sound kresekSound;
    private boolean active = false;
    private float timeElapsed = 0f;
    private float duration = 400f; // Default duration

    // HP / Life system
    private float hp = 100f;
    private final float maxHp = 100f;
    private boolean failed = false;
    private boolean wasFailed = false;

    // Lanes styling
    private final String[] laneKeys = {"D", "F", "J", "K"};

    public void start(Music music) {
        start(music, 400f);
    }

    public void start(Music music, float targetDuration) {
        this.music = music;
        this.duration = targetDuration;

        if (kresekSound == null) {
            try {
                kresekSound = Gdx.audio.newSound(Gdx.files.internal("kresek.wav"));
            } catch (Exception e) {
                Gdx.app.error("RhythmGame", "Could not load kresek.wav");
            }
        }

        notes.clear();
        score = 0;
        perfects = 0;
        goods = 0;
        misses = 0;
        combo = 0;
        maxCombo = 0;
        hp = 100f;
        failed = false;
        wasFailed = false;
        feedback = "BERSIAPLAH!";
        feedbackColor = Color.YELLOW;
        feedbackTimer = 1.5f;
        timeElapsed = 0f;
        musicStoppedTimer = 0f;
        lastNoteTime = 0f;
        active = true;

        // Generate seeded chart for a consistent rhythm game
        generateChart();

        if (music != null) {
            music.stop();
            music.setPosition(0);
            music.setLooping(false);
            music.play();
        }
    }

    public boolean isActive() {
        return active;
    }

    public boolean isFailed() {
        return failed;
    }

    public boolean wasFailed() {
        return wasFailed;
    }

    public float getCurrentTime() {
        if (music != null && music.isPlaying()) {
            return music.getPosition();
        }
        return timeElapsed;
    }

    private void generateChart() {
        notes.clear();
        
        int diff = SettingsManager.getDifficulty();
        if (diff == 0) {
            scrollSpeed = 280f;
            thresholdPerfect = 0.05f;
            thresholdGood = 0.10f;
            thresholdOk = 0.16f;
        } else if (diff == 2) {
            scrollSpeed = 500f;
            thresholdPerfect = 0.02f;
            thresholdGood = 0.05f;
            thresholdOk = 0.09f;
        } else {
            scrollSpeed = 380f;
            thresholdPerfect = 0.035f;
            thresholdGood = 0.08f;
            thresholdOk = 0.13f;
        }

        Random rand = new Random(42);
        float bpm = 130f;
        float beat = 60f / bpm; // ~0.4615s
        float halfBeat = beat / 2f; // ~0.2308s
        float quarterBeat = beat / 4f; // ~0.1154s
        
        float step = quarterBeat;
        float[] laneFreeTime = new float[4];
        for (int i = 0; i < 4; i++) {
            laneFreeTime[i] = 0f;
        }

        int totalSteps = (int) ((duration - 8.0f) / step);

        for (int s = 0; s < totalSteps; s++) {
            float time = 3.0f + s * step;

            boolean isDownbeat = (s % 4 == 0);
            boolean isUpbeat = (s % 2 == 0);
            
            float spawnProb = 0f;
            if (diff == 0) { // Easy
                if (isDownbeat) spawnProb = 0.5f;
            } else if (diff == 1) { // Medium
                if (isDownbeat) spawnProb = 0.7f;
                else if (isUpbeat) spawnProb = 0.35f;
            } else { // Hard
                if (isDownbeat) spawnProb = 0.85f;
                else if (isUpbeat) spawnProb = 0.6f;
                else spawnProb = 0.25f; // stream / sixteenth note
            }

            if (rand.nextFloat() > spawnProb) continue;

            int freeCount = 0;
            for (int i = 0; i < 4; i++) {
                if (time >= laneFreeTime[i]) {
                    freeCount++;
                }
            }
            if (freeCount == 0) continue;

            int chordSize = 1;
            if (isDownbeat && freeCount >= 2) {
                float chordProb = (diff == 0) ? 0.05f : (diff == 1 ? 0.2f : 0.4f);
                if (rand.nextFloat() < chordProb) {
                    chordSize = 2;
                }
            }

            Array<Integer> availableLanes = new Array<>();
            for (int i = 0; i < 4; i++) {
                if (time >= laneFreeTime[i]) availableLanes.add(i);
            }
            availableLanes.shuffle();

            for (int i = 0; i < chordSize && i < availableLanes.size; i++) {
                int lane = availableLanes.get(i);
                Note note = new Note(time, lane);
                
                float sliderProb = (diff == 0) ? 0.1f : (diff == 1 ? 0.15f : 0.2f);
                if (rand.nextFloat() < sliderProb) {
                    note.isSlider = true;
                    int[] sliderDurations = {4, 8, 16};
                    int durIndex = rand.nextInt(diff == 0 ? 2 : 3);
                    float durationSeconds = sliderDurations[durIndex] * step;
                    note.duration = durationSeconds;
                    laneFreeTime[lane] = time + durationSeconds + step;
                } else {
                    laneFreeTime[lane] = time + step;
                }

                notes.add(note);
            }
        }

        if (notes.size > 0) {
            Note lastNote = notes.get(notes.size - 1);
            lastNoteTime = lastNote.targetTime + (lastNote.isSlider ? lastNote.duration : 0f) + 3.0f;
        } else {
            lastNoteTime = duration;
        }
    }

    private void failGame() {
        failed = true;
        if (music != null) {
            music.stop();
        }
        feedback = "GAGAL!";
        feedbackColor = Color.RED;
        feedbackTimer = 999f;
    }

    private void retry() {
        failed = false;
        wasFailed = false;
        hp = 100f;
        score = 0;
        perfects = 0;
        goods = 0;
        misses = 0;
        combo = 0;
        maxCombo = 0;
        feedback = "BERSIAPLAH!";
        feedbackColor = Color.YELLOW;
        feedbackTimer = 1.5f;
        timeElapsed = 0f;
        active = true;

        generateChart();

        if (music != null) {
            music.stop();
            music.setPosition(0);
            music.play();
        }
    }

    private void giveUp() {
        failed = false;
        wasFailed = true;
        active = false;
        if (music != null) {
            music.stop();
        }
    }

    private boolean paused = false;

    public void pause() {
        paused = true;
        if (music != null && music.isPlaying()) {
            music.pause();
        }
    }

    public void resume() {
        paused = false;
        musicStoppedTimer = 0f;
        if (music != null && active && !failed) {
            music.play();
        }
    }

    public boolean isPaused() {
        return paused;
    }

    private float lastNoteTime = 0f;
    private float musicStoppedTimer = 0f;

    public void update(float delta) {
        if (!active || paused) return;

        if (failed) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.R) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                retry();
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                giveUp();
            }
            return;
        }

        float currentTime = getCurrentTime();
        if (music != null && music.isPlaying()) {
            timeElapsed = music.getPosition();
            musicStoppedTimer = 0f;
        } else {
            timeElapsed += delta;
            musicStoppedTimer += delta;
        }

        // Robust end condition: requires 2.5 seconds of continuous stopped audio or chart completion so transient stutters never end the game prematurely
        boolean musicEnded = (music != null && musicStoppedTimer >= 2.5f && timeElapsed > 10.0f);
        boolean chartEnded = (lastNoteTime > 0 && timeElapsed >= lastNoteTime);
        boolean durationEnded = (timeElapsed >= duration);

        if (musicEnded || chartEnded || durationEnded) {
            active = false;
            if (music != null) {
                music.stop();
            }
            return;
        }

        // Track missed notes that pass the hit zone
        for (int i = 0; i < notes.size; i++) {
            Note n = notes.get(i);

            // Auto-complete held slider
            if (n.isSlider && n.isHeld && currentTime >= n.targetTime + n.duration) {
                n.isHeld = false;
                n.hit = true;
                feedback = "PERFECT!";
                feedbackColor = Color.GOLD;
                score += 50;
                perfects++;
                combo++;
                maxCombo = Math.max(maxCombo, combo);
                hp = Math.min(maxHp, hp + 2f);
                feedbackTimer = 0.5f;
            }

            if (!n.hit && !n.missed && !n.isHeld && (currentTime - n.targetTime) > thresholdOk) {
                n.missed = true;
                feedback = "MISS!";
                feedbackColor = Color.RED;
                feedbackTimer = 0.5f;
                misses++;
                combo = 0;

                float hpDrain = 8f;
                int diff = SettingsManager.getDifficulty();
                if (diff == 0) hpDrain = 6f;
                else if (diff == 2) hpDrain = 12f;
                hp = Math.max(0f, hp - hpDrain);

                if (hp <= 0f && !failed) {
                    failGame();
                    return;
                }

                if (kresekSound != null) kresekSound.play(SettingsManager.getVolume());
            }
        }

        // Update feedback message timer
        if (feedbackTimer > 0) {
            feedbackTimer -= delta;
        }
    }

    /**
     * Triggers when a key is pressed.
     * Maps keys to lanes: D -> 0, F -> 1, J -> 2, K -> 3.
     */
    public boolean handleKeyPress(int keycode) {
        if (!active) {
            if (failed) {
                if (keycode == Input.Keys.R || keycode == Input.Keys.SPACE || keycode == Input.Keys.ENTER) {
                    retry();
                    return true;
                } else if (keycode == Input.Keys.ESCAPE) {
                    giveUp();
                    return true;
                }
            }
            return false;
        }

        int lane = -1;
        if (keycode == Input.Keys.D) lane = 0;
        else if (keycode == Input.Keys.F) lane = 1;
        else if (keycode == Input.Keys.J) lane = 2;
        else if (keycode == Input.Keys.K) lane = 3;

        if (lane == -1) return false;

        float currentTime = getCurrentTime();
        Note closestNote = null;
        float minTimeDiff = Float.MAX_VALUE;

        // Find the closest unhit note in the pressed lane
        for (int i = 0; i < notes.size; i++) {
            Note n = notes.get(i);
            if (n.lane == lane && !n.hit && !n.missed) {
                float diff = Math.abs(currentTime - n.targetTime);
                if (diff < minTimeDiff && diff <= thresholdOk) {
                    minTimeDiff = diff;
                    closestNote = n;
                }
            }
        }

        // Process the hit evaluation
        if (closestNote != null) {
            if (closestNote.isSlider) {
                closestNote.isHeld = true;
            } else {
                closestNote.hit = true;
            }
            if (minTimeDiff <= thresholdPerfect) {
                feedback = "PERFECT!";
                feedbackColor = Color.GOLD;
                score += 50;
                perfects++;
                combo++;
                maxCombo = Math.max(maxCombo, combo);
                hp = Math.min(maxHp, hp + 2f);
            } else if (minTimeDiff <= thresholdGood) {
                feedback = "GOOD!";
                feedbackColor = Color.GREEN;
                score += 30;
                goods++;
                combo++;
                maxCombo = Math.max(maxCombo, combo);
                hp = Math.min(maxHp, hp + 1f);
            } else {
                feedback = "OK!";
                feedbackColor = Color.CYAN;
                score += 15;
                combo++;
                maxCombo = Math.max(maxCombo, combo);
                hp = Math.min(maxHp, hp + 0.5f);
            }
            feedbackTimer = 0.5f;
            return true;
        } else {
            feedback = "MISS!";
            feedbackColor = Color.RED;
            feedbackTimer = 0.5f;
            misses++;
            combo = 0;

            float hpDrain = 2f;
            int diff = SettingsManager.getDifficulty();
            if (diff == 0) hpDrain = 1f;
            else if (diff == 2) hpDrain = 4f;
            hp = Math.max(0f, hp - hpDrain);

            if (hp <= 0f && !failed) {
                failGame();
            }

            if (kresekSound != null) kresekSound.play(SettingsManager.getVolume());
        }

        return false;
    }

    public boolean handleKeyRelease(int keycode) {
        if (!active) return false;
        int lane = -1;
        if (keycode == Input.Keys.D) lane = 0;
        else if (keycode == Input.Keys.F) lane = 1;
        else if (keycode == Input.Keys.J) lane = 2;
        else if (keycode == Input.Keys.K) lane = 3;

        if (lane == -1) return false;

        float currentTime = getCurrentTime();

        for (int i = 0; i < notes.size; i++) {
            Note n = notes.get(i);
            if (n.isSlider && n.isHeld && n.lane == lane) {
                n.isHeld = false;
                float expectedEnd = n.targetTime + n.duration;

                if (currentTime < expectedEnd - thresholdOk) {
                    n.missed = true;
                    feedback = "MISS!";
                    feedbackColor = Color.RED;
                    feedbackTimer = 0.5f;
                    misses++;
                    combo = 0;

                    float hpDrain = 8f;
                    int diff = SettingsManager.getDifficulty();
                    if (diff == 0) hpDrain = 6f;
                    else if (diff == 2) hpDrain = 12f;
                    hp = Math.max(0f, hp - hpDrain);

                    if (hp <= 0f && !failed) {
                        failGame();
                    }

                    if (kresekSound != null) kresekSound.play(SettingsManager.getVolume());
                } else {
                    n.hit = true;
                    feedback = "PERFECT!";
                    feedbackColor = Color.GOLD;
                    score += 50;
                    perfects++;
                    combo++;
                    maxCombo = Math.max(maxCombo, combo);
                    hp = Math.min(maxHp, hp + 2f);
                    feedbackTimer = 0.5f;
                }
                return true;
            }
        }
        return false;
    }

    public void draw(ShapeRenderer shape, SpriteBatch batch, BitmapFont font, float width, float height) {
        if (!active && !failed) return;

        float currentTime = getCurrentTime();
        float laneWidth = 80f;
        float totalWidth = laneWidth * 4;
        float startX = (width - totalWidth) / 2f;

        if (active) {
            // 1. Draw lanes backgrounds using ShapeRenderer
            Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
            shape.begin(ShapeRenderer.ShapeType.Filled);

            // Draw overall track background
            shape.setColor(new Color(0.02f, 0.04f, 0.1f, 0.6f));
            shape.rect(startX, 0, totalWidth, height);

            // Draw individual lanes separator lines
            shape.setColor(new Color(0.2f, 0.3f, 0.5f, 0.4f));
            for (int i = 1; i < 4; i++) {
                shape.rect(startX + i * laneWidth - 1, 0, 2, height);
            }

            // Draw target line/zone
            shape.setColor(new Color(0.97f, 0.96f, 0.95f, 0.8f));
            shape.rect(startX, hitZoneY - 4, totalWidth, 8);

            // Check key status for visual feedback beams & glow
            boolean[] lanePressed = new boolean[4];
            lanePressed[0] = Gdx.input.isKeyPressed(Input.Keys.D);
            lanePressed[1] = Gdx.input.isKeyPressed(Input.Keys.F);
            lanePressed[2] = Gdx.input.isKeyPressed(Input.Keys.J);
            lanePressed[3] = Gdx.input.isKeyPressed(Input.Keys.K);

            for (int i = 0; i < 4; i++) {
                if (lanePressed[i]) {
                    shape.setColor(new Color(1f, 1f, 1f, 0.45f));
                    shape.circle(startX + i * laneWidth + laneWidth / 2f, hitZoneY, 26);
                    
                    shape.setColor(new Color(1f, 1f, 1f, 0.08f));
                    shape.rect(startX + i * laneWidth, hitZoneY, laneWidth, height - hitZoneY);
                } else {
                    shape.setColor(new Color(0.97f, 0.96f, 0.95f, 0.25f));
                    shape.circle(startX + i * laneWidth + laneWidth / 2f, hitZoneY, 24);
                }
            }

            // Draw HP Bar Background
            shape.setColor(new Color(0.1f, 0.1f, 0.15f, 0.6f));
            shape.rect(startX + totalWidth + 25f, hitZoneY, 16f, height - 250f);

            // Draw HP Bar Fill
            float fillHeight = (height - 250f) * (hp / maxHp);
            if (hp > 50f) {
                shape.setColor(new Color(0.2f, 0.8f, 0.4f, 0.9f));
            } else if (hp > 20f) {
                shape.setColor(new Color(0.9f, 0.6f, 0.1f, 0.9f));
            } else {
                shape.setColor(new Color(0.9f, 0.2f, 0.2f, 0.9f));
            }
            shape.rect(startX + totalWidth + 25f, hitZoneY, 16f, fillHeight);

            // Draw falling notes
            for (int i = 0; i < notes.size; i++) {
                Note n = notes.get(i);
                if (n.hit || n.missed) continue;

                float timeOffset = n.targetTime - currentTime;
                float ny = hitZoneY + timeOffset * scrollSpeed;
                float endY = ny;
                if (n.isSlider) {
                    endY = hitZoneY + (n.targetTime + n.duration - currentTime) * scrollSpeed;
                }

                if (endY > 0 && ny < height + 50) {
                    Color laneColor;
                    if (n.lane == 0) laneColor = new Color(0.95f, 0.3f, 0.4f, 1f);
                    else if (n.lane == 1) laneColor = new Color(0.3f, 0.8f, 0.4f, 1f);
                    else if (n.lane == 2) laneColor = new Color(0.2f, 0.6f, 0.95f, 1f);
                    else laneColor = new Color(0.95f, 0.8f, 0.2f, 1f);

                    shape.setColor(laneColor);

                    float nx = startX + n.lane * laneWidth + laneWidth / 2f;

                    if (n.isSlider) {
                        shape.setColor(new Color(laneColor.r, laneColor.g, laneColor.b, 0.6f));
                        float drawNy = Math.max(ny, hitZoneY);
                        if (n.isHeld) drawNy = hitZoneY;
                        shape.rect(nx - 10, drawNy, 20, endY - drawNy);
                    }

                    if (!n.isHeld) {
                        shape.setColor(laneColor);
                        shape.circle(nx, ny, 20);
                        shape.setColor(Color.WHITE);
                        shape.circle(nx, ny, 8);
                    }
                }
            }

            shape.end();

            // Draw HP Bar Border & Music Progress Bar
            shape.begin(ShapeRenderer.ShapeType.Filled);
            float barW = 320f;
            float barH = 8f;
            float barX = (width - barW) / 2f;
            float barY = height - 120f;
            float progress = Math.min(1.0f, Math.max(0f, currentTime / duration));

            shape.setColor(new Color(0.12f, 0.15f, 0.25f, 0.8f));
            shape.rect(barX, barY, barW, barH);
            shape.setColor(new Color(0.2f, 0.85f, 1.0f, 0.95f));
            shape.rect(barX, barY, barW * progress, barH);
            shape.end();

            shape.begin(ShapeRenderer.ShapeType.Line);
            shape.setColor(new Color(1f, 1f, 1f, 0.4f));
            shape.rect(startX + totalWidth + 25f, hitZoneY, 16f, height - 250f);
            shape.rect(barX, barY, barW, barH);
            shape.end();

            Gdx.gl.glDisable(Gdx.gl.GL_BLEND);

            // 2. Draw text HUD
            batch.begin();
            font.setColor(Color.WHITE);
            font.draw(batch, "LATIHAN GITAR - \"SEANDAINYA - VIERRA\"", width / 2f - 200f, height - 35, 400, 1, false);
            font.draw(batch, "Skor: " + score + "   (P: " + perfects + "  G: " + goods + "  M: " + misses + ")", width / 2f - 200f, height - 65, 400, 1, false);

            int curSec = (int) Math.max(0, currentTime);
            int totalSec = (int) Math.max(0, duration);
            String timeStr = String.format("🎵 Waktu: %02d:%02d / %02d:%02d", curSec / 60, curSec % 60, totalSec / 60, totalSec % 60);
            font.setColor(Color.GOLD);
            font.draw(batch, timeStr, width / 2f - 200f, height - 95, 400, 1, false);

            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, "HP", startX + totalWidth + 20f, hitZoneY - 10f, 26f, 1, false);

            font.setColor(Color.WHITE);
            for (int i = 0; i < 4; i++) {
                float kx = startX + i * laneWidth + laneWidth / 2f - 8;
                font.draw(batch, laneKeys[i], kx, hitZoneY - 40);
            }

            // Draw combo in center
            if (combo > 0) {
                font.setColor(new Color(1f, 1f, 1f, 0.85f));
                font.getData().setScale(1.6f);
                font.draw(batch, String.valueOf(combo), startX, hitZoneY + 140f, totalWidth, Align.center, false);
                font.getData().setScale(0.85f);
                font.setColor(new Color(1f, 1f, 1f, 0.55f));
                font.draw(batch, "COMBO", startX, hitZoneY + 112f, totalWidth, Align.center, false);
                font.getData().setScale(1.0f);
            }

            // Draw feedback text centered
            if (feedbackTimer > 0) {
                font.setColor(feedbackColor);
                font.getData().setScale(1.2f);
                font.draw(batch, feedback, startX, hitZoneY + 60f, totalWidth, Align.center, false);
                font.getData().setScale(1.0f);
            }
            batch.end();
        }

        // Render FAILED screen overlay
        if (failed) {
            Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(new Color(0f, 0f, 0.05f, 0.85f));
            shape.rect(0, 0, width, height);
            shape.end();
            Gdx.gl.glDisable(Gdx.gl.GL_BLEND);

            batch.begin();
            font.setColor(Color.RED);
            font.getData().setScale(2.5f);
            font.draw(batch, "PERMAINAN GAGAL!", startX, height / 2f + 140f, totalWidth, Align.center, false);
            font.getData().setScale(1.0f);

            font.setColor(Color.WHITE);
            font.draw(batch, "Kamu terlalu banyak melakukan Miss.", startX - 100f, height / 2f + 70f, totalWidth + 200f, Align.center, false);

            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, "Skor Akhir: " + score, startX, height / 2f + 10f, totalWidth, Align.center, false);
            font.draw(batch, "Perfect: " + perfects, startX, height / 2f - 20f, totalWidth, Align.center, false);
            font.draw(batch, "Good: " + goods, startX, height / 2f - 50f, totalWidth, Align.center, false);
            font.draw(batch, "Miss: " + misses, startX, height / 2f - 80f, totalWidth, Align.center, false);
            font.draw(batch, "Combo Maksimal: " + maxCombo, startX, height / 2f - 110f, totalWidth, Align.center, false);

            font.setColor(Color.YELLOW);
            font.draw(batch, "Tekan [ R / SPACE / ENTER ] untuk Mengulang Latihan", startX - 100f, height / 2f - 170f, totalWidth + 200f, Align.center, false);
            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, "Tekan [ ESC ] untuk Keluar / Kembali ke Kost", startX - 100f, height / 2f - 200f, totalWidth + 200f, Align.center, false);
            batch.end();
        }
    }
}
