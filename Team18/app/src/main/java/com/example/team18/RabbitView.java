package com.example.team18;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

import androidx.appcompat.content.res.AppCompatResources;

public class RabbitView extends View {
    private static final long ACTION_DURATION_MS = 1250L;
    private static final long PREVIEW_GAP_MS = 1500L;
    private static final long STATIC_RESPONSE_MS = 1000L;
    private static final float RESPONSE_POSE_PHASE = .45f;
    private static final long BREATH_DURATION_MS = 4200L;
    private static final long BREATH_FRAME_MS = 33L;
    private static final float RABBIT_WIDTH = 624f;
    private static final float RABBIT_HEIGHT = 280f;
    private static final float RABBIT_ORIGIN_X = 458f;
    private static final float RABBIT_ORIGIN_Y = 1157f;

    private final Drawable room;
    private final Drawable patHeart;
    private final RabbitDrawing drawing = new RabbitDrawing();
    private final long breathingStarted = SystemClock.uptimeMillis();
    private final Runnable breathe = this::invalidate;
    private final Runnable resetStaticPose = () -> {
        stopMotion();
        invalidate();
    };
    private ValueAnimator motion;
    private PetItem interaction;
    private PetItem shopPreview;
    private final Runnable replayPreview = () -> play(shopPreview);
    private boolean blueCushion;
    private boolean sceneVisible;
    private float phase;
    private int controlsLeft;
    private int controlsTop;
    private int controlsRight;
    private float rabbitCenter;
    private float rabbitFloor;
    private float rabbitScale;
    private View headTouch;
    private View greetBubble;

    public RabbitView(Context context, AttributeSet attrs) {
        super(context, attrs);
        room = AppCompatResources.getDrawable(context, R.drawable.sunlit_room);
        patHeart = AppCompatResources.getDrawable(context, R.drawable.ic_pet_pat_heart);
    }

    void play(PetItem action) {
        if (action == null || action.category != PetItem.Category.ACTION) return;
        removeCallbacks(replayPreview);
        stopMotion();
        interaction = action;
        if (!animationsEnabled()) {
            phase = RESPONSE_POSE_PHASE;
            invalidate();
            if (shopPreview != action) postDelayed(resetStaticPose, STATIC_RESPONSE_MS);
            return;
        }
        motion = ValueAnimator.ofFloat(0f, 1f);
        motion.setDuration(ACTION_DURATION_MS);
        motion.setInterpolator(new LinearInterpolator());
        motion.addUpdateListener(animator -> {
            phase = (float) animator.getAnimatedValue();
            invalidate();
        });
        motion.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animator) {
                motion = null;
                phase = 0f;
                interaction = null;
                invalidate();
                if (sceneVisible && shopPreview == action) {
                    postDelayed(replayPreview, PREVIEW_GAP_MS);
                }
            }
        });
        motion.start();
    }

    void showShopPreview(PetItem item) {
        removeCallbacks(replayPreview);
        stopMotion();
        shopPreview = item;
        if (item != null && item.category == PetItem.Category.ACTION) play(item);
        invalidate();
    }

    void setBlueCushion(boolean blueCushion) {
        if (this.blueCushion == blueCushion) return;
        this.blueCushion = blueCushion;
        updateRabbitPlacement();
        invalidate();
    }

    void setHomeInteractionTargets(View headTouch, View greetBubble) {
        this.headTouch = headTouch;
        this.greetBubble = greetBubble;
        headTouch.setPivotX(0f);
        headTouch.setPivotY(0f);
        updateRabbitPlacement();
    }

    void setControlsBounds(int left, int top, int right) {
        controlsLeft = left;
        controlsTop = top;
        controlsRight = right;
        updateRabbitPlacement();
        invalidate();
    }

    private void stopMotion() {
        removeCallbacks(resetStaticPose);
        if (motion != null) {
            motion.removeAllListeners();
            motion.cancel();
            motion = null;
        }
        phase = 0f;
        interaction = null;
    }

    @Override public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
        sceneVisible = isVisible;
        if (isVisible) {
            invalidate();
        } else {
            removeCallbacks(breathe);
            removeCallbacks(replayPreview);
            stopMotion();
        }
    }

    @Override protected void onDetachedFromWindow() {
        removeCallbacks(breathe);
        showShopPreview(null);
        super.onDetachedFromWindow();
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        float scale = Math.max(width / (float) room.getIntrinsicWidth(),
                height / (float) room.getIntrinsicHeight());
        int drawnWidth = Math.round(room.getIntrinsicWidth() * scale);
        int drawnHeight = Math.round(room.getIntrinsicHeight() * scale);
        int left = (width - drawnWidth) / 2;
        float imageCenter = width > height ? .42f : .5f;
        int centeredTop = Math.round(height / 2f - drawnHeight * imageCenter);
        int top = Math.max(height - drawnHeight, Math.min(0, centeredTop));
        room.setBounds(left, top, left + drawnWidth, top + drawnHeight);
        updateRabbitPlacement();
    }

    private void updateRabbitPlacement() {
        if (getWidth() == 0 || getHeight() == 0) return;
        int sceneLeft = 0;
        int sceneRight = getWidth();
        rabbitScale = Math.min(getWidth() * .55f / RABBIT_WIDTH, getHeight() * .24f / RABBIT_HEIGHT);
        rabbitFloor = room.getBounds().top + room.getBounds().height() * .67f;
        if (controlsRight > 0 && controlsRight - controlsLeft < getWidth()) {
            if (controlsLeft == 0) sceneLeft = controlsRight;
            else sceneRight = controlsLeft;
            rabbitFloor = Math.min(rabbitFloor, getHeight() * .85f);
        } else if (controlsTop > 0) {
            rabbitFloor = Math.min(rabbitFloor, controlsTop - 50f * rabbitScale);
        }
        rabbitCenter = (sceneLeft + sceneRight) / 2f;
        positionHomeInteractionTargets(sceneLeft, sceneRight);
    }

    private void positionHomeInteractionTargets(int sceneLeft, int sceneRight) {
        if (headTouch == null) return;
        float density = getResources().getDisplayMetrics().density;
        ViewGroup.LayoutParams baseHeadSize = headTouch.getLayoutParams();
        int headWidth = Math.max(baseHeadSize.width, Math.round(310f * rabbitScale));
        int headHeight = Math.max(baseHeadSize.height, Math.round(250f * rabbitScale));
        // Transform the touch target during the first layout without waiting for a remeasure.
        headTouch.setScaleX(headWidth / (float) baseHeadSize.width);
        headTouch.setScaleY(headHeight / (float) baseHeadSize.height);
        float headY = blueCushion ? 1013f - RabbitDrawing.CUSHION_LIFT : 1013f;
        headTouch.setTranslationX(sceneX(375f) - headWidth / 2f);
        headTouch.setTranslationY(sceneY(headY) - headHeight / 2f);

        int bubbleSize = greetBubble.getLayoutParams().width;
        float inset = 8f * density;
        float bubbleLeft = sceneX(585f) - bubbleSize / 2f;
        greetBubble.setTranslationX(Math.max(sceneLeft + inset,
                Math.min(bubbleLeft, sceneRight - inset - bubbleSize)));
        greetBubble.setTranslationY(Math.max(inset, sceneY(862f) - bubbleSize / 2f));
    }

    private float sceneX(float rabbitX) {
        return rabbitCenter + (rabbitX - RABBIT_ORIGIN_X) * rabbitScale;
    }

    private float sceneY(float rabbitY) {
        return rabbitFloor + (rabbitY - RABBIT_ORIGIN_Y) * rabbitScale;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        room.draw(canvas);
        boolean animate = animationsEnabled();
        float breath = animate ? breathProgress() : 0f;
        canvas.save();
        canvas.translate(rabbitCenter, rabbitFloor);
        canvas.scale(rabbitScale, rabbitScale);
        canvas.translate(-RABBIT_ORIGIN_X, -RABBIT_ORIGIN_Y);
        drawing.draw(canvas, interaction, phase, breath, blueCushion || shopPreview == PetItem.BLUE_CUSHION);
        canvas.restore();
        if (interaction == PetItem.PET && shopPreview == null && headTouch != null && headTouch.isShown()) {
            drawPatHeart(canvas);
        }

        removeCallbacks(breathe);
        if (sceneVisible && animate && motion == null) postDelayed(breathe, BREATH_FRAME_MS);
    }

    private void drawPatHeart(Canvas canvas) {
        float opacity = Math.min(1f, Math.min(phase / .12f, (1f - phase) / .24f));
        int size = patHeart.getIntrinsicWidth();
        int left = Math.round(sceneX(341f) - size / 2f);
        int top = Math.round(sceneY(866f) - size / 2f);
        patHeart.setBounds(left, top, left + size, top + size);
        patHeart.setAlpha(Math.round(opacity * 255));
        patHeart.draw(canvas);
    }

    private float breathProgress() {
        long elapsed = (SystemClock.uptimeMillis() - breathingStarted) % BREATH_DURATION_MS;
        return (float) ((1 - Math.cos(elapsed * 2 * Math.PI / BREATH_DURATION_MS)) * .5);
    }

    private boolean animationsEnabled() {
        if (Build.VERSION.SDK_INT >= 26) return ValueAnimator.areAnimatorsEnabled();
        return Settings.Global.getFloat(getContext().getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
    }
}
