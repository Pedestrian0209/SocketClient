package com.zk.client;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SurfaceHolder.Callback {
    private SurfaceView mSurfaceView;
    private Surface mSurface;
    private ImageView mImg;
    private TextView mTxt;
    private SocketClient mClient;
    private String mIp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);
        mSurfaceView.getHolder().addCallback(this);
        mImg = (ImageView) findViewById(R.id.img);
        findViewById(R.id.connect).setOnClickListener(this);
        findViewById(R.id.write).setOnClickListener(this);
        mTxt = (TextView) findViewById(R.id.txt);
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
                    mClient.startClientThread();
                    mClient.mHandler = new Handler() {
                        @Override
                        public void handleMessage(Message msg) {
                            super.handleMessage(msg);
                            if (msg.what == 10) {
                                mImg.setImageBitmap((Bitmap) msg.obj);
                            } else {
                                mTxt.setText(mTxt.getText().toString() + "\n" + msg.obj.toString());
                            }
                        }
                    };
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
}
