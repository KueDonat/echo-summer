package com.echosummer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class SettingsManager {
    private static final String PREFS_NAME = "EchoSummerSettings";
    
    private static Preferences getPrefs() {
        return Gdx.app.getPreferences(PREFS_NAME);
    }
    
    public static float getVolume() {
        return getPrefs().getFloat("volume", 0.5f);
    }
    
    public static void setVolume(float v) {
        getPrefs().putFloat("volume", Math.max(0f, Math.min(1f, v)));
        getPrefs().flush();
    }
    
    // 0 = Easy, 1 = Medium, 2 = Hard
    public static int getDifficulty() {
        return getPrefs().getInteger("difficulty", 1);
    }
    
    public static void setDifficulty(int d) {
        getPrefs().putInteger("difficulty", Math.max(0, Math.min(2, d)));
        getPrefs().flush();
    }
}
