package com.echosummer.game;

import com.badlogic.gdx.Gdx;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds all mutable variables of the active playthrough.
 * Can serialize to/deserialize from a simple key-value string.
 */
public class GameState {
    public String chapter = "PROLOGUE";
    public int day = 30;
    public int money = 100000;
    
    public String mental = "LOW";
    public int creativity = 10;
    public int confidence = 10;
    public int patience = 10;
    
    public int claraRel = 0;
    public int raniaRel = 0;
    public int bagasRel = 0;
    public int sherlyRel = 0;
    
    public String lyrics = "";
    public String melody = "";
    public boolean songUnlocked = false;
    public int songQuality = 0;
    
    public String dialogueNodeId = "PROLOG_START";
    
    // Performance state
    public int performanceScore = 0;
    public boolean guitarStringBroken = false;
    public boolean claraSang = false;

    // Event completion state
    public boolean day28EventDone = false;
    public boolean day26EventDone = false;
    public boolean day24EventDone = false;
    public boolean ch2CompositionDone = false;
    public boolean ch3AldoDone = false;
    public boolean ch2ChatRead = false;
    public boolean freeDayEventDone = false;
    public int claraChatDayReplied = -1;
    public int claraChatChoiceIndex = 0;

    // Resuming state
    public String currentZone = "KOST";
    public float rakshaX = 100f;
    public String gameplayState = "DIALOGUE_STATE";
    
    public void reset() {
        chapter = "PROLOGUE";
        day = 30;
        money = 100000;
        mental = "LOW";
        creativity = 10;
        confidence = 10;
        patience = 10;
        claraRel = 0;
        raniaRel = 0;
        bagasRel = 0;
        sherlyRel = 0;
        lyrics = "";
        melody = "";
        songUnlocked = false;
        songQuality = 0;
        dialogueNodeId = "PROLOG_START";
        performanceScore = 0;
        guitarStringBroken = false;
        claraSang = false;
        day28EventDone = false;
        day26EventDone = false;
        day24EventDone = false;
        ch2CompositionDone = false;
        ch3AldoDone = false;
        ch2ChatRead = false;
        freeDayEventDone = false;
        currentZone = "KOST";
        rakshaX = 100f;
        gameplayState = "DIALOGUE_STATE";
    }
    
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("chapter=").append(chapter).append("\n");
        sb.append("day=").append(day).append("\n");
        sb.append("money=").append(money).append("\n");
        sb.append("mental=").append(mental).append("\n");
        sb.append("creativity=").append(creativity).append("\n");
        sb.append("confidence=").append(confidence).append("\n");
        sb.append("patience=").append(patience).append("\n");
        sb.append("claraRel=").append(claraRel).append("\n");
        sb.append("raniaRel=").append(raniaRel).append("\n");
        sb.append("bagasRel=").append(bagasRel).append("\n");
        sb.append("sherlyRel=").append(sherlyRel).append("\n");
        sb.append("lyrics=").append(lyrics).append("\n");
        sb.append("melody=").append(melody).append("\n");
        sb.append("songUnlocked=").append(songUnlocked).append("\n");
        sb.append("songQuality=").append(songQuality).append("\n");
        sb.append("dialogueNodeId=").append(dialogueNodeId).append("\n");
        sb.append("performanceScore=").append(performanceScore).append("\n");
        sb.append("guitarStringBroken=").append(guitarStringBroken).append("\n");
        sb.append("claraSang=").append(claraSang).append("\n");
        sb.append("day28EventDone=").append(day28EventDone).append("\n");
        sb.append("day26EventDone=").append(day26EventDone).append("\n");
        sb.append("day24EventDone=").append(day24EventDone).append("\n");
        sb.append("ch2CompositionDone=").append(ch2CompositionDone).append("\n");
        sb.append("ch3AldoDone=").append(ch3AldoDone).append("\n");
        sb.append("ch2ChatRead=").append(ch2ChatRead).append("\n");
        sb.append("freeDayEventDone=").append(freeDayEventDone).append("\n");
        sb.append("claraChatDayReplied=").append(claraChatDayReplied).append("\n");
        sb.append("claraChatChoiceIndex=").append(claraChatChoiceIndex).append("\n");
        sb.append("currentZone=").append(currentZone).append("\n");
        sb.append("rakshaX=").append(rakshaX).append("\n");
        sb.append("gameplayState=").append(gameplayState).append("\n");
        return sb.toString();
    }
    
    public void deserialize(String data) {
        if (data == null || data.trim().isEmpty()) return;
        String[] lines = data.split("\n");
        Map<String, String> map = new HashMap<>();
        for (String line : lines) {
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                map.put(parts[0].trim(), parts[1].trim());
            }
        }
        
        try {
            if (map.containsKey("chapter")) chapter = map.get("chapter");
            if (map.containsKey("day")) {
                int parsedDay = Integer.parseInt(map.get("day"));
                day = parsedDay > 0 ? parsedDay : 30;
            }
            if (map.containsKey("money")) money = Integer.parseInt(map.get("money"));
            if (map.containsKey("mental")) mental = map.get("mental");
            if (map.containsKey("creativity")) creativity = Integer.parseInt(map.get("creativity"));
            if (map.containsKey("confidence")) confidence = Integer.parseInt(map.get("confidence"));
            if (map.containsKey("patience")) patience = Integer.parseInt(map.get("patience"));
            if (map.containsKey("claraRel")) claraRel = Integer.parseInt(map.get("claraRel"));
            if (map.containsKey("raniaRel")) raniaRel = Integer.parseInt(map.get("raniaRel"));
            if (map.containsKey("bagasRel")) bagasRel = Integer.parseInt(map.get("bagasRel"));
            if (map.containsKey("sherlyRel")) sherlyRel = Integer.parseInt(map.get("sherlyRel"));
            if (map.containsKey("lyrics")) lyrics = map.get("lyrics");
            if (map.containsKey("melody")) melody = map.get("melody");
            if (map.containsKey("songUnlocked")) songUnlocked = Boolean.parseBoolean(map.get("songUnlocked"));
            if (map.containsKey("songQuality")) songQuality = Integer.parseInt(map.get("songQuality"));
            if (map.containsKey("dialogueNodeId")) dialogueNodeId = map.get("dialogueNodeId");
            if (map.containsKey("performanceScore")) performanceScore = Integer.parseInt(map.get("performanceScore"));
            if (map.containsKey("guitarStringBroken")) guitarStringBroken = Boolean.parseBoolean(map.get("guitarStringBroken"));
            if (map.containsKey("claraSang")) claraSang = Boolean.parseBoolean(map.get("claraSang"));
            if (map.containsKey("day28EventDone")) day28EventDone = Boolean.parseBoolean(map.get("day28EventDone"));
            if (map.containsKey("day26EventDone")) day26EventDone = Boolean.parseBoolean(map.get("day26EventDone"));
            if (map.containsKey("day24EventDone")) day24EventDone = Boolean.parseBoolean(map.get("day24EventDone"));
            if (map.containsKey("ch2CompositionDone")) ch2CompositionDone = Boolean.parseBoolean(map.get("ch2CompositionDone"));
            if (map.containsKey("ch3AldoDone")) ch3AldoDone = Boolean.parseBoolean(map.get("ch3AldoDone"));
            if (map.containsKey("ch2ChatRead")) ch2ChatRead = Boolean.parseBoolean(map.get("ch2ChatRead"));
            if (map.containsKey("freeDayEventDone")) freeDayEventDone = Boolean.parseBoolean(map.get("freeDayEventDone"));
            if (map.containsKey("claraChatDayReplied")) claraChatDayReplied = Integer.parseInt(map.get("claraChatDayReplied"));
            if (map.containsKey("claraChatChoiceIndex")) claraChatChoiceIndex = Integer.parseInt(map.get("claraChatChoiceIndex"));
            if (map.containsKey("currentZone")) currentZone = map.get("currentZone");
            if (map.containsKey("rakshaX")) rakshaX = Float.parseFloat(map.get("rakshaX"));
            if (map.containsKey("gameplayState")) gameplayState = map.get("gameplayState");
        } catch (Exception e) {
            Gdx.app.error("GameState", "Error parsing save state: " + e.getMessage());
        }
    }
}
