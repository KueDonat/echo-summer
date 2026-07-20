package com.echosummer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * MainMenuScreen represents the main menu of Echo Summer.
 * Redesigned to match the reference image:
 * - Two-line bold title: ECHO SUMMER.
 * - Heartbeat line below the title.
 * - 6 custom buttons with torn-paper style backgrounds and circular icons.
 */
public class MainMenuScreen implements Screen {
    private final Main game;
    private Stage stage;
    private Skin skin;
    private Texture backgroundTexture;
    private Texture heartbeatTexture;
    
    // Textures for buttons and icons so they can be disposed
    private final List<Texture> managedTextures = new ArrayList<>();

    public MainMenuScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // 1. Play Background Music
        game.playMenuMusic();

        // 2. Load Background Texture
        backgroundTexture = new Texture(Gdx.files.internal("background.png"));
        backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        // 3. Initialize UI Assets and Fonts
        createSkinAndAssets();

        // 3. Construct Layout Table
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.left().top();
        mainTable.padLeft(80).padTop(60);

        // Title Style
        Label.LabelStyle titleStyle = new Label.LabelStyle(skin.getFont("titleFont"), new Color(0.97f, 0.96f, 0.95f, 1f));
        Label titleEcho = new Label("ECHO", titleStyle);
        Label titleSummer = new Label("SUMMER", titleStyle);

        // Heartbeat line decoration
        Image heartbeatLine = new Image(heartbeatTexture);

        // Create interactive buttons
        Button continueBtn = createMenuButton("CONTINUE", "Lanjutkan perjalananmu", "continue");
        Button newGameBtn = createMenuButton("NEW GAME", "Mulai kisah baru", "new_game");
        Button loadGameBtn = createMenuButton("LOAD GAME", "Muat permainan", "load_game");
        Button galleryBtn = createMenuButton("GALLERY", "Lihat ilustrasi & momen", "gallery");
        Button settingsBtn = createMenuButton("SETTINGS", "Pengaturan permainan", "settings");
        Button exitBtn = createMenuButton("EXIT", "Keluar dari Echo Summer", "exit");

        // Configure Listeners
        configureButton(continueBtn, new Runnable() {
            @Override
            public void run() {
                loadSavedGame();
            }
        });

        configureButton(newGameBtn, new Runnable() {
            @Override
            public void run() {
                startNewGame();
            }
        });

        configureButton(loadGameBtn, new Runnable() {
            @Override
            public void run() {
                loadSavedGame();
            }
        });

        configureButton(galleryBtn, new Runnable() {
            @Override
            public void run() {
                Gdx.app.log("MainMenuScreen", "Opening Gallery (Not implemented yet)...");
            }
        });

        configureButton(settingsBtn, new Runnable() {
            @Override
            public void run() {
                game.setScreen(new SettingsScreen(game, MainMenuScreen.this));
            }
        });

        configureButton(exitBtn, new Runnable() {
            @Override
            public void run() {
                exitGame();
            }
        });

        // Build Table Hierarchy
        mainTable.add(titleEcho).align(Align.left).row();
        mainTable.add(titleSummer).align(Align.left).padTop(-12).row();
        mainTable.add(heartbeatLine).align(Align.left).padTop(10).padBottom(25).width(320).height(30).row();
        
        mainTable.add(continueBtn).width(350).height(70).padBottom(10).align(Align.left).row();
        mainTable.add(newGameBtn).width(350).height(70).padBottom(10).align(Align.left).row();
        mainTable.add(loadGameBtn).width(350).height(70).padBottom(10).align(Align.left).row();
        mainTable.add(galleryBtn).width(350).height(70).padBottom(10).align(Align.left).row();
        mainTable.add(settingsBtn).width(350).height(70).padBottom(10).align(Align.left).row();
        mainTable.add(exitBtn).width(350).height(70).align(Align.left);

        stage.addActor(mainTable);
    }

    private void createSkinAndAssets() {
        skin = new Skin();

        // Load custom fonts
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("NotoSans-Regular.ttf"));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();

        // 1. Large Title Font (with shadow and outline for premium look & high contrast)
        parameter.size = 72;
        parameter.borderWidth = 2f;
        parameter.borderColor = new Color(0.05f, 0.08f, 0.16f, 0.6f);
        parameter.shadowColor = new Color(0f, 0f, 0f, 0.35f);
        parameter.shadowOffsetX = 3;
        parameter.shadowOffsetY = 3;
        BitmapFont titleFont = generator.generateFont(parameter);
        skin.add("titleFont", titleFont);

        // Reset parameters for Button Fonts
        parameter.borderWidth = 0f;
        parameter.shadowOffsetX = 0;
        parameter.shadowOffsetY = 0;

        // 2. Button Title Font (Bold style size)
        parameter.size = 18;
        BitmapFont buttonTitleFont = generator.generateFont(parameter);
        skin.add("buttonTitleFont", buttonTitleFont);

        // 3. Button Subtitle Font (Muted normal font)
        parameter.size = 12;
        BitmapFont buttonSubtitleFont = generator.generateFont(parameter);
        skin.add("buttonSubtitleFont", buttonSubtitleFont);

        generator.dispose();

        // Generate Torn Paper textures programmatically
        Texture buttonUp = generateTornPaperTexture(350, 70, new Color(0.97f, 0.96f, 0.95f, 0.85f));
        Texture buttonOver = generateTornPaperTexture(350, 70, new Color(1.0f, 0.99f, 0.98f, 0.95f));
        Texture buttonDown = generateTornPaperTexture(350, 70, new Color(0.90f, 0.89f, 0.87f, 0.9f));
        
        skin.add("buttonUp", buttonUp);
        skin.add("buttonOver", buttonOver);
        skin.add("buttonDown", buttonDown);
        
        managedTextures.add(buttonUp);
        managedTextures.add(buttonOver);
        managedTextures.add(buttonDown);

        // Generate ECG / Heartbeat Line texture
        heartbeatTexture = generateHeartbeatTexture(320, 30);
    }

    private Texture generateTornPaperTexture(int width, int height, Color color) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0, 0, 0, 0));
        pixmap.fill();
        pixmap.setColor(color);
        
        for (int y = 0; y < height; y++) {
            // Jagged tooth pattern on the right edge
            int toothSize = 10;
            int toothPhase = y % toothSize;
            int indent = (toothPhase < toothSize / 2) ? toothPhase : (toothSize - toothPhase);
            int rightEdge = width - 15 + indent;
            
            for (int x = 0; x < rightEdge; x++) {
                pixmap.drawPixel(x, y);
            }
        }
        
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    private Texture generateHeartbeatTexture(int width, int height) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0, 0, 0, 0));
        pixmap.fill();
        
        pixmap.setColor(new Color(0.97f, 0.96f, 0.95f, 0.9f));
        int yOff = height / 2;
        
        // Left baseline
        pixmap.drawLine(0, yOff, 220, yOff);
        pixmap.drawLine(0, yOff + 1, 220, yOff + 1);

        // ECG Pulse
        // 1. Dip down
        pixmap.drawLine(220, yOff, 224, yOff + 3);
        pixmap.drawLine(220, yOff + 1, 224, yOff + 4);

        // 2. Rise up
        pixmap.drawLine(224, yOff + 3, 230, 2);
        pixmap.drawLine(224, yOff + 4, 230, 3);

        // 3. Deep drop
        pixmap.drawLine(230, 2, 236, 28);
        pixmap.drawLine(230, 3, 236, 29);

        // 4. Return to baseline
        pixmap.drawLine(236, 28, 240, yOff);
        pixmap.drawLine(236, 29, 240, yOff + 1);

        // Right baseline
        pixmap.drawLine(240, yOff, width, yOff);
        pixmap.drawLine(240, yOff + 1, width, yOff + 1);

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    private Texture generateIconTexture(String type) {
        int size = 44;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        
        pixmap.setColor(new Color(0, 0, 0, 0));
        pixmap.fill();
        
        Color circleColor = new Color(0.1f, 0.15f, 0.31f, 1f);
        pixmap.setColor(circleColor);
        pixmap.fillCircle(size / 2, size / 2, size / 2 - 1);
        
        pixmap.setColor(Color.WHITE);
        int center = size / 2;
        
        if (type.equals("continue")) {
            int x1 = center - 5;
            int y1 = center - 8;
            int x2 = center - 5;
            int y2 = center + 8;
            int x3 = center + 8;
            int y3 = center;
            pixmap.fillTriangle(x1, y1, x2, y2, x3, y3);
        } else if (type.equals("new_game")) {
            pixmap.fillRectangle(center - 8, center - 1, 16, 3);
            pixmap.fillRectangle(center - 1, center - 8, 3, 16);
        } else if (type.equals("load_game")) {
            pixmap.fillRectangle(center - 10, center - 5, 20, 11);
            pixmap.fillRectangle(center - 10, center - 8, 8, 3);
        } else if (type.equals("gallery")) {
            pixmap.fillRectangle(center - 9, center - 7, 18, 14);
            pixmap.setColor(circleColor);
            pixmap.fillRectangle(center - 7, center - 5, 14, 10);
            
            pixmap.setColor(Color.WHITE);
            pixmap.fillTriangle(center - 7, center + 4, center - 1, center - 2, center + 5, center + 4);
            pixmap.fillTriangle(center - 2, center + 4, center + 2, center + 0, center + 6, center + 4);
            pixmap.fillCircle(center + 3, center - 2, 2);
        } else if (type.equals("settings")) {
            pixmap.fillCircle(center, center, 7);
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4;
                int tx = (int) (center + Math.cos(angle) * 8);
                int ty = (int) (center + Math.sin(angle) * 8);
                pixmap.fillCircle(tx, ty, 2);
            }
            pixmap.setColor(circleColor);
            pixmap.fillCircle(center, center, 3);
        } else if (type.equals("exit")) {
            pixmap.fillRectangle(center - 9, center - 8, 3, 16);
            pixmap.fillRectangle(center - 6, center - 8, 7, 3);
            pixmap.fillRectangle(center - 6, center + 5, 7, 3);
            
            pixmap.fillRectangle(center - 5, center - 1, 9, 3);
            pixmap.fillTriangle(center + 1, center - 5, center + 7, center, center + 1, center + 5);
        }
        
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    private Button createMenuButton(String title, String subtitle, String iconType) {
        Button.ButtonStyle style = new Button.ButtonStyle();
        style.up = skin.newDrawable("buttonUp");
        style.over = skin.newDrawable("buttonOver");
        style.down = skin.newDrawable("buttonDown");
        
        Button button = new Button(style);
        button.setTransform(true);
        button.setOrigin(Align.center);
        
        Table cellTable = new Table();
        cellTable.left().center();
        
        Texture iconTexture = generateIconTexture(iconType);
        managedTextures.add(iconTexture);
        
        Image iconImage = new Image(iconTexture);
        cellTable.add(iconImage).size(44, 44).padLeft(14).padRight(16);
        
        Table textTable = new Table();
        textTable.left();
        
        Label.LabelStyle titleStyle = new Label.LabelStyle(skin.getFont("buttonTitleFont"), new Color(0.1f, 0.15f, 0.31f, 1f));
        Label titleLabel = new Label(title, titleStyle);
        textTable.add(titleLabel).align(Align.left).row();
        
        Label.LabelStyle subtitleStyle = new Label.LabelStyle(skin.getFont("buttonSubtitleFont"), new Color(0.35f, 0.4f, 0.5f, 1f));
        Label subtitleLabel = new Label(subtitle, subtitleStyle);
        textTable.add(subtitleLabel).align(Align.left).padTop(2);
        
        cellTable.add(textTable).expandX().left();
        button.add(cellTable).expand().fill();
        
        return button;
    }

    private void configureButton(final Button button, final Runnable clickAction) {
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                clickAction.run();
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                if (pointer == -1) {
                    button.clearActions();
                    button.addAction(Actions.parallel(
                        Actions.scaleTo(1.03f, 1.03f, 0.15f, Interpolation.fade),
                        Actions.color(Color.WHITE, 0.15f)
                    ));
                }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                super.exit(event, x, y, pointer, toActor);
                if (pointer == -1) {
                    button.clearActions();
                    button.addAction(Actions.parallel(
                        Actions.scaleTo(1.0f, 1.0f, 0.15f, Interpolation.fade),
                        Actions.color(Color.WHITE, 0.15f)
                    ));
                }
            }
        });
    }

    private void startNewGame() {
        Gdx.app.log("MainMenuScreen", "Initializing new game data. Starting Prologue...");
        game.setScreen(new GameplayScreen(game, false));
    }

    private void loadSavedGame() {
        String saveFileName = "savegame.dat";
        Gdx.app.log("MainMenuScreen", "Attempting to load saved game...");
        
        try {
            com.badlogic.gdx.files.FileHandle file = Gdx.files.local(saveFileName);

            if (!file.exists()) {
                throw new FileNotFoundException("Save file '" + saveFileName + "' does not exist.");
            }

            String saveContent = file.readString();
            Gdx.app.log("MainMenuScreen", "Save file read successfully: " + saveContent);
            game.setScreen(new GameplayScreen(game, true));

        } catch (FileNotFoundException e) {
            Gdx.app.error("MainMenuScreen", "Load Game Failed: " + e.getMessage());
            
            // Auto-recovery
            try {
                com.badlogic.gdx.files.FileHandle file = Gdx.files.local(saveFileName);
                file.writeString("Level=Prologue;Timestamp=" + System.currentTimeMillis() + ";MusicTrack=EchoSummerIntro", false);
                Gdx.app.log("MainMenuScreen", "Default save file created. Retrying loading flow...");
                game.setScreen(new GameplayScreen(game, true));
            } catch (Exception ex) {
                Gdx.app.error("MainMenuScreen", "Failed to create fallback save file: " + ex.getMessage());
            }

        } catch (Exception e) {
            Gdx.app.error("MainMenuScreen", "Unexpected I/O exception occurred: " + e.getMessage());
        }
    }

    private void openSettings() {
        Gdx.app.log("MainMenuScreen", "Opening settings...");
        game.setScreen(new SettingsScreen(game, this));
    }

    private void exitGame() {
        Gdx.app.log("MainMenuScreen", "Exiting game safely.");
        Gdx.app.exit();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.08f, 0.12f, 0.15f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        // Draw Background Image
        game.getBatch().begin();
        game.getBatch().draw(backgroundTexture, 0, 0, width, height);
        game.getBatch().end();

        // Render UI Stage
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        if (stage != null) {
            stage.dispose();
        }
        if (skin != null) {
            skin.dispose();
        }
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
        }
        if (heartbeatTexture != null) {
            heartbeatTexture.dispose();
        }
        for (Texture tex : managedTextures) {
            if (tex != null) {
                tex.dispose();
            }
        }
        managedTextures.clear();
    }
}
