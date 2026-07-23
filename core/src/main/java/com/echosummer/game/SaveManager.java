package com.echosummer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

/**
 * Handles persistence of GameState to local disk.
 * Extended to support multiple save slots and metadata retrieval.
 */
public class SaveManager {
    public static void saveGame(GameState state) {
        saveGame(state, "savegame.dat");
    }

    public static void saveGame(GameState state, String filename) {
        try {
            FileHandle file = Gdx.files.local(filename);
            file.writeString(state.serialize(), false);
            Gdx.app.log("SaveManager", "Game saved successfully to: " + filename);
        } catch (Exception e) {
            Gdx.app.error("SaveManager", "Failed to save game: " + e.getMessage());
        }
    }

    public static GameState loadGame() {
        return loadGame("savegame.dat");
    }

    public static GameState loadGame(String filename) {
        GameState state = new GameState();
        try {
            FileHandle file = Gdx.files.local(filename);
            if (file.exists()) {
                String content = file.readString();
                state.deserialize(content);
                Gdx.app.log("SaveManager", "Game loaded successfully from: " + filename);
            } else {
                Gdx.app.log("SaveManager", "No save file found. Initializing new game state for: " + filename);
                state.reset();
            }
        } catch (Exception e) {
            Gdx.app.error("SaveManager", "Failed to load game: " + e.getMessage());
            state.reset();
        }
        return state;
    }

    public static String getLatestSaveFile() {
        String[] files = {"savegame.dat", "savegame_1.dat", "savegame_2.dat", "savegame_3.dat"};
        String latestFile = "savegame.dat";
        long latestTime = -1;

        for (String filename : files) {
            FileHandle file = Gdx.files.local(filename);
            if (file.exists()) {
                long time = file.lastModified();
                if (time > latestTime) {
                    latestTime = time;
                    latestFile = filename;
                }
            }
        }
        return latestFile;
    }

    public static String getSaveMetadata(String filename) {
        FileHandle file = Gdx.files.local(filename);
        if (!file.exists()) {
            return "Slot Kosong";
        }

        try {
            String content = file.readString();
            String[] lines = content.split("\n");
            String chapter = "PROLOGUE";
            int day = 30;
            int money = 100000;

            for (String line : lines) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String val = parts[1].trim();
                    if (key.equals("chapter")) {
                        chapter = val;
                    } else if (key.equals("day")) {
                        day = Integer.parseInt(val);
                    } else if (key.equals("money")) {
                        money = Integer.parseInt(val);
                    }
                }
            }

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
            String dateStr = sdf.format(new java.util.Date(file.lastModified()));

            String babName = chapter;
            if (chapter.equals("PROLOGUE")) babName = "Prolog";
            else if (chapter.equals("CHAPTER_1")) babName = "Bab 1";
            else if (chapter.equals("CHAPTER_2")) babName = "Bab 2";
            else if (chapter.equals("CHAPTER_3")) babName = "Bab 3";
            else if (chapter.equals("CHAPTER_4")) babName = "Bab 4";

            return babName + " - H-" + day + " (Rp" + money + ")\n" + dateStr;
        } catch (Exception e) {
            return "Data Rusak";
        }
    }
}
