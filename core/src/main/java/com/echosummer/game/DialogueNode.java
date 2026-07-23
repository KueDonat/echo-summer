package com.echosummer.game;

/**
 * Represents a single node in the branching dialogue/story tree.
 */
public class DialogueNode {
    public String nodeId;
    public String speaker;
    public String text;
    public Choice[] choices;
    public String nextId;
    public NodeAction action;
    public String expression = null;
    public String background = null;

    // Cinematic Transition Metadata
    public boolean isTransition = false;
    public int transFromDay = -1;
    public int transToDay = -1;
    public String transChapterTitle = null;
    public String transChapterSubtitle = null;
    public String transNextState = null;
    public String transNextNodeId = null;

    public DialogueNode(String nodeId, String speaker, String text, String nextId) {
        this.nodeId = nodeId;
        this.speaker = speaker;
        this.text = text;
        this.nextId = nextId;
        this.choices = null;
        this.action = null;
    }

    public DialogueNode(String nodeId, String speaker, String text, String nextId, NodeAction action) {
        this.nodeId = nodeId;
        this.speaker = speaker;
        this.text = text;
        this.nextId = nextId;
        this.choices = null;
        this.action = action;
    }

    public DialogueNode(String nodeId, String speaker, String text, Choice[] choices) {
        this.nodeId = nodeId;
        this.speaker = speaker;
        this.text = text;
        this.choices = choices;
        this.nextId = null;
        this.action = null;
    }

    public DialogueNode(String nodeId, String speaker, String text, Choice[] choices, NodeAction action) {
        this.nodeId = nodeId;
        this.speaker = speaker;
        this.text = text;
        this.choices = choices;
        this.nextId = null;
        this.action = action;
    }

    public interface NodeAction {
        void execute(GameState state);
    }
}
