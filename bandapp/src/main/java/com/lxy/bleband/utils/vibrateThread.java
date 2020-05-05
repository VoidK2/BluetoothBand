package com.lxy.bleband.utils;

import android.content.Context;
import android.os.Vibrator;

public class vibrateThread extends Thread {
    private Context context;
//    必须传递context，否则无法运行
    public vibrateThread(Context context) {
        this.context = context;
    }

    public void run() {
        Vibrator vibrator = (Vibrator) context.getSystemService(context.VIBRATOR_SERVICE);
        long[] patter = {2000, 1000, 2000, 1000, 2000, 1000};/*循环三次，震动长2s，暂停1s*/
        vibrator.vibrate(patter, -1); /*-1为不循环*/
    }
}
