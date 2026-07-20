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

    public String feedback = "";
    public float feedbackTimer = 0f;
    public Color feedbackColor = Color.WHITE;

    private final float hitZoneY = 150f;

    // Precision thresholds in seconds
    private float thresholdPerfect = 0.05f; // < 50ms
    private float thresholdGood = 0.12f;    // < 120ms
    private float thresholdOk = 0.20f;      // < 200ms

    private Music music;
    private Sound kresekSound;
    private boolean active = false;
    private float timeElapsed = 0f;
    private final float duration = 400f; // Long enough to cover full song

    // Lanes styling
    private final String[] laneKeys = {"D", "F", "J", "K"};

    public void start(Music music) {
        this.music = music;

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
        feedback = "GET READY!";
        feedbackColor = Color.YELLOW;
        feedbackTimer = 1.5f;
        timeElapsed = 0f;
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

    public float getCurrentTime() {
        if (music != null && music.isPlaying()) {
            return music.getPosition();
        }
        return timeElapsed;
    }

    private void generateChart() {
        notes.clear();
        
        float timeStepMultiplier = 1.0f;
        int diff = SettingsManager.getDifficulty();
        if (diff == 0) {
            scrollSpeed = 250f;
            thresholdPerfect = 0.08f;
            thresholdGood = 0.16f;
            thresholdOk = 0.25f;
            timeStepMultiplier = 1.3f;
        } else if (diff == 2) {
            scrollSpeed = 500f;
            thresholdPerfect = 0.04f;
            thresholdGood = 0.10f;
            thresholdOk = 0.15f;
            timeStepMultiplier = 0.75f;
        } else {
            scrollSpeed = 350f;
            thresholdPerfect = 0.05f;
            thresholdGood = 0.12f;
            thresholdOk = 0.20f;
            timeStepMultiplier = 1.0f;
        }

        Random rand = new Random(42);
        float time = 3.0f;
        int lastLane = -1;

        while (time < duration - 3.0f) {
            int lane = rand.nextInt(4);
            if (lane == lastLane) lane = (lane + 1) % 4;

            Note newNote = new Note(time, lane);
            if (rand.nextFloat() < 0.2f) {
                newNote.isSlider = true;
                newNote.duration = (1.0f + rand.nextFloat() * 1.5f) * timeStepMultiplier;
            }
            notes.add(newNote);
            lastLane = lane;

            if (!newNote.isSlider && rand.nextFloat() < 0.25f) {
                notes.add(new Note(time, (lane + 2) % 4));
            }

            time += (newNote.isSlider ? newNote.duration : 0) + (0.6f + rand.nextFloat() * 0.8f) * timeStepMultiplier;
        }
    }

    public void update(float delta) {
        if (!active) return;

        float currentTime = getCurrentTime();
        if (music == null || !music.isPlaying()) {
            timeElapsed += delta;
        } else {
            timeElapsed = music.getPosition();
        }

        // End condition
        if (timeElapsed >= duration || (music != null && !music.isPlaying() && timeElapsed > 5.0f)) {
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
                feedbackTimer = 0.5f;
            }

            if (!n.hit && !n.missed && !n.isHeld && (currentTime - n.targetTime) > thresholdOk) {
                n.missed = true;
                feedback = "MISS!";
                feedbackColor = Color.RED;
                feedbackTimer = 0.5f;
                misses++;
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
        if (!active) return false;

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
            } else if (minTimeDiff <= thresholdGood) {
                feedback = "GOOD!";
                feedbackColor = Color.GREEN;
                score += 30;
                goods++;
            } else {
                feedback = "OK!";
                feedbackColor = Color.CYAN;
                score += 15;
            }
            feedbackTimer = 0.5f;
            return true;
        } else {
            feedback = "MISS!";
            feedbackColor = Color.RED;
            feedbackTimer = 0.5f;
            misses++;
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
                    if (kresekSound != null) kresekSound.play(SettingsManager.getVolume());
                } else {
                    n.hit = true;
                    feedback = "PERFECT!";
                    feedbackColor = Color.GOLD;
                    score += 50;
                    perfects++;
                    feedbackTimer = 0.5f;
                }
                return true;
            }
        }
        return false;
    }

    public void draw(ShapeRenderer shape, SpriteBatch batch, BitmapFont font, float width, float height) {
        if (!active) return;

        float currentTime = getCurrentTime();
        float laneWidth = 80f;
        float totalWidth = laneWidth * 4;
        float startX = (width - totalWidth) / 2f;

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

        // Draw lane targets circles
        for (int i = 0; i < 4; i++) {
            shape.setColor(new Color(0.97f, 0.96f, 0.95f, 0.25f));
            shape.circle(startX + i * laneWidth + laneWidth / 2f, hitZoneY, 24);
        }

        // Draw falling notes based on their target times
        for (int i = 0; i < notes.size; i++) {
            Note n = notes.get(i);
            if (n.hit || n.missed) continue;

            // Derive Y position directly from time offset
            float timeOffset = n.targetTime - currentTime;
            float ny = hitZoneY + timeOffset * scrollSpeed;
            float endY = ny;
            if (n.isSlider) {
                endY = hitZoneY + (n.targetTime + n.duration - currentTime) * scrollSpeed;
            }

            // Only draw note if it's visible on screen
            if (endY > 0 && ny < height + 50) {
                // Color based on lane
                Color laneColor;
                if (n.lane == 0) laneColor = new Color(0.95f, 0.3f, 0.4f, 1f); // Red
                else if (n.lane == 1) laneColor = new Color(0.3f, 0.8f, 0.4f, 1f); // Green
                else if (n.lane == 2) laneColor = new Color(0.2f, 0.6f, 0.95f, 1f); // Blue
                else laneColor = new Color(0.95f, 0.8f, 0.2f, 1f); // Yellow

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
        Gdx.gl.glDisable(Gdx.gl.GL_BLEND);

        // 2. Draw text HUD
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "LATIHAN GITAR - \"SEANDAINYA - VIERRA\"", width / 2f - 200f, height - 40, 400, 1, false);
        font.draw(batch, "Score: " + score + "   (P: " + perfects + "  G: " + goods + "  M: " + misses + ")", width / 2f - 200f, height - 70, 400, 1, false);

        // Draw key labels under targets
        for (int i = 0; i < 4; i++) {
            float kx = startX + i * laneWidth + laneWidth / 2f - 8;
            font.draw(batch, laneKeys[i], kx, hitZoneY - 40);
        }

        // Draw hit feedback text
        if (feedbackTimer > 0) {
            font.setColor(feedbackColor);
            font.draw(batch, feedback, width / 2f - 100f, hitZoneY + 80, 200, 1, false);
        }
        batch.end();
    }
}
