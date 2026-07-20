package com.echosummer.game;

/**
 * Represents a choice the player can make during dialogue.
 */
public class Choice {
    public String text;
    public String nextNodeId;
    public ChoiceAction action;

    public Choice(String text, String nextNodeId) {
        this.text = text;
        this.nextNodeId = nextNodeId;
        this.action = null;
    }

    public Choice(String text, String nextNodeId, ChoiceAction action) {
        this.text = text;
        this.nextNodeId = nextNodeId;
        this.action = action;
    }

    public interface ChoiceAction {
        void execute(GameState state);
    }
}
