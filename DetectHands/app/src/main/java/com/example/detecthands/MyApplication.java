package com.example.detecthands;

import android.app.Application;

import com.example.detecthands.utils.ContextHolder;
import com.example.detecthands.utils.ToastUtils;
import com.qiniu.pili.droid.shortvideo.PLShortVideoEnv;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PLShortVideoEnv.init(this);
        ToastUtils.init(this);
        ContextHolder.initial(this);
    }
}
