package com.myTest.Test.android.android;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class GamePanel extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder holder;
    private ArrayList<rndSqr> squares = new ArrayList<>();
    private Random rnd = new Random();
    private boolean running = true;
    private Handler handler = new Handler();
    private int score = 0;
    private int lives = 3;

    private int squaresPerWave = 3;
    private int waveSpeed = 20;
    private int waveCount = 0;
    private double pestProbability = 0.15;

    private long gameStartTime;
    private long gameDuration = 2 * 60 * 1000; // 2 minutes
    private Bitmap background;

    private int[] imageResources = {
        R.drawable.banana, R.drawable.apple, R.drawable.cherry,
        R.drawable.mango, R.drawable.plum, R.drawable.strawberry,
        R.drawable.raspberry, R.drawable.orange
    };

    private int[] pestImageResources = {
        R.drawable.pest1, R.drawable.pest2, R.drawable.pest3
    };

    private int[] flowerImageResource = {
        R.drawable.flower1, R.drawable.flower2, R.drawable.flower3
    };

    public GamePanel(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
        background = BitmapFactory.decodeResource(getResources(), R.drawable.background);
    }

    private void startWaves() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!running) return;

                long elapsedTime = System.currentTimeMillis() - gameStartTime;
                if (elapsedTime >= gameDuration || lives <= 0) {
                    running = false;
                    handler.removeCallbacksAndMessages(null);
                    showGameOverScreen();
                    return;
                }

                if (squares.isEmpty()) { // Ensure new wave spawns
                    spawnWave(squaresPerWave, waveSpeed);
                    waveSpeed += 2.5;
                    waveCount++;

                    if (waveCount % 3 == 0) {
                        squaresPerWave = Math.min(squaresPerWave + 2, 8);
                    }

                    pestProbability = Math.min(pestProbability + 0.002, 0.5);
                }

                updateSquares();
                render();
                handler.postDelayed(this, 50);
            }
        }, 50);
    }

    private void spawnWave(int numSquares, int speed) {
        for (int i = 0; i < numSquares; i++) {
            int minX = 50; // Minimum x position (to avoid spawning out of bounds)
            int maxX = getWidth() - 150; // Maximum x position (considering object width)
            int x = rnd.nextInt(Math.max(1, maxX - minX)) + minX;

            PointF pos = new PointF(x, 0);
            int size = 150;
            boolean isPenalty = (rnd.nextDouble() < pestProbability);
            Bitmap image;

            if (isPenalty) {
                int pestIndex = rnd.nextInt(pestImageResources.length);
                image = BitmapFactory.decodeResource(getResources(), pestImageResources[pestIndex]);
                squares.add(new rndSqr(pos, size, image, 0, (int) (speed * 1.5), true));
            } else {
                if (rnd.nextDouble() < 0.1) {
                    int flowerIndex = rnd.nextInt(flowerImageResource.length);
                    image = BitmapFactory.decodeResource(getResources(), flowerImageResource[flowerIndex]);
                    rndSqr obj = new rndSqr(pos, size, image, speed);
                    obj.setPoints(5);
                    squares.add(obj);
                } else {
                    int imageResId = imageResources[rnd.nextInt(imageResources.length)];
                    image = BitmapFactory.decodeResource(getResources(), imageResId);
                    rndSqr obj = new rndSqr(pos, size, image, speed);
                    obj.setPoints(1);
                    squares.add(obj);
                }
            }
        }
    }


    private void updateSquares() {
        Iterator<rndSqr> iterator = squares.iterator();
        while (iterator.hasNext()) {
            rndSqr square = iterator.next();
            square.update(getWidth(), getHeight());
            if (square.pos.y > getHeight()) {
                if (!square.isPenalty()) {
                    lives--;
                }
                iterator.remove();
            }
        }
    }

    private void render() {
        Canvas c = holder.lockCanvas();
        if (c != null) {
            c.drawBitmap(background, 0, 0, null);
            for (rndSqr square : squares) {
                square.draw(c);
            }

            Paint paint = new Paint();
            paint.setColor(Color.GREEN);
            paint.setTextSize(70);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            c.drawText("Score: " + score, 30, 80, paint);
            c.drawText("Lives: " + lives, 30, 140, paint);

            long elapsedTime = System.currentTimeMillis() - gameStartTime;
            long timeRemaining = Math.max(0, gameDuration - elapsedTime);
            String timeText = String.format("%02d:%02d", (timeRemaining / 60000) % 60, (timeRemaining / 1000) % 60);

            Paint timerPaint = new Paint();
            timerPaint.setColor(Color.YELLOW);
            timerPaint.setTextSize(100);
            timerPaint.setTextAlign(Paint.Align.CENTER);
            c.drawText(timeText, getWidth() / 2, 80, timerPaint);

            holder.unlockCanvasAndPost(c);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            PointF touchPos = new PointF(event.getX(), event.getY());
            Iterator<rndSqr> iterator = squares.iterator();
            while (iterator.hasNext()) {
                rndSqr square = iterator.next();
                if (square.contains(touchPos)) {
                    if (square.isPenalty()) {
                        lives--;
                    } else {
                        score += square.getPoints();
                    }
                    iterator.remove();
                }
            }
        }
        return true;
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        running = true;
        gameStartTime = System.currentTimeMillis();

        background = Bitmap.createScaledBitmap(
            BitmapFactory.decodeResource(getResources(), R.drawable.gamebackground),
            getWidth(), getHeight(), true
        );

        startWaves();
        render();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        running = false;
        handler.removeCallbacksAndMessages(null);
    }

    private void showGameOverScreen() {
        post(new Runnable() {
            @OptIn(markerClass = UnstableApi.class)
            @Override
            public void run() {
                Context context = getContext();
                if (context == null) {
                    Log.e("GameOver", "Context is null. Cannot proceed.");
                    return;
                }

                // Pause BGM and play game over sound
                SoundManager.getInstance(context).pauseBgm();
                SoundManager.getInstance(context).playGameOverSfx();

                // Update high score
                HighScoreManager highScoreManager = HighScoreManager.getInstance(context);
                int currentHighScore = highScoreManager.getHighScore();
                if (score > currentHighScore) {
                    highScoreManager.updateHighScore(score);
                    currentHighScore = score;
                }

                if (context instanceof GameStart) {
                    GameStart activity = (GameStart) context;
                    FrameLayout gameContainer = activity.findViewById(R.id.gameContainer);
                    if (gameContainer == null) {
                        Log.e("GameOver", "gameContainer not found.");
                        return;
                    }
                    gameContainer.removeAllViews();

                    LayoutInflater inflater = LayoutInflater.from(context);
                    View gameOverView = inflater.inflate(R.layout.game_over, null);

                    TextView scoreText = gameOverView.findViewById(R.id.finalScoreTextView);
                    TextView highScoreText = gameOverView.findViewById(R.id.highScoreTextView);

                    if (scoreText != null) scoreText.setText("Puntos: " + score);
                    if (highScoreText != null) highScoreText.setText("Rurok ng Puntos: " + currentHighScore);

                    ImageButton playAgainButton = gameOverView.findViewById(R.id.playAgainButton);
                    ImageButton homeButton = gameOverView.findViewById(R.id.homeButton);

                    if (playAgainButton != null) {
                        playAgainButton.setOnClickListener(v -> {
                            SoundManager.getInstance(context).playPopSound();
                            v.startAnimation(AnimationUtils.loadAnimation(context, R.anim.button_pop));
                            new Handler().postDelayed(activity::startGame, 100);
                        });
                    }

                    if (homeButton != null) {
                        homeButton.setOnClickListener(v -> {
                            SoundManager.getInstance(context).playPopSound();
                            v.startAnimation(AnimationUtils.loadAnimation(context, R.anim.button_pop));
                            new Handler().postDelayed(() -> {
                                Intent intent = new Intent(activity, MainUI.class);
                                activity.startActivity(intent);
                                activity.finish();
                            }, 100);
                        });
                    }

                    gameContainer.addView(gameOverView);
                }
            }
        });
    }

}
