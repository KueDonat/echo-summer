package com.echosummer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class SettingsScreen implements Screen {
    private final Main game;
    private final Screen previousScreen;
    private Stage stage;
    private Skin skin;
    
    private Label volumeLabel;
    private TextButton btnEasy;
    private TextButton btnMedium;
    private TextButton btnHard;

    public SettingsScreen(Main game, Screen previousScreen) {
        this.game = game;
        this.previousScreen = previousScreen;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        createDefaultSkin();

        Table table = new Table();
        table.setFillParent(true);
        table.center();

        Label.LabelStyle headerStyle = new Label.LabelStyle(skin.getFont("header"), new Color(0.95f, 0.96f, 0.98f, 1f));
        Label headerLabel = new Label("SETTINGS", headerStyle);
        headerLabel.setAlignment(Align.center);

        // Volume Controls
        Label.LabelStyle labelStyle = new Label.LabelStyle(skin.getFont("default"), new Color(0.8f, 0.82f, 0.88f, 1f));
        volumeLabel = new Label("Volume: " + Math.round(SettingsManager.getVolume() * 100f) + "%", labelStyle);
        volumeLabel.setAlignment(Align.center);
        
        TextButton btnVolDown = new TextButton("-", skin);
        TextButton btnVolUp = new TextButton("+", skin);
        
        btnVolDown.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                float v = SettingsManager.getVolume();
                SettingsManager.setVolume(v - 0.1f);
                updateVolumeUI();
            }
        });
        
        btnVolUp.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                float v = SettingsManager.getVolume();
                SettingsManager.setVolume(v + 0.1f);
                updateVolumeUI();
            }
        });
        
        Table volTable = new Table();
        volTable.add(btnVolDown).width(50).height(40).padRight(20);
        volTable.add(volumeLabel).width(150);
        volTable.add(btnVolUp).width(50).height(40).padLeft(20);

        // Difficulty Controls
        Label diffLabel = new Label("Difficulty", labelStyle);
        diffLabel.setAlignment(Align.center);
        
        btnEasy = new TextButton("EASY", skin);
        btnMedium = new TextButton("MEDIUM", skin);
        btnHard = new TextButton("HARD", skin);
        
        btnEasy.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                SettingsManager.setDifficulty(0);
                updateDifficultyUI();
            }
        });
        
        btnMedium.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                SettingsManager.setDifficulty(1);
                updateDifficultyUI();
            }
        });
        
        btnHard.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                SettingsManager.setDifficulty(2);
                updateDifficultyUI();
            }
        });
        
        Table diffTable = new Table();
        diffTable.add(btnEasy).width(100).height(40).pad(10);
        diffTable.add(btnMedium).width(100).height(40).pad(10);
        diffTable.add(btnHard).width(100).height(40).pad(10);
        
        updateDifficultyUI();

        // Back button
        TextButton backButton = new TextButton("Back", skin);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (previousScreen != null) {
                    game.setScreen(previousScreen);
                } else {
                    game.setScreen(new MainMenuScreen(game));
                }
            }
        });

        table.add(headerLabel).padBottom(40).row();
        table.add(volTable).padBottom(20).row();
        table.add(diffLabel).padBottom(10).row();
        table.add(diffTable).padBottom(40).row();
        table.add(backButton).width(200).height(50);

        stage.addActor(table);
    }
    
    private void updateVolumeUI() {
        volumeLabel.setText("Volume: " + Math.round(SettingsManager.getVolume() * 100f) + "%");
        game.updateMusicVolume();
    }
    
    private void updateDifficultyUI() {
        int diff = SettingsManager.getDifficulty();
        btnEasy.getLabel().setColor(diff == 0 ? Color.YELLOW : Color.WHITE);
        btnMedium.getLabel().setColor(diff == 1 ? Color.YELLOW : Color.WHITE);
        btnHard.getLabel().setColor(diff == 2 ? Color.YELLOW : Color.WHITE);
    }

    private void createDefaultSkin() {
        skin = new Skin();

        com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator generator = new com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator(Gdx.files.internal("NotoSans-Regular.ttf"));
        com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter parameter = 
            new com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter();
        
        parameter.size = 20;
        BitmapFont defaultFont = generator.generateFont(parameter);
        skin.add("default", defaultFont);

        parameter.size = 32;
        BitmapFont headerFont = generator.generateFont(parameter);
        skin.add("header", headerFont);

        generator.dispose();

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("whiteTexture", new Texture(pixmap));
        pixmap.dispose();

        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.up = skin.newDrawable("whiteTexture", new Color(0.9f, 0.9f, 0.95f, 0.15f));
        textButtonStyle.down = skin.newDrawable("whiteTexture", new Color(0.9f, 0.9f, 0.95f, 0.35f));
        textButtonStyle.over = skin.newDrawable("whiteTexture", new Color(0.9f, 0.9f, 0.95f, 0.25f));
        textButtonStyle.font = skin.getFont("default");
        textButtonStyle.fontColor = new Color(0.9f, 0.92f, 0.95f, 1f);
        textButtonStyle.downFontColor = Color.WHITE;
        textButtonStyle.overFontColor = Color.WHITE;
        
        skin.add("default", textButtonStyle);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

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
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
    }
}
