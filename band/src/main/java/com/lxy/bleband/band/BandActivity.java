package com.lxy.bleband.band;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.lxy.bleband.R;
import com.lxy.bleband.client.ClientService;
import com.lxy.bleband.server.ServerService;
import com.lxy.bleband.utils.flashSOSThread;
import com.lxy.bleband.utils.ringThread;
import com.lxy.bleband.utils.vibrateThread;

public class BandActivity extends Activity {
    private static final String TAG = "BandActivity";
    private TextView tvName, tvAddress;
    private Button sendAlert;
    private BluetoothDevice device;
    private String uuid;
    private boolean isClient;
    private Handler handler;
    private ClientService clientService;
    private ServerService serverService;
    private volatile static boolean exit = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_band);
        initPreData();
        bindView();
        initView();
        setSendAlert();
        registerReceiver();
        ExecuteBleMsg();
    }

    //    注册接收器
    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, intentFilter);
    }
    private void bindView() {
        tvName = findViewById(R.id.tvName);
        tvAddress = findViewById(R.id.tvAddress);
        sendAlert = findViewById(R.id.send_alert);
    }
    private void setSendAlert(){
        sendAlert.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                String alertString = "alert";
                if (isClient) {
                    clientService.write(alertString);
                } else {
                    serverService.write(alertString);
                }
            }
        });
    }
    private void initPreData() {
        device = getIntent().getParcelableExtra("device");
        uuid = getIntent().getStringExtra("uuid");
        if (uuid == null) {
            isClient = false;
        } else {
            isClient = true;
        }
        if (device == null) {
            Toast.makeText(this, "对方已经退出连接!", Toast.LENGTH_LONG).show();
            finish();
        }
    }
    @SuppressLint("HandlerLeak")
    private void ExecuteBleMsg() {
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case DebugService.WRITE_DATA_SUCCESS:
                        Log.i(TAG, "write success");
                        String selfText = (String) msg.obj;
                        break;
                    case DebugService.WRITE_DATA_FAIL:
                        Log.i(TAG, "write fail");
                        allOnSelf();
                        exitChatDialog("对方已经退出连接，即将停止进程", false);
                    case DebugService.READ_DATA_FAIL:
                        allOnSelf();
                        exitChatDialog("对方已经退出连接，即将停止进程", false);
                        break;
                    case DebugService.READ_DATA_SUCCESS:
                        Log.i(TAG, "read success");
                        String text = (String) msg.obj;
                        if(text.equals("flash")){
                            flashOnSelf();
                        }else if(text.equals("ring")){
                            ringOnSelf();
                        }else if(text.equals("vibrate")){
                            vibrationOnSelf();
                        }else if(text.equals("alert")){
                            allOnSelf();
                        }else{
                            Toast.makeText(BandActivity.this, text, Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
            }
        };
        if (isClient) {
            clientService = ClientService.getInstance(handler);
            clientService.connect(device, uuid);
        } else {
            serverService = ServerService.getInstance(handler);
        }
    }

    private void initView() {
        if (device.getName() == null) {
            tvName.setText("未知设备");
        } else {
            tvName.setText(device.getName());
        }
        tvAddress.setText(device.getAddress());
    }

    BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, intent.getAction());
            if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                if (clientService != null) {
                    clientService.cancel();
                }
                allOnSelf();
                exitChatDialog("当前连接已断开，请重新连接", false);
            } else if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    allOnSelf();
                    exitChatDialog("当前连接已断开，请重新连接", false);
                }
            }
        }
    };

    private void vibrationOnSelf(){
        Toast.makeText(BandActivity.this, "本机震动！", Toast.LENGTH_SHORT).show();
        Thread thread = new vibrateThread(this);
        thread.start();
    }
    private void flashOnSelf() {
        Toast.makeText(BandActivity.this, "本机闪光灯SOS开启！", Toast.LENGTH_SHORT).show();
        Thread thread = new flashSOSThread(this);
        thread.start();
    }
    private void ringOnSelf(){
        Toast.makeText(BandActivity.this, "本机响铃！", Toast.LENGTH_SHORT).show();
        Thread thread = new ringThread(this);
        thread.start();
    }
    private void allOnSelf(){
        Toast.makeText(BandActivity.this, "本机声光提示！", Toast.LENGTH_SHORT).show();
        Thread thread = new vibrateThread(this);
        Thread thread2 = new flashSOSThread(this);
        Thread thread3 = new ringThread(this);
        thread.start();
        thread2.start();
        thread3.start();
    }

    @Override
    public void onBackPressed() {
        exitChatDialog("确认退出连接？", false);
    }
    public void exitChatDialog(String text, boolean cancelable) {
        if (exit) {
            return;
        }
        exit = true;
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        if (isClient) {
                            clientService.cancel();
                        }
                    }
                })
                .setMessage(text).create();
        dialog.setCancelable(cancelable);
        dialog.show();
        if (isClient) {
            clientService.cancel();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothReceiver);
        if (isClient) {
            clientService.cancel();
        }
    }

}
