package com.example.team18;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String FINISH_CHANNEL = "focus_finish";
    private static final String SESSION_TASK_ID_KEY = "session_task_id";
    static final String ROUND_ID_KEY = "round_id";
    private FocusDatabase database;
    private SharedPreferences preferences;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView selectedTaskView, contextHint, cycleProgress;
    private Button startPauseButton, endButton;
    private View countdownDials, cyclePanel, cycleSetup;
    private NumberStepperView cycleCountPicker;
    private TimerDialView focusDial, restDial, elapsedDial;
    private View petScreen, focusScreen;
    private RabbitView rabbitView;
    private PetShopPanel shopPanel;
    private RoomDecorations roomDecorations;
    private boolean canPatPet;
    private AlertDialog endConfirmation;
    private AlertDialog focusResultDialog;
    private OnBackPressedCallback backToPet;
    private long selectedTaskId = -1;
    private String selectedTaskTitle = "";
    private String sessionTaskTitle = "";
    private long sessionTaskId = -1;
    private String roundId;
    private String planId;
    private String pendingPlanResultId;
    private boolean breakMode;
    private boolean running;
    private long remainingMs;
    private long endAt;
    private FocusTime.Mode focusMode;
    private long countUpAccumulatedMs;
    private long segmentStartedElapsedMs;
    private long segmentStartedWallMs;
    private int segmentBootCount;
    private int bootCount;
    private int focusSeconds;
    private int breakSeconds;
    private int cycleCount;
    private int cycleRound;
    private int themeType;
    private MediaPlayer soundPlayer;
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private final SensorEventListener lightListener = new SensorEventListener() {
        @Override public void onSensorChanged(SensorEvent event) {
            if (event.values[0] < 35f) contextHint.setText("☀ 光线有点暗，记得照顾眼睛");
            else if (contextHint.getText().toString().contains("光线")) contextHint.setText("");
        }
        @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    };
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100);
            if (level <= 15) contextHint.setText("♡ 电量较低，可以关掉环境音省电");
        }
    };

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (!running) return;
            if (roundHasExpired()) {
                completeRound();
                return;
            }
            showTimer();
            handler.postDelayed(this, 500);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_shell);
        getWindow().setStatusBarColor(getColor(R.color.page_background));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.appShell), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        database = new FocusDatabase(this);
        preferences = getSharedPreferences("focus_settings", MODE_PRIVATE);
        selectedTaskView = findViewById(R.id.selectedTask);
        contextHint = findViewById(R.id.contextHint);
        countdownDials = findViewById(R.id.countdownDials);
        cyclePanel = findViewById(R.id.cyclePanel);
        cycleSetup = findViewById(R.id.cycleSetup);
        cycleCountPicker = findViewById(R.id.cycleCountPicker);
        cycleProgress = findViewById(R.id.cycleProgress);
        focusDial = findViewById(R.id.focusDial);
        restDial = findViewById(R.id.restDial);
        elapsedDial = findViewById(R.id.elapsedDial);
        focusDial.configure(R.string.timer_focus_duration, false, seconds -> setDuration(false, seconds));
        restDial.configure(R.string.timer_rest_duration, true, seconds -> setDuration(true, seconds));
        elapsedDial.configure(R.string.focus_count_up, false, null);
        startPauseButton = findViewById(R.id.startPauseButton);
        endButton = findViewById(R.id.endButton);
        RadioGroup focusModeOptions = findViewById(R.id.focusModeOptions);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(new NotificationChannel(FINISH_CHANNEL, "番茄完成提醒",
                    NotificationManager.IMPORTANCE_DEFAULT));
        }

        focusSeconds = TimerDurations.read(preferences, false);
        breakSeconds = TimerDurations.read(preferences, true);
        cycleCount = FocusCycle.clampRoundCount(preferences.getInt("focus_cycle_count", 1));
        cycleRound = Math.min(cycleCount,
                FocusCycle.clampRoundCount(preferences.getInt("focus_cycle_round", 1)));
        breakMode = preferences.getBoolean("break_mode", false);
        running = preferences.getBoolean("running", false);
        focusMode = FocusTime.Mode.fromStoredValue(preferences.getString("focus_timer_mode", null));
        countUpAccumulatedMs = preferences.getLong("count_up_accumulated_ms", 0);
        segmentStartedElapsedMs = preferences.getLong("segment_started_elapsed_ms", 0);
        segmentStartedWallMs = preferences.getLong("segment_started_wall_ms", 0);
        segmentBootCount = preferences.getInt("segment_boot_count", -1);
        bootCount = Settings.Global.getInt(getContentResolver(), Settings.Global.BOOT_COUNT, -1);
        endAt = preferences.getLong("end_at", 0);
        remainingMs = preferences.getLong("remaining_ms", durationMs());
        sessionTaskTitle = preferences.getString("session_task_title", "");
        sessionTaskId = preferences.getLong(SESSION_TASK_ID_KEY,
                preferences.getLong("selected_task_id", -1));
        roundId = preferences.getString(ROUND_ID_KEY, null);
        planId = preferences.getString("focus_plan_id", null);
        pendingPlanResultId = preferences.getString("pending_focus_plan_result", null);
        boolean legacyPause = !preferences.contains(SESSION_TASK_ID_KEY) && remainingMs < durationMs();
        if (roundId == null && (running || legacyPause)) roundId = UUID.randomUUID().toString();
        if (!roundIsActive()) clearCycle();

        startPauseButton.setOnClickListener(v -> startOrPause());
        endButton.setOnClickListener(v -> confirmEnd());
        cycleCountPicker.configure(getString(R.string.focus_round_count), FocusCycle.MIN_ROUNDS,
                FocusCycle.MAX_ROUNDS, false, getColor(R.color.ui_accent), value -> {
                    if (roundIsActive()) return;
                    cycleCount = value;
                    saveState();
                });
        focusModeOptions.check(focusMode == FocusTime.Mode.COUNT_UP ? R.id.countUpOption : R.id.countdownOption);
        focusModeOptions.setOnCheckedChangeListener((group, checkedId) -> selectFocusMode(checkedId));
        findViewById(R.id.tasksEntry).setOnClickListener(
                v -> startActivity(new Intent(this, TaskActivity.class)));
        findViewById(R.id.reviewEntry).setOnClickListener(
                v -> startActivity(new Intent(this, ReviewActivity.class)));
        findViewById(R.id.settingsButton).setOnClickListener(
                v -> startActivity(new Intent(this, SettingsActivity.class)));
        setupPetHome();
        recoverTimer();
        if (roundIsActive() && usesPlanRewards() && planId == null) {
            // A plan already running at upgrade keeps the rewards previously paid per round.
            planId = UUID.randomUUID().toString();
            saveCycleBoundary();
        }
        boolean showFocus = roundIsActive() || pendingPlanResultId != null
                || (savedInstanceState != null && savedInstanceState.getBoolean("focus_screen"));
        showScreen(showFocus);
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("focus_screen", focusScreen.getVisibility() == View.VISIBLE);
        super.onSaveInstanceState(outState);
    }

    @Override protected void onResume() {
        super.onResume();
        boolean active = roundIsActive();
        refreshSelectedTask();
        focusSeconds = TimerDurations.read(preferences, false);
        breakSeconds = TimerDurations.read(preferences, true);
        if (active) showScreen(true);
        else remainingMs = durationMs();
        themeType = preferences.getInt("theme_type", 0);
        refreshPetHome();
        shopPanel.setPreviewActive(true);
        refreshClock();
        showPendingPlanResult();
        contextHint.setText("");
        if (preferences.getBoolean("light_hint", false)) {
            if (lightSensor == null) contextHint.setText("这台设备没有光线传感器");
            else sensorManager.registerListener(lightListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (preferences.getBoolean("battery_hint", false)) {
            registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        }
    }

    @Override protected void onPause() {
        shopPanel.setPreviewActive(false);
        handler.removeCallbacks(ticker);
        sensorManager.unregisterListener(lightListener);
        if (preferences.getBoolean("battery_hint", false)) unregisterReceiver(batteryReceiver);
        releaseSound();
        saveState();
        super.onPause();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(ticker);
        if (endConfirmation != null) endConfirmation.dismiss();
        if (focusResultDialog != null) focusResultDialog.dismiss();
        shopPanel.release();
        database.close();
        super.onDestroy();
    }

    private long durationMs() {
        return (breakMode ? breakSeconds : focusSeconds) * 1000L;
    }

    private void setDuration(boolean rest, int seconds) {
        if (roundIsActive()) return;
        if (rest) breakSeconds = seconds;
        else focusSeconds = seconds;
        remainingMs = durationMs();
        SharedPreferences.Editor editor = timerState();
        TimerDurations.write(editor, rest, seconds);
        editor.apply();
        showTimer();
        refreshPetTimerDetails();
    }

    private boolean roundIsActive() {
        return running || roundId != null;
    }

    private boolean roundHasExpired() {
        return running && !isCountUp() && countdownRemainingMs() == 0;
    }

    private boolean isCountUp() {
        return !breakMode && focusMode == FocusTime.Mode.COUNT_UP;
    }

    private boolean usesPlanRewards() {
        return focusMode == FocusTime.Mode.COUNTDOWN && cycleCount > 1;
    }

    private long countUpElapsedMs() {
        return countUpAccumulatedMs + (running ? activeSegmentMs() : 0);
    }

    private long activeSegmentMs() {
        long nowElapsed = SystemClock.elapsedRealtime();
        if (bootCount == segmentBootCount && nowElapsed >= segmentStartedElapsedMs) {
            return nowElapsed - segmentStartedElapsedMs;
        }
        return Math.max(0, System.currentTimeMillis() - segmentStartedWallMs);
    }

    private void selectFocusMode(int checkedId) {
        if (roundIsActive() || breakMode) return;
        focusMode = checkedId == R.id.countUpOption ? FocusTime.Mode.COUNT_UP : FocusTime.Mode.COUNTDOWN;
        saveState();
        showTimer();
        refreshPetHome();
    }

    private void setupPetHome() {
        petScreen = findViewById(R.id.petScreen);
        focusScreen = findViewById(R.id.focusScreen);
        rabbitView = findViewById(R.id.rabbitView);
        shopPanel = findViewById(R.id.petShopPanel);
        roomDecorations = new RoomDecorations(preferences);
        shopPanel.bind(database, roomDecorations, rabbitView, findViewById(R.id.petHomeChrome),
                this::refreshPetHome, () -> setShopOpen(false));
        View homeControls = findViewById(R.id.petHomeControls);
        Configuration screen = getResources().getConfiguration();
        if (screen.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            int widthDp = Math.min(340, screen.screenWidthDp / 2);
            homeControls.getLayoutParams().width =
                    Math.round(widthDp * getResources().getDisplayMetrics().density);
        }
        homeControls.addOnLayoutChangeListener(
                (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                        rabbitView.setControlsBounds(left, top, right));
        findViewById(R.id.petNameButton).setOnClickListener(v -> renamePet());
        findViewById(R.id.petGrowthButton).setOnClickListener(v -> showGrowth());
        findViewById(R.id.petShopButton).setOnClickListener(
                v -> setShopOpen(true));
        findViewById(R.id.petTimerButton).setOnClickListener(v -> showScreen(true));
        findViewById(R.id.focusBackButton).setOnClickListener(v -> showScreen(false));
        View petHead = findViewById(R.id.petHeadTouch);
        View petGreet = findViewById(R.id.petGreetBubble);
        petHead.setOnClickListener(v -> {
            if (canPatPet) play(PetItem.PET);
            else Toast.makeText(this, R.string.pet_pat_locked, Toast.LENGTH_SHORT).show();
        });
        petGreet.setOnClickListener(v -> play(PetItem.GREET));
        rabbitView.setHomeInteractionTargets(petHead, petGreet);
        findViewById(R.id.petTasksButton).setOnClickListener(
                v -> startActivity(new Intent(this, TaskActivity.class)));
        findViewById(R.id.petReviewButton).setOnClickListener(
                v -> startActivity(new Intent(this, ReviewActivity.class)));
        findViewById(R.id.petSettingsButton).setOnClickListener(
                v -> startActivity(new Intent(this, SettingsActivity.class)));
        backToPet = new OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() {
                if (shopPanel.isOpen()) setShopOpen(false);
                else showScreen(false);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backToPet);
    }

    private void showScreen(boolean showFocus) {
        if (showFocus) shopPanel.setOpen(false);
        focusScreen.setVisibility(showFocus ? View.VISIBLE : View.GONE);
        petScreen.setVisibility(showFocus ? View.GONE : View.VISIBLE);
        backToPet.setEnabled(showFocus || shopPanel.isOpen());
    }

    private void setShopOpen(boolean open) {
        shopPanel.setOpen(open);
        backToPet.setEnabled(open || focusScreen.getVisibility() == View.VISIBLE);
    }

    private String petName() {
        return preferences.getString("pet_name", getString(R.string.pet_default_name));
    }

    private void refreshPetHome() {
        Set<PetItem> owned = database.ownedPetItems();
        findViewById(R.id.petGreetBubble).setVisibility(
                owned.contains(PetItem.GREET) ? View.VISIBLE : View.GONE);
        canPatPet = owned.contains(PetItem.PET);
        findViewById(R.id.petHeadTouch).setContentDescription(
                getString(canPatPet ? R.string.pet_pat : R.string.pet_pat_locked));
        rabbitView.setBlueCushion(
                roomDecorations.equipped(PetItem.RoomSlot.CUSHION, owned) == PetItem.BLUE_CUSHION);
        ((Button) findViewById(R.id.petNameButton)).setText(getString(R.string.pet_name_button, petName()));
        refreshPetTimerDetails();
    }

    private void refreshPetTimerDetails() {
        String task = focusTaskLabel();
        String detail = focusMode == FocusTime.Mode.COUNT_UP ? getString(R.string.focus_count_up)
                : FocusTime.format(this, focusSeconds);
        ((TextView) findViewById(R.id.petTimerDetails)).setText(
                getString(R.string.pet_timer_details, task, detail));
    }

    private String focusTaskLabel() {
        if (!selectedTaskTitle.isEmpty()) return selectedTaskTitle;
        return getString(focusMode == FocusTime.Mode.COUNT_UP ? R.string.focus_free_task : R.string.pet_no_task);
    }

    private void play(PetItem interaction) {
        rabbitView.play(interaction);
        int response = interaction == PetItem.GREET
                ? R.string.pet_greet_response : R.string.pet_pat_response;
        rabbitView.announceForAccessibility(getString(response, petName()));
    }

    private void renamePet() {
        FrameLayout container = new FrameLayout(this);
        int margin = Math.round(24 * getResources().getDisplayMetrics().density);
        container.setPadding(margin, 0, margin, 0);
        EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(16)});
        name.setText(petName());
        name.selectAll();
        container.addView(name);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pet_rename)
                .setView(container)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();
        dialog.setOnShowListener(window -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nickname = name.getText().toString().trim();
            if (nickname.isEmpty()) {
                name.setError(getString(R.string.pet_empty_name));
                return;
            }
            preferences.edit().putString("pet_name", nickname).apply();
            ((Button) findViewById(R.id.petNameButton)).setText(getString(R.string.pet_name_button, nickname));
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void showGrowth() {
        long total = database.totalFocusedSeconds();
        View growth = getLayoutInflater().inflate(R.layout.dialog_pet_growth, null);
        ((TextView) growth.findViewById(R.id.petGrowthTotal)).setText(
                getString(R.string.pet_growth_total, FocusTime.format(this, total)));
        ArrayList<String> labels = new ArrayList<>();
        for (PetItem item : database.ownedPetItems()) labels.add(getString(item.label));
        ((TextView) growth.findViewById(R.id.petGrowthInteractions)).setText(labels.isEmpty()
                ? getString(R.string.pet_no_items) : TextUtils.join("、", labels));
        new MaterialAlertDialogBuilder(this).setTitle(R.string.pet_growth).setView(growth)
                .setPositiveButton("关闭", null).show();
    }

    private void refreshSelectedTask() {
        selectedTaskId = preferences.getLong("selected_task_id", -1);
        FocusDatabase.Task task = selectedTaskId == -1 ? null : database.task(selectedTaskId);
        if (task == null || task.done) {
            selectedTaskId = -1;
            selectedTaskTitle = "";
            preferences.edit().putLong("selected_task_id", -1)
                    .putString("selected_task_title", "").apply();
        } else {
            selectedTaskTitle = task.title;
            preferences.edit().putString("selected_task_title", task.title).apply();
        }
    }

    private SharedPreferences.Editor timerState() {
        return preferences.edit()
                .putBoolean("break_mode", breakMode)
                .putBoolean("running", running)
                .putString("focus_timer_mode", focusMode.name())
                .putInt("focus_cycle_count", cycleCount)
                .putInt("focus_cycle_round", cycleRound)
                .putLong("count_up_accumulated_ms", countUpAccumulatedMs)
                .putLong("segment_started_elapsed_ms", segmentStartedElapsedMs)
                .putLong("segment_started_wall_ms", segmentStartedWallMs)
                .putInt("segment_boot_count", segmentBootCount)
                .putLong("end_at", endAt)
                .putLong("remaining_ms", remainingMs)
                .putString("session_task_title", sessionTaskTitle)
                .putLong(SESSION_TASK_ID_KEY, sessionTaskId)
                .putString(ROUND_ID_KEY, roundId)
                .putString("focus_plan_id", planId)
                .putString("pending_focus_plan_result", pendingPlanResultId);
    }

    private void saveState() {
        timerState().apply();
    }

    private void saveCycleBoundary() {
        if (!timerState().commit()) Log.w("MainActivity", "Could not persist timer phase");
    }

    private void refreshClock() {
        updatePrimaryColor();
        showTimer();
        handler.removeCallbacks(ticker);
        if (running) handler.post(ticker);
        updateSound();
    }

    private void showTimer() {
        boolean countUp = isCountUp();
        boolean active = roundIsActive();
        long displayMs = countUp ? countUpElapsedMs() : countdownRemainingMs();
        long seconds = countUp ? displayMs / 1000 : (displayMs + 999) / 1000;
        countdownDials.setVisibility(countUp ? View.GONE : View.VISIBLE);
        elapsedDial.setVisibility(countUp ? View.VISIBLE : View.GONE);
        if (countUp) {
            elapsedDial.render(seconds, false, 0f);
        } else {
            float progress = active ? 1f - (float) displayMs / Math.max(1L, durationMs()) : 0f;
            focusDial.render(breakMode ? focusSeconds : seconds, !active, breakMode ? 0f : progress);
            restDial.render(breakMode ? seconds : breakSeconds, !active, breakMode ? progress : 0f);
        }
        cyclePanel.setVisibility(countUp ? View.GONE : View.VISIBLE);
        if (!countUp) {
            cycleSetup.setVisibility(active ? View.GONE : View.VISIBLE);
            cycleProgress.setVisibility(active ? View.VISIBLE : View.GONE);
            if (active) {
                String phase = getString(R.string.focus_round_progress, cycleRound, cycleCount,
                        getString(breakMode ? R.string.timer_rest_duration : R.string.timer_focus_duration));
                if (!phase.contentEquals(cycleProgress.getText())) cycleProgress.setText(phase);
            } else {
                cycleCountPicker.setValue(cycleCount);
            }
        }
        selectedTaskView.setText(active && !sessionTaskTitle.isEmpty() ? sessionTaskTitle : focusTaskLabel());
        startPauseButton.setText(running ? "暂停计时" : active
                ? "继续计时" : "开始专注");
        startPauseButton.setEnabled(active || countUp || durationMs() > 0);
        endButton.setVisibility(active ? View.VISIBLE : View.GONE);
        boolean expired = roundHasExpired();
        endButton.setEnabled(active && !expired);
        if (expired) {
            startPauseButton.setText(R.string.focus_retry_save);
        }
        findViewById(R.id.countdownOption).setEnabled(!active);
        findViewById(R.id.countUpOption).setEnabled(!active);
    }

    private void updatePrimaryColor() {
        int primary = roundIsActive() || themeType == 1 ? R.color.ui_accent : R.color.tomato_dark;
        startPauseButton.setBackgroundTintList(new ColorStateList(
                new int[][]{{-android.R.attr.state_enabled}, {}},
                new int[]{getColor(R.color.timer_disabled), getColor(primary)}));
    }

    private long countdownRemainingMs() {
        if (!running) return remainingMs;
        // A countdown started before the upgrade only has its original wall-clock deadline.
        if (segmentStartedWallMs == 0) return Math.max(0, endAt - System.currentTimeMillis());
        return Math.max(0, remainingMs - activeSegmentMs());
    }

    private long countdownFinishedAt() {
        if (segmentStartedWallMs == 0) return endAt;
        return System.currentTimeMillis() - Math.max(0, activeSegmentMs() - remainingMs);
    }

    private void startOrPause() {
        boolean requestNotificationPermission = false;
        if (running) {
            if (roundHasExpired()) {
                completeRound();
                return;
            }
            if (isCountUp()) {
                countUpAccumulatedMs = countUpElapsedMs();
            } else {
                long remaining = countdownRemainingMs();
                if (remaining == 0) {
                    completeRound();
                    return;
                }
                remainingMs = remaining;
            }
            running = false;
        } else {
            if (!isCountUp() && durationMs() == 0) return;
            if (!roundIsActive() && !isCountUp() && selectedTaskTitle.isEmpty()) {
                Toast.makeText(this, "请先到任务清单选择任务", Toast.LENGTH_SHORT).show();
                return;
            }
            if (roundId == null) {
                roundId = UUID.randomUUID().toString();
                if (usesPlanRewards()) planId = UUID.randomUUID().toString();
                sessionTaskTitle = focusTaskLabel();
                sessionTaskId = selectedTaskId;
                requestNotificationPermission = !isCountUp() && preferences.getBoolean("notifications", true);
            }
            startSegment();
        }
        saveState();
        refreshClock();
        if (requestNotificationPermission && Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
        }
    }

    private void startSegment() {
        segmentStartedElapsedMs = SystemClock.elapsedRealtime();
        segmentStartedWallMs = System.currentTimeMillis();
        segmentBootCount = bootCount;
        running = true;
    }

    private void recoverTimer() {
        if (planId != null && database.savedFocusPlan(planId) != null) {
            closeSettledPlan();
            return;
        }
        if (breakMode || roundId == null) return;
        Boolean completed = database.savedRoundCompleted(roundId);
        if (completed == null) return;
        if (completed && !isCountUp()) {
            advanceAfterPhase();
        } else {
            clearCycle();
            saveCycleBoundary();
        }
    }

    private boolean advanceAfterPhase() {
        FocusCycle.NextPhase next = FocusCycle.afterPhase(breakMode, breakSeconds, cycleRound, cycleCount);
        if (next == FocusCycle.NextPhase.FINISHED) {
            if (planId != null) return settlePlan(true);
            clearCycle();
        } else {
            breakMode = next == FocusCycle.NextPhase.REST;
            if (!breakMode) cycleRound++;
            remainingMs = durationMs();
            roundId = UUID.randomUUID().toString();
            startSegment();
        }
        saveCycleBoundary();
        return true;
    }

    private void completeRound() {
        if (!roundHasExpired()) return;
        boolean wasBreak = breakMode;
        handler.removeCallbacks(ticker);
        if (endConfirmation != null) endConfirmation.dismiss();
        String taskTitle = sessionTaskTitle;
        int seconds = focusSeconds;
        boolean grouped = planId != null;
        FocusDatabase.RoundResult result = null;
        if (!wasBreak) {
            result = recordFocusRound(seconds, countdownFinishedAt(), true);
            if (result == null) {
                releaseSound();
                showTimer();
                return;
            }
        }
        if (!advanceAfterPhase()) {
            releaseSound();
            showTimer();
            return;
        }
        refreshClock();
        if (wasBreak || result.inserted) sendFinishNotification(wasBreak);
        if (grouped) {
            showPendingPlanResult();
        } else if (result != null && result.inserted) {
            showFocusResult(taskTitle, seconds, true, result.earnedCoins, false);
        }
    }

    private void showPendingPlanResult() {
        if (pendingPlanResultId == null || focusResultDialog != null) return;
        FocusDatabase.PlanResult result = database.savedFocusPlan(pendingPlanResultId);
        if (result != null) {
            showFocusResult(result.taskTitle, result.seconds, result.completed, result.earnedCoins, true);
        }
    }

    private void showFocusResult(String taskTitle, int seconds, boolean completed, int coins,
                                 boolean wholePlan) {
        if (focusResultDialog != null) focusResultDialog.dismiss();
        View celebration = getLayoutInflater().inflate(R.layout.dialog_complete, null);
        int title = wholePlan
                ? (completed ? R.string.completed_focus_plan : R.string.saved_focus_plan)
                : (completed ? R.string.completed_focus : R.string.saved_focus);
        ((TextView) celebration.findViewById(R.id.completeTitle)).setText(title);
        ((TextView) celebration.findViewById(R.id.completeMessage)).setText(
                getString(R.string.pet_timer_details, taskTitle, FocusTime.format(this, seconds)));
        TextView rewards = celebration.findViewById(R.id.completeCoins);
        rewards.setText(getString(wholePlan ? R.string.completed_plan_coins : R.string.completed_coins,
                coins));
        rewards.setVisibility(View.VISIBLE);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setView(celebration)
                .setPositiveButton(R.string.focus_confirm, (window, which) -> {
                    if (wholePlan) {
                        pendingPlanResultId = null;
                        saveCycleBoundary();
                    }
                    showScreen(true);
                })
                .setCancelable(false)
                .create();
        focusResultDialog = dialog;
        dialog.setOnDismissListener(window -> {
            if (focusResultDialog == dialog) focusResultDialog = null;
        });
        dialog.show();
    }

    private void confirmEnd() {
        if (!roundIsActive()) return;
        if (roundHasExpired()) {
            completeRound();
            return;
        }
        int message = planId != null ? R.string.focus_end_plan
                : breakMode ? R.string.focus_end_rest
                : isCountUp() ? R.string.focus_end_count_up : R.string.focus_end_message;
        endConfirmation = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.focus_end_title)
                .setMessage(message)
                .setNegativeButton(R.string.focus_cancel, null)
                .setPositiveButton(R.string.focus_confirm, (dialog, which) -> endCycle())
                .create();
        endConfirmation.setOnDismissListener(dialog -> endConfirmation = null);
        endConfirmation.show();
    }

    private void endCycle() {
        if (!roundIsActive()) return;
        boolean countUp = isCountUp();
        long remaining = countUp ? 0 : countdownRemainingMs();
        if (running && !countUp && remaining == 0) {
            completeRound();
            return;
        }
        if (breakMode) {
            if (planId != null) {
                if (!settlePlan(false)) return;
            } else {
                clearCycle();
                saveCycleBoundary();
            }
            refreshClock();
            showPendingPlanResult();
            return;
        }
        long elapsedMs = countUp ? countUpElapsedMs() : durationMs() - remaining;
        int seconds = (int) Math.min(Integer.MAX_VALUE, Math.max(0, elapsedMs) / 1000);
        String taskTitle = sessionTaskTitle;
        FocusDatabase.RoundResult result = recordFocusRound(seconds, System.currentTimeMillis(), countUp);
        if (result == null) return;
        boolean grouped = planId != null;
        if (grouped) {
            closeSettledPlan();
        } else {
            clearCycle();
            saveCycleBoundary();
        }
        refreshClock();
        if (grouped) {
            showPendingPlanResult();
        } else if (result.inserted) {
            showFocusResult(taskTitle, seconds, countUp, result.earnedCoins, false);
        }
    }

    private boolean settlePlan(boolean completed) {
        try {
            database.settleFocusPlan(planId, sessionTaskTitle, completed);
        } catch (SQLiteException error) {
            Log.e("MainActivity", "Could not settle focus plan", error);
            Toast.makeText(this, R.string.focus_save_failure, Toast.LENGTH_LONG).show();
            return false;
        }
        closeSettledPlan();
        return true;
    }

    private void closeSettledPlan() {
        pendingPlanResultId = planId;
        clearCycle();
        saveCycleBoundary();
    }

    private FocusDatabase.RoundResult recordFocusRound(int seconds, long finishedAt, boolean completed) {
        if (roundId == null) roundId = UUID.randomUUID().toString();
        // Make the key durable before the database write so a restored timer retries the same row.
        if (timerState().commit()) {
            try {
                return database.saveFocusRound(roundId, sessionTaskId, sessionTaskTitle,
                        seconds, finishedAt, focusMode, completed, planId);
            } catch (SQLiteException error) {
                Log.e("MainActivity", "Could not save focus round", error);
            }
        }
        Toast.makeText(this, R.string.focus_save_failure, Toast.LENGTH_LONG).show();
        return null;
    }

    private void clearCycle() {
        running = false;
        breakMode = false;
        cycleRound = 1;
        remainingMs = durationMs();
        endAt = 0;
        countUpAccumulatedMs = 0;
        segmentStartedElapsedMs = 0;
        segmentStartedWallMs = 0;
        segmentBootCount = -1;
        roundId = null;
        planId = null;
        sessionTaskId = -1;
        sessionTaskTitle = "";
    }

    private void updateSound() {
        releaseSound();
        if (!running || roundHasExpired() || !preferences.getBoolean("sound_enabled", false)) return;
        int type = preferences.getInt("sound_type", 0);
        int resource = type == 1 ? R.raw.forest : type == 2 ? R.raw.waves : R.raw.rain;
        soundPlayer = MediaPlayer.create(this, resource);
        if (soundPlayer == null) return;
        float volume = preferences.getInt("sound_volume", 35) / 100f;
        soundPlayer.setVolume(volume, volume);
        soundPlayer.setLooping(true);
        soundPlayer.start();
    }

    private void releaseSound() {
        if (soundPlayer != null) {
            soundPlayer.release();
            soundPlayer = null;
        }
    }

    private void sendFinishNotification(boolean wasBreak) {
        if (!preferences.getBoolean("notifications", true)) return;
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;
        Intent target = new Intent(this, wasBreak ? MainActivity.class : ReviewActivity.class);
        if (wasBreak) target.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent tap = PendingIntent.getActivity(this, 1, target,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification note = new NotificationCompat.Builder(this, FINISH_CHANNEL)
                .setSmallIcon(R.drawable.ic_tomato_notification)
                .setContentTitle(wasBreak ? "休息结束啦" : getString(R.string.completed_focus))
                .setContentText(wasBreak
                        ? (roundIsActive() ? "已进入下一轮专注" : "本次番茄钟已完成")
                        : "这一轮已经记入你的专注历史")
                .setAutoCancel(true).setContentIntent(tap).build();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(101, note);
    }
}
