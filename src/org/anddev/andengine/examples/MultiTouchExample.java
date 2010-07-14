package org.anddev.andengine.examples;

import java.util.HashMap;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.Scene.IOnAreaTouchListener;
import org.anddev.andengine.entity.scene.Scene.IOnSceneTouchListener;
import org.anddev.andengine.entity.scene.Scene.ITouchArea;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.util.FPSLogger;
import org.anddev.andengine.examples.adt.card.Card;
import org.anddev.andengine.extenion.input.touch.controller.MultiTouch;
import org.anddev.andengine.extenion.input.touch.controller.MultiTouchController;
import org.anddev.andengine.extenion.input.touch.controller.MultiTouchException;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.Texture;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.texture.region.TextureRegionFactory;

import android.view.MotionEvent;
import android.widget.Toast;

/**
 * @author Nicolas Gramlich
 * @since 18:20:40 - 18.06.2010
 */
public class MultiTouchExample extends BaseExample {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final int CAMERA_WIDTH = 720;
	private static final int CAMERA_HEIGHT = 480;

	// ===========================================================
	// Fields
	// ===========================================================

	private Camera mCamera;
	private Texture mCardDeckTexture;

	protected HashMap<Integer, Sprite> mPointerIDToSpriteMap = new HashMap<Integer, Sprite>();

	private HashMap<Card, TextureRegion> mCardTotextureRegionMap;

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public Engine onLoadEngine() {
		this.mCamera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		final Engine engine = new Engine(new EngineOptions(true, ScreenOrientation.LANDSCAPE, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), this.mCamera));

		try {
			if(MultiTouch.isSupported(this)) {
				engine.setTouchController(new MultiTouchController());
				if(MultiTouch.isSupportedDistinct(this)) {
					Toast.makeText(this, "MultiTouch detected --> Drag multiple Sprites with multiple fingers!", Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(this, "MultiTouch detected --> Drag multiple Sprites with multiple fingers!\n\n(Your device might have problems to distinguish between separate fingers.)", Toast.LENGTH_LONG).show();
				}
			} else {
				Toast.makeText(this, "Sorry your device does NOT support MultiTouch!\n\n(Falling back to SingleTouch.)", Toast.LENGTH_LONG).show();
			}
		} catch (final MultiTouchException e) {
			Toast.makeText(this, "Sorry your Android Version does NOT support MultiTouch!\n\n(Falling back to SingleTouch.)", Toast.LENGTH_LONG).show();
		}

		return engine;
	}

	@Override
	public void onLoadResources() {
		this.mCardDeckTexture = new Texture(1024, 512, TextureOptions.BILINEAR);

		TextureRegionFactory.createFromAsset(this.mCardDeckTexture, this, "gfx/carddeck_tiled.png", 0, 0);

		this.mCardTotextureRegionMap = new HashMap<Card, TextureRegion>();

		/* Extract the TextureRegion of each card in the whole deck. */
		for(final Card card : Card.values()) {
			final TextureRegion cardTextureRegion = TextureRegionFactory.extractFromTexture(this.mCardDeckTexture, card.getTexturePositionX(), card.getTexturePositionY(), Card.CARD_WIDTH, Card.CARD_HEIGHT);
			this.mCardTotextureRegionMap.put(card, cardTextureRegion);
		}

		this.mEngine.getTextureManager().loadTexture(this.mCardDeckTexture);
	}

	@Override
	public Scene onLoadScene() {
		this.mEngine.registerPreFrameHandler(new FPSLogger());

		final Scene scene = new Scene(1);
		scene.setOnAreaTouchTraversalFrontToBack();

		this.addCard(scene, Card.CLUB_ACE, 200, 100);
		this.addCard(scene, Card.HEART_ACE, 200, 260);
		this.addCard(scene, Card.DIAMOND_ACE, 440, 100);
		this.addCard(scene, Card.SPADE_ACE, 440, 260);

		scene.setBackgroundColor(0.09804f, 0.6274f, 0.8784f);

		scene.setOnSceneTouchListener(new IOnSceneTouchListener() {
			@Override
			public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent pSceneTouchEvent) {
				return MultiTouchExample.this.updateSelectedCardPosition(pSceneTouchEvent);
			}
		});

		scene.setOnAreaTouchListener(new IOnAreaTouchListener() {
			@Override
			public boolean onAreaTouched(final ITouchArea pTouchArea, final TouchEvent pSceneTouchEvent) {
				if(pSceneTouchEvent.getAction() == MotionEvent.ACTION_DOWN) {
					final Sprite sprite = (Sprite) pTouchArea;
					sprite.setScale(1.2f);
					/* Associate Pointer with card-sprite. */
					MultiTouchExample.this.mPointerIDToSpriteMap.put(pSceneTouchEvent.getPointerID(), sprite);
					return true;
				} else {
					return MultiTouchExample.this.updateSelectedCardPosition(pSceneTouchEvent);
				}
			}
		});

		return scene;
	}

	@Override
	public void onLoadComplete() {

	}

	// ===========================================================
	// Methods
	// ===========================================================

	private boolean updateSelectedCardPosition(final TouchEvent pSceneTouchEvent) {
		if(pSceneTouchEvent.getAction() == MotionEvent.ACTION_MOVE) {
			/* Get associated card-sprite (if available). */
			final Sprite card = this.mPointerIDToSpriteMap.get(pSceneTouchEvent.getPointerID());
			if(card != null) {
				card.setPosition(pSceneTouchEvent.getX() - Card.CARD_WIDTH / 2, pSceneTouchEvent.getY() - Card.CARD_HEIGHT / 2);
			}
		} else if(pSceneTouchEvent.getAction() == MotionEvent.ACTION_UP) {
			/* 'Reset' associated card-sprite (if available). */
			final Sprite removed = this.mPointerIDToSpriteMap.remove(pSceneTouchEvent.getPointerID());
			if(removed != null) {
				removed.setScale(1.0f);
			}
		}
		return true;
	}

	private void addCard(final Scene pScene, final Card pCard, final int pX, final int pY) {
		final Sprite sprite = new Sprite(pX, pY, this.mCardTotextureRegionMap.get(pCard));

		pScene.getTopLayer().addEntity(sprite);
		pScene.registerTouchArea(sprite);
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}