package com.example.team18;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class ReviewLoader {
    public interface Callback {
        void onLoaded(FocusDatabase.WeekReview review);
        void onError();
    }

    private final Context applicationContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ReviewLoader(Context context) {
        applicationContext = context.getApplicationContext();
    }

    public void load(Callback callback) {
        new Thread(() -> {
            FocusDatabase database = new FocusDatabase(applicationContext);
            try {
                FocusDatabase.WeekReview review = database.weekReview();
                mainHandler.post(() -> callback.onLoaded(review));
            } catch (RuntimeException error) {
                mainHandler.post(callback::onError);
            } finally {
                database.close();
            }
        }).start();
    }
}
