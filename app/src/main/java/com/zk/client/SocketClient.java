package com.zk.client;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * Created by dell on 2017/9/13.
 */

public class SocketClient {
    private static final String TAG = SocketClient.class.getSimpleName();
    private static final String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC; // H.264 Advanced Video Coding
    private static final int VIDEO_BITRATE = 500000; // 500Kbps
    private static final int FRAME_RATE = 30; // 30 fps
    private static final int IFRAME_INTERVAL = 2; // 2 seconds between I-frames
    private static final int TIMEOUT_US = 10000;
    private int mWidth = 720;
    private int mHeight = 1280;
    private int mDpi = 1;
    private MediaCodec mDecoder;
    private Socket mSocket;
    private String mIp;//ip地址
    private boolean mIsRunning;
    private PrintWriter mPrintWriter;
    private InputStream mInputStream;
    private Surface mSurface;
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
                if (true) {
                    int b1 = mInputStream.read();
                    int b2 = mInputStream.read();
                    int b3 = mInputStream.read();
                    int b4 = mInputStream.read();
                    int length = (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
                    byte[] bytes = new byte[length];
                    int read = 0;
                    while ((read < length)) {
                        read += mInputStream.read(bytes, read, length - read);
                    }
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, length);
                    Message message = new Message();
                    message.what = 10;
                    message.obj = bitmap;
                    mHandler.sendMessage(message);
                    continue;
                }
                byte[] bt = new byte[100000];
                mInputStream.read(bt);
                int length = bytesToInt(bt, 0);
                if (length > 4 && length < 100000) {
                    sendMessage("length = " + length);
                    onFrame(bt, 4, length, 0);
                }
            } catch (IOException e) {
                e.printStackTrace();
                sendMessage("read error");
            }
        }
    }

    public static int bytesToInt(byte[] src, int offset) {
        int value;
        value = (int) ((src[offset] & 0xFF)
                | ((src[offset + 1] & 0xFF) << 8)
                | ((src[offset + 2] & 0xFF) << 16)
                | ((src[offset + 3] & 0xFF) << 24));
        return value;
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

    public void setSurface(Surface surface) {
        mSurface = surface;
        try {
            prepareDecoder();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void prepareDecoder() throws IOException {
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
        Log.d(TAG, "created video format: " + format);
        mDecoder = MediaCodec.createDecoderByType(MIME_TYPE);
        mDecoder.configure(format, mSurface, null, 0);
        Log.d(TAG, "created input surface: " + mSurface);
        mDecoder.start();
    }

    private int mCount = 0;

    public void onFrame(byte[] buf, int offset, int length, int flag) {
        Log.d(TAG, "onFrame length = " + length);
        ByteBuffer[] inputBuffers = mDecoder.getInputBuffers();
        int inputBufferIndex = mDecoder.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(buf, offset, length);
            mDecoder.queueInputBuffer(inputBufferIndex, 0, length, mCount * 1000000 / FRAME_RATE, 0);
            mCount++;
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mDecoder.dequeueOutputBuffer(bufferInfo, 0);
        while (outputBufferIndex >= 0) {
            mDecoder.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mDecoder.dequeueOutputBuffer(bufferInfo, 0);
        }
    }
}
