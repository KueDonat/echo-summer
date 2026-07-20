package com.echosummer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

/**
 * Handles persistence of GameState to local disk.
 */
public class SaveManager {
    private static final String SAVE_FILE_NAME = "savegame.dat";

    public static void saveGame(GameState state) {
        try {
            FileHandle file = Gdx.files.local(SAVE_FILE_NAME);
            file.writeString(state.serialize(), false);
            Gdx.app.log("SaveManager", "Game saved successfully.");
        } catch (Exception e) {
            Gdx.app.error("SaveManager", "Failed to save game: " + e.getMessage());
        }
    }

    public static GameState loadGame() {
        GameState state = new GameState();
        try {
            FileHandle file = Gdx.files.local(SAVE_FILE_NAME);
            if (file.exists()) {
                String content = file.readString();
                state.deserialize(content);
                Gdx.app.log("SaveManager", "Game loaded successfully.");
            } else {
                Gdx.app.log("SaveManager", "No save file found. Initializing new game state.");
            }
        } catch (Exception e) {
            Gdx.app.error("SaveManager", "Failed to load game: " + e.getMessage());
        }
        return state;
    }
}
