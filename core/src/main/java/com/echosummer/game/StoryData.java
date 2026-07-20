package com.echosummer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains the complete dialogue script, event triggers, and choices for Echo Summer.
 * Refactored to load from assets/story.json.
 */
public class StoryData {
    public static Map<String, DialogueNode> buildStory(
        final GameState state, 
        final Runnable onStartRhythmGame, 
        final Runnable onTriggerEnding
    ) {
        Map<String, DialogueNode> nodes = new HashMap<>();

        try {
            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(Gdx.files.internal("story.json"));

            for (JsonValue nodeVal = root.child(); nodeVal != null; nodeVal = nodeVal.next()) {
                String nodeId = nodeVal.getString("nodeId");
                String speaker = nodeVal.getString("speaker", "");
                String text = nodeVal.getString("text", "");
                String nextId = nodeVal.getString("nextId", null);

                // Parse Choices if present
                Choice[] choices = null;
                if (nodeVal.has("choices")) {
                    JsonValue choicesVal = nodeVal.get("choices");
                    choices = new Choice[choicesVal.size];
                    int i = 0;
                    for (JsonValue choiceVal = choicesVal.child(); choiceVal != null; choiceVal = choiceVal.next()) {
                        String cText = choiceVal.getString("text");
                        String cNextId = choiceVal.getString("nextNodeId");

                        // Parse ChoiceAction
                        Choice.ChoiceAction cAction = null;
                        if (choiceVal.has("action")) {
                            final JsonValue actionVal = choiceVal.get("action");
                            cAction = new Choice.ChoiceAction() {
                                @Override
                                public void execute(GameState s) {
                                    applyStateChanges(s, actionVal);
                                }
                            };
                        }

                        choices[i++] = new Choice(cText, cNextId, cAction);
                    }
                }

                // Parse NodeAction
                DialogueNode.NodeAction nodeAction = null;
                if (nodeVal.has("action")) {
                    final JsonValue actionVal = nodeVal.get("action");
                    String actionType = actionVal.getString("type", null);

                    if ("START_RHYTHM_GAME".equals(actionType)) {
                        nodeAction = new DialogueNode.NodeAction() {
                            @Override
                            public void execute(GameState s) {
                                onStartRhythmGame.run();
                            }
                        };
                    } else if ("TRIGGER_ENDING".equals(actionType)) {
                        nodeAction = new DialogueNode.NodeAction() {
                            @Override
                            public void execute(GameState s) {
                                onTriggerEnding.run();
                            }
                        };
                    } else if ("CH1_DAY_NEXT_ACTION".equals(actionType)) {
                        nodeAction = new DialogueNode.NodeAction() {
                            @Override
                            public void execute(GameState s) {
                                s.day--;
                                if (s.day < 20) {
                                    s.chapter = "CHAPTER_2";
                                    s.day = 19;
                                    s.dialogueNodeId = "CH2_START";
                                } else {
                                    s.dialogueNodeId = "CH1_DAY_LOOP";
                                }
                            }
                        };
                    } else if ("CH2_TRANSITION_ACTION".equals(actionType)) {
                        nodeAction = new DialogueNode.NodeAction() {
                            @Override
                            public void execute(GameState s) {
                                boolean isPerfect = (s.lyrics.equals("romantis") && s.melody.equals("indie")) ||
                                                    (s.lyrics.equals("sedih") && s.melody.equals("slow")) ||
                                                    (s.lyrics.equals("semangat") && s.melody.equals("rock"));
                                if (isPerfect) {
                                    s.songUnlocked = true;
                                    s.songQuality = 100;
                                    s.confidence += 5;
                                } else {
                                    s.songUnlocked = false;
                                    s.songQuality = 60;
                                }
                                s.chapter = "CHAPTER_3";
                                s.day = 9;
                            }
                        };
                    } else {
                        // Standard state changes
                        nodeAction = new DialogueNode.NodeAction() {
                            @Override
                            public void execute(GameState s) {
                                applyStateChanges(s, actionVal);
                            }
                        };
                    }
                }

                DialogueNode dNode;
                if (choices != null) {
                    dNode = new DialogueNode(nodeId, speaker, text, choices, nodeAction);
                } else {
                    dNode = new DialogueNode(nodeId, speaker, text, nextId, nodeAction);
                }

                dNode.expression = nodeVal.getString("expression", null);

                // Parse Transition Metadata if present
                if (nodeVal.has("transition")) {
                    JsonValue transVal = nodeVal.get("transition");
                    dNode.isTransition = true;
                    dNode.transFromDay = transVal.getInt("fromDay", -1);
                    dNode.transToDay = transVal.getInt("toDay", -1);
                    dNode.transChapterTitle = transVal.getString("chapterTitle", null);
                    dNode.transChapterSubtitle = transVal.getString("chapterSubtitle", null);
                    dNode.transNextState = transVal.getString("nextState", null);
                    dNode.transNextNodeId = transVal.getString("nextNodeId", null);
                }

                nodes.put(nodeId, dNode);
            }
        } catch (Exception e) {
            Gdx.app.error("StoryData", "Error reading or parsing story.json: " + e.getMessage());
            e.printStackTrace();
        }

        return nodes;
    }

    private static void applyStateChanges(GameState s, JsonValue actionVal) {
        if (actionVal == null) return;

        for (JsonValue param = actionVal.child(); param != null; param = param.next()) {
            String name = param.name();

            if ("chapter".equals(name)) s.chapter = param.asString();
            else if ("lyrics".equals(name)) s.lyrics = param.asString();
            else if ("melody".equals(name)) s.melody = param.asString();
            else if ("mental".equals(name)) s.mental = param.asString();
            else if ("guitarStringBroken".equals(name)) s.guitarStringBroken = param.asBoolean();

            else if ("day_set".equals(name)) s.day = param.asInt();
            else if ("money_add".equals(name)) s.money += param.asInt();
            else if ("claraRel_add".equals(name)) s.claraRel += param.asInt();
            else if ("raniaRel_add".equals(name)) s.raniaRel += param.asInt();
            else if ("bagasRel_add".equals(name)) s.bagasRel += param.asInt();
            else if ("sherlyRel_add".equals(name)) s.sherlyRel += param.asInt();
            else if ("creativity_add".equals(name)) s.creativity += param.asInt();
            else if ("confidence_add".equals(name)) s.confidence += param.asInt();
            else if ("patience_add".equals(name)) s.patience += param.asInt();
        }
    }
}
