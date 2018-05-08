package com.wasu.tvoscamera.suface;

import android.os.Handler;
import android.os.Looper;

public class MainHandler extends Handler {
    private static volatile MainHandler instance=null;

    public static MainHandler getInstance() {
        if (null == instance) {
            synchronized (MainHandler.class) {
                if (null == instance) {
                    instance = new MainHandler();
                }
            }
        }
        return instance;
    }
    private MainHandler() {
        super(Looper.getMainLooper());
    }
}
