package com.lxy.bleband.server;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import com.lxy.bleband.client.ClientService;
import com.lxy.bleband.control.DebugActivity;

public class AcceptService extends Service {
    private static final String TAG = "AcceptService";

    @Override
    public void onCreate() {
        super.onCreate();
        initServerService();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private ServerService serverService;
    private Handler serverHandler;
//    接受指令并打开下一个activity
    @SuppressLint("HandlerLeak")
    private void initServerService() {
        serverHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case ClientService.CONNECTED_SUCCESS:
                        Log.i(TAG, "accept connected");
                        BluetoothDevice device = (BluetoothDevice) msg.obj;
                        Intent intent = new Intent(AcceptService.this, DebugActivity.class);
                        intent.putExtra("device", device);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        break;
                    case ClientService.CONNECTED_FAIL:
                        Log.i(TAG, "connected fail");
                        break;
                }
            }
        };
        serverService = ServerService.getInstance(serverHandler);
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        serverService.startAccept(adapter);
    }
//    销毁activity和service
    @Override
    public void onDestroy() {
        super.onDestroy();
        serverService.cancel();
    }
}
