package com.lxy.bleband.server;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.lxy.bleband.control.DebugService;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ServerService implements DebugService {
    private static final String TAG = "ServerService";
    public static final String serverUuid = "aa87c0d0-afac-11de-8a39-0800200c9a66";
    private static ServerService instance;
    private static Handler mainHandler;
    private BluetoothSocket mSocket;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    volatile static boolean stop = false;

    private ServerService() {
    }
//    保持单例
    public static ServerService getInstance(Handler handler) {
        mainHandler = handler;
        if (instance == null) {
            Log.i(TAG, "server instance is null");
            synchronized (ServerService.class) {
                if (instance == null) {
                    instance = new ServerService();
                }
            }
        }
        return instance;
    }
//    接受对方的指令并发送打开下一个activity的指令
    public void startAccept(final BluetoothAdapter bluetoothAdapter) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BluetoothServerSocket serverSocket
                            = bluetoothAdapter.listenUsingRfcommWithServiceRecord("chat-server", UUID.fromString(serverUuid));
                    mSocket = serverSocket.accept();
                    mSocket.getRemoteDevice();
                    mOutputStream = mSocket.getOutputStream();
                    mInputStream = mSocket.getInputStream();
                    crateReadThread();
                    Message msg = mainHandler.obtainMessage();
                    msg.what = CONNECTED_SUCCESS;
                    msg.obj = mSocket.getRemoteDevice();
                    mainHandler.sendMessage(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "client connect error:" + e.getMessage());
                    mainHandler.sendEmptyMessage(CONNECTED_FAIL);
                }
            }
        }).start();
    }
//    读进程
    private void crateReadThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true && !stop) {
                        int length = 0;
                        while (length == 0 && !stop) {
                            length = mInputStream.available();
                        }
                        byte[] bytes = new byte[length];
                        mInputStream.read(bytes);
                        String text = new String(bytes, "UTF-8");
                        Log.i(TAG, "receive:" + text);
                        Message msg = mainHandler.obtainMessage();
                        msg.obj = text;
                        msg.what = READ_DATA_SUCCESS;
                        mainHandler.sendMessage(msg);
                    }
                } catch (IOException e) {
                    mainHandler.sendEmptyMessage(READ_DATA_FAIL);
                    e.printStackTrace();
                }
            }
        }).start();
    }
//    写进程
    public void write(String text) {
        if (mSocket != null) {
            if (!mSocket.isConnected()) {
                mainHandler.sendEmptyMessage(ServerService.BLUETOOTH_SOCKET_CLOSED);
                Log.i(TAG, "write fail when socket was closed");
                return;
            }
        }
        Message msg = mainHandler.obtainMessage();
        String data = text;
        if (mOutputStream != null) {
            try {
                mOutputStream.write(text.getBytes());
                mOutputStream.flush();
                msg.what = WRITE_DATA_SUCCESS;
            } catch (IOException e) {
                e.printStackTrace();
                data = e.getMessage();
                msg.what = WRITE_DATA_SUCCESS;
            }
        }
        msg.obj = data;
        mainHandler.sendMessage(msg);
    }
//    关闭一些东西
    public void cancel() {
        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mSocket = null;
        }
        if (mOutputStream != null) {
            try {
                mOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mOutputStream = null;
        }
        if (mInputStream != null) {
            try {
                mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mInputStream = null;
        }
    }
}
