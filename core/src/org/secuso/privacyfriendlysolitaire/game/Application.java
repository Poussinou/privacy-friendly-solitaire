package org.secuso.privacyfriendlysolitaire.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.viewport.Viewport;

import org.secuso.privacyfriendlysolitaire.CallBackListener;
import org.secuso.privacyfriendlysolitaire.HistorianListener;
import org.secuso.privacyfriendlysolitaire.ScoreListener;
import org.secuso.privacyfriendlysolitaire.generator.GeneratorSolitaireInstance;

/**
 * @author: I. Dix
 * the outer application, holding everything together (model, view, controller)
 * it is responsible for creating and redrawing the stage and is the contact point from the Android app
 */
public class Application extends ApplicationAdapter implements ScoreListener, HistorianListener {
    private Stage stage;

    private CallBackListener listener;

    // state of game
    private SolitaireGame game;
    private View view;
    private Controller controller;

    private Scorer scorer;
    private Historian historian;

    private int cardDrawMode;
    private int scoreMode;

    private Color backgroundColour;

    public void customConstructor(int cardDrawMode, int scoreMode, Color backgroundColour) {
        this.cardDrawMode = cardDrawMode;
        this.scoreMode = scoreMode;
        this.backgroundColour = backgroundColour;
    }

    @Override
    public void create() {
        stage = new Stage();
        initMVC();
    }

    private void initMVC() {
        game = GeneratorSolitaireInstance.buildPlayableSolitaireInstance(cardDrawMode, scoreMode);

        initVC();

        Gdx.input.setInputProcessor(new GestureDetector(controller));
    }

    private void initVC() {
        view = new View(game, stage);
        game.registerGameListener(view);

        if (scoreMode == Constants.MODE_STANDARD) {
            scorer = new StandardScorer();
        } else if (scoreMode == Constants.MODE_VEGAS) {
            scorer = new VegasScorer();
        }
        game.registerGameListener(scorer);
        scorer.registerScoreListener(this);
        scorer.update(game);

        historian = new Historian();
        game.registerGameListener(historian);
        historian.registerHistorianListener(this);
        historian.update(game);

        controller = new Controller(game, view);
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(backgroundColour.r, backgroundColour.g, backgroundColour.b, backgroundColour.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        if (game.isWon() && listener != null) {
            listener.onWon();
        }

        stage.act();
        stage.draw();
    }


    @Override
    public void dispose() {
        for (Actor actor : stage.getActors()) {
            actor.addAction(Actions.removeActor());
        }
    }

    public void registerCallBackListener(CallBackListener listener) {
        this.listener = listener;
    }

    public void undo() {
        if (historian.canUndo()) {
            game.deleteGameListeners();
//            game = null;
//            System.gc();
            game = historian.undo();
            reinitGameListeners();

            // clear stage
            final Viewport v = stage.getViewport();
            stage.clear();
            Gdx.app.postRunnable(new Runnable() {
                @Override
                public void run() {
                    stage = new Stage(v, new SpriteBatch());

                    reinitViewAndController();
                }
            });
        }
    }

    public void redo() {
        if (historian.canRedo()) {
            game.deleteGameListeners();
            game = historian.redo();
            reinitGameListeners();

            // clear stage
            final Viewport v = stage.getViewport();
            stage.clear();
            Gdx.app.postRunnable(new Runnable() {
                @Override
                public void run() {
                    stage = new Stage(v, new SpriteBatch());

                    reinitViewAndController();
                }
            });
        }
    }


    private void reinitViewAndController() {
//        view = null;
//        controller = null;
//        System.gc();
        Gdx.app.log("view hash before setting new view", String.valueOf(view.hashCode()));
        view = new View(game, stage);
        Gdx.app.log("view hash after setting new view", String.valueOf(view.hashCode()));
        Gdx.app.log("reinit ", game.toString());
        game.registerGameListener(view);
        Gdx.app.log("controller hash before setting new controller", String.valueOf(controller.hashCode()));
        controller = new Controller(game, view);
        Gdx.app.log("controller hash after setting new controller", String.valueOf(controller.hashCode()));
    }

    private void reinitGameListeners() {
        game.registerGameListener(scorer);
        game.registerGameListener(historian);
        scorer.update(game);
        historian.notifyListener();
    }

    @Override
    public void possibleActions(boolean canUndo, boolean canRedo) {
        listener.possibleActionsHistorian(canUndo, canRedo);
    }

    @Override
    public void score(int score) {
        listener.score(score);
    }
}
