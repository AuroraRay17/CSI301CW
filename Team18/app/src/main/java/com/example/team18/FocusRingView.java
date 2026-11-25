package com.example.team18;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class FocusRingView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private float progress;
    private boolean restMode;

    public FocusRingView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setProgress(float progress, boolean restMode) {
        this.progress = Math.max(0f, Math.min(1f, progress));
        this.restMode = restMode;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float stroke = 6f * density;
        float padding = stroke / 2f + 2f * density;
        bounds.set(padding, padding, getWidth() - padding, getHeight() - padding);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(getResources().getColor(restMode ? R.color.timer_rest_track : R.color.ring_track, null));
        canvas.drawArc(bounds, 0f, 360f, false, paint);
        paint.setColor(getResources().getColor(restMode ? R.color.mint : R.color.tomato, null));
        canvas.drawArc(bounds, -90f, 360f * progress, false, paint);
    }
}
