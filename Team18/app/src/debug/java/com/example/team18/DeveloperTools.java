package com.example.team18;

import android.annotation.SuppressLint;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

final class DeveloperTools implements DefaultLifecycleObserver {
    private static final long HOLD_DURATION_MS = 3000;
    private static final String ACCESS_CODE = "7777777";

    private final AppCompatActivity activity;
    private final View title;
    private final int touchSlop;
    private final Runnable finishHold = this::showCodePrompt;
    private float xOnDown;
    private float yOnDown;
    private AlertDialog activeDialog;

    private DeveloperTools(AppCompatActivity activity, View title) {
        this.activity = activity;
        this.title = title;
        touchSlop = ViewConfiguration.get(activity).getScaledTouchSlop();
    }

    // The title has no tap action. Only an uninterrupted three-second hold opens the prompt.
    @SuppressLint("ClickableViewAccessibility")
    static void install(AppCompatActivity activity, View title) {
        DeveloperTools tools = new DeveloperTools(activity, title);
        title.setOnTouchListener(tools::handleTouch);
        activity.getLifecycle().addObserver(tools);
    }

    private boolean handleTouch(View view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                cancelHold();
                xOnDown = event.getX();
                yOnDown = event.getY();
                title.postDelayed(finishHold, HOLD_DURATION_MS);
                break;
            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                float y = event.getY();
                if (Math.abs(x - xOnDown) > touchSlop || Math.abs(y - yOnDown) > touchSlop
                        || x < 0 || y < 0 || x >= title.getWidth() || y >= title.getHeight()) {
                    cancelHold();
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                cancelHold();
                break;
            default:
                break;
        }
        return true;
    }

    private void cancelHold() {
        title.removeCallbacks(finishHold);
    }

    @Override public void onPause(LifecycleOwner owner) {
        cancelHold();
    }

    @Override public void onDestroy(LifecycleOwner owner) {
        if (activeDialog != null) activeDialog.dismiss();
        title.setOnTouchListener(null);
    }

    private void showCodePrompt() {
        if (activeDialog != null
                || !activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            return;
        }
        LinearLayout content = dialogContent();
        EditText field = addNumberField(content, R.string.dev_code_hint);
        field.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        AlertDialog prompt = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.dev_code_title)
                .setView(content)
                .setNegativeButton(R.string.focus_cancel, null)
                .setPositiveButton(R.string.focus_confirm, null)
                .create();
        showDialog(prompt, field);
        prompt.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            if (!ACCESS_CODE.contentEquals(field.getText())) {
                field.setError(activity.getString(R.string.dev_code_error));
                return;
            }
            prompt.dismiss();
            showCoinPanel();
        });
    }

    private void showCoinPanel() {
        LinearLayout content = dialogContent();
        TextView balance = new TextView(activity);
        balance.setTextColor(activity.getColor(R.color.ui_accent));
        balance.setTextSize(16);
        content.addView(balance);
        try (FocusDatabase database = new FocusDatabase(activity)) {
            updateBalance(balance, database.coinBalance());
        }

        EditText field = addNumberField(content, R.string.dev_coin_amount);
        AlertDialog panel = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.dev_panel_title)
                .setView(content)
                .setNegativeButton(R.string.dev_panel_close, null)
                .setPositiveButton(R.string.dev_grant_coins, null)
                .create();
        showDialog(panel, field);
        panel.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> addCoins(field, balance));
    }

    private void addCoins(EditText field, TextView balance) {
        int amount;
        try {
            amount = Integer.parseInt(field.getText().toString());
        } catch (NumberFormatException invalidAmount) {
            amount = 0;
        }
        if (amount <= 0) {
            field.setError(activity.getString(R.string.dev_coin_error));
            return;
        }
        try (FocusDatabase database = new FocusDatabase(activity)) {
            updateBalance(balance, grantCoins(database, amount));
            field.getText().clear();
        } catch (SQLiteException error) {
            Log.e("DeveloperTools", "Could not grant test coins", error);
            field.setError(activity.getString(R.string.dev_coin_save_error));
        }
    }

    private void updateBalance(TextView label, long amount) {
        label.setText(activity.getString(R.string.pet_shop_balance, amount));
    }

    private LinearLayout dialogContent() {
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        int margin = Math.round(24 * activity.getResources().getDisplayMetrics().density);
        content.setPaddingRelative(margin, margin / 2, margin, 0);
        return content;
    }

    private EditText addNumberField(LinearLayout content, int hint) {
        EditText field = new EditText(activity);
        field.setSingleLine();
        field.setInputType(InputType.TYPE_CLASS_NUMBER);
        field.setHint(hint);
        content.addView(field);
        return field;
    }

    private void showDialog(AlertDialog dialog, EditText field) {
        activeDialog = dialog;
        dialog.setOnDismissListener(window -> {
            if (activeDialog == dialog) activeDialog = null;
        });
        field.requestFocus();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();
    }

    static String coinCreditSql(SQLiteDatabase db) {
        long exists = DatabaseUtils.longForQuery(db,
                "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='debug_coin_grants'", null);
        return exists == 0 ? "" : "(SELECT COALESCE(SUM(amount), 0) FROM debug_coin_grants) + ";
    }

    private static long grantCoins(FocusDatabase database, int amount) {
        SQLiteDatabase db = database.getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS debug_coin_grants (amount INTEGER NOT NULL CHECK(amount > 0))");
            db.execSQL("INSERT INTO debug_coin_grants (amount) VALUES (?)", new Object[]{amount});
            long balance = database.coinBalance();
            db.setTransactionSuccessful();
            return balance;
        } finally {
            db.endTransaction();
        }
    }
}
