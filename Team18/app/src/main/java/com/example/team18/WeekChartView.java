package com.example.team18;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.MotionEvent;

public class WeekChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bar = new RectF();
    private final int[] minutes = new int[7];
    private final String[] labels = new String[7];
    public interface DayClickListener { void onDayClick(String label, int minutes); }
    private DayClickListener dayClickListener;

    public WeekChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setData(int[] values, String[] dayLabels) {
        for (int i = 0; i < 7; i++) {
            minutes[i] = values[i];
            labels[i] = dayLabels[i];
        }
        invalidate();
    }

    public void setDayClickListener(DayClickListener listener) {
        dayClickListener = listener;
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_UP && dayClickListener != null) {
            int index = Math.min(6, Math.max(0, (int) (event.getX() / (getWidth() / 7f))));
            dayClickListener.onDayClick(labels[index], minutes[index]);
            performClick();
            return true;
        }
        return true;
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float bottom = getHeight() - 38f * density;
        float top = 30f * density;
        float barMaxHeight = Math.max(1f, bottom - top);
        int max = 5;
        for (int value : minutes) max = Math.max(max, value);
        float slot = getWidth() / 7f;
        float barWidth = Math.min(24f * density, slot * 0.42f);
        paint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < 7; i++) {
            float center = slot * (i + 0.5f);
            float height = minutes[i] == 0 ? 4f * density : barMaxHeight * minutes[i] / max;
            bar.set(center - barWidth / 2f, bottom - height, center + barWidth / 2f, bottom);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(getResources().getColor(i == 6 ? R.color.tomato : R.color.mode_pill, null));
            canvas.drawRoundRect(bar, 8f * density, 8f * density, paint);
            paint.setColor(getResources().getColor(R.color.ink, null));
            paint.setTextSize(11f * getResources().getDisplayMetrics().scaledDensity);
            canvas.drawText(String.valueOf(minutes[i]), center, Math.max(top - 5f * density, bottom - height - 7f * density), paint);
            paint.setColor(getResources().getColor(R.color.muted_text, null));
            paint.setTextSize(10f * getResources().getDisplayMetrics().scaledDensity);
            canvas.drawText(labels[i] == null ? "" : labels[i], center, getHeight() - 13f * density, paint);
        }
    }
}
