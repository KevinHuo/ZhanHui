package com.example.detecthands.utils;

import android.content.Context;

public class ContextHolder {

    static Context sApplicationcontext;

    public static void initial(Context context) {
        sApplicationcontext = context;
    }

    public static Context getContext() {
        return sApplicationcontext;
    }
}
