package com.zk.client;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
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

    private byte[] mAudioBuffer = new byte[8821];

    /**
     * 去读输入流
     */
    private void read() {
        mAudioTrack.play();
        while (mIsRunning) {
            try {
                int b1 = mInputStream.read();
                int b2 = mInputStream.read();
                int b3 = mInputStream.read();
                int b4 = mInputStream.read();
                if (b1 < 0 || b2 < 0 || b3 < 0 || b4 < 0) {
                    continue;
                }
                final int size = (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
                Log.d(TAG, "read size = " + size);
                if (size > 0 && size < 100000) {
                    final byte[] bytes = new byte[size];
                    int read = 0;
                    while ((read < size)) {
                        read += mInputStream.read(bytes, read, size - read);
                    }
                    int type = bytes[0];
                    Log.d(TAG, "read type = " + type);
                    if (type == 0) {
                        /*new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "read write start");
                                write(bytes, size);
                                Log.d(TAG, "read write end");
                            }
                        }).start();*/
                    } else if (type == 1) {
                        onFrame(bytes, 1, size - 1, 0);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                sendMessage("read error");
            }
        }
        mAudioTrack.stop();
        mAudioTrack.release();
    }

    private synchronized void write(byte[] src, int size) {
        mAudioTrack.write(src, 1, size - 1);
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
                    Log.d(TAG, "sendMsg message = " + message);
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
            prepareAudioTrack();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private AudioTrack mAudioTrack;

    private void prepareAudioTrack() {
        int minBufSize = AudioTrack.getMinBufferSize(44100,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                minBufSize, AudioTrack.MODE_STREAM);
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
        Log.d(TAG, "onFrame end");
    }
}
