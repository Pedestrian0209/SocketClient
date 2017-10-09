package com.zk.client;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SurfaceHolder.Callback, View.OnTouchListener {
    private SurfaceView mSurfaceView;
    private Surface mSurface;
    private TextView mTxt;
    private SocketClient mClient;
    private String mIp;
    private AudioTrack mAudioTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);
        mSurfaceView.getHolder().addCallback(this);
        findViewById(R.id.connect).setOnClickListener(this);
        findViewById(R.id.write).setOnClickListener(this);
        mTxt = (TextView) findViewById(R.id.txt);
        findViewById(R.id.container).setOnTouchListener(this);
        //prepareAudioTrack();
    }

    private void prepareAudioTrack() {
        int minBufSize = AudioTrack.getMinBufferSize(44100,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                minBufSize, AudioTrack.MODE_STREAM);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connect:
                getIp();
                if (!TextUtils.isEmpty(mIp)) {
                    mTxt.setText(mTxt.getText().toString() + "\n" + "ip = " + mIp);
                    mClient = new SocketClient("192.168.43.1");
                    mClient.setSurface(mSurface);
                    mClient.mHandler = new Handler() {
                        @Override
                        public void handleMessage(Message msg) {
                            super.handleMessage(msg);
                            switch (msg.what) {
                                case 10:
                                    Log.d("zhang","write start");
                                    //mAudioTrack.write((byte[]) msg.obj, 1, msg.arg1 - 1);
                                    Log.d("zhang","write end");
                                    break;
                            }
                            mTxt.setText(mTxt.getText().toString() + "\n" + msg.obj.toString());
                        }
                    };
                    mClient.startClientThread();
                    //mAudioTrack.play();
                } else {
                    mTxt.setText(mTxt.getText().toString() + "\n" + "ip is null");
                }
                break;
            case R.id.write:
                if (mClient != null) {
                    mClient.sendMsg("haha");
                }
                break;
        }
    }

    private void getIp() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            mIp = intIP2StringIP(wifiInfo.getIpAddress());
        }
    }

    /**
     * 将得到的int类型的IP转换为String类型
     *
     * @param ip
     * @return
     */
    public static String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mClient != null) {
            mClient.stopClientThread();
        }
        //mAudioTrack.stop();
        //mAudioTrack.release();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurface = holder.getSurface();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurface = null;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mClient != null) {
            String str = "TOUCH#" + event.getAction() + "#" + (int) event.getX() + "#" + (int) event.getY() + "#";
            Log.d("SocketClient", "onTouch str = " + str);
            mClient.sendMsg(str);
        }
        return true;
    }
}
