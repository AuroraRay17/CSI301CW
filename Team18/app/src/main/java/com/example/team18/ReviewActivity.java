package com.example.team18;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.Calendar;

public class ReviewActivity extends AppCompatActivity {
    private TextView loadingText, todayMinutes, todaySessions, weekSummary, recentList, historyList, streakText, taskBreakdown;
    private TextView lifetimeDuration;
    private WeekChartView weekChart;
    private ReviewLoader loader;
    private int requestNumber;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);
        getWindow().setStatusBarColor(getColor(R.color.page_background));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        loadingText = findViewById(R.id.loadingText);
        lifetimeDuration = findViewById(R.id.lifetimeDuration);
        todayMinutes = findViewById(R.id.todayMinutes);
        todaySessions = findViewById(R.id.todaySessions);
        weekSummary = findViewById(R.id.weekSummary);
        recentList = findViewById(R.id.recentList);
        historyList = findViewById(R.id.historyList);
        streakText = findViewById(R.id.streakText);
        taskBreakdown = findViewById(R.id.taskBreakdown);
        weekChart = findViewById(R.id.weekChart);
        loader = new ReviewLoader(this);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        findViewById(R.id.shareButton).setOnClickListener(v -> shareReport());
        weekChart.setDayClickListener((day, minutes) -> Toast.makeText(this,
                day + " 专注了 " + minutes + " 分钟", Toast.LENGTH_SHORT).show());
    }

    @Override protected void onResume() {
        super.onResume();
        loadReview();
    }

    @Override protected void onDestroy() {
        requestNumber++;
        super.onDestroy();
    }

    private void loadReview() {
        loadingText.setVisibility(View.VISIBLE);
        loadingText.setText("正在整理最近七天的记录…");
        int request = ++requestNumber;
        loader.load(new ReviewLoader.Callback() {
            @Override public void onLoaded(FocusDatabase.WeekReview review) {
                if (request != requestNumber || isFinishing() || isDestroyed()) return;
                loadingText.setVisibility(View.GONE);
                render(review);
            }

            @Override public void onError() {
                if (request != requestNumber || isFinishing() || isDestroyed()) return;
                loadingText.setText("读取记录失败，请返回后重试。");
            }
        });
    }

    private void render(FocusDatabase.WeekReview review) {
        lifetimeDuration.setText(getString(R.string.pet_growth_total, FocusTime.format(this, review.lifetimeSeconds)));
        lifetimeDuration.setVisibility(View.VISIBLE);
        todayMinutes.setText(String.valueOf(review.todayMinutes));
        todaySessions.setText(String.valueOf(review.todaySessions));
        weekSummary.setText(review.totalMinutes + " 分钟 · " + review.totalSessions + " 轮");
        weekChart.setData(review.dailyMinutes, review.dayLabels);
        int streak = 0;
        for (int i = 6; i >= 0; i--) {
            if (review.dailySeconds[i] == 0) {
                if (i == 6) continue;
                break;
            }
            streak++;
        }
        streakText.setText("连续专注 " + streak + " 天 · 为自己鼓掌 👏");
        Map<String, Integer> totals = new HashMap<>();
        for (FocusDatabase.Session session : review.allSessions) {
            totals.put(session.taskTitle, totals.containsKey(session.taskTitle)
                    ? totals.get(session.taskTitle) + session.seconds : session.seconds);
        }
        StringBuilder breakdown = new StringBuilder();
        totals.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue())
                .limit(5).forEach(entry -> breakdown.append(entry.getKey()).append("  ·  ")
                .append(entry.getValue() / 60).append(" 分钟\n"));
        taskBreakdown.setText(breakdown.length() == 0 ? "还没有任务记录。" : breakdown.toString().trim());
        SimpleDateFormat format = new SimpleDateFormat("M月d日 HH:mm", Locale.CHINA);
        StringBuilder history = new StringBuilder();
        for (FocusDatabase.Session session : review.allSessions) {
            history.append(format.format(new Date(session.finishedAt))).append("   ")
                    .append(session.taskTitle).append("\n")
                    .append(session.seconds / 60).append("分")
                    .append(session.seconds % 60).append("秒")
                    .append("  ·  ").append(sessionModeLabel(session))
                    .append(session.completed ? "  ·  已保存" : "  ·  提前结束")
                    .append("\n\n");
        }
        historyList.setText(history.length() == 0 ? "还没有专注记录。" : history.toString().trim());
        if (review.recentSessions.isEmpty()) {
            recentList.setText("完成一轮专注后，记录会出现在这里。");
            return;
        }
        StringBuilder text = new StringBuilder();
        for (FocusDatabase.Session session : review.recentSessions) {
            if (text.length() > 0) text.append("\n\n");
            text.append(session.taskTitle).append("  ·  ").append(session.seconds / 60)
                    .append("分").append(session.seconds % 60).append("秒")
                    .append("  ·  ").append(sessionModeLabel(session)).append("\n")
                    .append(format.format(new Date(session.finishedAt)));
        }
        recentList.setText(text.toString());
    }

    private String sessionModeLabel(FocusDatabase.Session session) {
        return getString(session.mode == FocusTime.Mode.COUNT_UP
                ? R.string.focus_count_up : R.string.focus_countdown);
    }

    private void shareReport() {
        String summary = "🍅 我的专注周报：" + weekSummary.getText() + "。" + streakText.getText();
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, summary);
        startActivity(Intent.createChooser(share, "分享专注周报"));
    }
}
