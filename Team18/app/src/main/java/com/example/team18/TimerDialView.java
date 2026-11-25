package com.example.team18;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TimerDialView extends FrameLayout {
    interface OnDurationChanged {
        void onDurationChanged(int seconds);
    }

    private static final TimerDurations.Unit[] UNITS = TimerDurations.Unit.values();
    private final FocusRingView ring;
    private final LinearLayout controls;
    private final TextView readout;
    private final NumberStepperView[] digits = new NumberStepperView[UNITS.length];
    private final TextView[] separators = new TextView[UNITS.length - 1];
    private int diameter = -1;
    private boolean rest;
    private int title;
    private int editableSeconds;
    private OnDurationChanged listener;

    public TimerDialView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ring = new FocusRingView(context, null);
        addView(ring, new LayoutParams(0, 0, Gravity.CENTER));

        controls = new LinearLayout(context);
        controls.setGravity(Gravity.CENTER);
        controls.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        addView(controls, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        for (int i = 0; i < UNITS.length; i++) {
            if (i > 0) {
                TextView separator = new TextView(context);
                NumberStepperView.styleNumber(separator);
                separator.setText(":");
                separator.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                controls.addView(separator, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT));
                separators[i - 1] = separator;
            }
            digits[i] = new NumberStepperView(context, null);
            controls.addView(digits[i], new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        readout = new TextView(context);
        NumberStepperView.styleNumber(readout);
        addView(readout, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));
    }

    void configure(int title, boolean rest, OnDurationChanged listener) {
        this.title = title;
        this.rest = rest;
        this.listener = listener;
        Context context = getContext();
        int accent = context.getColor(rest ? R.color.rest_active : R.color.tomato_dark);
        for (int i = 0; i < UNITS.length; i++) {
            TimerDurations.Unit unit = UNITS[i];
            String label = context.getString(title) + context.getString(unit.label);
            digits[i].configure(label, 0, unit.maximum, true, accent, value -> changeDigit(unit, value));
        }
    }

    void render(long seconds, boolean editable, float progress) {
        ring.setProgress(progress, rest);
        controls.setVisibility(editable ? VISIBLE : GONE);
        readout.setVisibility(editable ? GONE : VISIBLE);
        if (editable) {
            editableSeconds = (int) seconds;
            for (int i = 0; i < UNITS.length; i++) {
                digits[i].setValue(UNITS[i].read(editableSeconds));
            }
        } else {
            String value = TimerDurations.formatClock(seconds);
            if (!value.contentEquals(readout.getText())) {
                readout.setText(value);
                readout.setContentDescription(getContext().getString(title) + " " + value);
            }
        }
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int side = Math.min(width, dp(224));
        if (side != diameter && side > 0) {
            diameter = side;
            ring.getLayoutParams().width = side;
            ring.getLayoutParams().height = side;
            int digitWidth = Math.min(dp(50), Math.round(side * .23f));
            int previewHeight = Math.min(dp(44), Math.round(side * .21f));
            int numberHeight = Math.min(dp(48), Math.round(side * .26f));
            float digitTextSp = Math.min(25f, side / getResources().getDisplayMetrics().density * .15f);
            for (NumberStepperView digit : digits) {
                digit.getLayoutParams().width = digitWidth;
                digit.resizeControls(previewHeight, numberHeight, digitTextSp);
            }
            for (TextView separator : separators) {
                separator.getLayoutParams().width = Math.round(side * .06f);
                separator.setTextSize(digitTextSp);
            }
            readout.setTextSize(Math.min(36f, side / getResources().getDisplayMetrics().density * .17f));
        }
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(side, MeasureSpec.EXACTLY));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void changeDigit(TimerDurations.Unit unit, int value) {
        if (listener == null) return;
        editableSeconds = unit.step(editableSeconds, value - unit.read(editableSeconds));
        listener.onDurationChanged(editableSeconds);
    }
}
