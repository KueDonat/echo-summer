package com.echosummer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.math.Vector3;
import java.util.Map;
import java.util.HashMap;

/**
 * Core gameplay screen matching Afterlove EP's architecture.
 * Manages the Calendar System, integrated Dialogue VN, Exploration side-scrolling,
 * and the 4-lane Rhythm Game Engine using a state-machine flow.
 */
public class GameplayScreen implements Screen, InputProcessor {
    public enum GameplayState {
        EXPLORATION_STATE,
        DIALOGUE_STATE,
        RHYTHM_STATE,
        CINEMATIC_TRANSITION_STATE,
        PAUSED_STATE,
        PHONE_STATE
    }

    private int phoneScreen = 0; // 0=contacts, 1=Clara, 2=Bagas, 3=Rania, 4=Sherly

    private final Main game;
    private final boolean isLoadGame;
    private GameplayState state;
    private GameplayState previousState;

    // Viewport & Camera for virtual resolution (1280x720)
    private OrthographicCamera camera;
    private Viewport viewport;
    private final float VIRTUAL_WIDTH = 1280f;
    private final float VIRTUAL_HEIGHT = 720f;

    // Renderers & Fonts
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private BitmapFont dialogueFont;
    private BitmapFont choiceFont;

    // Active background texture
    private Texture currentBackground;

    // Calendar & Time system state
    private int remainingDays = 30;
    private String timeOfDay = "SORE";

    // Story data and state
    private GameState gameState;
    private Map<String, DialogueNode> storyNodes;
    private DialogueNode currentNode;

    // Shared backgrounds loaded in memory
    private Array<Texture> prologTextures;
    private Texture studioTexture;
    private Texture skyTexture;
    private Texture kamarKostTexture;
    private Texture kostOutsideTexture;
    private Texture jalanRayaTexture;

    // EXPLORATION_STATE: Sidescrolling character controller
    private Array<Texture> walkTextures;
    private Array<Texture> idleTextures;
    private Array<Texture> claraIdleTextures;

    private Animation<TextureRegion> walkAnimation;
    private Animation<TextureRegion> idleAnimation;
    private Animation<TextureRegion> claraIdleAnimation;

    private float rakshaX = 100f;
    private float rakshaY = 120f;
    private float animationTime = 0f;
    private float claraAnimationTime = 0f;
    private boolean rakshaFacingRight = true;

    // Exploration zone system (each zone is a separate room with its own background)
    private enum ExplorationZone { KOST, KOST_OUTSIDE, WARKOP, KAMPUS }
    private ExplorationZone currentZone = ExplorationZone.KOST;

    // Zone transition loading screen state
    private boolean isZoneTransitioning = false;
    private float zoneTransAlpha = 0f;       // 0=transparent, 1=fully black
    private boolean zoneTransFadingOut = true; // true=fade to black, false=fade in
    private ExplorationZone pendingZone = null;
    private String zoneTransLabel = "";
    private float zoneTransLabelAlpha = 0f;
    private float zoneTransTimer = 0f;

    // Interaction zone flags (set each frame, used to drive E-key prompts)
    private boolean nearBed = false;
    private boolean nearClara = false;
    private boolean nearKostGate = false;
    // Context tracking for rhythm game completion
    private boolean rhythmFromGuitarPractice = false;

    // DIALOGUE_STATE: Typewriter dialogues
    private String typedText = "";
    private float typewriterTimer = 0f;
    private final float typewriterCharDelay = 0.02f;
    private int typedCharCount = 0;
    private boolean isDialogueFinished = false;
    private float nextIndicatorTimer = 0f;

    // Preprocessed dialogue states
    private String currentSpeakerName = "";
    private String currentDialogueText = "";
    private String rakshaExpression = "biasa aja";
    private String claraExpression = "biasa aja";
    private String sherlyExpression = "biasa aja";
    private String activeRightCharacter = "Clara";
    private Map<String, Map<String, Texture>> expressionTextures;

    // RHYTHM_STATE: Engine
    private RhythmGame rhythmGame;
    private Music rhythmMusic;

    // Cinematic Transition Variables
    private int transFromDay;
    private int transToDay;
    private String transNarrativeText;
    private String transChapterTitle;
    private String transChapterSubtitle;
    private GameplayState transNextState;
    private String transNextNode;
    private int transPhase;
    private float transTime;
    private float transAlpha;
    private float transNumberDisplay;

    public GameplayScreen(Main game, boolean isLoadGame) {
        this.game = game;
        this.isLoadGame = isLoadGame;
    }

    @Override
    public void show() {
        shapeRenderer = new ShapeRenderer();
        state = GameplayState.DIALOGUE_STATE;

        // Setup Virtual Camera and Viewport
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        viewport.apply();
        camera.position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0);

        // Initialize Input multiplexer
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(this);
        Gdx.input.setInputProcessor(multiplexer);

        // Ensure menu music is stopped
        game.stopMenuMusic();

        // Load custom typography fonts
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("NotoSans-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        
        parameter.size = 18;
        parameter.color = Color.WHITE;
        font = generator.generateFont(parameter);

        parameter.size = 22;
        parameter.borderWidth = 1.2f;
        parameter.borderColor = new Color(0f, 0f, 0.05f, 0.8f);
        dialogueFont = generator.generateFont(parameter);

        parameter.size = 20;
        parameter.borderWidth = 0.5f;
        parameter.borderColor = new Color(0f, 0f, 0f, 0.5f);
        choiceFont = generator.generateFont(parameter);

        generator.dispose();

        // Load shared backgrounds
        prologTextures = new Array<>();
        for (int i = 1; i <= 5; i++) {
            Texture tex = new Texture(Gdx.files.internal("prolog/" + i + ".png"));
            tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            prologTextures.add(tex);
        }
        studioTexture = new Texture(Gdx.files.internal("background/background_studio.jpg"));
        studioTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        skyTexture = new Texture(Gdx.files.internal("background.png"));
        skyTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        kamarKostTexture = new Texture(Gdx.files.internal("background/background_kamar_kost.png"));
        kamarKostTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        kostOutsideTexture = new Texture(Gdx.files.internal("background/background_kost_outside.jpg"));
        kostOutsideTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        jalanRayaTexture = new Texture(Gdx.files.internal("background/background_jalan_raya.png"));
        jalanRayaTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        // Load GameState
        if (isLoadGame) {
            gameState = SaveManager.loadGame();
        } else {
            gameState = new GameState();
            gameState.reset();
            SaveManager.saveGame(gameState);
        }

        // Initialize story graph
        storyNodes = StoryData.buildStory(gameState, new Runnable() {
            @Override
            public void run() {
                // onStartRhythmGame
                state = GameplayState.RHYTHM_STATE;
                rhythmMusic = Gdx.audio.newMusic(Gdx.files.internal("backsound_main_menu.mp3"));
                rhythmMusic.setVolume(0.5f);
                rhythmGame.start(rhythmMusic);
            }
        }, new Runnable() {
            @Override
            public void run() {
                // onTriggerEnding
                if (gameState.claraSang && gameState.performanceScore >= 800) {
                    loadNode("END_TRUE_1");
                } else if (gameState.performanceScore >= 1200) {
                    loadNode("END_TRUE_1");
                } else if (gameState.bagasRel >= 4 && gameState.performanceScore >= 400) {
                    loadNode("END_COMEDY_1");
                } else {
                    loadNode("END_WORST_1");
                }
            }
        });

        // Load Character animation frames (IDLE_RAKSHA & WALKING_RAKSHA)
        idleTextures = new Array<>();
        TextureRegion[] idleFrames = new TextureRegion[8];
        for (int i = 1; i <= 8; i++) {
            String path = String.format("sprite/IDLE_RAKSHA/pose_%02d.png", i);
            Texture tex = new Texture(Gdx.files.internal(path));
            tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            idleTextures.add(tex);
            idleFrames[i - 1] = new TextureRegion(tex);
        }
        idleAnimation = new Animation<>(0.15f, idleFrames);

        walkTextures = new Array<>();
        TextureRegion[] walkFrames = new TextureRegion[16];
        for (int i = 1; i <= 16; i++) {
            String path = String.format("sprite/WALKING_RAKSHA/walk_%02d.png", i);
            Texture tex = new Texture(Gdx.files.internal(path));
            tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            walkTextures.add(tex);
            walkFrames[i - 1] = new TextureRegion(tex);
        }
        walkAnimation = new Animation<>(0.11f, walkFrames);

        // Load Clara animation frames (IDLE_CLARA)
        claraIdleTextures = new Array<>();
        TextureRegion[] claraFrames = new TextureRegion[16];
        for (int i = 1; i <= 16; i++) {
            String path = String.format("sprite/IDLE_CLARA/girl_%02d.png", i);
            Texture tex = new Texture(Gdx.files.internal(path));
            tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            claraIdleTextures.add(tex);
            claraFrames[i - 1] = new TextureRegion(tex);
        }
        claraIdleAnimation = new Animation<>(0.12f, claraFrames);

        // Initialize Rhythm engine
        rhythmGame = new RhythmGame();

        // Load expression textures
        loadExpressionTextures();

        // Load starting node
        loadNode(gameState.dialogueNodeId);
    }

    private void loadNode(String nodeId) {
        if (nodeId == null || !storyNodes.containsKey(nodeId)) {
            Gdx.app.error("GameplayScreen", "Dialogue node not found: " + nodeId);
            game.setScreen(new MainMenuScreen(game));
            return;
        }

        gameState.dialogueNodeId = nodeId;
        currentNode = storyNodes.get(nodeId);

        // Execute action if present
        if (currentNode.action != null) {
            currentNode.action.execute(gameState);
        }

        // Intercept for cinematic transitions dynamically from node metadata
        if (currentNode.isTransition) {
            int fromDay = currentNode.transFromDay;
            int toDay = currentNode.transToDay;
            if (fromDay == -1 && toDay == -1) {
                fromDay = gameState.day + 1;
                toDay = gameState.day;
            }
            
            // Map String to GameplayState
            GameplayState nextState = GameplayState.EXPLORATION_STATE;
            if ("DIALOGUE".equalsIgnoreCase(currentNode.transNextState)) {
                nextState = GameplayState.DIALOGUE_STATE;
            } else if ("RHYTHM".equalsIgnoreCase(currentNode.transNextState)) {
                nextState = GameplayState.RHYTHM_STATE;
            }
            
            startCinematicTransition(
                fromDay, 
                toDay, 
                currentNode.text, 
                currentNode.transChapterTitle, 
                currentNode.transChapterSubtitle, 
                nextState, 
                currentNode.transNextNodeId
            );
            return;
        }

        // Sync HUD properties
        remainingDays = gameState.day;
        updateTimeOfDay();

        // Determine background
        if (nodeId.equals("PROLOG_START")) {
            currentBackground = prologTextures.get(0);
        } else if (nodeId.equals("PROLOG_1")) {
            currentBackground = prologTextures.get(1);
        } else if (nodeId.equals("PROLOG_2")) {
            currentBackground = prologTextures.get(2);
        } else if (nodeId.equals("PROLOG_3") || nodeId.equals("PROLOG_4") || nodeId.equals("PROLOG_5") || nodeId.equals("PROLOG_6") || nodeId.equals("PROLOG_7")) {
            currentBackground = prologTextures.get(3);
        } else if (nodeId.startsWith("PROLOG_")) {
            currentBackground = prologTextures.get(4);
        } else if (nodeId.equals("CH1_INTRO") || nodeId.equals("CH1_DAY_NEXT")) {
            currentBackground = kostOutsideTexture;
        } else if (nodeId.equals("CH1_CHOICE_C_RESULT") || nodeId.equals("CH1_PRACTICE_END") || nodeId.equals("CH3_POST_RESULT_1") || nodeId.equals("CH3_ALDO_RESULT_1")) {
            currentBackground = kamarKostTexture;
        } else if (nodeId.startsWith("CH3_BUS_")) {
            currentBackground = jalanRayaTexture;
        } else if (nodeId.startsWith("END_") || nodeId.startsWith("CREDITS_") || nodeId.equals("GAME_OVER")) {
            currentBackground = skyTexture;
        } else {
            currentBackground = studioTexture;
        }

        if (currentBackground == kamarKostTexture || currentBackground == kostOutsideTexture) {
            activeRightCharacter = "None";
        }

        // Handle states
        if (nodeId.equals("PROLOG_START")) {
            state = GameplayState.DIALOGUE_STATE;
        } else if (nodeId.equals("CH1_INTRO")) {
            state = GameplayState.DIALOGUE_STATE;
        } else if (nodeId.equals("CH1_DAY_LOOP")) {
            if (gameState.day < 20) {
                loadNode("CH2_START");
                return;
            }
            state = GameplayState.EXPLORATION_STATE;
            currentZone = ExplorationZone.KOST; // always start fresh day at kost
            rakshaX = 100f;
            rakshaFacingRight = true;
            isZoneTransitioning = false;
            currentNode.text = "Hari ke-" + gameState.day + " menjelang festival.\nSisa uang: Rp" + gameState.money + ".\nBagaimana aku menghabiskan hari ini?";
        } else {
            state = GameplayState.DIALOGUE_STATE;
        }

        // Separate speaker name and process dialogue text before typewriter starts
        String rawText = getProcessedNodeText();
        String speaker = currentNode.speaker;
        String text = rawText;

        if (speaker == null || speaker.isEmpty()) {
            if (rawText.contains(":")) {
                int colonIdx = rawText.indexOf(":");
                speaker = rawText.substring(0, colonIdx).trim();
                text = rawText.substring(colonIdx + 1).trim();
            }
        }

        // Strip quotes from dialogue text safely
        if (text.startsWith("\"") && text.endsWith("\"") && text.length() >= 2) {
            text = text.substring(1, text.length() - 1);
        }

        currentSpeakerName = speaker;
        currentDialogueText = text;

        updateDialogueExpressions(currentSpeakerName, currentDialogueText);

        // Auto typewriter initialization
        startTypewriter(currentDialogueText);

        // Autosave at transitions
        if (nodeId.endsWith("_TRANSITION") || nodeId.equals("CH1_DAY_NEXT") || nodeId.equals("CH1_INTRO") || 
            nodeId.equals("CH2_START") || nodeId.equals("CH3_START") || nodeId.equals("CH4_START")) {
            SaveManager.saveGame(gameState);
        }
    }

    private String getProcessedNodeText() {
        if (currentNode == null) return "";
        String text = currentNode.text;

        if (currentNode.nodeId.equals("CH2_RESULT")) {
            boolean isPerfect = (gameState.lyrics.equals("romantis") && gameState.melody.equals("indie")) ||
                                (gameState.lyrics.equals("sedih") && gameState.melody.equals("slow")) ||
                                (gameState.lyrics.equals("semangat") && gameState.melody.equals("rock"));
            if (isPerfect) {
                text = "Raksha: \"Selesai! Lirik " + gameState.lyrics + " dipadukan dengan melodi " + gameState.melody + " bener-bener harmonis.\"\n\nClara tersenyum puas mendengarnya. \"Lagu ini sempurna, Raka. Kita pasti bisa!\"";
            } else {
                text = "Raksha: \"Hah... Lirik " + gameState.lyrics + " dipadukan dengan melodi " + gameState.melody + " rasanya agak kurang pas.\"\n\nClara melipat tangannya, menghela napas. \"Agak canggung sih, tapi ya sudahlah, kita gak punya banyak waktu lagi.\"";
            }
        } else if (currentNode.nodeId.equals("CH4_STRING_RESULT_3")) {
            if (gameState.claraRel >= 8) {
                text = "Raksha: \"Clara! Bernyanyilah bersamaku!\"\n\nClara menatapku terkejut, namun kemudian mengangguk mantap. Dia mendekati mikrofon dan mulai menyanyi. Suara kami berpadu indah, membuat penonton bersorak kagum!";
            } else {
                text = "Raksha: \"Clara! Bernyanyilah bersamaku!\"\n\nClara menggeleng panik. \"Gue gak bisa, Raka! Gue gak pernah nyanyi di depan orang banyak!\"\n\nSuasana panggung menjadi sangat canggung dan tempo berantakan...";
            }
        }
        return text;
    }

    private void updateTimeOfDay() {
        if (gameState == null) return;
        if (gameState.chapter.equals("PROLOGUE")) {
            timeOfDay = "SORE";
        } else if (gameState.chapter.equals("CHAPTER_1")) {
            timeOfDay = "SIANG";
        } else if (gameState.chapter.equals("CHAPTER_2")) {
            timeOfDay = "SORE";
        } else if (gameState.chapter.equals("CHAPTER_3")) {
            timeOfDay = "SORE";
        } else if (gameState.chapter.equals("CHAPTER_4")) {
            timeOfDay = "MALAM";
        } else {
            timeOfDay = "PAGI";
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.03f, 0.05f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        animationTime += delta;
        claraAnimationTime += delta;

        // Apply Viewport Camera projection
        camera.update();
        game.getBatch().setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        GameplayState drawState = state;
        if (state == GameplayState.PAUSED_STATE) {
            drawState = previousState;
        }
        
        switch (drawState) {
            case EXPLORATION_STATE:
                if (state != GameplayState.PAUSED_STATE) updateExploration(delta);
                renderExploration(delta);
                break;
            case DIALOGUE_STATE:
                if (state != GameplayState.PAUSED_STATE) updateDialogue(delta);
                renderDialogue(delta);
                break;
            case RHYTHM_STATE:
                if (state != GameplayState.PAUSED_STATE) updateRhythm(delta);
                renderRhythm(delta);
                break;
            case CINEMATIC_TRANSITION_STATE:
                if (state != GameplayState.PAUSED_STATE && state != GameplayState.PHONE_STATE) updateCinematicTransition(delta);
                renderCinematicTransition(delta);
                break;
        }

        // 4. Paused / Phone Overlays
        if (state == GameplayState.PAUSED_STATE) {
            renderPauseMenu();
        } else if (state == GameplayState.PHONE_STATE) {
            renderPhoneMenu();
        }
    }
    
    private void renderPauseMenu() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0f, 0f, 0f, 0.7f));
        shapeRenderer.rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        
        float menuW = 400f;
        float menuH = 450f;
        float mx = (VIRTUAL_WIDTH - menuW) / 2f;
        float my = (VIRTUAL_HEIGHT - menuH) / 2f;
        
        shapeRenderer.setColor(new Color(0.1f, 0.12f, 0.18f, 1f));
        shapeRenderer.rect(mx, my, menuW, menuH);
        
        // Buttons
        float btnW = 200f;
        float btnH = 50f;
        float bx = (VIRTUAL_WIDTH - btnW) / 2f;
        
        shapeRenderer.setColor(Color.LIGHT_GRAY);
        // Resume & Exit
        shapeRenderer.rect(bx, my + 350, btnW, btnH); // Resume
        shapeRenderer.rect(bx, my + 40,  btnW, btnH); // Exit
        
        // Volume
        shapeRenderer.rect(bx - 60, my + 260, 40, 40); // Vol -
        shapeRenderer.rect(bx + btnW + 20, my + 260, 40, 40); // Vol +
        
        // Difficulty
        int diff = SettingsManager.getDifficulty();
        shapeRenderer.setColor(diff == 0 ? Color.YELLOW : Color.LIGHT_GRAY);
        shapeRenderer.rect(mx + 20, my + 150, 100, 40); // Easy
        
        shapeRenderer.setColor(diff == 1 ? Color.YELLOW : Color.LIGHT_GRAY);
        shapeRenderer.rect(mx + 150, my + 150, 100, 40); // Medium
        
        shapeRenderer.setColor(diff == 2 ? Color.YELLOW : Color.LIGHT_GRAY);
        shapeRenderer.rect(mx + 280, my + 150, 100, 40); // Hard
        
        shapeRenderer.end();
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(mx, my, menuW, menuH);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        
        game.getBatch().begin();
        choiceFont.setColor(Color.WHITE);
        choiceFont.draw(game.getBatch(), "PAUSED", mx, my + menuH - 20, menuW, Align.center, false);
        
        choiceFont.setColor(Color.BLACK);
        choiceFont.draw(game.getBatch(), "Resume", bx, my + 350 + 35, btnW, Align.center, false);
        choiceFont.draw(game.getBatch(), "Exit to Menu", bx, my + 40 + 35, btnW, Align.center, false);
        
        choiceFont.draw(game.getBatch(), "-", bx - 60, my + 260 + 28, 40, Align.center, false);
        choiceFont.draw(game.getBatch(), "+", bx + btnW + 20, my + 260 + 28, 40, Align.center, false);
        
        choiceFont.draw(game.getBatch(), "Easy", mx + 20, my + 150 + 28, 100, Align.center, false);
        choiceFont.draw(game.getBatch(), "Medium", mx + 150, my + 150 + 28, 100, Align.center, false);
        choiceFont.draw(game.getBatch(), "Hard", mx + 280, my + 150 + 28, 100, Align.center, false);
        
        choiceFont.setColor(Color.WHITE);
        choiceFont.draw(game.getBatch(), "Volume: " + Math.round(SettingsManager.getVolume() * 100f) + "%", bx, my + 260 + 28, btnW, Align.center, false);
        choiceFont.draw(game.getBatch(), "Difficulty", mx, my + 150 + 70, menuW, Align.center, false);
        
        game.getBatch().end();
    }

    private void renderPhoneMenu() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0f, 0f, 0f, 0.6f));
        shapeRenderer.rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        
        float phoneW = 350f;
        float phoneH = 600f;
        float px = (VIRTUAL_WIDTH - phoneW) / 2f;
        float py = (VIRTUAL_HEIGHT - phoneH) / 2f;
        
        // Phone body
        shapeRenderer.setColor(new Color(0.9f, 0.9f, 0.9f, 1f));
        shapeRenderer.rect(px, py, phoneW, phoneH);
        
        // Phone screen
        shapeRenderer.setColor(new Color(0.1f, 0.1f, 0.1f, 1f));
        shapeRenderer.rect(px + 10, py + 10, phoneW - 20, phoneH - 20);
        
        // Header
        shapeRenderer.setColor(new Color(0.2f, 0.4f, 0.8f, 1f));
        shapeRenderer.rect(px + 10, py + phoneH - 70, phoneW - 20, 60);
        
        // Items
        if (phoneScreen == 0) {
            // Contacts
            shapeRenderer.setColor(new Color(0.2f, 0.2f, 0.2f, 1f));
            for (int i = 0; i < 4; i++) {
                shapeRenderer.rect(px + 20, py + phoneH - 140 - (i * 70), phoneW - 40, 60);
            }
        } else {
            // Chat options
            shapeRenderer.setColor(new Color(0.15f, 0.5f, 0.15f, 1f));
            shapeRenderer.rect(px + 20, py + 90, phoneW - 40, 50); // Latihan
            if (phoneScreen == 1) { // Clara
                shapeRenderer.setColor(new Color(0.6f, 0.15f, 0.3f, 1f));
                shapeRenderer.rect(px + 20, py + 30, phoneW - 40, 50); // Date
            }
            // Back button
            shapeRenderer.setColor(Color.DARK_GRAY);
            shapeRenderer.rect(px + 20, py + phoneH - 120, 80, 40);
        }
        
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        
        game.getBatch().begin();
        if (phoneScreen == 0) {
            choiceFont.setColor(Color.WHITE);
            choiceFont.draw(game.getBatch(), "Contacts", px, py + phoneH - 30, phoneW, Align.center, false);
            
            choiceFont.draw(game.getBatch(), "Clara", px + 40, py + phoneH - 100);
            choiceFont.draw(game.getBatch(), "Bagas", px + 40, py + phoneH - 170);
            choiceFont.draw(game.getBatch(), "Rania", px + 40, py + phoneH - 240);
            choiceFont.draw(game.getBatch(), "Sherly", px + 40, py + phoneH - 310);
        } else {
            String name = phoneScreen == 1 ? "Clara" : (phoneScreen == 2 ? "Bagas" : (phoneScreen == 3 ? "Rania" : "Sherly"));
            choiceFont.setColor(Color.WHITE);
            choiceFont.draw(game.getBatch(), name, px, py + phoneH - 30, phoneW, Align.center, false);
            
            choiceFont.draw(game.getBatch(), "< Back", px + 30, py + phoneH - 95);
            
            choiceFont.draw(game.getBatch(), "Ajak Latihan (-1 Hari)", px, py + 125, phoneW, Align.center, false);
            if (phoneScreen == 1) {
                choiceFont.draw(game.getBatch(), "Ajak Date Personal", px, py + 65, phoneW, Align.center, false);
            }
        }
        game.getBatch().end();
    }

    // ==========================================
    // STATE: EXPLORATION_STATE
    // ==========================================

    /** Trigger a zone transition: fade to black, switch zone, fade back in */
    private void startZoneTransition(ExplorationZone nextZone, String label) {
        if (isZoneTransitioning) return;
        isZoneTransitioning = true;
        pendingZone = nextZone;
        zoneTransLabel = label;
        zoneTransAlpha = 0f;
        zoneTransLabelAlpha = 0f;
        zoneTransFadingOut = true;
        zoneTransTimer = 0f;
    }

    private void updateExploration(float delta) {
        // If a zone transition is in progress, update it instead
        if (isZoneTransitioning) {
            updateZoneTransition(delta);
            return;
        }

        float moveSpeed = 200f;
        boolean movingLeft  = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean movingRight = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);

        if (movingLeft) {
            rakshaX -= moveSpeed * delta;
            rakshaFacingRight = false;
        } else if (movingRight) {
            rakshaX += moveSpeed * delta;
            rakshaFacingRight = true;
        }

        // Update interaction zone flags each frame
        nearBed   = (currentZone == ExplorationZone.KOST)   && rakshaX >= 820f && rakshaX <= 1160f;
        nearClara = (currentZone == ExplorationZone.KAMPUS) && rakshaX >= 780f;
        nearKostGate = (currentZone == ExplorationZone.KOST_OUTSIDE) && rakshaX >= 480f && rakshaX <= 750f;

        // Zone-specific edge / movement clamping
        switch (currentZone) {
            case KOST:
                if (rakshaX < 60f) rakshaX = 60f;
                if (rakshaX > VIRTUAL_WIDTH - 60f) {
                    startZoneTransition(ExplorationZone.KOST_OUTSIDE, "Keluar Kost...");
                }
                break;
            case KOST_OUTSIDE:
                if (rakshaX < 60f) rakshaX = 60f;
                if (rakshaX > VIRTUAL_WIDTH - 60f) {
                    startZoneTransition(ExplorationZone.WARKOP, "Menuju Jalan Raya");
                }
                break;
            case WARKOP:
                if (rakshaX < 60f) {
                    startZoneTransition(ExplorationZone.KOST_OUTSIDE, "Kembali ke Depan Kost");
                }
                if (rakshaX > VIRTUAL_WIDTH - 60f) {
                    startZoneTransition(ExplorationZone.KAMPUS, "Tiba di Kampus");
                }
                break;
            case KAMPUS:
                if (rakshaX < 60f) {
                    startZoneTransition(ExplorationZone.WARKOP, "Kembali ke Jalan Raya");
                }
                if (rakshaX > VIRTUAL_WIDTH - 60f) rakshaX = VIRTUAL_WIDTH - 60f;
                break;
        }

        // E / W key: trigger interaction for the nearest hotspot (justPressed = single-fire)
        if (Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            if (nearBed) {
                loadNode("CH1_KOST_CHOICES");
            } else if (nearClara) {
                loadNode("CH1_KAMPUS_CHOICES");
            } else if (nearKostGate && Gdx.input.isKeyJustPressed(Input.Keys.W)) {
                startZoneTransition(ExplorationZone.KOST, "Masuk ke Kamar Kost");
            }
        }
    }

    private void updateZoneTransition(float delta) {
        float fadeSpeed = 2.0f; // 0.5s to go black
        zoneTransTimer += delta;

        if (zoneTransFadingOut) {
            // Phase 1: fade to black
            zoneTransAlpha = Math.min(1f, zoneTransAlpha + fadeSpeed * delta);
            if (zoneTransAlpha >= 1f) {
                // Fully black — switch zone and spawn Raksha at correct side
                currentZone = pendingZone;
                if (pendingZone == ExplorationZone.KOST) {
                    rakshaX = VIRTUAL_WIDTH - 150f; // entering from right, spawn at right side
                    rakshaFacingRight = false;
                } else if (pendingZone == ExplorationZone.KOST_OUTSIDE && currentZone == ExplorationZone.KOST) {
                    rakshaX = 615f; // entering from kost door, spawn at gate
                    rakshaFacingRight = true;
                } else {
                    rakshaX = 150f; // entering from left, spawn at left side
                    rakshaFacingRight = true;
                }
                zoneTransFadingOut = false;
                zoneTransTimer = 0f;
            }
        } else {
            // Phase 2: show label briefly then fade in
            zoneTransLabelAlpha = Math.min(1f, zoneTransTimer * 2.5f);
            if (zoneTransTimer > 0.8f) {
                // Start fading back in
                zoneTransAlpha = Math.max(0f, zoneTransAlpha - fadeSpeed * delta);
                if (zoneTransAlpha <= 0f) {
                    isZoneTransitioning = false;
                }
            }
        }
    }

    private void renderExploration(float delta) {
        SpriteBatch batch = game.getBatch();
        batch.begin();

        // 1. Draw full-screen background matching current zone
        Texture zoneBg;
        switch (currentZone) {
            case KOST:   zoneBg = kamarKostTexture; break;
            case KOST_OUTSIDE: zoneBg = kostOutsideTexture; break;
            case WARKOP: zoneBg = jalanRayaTexture;    break;
            default:     zoneBg = studioTexture;    break; // KAMPUS
        }
        if (zoneBg != null) {
            batch.setColor(Color.WHITE);
            batch.draw(zoneBg, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        }

        // Consistent character scale across all zones
        float rakshaSpriteIdle = 1150f;
        float rakshaSpriteWalk = 1405f;
        float claraSpriteSize  = 1150f;
        
        float idleDrawY, walkDrawY, claraDrawY;

        switch (currentZone) {
            case KOST:
                // Floor level is higher up, not in the black bar
                idleDrawY  = -320f; 
                walkDrawY  = -450f;
                claraDrawY = -320f;
                break;
            case KOST_OUTSIDE:
                idleDrawY  = -300f;
                walkDrawY  = -430f;
                claraDrawY = -300f;
                break;
            case WARKOP:
                // Raised sidewalk
                idleDrawY  = -300f;
                walkDrawY  = -430f;
                claraDrawY = -300f;
                break;
            default: // KAMPUS
                // Studio floor
                idleDrawY  = -320f;
                walkDrawY  = -450f;
                claraDrawY = -320f;
                break;
        }

        // 2. Draw Clara — only visible in the KAMPUS zone, at center-right
        if (currentZone == ExplorationZone.KAMPUS) {
            float claraDrawX = 950f - claraSpriteSize / 2f;
            TextureRegion claraFrame = claraIdleAnimation.getKeyFrame(claraAnimationTime, true);
            batch.draw(claraFrame, claraDrawX, claraDrawY, claraSpriteSize, claraSpriteSize);
        }

        // 3. Draw Raksha (walk vs idle animation)
        boolean isMoving = !isZoneTransitioning && (
                Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT) ||
                Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT));

        TextureRegion rakshaFrame;
        float rakshaSpriteSize;
        float rakshaDrawY;

        if (isMoving) {
            rakshaFrame    = walkAnimation.getKeyFrame(animationTime, true);
            rakshaSpriteSize = rakshaSpriteWalk;
            rakshaDrawY      = walkDrawY;
        } else {
            rakshaFrame    = idleAnimation.getKeyFrame(animationTime, true);
            rakshaSpriteSize = rakshaSpriteIdle;
            rakshaDrawY      = idleDrawY;
        }

        float rakshaDrawX = rakshaX - rakshaSpriteSize / 2f;
        if (rakshaFacingRight) {
            batch.draw(rakshaFrame, rakshaDrawX, rakshaDrawY, rakshaSpriteSize, rakshaSpriteSize);
        } else {
            batch.draw(rakshaFrame, rakshaDrawX + rakshaSpriteSize, rakshaDrawY, -rakshaSpriteSize, rakshaSpriteSize);
        }

        // 4. Zone hint text
        font.setColor(Color.GOLD);
        String hint;
        switch (currentZone) {
            case KOST:   hint = "[Kamar Kost]  Jalan ke kanan menuju pintu keluar"; break;
            case KOST_OUTSIDE: hint = "[Luar Kost]  Berdiri di gerbang dan tekan [W] untuk masuk kost"; break;
            case WARKOP: hint = "[Jalan Raya]  Terus ke kanan menuju kampus, kiri kembali ke luar kost"; break;
            default:     hint = "[Kampus]  Dekati Clara untuk memilih kegiatan hari ini!"; break;
        }
        font.draw(batch, hint, 50, VIRTUAL_HEIGHT - 30);

        // Interaction prompts — show when near a hotspot
        if (nearBed || nearClara || nearKostGate) {
            String promptText = "";
            if (nearBed) promptText = "[ E ]  Tempat Tidur  —  Latihan Gitar / Tidur";
            else if (nearClara) promptText = "[ E ]  Bicara dengan Clara  —  Pilih kegiatan";
            else if (nearKostGate) promptText = "[ W ]  Masuk ke Kamar Kost";
            
            float promptW = 460f;
            float promptX = (VIRTUAL_WIDTH - promptW) / 2f;
            float promptY = VIRTUAL_HEIGHT / 2f + 50f;

            batch.end();
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, 0.65f);
            shapeRenderer.rect(promptX, promptY - 10f, promptW, 48f);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
            batch.begin();
            font.setColor(Color.GOLD);
            font.getData().setScale(1.05f);
            font.draw(batch, promptText, promptX + 10f, promptY + 30f);
            font.getData().setScale(1.0f);
        }
        batch.end();



        // 5. Zone transition loading overlay (fade to/from black with location label)
        if (isZoneTransitioning && zoneTransAlpha > 0f) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, zoneTransAlpha);
            shapeRenderer.rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);

            // Draw location label when sufficiently opaque
            if (!zoneTransFadingOut && zoneTransLabelAlpha > 0f) {
                batch.begin();
                font.setColor(1f, 1f, 1f, zoneTransLabelAlpha * zoneTransAlpha);
                font.getData().setScale(1.8f);
                font.draw(batch, zoneTransLabel, 0f, VIRTUAL_HEIGHT / 2f + 20f,
                        VIRTUAL_WIDTH, com.badlogic.gdx.utils.Align.center, false);
                font.getData().setScale(1.0f);
                batch.end();
            }
        }

        // 6. Draw HUD Badge
        drawHUD(batch, shapeRenderer);
    }

    // ==========================================
    // STATE: DIALOGUE_STATE (Visual Novel VN)
    // ==========================================
    private void startTypewriter(String text) {
        typedText = "";
        typedCharCount = 0;
        typewriterTimer = 0f;
        isDialogueFinished = false;
    }

    private void updateDialogue(float delta) {
        String fullText = currentDialogueText;
        if (isDialogueFinished) return;

        typewriterTimer += delta;
        if (typewriterTimer >= typewriterCharDelay) {
            typewriterTimer = 0f;
            typedCharCount++;
            if (typedCharCount >= fullText.length()) {
                typedText = fullText;
                isDialogueFinished = true;
            } else {
                typedText = fullText.substring(0, typedCharCount);
            }
        }
        nextIndicatorTimer += delta;
    }

    private void renderDialogue(float delta) {
        SpriteBatch batch = game.getBatch();
        
        // 1. Draw Background
        batch.begin();
        if (currentBackground != null) {
            batch.setColor(Color.WHITE);
            batch.draw(currentBackground, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        }
        batch.end();

        // 2. Draw Characters
        batch.begin();
        renderCharacters(batch, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        batch.end();

        // 3. Determine Dialogue Box dimensions and position (Speech Bubble style)
        // We ALWAYS show dialogue box, even if there are choices, so the context text is visible
        boolean hasChoices = currentNode != null && currentNode.choices != null;

        float boxX = 40f;
        float boxY = 30f;
        float boxW = VIRTUAL_WIDTH - 80f;
        float boxH = 150f;
        
        // Dynamically adjust box height for long text or multiple paragraphs
        String speechContent = typedText;
        if (speechContent != null && (speechContent.length() > 120 || speechContent.contains("\n"))) {
            boxH = 220f;
        }

        String speakerName = currentSpeakerName;

        // Main box parallelogram points (slanted backward \ \)
        float mainSlant = 40f;
        float cx1 = boxX + mainSlant, cy1 = boxY;
        float cx2 = boxX + boxW + mainSlant, cy2 = boxY;
        float cx3 = boxX + boxW, cy3 = boxY + boxH;
        float cx4 = boxX, cy4 = boxY + boxH;

        // Name box parallelogram points (slanted backward \ \)
        float nameSlant = 20f;
        float nameW = 280f;
        float nameH = 50f;
        float nameX = boxX + 60f;
        float nameY = boxY + boxH; // exactly on top of main box

        float nx1 = nameX + nameSlant, ny1 = nameY;
        float nx2 = nameX + nameW + nameSlant, ny2 = nameY;
        float nx3 = nameX + nameW, ny3 = nameY + nameH;
        float nx4 = nameX, ny4 = nameY + nameH;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        
        // 1. Draw Fills
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0.0f, 0.0f, 0.0f, 0.95f)); // Solid Black
        
        // Main Box Fill
        shapeRenderer.triangle(cx1, cy1, cx2, cy2, cx3, cy3);
        shapeRenderer.triangle(cx1, cy1, cx3, cy3, cx4, cy4);
        
        // Name Box Fill
        if (speakerName != null && !speakerName.isEmpty()) {
            shapeRenderer.triangle(nx1, ny1, nx2, ny2, nx3, ny3);
            shapeRenderer.triangle(nx1, ny1, nx3, ny3, nx4, ny4);
        }
        shapeRenderer.end();
        
        // 2. Draw Continuous Border
        Gdx.gl.glLineWidth(5f); // Thicker outline
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        
        if (speakerName != null && !speakerName.isEmpty()) {
            // Continuous perimeter
            shapeRenderer.line(cx1, cy1, cx2, cy2); // Bottom
            shapeRenderer.line(cx2, cy2, cx3, cy3); // Right
            shapeRenderer.line(cx3, cy3, nx2, ny1); // Top right
            shapeRenderer.line(nx2, ny1, nx3, ny3); // Name box right
            shapeRenderer.line(nx3, ny3, nx4, ny4); // Name box top
            shapeRenderer.line(nx4, ny4, nx1, ny1); // Name box left
            shapeRenderer.line(nx1, ny1, cx4, cy4); // Top left
            shapeRenderer.line(cx4, cy4, cx1, cy1); // Left
        } else {
            shapeRenderer.polygon(new float[]{ cx1, cy1, cx2, cy2, cx3, cy3, cx4, cy4 });
        }
        
        shapeRenderer.end();
        Gdx.gl.glLineWidth(1f);
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // 5. Draw text dialogue & speaker name
        batch.begin();
        if (currentNode != null) {
            if (speakerName != null && !speakerName.isEmpty()) {
                dialogueFont.setColor(Color.WHITE);
                dialogueFont.getData().setScale(1.25f);
                dialogueFont.draw(batch, speakerName, nameX + 30f, nameY + 36f);
                dialogueFont.getData().setScale(1.0f);
                
                dialogueFont.draw(batch, speechContent, boxX + 60f, boxY + boxH - 35f, boxW - 80f, Align.left, true);
            } else {
                dialogueFont.setColor(Color.WHITE);
                dialogueFont.draw(batch, speechContent, boxX + 60f, boxY + boxH - 35f, boxW - 80f, Align.left, true);
            }

            // Flashing next indicator ◆
            if (isDialogueFinished && !hasChoices) {
                if ((int)(nextIndicatorTimer * 2.5f) % 2 == 0) {
                    dialogueFont.setColor(Color.ORANGE);
                    dialogueFont.draw(batch, "\u25C6", boxX + boxW - 40f, boxY + 40f);
                }
            }
        }
        batch.end();

        // 5. Draw choices overlaid in center if active
        if (hasChoices) {
            renderChoices(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        }

        // 6. Draw HUD Overlay
        drawHUD(batch, shapeRenderer);
    }

    private void renderCharacters(SpriteBatch batch, float width, float height) {
        if (currentNode == null) return;
        String nodeId = currentNode.nodeId;

        // Skip rendering character models during full illustrations/prologue/endings
        if (nodeId.startsWith("PROLOG_") || nodeId.startsWith("CREDITS_") || nodeId.equals("GAME_OVER")) {
            return;
        }

        // Large portrait height anchored to bottom of screen
        float targetHeight = VIRTUAL_HEIGHT * 0.80f;
        float yPos = 0f;

        float leftX = width * 0.28f;
        float rightX = width * 0.72f;

        String speaker = currentSpeakerName;

        float rakshaAlpha = 0.5f;
        float rightAlpha = 0.5f;

        if (speaker != null) {
            if (speaker.equalsIgnoreCase("Raksha")) {
                rakshaAlpha = 1.0f;
                rightAlpha = 0.4f;
            } else if (speaker.equalsIgnoreCase("Clara") || speaker.equalsIgnoreCase("Sherly")) {
                rightAlpha = 1.0f;
                rakshaAlpha = 0.4f;
            } else if (speaker.isEmpty()) {
                rakshaAlpha = 0.6f;
                rightAlpha = 0.6f;
            } else {
                rakshaAlpha = 0.5f;
                rightAlpha = 0.5f;
            }
        }

        // Draw Raksha (Left)
        Texture rakshaTex = null;
        if (expressionTextures != null && expressionTextures.containsKey("raksha")) {
            rakshaTex = expressionTextures.get("raksha").get(rakshaExpression);
        }
        drawCharacter(batch, rakshaTex, idleAnimation.getKeyFrame(animationTime, true), leftX, yPos, targetHeight, rakshaAlpha);

        // Draw Right Character (Clara or Sherly)
        if ("None".equals(activeRightCharacter)) {
            // Do not draw right character
        } else if ("Sherly".equals(activeRightCharacter)) {
            Texture sherlyTex = null;
            if (expressionTextures != null && expressionTextures.containsKey("sherly")) {
                sherlyTex = expressionTextures.get("sherly").get(sherlyExpression);
            }
            drawCharacter(batch, sherlyTex, null, rightX, yPos, targetHeight, rightAlpha);
        } else {
            // Clara
            Texture claraTex = null;
            if (expressionTextures != null && expressionTextures.containsKey("clara")) {
                claraTex = expressionTextures.get("clara").get(claraExpression);
            }
            drawCharacter(batch, claraTex, claraIdleAnimation.getKeyFrame(claraAnimationTime, true), rightX, yPos, targetHeight, rightAlpha);
        }

        batch.setColor(Color.WHITE);
    }

    private void drawCharacter(SpriteBatch batch, Texture tex, TextureRegion fallbackFrame, float centerX, float yPos, float targetHeight, float alpha) {
        batch.setColor(1f, 1f, 1f, alpha);
        if (tex != null) {
            float aspect = (float) tex.getWidth() / (float) tex.getHeight();
            float drawH = targetHeight;
            float drawW = drawH * aspect;
            float drawX = centerX - drawW / 2f;
            batch.draw(tex, drawX, yPos, drawW, drawH);
        } else if (fallbackFrame != null) {
            // Use original large sprite rendering unchanged
            float originalSize = 2000f;
            float originalY = -850f;
            float drawX = centerX - originalSize / 2f;
            batch.draw(fallbackFrame, drawX, originalY, originalSize, originalSize);
        }
    }

    private void renderChoices(float width, float height) {
        if (currentNode == null || currentNode.choices == null) return;

        Choice[] choices = currentNode.choices;
        int n = choices.length;
        float btnW = 600f;
        float btnH = 50f;
        float gap = 15f;
        float totalH = n * btnH + (n - 1) * gap;
        float startY = (height - totalH) / 2f;
        float startX = (width - btnW) / 2f;

        // Map mouse to virtual coordinates
        Vector3 mousePoint = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mousePoint);
        float mouseX = mousePoint.x;
        float mouseY = mousePoint.y;

        SpriteBatch batch = game.getBatch();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        for (int i = 0; i < n; i++) {
            float btnY = startY + (n - 1 - i) * (btnH + gap);
            boolean isHovered = mouseX >= startX && mouseX <= startX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;

            float slant = 25f;
            float px1 = startX - slant;
            float py1 = btnY;
            float px2 = startX + btnW - slant;
            float py2 = btnY;
            float px3 = startX + btnW;
            float py3 = btnY + btnH;
            float px4 = startX;
            float py4 = btnY + btnH;

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            if (isHovered) {
                shapeRenderer.setColor(new Color(0.85f, 0.05f, 0.05f, 1.0f)); // Persona Red
            } else {
                shapeRenderer.setColor(new Color(0.05f, 0.05f, 0.05f, 0.95f)); // Solid Black
            }
            shapeRenderer.triangle(px1, py1, px2, py2, px3, py3);
            shapeRenderer.triangle(px1, py1, px3, py3, px4, py4);
            shapeRenderer.end();

            Gdx.gl.glLineWidth(4f);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(Color.WHITE);
            shapeRenderer.polygon(new float[]{ px1, py1, px2, py2, px3, py3, px4, py4 });
            shapeRenderer.end();
            Gdx.gl.glLineWidth(1f);
        }
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        for (int i = 0; i < n; i++) {
            Choice choice = choices[i];
            float btnY = startY + (n - 1 - i) * (btnH + gap);
            boolean isHovered = mouseX >= startX && mouseX <= startX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;

            choiceFont.setColor(Color.WHITE);
            float textY = btnY + btnH / 2f + 7f;
            
            if (isHovered) {
                choiceFont.getData().setScale(1.1f);
                choiceFont.draw(batch, choice.text, startX, textY, btnW, Align.center, false);
                choiceFont.getData().setScale(1.0f);
            } else {
                choiceFont.draw(batch, choice.text, startX, textY, btnW, Align.center, false);
            }
        }
        batch.end();
    }

    private void advanceDialogue() {
        if (!isDialogueFinished) {
            typedText = currentDialogueText;
            isDialogueFinished = true;
            return;
        }

        if (currentNode != null) {
            if (currentNode.choices != null) return; // Prevent advancing when showing choices

            String nextId = currentNode.nextId;
            if (nextId == null || nextId.isEmpty() || nextId.equals("GAME_OVER")) {
                game.setScreen(new MainMenuScreen(game));
            } else if (nextId.equals("START_GUITAR_RHYTHM_GAME")) {
                rhythmFromGuitarPractice = true;
                state = GameplayState.RHYTHM_STATE;
                nearBed   = false;
                nearClara = false;
                rhythmMusic = Gdx.audio.newMusic(Gdx.files.internal("music/cover seandainya.mp3"));
                rhythmMusic.setVolume(SettingsManager.getVolume());
                rhythmGame.start(rhythmMusic);
            } else {
                loadNode(nextId);
            }
        }
    }

    // ==========================================
    // STATE: RHYTHM_STATE
    // ==========================================
    private void updateRhythm(float delta) {
        rhythmGame.update(delta);
        if (!rhythmGame.isActive()) {
            gameState.performanceScore = rhythmGame.score;
            if (rhythmMusic != null) {
                rhythmMusic.dispose();
                rhythmMusic = null;
            }
            if (rhythmFromGuitarPractice) {
                // Guitar practice in kost: award stats based on performance then end day
                rhythmFromGuitarPractice = false;
                // Bonus stat scaling with score (base: creativity+3, confidence+2)
                int bonusCreativity = 3 + (rhythmGame.perfects >= 10 ? 2 : 0);
                int bonusConfidence = 2 + (rhythmGame.perfects >= 15 ? 1 : 0);
                gameState.creativity  += bonusCreativity;
                gameState.confidence  += bonusConfidence;
                loadNode("CH1_PRACTICE_END");
            } else {
                loadNode("CH4_STRING_SNAP");
            }
        }
    }

    private void renderRhythm(float delta) {
        shapeRenderer.setProjectionMatrix(game.getBatch().getProjectionMatrix());
        rhythmGame.draw(shapeRenderer, game.getBatch(), font, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
    }

    // ==========================================
    // SHARED CALENDAR / HUD
    // ==========================================
    private void drawHUD(SpriteBatch batch, ShapeRenderer shape) {
        if (gameState == null) return;

        float badgeW = 220f;
        float badgeH = 80f;
        boolean showMoney = gameState.chapter.equals("CHAPTER_1");
        if (showMoney) {
            badgeH = 110f;
        }

        float badgeX = VIRTUAL_WIDTH - badgeW - 20f;
        float badgeY = VIRTUAL_HEIGHT - badgeH - 20f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(new Color(0.02f, 0.05f, 0.12f, 0.8f));
        shape.rect(badgeX, badgeY, badgeW, badgeH);
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(new Color(0.2f, 0.4f, 0.8f, 0.7f));
        shape.rect(badgeX, badgeY, badgeW, badgeH);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        font.setColor(Color.YELLOW);
        font.draw(batch, "📅 Sisa Hari: " + remainingDays, badgeX + 15f, badgeY + badgeH - 15f);

        font.setColor(Color.WHITE);
        font.draw(batch, "🕒 Waktu: " + timeOfDay, badgeX + 15f, badgeY + badgeH - 42f);

        if (showMoney) {
            font.setColor(Color.GREEN);
            font.draw(batch, "🪙 Uang: Rp" + gameState.money, badgeX + 15f, badgeY + badgeH - 69f);
            font.setColor(Color.WHITE);
            font.draw(batch, "🎸 Kreativitas: " + gameState.creativity, badgeX + 15f, badgeY + badgeH - 90f);
        }
        
        if (state == GameplayState.EXPLORATION_STATE) {
            font.setColor(Color.CYAN);
            font.draw(batch, "[A]/[D] atau [Kiri]/[Kanan] \uD83D\uDEB6 Jalan", 20f, 110f);
            font.draw(batch, "[SPACE] \uD83D\uDCAC Interaksi", 20f, 80f);
            font.draw(batch, "[TAB] \uD83D\uDCF1 Buka HP", 20f, 50f);
            font.draw(batch, "[ESC] \u23F8 Pause Menu", 20f, 20f);
        }
        batch.end();
    }

    // ==========================================
    // INPUT PROCESSOR IMPLEMENTATION
    // ==========================================
    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            if (state != GameplayState.PAUSED_STATE) {
                previousState = state;
                state = GameplayState.PAUSED_STATE;
                if (rhythmMusic != null && rhythmMusic.isPlaying()) {
                    rhythmMusic.pause();
                }
            }
            return true;
        }

        if (keycode == Input.Keys.TAB) {
            if (state == GameplayState.EXPLORATION_STATE) {
                previousState = state;
                state = GameplayState.PHONE_STATE;
                phoneScreen = 0;
                return true;
            } else if (state == GameplayState.PHONE_STATE) {
                state = previousState;
                return true;
            }
        }

        if (state == GameplayState.DIALOGUE_STATE) {
            if (currentNode != null && currentNode.choices != null) return false;
            if (keycode == Input.Keys.SPACE || keycode == Input.Keys.ENTER) {
                advanceDialogue();
                return true;
            }
        } else if (state == GameplayState.RHYTHM_STATE) {
            rhythmGame.handleKeyPress(keycode);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (state == GameplayState.RHYTHM_STATE) {
            return rhythmGame.handleKeyRelease(keycode);
        }
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        // Map mouse coordinates to virtual coordinates
        Vector3 touchPoint = new Vector3(screenX, screenY, 0);
        viewport.unproject(touchPoint);
        float rx = touchPoint.x;
        float ry = touchPoint.y;
        
        if (state == GameplayState.PAUSED_STATE) {
            float btnW = 200f;
            float btnH = 50f;
            float bx = (VIRTUAL_WIDTH - btnW) / 2f;
            float menuW = 400f;
            float menuH = 450f;
            float mx = (VIRTUAL_WIDTH - menuW) / 2f;
            float my = (VIRTUAL_HEIGHT - menuH) / 2f;
            
            // Resume
            if (rx >= bx && rx <= bx + btnW && ry >= my + 350 && ry <= my + 350 + btnH) {
                state = previousState;
                if (state == GameplayState.RHYTHM_STATE && rhythmMusic != null && rhythmGame.isActive()) {
                    rhythmMusic.play();
                }
                return true;
            }
            // Exit
            if (rx >= bx && rx <= bx + btnW && ry >= my + 40 && ry <= my + 40 + btnH) {
                game.setScreen(new MainMenuScreen(game));
                return true;
            }
            // Vol -
            if (rx >= bx - 60 && rx <= bx - 20 && ry >= my + 260 && ry <= my + 300) {
                SettingsManager.setVolume(SettingsManager.getVolume() - 0.1f);
                game.updateMusicVolume();
                if (rhythmMusic != null) rhythmMusic.setVolume(SettingsManager.getVolume());
                return true;
            }
            // Vol +
            if (rx >= bx + btnW + 20 && rx <= bx + btnW + 60 && ry >= my + 260 && ry <= my + 300) {
                SettingsManager.setVolume(SettingsManager.getVolume() + 0.1f);
                game.updateMusicVolume();
                if (rhythmMusic != null) rhythmMusic.setVolume(SettingsManager.getVolume());
                return true;
            }
            // Easy
            if (rx >= mx + 20 && rx <= mx + 120 && ry >= my + 150 && ry <= my + 190) {
                SettingsManager.setDifficulty(0);
                return true;
            }
            // Medium
            if (rx >= mx + 150 && rx <= mx + 250 && ry >= my + 150 && ry <= my + 190) {
                SettingsManager.setDifficulty(1);
                return true;
            }
            // Hard
            if (rx >= mx + 280 && rx <= mx + 380 && ry >= my + 150 && ry <= my + 190) {
                SettingsManager.setDifficulty(2);
                return true;
            }
            return true;
        }

        if (state == GameplayState.PHONE_STATE) {
            float phoneW = 350f;
            float phoneH = 600f;
            float px = (VIRTUAL_WIDTH - phoneW) / 2f;
            float py = (VIRTUAL_HEIGHT - phoneH) / 2f;
            
            if (phoneScreen == 0) {
                // Contacts
                for (int i = 0; i < 4; i++) {
                    float rectY = py + phoneH - 140 - (i * 70);
                    if (rx >= px + 20 && rx <= px + phoneW - 20 && ry >= rectY && ry <= rectY + 60) {
                        phoneScreen = i + 1; // 1=Clara, 2=Bagas, 3=Rania, 4=Sherly
                        return true;
                    }
                }
            } else {
                // Back
                if (rx >= px + 20 && rx <= px + 100 && ry >= py + phoneH - 120 && ry <= py + phoneH - 80) {
                    phoneScreen = 0;
                    return true;
                }
                // Ajak Latihan
                if (rx >= px + 20 && rx <= px + phoneW - 20 && ry >= py + 90 && ry <= py + 140) {
                    state = previousState;
                    loadNode("PHONE_INVITE_PRACTICE");
                    return true;
                }
                // Ajak Date (only for Clara)
                if (phoneScreen == 1) {
                    if (rx >= px + 20 && rx <= px + phoneW - 20 && ry >= py + 30 && ry <= py + 80) {
                        state = previousState;
                        loadNode("PHONE_CLARA_DATE");
                        return true;
                    }
                }
            }
            return true;
        }

        if (state == GameplayState.DIALOGUE_STATE) {
            if (currentNode != null && currentNode.choices != null) {
                Choice[] choices = currentNode.choices;
                int n = choices.length;
                float btnW = 600f;
                float btnH = 50f;
                float gap = 15f;
                float totalH = n * btnH + (n - 1) * gap;
                float startY = (VIRTUAL_HEIGHT - totalH) / 2f;
                float startX = (VIRTUAL_WIDTH - btnW) / 2f;

                for (int i = 0; i < n; i++) {
                    float btnY = startY + (n - 1 - i) * (btnH + gap);
                    if (rx >= startX && rx <= startX + btnW && ry >= btnY && ry <= btnY + btnH) {
                        Choice choice = choices[i];

                        // Clara duet condition trigger check
                        if (currentNode.nodeId.equals("CH4_STRING_CHOICE") && i == 2) {
                            if (gameState.claraRel >= 8) {
                                gameState.claraSang = true;
                            } else {
                                gameState.claraSang = false;
                            }
                        }

                        // Guitar practice routes through the rhythm mini-game.
                        // Stats are awarded AFTER the game finishes to avoid double-application.
                        if (choice.action != null) {
                            choice.action.execute(gameState);
                        }
                        loadNode(choice.nextNodeId);
                        return true;
                    }
                }
                return false;
            } else {
                advanceDialogue();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    // ==========================================
    // SCREEN INTERFACE METHODS
    // ==========================================
    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (font != null) font.dispose();
        if (dialogueFont != null) dialogueFont.dispose();
        if (choiceFont != null) choiceFont.dispose();

        if (studioTexture != null) studioTexture.dispose();
        if (skyTexture != null) skyTexture.dispose();
        if (kamarKostTexture != null) kamarKostTexture.dispose();
        if (jalanRayaTexture != null) jalanRayaTexture.dispose();
        if (kostOutsideTexture != null) kostOutsideTexture.dispose();

        if (prologTextures != null) {
            for (Texture tex : prologTextures) {
                if (tex != null) tex.dispose();
            }
        }
        if (idleTextures != null) {
            for (Texture tex : idleTextures) {
                if (tex != null) tex.dispose();
            }
        }
        if (walkTextures != null) {
            for (Texture tex : walkTextures) {
                if (tex != null) tex.dispose();
            }
        }
        if (claraIdleTextures != null) {
            for (Texture tex : claraIdleTextures) {
                if (tex != null) tex.dispose();
            }
        }
        if (rhythmMusic != null) {
            rhythmMusic.dispose();
        }

        if (expressionTextures != null) {
            for (Map<String, Texture> charMap : expressionTextures.values()) {
                for (Texture tex : charMap.values()) {
                    if (tex != null) tex.dispose();
                }
            }
            expressionTextures.clear();
        }
    }

    private String detectExpression(String text) {
        if (text == null) return "biasa aja";
        String lower = text.toLowerCase();
        if (lower.contains("marah") || lower.contains("kesal") || lower.contains("benci") || lower.contains("pergi sana") || lower.contains("ganggu") || lower.contains("dengus") || lower.contains("ketus") || lower.contains("teriak")) {
            return "marah";
        }
        if (lower.contains("sedih") || lower.contains("pahit") || lower.contains("menangis") || lower.contains("kecewa") || lower.contains("maaf") || lower.contains("sorry") || lower.contains("kacau") || lower.contains("lelah")) {
            return "sedih";
        }
        if (lower.contains("cemas") || lower.contains("panik") || lower.contains("takut") || lower.contains("khawatir") || lower.contains("jantungku") || lower.contains("deg-degan") || lower.contains("stres") || lower.contains("gemetar")) {
            return "cemas";
        }
        if (lower.contains("malu") || lower.contains("merona") || lower.contains("canggung") || lower.contains("tersipu")) {
            return "malu";
        }
        if (lower.contains("bahagia") || lower.contains("senang") || lower.contains("senyum") || lower.contains("tertawa") || lower.contains("tawa") || lower.contains("keren") || lower.contains("hebat") || lower.contains("berhasil") || lower.contains("mantap") || lower.contains("histeris") || lower.contains("kegirangan") || lower.contains("haru")) {
            return "bahagia";
        }
        return "biasa aja";
    }

    private void loadExpressionTextures() {
        expressionTextures = new HashMap<>();

        Map<String, String> claraFiles = new HashMap<>();
        // CLARA MAHASISWA
        claraFiles.put("MAHASISWA_bahagia", "expression/CLARA MAHASISWA/bahagia-Photoroom (2).png");
        claraFiles.put("MAHASISWA_biasa aja", "expression/CLARA MAHASISWA/biasa aja-Photoroom (2).png");
        claraFiles.put("MAHASISWA_cemas", "expression/CLARA MAHASISWA/cemas-Photoroom (2).png");
        claraFiles.put("MAHASISWA_malu", "expression/CLARA MAHASISWA/malu-Photoroom (2).png");
        claraFiles.put("MAHASISWA_marah", "expression/CLARA MAHASISWA/marah-Photoroom (2).png");
        claraFiles.put("MAHASISWA_sedih", "expression/CLARA MAHASISWA/sedih-Photoroom (2).png");
        // CLARA LATIHAN
        claraFiles.put("LATIHAN_bahagia", "expression/CLARA LATIHAN/happy-Photoroom.png");
        claraFiles.put("LATIHAN_biasa aja", "expression/CLARA LATIHAN/netral-Photoroom.png");
        claraFiles.put("LATIHAN_cemas", "expression/CLARA LATIHAN/Worried-Photoroom.png");
        claraFiles.put("LATIHAN_malu", "expression/CLARA LATIHAN/shy-Photoroom.png");
        claraFiles.put("LATIHAN_marah", "expression/CLARA LATIHAN/angry-Photoroom.png");
        claraFiles.put("LATIHAN_sedih", "expression/CLARA LATIHAN/sad-Photoroom.png");
        // CLARA PERFOM
        claraFiles.put("PERFOM_bahagia", "expression/CLARA PERFOM/bahagia-Photoroom (1).png");
        claraFiles.put("PERFOM_biasa aja", "expression/CLARA PERFOM/biasa aja-Photoroom (1).png");
        claraFiles.put("PERFOM_cemas", "expression/CLARA PERFOM/cemas-Photoroom (1).png");
        claraFiles.put("PERFOM_malu", "expression/CLARA PERFOM/malu-Photoroom (1).png");
        claraFiles.put("PERFOM_marah", "expression/CLARA PERFOM/marah-Photoroom (1).png");
        claraFiles.put("PERFOM_sedih", "expression/CLARA PERFOM/sedih-Photoroom (1).png");

        Map<String, String> rakshaFiles = new HashMap<>();
        // RAKSHA MAHASISWA
        rakshaFiles.put("MAHASISWA_bahagia", "expression/RAKSHA MAHASISWA 2/bahagia-Photoroom (13).png");
        rakshaFiles.put("MAHASISWA_biasa aja", "expression/RAKSHA MAHASISWA 2/biasa aja-Photoroom (13).png");
        rakshaFiles.put("MAHASISWA_cemas", "expression/RAKSHA MAHASISWA 2/cemas-Photoroom (13).png");
        rakshaFiles.put("MAHASISWA_malu", "expression/RAKSHA MAHASISWA 2/malu-Photoroom (13).png");
        rakshaFiles.put("MAHASISWA_marah", "expression/RAKSHA MAHASISWA 2/marah-Photoroom (12).png");
        rakshaFiles.put("MAHASISWA_sedih", "expression/RAKSHA MAHASISWA 2/sedih-Photoroom (12).png");
        // RAKSHA LATIHAN
        rakshaFiles.put("LATIHAN_bahagia", "expression/RAKSHA LATIHAN 2/bahagia-Photoroom (12).png");
        rakshaFiles.put("LATIHAN_biasa aja", "expression/RAKSHA LATIHAN 2/biasa aja-Photoroom (12).png");
        rakshaFiles.put("LATIHAN_cemas", "expression/RAKSHA LATIHAN 2/cemas-Photoroom (12).png");
        rakshaFiles.put("LATIHAN_malu", "expression/RAKSHA LATIHAN 2/malu-Photoroom (12).png");
        rakshaFiles.put("LATIHAN_marah", "expression/RAKSHA LATIHAN 2/marah-Photoroom (11).png");
        rakshaFiles.put("LATIHAN_sedih", "expression/RAKSHA LATIHAN 2/sedih-Photoroom (11).png");
        // RAKSHA PERFOM
        rakshaFiles.put("PERFOM_bahagia", "expression/RAKSHA PERFOM 2/bahagia-Photoroom (15).png");
        rakshaFiles.put("PERFOM_biasa aja", "expression/RAKSHA PERFOM 2/biasa aja-Photoroom (14).png");
        rakshaFiles.put("PERFOM_cemas", "expression/RAKSHA PERFOM 2/cemas-Photoroom (14).png");
        rakshaFiles.put("PERFOM_malu", "expression/RAKSHA PERFOM 2/malu-Photoroom (14).png");
        rakshaFiles.put("PERFOM_marah", "expression/RAKSHA PERFOM 2/marah-Photoroom (13).png");
        rakshaFiles.put("PERFOM_sedih", "expression/RAKSHA PERFOM 2/sedih-Photoroom (13).png");

        Map<String, String> sherlyFiles = new HashMap<>();
        // SHERLY MAHASISWA (SMESTER AKHIR)
        sherlyFiles.put("MAHASISWA_bahagia", "expression/SHERLY SMESTER AKHIR/bahagia-Photoroom (7).png");
        sherlyFiles.put("MAHASISWA_biasa aja", "expression/SHERLY SMESTER AKHIR/biasa aja-Photoroom (7).png");
        sherlyFiles.put("MAHASISWA_cemas", "expression/SHERLY SMESTER AKHIR/cemas-Photoroom (7).png");
        sherlyFiles.put("MAHASISWA_malu", "expression/SHERLY SMESTER AKHIR/malu2-Photoroom.png");
        sherlyFiles.put("MAHASISWA_marah", "expression/SHERLY SMESTER AKHIR/marah2-Photoroom.png");
        sherlyFiles.put("MAHASISWA_sedih", "expression/SHERLY SMESTER AKHIR/sedih-Photoroom (7).png");
        // SHERLY MASA FINAL
        sherlyFiles.put("FINAL_bahagia", "expression/SHERLY MASA FINAL/bahagia-Photoroom (6).png");
        sherlyFiles.put("FINAL_biasa aja", "expression/SHERLY MASA FINAL/biasa aja-Photoroom (6).png");
        sherlyFiles.put("FINAL_cemas", "expression/SHERLY MASA FINAL/cemas-Photoroom (6).png");
        sherlyFiles.put("FINAL_malu", "expression/SHERLY MASA FINAL/malu-Photoroom (6).png");
        sherlyFiles.put("FINAL_marah", "expression/SHERLY MASA FINAL/marah-Photoroom (6).png");
        sherlyFiles.put("FINAL_sedih", "expression/SHERLY MASA FINAL/sedih-Photoroom (6).png");

        loadCharacterExpressionMap("clara", claraFiles);
        loadCharacterExpressionMap("raksha", rakshaFiles);
        loadCharacterExpressionMap("sherly", sherlyFiles);
    }

    private void loadCharacterExpressionMap(String charKey, Map<String, String> files) {
        Map<String, Texture> textures = new HashMap<>();
        for (Map.Entry<String, String> entry : files.entrySet()) {
            try {
                Texture tex = new Texture(Gdx.files.internal(entry.getValue()));
                tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                textures.put(entry.getKey(), tex);
            } catch (Exception e) {
                Gdx.app.error("GameplayScreen", "Failed to load expression " + entry.getKey() + " for " + charKey + " at " + entry.getValue() + ": " + e.getMessage());
            }
        }
        expressionTextures.put(charKey, textures);
    }

    private void updateDialogueExpressions(String speaker, String text) {
        String nodeExpr = currentNode.expression;
        if (nodeExpr == null || nodeExpr.isEmpty()) {
            nodeExpr = detectExpression(text);
        }

        // If the expression string has an outfit prefix, use it. Otherwise prepend MAHASISWA_
        if (nodeExpr != null && !nodeExpr.isEmpty()) {
            if (nodeExpr.contains(":") || nodeExpr.contains(",")) {
                String[] parts = nodeExpr.split(",");
                for (String part : parts) {
                    String[] kv = part.split(":");
                    if (kv.length == 2) {
                        String charName = kv[0].trim().toLowerCase();
                        String exprName = kv[1].trim();
                        
                        if (!exprName.contains("_")) {
                            exprName = "MAHASISWA_" + exprName;
                        }
                        
                        if (charName.equals("clara")) {
                            claraExpression = exprName;
                        } else if (charName.equals("raksha")) {
                            rakshaExpression = exprName;
                        } else if (charName.equals("sherly")) {
                            sherlyExpression = exprName;
                        }
                    }
                }
            } else {
                String exprName = nodeExpr;
                if (!exprName.contains("_")) {
                    exprName = "MAHASISWA_" + exprName;
                }
                
                if ("Clara".equalsIgnoreCase(speaker)) {
                    claraExpression = exprName;
                    rakshaExpression = "MAHASISWA_biasa aja";
                } else if ("Sherly".equalsIgnoreCase(speaker)) {
                    sherlyExpression = exprName;
                    rakshaExpression = "MAHASISWA_biasa aja";
                } else if ("Raksha".equalsIgnoreCase(speaker)) {
                    rakshaExpression = exprName;
                    if ("Clara".equals(activeRightCharacter)) {
                        claraExpression = "MAHASISWA_biasa aja";
                    } else {
                        sherlyExpression = "MAHASISWA_biasa aja";
                    }
                } else {
                    // Update all to default if no speaker
                    rakshaExpression = "MAHASISWA_biasa aja";
                    claraExpression = "MAHASISWA_biasa aja";
                    sherlyExpression = "MAHASISWA_biasa aja";
                }
            }
        }

        if ("Sherly".equalsIgnoreCase(speaker)) {
            activeRightCharacter = "Sherly";
        } else if ("Clara".equalsIgnoreCase(speaker)) {
            activeRightCharacter = "Clara";
        }
    }

    private void startCinematicTransition(int fromDay, int toDay, String narrativeText, String chTitle, String chSubtitle, GameplayState nextState, String nextNode) {
        this.state = GameplayState.CINEMATIC_TRANSITION_STATE;
        this.transFromDay = fromDay;
        this.transToDay = toDay;
        this.transNarrativeText = narrativeText;
        this.transChapterTitle = chTitle;
        this.transChapterSubtitle = chSubtitle;
        this.transNextState = nextState;
        this.transNextNode = nextNode;
        
        this.transPhase = 0;
        this.transTime = 0;
        this.transAlpha = 0;
        this.transNumberDisplay = fromDay;
    }

    private void updateCinematicTransition(float delta) {
        transTime += delta;
        
        switch (transPhase) {
            case 0: // Fade Out previous screen (0.5s)
                transAlpha = Math.min(1.0f, transTime / 0.5f);
                if (transTime >= 0.5f) {
                    transTime = 0;
                    transAlpha = 0;
                    if (transNarrativeText != null && !transNarrativeText.isEmpty()) {
                        transPhase = 1;
                    } else if (transFromDay != transToDay) {
                        transPhase = 2;
                    } else if (transChapterTitle != null && !transChapterTitle.isEmpty()) {
                        transPhase = 3;
                    } else {
                        transPhase = 4;
                    }
                }
                break;
                
            case 1: // Narrative Text (Fade in 1.0s, hold 3.0s, fade out 1.0s)
                if (transTime < 1.0f) {
                    transAlpha = transTime / 1.0f;
                } else if (transTime < 4.0f) {
                    transAlpha = 1.0f;
                } else if (transTime < 5.0f) {
                    transAlpha = Math.max(0.0f, 1.0f - (transTime - 4.0f) / 1.0f);
                } else {
                    transTime = 0;
                    transAlpha = 0;
                    if (transFromDay != transToDay) {
                        transPhase = 2;
                    } else if (transChapterTitle != null && !transChapterTitle.isEmpty()) {
                        transPhase = 3;
                    } else {
                        transPhase = 4;
                    }
                }
                break;
                
            case 2: // Day Count Phase (Fade in 0.5s, countdown 0.6s, hold 0.6s, fade out 0.5s)
                if (transTime < 0.5f) {
                    transAlpha = transTime / 0.5f;
                    transNumberDisplay = transFromDay;
                } else if (transTime < 1.7f) {
                    transAlpha = 1.0f;
                    float progress = Math.min(1.0f, (transTime - 0.5f) / 0.6f);
                    transNumberDisplay = transFromDay + (transToDay - transFromDay) * progress;
                } else if (transTime < 2.2f) {
                    transAlpha = Math.max(0.0f, 1.0f - (transTime - 1.7f) / 0.5f);
                } else {
                    transTime = 0;
                    transAlpha = 0;
                    if (transChapterTitle != null && !transChapterTitle.isEmpty()) {
                        transPhase = 3;
                    } else {
                        transPhase = 4;
                    }
                }
                break;
                
            case 3: // Chapter Title Phase (Fade in 0.8s, hold 2.5s, fade out 0.8s)
                if (transTime < 0.8f) {
                    transAlpha = transTime / 0.8f;
                } else if (transTime < 3.3f) {
                    transAlpha = 1.0f;
                } else if (transTime < 4.1f) {
                    transAlpha = Math.max(0.0f, 1.0f - (transTime - 3.3f) / 0.8f);
                } else {
                    transTime = 0;
                    transAlpha = 0;
                    transPhase = 4;
                }
                break;
                
            case 4: // Fade In new screen (0.5s)
                transAlpha = Math.min(1.0f, transTime / 0.5f);
                if (transTime >= 0.5f) {
                    state = transNextState;
                    loadNode(transNextNode);
                    if (state == GameplayState.EXPLORATION_STATE) {
                        rakshaX = 100f;
                    }
                }
                break;
        }
    }

    private void renderCinematicTransition(float delta) {
        SpriteBatch batch = game.getBatch();
        
        if (transPhase == 0) {
            batch.begin();
            if (currentBackground != null) {
                batch.draw(currentBackground, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
            }
            batch.end();
            
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, transAlpha);
            shapeRenderer.rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        } else if (transPhase == 4) {
            batch.begin();
            Texture nextBg = null;
            if (transNextNode != null && storyNodes.containsKey(transNextNode)) {
                String nextNodeId = transNextNode;
                if (nextNodeId.equals("PROLOG_START")) nextBg = prologTextures.get(0);
                else if (nextNodeId.equals("PROLOG_1")) nextBg = prologTextures.get(1);
                else if (nextNodeId.equals("PROLOG_2")) nextBg = prologTextures.get(2);
                else if (nextNodeId.equals("PROLOG_3") || nextNodeId.equals("PROLOG_4") || nextNodeId.equals("PROLOG_5") || nextNodeId.equals("PROLOG_6") || nextNodeId.equals("PROLOG_7")) nextBg = prologTextures.get(3);
                else if (nextNodeId.startsWith("PROLOG_")) nextBg = prologTextures.get(4);
                else if (nextNodeId.equals("CH1_INTRO") || nextNodeId.equals("CH1_DAY_NEXT")) nextBg = kostOutsideTexture;
                else if (nextNodeId.equals("CH1_CHOICE_C_RESULT") || nextNodeId.equals("CH1_PRACTICE_END") || nextNodeId.equals("CH3_POST_RESULT_1") || nextNodeId.equals("CH3_ALDO_RESULT_1")) nextBg = kamarKostTexture;
                else if (nextNodeId.startsWith("CH3_BUS_")) nextBg = jalanRayaTexture;
                else if (nextNodeId.startsWith("END_") || nextNodeId.startsWith("CREDITS_") || nextNodeId.equals("GAME_OVER")) nextBg = skyTexture;
                else nextBg = studioTexture;
            }
            if (nextBg == null) nextBg = currentBackground;
            if (nextBg != null) {
                batch.draw(nextBg, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
            }
            batch.end();
            
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, 1.0f - transAlpha);
            shapeRenderer.rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        } else {
            Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            
            batch.begin();
            if (transPhase == 1) {
                font.setColor(1f, 1f, 1f, transAlpha);
                font.getData().setScale(1.2f);
                font.draw(batch, transNarrativeText, 150f, VIRTUAL_HEIGHT / 2f + 100f, VIRTUAL_WIDTH - 300f, Align.center, true);
                font.getData().setScale(1.0f);
            } else if (transPhase == 2) {
                font.setColor(Color.GOLD.r, Color.GOLD.g, Color.GOLD.b, transAlpha);
                font.getData().setScale(2.5f);
                font.draw(batch, "SISA HARI: " + Math.round(transNumberDisplay), 0f, VIRTUAL_HEIGHT / 2f + 20f, VIRTUAL_WIDTH, Align.center, false);
                font.getData().setScale(1.0f);
            } else if (transPhase == 3) {
                font.setColor(Color.GOLD.r, Color.GOLD.g, Color.GOLD.b, transAlpha);
                font.getData().setScale(2.0f);
                font.draw(batch, transChapterTitle, 0f, VIRTUAL_HEIGHT / 2f + 60f, VIRTUAL_WIDTH, Align.center, false);
                font.getData().setScale(1.2f);
                font.setColor(1f, 1f, 1f, transAlpha);
                font.draw(batch, "\"" + transChapterSubtitle + "\"", 0f, VIRTUAL_HEIGHT / 2f - 10f, VIRTUAL_WIDTH, Align.center, false);
                font.getData().setScale(1.0f);
            }
            batch.end();
        }
    }
}
