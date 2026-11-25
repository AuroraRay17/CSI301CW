package com.example.team18;

import android.content.SharedPreferences;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class TaskActivity extends AppCompatActivity {
    private FocusDatabase database;
    private SharedPreferences preferences;
    private final List<FocusDatabase.Task> tasks = new ArrayList<>();
    private EditText taskInput;
    private ListView taskList;
    private TextView taskCount, emptyTask;
    private long selectedTaskId;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);
        getWindow().setStatusBarColor(getColor(R.color.page_background));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        database = new FocusDatabase(this);
        preferences = getSharedPreferences("focus_settings", MODE_PRIVATE);
        taskInput = findViewById(R.id.taskInput);
        taskList = findViewById(R.id.taskList);
        taskCount = findViewById(R.id.taskCount);
        emptyTask = findViewById(R.id.emptyTask);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        findViewById(R.id.addTaskButton).setOnClickListener(v -> addTask());
        taskList.setOnItemClickListener((parent, view, position, id) -> selectTask(tasks.get(position)));
        taskList.setOnItemLongClickListener((parent, view, position, id) -> {
            showTaskActions(tasks.get(position));
            return true;
        });
    }

    @Override protected void onResume() {
        super.onResume();
        selectedTaskId = preferences.getLong("selected_task_id", -1);
        refreshTasks();
    }

    @Override protected void onDestroy() {
        database.close();
        super.onDestroy();
    }

    private boolean sessionActive() {
        return TimerDurations.hasActiveRound(preferences);
    }

    private void selectTask(FocusDatabase.Task task) {
        if (task.done) {
            Toast.makeText(this, "已完成的任务不能开始新一轮", Toast.LENGTH_SHORT).show();
            return;
        }
        if (sessionActive()) {
            Toast.makeText(this, "请先结束当前轮次，再切换任务", Toast.LENGTH_SHORT).show();
            return;
        }
        selectedTaskId = task.id;
        preferences.edit().putLong("selected_task_id", task.id)
                .putString("selected_task_title", task.title).apply();
        finish();
    }

    private void addTask() {
        String title = taskInput.getText().toString().trim();
        if (title.isEmpty()) {
            taskInput.setError("请输入任务名称");
            return;
        }
        database.addTask(title);
        taskInput.setText("");
        refreshTasks();
    }

    private void refreshTasks() {
        tasks.clear();
        tasks.addAll(database.tasks());
        int unfinished = 0;
        for (FocusDatabase.Task task : tasks) if (!task.done) unfinished++;
        taskCount.setText(unfinished + " 个待办");
        emptyTask.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
        taskList.setVisibility(tasks.isEmpty() ? View.GONE : View.VISIBLE);
        ViewGroup.LayoutParams layout = taskList.getLayoutParams();
        layout.height = Math.round(tasks.size() * 59f * getResources().getDisplayMetrics().density);
        taskList.setLayoutParams(layout);
        taskList.setAdapter(new ArrayAdapter<FocusDatabase.Task>(this, R.layout.task_row, tasks) {
            @Override public View getView(int position, View recycled, ViewGroup parent) {
                View row = recycled == null
                        ? LayoutInflater.from(TaskActivity.this).inflate(R.layout.task_row, parent, false)
                        : recycled;
                FocusDatabase.Task task = getItem(position);
                TextView symbol = row.findViewById(R.id.taskSymbol);
                TextView title = row.findViewById(R.id.taskTitle);
                TextView badge = row.findViewById(R.id.taskBadge);
                boolean selected = task.id == selectedTaskId && !task.done;
                symbol.setText(task.done ? "✓" : selected ? "●" : "○");
                title.setText(task.title);
                title.setPaintFlags(task.done
                        ? title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                        : title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                title.setTextColor(getColor(task.done ? R.color.muted_text : R.color.ink));
                badge.setText(task.done ? "已完成" : selected
                        ? "已选中 · 目标" + task.goal + "轮" : "目标" + task.goal + "轮  ›");
                badge.setTextColor(getColor(selected ? R.color.tomato_dark : R.color.muted_text));
                return row;
            }
        });
    }

    private void showTaskActions(FocusDatabase.Task task) {
        if (task.id == selectedTaskId && sessionActive()) {
            Toast.makeText(this, "当前轮次进行中，暂时不能修改这个任务", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] actions = {"查看目标与备注", "编辑名称", task.done ? "标记未完成" : "标记完成", "删除任务"};
        new MaterialAlertDialogBuilder(this).setTitle(task.title).setItems(actions, (dialog, which) -> {
            if (which == 0) {
                startActivity(new Intent(this, TaskDetailActivity.class).putExtra("task_id", task.id));
            } else if (which == 1) {
                EditText input = new EditText(this);
                input.setText(task.title);
                new MaterialAlertDialogBuilder(this).setTitle("编辑任务").setView(input)
                        .setNegativeButton("取消", null)
                        .setPositiveButton("保存", (d, w) -> {
                            String title = input.getText().toString().trim();
                            if (title.isEmpty()) {
                                Toast.makeText(this, "名称不能为空", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            database.updateTask(task.id, title);
                            if (selectedTaskId == task.id) {
                                preferences.edit().putString("selected_task_title", title).apply();
                            }
                            refreshTasks();
                        }).show();
            } else if (which == 2) {
                database.setDone(task.id, !task.done);
                if (selectedTaskId == task.id && !task.done) clearSelection();
                refreshTasks();
            } else {
                new MaterialAlertDialogBuilder(this).setTitle("删除任务？")
                        .setMessage("历史专注记录会保留。")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("删除", (d, w) -> {
                            database.deleteTask(task.id);
                            if (selectedTaskId == task.id) clearSelection();
                            refreshTasks();
                        }).show();
            }
        }).show();
    }

    private void clearSelection() {
        selectedTaskId = -1;
        preferences.edit().putLong("selected_task_id", -1)
                .putString("selected_task_title", "").apply();
    }
}
