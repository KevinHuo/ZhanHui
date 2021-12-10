package com.example.detecthands.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

public class ToastUtils {

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static Toast sToast;

    public static void init(Context context) {
        sToast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
    }

    public static void showShortToast(String message) {
        showToast(message, Toast.LENGTH_SHORT);
    }

    public static void showLongToast(String message) {
        showToast(message, Toast.LENGTH_LONG);
    }

    public static void cancel() {
        if (sToast != null) {
            sToast.cancel();
        }
    }

    private static void showToast(String message, int duration) {
        Runnable runnable = () -> {
            if (sToast != null) {
                sToast.setText(message);
                sToast.setDuration(duration);
                sToast.show();
                Log.e("飞", message);
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            HANDLER.post(runnable);
        }
    }
}
