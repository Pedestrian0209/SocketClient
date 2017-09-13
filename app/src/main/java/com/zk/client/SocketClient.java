package com.zk.client;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by dell on 2017/9/13.
 */

public class SocketClient {
    private static final String TAG = SocketClient.class.getSimpleName();
    private Socket mSocket;
    private String mIp;//ip地址
    private boolean mIsRunning;
    private PrintWriter mPrintWriter;
    private InputStream mInputStream;
    public static Handler mHandler;

    public SocketClient(String ip) {
        mIp = ip;
    }

    /**
     * 开启客户端线程并建立链接
     */
    public void startClientThread() {
        mIsRunning = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "startClientThread");
                    mSocket = new Socket(mIp, 1024);
                    mSocket.setSoTimeout(5000);
                    Log.d(TAG, "startClientThread 1");
                    mPrintWriter = new PrintWriter(mSocket.getOutputStream());
                    mInputStream = mSocket.getInputStream();
                    Log.d(TAG, "startClientThread 2");
                    read();
                    mPrintWriter.close();
                    mPrintWriter = null;
                    mInputStream.close();
                    mInputStream = null;
                    Log.d(TAG, "startClientThread end");
                } catch (IOException e) {
                    e.printStackTrace();
                    sendMessage("start error");
                }
            }
        }).start();
    }

    /**
     * 停止客户端线程
     */
    public void stopClientThread() {
        mIsRunning = false;
        try {
            mSocket.close();
            mSocket = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 去读输入流
     */
    private void read() {
        while (mIsRunning) {
            try {
                byte[] bt = new byte[50];
                mInputStream.read(bt);
                String str = new String(bt);
                sendMessage(str);
            } catch (IOException e) {
                e.printStackTrace();
                sendMessage("read error");
            }
        }
    }

    private void sendMessage(String message) {
        Message msg = new Message();
        msg.obj = message;
        mHandler.sendMessage(msg);
    }

    public void sendMsg(final String message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mSocket != null && mPrintWriter != null) {
                    mPrintWriter.write(message);
                    mPrintWriter.flush();
                }
            }
        }).start();
    }
}
