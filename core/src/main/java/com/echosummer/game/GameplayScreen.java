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
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.echosummer.game.ds.CustomStack;
import com.echosummer.game.ds.CustomLinkedList;
import com.echosummer.game.ds.LocationGraph;
import com.echosummer.game.ds.CustomHashTable;
import com.echosummer.game.ds.CustomTree;
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
    private Texture kedaiKopiTexture;
    private Texture jalanSetapakTexture;
    private Texture jalanDanauTexture;
    private Texture luarRuangStudioTexture;
    private Texture dalamStudioTexture;
    private Texture ukmSeniTexture;
    private Texture tamanKampusTexture;
    private Texture kantinTexture;
    private Texture lorong1Texture;
    private Texture lorong2Texture;

    // Night background textures (loaded from background/background malam/)
    private Texture kamarKostMalamTexture;
    private Texture kostOutsideMalamTexture;
    private Texture jalanRayaMalamTexture;
    private Texture kedaiKopiMalamTexture;
    private Texture jalanSetapakMalamTexture;
    private Texture jalanDanauMalamTexture;
    private Texture luarRuangStudioMalamTexture;
    private Texture dalamStudioMalamTexture;
    private Texture ukmSeniMalamTexture;
    private Texture tamanKampusMalamTexture;
    private Texture kantinMalamTexture;
    private Texture lorong1MalamTexture;
    private Texture lorong2MalamTexture;
    private Texture studioMalamTexture;

    // PFP Avatars for Smartphone
    private Texture pfpGroup;
    private Texture pfpClara;
    private Texture pfpBagas;
    private Texture pfpSherly;
    private Texture pfpRania;

    // EXPLORATION_STATE: Sidescrolling character controller
    private Array<Texture> walkTextures;
    private Array<Texture> idleTextures;
    private Array<Texture> claraIdleTextures;
    private Array<Texture> bagasIdleTextures;

    private Animation<TextureRegion> walkAnimation;
    private Animation<TextureRegion> idleAnimation;
    private Animation<TextureRegion> claraIdleAnimation;
    private Animation<TextureRegion> raniaIdleAnimation;
    private Animation<TextureRegion> sherlyIdleAnimation;
    private Animation<TextureRegion> bagasIdleAnimation;
    private float raniaX = 300f;
    private float sherlyX = 500f;
    private float bagasX = 640f;
    private boolean nearRania = false;
    private boolean nearSherly = false;
    private boolean nearBagas = false;

    private float rakshaX = 100f;
    private float rakshaY = 120f;
    private float animationTime = 0f;
    private float claraAnimationTime = 0f;
    private boolean rakshaFacingRight = true;

    // Exploration zone system (each zone is a separate room with its own background)
    private enum ExplorationZone { KOST, KOST_OUTSIDE, WARKOP, TAMAN_KAMPUS, KANTIN, LORONG_1, LORONG_2, KAMPUS, KEDAI_KOPI, STUDIO_SENI, JALAN_SETAPAK, JALAN_DANAU, LUAR_RUANG_STUDIO, DALAM_STUDIO, UKM_MUSIK }
    private ExplorationZone currentZone = ExplorationZone.KOST;

    // Zone transition loading screen state
    private boolean isZoneTransitioning = false;
    private float zoneTransAlpha = 0f;       // 0=transparent, 1=fully black
    private boolean zoneTransFadingOut = true; // true=fade to black, false=fade in
    private ExplorationZone pendingZone = null;
    private String zoneTransLabel = "";
    private float zoneTransLabelAlpha = 0f;
    private float zoneTransTimer = 0f;
    private float interactionCooldown = 0f;

    // Interaction zone flags (set each frame, used to drive E-key prompts)
    private boolean nearBed = false;
    private boolean nearClara = false;
    private boolean nearKostGate = false;
    private boolean nearJalanTanah = false;
    private boolean nearStudioDoor = false;
    private boolean nearStudioExit = false;
    private boolean nearKantinDoor = false;
    private boolean nearKantinExit = false;
    private boolean nearUkmMusikDoor = false;
    private boolean nearUkmMusikExit = false;
    private boolean nearUkmSeniDoor = false;
    private boolean nearStudioSeniExit = false;
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
    private String rakshaExpression = "MAHASISWA_biasa aja";
    private String claraExpression = "MAHASISWA_biasa aja";
    private String sherlyExpression = "MAHASISWA_biasa aja";
    private String raniaExpression = "MAHASISWA_biasa aja";
    private String bagasExpression = "MAHASISWA_biasa aja";
    private String activeRightCharacter = "None";
    private Map<String, Map<String, Texture>> expressionTextures;
    private Map<String, Texture> backgroundMap = new HashMap<>();

    // Movie Credits System
    private static class CreditSection {
        public String header;
        public Array<String> names = new Array<>();
    }
    private String creditsTitle = "ECHO SUMMER";
    private String creditsSubtitle = "";
    private String creditsClosingQuote = "";
    private String creditsCopyright = "";
    private Array<CreditSection> creditsList = new Array<>();
    private float creditsScrollY = -100f;
    private boolean creditsLoaded = false;

    // Save Game state
    private String saveFileName = "savegame.dat";
    private boolean isSaveOverlayActive = false;
    private float saveSuccessTimer = 0f;
    private int savedSlotNum = 1;

    // RHYTHM_STATE: Engine
    private RhythmGame rhythmGame;
    private Music rhythmMusic;
    private Music creditsMusic;
    private boolean rhythmFromBandPractice = false;

    // Data Structure Interactive UI Overlay States
    private boolean isDialogueHistoryActive = false;
    private boolean isMapGraphActive = false;
    private boolean isInventoryActive = false;

    // Data Structure Core Engine Members
    private final CustomStack<DialogueNode> dialogueHistoryStack = new CustomStack<>();
    private final LocationGraph<ExplorationZone> mapNavigationGraph = new LocationGraph<>();
    private final CustomLinkedList<String> activeQuestList = new CustomLinkedList<>();
    private CustomHashTable<String, DialogueNode> storyNodeHashTable;
    private CustomTree<DialogueNode> storyDecisionTree;

    private void initMapNavigationGraph() {
        for (ExplorationZone zone : ExplorationZone.values()) {
            mapNavigationGraph.addVertex(zone);
        }
        mapNavigationGraph.addEdge(ExplorationZone.KOST, ExplorationZone.KOST_OUTSIDE);
        mapNavigationGraph.addEdge(ExplorationZone.KOST_OUTSIDE, ExplorationZone.WARKOP);
        mapNavigationGraph.addEdge(ExplorationZone.WARKOP, ExplorationZone.KAMPUS);
        mapNavigationGraph.addEdge(ExplorationZone.KAMPUS, ExplorationZone.TAMAN_KAMPUS);
        mapNavigationGraph.addEdge(ExplorationZone.TAMAN_KAMPUS, ExplorationZone.KANTIN);
        mapNavigationGraph.addEdge(ExplorationZone.KANTIN, ExplorationZone.LORONG_1);
        mapNavigationGraph.addEdge(ExplorationZone.LORONG_1, ExplorationZone.LORONG_2);
        mapNavigationGraph.addEdge(ExplorationZone.LORONG_2, ExplorationZone.LUAR_RUANG_STUDIO);
        mapNavigationGraph.addEdge(ExplorationZone.LUAR_RUANG_STUDIO, ExplorationZone.DALAM_STUDIO);
        mapNavigationGraph.addEdge(ExplorationZone.DALAM_STUDIO, ExplorationZone.UKM_MUSIK);
        mapNavigationGraph.addEdge(ExplorationZone.KAMPUS, ExplorationZone.STUDIO_SENI);
        mapNavigationGraph.addEdge(ExplorationZone.WARKOP, ExplorationZone.KEDAI_KOPI);
        mapNavigationGraph.addEdge(ExplorationZone.KAMPUS, ExplorationZone.JALAN_SETAPAK);
        mapNavigationGraph.addEdge(ExplorationZone.JALAN_SETAPAK, ExplorationZone.JALAN_DANAU);
    }

    private boolean isPracticeDay(int day) {
        return day == 20 || day == 19 || day == 18 || day == 15 || day == 12;
    }

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
        this.saveFileName = "savegame.dat";
    }

    public GameplayScreen(Main game, boolean isLoadGame, String saveFileName) {
        this.game = game;
        this.isLoadGame = isLoadGame;
        this.saveFileName = saveFileName;
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
        studioTexture = new Texture(Gdx.files.internal("background/background_ukm_musik.jpg"));
        studioTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        skyTexture = new Texture(Gdx.files.internal("background.png"));
        skyTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        kamarKostTexture = new Texture(Gdx.files.internal("background/background_kamar_kost.png"));
        kamarKostTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        kostOutsideTexture = new Texture(Gdx.files.internal("background/background_kost_outside.jpg"));
        kostOutsideTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        jalanRayaTexture = new Texture(Gdx.files.internal("background/background_jalan_raya.png"));
        jalanRayaTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        kedaiKopiTexture = new Texture(Gdx.files.internal("background/background_kedai_kopi.png"));
        kedaiKopiTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        jalanSetapakTexture = new Texture(Gdx.files.internal("background/background_jalan_setapak.png"));
        jalanSetapakTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        jalanDanauTexture = new Texture(Gdx.files.internal("background/background_jalan_danau.png"));
        jalanDanauTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        luarRuangStudioTexture = new Texture(Gdx.files.internal("background/background_luar_ruang_studio.png"));
        luarRuangStudioTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        dalamStudioTexture = new Texture(Gdx.files.internal("background/background_dalam_studio.png"));
        dalamStudioTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        ukmSeniTexture = new Texture(Gdx.files.internal("background/background_ukm_seni.png"));
        ukmSeniTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        tamanKampusTexture = new Texture(Gdx.files.internal("background/background_taman_kampus.png"));
        tamanKampusTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        kantinTexture = new Texture(Gdx.files.internal("background/background_kantin.png"));
        kantinTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        lorong1Texture = new Texture(Gdx.files.internal("background/background_lorong_1.png"));
        lorong1Texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        lorong2Texture = new Texture(Gdx.files.internal("background/background_lorong_2.png"));
        lorong2Texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        // Load night backgrounds from background/background malam/
        kamarKostMalamTexture = new Texture(Gdx.files.internal("background/background malam/background_kamar_kost_malam.png"));
        kamarKostMalamTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        kostOutsideMalamTexture = new Texture(Gdx.files.internal("background/background malam/background_kost_outside_malam.png"));
        kostOutsideMalamTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        jalanRayaMalamTexture = new Texture(Gdx.files.internal("background/background malam/background_jalan_raya_malam.png"));
        jalanRayaMalamTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        kedaiKopiMalamTexture = new Texture(Gdx.files.internal("background/background malam/background_kedai_kopi_malam.png"));
        kedaiKopiMalamTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        jalanSetapakMalamTexture = new Texture(Gdx.files.internal("background/background malam/background_jalan_setapak.png"));
        jalanSetapakMalamTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        jalanDanauMalamTexture = new Texture(Gdx.files.internal("background/background malam/background_jalan_danau_malam.png"));
        jalanDanauMalamTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        luarRuangStudioMalamTexture = new Texture(Gdx.files.internal("background/background malam/background_luar_ruang_studio_malam.png"));
        luarRuangStudioMalamTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        dalamStudioMalamTexture = new Texture(Gdx.files.internal("background/background malam/background_dalam_studio_malam.png"));
        dalamStudioMalamTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        ukmSeniMalamTexture = new Texture(Gdx.files.internal("background/background malam/background_ukm_seni_malam.png"));
        ukmSeniMalamTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        tamanKampusMalamTexture = new Texture(Gdx.files.internal("background/background malam/background_taman_kampus.png"));
        tamanKampusMalamTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        kantinMalamTexture = new Texture(Gdx.files.internal("background/background malam/background_kantin_malam.png"));
        kantinMalamTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        lorong1MalamTexture = new Texture(Gdx.files.internal("background/background malam/background_lorong_1_malam.png"));
        lorong1MalamTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        lorong2MalamTexture = new Texture(Gdx.files.internal("background/background malam/background_lorong_2_malam.png"));
        lorong2MalamTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        studioMalamTexture = new Texture(Gdx.files.internal("background/background malam/background_ukm_musik_malam.png"));
        studioMalamTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        try {
            if (Gdx.files.internal("pfp/pfp_group.jpg").exists()) pfpGroup = new Texture(Gdx.files.internal("pfp/pfp_group.jpg"));
            if (Gdx.files.internal("pfp/pfp_clara.jpg").exists()) pfpClara = new Texture(Gdx.files.internal("pfp/pfp_clara.jpg"));
            if (Gdx.files.internal("pfp/pfp_bagas.jpg").exists()) pfpBagas = new Texture(Gdx.files.internal("pfp/pfp_bagas.jpg"));
            if (Gdx.files.internal("pfp/pfp_sherly.jpg").exists()) pfpSherly = new Texture(Gdx.files.internal("pfp/pfp_sherly.jpg"));
            if (Gdx.files.internal("pfp/pfp_rania.jpg").exists()) pfpRania = new Texture(Gdx.files.internal("pfp/pfp_rania.jpg"));
        } catch (Exception e) {
            Gdx.app.error("GameplayScreen", "Failed loading PFP textures: " + e.getMessage());
        }

        // Load GameState
        if (isLoadGame) {
            gameState = SaveManager.loadGame(saveFileName);
            if (gameState.day <= 0) {
                gameState.day = 30;
            }
            syncChapterWithDay();
            remainingDays = gameState.day;
            updateTimeOfDay();
            
            // Restore loaded state
            state = GameplayState.valueOf(gameState.gameplayState);
            currentZone = ExplorationZone.valueOf(gameState.currentZone);
            rakshaX = gameState.rakshaX;
            
            // Sync background to loaded zone if in exploration
            if (state == GameplayState.EXPLORATION_STATE) {
                switch (currentZone) {
                    case KOST:          currentBackground = kamarKostTexture; break;
                    case KOST_OUTSIDE:  currentBackground = kostOutsideTexture; break;
                    case WARKOP:        currentBackground = jalanRayaTexture; break;
                    case TAMAN_KAMPUS:  currentBackground = tamanKampusTexture; break;
                    case KANTIN:        currentBackground = kantinTexture; break;
                    case LORONG_1:      currentBackground = lorong1Texture; break;
                    case LORONG_2:      currentBackground = lorong2Texture; break;
                    case KEDAI_KOPI:    currentBackground = kedaiKopiTexture; break;
                    case KAMPUS:        currentBackground = studioTexture; break;
                    case STUDIO_SENI:   currentBackground = ukmSeniTexture; break;
                    case JALAN_SETAPAK: currentBackground = jalanSetapakTexture; break;
                    case JALAN_DANAU:   currentBackground = jalanDanauTexture; break;
                    case LUAR_RUANG_STUDIO: currentBackground = luarRuangStudioTexture; break;
                    case DALAM_STUDIO:  currentBackground = dalamStudioTexture; break;
                    case UKM_MUSIK:     currentBackground = studioTexture; break;
                    default:            currentBackground = studioTexture; break;
                }
            }
        } else {
            gameState = new GameState();
            gameState.reset();
            SaveManager.saveGame(gameState, saveFileName);
        }

        // Initialize story graph
        storyNodes = StoryData.buildStory(gameState, new Runnable() {
            @Override
            public void run() {
                // onStartRhythmGame
                state = GameplayState.RHYTHM_STATE;
                rhythmMusic = Gdx.audio.newMusic(Gdx.files.internal("music/Tatap_Esok.mp3"));
                rhythmMusic.setVolume(SettingsManager.getVolume());
                rhythmGame.start(rhythmMusic, "LATIHAN BAND - \"TATAP ESOK\"");
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
        claraIdleAnimation.setPlayMode(Animation.PlayMode.LOOP);

        // Rania Idle
        Array<TextureRegion> raniaFrames = new Array<>();
        for (int i = 1; i <= 16; i++) {
            String path = "sprite/IDLE_RANIA/pose_" + String.format("%02d", i) + ".png";
            if (Gdx.files.internal(path).exists()) {
                Texture tex = new Texture(Gdx.files.internal(path));
                tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                raniaFrames.add(new TextureRegion(tex));
            }
        }
        if (raniaFrames.size > 0) {
            raniaIdleAnimation = new Animation<>(0.25f, raniaFrames);
            raniaIdleAnimation.setPlayMode(Animation.PlayMode.LOOP);
        } else {
            Gdx.app.error("GameplayScreen", "No rania idle frames found!");
        }

        // Sherly Idle
        Array<TextureRegion> sherlyFrames = new Array<>();
        for (int i = 1; i <= 16; i++) {
            String path = "sprite/IDLE_SHERLY/pose_" + String.format("%02d", i) + ".png";
            if (Gdx.files.internal(path).exists()) {
                Texture tex = new Texture(Gdx.files.internal(path));
                tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                sherlyFrames.add(new TextureRegion(tex));
            }
        }
        if (sherlyFrames.size > 0) {
            sherlyIdleAnimation = new Animation<>(0.4f, sherlyFrames);
            sherlyIdleAnimation.setPlayMode(Animation.PlayMode.LOOP);
        } else {
            Gdx.app.error("GameplayScreen", "No sherly idle frames found!");
        }

        // Bagas Idle
        bagasIdleTextures = new Array<>();
        Array<TextureRegion> bagasFrames = new Array<>();
        for (int i = 1; i <= 16; i++) {
            String path = String.format("sprite/IDLE_BAGAS/pose_%02d.png", i);
            if (Gdx.files.internal(path).exists()) {
                Texture tex = new Texture(Gdx.files.internal(path));
                tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                bagasIdleTextures.add(tex);
                bagasFrames.add(new TextureRegion(tex));
            }
        }
        if (bagasFrames.size > 0) {
            bagasIdleAnimation = new Animation<>(0.3f, bagasFrames);
            bagasIdleAnimation.setPlayMode(Animation.PlayMode.LOOP);
        } else {
            Gdx.app.error("GameplayScreen", "No bagas idle frames found!");
        }

        // Initialize Rhythm engine
        rhythmGame = new RhythmGame();

        // Load expression textures
        loadExpressionTextures();

        // Initialize Data Structure map graph & Story Data Structures
        initMapNavigationGraph();
        storyNodeHashTable = StoryData.buildStoryHashTable(
            gameState,
            new Runnable() { @Override public void run() { loadNode("START_CONCERT_RHYTHM_GAME"); } },
            new Runnable() { @Override public void run() { loadNode("END_TRUE_1"); } }
        );
        storyDecisionTree = StoryData.buildStoryTree(storyNodeHashTable);

        activeQuestList.clear();
        activeQuestList.add(getCurrentQuest());

        // Load starting node
        if (isLoadGame) {
            loadNodeBackgroundOnly(gameState.dialogueNodeId);
            if (state == GameplayState.DIALOGUE_STATE) {
                loadNode(gameState.dialogueNodeId);
            } else {
                currentNode = storyNodes.get(gameState.dialogueNodeId);
                if (currentNode != null && currentNode.nodeId != null && currentNode.nodeId.endsWith("_DAY_LOOP")) {
                    currentNode.text = "Hari ke-" + gameState.day + " menjelang festival.\nSisa uang: Rp" + gameState.money + ".\nBagaimana aku menghabiskan hari ini?";
                }
            }
        } else {
            loadNode(gameState.dialogueNodeId);
        }
    }

    private void loadNode(String nodeId) {
        if (nodeId == null || "EXPLORATION_MODE".equals(nodeId) || !storyNodes.containsKey(nodeId)) {
            if (nodeId != null && !"EXPLORATION_MODE".equals(nodeId) && !storyNodes.containsKey(nodeId)) {
                Gdx.app.error("GameplayScreen", "Dialogue node not found: " + nodeId);
            }
            state = GameplayState.EXPLORATION_STATE;
            interactionCooldown = 0.35f;
            currentNode = null;
            updateTimeOfDay();
            return;
        }

        if (currentNode != null) {
            dialogueHistoryStack.push(currentNode);
        }
        gameState.dialogueNodeId = nodeId;
        if (storyNodeHashTable != null && storyNodeHashTable.containsKey(nodeId)) {
            currentNode = storyNodeHashTable.get(nodeId);
        } else {
            currentNode = storyNodes.get(nodeId);
        }

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
        syncChapterWithDay();
        remainingDays = gameState.day;
        if (nodeId.equals("FREE_DATE_CLARA_END") || nodeId.equals("CH1_PRACTICE_END")) {
            gameState.freeDayEventDone = true;
        }
        updateTimeOfDay();

        // Determine background: Prologue / Endings / Bus cutscenes use fixed assets; all gameplay dialogues match MC's current zone!
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
        } else if (nodeId.startsWith("END_TRUE_2")) {
            currentBackground = jalanRayaTexture;
        } else if (nodeId.startsWith("END_TRUE_")) {
            currentBackground = kantinTexture;
        } else if (nodeId.startsWith("END_COMEDY_")) {
            currentBackground = kantinTexture;
        } else if (nodeId.startsWith("END_WORST_")) {
            currentBackground = kamarKostTexture;
        } else if (nodeId.startsWith("END_") || nodeId.startsWith("CREDITS_") || nodeId.equals("GAME_OVER")) {
            currentBackground = skyTexture;
        } else if (nodeId.startsWith("CH3_BUS_")) {
            currentBackground = jalanRayaTexture;
        } else if (nodeId.startsWith("CH3_ALDO_") || nodeId.startsWith("CH3_RUMOR_")) {
            currentBackground = studioTexture;
        } else {
            // Guarantee background always matches MC's current location during gameplay/exploration!
            Texture zoneBg = getZoneTexture(currentZone);
            if (zoneBg != null) {
                currentBackground = zoneBg;
            } else {
                Texture jsonBg = currentNode != null ? getBackgroundTexture(currentNode.background) : null;
                currentBackground = jsonBg != null ? jsonBg : kamarKostTexture;
            }
        }

        if (nodeId.startsWith("CREDITS_") || nodeId.equals("GAME_OVER")) {
            creditsScrollY = -100f;
        }

        if (currentBackground == kamarKostTexture || currentBackground == kamarKostMalamTexture || currentBackground == kostOutsideTexture || currentBackground == kostOutsideMalamTexture || (nodeId != null && (nodeId.startsWith("BLOCK_") || nodeId.startsWith("PHONE_")))) {
            activeRightCharacter = "None";
        } else if (nodeId.contains("SHERLY")) {
            activeRightCharacter = "Sherly";
        } else if (nodeId.contains("RANIA")) {
            activeRightCharacter = "Rania";
        } else if (nodeId.contains("BAGAS")) {
            activeRightCharacter = "Bagas";
        } else if (nodeId.contains("CLARA") || nodeId.startsWith("PROLOG_") || currentZone == ExplorationZone.DALAM_STUDIO || currentZone == ExplorationZone.UKM_MUSIK || currentZone == ExplorationZone.KAMPUS) {
            activeRightCharacter = "Clara";
        } else {
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
            currentBackground = kamarKostTexture;
            rakshaX = 100f;
            rakshaFacingRight = true;
            isZoneTransitioning = false;
            currentNode.text = "Hari ke-" + gameState.day + " menjelang festival.\nSisa uang: Rp" + gameState.money + ".\nBagaimana aku menghabiskan hari ini?";
            triggerAutoSave();
        } else if (nodeId.equals("CH2_DAY_LOOP")) {
            state = GameplayState.EXPLORATION_STATE;
            currentZone = ExplorationZone.KOST;
            currentBackground = kamarKostTexture;
            rakshaX = 100f;
            rakshaFacingRight = true;
            isZoneTransitioning = false;
            triggerAutoSave();
        } else if (nodeId.equals("CH3_DAY_LOOP")) {
            state = GameplayState.EXPLORATION_STATE;
            currentZone = ExplorationZone.KOST;
            currentBackground = kamarKostTexture;
            rakshaX = 100f;
            rakshaFacingRight = true;
            isZoneTransitioning = false;
            triggerAutoSave();
        } else if (nodeId.equals("CH4_DAY_LOOP")) {
            state = GameplayState.EXPLORATION_STATE;
            currentZone = ExplorationZone.KOST;
            currentBackground = kamarKostTexture;
            rakshaX = 100f;
            rakshaFacingRight = true;
            isZoneTransitioning = false;
            triggerAutoSave();
        } else {
            if (state == GameplayState.EXPLORATION_STATE) {
                activeRightCharacter = "None";
            }
            state = GameplayState.DIALOGUE_STATE;
        }

        // Separate speaker name and process dialogue text before typewriter starts
        String rawText = getProcessedNodeText();
        String speaker = currentNode.speaker;
        String text = rawText;

        if (speaker == null || speaker.isEmpty()) {
            if (rawText.contains(":")) {
                int colonIdx = rawText.indexOf(":");
                String candidateSpeaker = rawText.substring(0, colonIdx).trim();
                // Ensure candidate speaker contains no linebreaks and is short to avoid treating narrative introduction lines as speaker name
                if (!candidateSpeaker.contains("\n") && candidateSpeaker.length() < 30) {
                    speaker = candidateSpeaker;
                    text = rawText.substring(colonIdx + 1).trim();
                }
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
            triggerAutoSave();
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

    private void syncChapterWithDay() {
        if (gameState == null) return;
        if (gameState.day <= 20 && gameState.day >= 10 && "CHAPTER_1".equals(gameState.chapter)) {
            gameState.chapter = "CHAPTER_2";
        } else if (gameState.day <= 9 && gameState.day >= 1 && ("CHAPTER_1".equals(gameState.chapter) || "CHAPTER_2".equals(gameState.chapter))) {
            gameState.chapter = "CHAPTER_3";
        } else if (gameState.day <= 0 || "CHAPTER_4".equals(gameState.chapter)) {
            gameState.chapter = "CHAPTER_4";
            if (gameState.day < 1) gameState.day = 1;
        }
    }

    private boolean isCurrentDayEventCompleted() {
        if (gameState == null) return false;
        if (gameState.day == 28) return gameState.day28EventDone;
        if (gameState.day == 26) return gameState.day26EventDone;
        if (gameState.day == 24) return gameState.day24EventDone;
        if (gameState.day == 20 || gameState.day == 18 || gameState.day == 15 || gameState.day == 12) return gameState.ch2CompositionDone;
        if (gameState.day == 10 || gameState.day == 8 || gameState.day == 7 || gameState.day == 5 || gameState.day == 3) return gameState.ch3AldoDone;
        return gameState.freeDayEventDone;
    }

    private void updateTimeOfDay() {
        if (gameState == null) return;
        syncChapterWithDay();
        if (gameState.chapter.equals("PROLOGUE")) {
            timeOfDay = "SORE";
        } else if (gameState.chapter.equals("CHAPTER_4")) {
            timeOfDay = "MALAM";
        } else {
            timeOfDay = isCurrentDayEventCompleted() ? "MALAM" : "SORE";
        }
    }

    private Texture getZoneTexture(ExplorationZone zone) {
        boolean isNight = "MALAM".equals(timeOfDay);
        switch (zone) {
            case KOST:              return isNight ? kamarKostMalamTexture : kamarKostTexture;
            case KOST_OUTSIDE:      return isNight ? kostOutsideMalamTexture : kostOutsideTexture;
            case WARKOP:            return isNight ? jalanRayaMalamTexture : jalanRayaTexture;
            case TAMAN_KAMPUS:      return isNight ? tamanKampusMalamTexture : tamanKampusTexture;
            case KANTIN:            return isNight ? kantinMalamTexture : kantinTexture;
            case LORONG_1:          return isNight ? lorong1MalamTexture : lorong1Texture;
            case LORONG_2:          return isNight ? lorong2MalamTexture : lorong2Texture;
            case KEDAI_KOPI:        return isNight ? kedaiKopiMalamTexture : kedaiKopiTexture;
            case STUDIO_SENI:       return isNight ? ukmSeniMalamTexture : ukmSeniTexture;
            case JALAN_SETAPAK:     return isNight ? jalanSetapakMalamTexture : jalanSetapakTexture;
            case JALAN_DANAU:       return isNight ? jalanDanauMalamTexture : jalanDanauTexture;
            case LUAR_RUANG_STUDIO: return isNight ? luarRuangStudioMalamTexture : luarRuangStudioTexture;
            case DALAM_STUDIO:      return isNight ? dalamStudioMalamTexture : dalamStudioTexture;
            case UKM_MUSIK:         return isNight ? studioMalamTexture : studioTexture;
            default:                return isNight ? studioMalamTexture : studioTexture;
        }
    }

    private Texture getBackgroundTexture(String path) {
        if (path == null || path.isEmpty()) return null;
        boolean isNight = "MALAM".equals(timeOfDay);
        if (isNight) {
            if (path.contains("kamar_kost")) return kamarKostMalamTexture;
            if (path.contains("kost_outside")) return kostOutsideMalamTexture;
            if (path.contains("jalan_raya")) return jalanRayaMalamTexture;
            if (path.contains("kedai_kopi")) return kedaiKopiMalamTexture;
            if (path.contains("jalan_setapak")) return jalanSetapakMalamTexture;
            if (path.contains("jalan_danau")) return jalanDanauMalamTexture;
            if (path.contains("luar_ruang_studio")) return luarRuangStudioMalamTexture;
            if (path.contains("dalam_studio")) return dalamStudioMalamTexture;
            if (path.contains("ukm_seni")) return ukmSeniMalamTexture;
            if (path.contains("taman_kampus")) return tamanKampusMalamTexture;
            if (path.contains("kantin")) return kantinMalamTexture;
            if (path.contains("lorong_1")) return lorong1MalamTexture;
            if (path.contains("lorong_2")) return lorong2MalamTexture;
            if (path.contains("ukm_musik")) return studioMalamTexture;
        }
        if (path.contains("kamar_kost")) return kamarKostTexture;
        if (path.contains("kost_outside")) return kostOutsideTexture;
        if (path.contains("jalan_raya")) return jalanRayaTexture;
        if (path.contains("kedai_kopi")) return kedaiKopiTexture;
        if (path.contains("jalan_setapak")) return jalanSetapakTexture;
        if (path.contains("jalan_danau")) return jalanDanauTexture;
        if (path.contains("luar_ruang_studio")) return luarRuangStudioTexture;
        if (path.contains("dalam_studio")) return dalamStudioTexture;
        if (path.contains("ukm_seni")) return ukmSeniTexture;
        if (path.contains("taman_kampus")) return tamanKampusTexture;
        if (path.contains("kantin")) return kantinTexture;
        if (path.contains("lorong_1")) return lorong1Texture;
        if (path.contains("lorong_2")) return lorong2Texture;
        if (path.contains("ukm_musik")) return studioTexture;
        return null;
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
            if (saveSuccessTimer > 0) {
                saveSuccessTimer -= delta;
            }
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
            if (isSaveOverlayActive) {
                renderSaveOverlay();
            } else {
                renderPauseMenu();
            }
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
        shapeRenderer.rect(bx, my + 95,  btnW, 45);   // Save Game
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
        choiceFont.draw(game.getBatch(), "Simpan Permainan", bx, my + 95 + 32, btnW, Align.center, false);
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

    // Phone sub-screen: 0 = app list, 1 = Echo Summer group chat
    private int phoneChatScroll = 0;

    private void renderPhoneMenu() {
        syncChapterWithDay();
        boolean isCh2 = "CHAPTER_2".equals(gameState.chapter) || (gameState.day <= 20 && gameState.day >= 10);
        SpriteBatch batch = game.getBatch();

        // ── Dim background ──────────────────────────────────────────
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.70f);
        shapeRenderer.rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        // ── Phone Body Dimensions ───────────────────────────────────
        float phoneW = 360f;
        float phoneH = 620f;
        float px = (VIRTUAL_WIDTH - phoneW) / 2f;
        float py = (VIRTUAL_HEIGHT - phoneH) / 2f;

        // Phone Shell (Dark Slate)
        shapeRenderer.setColor(0.11f, 0.13f, 0.16f, 1f);
        shapeRenderer.rect(px - 7, py - 7, phoneW + 14, phoneH + 14);
        
        // Screen Background (WhatsApp Dark Mode: #111B21)
        shapeRenderer.setColor(0.07f, 0.11f, 0.13f, 1f); // #111B21
        shapeRenderer.rect(px, py, phoneW, phoneH);

        // WA Theme Colors
        Color waTopBar = new Color(0.12f, 0.17f, 0.20f, 1f);       // #1F2C34
        Color waDarkCard = new Color(0.09f, 0.13f, 0.16f, 1f);     // #162127
        Color waAccentGreen = new Color(0.00f, 0.66f, 0.52f, 1f);  // #00A884
        Color bubbleIncoming = new Color(0.13f, 0.17f, 0.20f, 1f); // #202C33
        Color bubbleOutgoing = new Color(0.00f, 0.36f, 0.29f, 1f); // #005C4B
        Color choiceBtnColor = new Color(0.12f, 0.22f, 0.26f, 1f);

        float cardH = 62f;
        float startCardY = py + phoneH - 85f - cardH;

        if (phoneScreen == 0) {
            // ── WhatsApp Contact List Screen ─────────────────────────
            // Top Status Bar
            shapeRenderer.setColor(0.05f, 0.08f, 0.10f, 1f);
            shapeRenderer.rect(px, py + phoneH - 30, phoneW, 30);

            // WhatsApp App Header Bar
            shapeRenderer.setColor(waTopBar);
            shapeRenderer.rect(px, py + phoneH - 75, phoneW, 45);

            // 5 Contact Card Backgrounds
            for (int i = 0; i < 5; i++) {
                float cardY = startCardY - i * (cardH + 6f);
                shapeRenderer.setColor(waDarkCard);
                shapeRenderer.rect(px + 8, cardY, phoneW - 16, cardH);
            }

            // Unread badge for Clara if not replied today
            if (gameState.claraChatDayReplied != gameState.day) {
                float cardYClara = startCardY - 1 * (cardH + 6f);
                shapeRenderer.setColor(waAccentGreen);
                shapeRenderer.rect(px + phoneW - 35, cardYClara + 20, 20, 20);
            }

        } else {
            // ── Chat Screen (Group / Personal) ──────────────────────
            // WhatsApp Header Bar (#1F2C34)
            shapeRenderer.setColor(waTopBar);
            shapeRenderer.rect(px, py + phoneH - 55, phoneW, 55);

            // Back Arrow box
            shapeRenderer.setColor(0.08f, 0.12f, 0.15f, 1f);
            shapeRenderer.rect(px + 4, py + phoneH - 46, 36, 38);

            // Chat Wallpaper Background (#0B141A)
            shapeRenderer.setColor(0.04f, 0.08f, 0.10f, 1f);
            shapeRenderer.rect(px, py + 55, phoneW, phoneH - 110);

            // Bottom Input Bar
            shapeRenderer.setColor(0.07f, 0.11f, 0.13f, 1f);
            shapeRenderer.rect(px, py, phoneW, 55);

            float bW = phoneW - 40f;
            float topY = py + phoneH - 55f - 15f;

            if (phoneScreen == 1) {
                // Group Chat Bubbles
                float bH = 44f;
                float bPad = 10f;
                float bY1 = topY - bH;
                float bY2 = bY1 - (bH + bPad);
                float bY3 = bY2 - (bH + bPad);
                float bY4 = bY3 - (bH + bPad);
                float bY5 = bY4 - (bH + bPad);

                if (isCh2) {
                    shapeRenderer.setColor(bubbleIncoming);
                    shapeRenderer.rect(px + 14, bY1, bW, bH);
                    shapeRenderer.rect(px + 14, bY2, bW, bH);
                    shapeRenderer.rect(px + 14, bY3, bW, bH);
                    shapeRenderer.rect(px + 14, bY4, bW, bH);

                    if (gameState.ch2ChatRead) {
                        shapeRenderer.setColor(bubbleOutgoing);
                        shapeRenderer.rect(px + 14, bY5, bW, bH);
                    } else {
                        shapeRenderer.setColor(waAccentGreen);
                        shapeRenderer.rect(px + phoneW / 2f - 70f, py + 10, 140f, 38f);
                    }
                } else {
                    shapeRenderer.setColor(bubbleIncoming);
                    shapeRenderer.rect(px + 14, bY1, bW, bH);
                }

            } else if (phoneScreen == 2) {
                // Clara Bucin Chat
                shapeRenderer.setColor(bubbleIncoming);
                shapeRenderer.rect(px + 14, topY - 110f, bW, 110f);

                if (gameState.claraChatDayReplied == gameState.day) {
                    shapeRenderer.setColor(bubbleOutgoing);
                    shapeRenderer.rect(px + 14, topY - 180f, bW, 60f);

                    shapeRenderer.setColor(bubbleIncoming);
                    shapeRenderer.rect(px + 14, topY - 290f, bW, 95f);
                } else {
                    // Choice Option Cards
                    shapeRenderer.setColor(choiceBtnColor);
                    shapeRenderer.rect(px + 12, py + 70, phoneW - 24, 48f);
                    shapeRenderer.rect(px + 12, py + 15, phoneW - 24, 48f);
                }

            } else if (phoneScreen == 3) {
                // Bagas Chat
                shapeRenderer.setColor(bubbleIncoming);
                shapeRenderer.rect(px + 14, topY - 70f, bW, 70f);
                shapeRenderer.setColor(bubbleOutgoing);
                shapeRenderer.rect(px + 14, topY - 150f, bW, 70f);

            } else if (phoneScreen == 4) {
                // Sherly Chat
                shapeRenderer.setColor(bubbleIncoming);
                shapeRenderer.rect(px + 14, topY - 80f, bW, 80f);
                shapeRenderer.setColor(bubbleOutgoing);
                shapeRenderer.rect(px + 14, topY - 160f, bW, 70f);

            } else if (phoneScreen == 5) {
                // Rania Chat
                shapeRenderer.setColor(bubbleIncoming);
                shapeRenderer.rect(px + 14, topY - 80f, bW, 80f);
                shapeRenderer.setColor(bubbleOutgoing);
                shapeRenderer.rect(px + 14, topY - 160f, bW, 70f);
            }
        }

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // ── Text & Image Layer ───────────────────────────────────────
        batch.begin();

        if (phoneScreen == 0) {
            // Status bar
            choiceFont.getData().setScale(0.80f);
            choiceFont.setColor(Color.WHITE);
            choiceFont.draw(batch, "10:45", px + phoneW - 55, py + phoneH - 8);

            // WhatsApp App Title
            choiceFont.getData().setScale(0.95f);
            choiceFont.setColor(waAccentGreen);
            choiceFont.draw(batch, "WhatsApp", px + 15, py + phoneH - 44);

            // Row 0: Group Chat
            float y0 = startCardY;
            if (pfpGroup != null) batch.draw(pfpGroup, px + 16, y0 + 8, 46f, 46f);
            choiceFont.getData().setScale(0.78f);
            choiceFont.setColor(Color.WHITE);
            choiceFont.draw(batch, "Echo Summer (Grup)", px + 70, y0 + 52);
            choiceFont.getData().setScale(0.58f);
            choiceFont.setColor(Color.LIGHT_GRAY);
            choiceFont.draw(batch, "Bagas, Rania, Clara, Sherly, Raksha", px + 70, y0 + 26);
            choiceFont.draw(batch, "10:42", px + phoneW - 48, y0 + 52);

            // Row 1: Clara Chat (Bucin)
            float y1 = startCardY - 1 * (cardH + 6f);
            if (pfpClara != null) batch.draw(pfpClara, px + 16, y1 + 8, 46f, 46f);
            choiceFont.getData().setScale(0.78f);
            choiceFont.setColor(new Color(1f, 0.75f, 0.85f, 1f));
            choiceFont.draw(batch, "Clara ❤️", px + 70, y1 + 52);
            choiceFont.getData().setScale(0.58f);
            choiceFont.setColor(Color.WHITE);
            choiceFont.draw(batch, truncateSingleLine(getClaraDailyChatMsg(), 28), px + 70, y1 + 26);
            choiceFont.setColor(Color.LIGHT_GRAY);
            choiceFont.draw(batch, "10:44", px + phoneW - 48, y1 + 52);

            if (gameState.claraChatDayReplied != gameState.day) {
                choiceFont.getData().setScale(0.60f);
                choiceFont.setColor(Color.WHITE);
                choiceFont.draw(batch, "1", px + phoneW - 29, y1 + 35);
            }

            // Row 2: Bagas Chat
            float y2 = startCardY - 2 * (cardH + 6f);
            if (pfpBagas != null) batch.draw(pfpBagas, px + 16, y2 + 8, 46f, 46f);
            choiceFont.getData().setScale(0.78f);
            choiceFont.setColor(new Color(0.6f, 0.8f, 1f, 1f));
            choiceFont.draw(batch, "Bagas 🥁", px + 70, y2 + 52);
            choiceFont.getData().setScale(0.58f);
            choiceFont.setColor(Color.LIGHT_GRAY);
            choiceFont.draw(batch, "Gas pol latihan drum berikutnya!", px + 70, y2 + 26);
            choiceFont.draw(batch, "09:30", px + phoneW - 48, y2 + 52);

            // Row 3: Sherly Chat
            float y3 = startCardY - 3 * (cardH + 6f);
            if (pfpSherly != null) batch.draw(pfpSherly, px + 16, y3 + 8, 46f, 46f);
            choiceFont.getData().setScale(0.78f);
            choiceFont.setColor(new Color(0.85f, 0.6f, 1f, 1f));
            choiceFont.draw(batch, "Sherly 📋", px + 70, y3 + 52);
            choiceFont.getData().setScale(0.58f);
            choiceFont.setColor(Color.LIGHT_GRAY);
            choiceFont.draw(batch, "Jadwal promo sosmed & studio beres", px + 70, y3 + 26);
            choiceFont.draw(batch, "Kemarin", px + phoneW - 55, y3 + 52);

            // Row 4: Rania Chat
            float y4 = startCardY - 4 * (cardH + 6f);
            if (pfpRania != null) batch.draw(pfpRania, px + 16, y4 + 8, 46f, 46f);
            choiceFont.getData().setScale(0.78f);
            choiceFont.setColor(new Color(1f, 0.6f, 0.75f, 1f));
            choiceFont.draw(batch, "Rania 🎨", px + 70, y4 + 52);
            choiceFont.getData().setScale(0.58f);
            choiceFont.setColor(Color.LIGHT_GRAY);
            choiceFont.draw(batch, "Partitur & ilustrasi lukisan baru", px + 70, y4 + 26);
            choiceFont.draw(batch, "Kemarin", px + phoneW - 55, y4 + 52);

            choiceFont.getData().setScale(0.66f);
            choiceFont.setColor(Color.LIGHT_GRAY);
            choiceFont.draw(batch, "[Klik Kontak] Buka Chat   [TAB/ESC] Tutup", px + 12, py + 18);

        } else {
            // ── Single & Group Chat View Headers & Messages ──
            Texture currentPfp = pfpGroup;
            String headerTitle = "Echo Summer";
            String headerSub = "Bagas, Rania, Clara, Sherly, Raksha";

            if (phoneScreen == 2) {
                currentPfp = pfpClara;
                headerTitle = "Clara ❤️";
                headerSub = "Online - Bucin Mode 💕";
            } else if (phoneScreen == 3) {
                currentPfp = pfpBagas;
                headerTitle = "Bagas 🥁";
                headerSub = "Online - Drummer Echo Summer";
            } else if (phoneScreen == 4) {
                currentPfp = pfpSherly;
                headerTitle = "Sherly 📋";
                headerSub = "Online - Manager Echo Summer";
            } else if (phoneScreen == 5) {
                currentPfp = pfpRania;
                headerTitle = "Rania 🎨";
                headerSub = "Online - Bassist Echo Summer";
            }

            // Top Header: Back Arrow & PFP
            choiceFont.getData().setScale(0.85f);
            choiceFont.setColor(Color.WHITE);
            choiceFont.draw(batch, "<", px + 14, py + phoneH - 18);

            if (currentPfp != null) {
                batch.draw(currentPfp, px + 44, py + phoneH - 48, 38f, 38f);
            }

            choiceFont.getData().setScale(0.80f);
            choiceFont.setColor(Color.WHITE);
            choiceFont.draw(batch, headerTitle, px + 90, py + phoneH - 14);
            choiceFont.getData().setScale(0.58f);
            choiceFont.setColor(waAccentGreen);
            choiceFont.draw(batch, headerSub, px + 90, py + phoneH - 34);

            float topY = py + phoneH - 55f - 15f;

            if (phoneScreen == 1) {
                float bW = phoneW - 80f;
                float bH = 44f;
                float bPad = 10f;
                float bY1 = topY - bH;
                float bY2 = bY1 - (bH + bPad);
                float bY3 = bY2 - (bH + bPad);
                float bY4 = bY3 - (bH + bPad);
                float bY5 = bY4 - (bH + bPad);

                choiceFont.getData().setScale(0.72f);
                if (isCh2) {
                    choiceFont.setColor(new Color(0.5f, 0.75f, 1f, 1f));
                    choiceFont.draw(batch, "Bagas:", px + 20, bY1 + bH - 6);
                    choiceFont.setColor(Color.WHITE);
                    choiceFont.draw(batch, "Oi! Latihan bareng yuk di studio hari ini?", px + 20, bY1 + bH - 24);

                    choiceFont.setColor(new Color(1f, 0.75f, 0.85f, 1f));
                    choiceFont.draw(batch, "Rania:", px + 20, bY2 + bH - 6);
                    choiceFont.setColor(Color.WHITE);
                    choiceFont.draw(batch, "Siapp! Aku udah di sana dari tadi hehe", px + 20, bY2 + bH - 24);

                    choiceFont.setColor(new Color(1f, 0.9f, 0.5f, 1f));
                    choiceFont.draw(batch, "Clara:", px + 20, bY3 + bH - 6);
                    choiceFont.setColor(Color.WHITE);
                    choiceFont.draw(batch, "Otw! 5 menit lagi nyampe~", px + 20, bY3 + bH - 24);

                    choiceFont.setColor(new Color(0.85f, 0.5f, 1f, 1f));
                    choiceFont.draw(batch, "Sherly:", px + 20, bY4 + bH - 6);
                    choiceFont.setColor(Color.WHITE);
                    choiceFont.draw(batch, "Jangan lupa partitur nya, gue periksa!", px + 20, bY4 + bH - 24);

                    if (gameState.ch2ChatRead) {
                        choiceFont.setColor(new Color(0.5f, 1f, 0.6f, 1f));
                        choiceFont.draw(batch, "Raksha (kamu):", px + 20, bY5 + bH - 6);
                        choiceFont.setColor(Color.WHITE);
                        choiceFont.draw(batch, "Siap semua! Otw studio sekarang! ✓✓", px + 20, bY5 + bH - 24);

                        choiceFont.getData().setScale(0.62f);
                        choiceFont.setColor(Color.LIGHT_GRAY);
                        choiceFont.draw(batch, "[Top Bar / ESC] Kembali ke Kontak", px + 14, py + 20);
                    } else {
                        choiceFont.getData().setScale(0.78f);
                        choiceFont.setColor(Color.WHITE);
                        choiceFont.draw(batch, "Balas & Ke Studio!", px + phoneW / 2f - 70f, py + 38);
                        choiceFont.getData().setScale(0.62f);
                        choiceFont.setColor(Color.LIGHT_GRAY);
                        choiceFont.draw(batch, "[ENTER] Balas   [TAB/ESC] Tutup", px + 14, py + 14);
                    }
                } else {
                    choiceFont.setColor(Color.LIGHT_GRAY);
                    choiceFont.draw(batch, "Bagas: Gas latihan minggu depan!", px + 20, bY1 + bH - 14);
                    choiceFont.getData().setScale(0.62f);
                    choiceFont.setColor(Color.LIGHT_GRAY);
                    choiceFont.draw(batch, "[Top Bar / ESC] Kembali", px + 14, py + 14);
                }

            } else if (phoneScreen == 2) {
                // Clara Bucin Chat
                choiceFont.getData().setScale(0.68f);
                choiceFont.setColor(new Color(1f, 0.8f, 0.9f, 1f));
                choiceFont.draw(batch, "Clara ❤️:", px + 20, topY - 8);
                choiceFont.setColor(Color.WHITE);
                choiceFont.draw(batch, getClaraDailyChatMsg(), px + 20, topY - 28, phoneW - 50f, Align.left, true);

                if (gameState.claraChatDayReplied == gameState.day) {
                    // Raksha Reply
                    choiceFont.setColor(new Color(0.5f, 1f, 0.6f, 1f));
                    choiceFont.draw(batch, "Raksha (kamu):", px + 20, topY - 125f);
                    choiceFont.setColor(Color.WHITE);
                    String replyText = (gameState.claraChatChoiceIndex == 0) ? getClaraReplyOptionA() : getClaraReplyOptionB();
                    choiceFont.draw(batch, replyText + "  ✓✓", px + 20, topY - 145f, phoneW - 50f, Align.left, true);

                    // Clara Reaction
                    choiceFont.setColor(new Color(1f, 0.8f, 0.9f, 1f));
                    choiceFont.draw(batch, "Clara ❤️:", px + 20, topY - 200f);
                    choiceFont.setColor(Color.WHITE);
                    choiceFont.draw(batch, "Aaaa Raka beneran bikin salting bangeett! I love you so much ❤️❤️❤️\n(Hubungan Clara +2)", px + 20, topY - 220f, phoneW - 50f, Align.left, true);

                    choiceFont.getData().setScale(0.62f);
                    choiceFont.setColor(Color.LIGHT_GRAY);
                    choiceFont.draw(batch, "[Top Bar / ESC] Kembali ke Kontak", px + 14, py + 20);

                } else {
                    // Options
                    choiceFont.getData().setScale(0.65f);
                    choiceFont.setColor(waAccentGreen);
                    choiceFont.draw(batch, "Pilih Balasan Chat Clara:", px + 14, py + 130);

                    choiceFont.getData().setScale(0.60f);
                    choiceFont.setColor(Color.WHITE);
                    choiceFont.draw(batch, getClaraReplyOptionA(), px + 18, py + 102, phoneW - 36f, Align.left, true);
                    choiceFont.draw(batch, getClaraReplyOptionB(), px + 18, py + 47, phoneW - 36f, Align.left, true);

                    choiceFont.getData().setScale(0.62f);
                    choiceFont.setColor(Color.LIGHT_GRAY);
                    choiceFont.draw(batch, "[Klik Tombol] Balas Chat Clara ❤️", px + 14, py + 12);
                }

            } else if (phoneScreen == 3) {
                // Bagas Chat
                choiceFont.getData().setScale(0.68f);
                choiceFont.setColor(new Color(0.6f, 0.8f, 1f, 1f));
                choiceFont.draw(batch, "Bagas:", px + 20, topY - 8);
                choiceFont.setColor(Color.WHITE);
                choiceFont.draw(batch, "Oi Raka! Tempo drum kemarin makin rapi kan? Gas pol latihan berikutnya bro!", px + 20, topY - 28, phoneW - 50f, Align.left, true);

                choiceFont.setColor(new Color(0.5f, 1f, 0.6f, 1f));
                choiceFont.draw(batch, "Raksha (kamu):", px + 20, topY - 88f);
                choiceFont.setColor(Color.WHITE);
                choiceFont.draw(batch, "Mantap Gas! Tempo lu udah solid banget, siap bantai panggung!  ✓✓", px + 20, topY - 108f, phoneW - 50f, Align.left, true);

                choiceFont.getData().setScale(0.62f);
                choiceFont.setColor(Color.LIGHT_GRAY);
                choiceFont.draw(batch, "[Top Bar / ESC] Kembali ke Kontak", px + 14, py + 20);

            } else if (phoneScreen == 4) {
                // Sherly Chat
                choiceFont.getData().setScale(0.68f);
                choiceFont.setColor(new Color(0.85f, 0.6f, 1f, 1f));
                choiceFont.draw(batch, "Sherly:", px + 20, topY - 8);
                choiceFont.setColor(Color.WHITE);
                choiceFont.draw(batch, "Raka, jadwal promo sosmed dan kelengkapan studio Echo Summer beres. Jangan telat latihan!", px + 20, topY - 28, phoneW - 50f, Align.left, true);

                choiceFont.setColor(new Color(0.5f, 1f, 0.6f, 1f));
                choiceFont.draw(batch, "Raksha (kamu):", px + 20, topY - 98f);
                choiceFont.setColor(Color.WHITE);
                choiceFont.draw(batch, "Siap Mbak Manager! Dipastikan datang tepat waktu.  ✓✓", px + 20, topY - 118f, phoneW - 50f, Align.left, true);

                choiceFont.getData().setScale(0.62f);
                choiceFont.setColor(Color.LIGHT_GRAY);
                choiceFont.draw(batch, "[Top Bar / ESC] Kembali ke Kontak", px + 14, py + 20);

            } else if (phoneScreen == 5) {
                // Rania Chat
                choiceFont.getData().setScale(0.68f);
                choiceFont.setColor(new Color(1f, 0.6f, 0.75f, 1f));
                choiceFont.draw(batch, "Rania:", px + 20, topY - 8);
                choiceFont.setColor(Color.WHITE);
                choiceFont.draw(batch, "Raksha, aku nemu lukisan & partitur bagus banget kemarin... Nanti aku tunjukin ya!", px + 20, topY - 28, phoneW - 50f, Align.left, true);

                choiceFont.setColor(new Color(0.5f, 1f, 0.6f, 1f));
                choiceFont.draw(batch, "Raksha (kamu):", px + 20, topY - 98f);
                choiceFont.setColor(Color.WHITE);
                choiceFont.draw(batch, "Keren Ran! Gak sabar mau liat hasil lukisan kamu.  ✓✓", px + 20, topY - 118f, phoneW - 50f, Align.left, true);

                choiceFont.getData().setScale(0.62f);
                choiceFont.setColor(Color.LIGHT_GRAY);
                choiceFont.draw(batch, "[Top Bar / ESC] Kembali ke Kontak", px + 14, py + 20);
            }
        }

        choiceFont.getData().setScale(1.0f);
        batch.end();
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
        nearClara = (currentZone == ExplorationZone.DALAM_STUDIO || currentZone == ExplorationZone.UKM_MUSIK || currentZone == ExplorationZone.KAMPUS) && (currentZone == ExplorationZone.DALAM_STUDIO ? rakshaX >= 150f : rakshaX >= 700f);
        nearKostGate = (currentZone == ExplorationZone.KOST_OUTSIDE) && rakshaX >= 480f && rakshaX <= 750f;
        
        nearRania  = canSeeRania()  && (currentZone == ExplorationZone.STUDIO_SENI) && Math.abs(rakshaX - raniaX) < 150f;
        nearSherly = canSeeSherly() && (currentZone == ExplorationZone.KEDAI_KOPI)  && Math.abs(rakshaX - sherlyX) < 150f;
        nearBagas  = canSeeBagas()  && (currentZone == ExplorationZone.KANTIN)      && Math.abs(rakshaX - bagasX) < 150f;
        nearJalanTanah = (currentZone == ExplorationZone.JALAN_SETAPAK) && rakshaX >= 440f && rakshaX <= 590f;
        nearStudioDoor = (currentZone == ExplorationZone.LUAR_RUANG_STUDIO) && rakshaX >= 630f && rakshaX <= 800f;
        nearStudioExit = (currentZone == ExplorationZone.DALAM_STUDIO) && rakshaX >= 60f && rakshaX <= 250f;
        nearUkmMusikExit = (currentZone == ExplorationZone.UKM_MUSIK) && rakshaX >= 60f && rakshaX <= 250f;
        nearKantinDoor = (currentZone == ExplorationZone.TAMAN_KAMPUS) && rakshaX >= 500f && rakshaX <= 780f;
        nearKantinExit = (currentZone == ExplorationZone.KANTIN) && rakshaX >= 500f && rakshaX <= 780f;
        nearUkmMusikDoor = (currentZone == ExplorationZone.LORONG_1) && rakshaX >= 850f && rakshaX <= 1050f;
        nearUkmSeniDoor  = (currentZone == ExplorationZone.LORONG_2) && rakshaX >= 850f && rakshaX <= 1050f;
        nearStudioSeniExit = (currentZone == ExplorationZone.STUDIO_SENI) && rakshaX >= 60f && rakshaX <= 250f;

        // Zone-specific edge / movement clamping
        switch (currentZone) {
            case KOST:
                if (rakshaX < 60f) rakshaX = 60f;
                if (rakshaX > VIRTUAL_WIDTH - 60f) {
                    startZoneTransition(ExplorationZone.KOST_OUTSIDE, "Keluar Kost...");
                }
                break;
            case KOST_OUTSIDE:
                if (rakshaX < 60f) {
                    if (gameState.day == 24 && !gameState.day24EventDone) {
                        rakshaX = 150f;
                        loadNode("BLOCK_DAY24_RANIA");
                    } else {
                        startZoneTransition(ExplorationZone.KEDAI_KOPI, "Menuju Kedai Kopi");
                    }
                }
                if (rakshaX > VIRTUAL_WIDTH - 60f) {
                    if (gameState.day == 26 && !gameState.day26EventDone) {
                        rakshaX = VIRTUAL_WIDTH - 150f;
                        loadNode("BLOCK_DAY26_SHERLY");
                    } else {
                        startZoneTransition(ExplorationZone.WARKOP, "Menuju Jalan Raya");
                    }
                }
                break;
            case KEDAI_KOPI:
                if (rakshaX < 60f) {
                    if (gameState.day == 26 && !gameState.day26EventDone) {
                        rakshaX = 150f;
                        loadNode("BLOCK_DAY26_SHERLY");
                    } else {
                        startZoneTransition(ExplorationZone.JALAN_DANAU, "Menuju Danau");
                    }
                }
                if (rakshaX > VIRTUAL_WIDTH - 60f) {
                    startZoneTransition(ExplorationZone.KOST_OUTSIDE, "Kembali ke Depan Kost");
                }
                break;
            case JALAN_DANAU:
                if (rakshaX < 60f) {
                    startZoneTransition(ExplorationZone.JALAN_SETAPAK, "Menuju Jalan Setapak");
                }
                if (rakshaX > VIRTUAL_WIDTH - 60f) {
                    startZoneTransition(ExplorationZone.KEDAI_KOPI, "Kembali ke Kedai Kopi");
                }
                break;
            case JALAN_SETAPAK:
                if (rakshaX < 60f) rakshaX = 60f;
                if (rakshaX > VIRTUAL_WIDTH - 60f) {
                    startZoneTransition(ExplorationZone.JALAN_DANAU, "Kembali ke Danau");
                }
                break;
            case LUAR_RUANG_STUDIO:
                if (rakshaX < 60f) {
                    startZoneTransition(ExplorationZone.JALAN_SETAPAK, "Kembali ke Jalan Setapak");
                }
                if (rakshaX > VIRTUAL_WIDTH - 60f) rakshaX = VIRTUAL_WIDTH - 60f;
                break;
            case DALAM_STUDIO:
                if (rakshaX < 60f) {
                    startZoneTransition(ExplorationZone.LUAR_RUANG_STUDIO, "Keluar ke Luar Studio");
                }
                if (rakshaX > VIRTUAL_WIDTH - 60f) rakshaX = VIRTUAL_WIDTH - 60f;
                break;
            case UKM_MUSIK:
                if (rakshaX < 60f) {
                    startZoneTransition(ExplorationZone.LORONG_1, "Keluar ke Lorong 1");
                }
                if (rakshaX > VIRTUAL_WIDTH - 60f) rakshaX = VIRTUAL_WIDTH - 60f;
                break;
            case WARKOP:
                if (rakshaX < 60f) {
                    if (gameState.day == 24 && !gameState.day24EventDone) {
                        rakshaX = 150f;
                        loadNode("BLOCK_DAY24_RANIA");
                    } else {
                        startZoneTransition(ExplorationZone.KOST_OUTSIDE, "Kembali ke Depan Kost");
                    }
                }
                if (rakshaX > VIRTUAL_WIDTH - 60f) {
                    startZoneTransition(ExplorationZone.TAMAN_KAMPUS, "Menuju Taman Kampus");
                }
                break;
            case TAMAN_KAMPUS:
                if (rakshaX < 60f) {
                    startZoneTransition(ExplorationZone.WARKOP, "Kembali ke Jalan Raya");
                }
                if (rakshaX > VIRTUAL_WIDTH - 60f) {
                    startZoneTransition(ExplorationZone.LORONG_1, "Menuju Lorong 1");
                }
                break;
            case KANTIN:
                if (rakshaX < 60f) {
                    startZoneTransition(ExplorationZone.TAMAN_KAMPUS, "Kembali ke Taman Kampus");
                }
                if (rakshaX > VIRTUAL_WIDTH - 60f) {
                    if ("CHAPTER_4".equals(gameState.chapter)) {
                        // Chapter 4: entering deep kantin triggers the concert sequence
                        loadNode("CH4_CLARA_PEP");
                    } else {
                        startZoneTransition(ExplorationZone.TAMAN_KAMPUS, "Kembali ke Taman Kampus");
                    }
                }
                break;
            case LORONG_1:
                if (rakshaX < 60f) {
                    startZoneTransition(ExplorationZone.TAMAN_KAMPUS, "Kembali ke Taman Kampus");
                }
                if (rakshaX > VIRTUAL_WIDTH - 60f) {
                    startZoneTransition(ExplorationZone.LORONG_2, "Menuju Lorong 2");
                }
                break;
            case LORONG_2:
                if (rakshaX < 60f) {
                    startZoneTransition(ExplorationZone.LORONG_1, "Kembali ke Lorong 1");
                }
                if (rakshaX > 1000f) {
                    rakshaX = 1000f; // Pembatas (barrier) arah loker, hak akses hanya sampai pintu UKM Seni
                }
                break;
            case KAMPUS:
                if (rakshaX < 60f) {
                    if (gameState.day == 24 && !gameState.day24EventDone) {
                        rakshaX = 150f;
                        loadNode("BLOCK_DAY24_RANIA");
                    } else {
                        startZoneTransition(ExplorationZone.LORONG_2, "Kembali ke Lorong 2");
                    }
                }
                if (rakshaX > VIRTUAL_WIDTH - 60f) {
                    startZoneTransition(ExplorationZone.STUDIO_SENI, "Menuju Studio Seni");
                }
                break;
            case STUDIO_SENI:
                if (rakshaX < 60f) {
                    startZoneTransition(ExplorationZone.LORONG_2, "Keluar ke Lorong 2");
                }
                if (rakshaX > VIRTUAL_WIDTH - 60f) rakshaX = VIRTUAL_WIDTH - 60f;
                break;
        }

        if (interactionCooldown > 0f) {
            interactionCooldown -= delta;
        }

        // Key E or SPACE: NPC Conversations & Bed Interactions
        if ((Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) && interactionCooldown <= 0f) {
            if (nearBed) {
                if ("CHAPTER_4".equals(gameState.chapter)) {
                    loadNode("BLOCK_CH4_QUEST");
                } else {
                    String blockNode = getPendingMandatoryBlockNode();
                    if (blockNode != null) {
                        loadNode(blockNode);
                    } else {
                        loadNode("CH1_KOST_CHOICES");
                    }
                }
            } else if (nearClara) {
                String blockNode = getPendingMandatoryBlockNode();
                if (blockNode != null && !blockNode.equals("BLOCK_CH2_QUEST")) {
                    loadNode(blockNode);
                } else if (isPracticeDay(gameState.day) && currentZone == ExplorationZone.DALAM_STUDIO) {
                    loadNode("BAND_PRACTICE_START");
                } else if (gameState.freeDayEventDone) {
                    loadNode("CH1_KAMPUS_CHOICES");
                } else {
                    loadNode("FREE_DATE_CLARA_START");
                }
            } else if (nearSherly) {
                if (gameState.day26EventDone) {
                    loadNode("SHERLY_CASUAL_1");
                } else {
                    loadNode("CH1_SHERLY_EVENT");
                }
            } else if (nearRania) {
                if (gameState.day24EventDone) {
                    loadNode("RANIA_CASUAL_1");
                } else {
                    loadNode("CH1_RANIA_EVENT");
                }
            } else if (nearBagas) {
                if (gameState.day28EventDone) {
                    loadNode("BAGAS_CASUAL_1");
                } else {
                    loadNode("CH1_BAGAS_EVENT");
                }
            }
        }

        // Key W ONLY: Room / Door Transitions
        if (Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            if (nearKantinDoor) {
                startZoneTransition(ExplorationZone.KANTIN, "Masuk ke Kantin");
            } else if (nearKantinExit) {
                startZoneTransition(ExplorationZone.TAMAN_KAMPUS, "Keluar Kantin");
            } else if (nearUkmMusikDoor) {
                startZoneTransition(ExplorationZone.UKM_MUSIK, "Masuk ke Ruang UKM Musik");
            } else if (nearUkmMusikExit) {
                startZoneTransition(ExplorationZone.LORONG_1, "Keluar ke Lorong 1");
            } else if (nearUkmSeniDoor) {
                startZoneTransition(ExplorationZone.STUDIO_SENI, "Masuk ke Studio Seni");
            } else if (nearStudioSeniExit) {
                startZoneTransition(ExplorationZone.LORONG_2, "Keluar ke Lorong 2");
            } else if (nearKostGate) {
                if (gameState.day == 26 && !gameState.day26EventDone) {
                    loadNode("BLOCK_DAY26_SHERLY");
                } else if (gameState.day == 24 && !gameState.day24EventDone) {
                    loadNode("BLOCK_DAY24_RANIA");
                } else {
                    startZoneTransition(ExplorationZone.KOST, "Masuk ke Kamar Kost");
                }
            } else if (nearJalanTanah) {
                startZoneTransition(ExplorationZone.LUAR_RUANG_STUDIO, "Menuju Luar Studio");
            } else if (nearStudioDoor) {
                startZoneTransition(ExplorationZone.DALAM_STUDIO, "Masuk ke Dalam Studio");
            } else if (nearStudioExit) {
                startZoneTransition(ExplorationZone.LUAR_RUANG_STUDIO, "Keluar ke Luar Studio");
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
                ExplorationZone oldZone = currentZone;
                currentZone = pendingZone;
                
                // Sync currentBackground to matched zone
                currentBackground = getZoneTexture(currentZone);
                
                if (pendingZone == ExplorationZone.KOST) {
                    rakshaX = VIRTUAL_WIDTH - 150f;
                    rakshaFacingRight = false;
                } else if (pendingZone == ExplorationZone.KOST_OUTSIDE && oldZone == ExplorationZone.KOST) {
                    rakshaX = VIRTUAL_WIDTH / 2f;
                    rakshaFacingRight = true;
                } else if (pendingZone == ExplorationZone.KEDAI_KOPI && oldZone == ExplorationZone.KOST_OUTSIDE) {
                    rakshaX = VIRTUAL_WIDTH - 150f;
                    rakshaFacingRight = false;
                } else if (pendingZone == ExplorationZone.KOST_OUTSIDE && oldZone == ExplorationZone.KEDAI_KOPI) {
                    rakshaX = 150f;
                    rakshaFacingRight = true;
                } else if (pendingZone == ExplorationZone.STUDIO_SENI && oldZone == ExplorationZone.KAMPUS) {
                    rakshaX = 150f;
                    rakshaFacingRight = true;
                } else if (pendingZone == ExplorationZone.KAMPUS && oldZone == ExplorationZone.STUDIO_SENI) {
                    rakshaX = VIRTUAL_WIDTH - 150f;
                    rakshaFacingRight = false;
                } else if (pendingZone == ExplorationZone.WARKOP && oldZone == ExplorationZone.TAMAN_KAMPUS) {
                    rakshaX = VIRTUAL_WIDTH - 150f;
                    rakshaFacingRight = false;
                } else if (pendingZone == ExplorationZone.TAMAN_KAMPUS && oldZone == ExplorationZone.WARKOP) {
                    rakshaX = 150f;
                    rakshaFacingRight = true;
                } else if (pendingZone == ExplorationZone.KANTIN && oldZone == ExplorationZone.TAMAN_KAMPUS) {
                    rakshaX = VIRTUAL_WIDTH / 2f;
                    rakshaFacingRight = true;
                } else if (pendingZone == ExplorationZone.TAMAN_KAMPUS && oldZone == ExplorationZone.KANTIN) {
                    rakshaX = 640f;
                    rakshaFacingRight = true;
                } else if (pendingZone == ExplorationZone.LORONG_1 && oldZone == ExplorationZone.TAMAN_KAMPUS) {
                    rakshaX = 150f;
                    rakshaFacingRight = true;
                } else if (pendingZone == ExplorationZone.TAMAN_KAMPUS && oldZone == ExplorationZone.LORONG_1) {
                    rakshaX = VIRTUAL_WIDTH - 150f;
                    rakshaFacingRight = false;
                } else if (pendingZone == ExplorationZone.LORONG_2 && oldZone == ExplorationZone.LORONG_1) {
                    rakshaX = 150f;
                    rakshaFacingRight = true;
                } else if (pendingZone == ExplorationZone.LORONG_1 && oldZone == ExplorationZone.LORONG_2) {
                    rakshaX = VIRTUAL_WIDTH - 150f;
                    rakshaFacingRight = false;
                } else if (pendingZone == ExplorationZone.KAMPUS && oldZone == ExplorationZone.LORONG_2) {
                    rakshaX = 150f;
                    rakshaFacingRight = true;
                } else if (pendingZone == ExplorationZone.LORONG_2 && oldZone == ExplorationZone.KAMPUS) {
                    rakshaX = VIRTUAL_WIDTH - 150f;
                    rakshaFacingRight = false;
                } else if (pendingZone == ExplorationZone.KOST_OUTSIDE && oldZone == ExplorationZone.WARKOP) {
                    rakshaX = VIRTUAL_WIDTH - 150f;
                    rakshaFacingRight = false;
                } else if (pendingZone == ExplorationZone.JALAN_DANAU && oldZone == ExplorationZone.KEDAI_KOPI) {
                    rakshaX = VIRTUAL_WIDTH - 150f;
                    rakshaFacingRight = false;
                } else if (pendingZone == ExplorationZone.JALAN_DANAU && oldZone == ExplorationZone.JALAN_SETAPAK) {
                    rakshaX = 150f;
                    rakshaFacingRight = true;
                } else if (pendingZone == ExplorationZone.KEDAI_KOPI && oldZone == ExplorationZone.JALAN_DANAU) {
                    rakshaX = 150f;
                    rakshaFacingRight = true;
                } else if (pendingZone == ExplorationZone.JALAN_SETAPAK && oldZone == ExplorationZone.JALAN_DANAU) {
                    rakshaX = VIRTUAL_WIDTH - 150f;
                    rakshaFacingRight = false;
                } else if (pendingZone == ExplorationZone.JALAN_SETAPAK && oldZone == ExplorationZone.LUAR_RUANG_STUDIO) {
                    rakshaX = 520f;
                    rakshaFacingRight = true;
                } else if (pendingZone == ExplorationZone.LUAR_RUANG_STUDIO && oldZone == ExplorationZone.JALAN_SETAPAK) {
                    rakshaX = 150f;
                    rakshaFacingRight = true;
                } else if (pendingZone == ExplorationZone.LUAR_RUANG_STUDIO && oldZone == ExplorationZone.DALAM_STUDIO) {
                    rakshaX = 715f;
                    rakshaFacingRight = true;
                } else if (pendingZone == ExplorationZone.DALAM_STUDIO && oldZone == ExplorationZone.LUAR_RUANG_STUDIO) {
                    rakshaX = 150f;
                    rakshaFacingRight = true;
                    if (("CHAPTER_2".equals(gameState.chapter) || (gameState.day <= 20 && gameState.day >= 10)) && !gameState.ch2CompositionDone) {
                        if (!gameState.ch2ChatRead) {
                            // Block entry — must read the group chat first
                            loadNode("BLOCK_CH2_QUEST");
                            rakshaX = 200f;
                        } else {
                            // Chat read, all members here — trigger composition
                            loadNode("CH2_COMPOSITION");
                        }
                    }
                } else if (pendingZone == ExplorationZone.LORONG_1 && oldZone == ExplorationZone.UKM_MUSIK) {
                    rakshaX = 950f;
                    rakshaFacingRight = false;
                } else if (pendingZone == ExplorationZone.UKM_MUSIK && oldZone == ExplorationZone.LORONG_1) {
                    rakshaX = 150f;
                    rakshaFacingRight = true;
                    if ("CHAPTER_3".equals(gameState.chapter) && !gameState.ch3AldoDone) {
                        loadNode("CH3_ALDO_1");
                    }
                } else {
                    rakshaX = 150f; 
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

        // 1. Draw full-screen background matching current zone and time of day
        Texture zoneBg = getZoneTexture(currentZone);
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
            case WARKOP:
            case TAMAN_KAMPUS:
            case KANTIN:
            case LORONG_1:
            case LORONG_2:
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

        // 2. Draw NPCs
        boolean practiceDay = isPracticeDay(gameState.day);

        // Draw Clara — visible in UKM Musik (DALAM_STUDIO & UKM_MUSIK) & KAMPUS zone
        if (currentZone == ExplorationZone.DALAM_STUDIO || currentZone == ExplorationZone.UKM_MUSIK || currentZone == ExplorationZone.KAMPUS) {
            float claraDrawX = (practiceDay && currentZone == ExplorationZone.DALAM_STUDIO) ? 880f - claraSpriteSize / 2f : 950f - claraSpriteSize / 2f;
            TextureRegion claraFrame = claraIdleAnimation.getKeyFrame(claraAnimationTime, true);
            batch.draw(claraFrame, claraDrawX, claraDrawY, claraSpriteSize, claraSpriteSize);
        }

        // Calculate feet Y based on Raksha's bounding box in 2048x2048 image
        float npcScale = 1150f / 2048f;
        float feetY = claraDrawY + (789f * npcScale); // approx 143f

        // Draw Rania: Studio Seni on Day 24, OR in DALAM_STUDIO on practice days (days 20, 18, 15, 12)
        if (raniaIdleAnimation != null && canSeeRania()) {
            if (currentZone == ExplorationZone.DALAM_STUDIO && practiceDay) {
                TextureRegion raniaFrame = raniaIdleAnimation.getKeyFrame(claraAnimationTime, true);
                float drawW = raniaFrame.getRegionWidth() * npcScale;
                float drawH = raniaFrame.getRegionHeight() * npcScale;
                batch.draw(raniaFrame, 480f - drawW / 2f, feetY, drawW, drawH);
            } else if (currentZone == ExplorationZone.STUDIO_SENI) {
                TextureRegion raniaFrame = raniaIdleAnimation.getKeyFrame(claraAnimationTime, true);
                float drawW = raniaFrame.getRegionWidth() * npcScale;
                float drawH = raniaFrame.getRegionHeight() * npcScale;
                batch.draw(raniaFrame, raniaX - drawW / 2f, feetY, drawW, drawH);
            }
        }

        // Draw Sherly: Kedai Kopi OR in DALAM_STUDIO on practice days
        if (sherlyIdleAnimation != null && canSeeSherly()) {
            if (currentZone == ExplorationZone.DALAM_STUDIO && practiceDay) {
                TextureRegion sherlyFrame = sherlyIdleAnimation.getKeyFrame(claraAnimationTime, true);
                float drawW = sherlyFrame.getRegionWidth() * npcScale;
                float drawH = sherlyFrame.getRegionHeight() * npcScale;
                batch.draw(sherlyFrame, 280f - drawW / 2f, feetY, drawW, drawH);
            } else if (currentZone == ExplorationZone.KEDAI_KOPI) {
                TextureRegion sherlyFrame = sherlyIdleAnimation.getKeyFrame(claraAnimationTime, true);
                float drawW = sherlyFrame.getRegionWidth() * npcScale;
                float drawH = sherlyFrame.getRegionHeight() * npcScale;
                batch.draw(sherlyFrame, sherlyX - drawW / 2f, feetY, drawW, drawH);
            }
        }

        // Draw Bagas: Kantin OR in DALAM_STUDIO on practice days
        if (bagasIdleAnimation != null && canSeeBagas()) {
            if (currentZone == ExplorationZone.DALAM_STUDIO && practiceDay) {
                TextureRegion bagasFrame = bagasIdleAnimation.getKeyFrame(claraAnimationTime, true);
                float drawW = bagasFrame.getRegionWidth() * npcScale;
                float drawH = bagasFrame.getRegionHeight() * npcScale;
                batch.draw(bagasFrame, 680f - drawW / 2f, feetY, drawW, drawH);
            } else if (currentZone == ExplorationZone.KANTIN) {
                TextureRegion bagasFrame = bagasIdleAnimation.getKeyFrame(claraAnimationTime, true);
                float drawW = bagasFrame.getRegionWidth() * npcScale;
                float drawH = bagasFrame.getRegionHeight() * npcScale;
                batch.draw(bagasFrame, bagasX - drawW / 2f, feetY, drawW, drawH);
            }
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

        // 4. Draw Modern Rich Non-Flat HUD (Location & Objective Cards with Pill Badges & Gradients)
        String locName = "";
        String locSub = "";
        switch (currentZone) {
            case KOST:
                locName = "Kamar Kost";
                locSub = "";
                break;
            case KOST_OUTSIDE:
                locName = "Halaman Kost";
                locSub = "[A] Kiri: Kedai Kopi    |    [D] Kanan: Jalan Raya";
                break;
            case WARKOP:
                locName = "Jalan Raya";
                locSub = "[A] Kiri: Halaman Kost    |    [D] Kanan: Taman Kampus";
                break;
            case TAMAN_KAMPUS:
                locName = "Taman Kampus";
                locSub = "[A] Kiri: Jalan Raya    |    [W] Masuk: Kantin    |    [D] Kanan: Lorong 1";
                break;
            case KANTIN:
                locName = "Kantin Kampus";
                locSub = "[W] Keluar ke Taman Kampus";
                break;
            case LORONG_1:
                locName = "Lorong 1";
                locSub = "[A] Kiri: Taman Kampus    |    [W] Masuk: UKM Musik    |    [D] Kanan: Lorong 2";
                break;
            case LORONG_2:
                locName = "Lorong 2";
                locSub = "[A] Kiri: Lorong 1    |    [W] Masuk: Studio Seni    |    [D] Kanan: Kampus Utama";
                break;
            case KEDAI_KOPI:
                locName = "Kedai Kopi";
                locSub = "[A] Kiri: Jalan Danau    |    [D] Kanan: Halaman Kost";
                break;
            case KAMPUS:
                locName = "Kampus Utama";
                locSub = "[A] Kiri: Lorong 2    |    [D] Kanan: Studio Seni";
                break;
            case STUDIO_SENI:
                locName = "Studio Seni";
                locSub = "[A] Kiri: Kampus Utama";
                break;
            case JALAN_SETAPAK:
                locName = "Jalan Setapak";
                locSub = "[D] Kanan: Jalan Danau";
                break;
            case JALAN_DANAU:
                locName = "Jalan Danau";
                locSub = "[A] Kiri: Jalan Setapak    |    [D] Kanan: Kedai Kopi";
                break;
            case LUAR_RUANG_STUDIO:
                locName = "Luar Studio Musik";
                locSub = "[A] Kiri: Jalan Setapak";
                break;
            case DALAM_STUDIO:
                locName = "Dalam Studio Musik";
                locSub = "";
                break;
            case UKM_MUSIK:
                locName = "Ruang UKM Musik";
                locSub = "";
                break;
            default:
                locName = "Unknown";
                locSub = "";
                break;
        }

        String rawQuest = getCurrentQuest();
        String cleanedQuest = rawQuest.replace("🎯 ", "").trim();
        String questDetail = cleanedQuest;
        if (cleanedQuest.startsWith("Objektif ")) {
            questDetail = cleanedQuest.substring(9).trim();
        }

        // Layout measurements for Tag Badges
        String tag1Text = " LOKASI ";
        String tag2Text = " OBJEKTIF ";
        GlyphLayout layoutTag1 = new GlyphLayout(font, tag1Text);
        GlyphLayout layoutTag2 = new GlyphLayout(font, tag2Text);
        GlyphLayout layoutLocName = new GlyphLayout(font, locName);
        GlyphLayout layoutLocSub = locSub.isEmpty() ? null : new GlyphLayout(font, locSub);
        GlyphLayout layoutQuestDetail = new GlyphLayout(font, questDetail);

        float paddingX = 14f;
        float paddingY = 10f;
        float stripeW = 5f;
        float tag1W = layoutTag1.width + 8f;
        float tag2W = layoutTag2.width + 8f;

        float row1LocW = tag1W + 12f + layoutLocName.width;
        float locContentW = (layoutLocSub != null && layoutLocSub.width > row1LocW) ? layoutLocSub.width : row1LocW;
        float questContentW = tag2W + 12f + layoutQuestDetail.width;

        float minCardW = 420f;
        float cardW = Math.max(minCardW, Math.max(locContentW, questContentW) + paddingX * 2 + stripeW + 12f);

        float lineHeight = font.getLineHeight();
        float locCardH = locSub.isEmpty() ? (lineHeight + paddingY * 2 + 4f) : (lineHeight * 2 + paddingY * 2 + 10f);
        float questCardH = lineHeight + paddingY * 2 + 4f;

        float cardStartX = 20f;
        float locCardY = VIRTUAL_HEIGHT - 15f - locCardH;
        float questCardY = locCardY - 10f - questCardH;

        batch.end();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(camera.combined);

        // 1. Drop Shadows behind Cards
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.0f, 0.0f, 0.0f, 0.40f);
        shapeRenderer.rect(cardStartX + 3f, locCardY - 3f, cardW, locCardH);
        shapeRenderer.rect(cardStartX + 3f, questCardY - 3f, cardW, questCardH);

        // 2. Card 1 Background (Rich Blue Gradient)
        shapeRenderer.rect(cardStartX, locCardY, cardW, locCardH,
            new Color(0.04f, 0.07f, 0.16f, 0.92f), new Color(0.04f, 0.07f, 0.16f, 0.92f),
            new Color(0.08f, 0.14f, 0.28f, 0.88f), new Color(0.08f, 0.14f, 0.28f, 0.88f));

        // Card 1 Left Accent Bar & Top Glow Strip
        shapeRenderer.setColor(0.2f, 0.65f, 1.0f, 0.95f);
        shapeRenderer.rect(cardStartX, locCardY, stripeW, locCardH);
        shapeRenderer.setColor(0.35f, 0.75f, 1.0f, 0.80f);
        shapeRenderer.rect(cardStartX + stripeW, locCardY + locCardH - 2f, cardW - stripeW, 2f);

        // Tag 1 Filled Pill Badge ("LOKASI")
        float tag1X = cardStartX + stripeW + paddingX;
        float tag1Y = locCardY + locCardH - paddingY - lineHeight;
        shapeRenderer.setColor(0.12f, 0.42f, 0.88f, 0.90f);
        shapeRenderer.rect(tag1X, tag1Y - 2f, tag1W, lineHeight + 4f);

        // 3. Card 2 Background (Rich Warm Amber Gradient)
        shapeRenderer.rect(cardStartX, questCardY, cardW, questCardH,
            new Color(0.08f, 0.07f, 0.04f, 0.92f), new Color(0.08f, 0.07f, 0.04f, 0.92f),
            new Color(0.16f, 0.13f, 0.06f, 0.88f), new Color(0.16f, 0.13f, 0.06f, 0.88f));

        // Card 2 Left Accent Bar & Top Glow Strip
        shapeRenderer.setColor(1.0f, 0.75f, 0.20f, 0.95f);
        shapeRenderer.rect(cardStartX, questCardY, stripeW, questCardH);
        shapeRenderer.setColor(1.0f, 0.85f, 0.40f, 0.80f);
        shapeRenderer.rect(cardStartX + stripeW, questCardY + questCardH - 2f, cardW - stripeW, 2f);

        // Tag 2 Solid Amber Pill Badge ("OBJEKTIF")
        float tag2X = cardStartX + stripeW + paddingX;
        float tag2Y = questCardY + questCardH - paddingY - lineHeight;
        shapeRenderer.setColor(1.0f, 0.75f, 0.20f, 0.95f);
        shapeRenderer.rect(tag2X, tag2Y - 2f, tag2W, lineHeight + 4f);
        shapeRenderer.end();

        // Outlines & Borders
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        // Card 1 Borders & Tag 1 Outer Frame
        shapeRenderer.setColor(0.25f, 0.50f, 0.85f, 0.6f);
        shapeRenderer.rect(cardStartX, locCardY, cardW, locCardH);
        shapeRenderer.setColor(0.40f, 0.80f, 1.0f, 0.85f);
        shapeRenderer.rect(tag1X, tag1Y - 2f, tag1W, lineHeight + 4f);

        if (!locSub.isEmpty()) {
            float divY = locCardY + paddingY + lineHeight + 3f;
            shapeRenderer.setColor(0.2f, 0.4f, 0.7f, 0.35f);
            shapeRenderer.line(tag1X, divY, cardStartX + cardW - paddingX, divY);
        }

        // Card 2 Borders & Tag 2 Outer Frame
        shapeRenderer.setColor(0.75f, 0.55f, 0.20f, 0.6f);
        shapeRenderer.rect(cardStartX, questCardY, cardW, questCardH);
        shapeRenderer.setColor(1.0f, 0.85f, 0.40f, 0.9f);
        shapeRenderer.rect(tag2X, tag2Y - 2f, tag2W, lineHeight + 4f);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();

        // Card 1 Text
        font.setColor(Color.WHITE);
        font.draw(batch, tag1Text, tag1X + 4f, tag1Y + lineHeight - 1f);

        font.setColor(new Color(0.92f, 0.97f, 1.0f, 1.0f));
        font.draw(batch, locName, tag1X + tag1W + 10f, tag1Y + lineHeight - 1f);

        if (!locSub.isEmpty()) {
            font.setColor(new Color(0.68f, 0.84f, 0.96f, 0.90f));
            font.draw(batch, locSub, tag1X, locCardY + paddingY + lineHeight - 1f);
        }

        // Card 2 Text (Solid Gold Tag + Dark text contrast!)
        font.setColor(new Color(0.08f, 0.06f, 0.02f, 1.0f));
        font.draw(batch, tag2Text, tag2X + 4f, tag2Y + lineHeight - 1f);

        font.setColor(new Color(1.0f, 0.90f, 0.50f, 1.0f));
        font.draw(batch, questDetail, tag2X + tag2W + 10f, tag2Y + lineHeight - 1f);

        // Interaction prompts — show when near a hotspot

        if (nearBed || nearClara || nearKostGate || nearRania || nearSherly || nearBagas || nearJalanTanah || nearStudioDoor || nearStudioExit || nearUkmMusikExit || nearKantinDoor || nearKantinExit || nearUkmMusikDoor || nearUkmSeniDoor || nearStudioSeniExit) {
            String promptText = "";
            if (nearBed) promptText = "[ E ]  Tempat Tidur  —  Latihan Gitar / Tidur";
            else if (nearClara) {
                if (isPracticeDay(gameState.day) && currentZone == ExplorationZone.DALAM_STUDIO) {
                    promptText = "[ E ]  Latihan Band (Tatap Esok)";
                } else {
                    promptText = "[ E ]  Bicara dengan Clara";
                }
            }
            else if (nearRania) promptText = "[ E ]  Bicara dengan Rania";
            else if (nearSherly) promptText = "[ E ]  Bicara dengan Sherly";
            else if (nearBagas) promptText = "[ E ]  Bicara dengan Bagas";
            else if (nearKantinDoor) promptText = "[ W ]  Masuk ke Kantin Kampus";
            else if (nearKantinExit) promptText = "[ W ]  Keluar ke Taman Kampus";
            else if (nearUkmMusikDoor) promptText = "[ W ]  Masuk ke UKM Musik";
            else if (nearUkmMusikExit) promptText = "[ W ]  Keluar ke Lorong 1";
            else if (nearUkmSeniDoor) promptText = "[ W ]  Masuk ke Studio Seni";
            else if (nearStudioSeniExit) promptText = "[ W ]  Keluar ke Lorong 2";
            else if (nearKostGate) promptText = "[ W ]  Masuk ke Kamar Kost";
            else if (nearJalanTanah) promptText = "[ W ]  Telusuri Jalan Setapak";
            else if (nearStudioDoor) promptText = "[ W ]  Masuk ke Studio Musik";
            else if (nearStudioExit) promptText = "[ W ]  Keluar ke Luar Studio";

            
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
        if (currentNode != null && (currentNode.nodeId.startsWith("CREDITS_") || currentNode.nodeId.equals("GAME_OVER"))) {
            renderMovieCredits(delta);
            return;
        }

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

        // Dynamically compute exact required height from actual text measurement so no text is ever clipped or cut off
        GlyphLayout textLayout = new GlyphLayout(dialogueFont, currentDialogueText != null ? currentDialogueText : "", Color.WHITE, boxW - 80f, Align.left, true);
        float reqH = textLayout.height + 70f;
        float boxH = Math.max(160f, Math.min(310f, reqH));
        
        String speechContent = typedText;
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
        if (nodeId.startsWith("PROLOG_") || nodeId.startsWith("END_") || nodeId.startsWith("CREDITS_") || nodeId.equals("GAME_OVER")) {
            return;
        }

        // Large portrait height anchored to bottom of screen
        float targetHeight = VIRTUAL_HEIGHT * 0.80f;
        float yPos = 0f;

        float leftX = width * 0.28f;
        float rightX = width * 0.72f;

        String speaker = currentSpeakerName;

        float rakshaDim = 0.6f;
        float rightDim = 0.6f;

        if (speaker != null) {
            if (speaker.equalsIgnoreCase("Raksha")) {
                rakshaDim = 1.0f;
                rightDim = 0.6f;
            } else if (speaker.equalsIgnoreCase("Clara") || speaker.equalsIgnoreCase("Sherly") || speaker.equalsIgnoreCase("Rania") || speaker.equalsIgnoreCase("Bagas")) {
                rightDim = 1.0f;
                rakshaDim = 0.6f;
            } else if (speaker.isEmpty()) {
                rakshaDim = 0.8f;
                rightDim = 0.8f;
            } else {
                rakshaDim = 0.6f;
                rightDim = 0.6f;
            }
        }

        // Draw Raksha (Left)
        Texture rakshaTex = getCharacterExpressionTexture("raksha", rakshaExpression);
        drawCharacter(batch, rakshaTex, idleAnimation.getKeyFrame(animationTime, true), leftX, yPos, targetHeight, rakshaDim);

        // Draw Right Character (Clara or Sherly or Rania)
        if ("None".equals(activeRightCharacter)) {
            // Do not draw right character
        } else if ("Sherly".equals(activeRightCharacter)) {
            Texture sherlyTex = getCharacterExpressionTexture("sherly", sherlyExpression);
            TextureRegion fallback = sherlyIdleAnimation != null ? sherlyIdleAnimation.getKeyFrame(claraAnimationTime, true) : null;
            drawCharacter(batch, sherlyTex, fallback, rightX, yPos, targetHeight, rightDim);
        } else if ("Rania".equals(activeRightCharacter)) {
            Texture raniaTex = getCharacterExpressionTexture("rania", raniaExpression);
            TextureRegion fallback = raniaIdleAnimation != null ? raniaIdleAnimation.getKeyFrame(claraAnimationTime, true) : null;
            drawCharacter(batch, raniaTex, fallback, rightX, yPos, targetHeight, rightDim);
        } else if ("Bagas".equals(activeRightCharacter)) {
            Texture bagasTex = getCharacterExpressionTexture("bagas", bagasExpression);
            TextureRegion fallback = bagasIdleAnimation != null ? bagasIdleAnimation.getKeyFrame(claraAnimationTime, true) : null;
            drawCharacter(batch, bagasTex, fallback, rightX, yPos, targetHeight, rightDim);
        } else {
            // Clara
            Texture claraTex = getCharacterExpressionTexture("clara", claraExpression);
            drawCharacter(batch, claraTex, claraIdleAnimation.getKeyFrame(claraAnimationTime, true), rightX, yPos, targetHeight, rightDim);
        }

        batch.setColor(Color.WHITE);
    }

    private void drawCharacter(SpriteBatch batch, Texture tex, TextureRegion fallbackFrame, float centerX, float yPos, float targetHeight, float dim) {
        batch.setColor(dim, dim, dim, 1f);
        if (tex != null) {
            float aspect = (float) tex.getWidth() / (float) tex.getHeight();
            float drawH = targetHeight;
            float drawW = drawH * aspect;
            float drawX = centerX - drawW / 2f;
            batch.draw(tex, drawX, yPos, drawW, drawH);
        } else if (fallbackFrame != null) {
            if (fallbackFrame.getRegionWidth() < 1000) {
                // Not a full-canvas 2048x2048 sprite, draw it dynamically
                float aspect = (float) fallbackFrame.getRegionWidth() / (float) fallbackFrame.getRegionHeight();
                float drawH = targetHeight;
                float drawW = drawH * aspect;
                float drawX = centerX - drawW / 2f;
                batch.draw(fallbackFrame, drawX, yPos, drawW, drawH);
            } else {
                // Use original large sprite rendering unchanged
                float originalSize = 2000f;
                float originalY = -850f;
                float drawX = centerX - originalSize / 2f;
                batch.draw(fallbackFrame, drawX, originalY, originalSize, originalSize);
            }
        }
    }

    private void renderChoices(float width, float height) {
        if (currentNode == null || currentNode.choices == null) return;

        Choice[] choices = currentNode.choices;
        int n = choices.length;
        float btnW = 880f;
        float btnH = 54f;
        float gap = 15f;
        float totalH = n * btnH + (n - 1) * gap;
        float startY = (height - totalH) / 2f + 60f;
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
            choiceFont.getData().setScale(1.0f);

            GlyphLayout layout = new GlyphLayout(choiceFont, choice.text);
            float maxUsableW = btnW - 40f;
            float targetScale = 1.0f;
            if (layout.width > maxUsableW) {
                targetScale = maxUsableW / layout.width;
            }
            if (isHovered) {
                targetScale *= 1.05f;
            }

            choiceFont.getData().setScale(targetScale);
            float textY = btnY + (btnH + layout.height * targetScale) / 2f;
            choiceFont.draw(batch, choice.text, startX, textY, btnW, Align.center, false);
            choiceFont.getData().setScale(1.0f);
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

            // If action already redirected dialogueNodeId (e.g. CHECK_RECRUITMENT_ACTION),
            // follow that redirect instead of nextId
            String redirectedId = gameState.dialogueNodeId;
            if ((nextId == null || nextId.isEmpty())
                    && redirectedId != null
                    && !redirectedId.equals(currentNode.nodeId)) {
                loadNode(redirectedId);
                return;
            }

            if (nextId == null || nextId.isEmpty()) {
                currentNode = null;
                isDialogueFinished = false;
                state = GameplayState.EXPLORATION_STATE;
                interactionCooldown = 0.35f;
                updateTimeOfDay();
            } else if (nextId.equals("GAME_OVER")) {
                game.setScreen(new MainMenuScreen(game));
            } else if (nextId.equals("EXPLORATION_MODE")) {
                currentNode = null;
                isDialogueFinished = false;
                state = GameplayState.EXPLORATION_STATE;
                interactionCooldown = 0.35f;
                updateTimeOfDay();
                return;
            } else if (nextId.equals("START_GUITAR_RHYTHM_GAME")) {
                rhythmFromGuitarPractice = true;
                rhythmFromBandPractice = false;
                state = GameplayState.RHYTHM_STATE;
                nearBed   = false;
                nearClara = false;
                rhythmMusic = Gdx.audio.newMusic(Gdx.files.internal("music/cover seandainya.mp3"));
                rhythmMusic.setVolume(SettingsManager.getVolume());
                rhythmGame.start(rhythmMusic, "LATIHAN GITAR - \"SEANDAINYA - VIERRA\"");
            } else if (nextId.equals("START_BAND_RHYTHM_GAME")) {
                rhythmFromBandPractice = true;
                rhythmFromGuitarPractice = false;
                state = GameplayState.RHYTHM_STATE;
                nearBed   = false;
                nearClara = false;
                rhythmMusic = Gdx.audio.newMusic(Gdx.files.internal("music/Tatap_Esok.mp3"));
                rhythmMusic.setVolume(SettingsManager.getVolume());
                rhythmGame.start(rhythmMusic, "LATIHAN BAND - \"TATAP ESOK\"");
            } else if (nextId.equals("START_CONCERT_RHYTHM_GAME")) {
                rhythmFromBandPractice = true;
                rhythmFromGuitarPractice = false;
                state = GameplayState.RHYTHM_STATE;
                nearBed   = false;
                nearClara = false;
                rhythmMusic = Gdx.audio.newMusic(Gdx.files.internal("music/Tatap_Esok.mp3"));
                rhythmMusic.setVolume(SettingsManager.getVolume());
                rhythmGame.start(rhythmMusic, "PANGGUNG ECHO SUMMER - \"TATAP ESOK\"");
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
            if (rhythmMusic != null) {
                rhythmMusic.dispose();
                rhythmMusic = null;
            }
            if (rhythmGame.wasFailed()) {
                rhythmFromGuitarPractice = false;
                rhythmFromBandPractice = false;
                state = GameplayState.EXPLORATION_STATE;
            } else {
                gameState.performanceScore = rhythmGame.score;
                if (rhythmFromGuitarPractice) {
                    // Guitar practice in kost: award stats based on performance then end day
                    rhythmFromGuitarPractice = false;
                    int bonusCreativity = 3 + (rhythmGame.perfects >= 10 ? 2 : 0);
                    int bonusConfidence = 2 + (rhythmGame.perfects >= 15 ? 1 : 0);
                    gameState.creativity  += bonusCreativity;
                    gameState.confidence  += bonusConfidence;
                    loadNode("CH1_PRACTICE_END");
                } else if (rhythmFromBandPractice) {
                    rhythmFromBandPractice = false;
                    int bonusStat = 3 + (rhythmGame.perfects >= 10 ? 2 : 0);
                    gameState.creativity += bonusStat;
                    gameState.confidence += bonusStat;
                    gameState.claraRel  += 2;
                    gameState.bagasRel  += 2;
                    gameState.raniaRel  += 2;
                    gameState.sherlyRel += 2;
                    if ("CHAPTER_4".equals(gameState.chapter) || (currentNode != null && currentNode.nodeId.startsWith("CH4_"))) {
                        loadNode("CH4_CONCERT_SUCCESS");
                    } else if ("CHAPTER_2".equals(gameState.chapter) || (currentNode != null && currentNode.nodeId.startsWith("CH2_"))) {
                        loadNode("CH2_END");
                    } else {
                        loadNode("BAND_PRACTICE_END");
                    }
                } else {
                    loadNode("CH4_CONCERT_SUCCESS");
                }
            }
        }
    }

    private void renderRhythm(float delta) {
        shapeRenderer.setProjectionMatrix(game.getBatch().getProjectionMatrix());
        rhythmGame.draw(shapeRenderer, game.getBatch(), font, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
    }

    private float autoSaveToastTimer = 0f;

    private void triggerAutoSave() {
        if (gameState == null) return;
        gameState.currentZone = currentZone.name();
        gameState.rakshaX = rakshaX;
        gameState.gameplayState = (state != null ? state : GameplayState.EXPLORATION_STATE).name();
        SaveManager.saveGame(gameState, "autosave.dat");
        autoSaveToastTimer = 2.5f;
    }

    // ==========================================
    // SHARED CALENDAR / HUD
    // ==========================================
    private void drawHUD(SpriteBatch batch, ShapeRenderer shape) {
        if (gameState == null) return;

        // Hide HUD badge during ending cutscenes, credits, or concert sequence!
        if (currentNode != null) {
            String nodeId = currentNode.nodeId;
            if (nodeId.startsWith("END_") || nodeId.startsWith("CREDITS_") || nodeId.equals("GAME_OVER") || nodeId.startsWith("CH4_")) {
                return;
            }
        }

        float badgeW = 220f;
        float badgeH = 80f;
        boolean showMoney = gameState.chapter.equals("CHAPTER_1");
        if (showMoney) {
            badgeH = 110f;
        }

        float badgeX = VIRTUAL_WIDTH - badgeW - 20f;
        float badgeY = VIRTUAL_HEIGHT - 15f - badgeH;
        float stripeW = 4f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(new Color(0.05f, 0.08f, 0.16f, 0.85f));
        shape.rect(badgeX, badgeY, badgeW, badgeH);

        shape.setColor(new Color(0.2f, 0.65f, 1.0f, 0.95f));
        shape.rect(badgeX, badgeY, stripeW, badgeH);
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(new Color(0.2f, 0.45f, 0.75f, 0.6f));
        shape.rect(badgeX, badgeY, badgeW, badgeH);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        if (batch.isDrawing()) {
            batch.end();
        }

        batch.begin();
        float textStartX = badgeX + stripeW + 12f;
        font.setColor(Color.YELLOW);
        font.draw(batch, "📅 Sisa Hari: " + remainingDays, textStartX, badgeY + badgeH - 15f);

        font.setColor(Color.WHITE);
        font.draw(batch, "🕒 Waktu: " + timeOfDay, textStartX, badgeY + badgeH - 42f);

        if (showMoney) {
            font.setColor(Color.GREEN);
            font.draw(batch, "🪙 Uang: Rp" + gameState.money, textStartX, badgeY + badgeH - 69f);
            font.setColor(Color.WHITE);
            font.draw(batch, "🎸 Kreativitas: " + gameState.creativity, textStartX, badgeY + badgeH - 90f);
        }
        batch.end();

        if (state == GameplayState.EXPLORATION_STATE || state == GameplayState.DIALOGUE_STATE) {
            float barW = 1020f;
            float barH = 34f;
            float barX = (VIRTUAL_WIDTH - barW) / 2f;
            float barY = 15f;

            Gdx.gl.glEnable(GL20.GL_BLEND);
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(new Color(0.04f, 0.06f, 0.12f, 0.88f));
            shape.rect(barX, barY, barW, barH);
            shape.end();

            shape.begin(ShapeRenderer.ShapeType.Line);
            shape.setColor(new Color(0.2f, 0.45f, 0.8f, 0.7f));
            shape.rect(barX, barY, barW, barH);
            shape.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);

            batch.begin();
            font.setColor(Color.WHITE);
            font.getData().setScale(0.85f);
            if (state == GameplayState.EXPLORATION_STATE) {
                font.draw(batch, "[A/D] Jalan   •   [SPACE] Interaksi   •   [TAB] HP   •   [M] Peta Kampus   •   [I] Inventaris   •   [H] Riwayat   •   [ESC] Menu", barX, barY + 23f, barW, Align.center, false);
            } else {
                font.draw(batch, "[SPACE / ENTER] Lanjut   •   [H] Riwayat Dialog   •   [BACKSPACE] Undo / Rewind Dialog", barX, barY + 23f, barW, Align.center, false);
            }
            font.getData().setScale(1.0f);
            batch.end();
        }

        if (autoSaveToastTimer > 0) {
            batch.begin();
            font.setColor(Color.LIME);
            font.draw(batch, "💾 Auto Saved", 20f, VIRTUAL_HEIGHT - 30f);
            batch.end();
        }

        // Data Structure Interactive UI Overlays
        renderDialogueHistoryStack();
        renderMapGraphOverlay();
        renderLinkedListInventory();
    }

    private void backtrackDialogue() {
        if (!dialogueHistoryStack.isEmpty()) {
            DialogueNode prev = dialogueHistoryStack.pop();
            if (prev != null && prev.nodeId != null) {
                currentNode = prev;
                gameState.dialogueNodeId = prev.nodeId;
                typedText = prev.text != null ? prev.text : "";
                isDialogueFinished = true;
            }
        }
    }

    private void renderDialogueHistoryStack() {
        if (!isDialogueHistoryActive) return;

        float cardW = 1040f;
        float cardH = 580f;
        float cardX = (VIRTUAL_WIDTH - cardW) / 2f;
        float cardY = (VIRTUAL_HEIGHT - cardH) / 2f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // Full screen dimming backdrop
        shapeRenderer.setColor(new Color(0f, 0f, 0f, 0.85f));
        shapeRenderer.rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        // Modal Card Background & Header Bar
        shapeRenderer.setColor(new Color(0.03f, 0.05f, 0.12f, 0.96f));
        shapeRenderer.rect(cardX, cardY, cardW, cardH);
        shapeRenderer.setColor(new Color(0.12f, 0.45f, 0.85f, 0.95f));
        shapeRenderer.rect(cardX, cardY + cardH - 52f, cardW, 52f);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(new Color(0.3f, 0.65f, 1.0f, 0.8f));
        shapeRenderer.rect(cardX, cardY, cardW, cardH);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        SpriteBatch batch = game.getBatch();
        batch.begin();
        font.setColor(Color.WHITE);
        font.getData().setScale(1.15f);
        font.draw(batch, "📚 RIWAYAT DIALOG (DIALOGUE LOG)", cardX + 20f, cardY + cardH - 15f);

        font.getData().setScale(0.85f);
        font.setColor(new Color(0.9f, 0.9f, 0.9f, 0.85f));
        font.draw(batch, "Tekan [ H / ESC ] untuk Tutup   |   Tekan [ BACKSPACE ] untuk Undo", cardX + cardW - 510f, cardY + cardH - 18f);

        int count = 0;
        float rowY = cardY + cardH - 85f;
        for (DialogueNode node : dialogueHistoryStack) {
            if (node == null || count >= 7) break;
            
            font.setColor(Color.GOLD);
            String speaker = node.speaker != null ? node.speaker : "Narasi";
            font.draw(batch, "[" + speaker + "]", cardX + 30f, rowY);

            font.setColor(Color.WHITE);
            String textPrev = node.text != null ? node.text.replace("\n", " ") : "...";
            if (textPrev.length() > 68) textPrev = textPrev.substring(0, 68) + "...";
            font.draw(batch, textPrev, cardX + 180f, rowY);

            rowY -= 62f;
            count++;
        }

        if (count == 0) {
            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, "(Belum ada riwayat dialog tersimpan dalam sesi percakapan ini)", cardX + 30f, rowY);
        }

        font.getData().setScale(1.0f);
        batch.end();
    }

    private void renderMapGraphOverlay() {
        if (!isMapGraphActive) return;

        float cardW = 1080f;
        float cardH = 610f;
        float cardX = (VIRTUAL_WIDTH - cardW) / 2f;
        float cardY = (VIRTUAL_HEIGHT - cardH) / 2f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Full screen dimming backdrop to cover calendar HUD and room text completely!
        shapeRenderer.setColor(new Color(0f, 0f, 0f, 0.85f));
        shapeRenderer.rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        // Modal Card Body
        shapeRenderer.setColor(new Color(0.04f, 0.07f, 0.14f, 0.98f));
        shapeRenderer.rect(cardX, cardY, cardW, cardH);
        
        // Header line accent
        shapeRenderer.setColor(new Color(0.1f, 0.65f, 0.45f, 0.95f));
        shapeRenderer.rect(cardX, cardY + cardH - 52f, cardW, 52f);

        // Active path card container
        shapeRenderer.setColor(new Color(0.07f, 0.14f, 0.22f, 0.9f));
        shapeRenderer.rect(cardX + 20f, cardY + cardH - 150f, cardW - 40f, 85f);

        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(new Color(0.2f, 0.8f, 0.55f, 0.8f));
        shapeRenderer.rect(cardX, cardY, cardW, cardH);
        shapeRenderer.rect(cardX + 20f, cardY + cardH - 150f, cardW - 40f, 85f);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        SpriteBatch batch = game.getBatch();
        batch.begin();
        font.setColor(Color.WHITE);
        font.getData().setScale(1.15f);
        font.draw(batch, "🗺️ PETA KAMPUS & RUTE NAVIGASI", cardX + 20f, cardY + cardH - 15f);

        font.getData().setScale(0.85f);
        font.setColor(new Color(0.9f, 1.0f, 0.9f, 0.9f));
        font.draw(batch, "Tekan [ M / ESC ] untuk Tutup   |   Sistem Navigasi Graf Kampus", cardX + cardW - 440f, cardY + cardH - 18f);

        // Current Location & Shortest Path BFS Output
        font.setColor(Color.GOLD);
        font.getData().setScale(0.95f);
        font.draw(batch, "📍 LOKASI SAAT INI: " + formatZoneName(currentZone), cardX + 35f, cardY + cardH - 80f);

        java.util.List<ExplorationZone> shortestPath = mapNavigationGraph.findShortestPathBFS(currentZone, ExplorationZone.UKM_MUSIK);
        StringBuilder sbPath = new StringBuilder("🚩 Rute Tercepat ke UKM Musik: ");
        if (shortestPath != null) {
            for (int i = 0; i < shortestPath.size(); i++) {
                sbPath.append(formatZoneNameShort(shortestPath.get(i)));
                if (i < shortestPath.size() - 1) sbPath.append(" ➔ ");
            }
        }
        String pathText = sbPath.toString();
        
        font.getData().setScale(0.85f);
        GlyphLayout pathLayout = new GlyphLayout(font, pathText);
        float maxUsableWidth = cardW - 80f;
        float pathScale = 0.85f;
        if (pathLayout.width > maxUsableWidth) {
            pathScale = (maxUsableWidth / pathLayout.width) * 0.85f;
        }
        font.getData().setScale(pathScale);
        font.setColor(Color.CYAN);
        font.draw(batch, pathText, cardX + 35f, cardY + cardH - 112f);
        font.getData().setScale(1.0f);

        // 2-Column Grid of Location Nodes
        font.setColor(Color.WHITE);
        font.draw(batch, "🌐 DAFTAR LOKASI & AKSES JALUR TERHUBUNG:", cardX + 25f, cardY + cardH - 170f);

        float col1X = cardX + 30f;
        float col2X = cardX + 560f;
        float maxColWidth = 480f;
        float nodeY = cardY + cardH - 205f;

        ExplorationZone[] zones = ExplorationZone.values();

        for (int i = 0; i < zones.length && i < 12; i++) {
            ExplorationZone z = zones[i];
            float drawX = (i < 6) ? col1X : col2X;
            float drawY = (i < 6) ? (nodeY - i * 54f) : (nodeY - (i - 6) * 54f);

            boolean isCurrent = (z == currentZone);
            font.setColor(isCurrent ? Color.GOLD : Color.WHITE);
            font.draw(batch, (i + 1) + ". " + formatZoneName(z), drawX, drawY);

            java.util.List<ExplorationZone> neighbors = mapNavigationGraph.getNeighbors(z);
            StringBuilder sbN = new StringBuilder("   • Terhubung ke: ");
            if (neighbors != null) {
                for (int k = 0; k < neighbors.size(); k++) {
                    sbN.append(formatZoneNameShort(neighbors.get(k)));
                    if (k < neighbors.size() - 1) sbN.append(", ");
                }
            }
            String nText = sbN.toString();
            font.getData().setScale(0.85f);
            GlyphLayout nLayout = new GlyphLayout(font, nText);
            float nScale = 0.85f;
            if (nLayout.width > maxColWidth) {
                nScale = (maxColWidth / nLayout.width) * 0.85f;
            }
            font.getData().setScale(nScale);
            font.setColor(isCurrent ? Color.YELLOW : Color.LIGHT_GRAY);
            font.draw(batch, nText, drawX, drawY - 22f);
            font.getData().setScale(1.0f);
        }

        font.getData().setScale(1.0f);
        batch.end();
    }

    private String formatZoneNameShort(ExplorationZone zone) {
        if (zone == null) return "-";
        switch (zone) {
            case KOST: return "Kost";
            case KOST_OUTSIDE: return "Depan Kost";
            case WARKOP: return "Warmindo";
            case KAMPUS: return "Gerbang Kampus";
            case TAMAN_KAMPUS: return "Taman";
            case KANTIN: return "Kantin";
            case LORONG_1: return "Lorong 1";
            case LORONG_2: return "Lorong 2";
            case KEDAI_KOPI: return "Kedai Kopi";
            case STUDIO_SENI: return "Studio Seni";
            case JALAN_SETAPAK: return "Jalan Setapak";
            case JALAN_DANAU: return "Danau";
            case LUAR_RUANG_STUDIO: return "Luar Studio";
            case DALAM_STUDIO: return "Dalam Studio";
            case UKM_MUSIK: return "UKM Musik";
            default: return zone.name();
        }
    }

    private String formatZoneName(ExplorationZone zone) {
        if (zone == null) return "-";
        switch (zone) {
            case KOST: return "Kamar Kost";
            case KOST_OUTSIDE: return "Depan Kost";
            case WARKOP: return "Warmindo Warkop";
            case KAMPUS: return "Gerbang Utama Kampus";
            case TAMAN_KAMPUS: return "Taman Kampus";
            case KANTIN: return "Kantin Kampus";
            case LORONG_1: return "Lorong Utama 1";
            case LORONG_2: return "Lorong Utama 2";
            case KEDAI_KOPI: return "Kedai Kopi Hits";
            case STUDIO_SENI: return "Studio Seni";
            case JALAN_SETAPAK: return "Jalan Setapak";
            case JALAN_DANAU: return "Area Danau Kampus";
            case LUAR_RUANG_STUDIO: return "Depan Ruang Studio";
            case DALAM_STUDIO: return "Dalam Studio Latihan";
            case UKM_MUSIK: return "Ruang UKM Musik";
            default: return zone.name();
        }
    }

    private void renderLinkedListInventory() {
        if (!isInventoryActive) return;

        float cardW = 1080f;
        float cardH = 610f;
        float cardX = (VIRTUAL_WIDTH - cardW) / 2f;
        float cardY = (VIRTUAL_HEIGHT - cardH) / 2f;

        float colW = 505f;
        float colH = 500f;
        float col1X = cardX + 20f;
        float col2X = cardX + colW + 30f;
        float innerY = cardY + 20f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Full screen dimming backdrop to cover calendar HUD completely!
        shapeRenderer.setColor(new Color(0f, 0f, 0f, 0.85f));
        shapeRenderer.rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        // Modal Card Background & Headers
        shapeRenderer.setColor(new Color(0.07f, 0.05f, 0.14f, 0.98f));
        shapeRenderer.rect(cardX, cardY, cardW, cardH);
        
        // Header line accent
        shapeRenderer.setColor(new Color(0.85f, 0.6f, 0.1f, 0.95f));
        shapeRenderer.rect(cardX, cardY + cardH - 52f, cardW, 52f);

        // Column Cards Background
        shapeRenderer.setColor(new Color(0.12f, 0.08f, 0.22f, 0.85f));
        shapeRenderer.rect(col1X, innerY, colW, colH);
        shapeRenderer.rect(col2X, innerY, colW, colH);

        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(new Color(0.9f, 0.7f, 0.2f, 0.8f));
        shapeRenderer.rect(cardX, cardY, cardW, cardH);
        shapeRenderer.rect(col1X, innerY, colW, colH);
        shapeRenderer.rect(col2X, innerY, colW, colH);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        SpriteBatch batch = game.getBatch();
        batch.begin();
        font.setColor(Color.WHITE);
        font.getData().setScale(1.15f);
        font.draw(batch, "🎒 INVENTARIS & JURNAL QUEST", cardX + 20f, cardY + cardH - 15f);

        font.getData().setScale(0.85f);
        font.setColor(new Color(1.0f, 0.95f, 0.8f, 0.9f));
        font.draw(batch, "Tekan [ I / ESC ] untuk Tutup   |   Catatan Jurnal Sesi Musik", cardX + cardW - 440f, cardY + cardH - 18f);

        // Column 1: Quests
        font.setColor(Color.GOLD);
        font.getData().setScale(1.0f);
        font.draw(batch, "📜 OBJEKTIF & QUEST AKTIF", col1X + 20f, innerY + colH - 25f);

        font.setColor(Color.WHITE);
        font.getData().setScale(0.9f);
        int qIdx = 1;
        float questY = innerY + colH - 70f;
        for (String quest : activeQuestList) {
            font.setColor(Color.YELLOW);
            font.draw(batch, "• Objektif (" + gameState.day + " Hari Terakhir):", col1X + 20f, questY);
            font.setColor(Color.WHITE);
            font.draw(batch, quest, col1X + 20f, questY - 26f);
            questY -= 75f;
            qIdx++;
        }

        // Column 2: Band Items
        font.setColor(Color.GOLD);
        font.getData().setScale(1.0f);
        font.draw(batch, "🎸 ITEM BAND & PERALATAN", col2X + 20f, innerY + colH - 25f);

        String[][] bandItems = {
            {"Gitar Akustik Fender", "Senar terpasang rapi, siap untuk latihan & konser."},
            {"Buku Catatan Lirik \"Tatap Esok\"", "Gubahan lagu kenangan bersama sahabat."},
            {"Pik Gitar Nyaman", "Aksesori pendukung latihan gubahan melodi."},
            {"Kopi Susu Warmindo Kost", "Item penambah stamina saat bergadang gubah lagu."}
        };

        float itemY = innerY + colH - 70f;
        for (int i = 0; i < bandItems.length; i++) {
            font.setColor(Color.CYAN);
            font.draw(batch, "• " + bandItems[i][0], col2X + 20f, itemY);
            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, bandItems[i][1], col2X + 35f, itemY - 22f);
            itemY -= 65f;
        }

        font.getData().setScale(1.0f);
        batch.end();
    }

    // ==========================================
    // INPUT PROCESSOR IMPLEMENTATION
    // ==========================================
    @Override
    public boolean keyDown(int keycode) {
        // 1. Phone Overlay State Key Handling (TAB, ESCAPE, ENTER)
        if (state == GameplayState.PHONE_STATE) {
            if (keycode == Input.Keys.TAB || keycode == Input.Keys.ESCAPE) {
                state = (previousState != null && previousState != GameplayState.PHONE_STATE) ? previousState : GameplayState.EXPLORATION_STATE;
                phoneScreen = 0;
                return true;
            }
            if (keycode == Input.Keys.ENTER) {
                if (phoneScreen == 0) {
                    phoneScreen = 1;
                } else if (phoneScreen == 1) {
                    if (("CHAPTER_2".equals(gameState.chapter) || (gameState.day <= 20 && gameState.day >= 10)) && !gameState.ch2ChatRead) {
                        gameState.ch2ChatRead = true;
                    } else {
                        state = (previousState != null && previousState != GameplayState.PHONE_STATE) ? previousState : GameplayState.EXPLORATION_STATE;
                        phoneScreen = 0;
                    }
                }
                return true;
            }
            return true;
        }

        // 2. Pause Menu (ESCAPE)
        if (keycode == Input.Keys.ESCAPE) {
            if (isDialogueHistoryActive || isMapGraphActive || isInventoryActive) {
                isDialogueHistoryActive = false;
                isMapGraphActive = false;
                isInventoryActive = false;
                return true;
            }
            if (state == GameplayState.RHYTHM_STATE && rhythmGame.isFailed()) {
                rhythmGame.handleKeyPress(keycode);
                return true;
            }
            if (state != GameplayState.PAUSED_STATE) {
                previousState = state;
                state = GameplayState.PAUSED_STATE;
                if (previousState == GameplayState.RHYTHM_STATE) {
                    rhythmGame.pause();
                }
                if (rhythmMusic != null && rhythmMusic.isPlaying()) {
                    rhythmMusic.pause();
                }
            } else {
                state = (previousState != null && previousState != GameplayState.PAUSED_STATE) ? previousState : GameplayState.EXPLORATION_STATE;
                if (state == GameplayState.RHYTHM_STATE) {
                    rhythmGame.resume();
                }
            }
            return true;
        }

        // Data Structure Interactive Keyboard Triggers
        if (keycode == Input.Keys.H) {
            isDialogueHistoryActive = !isDialogueHistoryActive;
            return true;
        }
        if (keycode == Input.Keys.M) {
            isMapGraphActive = !isMapGraphActive;
            return true;
        }
        if (keycode == Input.Keys.I) {
            isInventoryActive = !isInventoryActive;
            return true;
        }
        if (keycode == Input.Keys.BACKSPACE && state == GameplayState.DIALOGUE_STATE) {
            backtrackDialogue();
            return true;
        }

        // 3. Open Phone Overlay (TAB in Exploration)
        if (keycode == Input.Keys.TAB) {
            if (state == GameplayState.EXPLORATION_STATE) {
                if (gameState.day == 26 && !gameState.day26EventDone) {
                    loadNode("BLOCK_DAY26_SHERLY");
                    return true;
                }
                if (gameState.day == 24 && !gameState.day24EventDone) {
                    loadNode("BLOCK_DAY24_RANIA");
                    return true;
                }
                previousState = GameplayState.EXPLORATION_STATE;
                state = GameplayState.PHONE_STATE;
                phoneScreen = 0;
                return true;
            }
        }

        if (state == GameplayState.DIALOGUE_STATE) {
            if (currentNode != null && (currentNode.nodeId.startsWith("CREDITS_") || currentNode.nodeId.equals("GAME_OVER"))) {
                if (keycode == Input.Keys.SPACE || keycode == Input.Keys.ENTER) {
                    if (creditsScrollY >= VIRTUAL_HEIGHT / 2f) {
                        if (creditsMusic != null) {
                            if (creditsMusic.isPlaying()) creditsMusic.stop();
                            creditsMusic.dispose();
                            creditsMusic = null;
                        }
                        game.setScreen(new MainMenuScreen(game));
                        return true;
                    }
                    return true;
                }
            }
            if (currentNode != null && currentNode.choices != null) return false;
            if (keycode == Input.Keys.SPACE || keycode == Input.Keys.ENTER || keycode == Input.Keys.E) {
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
            float menuW = 400f;
            float menuH = 450f;
            float mx = (VIRTUAL_WIDTH - menuW) / 2f;
            float my = (VIRTUAL_HEIGHT - menuH) / 2f;

            if (isSaveOverlayActive) {
                float btnW = 300f;
                float btnH = 60f;
                float bx = (VIRTUAL_WIDTH - btnW) / 2f;

                // Slot 1
                if (rx >= bx && rx <= bx + btnW && ry >= my + 280 && ry <= my + 280 + btnH) {
                    saveGameToSlot(1);
                    return true;
                }
                // Slot 2
                if (rx >= bx && rx <= bx + btnW && ry >= my + 190 && ry <= my + 190 + btnH) {
                    saveGameToSlot(2);
                    return true;
                }
                // Slot 3
                if (rx >= bx && rx <= bx + btnW && ry >= my + 100 && ry <= my + 100 + btnH) {
                    saveGameToSlot(3);
                    return true;
                }
                // Kembali
                if (rx >= bx && rx <= bx + btnW && ry >= my + 30 && ry <= my + 30 + 45) {
                    isSaveOverlayActive = false;
                    saveSuccessTimer = 0f;
                    return true;
                }
                return true;
            } else {
                float btnW = 200f;
                float btnH = 50f;
                float bx = (VIRTUAL_WIDTH - btnW) / 2f;

                // Resume
                if (rx >= bx && rx <= bx + btnW && ry >= my + 350 && ry <= my + 350 + btnH) {
                    state = previousState;
                    if (state == GameplayState.RHYTHM_STATE) {
                        rhythmGame.resume();
                    }
                    return true;
                }
                // Save Game
                if (rx >= bx && rx <= bx + btnW && ry >= my + 95 && ry <= my + 95 + 45) {
                    isSaveOverlayActive = true;
                    saveSuccessTimer = 0f;
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
            }
            return true;
        }

        if (state == GameplayState.PHONE_STATE) {
            float phoneW = 360f;
            float phoneH = 620f;
            float px = (VIRTUAL_WIDTH - phoneW) / 2f;
            float py = (VIRTUAL_HEIGHT - phoneH) / 2f;

            // Clicking outside the phone body closes the phone
            if (rx < px || rx > px + phoneW || ry < py || ry > py + phoneH) {
                state = previousState != null ? previousState : GameplayState.EXPLORATION_STATE;
                phoneScreen = 0;
                return true;
            }

            if (phoneScreen == 0) {
                float cardH = 62f;
                float startCardY = py + phoneH - 85f - cardH;
                // Row 0: Group Chat Echo Summer
                float y0 = startCardY;
                if (rx >= px + 8 && rx <= px + phoneW - 8 && ry >= y0 && ry <= y0 + cardH) {
                    phoneScreen = 1;
                    return true;
                }
                // Row 1: Clara Chat (Bucin)
                float y1 = startCardY - 1 * (cardH + 6f);
                if (rx >= px + 8 && rx <= px + phoneW - 8 && ry >= y1 && ry <= y1 + cardH) {
                    phoneScreen = 2;
                    return true;
                }
                // Row 2: Bagas Chat
                float y2 = startCardY - 2 * (cardH + 6f);
                if (rx >= px + 8 && rx <= px + phoneW - 8 && ry >= y2 && ry <= y2 + cardH) {
                    phoneScreen = 3;
                    return true;
                }
                // Row 3: Sherly Chat
                float y3 = startCardY - 3 * (cardH + 6f);
                if (rx >= px + 8 && rx <= px + phoneW - 8 && ry >= y3 && ry <= y3 + cardH) {
                    phoneScreen = 4;
                    return true;
                }
                // Row 4: Rania Chat
                float y4 = startCardY - 4 * (cardH + 6f);
                if (rx >= px + 8 && rx <= px + phoneW - 8 && ry >= y4 && ry <= y4 + cardH) {
                    phoneScreen = 5;
                    return true;
                }
            } else {
                // Top bar / back button in any single or group chat
                if (ry >= py + phoneH - 55) {
                    phoneScreen = 0;
                    return true;
                }

                if (phoneScreen == 1) {
                    // Reply button in Ch2
                    if (("CHAPTER_2".equals(gameState.chapter) || (gameState.day <= 20 && gameState.day >= 10)) && !gameState.ch2ChatRead) {
                        if (rx >= px + phoneW / 2f - 70f && rx <= px + phoneW / 2f + 70f && ry >= py + 10 && ry <= py + 48) {
                            gameState.ch2ChatRead = true;
                            return true;
                        }
                    }
                } else if (phoneScreen == 2) {
                    // Clara chat reply options
                    if (gameState.claraChatDayReplied != gameState.day) {
                        // Option A: ry from py + 70 to py + 118
                        if (rx >= px + 12 && rx <= px + phoneW - 12 && ry >= py + 70 && ry <= py + 118) {
                            gameState.claraChatDayReplied = gameState.day;
                            gameState.claraChatChoiceIndex = 0;
                            gameState.claraRel += 2;
                            return true;
                        }
                        // Option B: ry from py + 15 to py + 63
                        if (rx >= px + 12 && rx <= px + phoneW - 12 && ry >= py + 15 && ry <= py + 63) {
                            gameState.claraChatDayReplied = gameState.day;
                            gameState.claraChatChoiceIndex = 1;
                            gameState.claraRel += 2;
                            return true;
                        }
                    }
                }
            }
            return true;
        }

        if (state == GameplayState.DIALOGUE_STATE) {
            if (currentNode != null && currentNode.choices != null) {
                Choice[] choices = currentNode.choices;
                int n = choices.length;
                float btnW = 880f;
                float btnH = 54f;
                float gap = 15f;
                float totalH = n * btnH + (n - 1) * gap;
                float startY = (VIRTUAL_HEIGHT - totalH) / 2f + 60f;
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
                        if (choice.nextNodeId != null && choice.nextNodeId.equals("CH1_DAY_NEXT")) {
                            if (!"MALAM".equals(timeOfDay)) {
                                loadNode("BLOCK_SLEEP_DAYTIME");
                            } else if (getPendingMandatoryBlockNode() != null) {
                                loadNode(getPendingMandatoryBlockNode());
                            } else {
                                loadNode(choice.nextNodeId);
                            }
                        } else {
                            loadNode(choice.nextNodeId);
                        }
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
        if (kedaiKopiTexture != null) kedaiKopiTexture.dispose();
        if (jalanSetapakTexture != null) jalanSetapakTexture.dispose();
        if (jalanDanauTexture != null) jalanDanauTexture.dispose();
        if (luarRuangStudioTexture != null) luarRuangStudioTexture.dispose();
        if (dalamStudioTexture != null) dalamStudioTexture.dispose();
        if (ukmSeniTexture != null) ukmSeniTexture.dispose();
        if (tamanKampusTexture != null) tamanKampusTexture.dispose();
        if (kantinTexture != null) kantinTexture.dispose();
        if (lorong1Texture != null) lorong1Texture.dispose();
        if (lorong2Texture != null) lorong2Texture.dispose();

        if (kamarKostMalamTexture != null) kamarKostMalamTexture.dispose();
        if (kostOutsideMalamTexture != null) kostOutsideMalamTexture.dispose();
        if (jalanRayaMalamTexture != null) jalanRayaMalamTexture.dispose();
        if (kedaiKopiMalamTexture != null) kedaiKopiMalamTexture.dispose();
        if (jalanSetapakMalamTexture != null) jalanSetapakMalamTexture.dispose();
        if (jalanDanauMalamTexture != null) jalanDanauMalamTexture.dispose();
        if (luarRuangStudioMalamTexture != null) luarRuangStudioMalamTexture.dispose();
        if (dalamStudioMalamTexture != null) dalamStudioMalamTexture.dispose();
        if (ukmSeniMalamTexture != null) ukmSeniMalamTexture.dispose();
        if (tamanKampusMalamTexture != null) tamanKampusMalamTexture.dispose();
        if (kantinMalamTexture != null) kantinMalamTexture.dispose();
        if (lorong1MalamTexture != null) lorong1MalamTexture.dispose();
        if (lorong2MalamTexture != null) lorong2MalamTexture.dispose();
        if (studioMalamTexture != null) studioMalamTexture.dispose();

        if (pfpGroup != null) pfpGroup.dispose();
        if (pfpClara != null) pfpClara.dispose();
        if (pfpBagas != null) pfpBagas.dispose();
        if (pfpSherly != null) pfpSherly.dispose();
        if (pfpRania != null) pfpRania.dispose();

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
        if (bagasIdleTextures != null) {
            for (Texture tex : bagasIdleTextures) {
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

        if (backgroundMap != null) {
            for (Texture tex : backgroundMap.values()) {
                if (tex != null) tex.dispose();
            }
            backgroundMap.clear();
        }
    }

    private boolean canSeeBagas() {
        if (gameState == null) return false;
        if (!"CHAPTER_1".equals(gameState.chapter)) return true;
        return gameState.day28EventDone || gameState.day <= 28;
    }

    private boolean canSeeSherly() {
        if (gameState == null) return false;
        if (!"CHAPTER_1".equals(gameState.chapter)) return true;
        return gameState.day26EventDone || gameState.day <= 26;
    }

    private boolean canSeeRania() {
        if (gameState == null) return false;
        if (!"CHAPTER_1".equals(gameState.chapter)) return true;
        return gameState.day24EventDone || gameState.day <= 24;
    }

    private String truncateSingleLine(String text, int maxChars) {
        if (text == null) return "";
        String single = text.replace("\n", " ").trim();
        if (single.length() > maxChars) {
            return single.substring(0, maxChars) + "...";
        }
        return single;
    }

    private String getClaraDailyChatMsg() {
        int d = gameState != null ? gameState.day : 30;
        if (d == 30) return "Rakaaa! Hari ini semangat ya latihannya~ Aku udah gak sabar pengen ketemu kamu lagi nanti sore! Jangan lupa sarapan ya sayangku~ ❤️";
        if (d >= 28) return "Raka, kamu tahu gak? Setiap kali aku ngeliat kamu petik gitar, jantungku rasanya mau melompat keluar... Kenapa sih kamu harus seganteng itu? 🙈";
        if (d >= 26) return "Rakaaa... Nanti malem telfonan yuk sebelum tidur? Aku mau denger suara kamu biar tidurnya makin nyenyak~ 😴💖";
        if (d >= 24) return "Raka, makasih ya udah selalu sabar nemenin dan dukung aku... Aku bersyukur banget bisa ketemu cowok sehebat dan se-sweet kamu 🥺❤️";
        return "Raka sayang! Selamat hari baru~ Ingat ya, hari ini harus selalu senyum karena ada aku yang mendoakan kamu dari jauh! Luv u! 😘";
    }

    private String getClaraReplyOptionA() {
        int d = gameState != null ? gameState.day : 30;
        if (d == 30) return "Semangat juga ya Ra! Gue kangen kamu ❤️";
        if (d >= 28) return "Dan setiap denger lo nyanyi, dunia milik berdua 💕";
        if (d >= 26) return "Pasti dong Ra, jam berapa pun telfon langsung gue angkat ❤️";
        if (d >= 24) return "Kamu itu hadiah terindah dalam hidup gue ✨";
        return "Luv u more Ra! Bikin makin semangat ngejalanin hari ❤️";
    }

    private String getClaraReplyOptionB() {
        int d = gameState != null ? gameState.day : 30;
        if (d == 30) return "Siap tuan putri, gue pasti sarapan nemenin kamu!";
        if (d >= 28) return "Main gitar emang khusus cuma buat kamu kok 😉";
        if (d >= 26) return "Nanti gue nyanyiin lagu pengantar tidur buat kamu 🥰";
        if (d >= 24) return "Aku janji gak akan pernah ngelepasin kamu ❤️";
        return "Senyum gue hari ini 100% khusus cuma buat kamu 🥰";
    }

    private String getPendingMandatoryBlockNode() {
        if (gameState == null) return null;
        String ch = gameState.chapter;
        int d = gameState.day;

        if ("CHAPTER_1".equals(ch) || "PROLOGUE".equals(ch)) {
            if (d == 28 && !gameState.day28EventDone && currentZone != ExplorationZone.KANTIN) {
                return "BLOCK_BAGAS_QUEST";
            } else if (d == 26 && !gameState.day26EventDone && currentZone != ExplorationZone.KEDAI_KOPI) {
                return "BLOCK_SHERLY_QUEST";
            } else if (d == 24 && !gameState.day24EventDone && currentZone != ExplorationZone.STUDIO_SENI) {
                return "BLOCK_RANIA_QUEST";
            }
            return null;
        } else if ("CHAPTER_2".equals(ch) || (d <= 20 && d >= 10)) {
            if (!gameState.ch2CompositionDone && currentZone != ExplorationZone.LUAR_RUANG_STUDIO && currentZone != ExplorationZone.DALAM_STUDIO) {
                return "BLOCK_CH2_QUEST";
            }
        } else if ("CHAPTER_3".equals(ch)) {
            if (!gameState.ch3AldoDone && currentZone != ExplorationZone.KEDAI_KOPI && currentZone != ExplorationZone.UKM_MUSIK) {
                return "BLOCK_CH3_QUEST";
            }
        } else if ("CHAPTER_4".equals(ch) || d <= 0) {
            if (currentZone != ExplorationZone.KANTIN) {
                return "BLOCK_CH4_QUEST";
            }
            return null;
        }
        return null;
    }



    private void loadCreditsJson() {
        if (creditsLoaded) return;
        try {
            if (Gdx.files.internal("credits.json").exists()) {
                JsonReader reader = new JsonReader();
                JsonValue root = reader.parse(Gdx.files.internal("credits.json"));
                creditsTitle = root.getString("title", "ECHO SUMMER");
                creditsSubtitle = root.getString("subtitle", "");
                creditsClosingQuote = root.getString("closingQuote", "");
                creditsCopyright = root.getString("copyright", "");

                creditsList.clear();
                if (root.has("credits")) {
                    for (JsonValue secVal = root.get("credits").child(); secVal != null; secVal = secVal.next()) {
                        CreditSection sec = new CreditSection();
                        sec.header = secVal.getString("header", "");
                        if (secVal.has("names")) {
                            for (JsonValue nameVal = secVal.get("names").child(); nameVal != null; nameVal = nameVal.next()) {
                                sec.names.add(nameVal.asString());
                            }
                        }
                        creditsList.add(sec);
                    }
                }
                creditsLoaded = true;
            }
        } catch (Exception e) {
            Gdx.app.error("GameplayScreen", "Failed to load credits.json: " + e.getMessage());
        }
    }

    private void renderMovieCredits(float delta) {
        if (creditsMusic == null) {
            try {
                if (Gdx.files.internal("music/Gravits.mp3").exists()) {
                    creditsMusic = Gdx.audio.newMusic(Gdx.files.internal("music/Gravits.mp3"));
                    creditsMusic.setLooping(false);
                    creditsMusic.setVolume(SettingsManager.getVolume());
                    creditsMusic.play();
                }
            } catch (Exception e) {
                Gdx.app.error("GameplayScreen", "Error playing Gravits.mp3 credits music: " + e.getMessage());
            }
        }
        loadCreditsJson();
        
        // Auto Scroll synced to Gravits.mp3 music duration (~4m35s)
        float contentHeight = 140f;
        for (CreditSection sec : creditsList) {
            contentHeight += 38f + (sec.names != null ? sec.names.size : 0) * 32f + 48f;
        }
        contentHeight += 250f;
        float totalDistance = contentHeight + 400f;

        if (creditsMusic != null && creditsMusic.isPlaying()) {
            float pos = creditsMusic.getPosition();
            float duration = 275.0f; // Gravits.mp3 length
            float progress = Math.min(1.0f, Math.max(0.0f, pos / duration));
            creditsScrollY = -100f + progress * totalDistance;
        } else {
            creditsScrollY += delta * 7.5f;
        }

        SpriteBatch batch = game.getBatch();

        // Dark Movie Overlay
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0.02f, 0.03f, 0.07f, 0.95f));
        shapeRenderer.rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();

        float curY = creditsScrollY;

        // Title
        font.setColor(Color.GOLD);
        font.getData().setScale(2.2f);
        font.draw(batch, creditsTitle, 0f, curY, VIRTUAL_WIDTH, Align.center, false);
        font.getData().setScale(1.0f);
        curY -= 55f;

        // Subtitle
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, creditsSubtitle, 0f, curY, VIRTUAL_WIDTH, Align.center, false);
        curY -= 85f;

        // Credit Sections
        for (CreditSection sec : creditsList) {
            font.setColor(Color.YELLOW);
            font.getData().setScale(1.2f);
            font.draw(batch, sec.header, 0f, curY, VIRTUAL_WIDTH, Align.center, false);
            font.getData().setScale(1.0f);
            curY -= 38f;

            font.setColor(Color.WHITE);
            for (String name : sec.names) {
                font.draw(batch, name, 0f, curY, VIRTUAL_WIDTH, Align.center, false);
                curY -= 32f;
            }
            curY -= 48f;
        }

        // Closing Quote
        if (creditsClosingQuote != null && !creditsClosingQuote.isEmpty()) {
            font.setColor(Color.CYAN);
            font.draw(batch, creditsClosingQuote, 150f, curY, VIRTUAL_WIDTH - 300f, Align.center, true);
            curY -= 100f;
        }

        // Copyright
        if (creditsCopyright != null && !creditsCopyright.isEmpty()) {
            font.setColor(Color.GRAY);
            font.draw(batch, creditsCopyright, 0f, curY, VIRTUAL_WIDTH, Align.center, false);
            curY -= 60f;
        }

        // Skip Prompt - Only show when credits have scrolled halfway up the screen!
        if (creditsScrollY >= VIRTUAL_HEIGHT / 2f) {
            font.setColor(Color.ORANGE);
            font.draw(batch, "[ SPACE / ENTER ] Tekan untuk kembali ke Menu Utama", 0f, 40f, VIRTUAL_WIDTH, Align.center, false);
        }

        batch.end();
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

        Map<String, String> raniaFiles = new HashMap<>();
        // RANIA MAHASISWA
        raniaFiles.put("MAHASISWA_bahagia", "expression/RANIA MAHASISWA/bahagia-Photoroom (9).png");
        raniaFiles.put("MAHASISWA_biasa aja", "expression/RANIA MAHASISWA/biasa aja-Photoroom (9).png");
        raniaFiles.put("MAHASISWA_cemas", "expression/RANIA MAHASISWA/cemas-Photoroom (9).png");
        raniaFiles.put("MAHASISWA_malu", "expression/RANIA MAHASISWA/malu-Photoroom (8).png");
        raniaFiles.put("MAHASISWA_marah", "expression/RANIA MAHASISWA/marah-Photoroom (8).png");
        raniaFiles.put("MAHASISWA_sedih", "expression/RANIA MAHASISWA/sedih-Photoroom (9).png");

        Map<String, String> bagasFiles = new HashMap<>();
        // BAGAS MAHASISWA
        bagasFiles.put("MAHASISWA_bahagia", "expression/BAGAS MAHASISWA/bahagia-Photoroom.png");
        bagasFiles.put("MAHASISWA_biasa aja", "expression/BAGAS MAHASISWA/biasa aja-Photoroom.png");
        bagasFiles.put("MAHASISWA_cemas", "expression/BAGAS MAHASISWA/cemas-Photoroom.png");
        bagasFiles.put("MAHASISWA_malu", "expression/BAGAS MAHASISWA/malu-Photoroom.png");
        bagasFiles.put("MAHASISWA_marah", "expression/BAGAS MAHASISWA/marah-Photoroom.png");
        bagasFiles.put("MAHASISWA_sedih", "expression/BAGAS MAHASISWA/sedih-Photoroom.png");

        loadCharacterExpressionMap("clara", claraFiles);
        loadCharacterExpressionMap("raksha", rakshaFiles);
        loadCharacterExpressionMap("sherly", sherlyFiles);
        loadCharacterExpressionMap("rania", raniaFiles);
        loadCharacterExpressionMap("bagas", bagasFiles);
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

    private Texture getCharacterExpressionTexture(String charKey, String expressionName) {
        if (expressionTextures == null || !expressionTextures.containsKey(charKey)) return null;
        Map<String, Texture> charMap = expressionTextures.get(charKey);
        if (charMap == null || charMap.isEmpty()) return null;

        if (expressionName == null || expressionName.isEmpty()) {
            expressionName = "MAHASISWA_biasa aja";
        }

        // 1. Exact match
        if (charMap.containsKey(expressionName)) {
            return charMap.get(expressionName);
        }

        // 2. Prepend MAHASISWA_ if missing outfit prefix
        if (!expressionName.contains("_")) {
            String prefixed = "MAHASISWA_" + expressionName;
            if (charMap.containsKey(prefixed)) {
                return charMap.get(prefixed);
            }
        }

        // 3. Fallback to default outfit expressions
        if (charMap.containsKey("MAHASISWA_biasa aja")) {
            return charMap.get("MAHASISWA_biasa aja");
        }
        if (charMap.containsKey("LATIHAN_biasa aja")) {
            return charMap.get("LATIHAN_biasa aja");
        }
        if (charMap.containsKey("FINAL_biasa aja")) {
            return charMap.get("FINAL_biasa aja");
        }

        // 4. Return any loaded expression texture for character
        return charMap.values().iterator().next();
    }

    private void updateDialogueExpressions(String speaker, String text) {
        String nodeId = currentNode != null ? currentNode.nodeId : "";

        // Determine active right character first before evaluating speaker expression defaults
        if (nodeId != null && (nodeId.startsWith("BLOCK_") || nodeId.startsWith("PHONE_") || currentBackground == kamarKostTexture || currentBackground == kamarKostMalamTexture || currentBackground == kostOutsideTexture || currentBackground == kostOutsideMalamTexture)) {
            activeRightCharacter = "None";
        } else if ("Sherly".equalsIgnoreCase(speaker) || (nodeId != null && nodeId.contains("SHERLY"))) {
            activeRightCharacter = "Sherly";
        } else if ("Rania".equalsIgnoreCase(speaker) || (nodeId != null && nodeId.contains("RANIA"))) {
            activeRightCharacter = "Rania";
        } else if ("Bagas".equalsIgnoreCase(speaker) || (nodeId != null && nodeId.contains("BAGAS"))) {
            activeRightCharacter = "Bagas";
        } else if ("Clara".equalsIgnoreCase(speaker) || (nodeId != null && (nodeId.contains("CLARA") || nodeId.startsWith("PROLOG_"))) || currentZone == ExplorationZone.DALAM_STUDIO || currentZone == ExplorationZone.UKM_MUSIK || currentZone == ExplorationZone.KAMPUS) {
            activeRightCharacter = "Clara";
        } else {
            activeRightCharacter = "None";
        }

        String nodeExpr = currentNode != null ? currentNode.expression : null;
        if (nodeExpr == null || nodeExpr.isEmpty() || "biasa aja".equals(nodeExpr)) {
            // Auto detect from bracketed actions in text if present
            if (text != null) {
                String lowerText = text.toLowerCase();
                if (lowerText.contains("(tersenyum)") || lowerText.contains("(tertawa)") || lowerText.contains("(bersemangat)") || lowerText.contains("hehe")) {
                    nodeExpr = "bahagia";
                } else if (lowerText.contains("(terkejut)") || lowerText.contains("(pucat)") || lowerText.contains("(panik)") || lowerText.contains("(cemas)") || lowerText.contains("(bingung)")) {
                    nodeExpr = "cemas";
                } else if (lowerText.contains("(menangis)") || lowerText.contains("(frustrasi)") || lowerText.contains("(sedih)") || lowerText.contains("(lesu)")) {
                    nodeExpr = "sedih";
                } else if (lowerText.contains("(marah)") || lowerText.contains("(kesal)") || lowerText.contains("(bentak)")) {
                    nodeExpr = "marah";
                } else if (lowerText.contains("(malu)") || lowerText.contains("(canggung)")) {
                    nodeExpr = "malu";
                }
            }
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
                        } else if (charName.equals("raksha") || charName.equals("raka")) {
                            rakshaExpression = exprName;
                        } else if (charName.equals("sherly")) {
                            sherlyExpression = exprName;
                        } else if (charName.equals("rania")) {
                            raniaExpression = exprName;
                        } else if (charName.equals("bagas")) {
                            bagasExpression = exprName;
                        }
                    }
                }
            } else {
                String exprName = nodeExpr;
                if (!exprName.contains("_")) {
                    exprName = "MAHASISWA_" + exprName;
                }
                
                String lowerSpeaker = speaker != null ? speaker.toLowerCase() : "";
                if ("clara".equalsIgnoreCase(speaker)) {
                    claraExpression = exprName;
                } else if ("sherly".equalsIgnoreCase(speaker)) {
                    sherlyExpression = exprName;
                } else if ("rania".equalsIgnoreCase(speaker)) {
                    raniaExpression = exprName;
                } else if ("bagas".equalsIgnoreCase(speaker)) {
                    bagasExpression = exprName;
                } else if ("raksha".equalsIgnoreCase(speaker) || "raka".equalsIgnoreCase(speaker) || lowerSpeaker.contains("raksha") || lowerSpeaker.contains("raka")) {
                    rakshaExpression = exprName;
                } else {
                    // Update all to default if no speaker
                    rakshaExpression = "MAHASISWA_biasa aja";
                    claraExpression = "MAHASISWA_biasa aja";
                    sherlyExpression = "MAHASISWA_biasa aja";
                    raniaExpression = "MAHASISWA_biasa aja";
                    bagasExpression = "MAHASISWA_biasa aja";
                }
            }
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
                    if (transToDay != -1) {
                        gameState.day = transToDay;
                        remainingDays = gameState.day;
                        syncChapterWithDay();
                        updateTimeOfDay();
                    }
                    state = transNextState;
                    loadNode(transNextNode);
                    if (state == GameplayState.EXPLORATION_STATE) {
                        rakshaX = 100f;
                        rakshaFacingRight = true;
                        currentZone = ExplorationZone.KOST;
                        currentBackground = kamarKostTexture;
                    }
                }
                break;
        }
    }

    private String getDayTransitionQuote(int targetDay) {
        if (targetDay >= 28) {
            return "\"Setiap langkah awal selalu penuh keraguan, namun kepakan nada pertama akan membuka jalan.\"";
        } else if (targetDay >= 24) {
            return "\"Harmoni tidak tercipta dari satu alat musik, melainkan dari detak jantung yang saling menyatu.\"";
        } else if (targetDay >= 20) {
            return "\"Di balik papan tulis studio yang penuh erased notes, mimpi besar mulai menemukan bentuknya.\"";
        } else if (targetDay >= 15) {
            return "\"Bahkan jika senar retak dan tempo melambat, jangan pernah mematikan nyala di dalam dada.\"";
        } else if (targetDay >= 9) {
            return "\"Suara yang jujur tidak pernah goyah oleh badai prasangka.\"";
        } else if (targetDay >= 1) {
            return "\"Lampu panggung menyala terang. Inilah momen saat waktu berhenti untuk menyambut suara kita.\"";
        } else {
            return "\"Resonansi lagu kita akan terus bergema melintasi waktu...\"";
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
                dialogueFont.setColor(1f, 1f, 1f, transAlpha);
                dialogueFont.getData().setScale(1.1f);
                dialogueFont.draw(batch, transNarrativeText, 150f, VIRTUAL_HEIGHT / 2f + 60f, VIRTUAL_WIDTH - 300f, Align.center, true);
                dialogueFont.getData().setScale(1.0f);
            } else if (transPhase == 2) {
                int currentDayDisplay = Math.round(transNumberDisplay);
                
                dialogueFont.setColor(0.3f, 0.8f, 1.0f, transAlpha * 0.85f);
                dialogueFont.getData().setScale(1.1f);
                dialogueFont.draw(batch, "⏳ SISA WAKTU MENJELANG FESTIVAL", 0f, VIRTUAL_HEIGHT / 2f + 90f, VIRTUAL_WIDTH, Align.center, false);
                
                dialogueFont.setColor(0.2f, 0.95f, 0.5f, transAlpha);
                dialogueFont.getData().setScale(2.5f);
                dialogueFont.draw(batch, "SISA HARI: " + currentDayDisplay, 0f, VIRTUAL_HEIGHT / 2f + 20f, VIRTUAL_WIDTH, Align.center, false);
                
                dialogueFont.setColor(1.0f, 0.9f, 0.4f, transAlpha * 0.9f);
                dialogueFont.getData().setScale(1.0f);
                String quote = getDayTransitionQuote(currentDayDisplay);
                dialogueFont.draw(batch, quote, 150f, VIRTUAL_HEIGHT / 2f - 60f, VIRTUAL_WIDTH - 300f, Align.center, true);
            } else if (transPhase == 3) {
                dialogueFont.setColor(0.3f, 0.85f, 1.0f, transAlpha);
                dialogueFont.getData().setScale(2.4f);
                dialogueFont.draw(batch, transChapterTitle, 0f, VIRTUAL_HEIGHT / 2f + 70f, VIRTUAL_WIDTH, Align.center, false);
                dialogueFont.getData().setScale(1.3f);
                dialogueFont.setColor(1f, 1f, 1f, transAlpha);
                dialogueFont.draw(batch, "\"" + transChapterSubtitle + "\"", 0f, VIRTUAL_HEIGHT / 2f - 10f, VIRTUAL_WIDTH, Align.center, false);
                
                dialogueFont.setColor(1.0f, 0.9f, 0.4f, transAlpha * 0.85f);
                dialogueFont.getData().setScale(1.0f);
                String chapterQuote = getDayTransitionQuote(transToDay);
                dialogueFont.draw(batch, chapterQuote, 150f, VIRTUAL_HEIGHT / 2f - 80f, VIRTUAL_WIDTH - 300f, Align.center, true);
            }
            batch.end();
        }
    }

    
    private String getCurrentQuest() {
        if (gameState == null) return "";
        if (gameState.day <= 0) gameState.day = 30;
        if (gameState.day == 30) return "🎯 Objektif (Hari 30): Temui Clara di Ruang UKM Musik.";
        if (gameState.day == 29) return "🎯 Objektif (Hari 29): Temui Clara di Ruang UKM Musik.";
        if (gameState.day == 28) {
            if (gameState.day28EventDone) {
                return "🎯 Objektif (Hari 28): Kembali ke Kamar Kost dan tidur untuk melanjutkan hari.";
            } else {
                return "🎯 Objektif (Hari 28): Temui Bagas di Kantin Kampus.";
            }
        }
        if (gameState.day == 26) {
            if (gameState.day26EventDone) {
                return "🎯 Objektif (Hari 26): Kembali ke Kamar Kost dan tidur untuk melanjutkan hari.";
            } else {
                return "🎯 Objektif (Hari 26): Ajak Sherly jadi manajer band di Kedai Kopi.";
            }
        }
        if (gameState.day == 24) {
            if (gameState.day24EventDone) {
                return "🎯 Objektif (Hari 24): Kembali ke Kamar Kost dan tidur untuk melanjutkan hari.";
            } else {
                return "🎯 Objektif (Hari 24): Temui Rania di Studio Seni Kampus.";
            }
        }
        if (gameState.day == 20 || gameState.day == 19) return "🎯 Objektif (Hari " + gameState.day + "): Latihan pertama dengan band (Di Ruang Studio).";
        if (gameState.day == 18) return "🎯 Objektif (Hari 18): Garap lagu bersama band (Di Ruang Studio).";
        if (gameState.day == 15) return "🎯 Objektif (Hari 15): Latihan intensif bersama band (Di Ruang Studio).";
        if (gameState.day == 12) return "🎯 Objektif (Hari 12): Gladi bersih bersama band (Di Ruang Studio).";
        if (gameState.day == 10) return "🎯 Objektif (Hari 10): Pergi ke UKM Musik untuk menemui anak-anak.";
        if (gameState.day == 8) return "🎯 Objektif (Hari 8): Bubar band? (Di Kamar Kost).";
        if (gameState.day == 7) return "🎯 Objektif (Hari 7): Kabur (Temui Clara di Kampus).";
        if (gameState.day == 5) return "🎯 Objektif (Hari 5): Bukti Aldo terungkap (Di Kamar Kost).";
        if (gameState.day == 3) return "🎯 Objektif (Hari 3): Latihan terakhir (Di Kamar Kost).";
        if (gameState.day == 1) return "🎯 Objektif (Hari 1): Echo Fest Panggung Festival! (Menuju Panggung Kantin Kampus).";
        
        return "🎯 Objektif (Hari " + gameState.day + "): Latihan Gitar di Kamar Kost [E] / Cek HP [TAB].";
    }

    private void renderSaveOverlay() {
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
        
        // Slot buttons
        float btnW = 300f;
        float btnH = 60f;
        float bx = (VIRTUAL_WIDTH - btnW) / 2f;
        
        shapeRenderer.setColor(Color.LIGHT_GRAY);
        shapeRenderer.rect(bx, my + 280, btnW, btnH);
        shapeRenderer.rect(bx, my + 190, btnW, btnH);
        shapeRenderer.rect(bx, my + 100, btnW, btnH);
        
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(bx, my + 30, btnW, 45);
        
        shapeRenderer.end();
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(mx, my, menuW, menuH);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        
        game.getBatch().begin();
        choiceFont.setColor(Color.WHITE);
        choiceFont.draw(game.getBatch(), "PILIH SLOT SIMPAN", mx, my + menuH - 20, menuW, Align.center, false);
        
        String meta1 = SaveManager.getSaveMetadata("savegame_1.dat");
        String meta2 = SaveManager.getSaveMetadata("savegame_2.dat");
        String meta3 = SaveManager.getSaveMetadata("savegame_3.dat");
        
        choiceFont.setColor(Color.BLACK);
        choiceFont.draw(game.getBatch(), "SLOT 1", bx, my + 280 + 50, btnW, Align.center, false);
        choiceFont.draw(game.getBatch(), "SLOT 2", bx, my + 190 + 50, btnW, Align.center, false);
        choiceFont.draw(game.getBatch(), "SLOT 3", bx, my + 100 + 50, btnW, Align.center, false);
        
        font.setColor(Color.DARK_GRAY);
        font.draw(game.getBatch(), meta1.replace("\n", "  |  "), bx, my + 280 + 22, btnW, Align.center, false);
        font.draw(game.getBatch(), meta2.replace("\n", "  |  "), bx, my + 190 + 22, btnW, Align.center, false);
        font.draw(game.getBatch(), meta3.replace("\n", "  |  "), bx, my + 100 + 22, btnW, Align.center, false);
        
        choiceFont.setColor(Color.WHITE);
        choiceFont.draw(game.getBatch(), "Kembali", bx, my + 30 + 32, btnW, Align.center, false);
        
        if (saveSuccessTimer > 0) {
            font.setColor(Color.YELLOW);
            font.draw(game.getBatch(), "Game Berhasil Disimpan di Slot " + savedSlotNum + "!", mx, my + 78, menuW, Align.center, false);
        }
        
        game.getBatch().end();
    }

    private void loadNodeBackgroundOnly(String nodeId) {
        if (nodeId == null || !storyNodes.containsKey(nodeId)) return;
        if (nodeId.equals("PROLOG_START")) {
            currentBackground = prologTextures.get(0);
        } else if (nodeId.startsWith("PROLOG_")) {
            currentBackground = prologTextures.get(4);
        } else if (nodeId.equals("CH1_INTRO") || nodeId.equals("CH1_DAY_NEXT")) {
            currentBackground = kostOutsideTexture;
        } else if (nodeId.equals("CH1_KOST_CHOICES") || nodeId.equals("CH1_KOST_CANCEL") || nodeId.equals("CH1_CHOICE_C_RESULT") || nodeId.equals("CH1_PRACTICE_END") || nodeId.equals("CH3_POST_RESULT_1") || nodeId.equals("CH3_ALDO_RESULT_1")) {
            currentBackground = kamarKostTexture;
        } else if (nodeId.startsWith("CH3_BUS_")) {
            currentBackground = jalanRayaTexture;
        } else if (nodeId.startsWith("END_") || nodeId.startsWith("CREDITS_") || nodeId.equals("GAME_OVER")) {
            currentBackground = skyTexture;
        } else {
            currentBackground = studioTexture;
        }
    }

    private void saveGameToSlot(int slotNum) {
        String slotFile = "savegame_" + slotNum + ".dat";
        gameState.currentZone = currentZone.name();
        gameState.rakshaX = rakshaX;
        gameState.gameplayState = (previousState != null ? previousState : GameplayState.EXPLORATION_STATE).name();
        SaveManager.saveGame(gameState, slotFile);
        savedSlotNum = slotNum;
        saveSuccessTimer = 2.0f;
    }
}
