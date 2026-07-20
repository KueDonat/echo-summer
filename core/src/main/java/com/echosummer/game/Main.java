package com.echosummer.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Main game entry class for Echo Summer.
 * Manages the screen states and shared resources such as SpriteBatch.
 */
public class Main extends Game {
    private SpriteBatch batch;
    private Music menuMusic;

    @Override
    public void create() {
        batch = new SpriteBatch();
        playMenuMusic();
        // Set the initial screen to the Main Menu
        this.setScreen(new MainMenuScreen(this));
    }

    /**
     * Starts playing the main menu background music.
     */
    public void playMenuMusic() {
        if (menuMusic == null) {
            menuMusic = Gdx.audio.newMusic(Gdx.files.internal("backsound_main_menu.mp3"));
            menuMusic.setLooping(true);
            menuMusic.setVolume(SettingsManager.getVolume()); 
        }
        if (!menuMusic.isPlaying()) {
            menuMusic.play();
        }
    }

    /**
     * Stops playing the main menu background music.
     */
    public void stopMenuMusic() {
        if (menuMusic != null && menuMusic.isPlaying()) {
            menuMusic.stop();
        }
    }
    
    public void updateMusicVolume() {
        if (menuMusic != null) {
            menuMusic.setVolume(SettingsManager.getVolume());
        }
    }

    /**
     * Encapsulated getter for the SpriteBatch.
     * Allows screens to share a single SpriteBatch instance for optimized rendering.
     * 
     * @return the shared SpriteBatch instance
     */
    public SpriteBatch getBatch() {
        return batch;
    }

    @Override
    public void render() {
        // Delegate rendering to the active screen
        super.render();
    }

    @Override
    public void dispose() {
        super.dispose();
        if (batch != null) {
            batch.dispose();
        }
        if (menuMusic != null) {
            menuMusic.dispose();
        }
    }
}

