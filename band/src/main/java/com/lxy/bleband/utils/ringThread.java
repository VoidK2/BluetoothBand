package com.lxy.bleband.utils;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;


public class ringThread extends Thread {
    private Context context;

    public ringThread(Context context) {
        this.context = context;
    }

    public void run() {
        Uri defaultRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE); /*播放默认铃声*/
//        Uri defaultRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
        Ringtone ringtone = RingtoneManager.getRingtone(context, defaultRingtoneUri);
        ringtone.play();
    }
}
