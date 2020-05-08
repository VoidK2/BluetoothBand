package com.lxy.bleband.control;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.lxy.bleband.R;
import com.lxy.bleband.client.ClientService;
import com.lxy.bleband.server.ServerService;
import com.lxy.bleband.utils.flashSOSThread;
import com.lxy.bleband.utils.ringThread;
import com.lxy.bleband.utils.vibrateThread;


public class DebugActivity extends Activity {
    private static final String TAG = "DebugActivity";
    private Button sendTextBtn;
    private Button vibrateBtn;
    private Button flashBtn;
    private Button ringBtn;
    private Button allBtn;
    private Button vibrateBtn2;
    private Button flashBtn2;
    private Button ringBtn2;
    private Button allBtn2;
    private TextView tvName, tvAddress;
    private EditText etInput;
    private boolean isClient;
    private ClientService clientService;
    private ServerService serverService;
    private BluetoothDevice device;
    private String uuid;
    private volatile static boolean exit = false;
    private Handler handler;
    private Short BLErssi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ctrl);
        initPreData();
        bindView();
        initView();
        setListener();
        registerReceiver();
        ExecuteBleMsg();
    }

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, intentFilter);
    }

    private void bindView() {
        tvName = findViewById(R.id.tvName);
        tvAddress = findViewById(R.id.tvAddress);
        sendTextBtn = findViewById(R.id.send_text);
        vibrateBtn = findViewById(R.id.vibrator_test);
        flashBtn = findViewById(R.id.flash_test);
        ringBtn = findViewById(R.id.ring_test);
        allBtn = findViewById(R.id.all_test);
        vibrateBtn2 = findViewById(R.id.vibrator_otherside);
        flashBtn2 = findViewById(R.id.flash_otherside);
        ringBtn2 = findViewById(R.id.ring_otherside);
        allBtn2 = findViewById(R.id.all_otherside);
        etInput = findViewById(R.id.edit_text);
    }

    private void setListener() {
        sendTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = etInput.getText().toString();
                while (text.startsWith("\n")) {
                    text = text.substring(2, text.length());
                }
                while (text.endsWith("\n")) {
                    text = text.substring(0, text.length() - 2);
                }
                if (text.length() > 0) {
                    if (isClient) {
                        Log.i(TAG, text);
                        clientService.write(text);
                    } else {
                        Log.i(TAG, text);
                        serverService.write(text);
                    }
                    etInput.setText("");
                }
            }
        });
        vibrateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrationOnSelf();
            }
        });
        flashBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flashOnSelf();
            }
        });
        ringBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ringOnSelf();
            }
        });
        allBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                allOnSelf();
            }
        });
        vibrateBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrationOnOtherSide();
            }
        });
        flashBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flashOnOtherSide();
            }
        });
        ringBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ringOnOtherSide();
            }
        });
        allBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                allOnOtherSide();
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
    public void ExecuteBleMsg() {
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case DebugService.WRITE_DATA_SUCCESS:
                        /*发送信息*/
                        Log.i(TAG, "write success");
                        String selfText = (String) msg.obj;
                        //todo 执行函数
                        Toast.makeText(DebugActivity.this, selfText, Toast.LENGTH_SHORT).show();
                        break;
                    case DebugService.WRITE_DATA_FAIL:
                        Log.i(TAG, "write fail");
                        allOnSelf();
                        exitChatDialog("对方已经断连，即将停止该Activity", false);
//                        break;
                    case DebugService.READ_DATA_FAIL:
                        allOnSelf();
                        exitChatDialog("对方已经断连，即将停止该Activity", false);
                        break;
                    case DebugService.READ_DATA_SUCCESS:
                        /*接收信息*/
                        Log.i(TAG, "read success");
                        String text = (String) msg.obj;
                        if (text.equals("flash")) {
                            flashOnSelf();
                        } else if (text.equals("ring")) {
                            ringOnSelf();
                        } else if (text.equals("vibrate")) {
                            vibrationOnSelf();
                        } else if (text.equals("alert")) {
                            allOnSelf();
                        } else {
                            Toast.makeText(DebugActivity.this, text, Toast.LENGTH_SHORT).show();
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
            tvName.setText("外星人");
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
            } else if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device2 = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                short rssi = intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI);
                String address = device2.getAddress();
                if (address == device.getAddress()) {
                    BLErssi = rssi;
                }
            }
        }
    };

    private void sendTextToRemoteDevice(String text) {
        if (text.length() > 0) {
            if (isClient) {
                Log.d(TAG, text);
                clientService.write(text);
            } else {
                Log.d(TAG, text);
                serverService.write(text);
            }
        }
    }

    //    震动铃声闪光等函数
    private void vibrationOnSelf() {
        Toast.makeText(DebugActivity.this, "本机震动！", Toast.LENGTH_SHORT).show();
        Thread thread = new vibrateThread(this);
        thread.start();
    }

    private void flashOnSelf() {
        Toast.makeText(DebugActivity.this, "本机闪光灯SOS开启！", Toast.LENGTH_SHORT).show();
        Thread thread = new flashSOSThread(this);
        thread.start();
    }

    private void ringOnSelf() {
        Toast.makeText(DebugActivity.this, "本机响铃！", Toast.LENGTH_SHORT).show();
        Thread thread = new ringThread(this);
        thread.start();
    }

    private void allOnSelf() {
        Toast.makeText(DebugActivity.this, "本机声光提示！", Toast.LENGTH_SHORT).show();
        Thread thread = new vibrateThread(this);
        Thread thread2 = new flashSOSThread(this);
        Thread thread3 = new ringThread(this);
        thread.start();
        thread2.start();
        thread3.start();
    }

    private void vibrationOnOtherSide() {
        Toast.makeText(DebugActivity.this, "手环震动！", Toast.LENGTH_SHORT).show();
        sendTextToRemoteDevice("vibrate");
    }

    private void flashOnOtherSide() {
        Toast.makeText(DebugActivity.this, "手环闪光！", Toast.LENGTH_SHORT).show();
        sendTextToRemoteDevice("flash");
    }

    private void ringOnOtherSide() {
        Toast.makeText(DebugActivity.this, "手环响铃！", Toast.LENGTH_SHORT).show();
        sendTextToRemoteDevice("ring");
    }

    private void allOnOtherSide() {
        Toast.makeText(DebugActivity.this, "手环提示！", Toast.LENGTH_SHORT).show();
        sendTextToRemoteDevice("alert");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothReceiver);
        if (isClient) {
            clientService.cancel();
        }
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
    public void onBackPressed() {
        exitChatDialog("退出当前连接？", false);
    }
}
