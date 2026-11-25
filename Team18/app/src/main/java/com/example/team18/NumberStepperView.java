package com.example.team18;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.graphics.ColorUtils;

import java.util.function.IntConsumer;

public class NumberStepperView extends LinearLayout {
    private final SwipeNumber previous;
    private final SwipeNumber number;
    private final SwipeNumber next;
    private final int touchSlop;
    private int minimum;
    private int maximum;
    private int value;
    private boolean pad;
    private String label;
    private IntConsumer onChange;
    private float dragAnchor;
    private float touchStartX;
    private float touchStartY;
    private boolean dragging;

    public NumberStepperView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        previous = addPreview(-1);
        number = new SwipeNumber(context);
        styleNumber(number);
        number.setTextSize(24);
        addView(number, new LayoutParams(LayoutParams.MATCH_PARENT, dp(28)));
        number.setOnTouchListener(this::drag);
        next = addPreview(1);
    }

    void configure(String label, int minimum, int maximum, boolean pad, int accent, IntConsumer onChange) {
        this.label = label;
        this.minimum = minimum;
        this.maximum = maximum;
        this.pad = pad;
        this.onChange = onChange;
        int previewColor = ColorUtils.blendARGB(getContext().getColor(R.color.muted_text), accent, .14f);
        previous.setTextColor(previewColor);
        next.setTextColor(previewColor);
        setValue(minimum);
    }

    void setValue(int value) {
        this.value = Math.max(minimum, Math.min(maximum, value));
        display(previous, TimerDurations.wrap(this.value - 1, minimum, maximum), R.string.stepper_previous);
        display(number, this.value, R.string.stepper_value);
        display(next, TimerDurations.wrap(this.value + 1, minimum, maximum), R.string.stepper_next);
    }

    void resizeControls(int previewHeight, int numberHeight, float textSizeSp) {
        previous.getLayoutParams().height = previewHeight;
        next.getLayoutParams().height = previewHeight;
        number.getLayoutParams().height = numberHeight;
        previous.setTextSize(textSizeSp * .76f);
        next.setTextSize(textSizeSp * .76f);
        number.setTextSize(textSizeSp);
    }

    static void styleNumber(TextView view) {
        view.setGravity(Gravity.CENTER);
        view.setIncludeFontPadding(false);
        view.setSingleLine();
        view.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        view.setTextColor(view.getContext().getColor(R.color.ink));
    }

    private SwipeNumber addPreview(int amount) {
        SwipeNumber preview = new SwipeNumber(getContext());
        styleNumber(preview);
        preview.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
        preview.setTextSize(18);
        TypedValue background = new TypedValue();
        if (getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless,
                background, true)) preview.setBackgroundResource(background.resourceId);
        preview.setOnClickListener(v -> step(amount));
        preview.setOnTouchListener(this::drag);
        addView(preview, new LayoutParams(LayoutParams.MATCH_PARENT, dp(26)));
        return preview;
    }

    private void display(SwipeNumber view, int value, int description) {
        String text = pad && value < 10 ? "0" + value : String.valueOf(value);
        if (!text.contentEquals(view.getText())) view.setText(text);
        view.setContentDescription(getContext().getString(description, label, text));
    }

    private void step(int amount) {
        int changed = TimerDurations.wrap(value + amount, minimum, maximum);
        if (changed == value || onChange == null) return;
        setValue(changed);
        onChange.accept(changed);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private boolean drag(View view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = event.getRawX();
                touchStartY = event.getRawY();
                dragAnchor = touchStartY;
                dragging = false;
                view.setPressed(view.isClickable());
                requestDisallowInterceptTouchEvent(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(event.getRawX() - touchStartX) >= touchSlop
                        || Math.abs(event.getRawY() - touchStartY) >= touchSlop) {
                    dragging = true;
                    view.setPressed(false);
                }
                int stepPx = dp(16);
                int moved = (int) ((dragAnchor - event.getRawY()) / stepPx);
                if (moved != 0) {
                    dragAnchor -= moved * stepPx;
                    step(moved);
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (!dragging) view.performClick();
                // Restore scrolling outside the picker after releasing it.
            case MotionEvent.ACTION_CANCEL:
                view.setPressed(false);
                requestDisallowInterceptTouchEvent(false);
                return true;
            default:
                return true;
        }
    }

    private static final class SwipeNumber extends AppCompatTextView {
        SwipeNumber(Context context) {
            super(context);
        }

        @Override public boolean performClick() {
            return super.performClick();
        }
    }
}
