package com.example.team18;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    private SharedPreferences preferences;
    private Spinner soundSpinner, themeSpinner;
    private Switch notificationSwitch, lightSwitch, batterySwitch, soundSwitch;
    private SeekBar volumeBar;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_settings);
        getWindow().setStatusBarColor(getColor(R.color.page_background));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        preferences = getSharedPreferences("focus_settings", MODE_PRIVATE);
        soundSpinner = findViewById(R.id.soundSpinner);
        themeSpinner = findViewById(R.id.themeSpinner);
        notificationSwitch = findViewById(R.id.notificationSwitch);
        lightSwitch = findViewById(R.id.lightSwitch);
        batterySwitch = findViewById(R.id.batterySwitch);
        soundSwitch = findViewById(R.id.soundSwitch);
        volumeBar = findViewById(R.id.volumeBar);
        setupSpinner(soundSpinner, new String[]{"雨声", "森林鸟鸣", "海浪水声"});
        setupSpinner(themeSpinner, new String[]{"珊瑚粉", "晴空蓝"});
        soundSpinner.setSelection(preferences.getInt("sound_type", 0));
        themeSpinner.setSelection(preferences.getInt("theme_type", 0));
        notificationSwitch.setChecked(preferences.getBoolean("notifications", true));
        lightSwitch.setChecked(preferences.getBoolean("light_hint", false));
        batterySwitch.setChecked(preferences.getBoolean("battery_hint", false));
        soundSwitch.setChecked(preferences.getBoolean("sound_enabled", false));
        volumeBar.setProgress(preferences.getInt("sound_volume", 35));
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        findViewById(R.id.saveSettingsButton).setOnClickListener(v -> save());
        DeveloperTools.install(this, findViewById(R.id.settingsTitle));
    }

    private void setupSpinner(Spinner spinner, String[] choices) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, choices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void save() {
        preferences.edit()
                .putInt("sound_type", soundSpinner.getSelectedItemPosition())
                .putInt("theme_type", themeSpinner.getSelectedItemPosition())
                .putInt("sound_volume", volumeBar.getProgress())
                .putBoolean("notifications", notificationSwitch.isChecked())
                .putBoolean("light_hint", lightSwitch.isChecked())
                .putBoolean("battery_hint", batterySwitch.isChecked())
                .putBoolean("sound_enabled", soundSwitch.isChecked()).apply();
        Toast.makeText(this, "偏好已保存", Toast.LENGTH_SHORT).show();
        finish();
    }
}
