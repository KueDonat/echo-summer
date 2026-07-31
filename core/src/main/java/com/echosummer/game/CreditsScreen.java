package com.echosummer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.ArrayList;
import java.util.List;

/**
 * CreditsScreen renders animated rolling credits loaded from credits.json.
 * Accessible from MainMenuScreen or directly after game completion.
 */
public class CreditsScreen implements Screen {
    private final Main game;
    private final Screen previousScreen;
    
    private Stage stage;
    private Skin skin;
    private Texture backgroundTexture;
    private ShapeRenderer shapeRenderer;
    
    private BitmapFont titleFont;
    private BitmapFont subtitleFont;
    private BitmapFont headerFont;
    private BitmapFont nameFont;
    private BitmapFont quoteFont;
    private BitmapFont copyrightFont;
    private BitmapFont backBtnFont;
    
    private String creditsTitle = "ECHO SUMMER";
    private String creditsSubtitle = "";
    private String creditsClosingQuote = "";
    private String creditsCopyright = "";
    
    private static class CreditSection {
        String header;
        List<String> names = new ArrayList<>();
    }
    
    private final List<CreditSection> creditsList = new ArrayList<>();
    private float scrollY = -100f;
    private boolean isDragging = false;
    private float lastDragY = 0f;
    private final List<Texture> managedTextures = new ArrayList<>();
    private com.badlogic.gdx.audio.Music creditsMusic;

    public CreditsScreen(Main game, Screen previousScreen) {
        this.game = game;
        this.previousScreen = previousScreen;
    }

    public CreditsScreen(Main game) {
        this(game, null);
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        shapeRenderer = new ShapeRenderer();
        
        try {
            if (Gdx.files.internal("music/Gravits.mp3").exists()) {
                creditsMusic = Gdx.audio.newMusic(Gdx.files.internal("music/Gravits.mp3"));
                creditsMusic.setLooping(false);
                creditsMusic.setVolume(SettingsManager.getVolume());
                creditsMusic.play();
            }
        } catch (Exception e) {
            Gdx.app.error("CreditsScreen", "Error playing Gravits.mp3: " + e.getMessage());
        }

        loadCreditsJson();
        createFontsAndAssets();

        // Setup input processor with multiplexer so back button works and key/touch gestures work
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(stage);
        multiplexer.addProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK || keycode == Input.Keys.SPACE || keycode == Input.Keys.ENTER) {
                    returnToPreviousScreen();
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                isDragging = true;
                lastDragY = screenY;
                return super.touchDown(screenX, screenY, pointer, button);
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                isDragging = false;
                return super.touchUp(screenX, screenY, pointer, button);
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (isDragging) {
                    float deltaY = lastDragY - screenY;
                    scrollY += deltaY;
                    lastDragY = screenY;
                    return true;
                }
                return false;
            }

            @Override
            public boolean scrolled(float amountX, float amountY) {
                scrollY += amountY * 30f;
                return true;
            }
        });
        Gdx.input.setInputProcessor(multiplexer);

        // Load background
        if (Gdx.files.internal("background.png").exists()) {
            backgroundTexture = new Texture(Gdx.files.internal("background.png"));
            backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }

        // Add Floating Back Button at Bottom Center
        Table topTable = new Table();
        topTable.setFillParent(true);
        topTable.bottom().padBottom(30);

        Button backButton = createBackButton();
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                returnToPreviousScreen();
            }
        });

        topTable.add(backButton).width(260).height(50);
        stage.addActor(topTable);
    }

    private void loadCreditsJson() {
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
            }
        } catch (Exception e) {
            Gdx.app.error("CreditsScreen", "Failed to load credits.json: " + e.getMessage());
        }
    }

    private void createFontsAndAssets() {
        skin = new Skin();

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("NotoSans-Regular.ttf"));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();

        // 1. Title Font
        parameter.size = 42;
        parameter.borderWidth = 1.5f;
        parameter.borderColor = new Color(0.05f, 0.08f, 0.16f, 0.8f);
        parameter.shadowColor = new Color(0f, 0f, 0f, 0.4f);
        parameter.shadowOffsetX = 2;
        parameter.shadowOffsetY = 2;
        titleFont = generator.generateFont(parameter);

        // 2. Subtitle Font
        parameter.size = 18;
        parameter.borderWidth = 0;
        parameter.shadowOffsetX = 0;
        parameter.shadowOffsetY = 0;
        subtitleFont = generator.generateFont(parameter);

        // 3. Header Font
        parameter.size = 22;
        parameter.borderWidth = 1f;
        parameter.borderColor = new Color(0.1f, 0.1f, 0.2f, 0.5f);
        headerFont = generator.generateFont(parameter);

        // 4. Name Font
        parameter.size = 18;
        parameter.borderWidth = 0;
        nameFont = generator.generateFont(parameter);

        // 5. Quote Font
        parameter.size = 16;
        quoteFont = generator.generateFont(parameter);

        // 6. Copyright Font
        parameter.size = 14;
        copyrightFont = generator.generateFont(parameter);

        // 7. Back Button Font
        parameter.size = 16;
        backBtnFont = generator.generateFont(parameter);
        skin.add("backBtnFont", backBtnFont);

        generator.dispose();

        // Generate Back Button Texture
        Texture buttonUp = generateButtonTexture(260, 50, new Color(0.15f, 0.20f, 0.35f, 0.85f));
        Texture buttonOver = generateButtonTexture(260, 50, new Color(0.25f, 0.35f, 0.55f, 0.95f));
        
        skin.add("buttonUp", buttonUp);
        skin.add("buttonOver", buttonOver);
        
        managedTextures.add(buttonUp);
        managedTextures.add(buttonOver);
    }

    private Texture generateButtonTexture(int width, int height, Color color) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fillRectangle(0, 0, width, height);
        
        pixmap.setColor(new Color(0.97f, 0.96f, 0.95f, 0.6f));
        pixmap.drawRectangle(0, 0, width, height);
        
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    private Button createBackButton() {
        Button.ButtonStyle style = new Button.ButtonStyle();
        style.up = skin.newDrawable("buttonUp");
        style.over = skin.newDrawable("buttonOver");
        
        Button button = new Button(style);
        button.setTransform(true);
        button.setOrigin(Align.center);
        
        Label.LabelStyle labelStyle = new Label.LabelStyle(skin.getFont("backBtnFont"), new Color(0.97f, 0.96f, 0.95f, 1f));
        Label label = new Label("[ ESC / TAP ] KEMBALI MENU", labelStyle);
        button.add(label).expand().center();
        
        return button;
    }

    private void returnToPreviousScreen() {
        if (creditsMusic != null) {
            if (creditsMusic.isPlaying()) {
                creditsMusic.stop();
            }
            creditsMusic.dispose();
            creditsMusic = null;
        }
        if (previousScreen != null) {
            game.setScreen(previousScreen);
        } else {
            game.setScreen(new MainMenuScreen(game));
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.07f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.getViewport().apply();
        SpriteBatch batch = game.getBatch();
        batch.setProjectionMatrix(stage.getCamera().combined);

        float screenW = stage.getViewport().getWorldWidth();
        float screenH = stage.getViewport().getWorldHeight();

        // 1. Draw Background Image
        if (backgroundTexture != null) {
            batch.begin();
            batch.setColor(0.3f, 0.3f, 0.4f, 1f);
            batch.draw(backgroundTexture, 0, 0, screenW, screenH);
            batch.setColor(Color.WHITE);
            batch.end();
        }

        // 2. Dark Overlay
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(stage.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0.04f, 0.06f, 0.10f, 0.88f));
        shapeRenderer.rect(0, 0, screenW, screenH);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // 3. Auto Scroll synced to Gravits.mp3 music duration
        if (!isDragging) {
            float contentHeight = 140f;
            for (CreditSection sec : creditsList) {
                contentHeight += 38f + sec.names.size() * 32f + 45f;
            }
            contentHeight += 250f;
            float totalDistance = contentHeight + 400f;

            if (creditsMusic != null && creditsMusic.isPlaying()) {
                float pos = creditsMusic.getPosition();
                float duration = 275.0f; // Gravits.mp3 total duration (~4m35s)
                float progress = Math.min(1.0f, Math.max(0.0f, pos / duration));
                scrollY = -100f + progress * totalDistance;
            } else {
                scrollY += delta * 7.5f;
            }
        }

        // 4. Render Movie Credits Text
        batch.begin();

        float curY = scrollY;

        // Title
        titleFont.setColor(new Color(0.98f, 0.85f, 0.35f, 1f));
        titleFont.draw(batch, creditsTitle, 0f, curY, screenW, Align.center, false);
        curY -= 55f;

        // Subtitle
        subtitleFont.setColor(new Color(0.85f, 0.88f, 0.95f, 0.9f));
        subtitleFont.draw(batch, creditsSubtitle, 0f, curY, screenW, Align.center, false);
        curY -= 85f;

        // Sections
        for (CreditSection sec : creditsList) {
            headerFont.setColor(new Color(1.0f, 0.80f, 0.30f, 1f));
            headerFont.draw(batch, sec.header, 0f, curY, screenW, Align.center, false);
            curY -= 38f;

            nameFont.setColor(Color.WHITE);
            for (String name : sec.names) {
                nameFont.draw(batch, name, 0f, curY, screenW, Align.center, false);
                curY -= 32f;
            }
            curY -= 45f;
        }

        // Closing Quote
        if (creditsClosingQuote != null && !creditsClosingQuote.isEmpty()) {
            quoteFont.setColor(new Color(0.5f, 0.9f, 0.95f, 0.95f));
            quoteFont.draw(batch, creditsClosingQuote, 100f, curY, screenW - 200f, Align.center, true);
            curY -= 90f;
        }

        // Copyright
        if (creditsCopyright != null && !creditsCopyright.isEmpty()) {
            copyrightFont.setColor(new Color(0.6f, 0.65f, 0.7f, 0.8f));
            copyrightFont.draw(batch, creditsCopyright, 0f, curY, screenW, Align.center, false);
            curY -= 60f;
        }

        // Loop credits if scrolled past content
        if (curY > screenH + 200f) {
            scrollY = -100f;
        }

        batch.end();

        // 5. Render Stage
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
        if (creditsMusic != null) {
            if (creditsMusic.isPlaying()) {
                creditsMusic.stop();
            }
            creditsMusic.dispose();
            creditsMusic = null;
        }
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (backgroundTexture != null) backgroundTexture.dispose();

        if (titleFont != null) titleFont.dispose();
        if (subtitleFont != null) subtitleFont.dispose();
        if (headerFont != null) headerFont.dispose();
        if (nameFont != null) nameFont.dispose();
        if (quoteFont != null) quoteFont.dispose();
        if (copyrightFont != null) copyrightFont.dispose();
        if (backBtnFont != null) backBtnFont.dispose();

        for (Texture tex : managedTextures) {
            if (tex != null) tex.dispose();
        }
        managedTextures.clear();
    }
}
