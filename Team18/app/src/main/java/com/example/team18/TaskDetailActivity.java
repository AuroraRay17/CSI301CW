package com.example.team18;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class TaskDetailActivity extends AppCompatActivity {
    private FocusDatabase database;
    private long taskId;
    private EditText goalInput, noteInput;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_task_detail);
        getWindow().setStatusBarColor(getColor(R.color.page_background));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        database = new FocusDatabase(this);
        taskId = getIntent().getLongExtra("task_id", -1);
        FocusDatabase.Task task = database.task(taskId);
        if (task == null) { finish(); return; }
        goalInput = findViewById(R.id.goalInput);
        noteInput = findViewById(R.id.noteInput);
        ((TextView) findViewById(R.id.detailTitle)).setText(task.title);
        ((TextView) findViewById(R.id.detailProgress)).setText(
                "已完成 " + database.completedForTask(task.id) + " / " + task.goal + " 轮");
        goalInput.setText(String.valueOf(task.goal));
        noteInput.setText(task.note);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        findViewById(R.id.saveDetailButton).setOnClickListener(v -> save());
    }

    private void save() {
        try {
            int goal = Integer.parseInt(goalInput.getText().toString());
            if (goal < 1 || goal > 99) throw new NumberFormatException();
            database.updateTaskDetails(taskId, goal, noteInput.getText().toString().trim());
            Toast.makeText(this, "任务目标已保存", Toast.LENGTH_SHORT).show();
            finish();
        } catch (NumberFormatException error) {
            goalInput.setError("请输入 1–99 轮");
        }
    }

    @Override protected void onDestroy() {
        database.close();
        super.onDestroy();
    }
}
