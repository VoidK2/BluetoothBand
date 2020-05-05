package com.lxy.bleband.utils;

import android.content.Context;

public class flashSOSThread extends Thread {
    private Context context;

    public flashSOSThread(Context context) {
        this.context = context;
    }

    public void sleeep500ms() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sleeep1500ms() {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        FlashUtils utils = new FlashUtils(this.context);
        for (int i = 0; i < 3; i++) {
            utils.open();
            sleeep500ms();
            utils.close();
            sleeep500ms();
        }
        for (int i = 0; i < 3; i++) {
            utils.open();
            sleeep1500ms();
            utils.close();
            sleeep500ms();
        }
        for (int i = 0; i < 3; i++) {
            utils.open();
            sleeep500ms();
            utils.close();
            sleeep500ms();
        }
    }
}
