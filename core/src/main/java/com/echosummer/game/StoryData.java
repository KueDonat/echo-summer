package com.echosummer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.echosummer.game.ds.CustomHashTable;
import com.echosummer.game.ds.CustomTree;
import com.echosummer.game.ds.adt.IHashTable;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains the complete dialogue script, event triggers, and choices for Echo Summer.
 * Integrated with CustomHashTable (O(1) node index) and CustomTree (Decision Tree branching).
 */
public class StoryData {
    public static CustomHashTable<String, DialogueNode> buildStoryHashTable(
        final GameState state, 
        final Runnable onStartRhythmGame, 
        final Runnable onTriggerEnding
    ) {
        CustomHashTable<String, DialogueNode> nodes = new CustomHashTable<>(127);

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
                                if (s.day > 1) {
                                    s.day--;
                                } else {
                                    s.day = 1;
                                    s.chapter = "CHAPTER_4";
                                }
                                s.freeDayEventDone = false;
                                s.dialogueNodeId = "CH1_DAY_LOOP";
                            }
                        };
                    } else if ("CHECK_RECRUITMENT_ACTION".equals(actionType)) {
                        nodeAction = new DialogueNode.NodeAction() {
                            @Override
                            public void execute(GameState s) {
                                if (s.day28EventDone && s.day26EventDone && s.day24EventDone) {
                                    s.dialogueNodeId = "CHECK_CH1_COMPLETE";
                                } else {
                                    s.dialogueNodeId = "CH1_MORE_MEMBERS_LEFT";
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
                                s.ch2CompositionDone = true;
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
                dNode.background = nodeVal.getString("background", null);

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
            Gdx.app.error("StoryData", "Error parsing story.json: " + e.getMessage());
        }

        return nodes;
    }

    public static Map<String, DialogueNode> buildStory(
        final GameState state, 
        final Runnable onStartRhythmGame, 
        final Runnable onTriggerEnding
    ) {
        CustomHashTable<String, DialogueNode> ht = buildStoryHashTable(state, onStartRhythmGame, onTriggerEnding);
        Map<String, DialogueNode> map = new HashMap<>();
        for (String key : ht.keys()) {
            map.put(key, ht.get(key));
        }
        return map;
    }

    public static CustomTree<DialogueNode> buildStoryTree(CustomHashTable<String, DialogueNode> nodes) {
        DialogueNode rootNode = nodes.get("PROLOG_START");
        if (rootNode == null) return new CustomTree<>();

        CustomTree<DialogueNode> tree = new CustomTree<>(rootNode);
        CustomTree.TreeNode<DialogueNode> rootTreeNode = tree.getRoot();

        // Recursively or iteratively populate child branches
        populateTreeBranches(rootTreeNode, nodes, 0);
        return tree;
    }

    private static void populateTreeBranches(CustomTree.TreeNode<DialogueNode> parentTreeNode, CustomHashTable<String, DialogueNode> nodes, int depth) {
        if (depth > 50 || parentTreeNode == null) return;
        DialogueNode dNode = parentTreeNode.getData();
        if (dNode == null) return;

        if (dNode.choices != null) {
            for (Choice choice : dNode.choices) {
                if (choice.nextNodeId != null && nodes.containsKey(choice.nextNodeId)) {
                    DialogueNode childNode = nodes.get(choice.nextNodeId);
                    CustomTree.TreeNode<DialogueNode> childTreeNode = parentTreeNode.addChild(childNode);
                    populateTreeBranches(childTreeNode, nodes, depth + 1);
                }
            }
        } else if (dNode.nextId != null && nodes.containsKey(dNode.nextId)) {
            DialogueNode childNode = nodes.get(dNode.nextId);
            CustomTree.TreeNode<DialogueNode> childTreeNode = parentTreeNode.addChild(childNode);
            populateTreeBranches(childTreeNode, nodes, depth + 1);
        }
    }
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
            else if ("day28EventDone".equals(name)) s.day28EventDone = param.asBoolean();
            else if ("day26EventDone".equals(name)) s.day26EventDone = param.asBoolean();
            else if ("day24EventDone".equals(name)) s.day24EventDone = param.asBoolean();
            else if ("ch2CompositionDone".equals(name)) s.ch2CompositionDone = param.asBoolean();
            else if ("ch3AldoDone".equals(name)) s.ch3AldoDone = param.asBoolean();

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
