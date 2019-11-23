package leavesc.hello.filetransfer;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.Collection;

import leavesc.hello.filetransfer.broadcast.DirectBroadcastReceiver;
import leavesc.hello.filetransfer.callback.DirectActionListener;
import leavesc.hello.filetransfer.model.FileTransfer;
import leavesc.hello.filetransfer.service.WifiServerService;

/**
 * 作者：leavesC
 * 时间：2019/2/27 23:52
 * 描述：
 * GitHub：https://github.com/leavesC
 * Blog：https://www.jianshu.com/u/9df45b87cfdf
 */
public class ReceiveFileActivity extends BaseActivity implements DirectActionListener {

    private WifiP2pManager wifiP2pManager;

    private WifiP2pManager.Channel channel;

    private BroadcastReceiver broadcastReceiver;

    private WifiServerService wifiServerService;

    private ProgressDialog progressDialog;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            WifiServerService.MyBinder binder = (WifiServerService.MyBinder) service;
            wifiServerService = binder.getService();
            wifiServerService.setProgressChangListener(progressChangListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            wifiServerService = null;
            bindService();
        }
    };

    private WifiServerService.OnProgressChangListener progressChangListener = new WifiServerService.OnProgressChangListener() {
        @Override
        public void onProgressChanged(final FileTransfer fileTransfer, final int progress) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.setMessage("文件名： " + new File(fileTransfer.getFilePath()).getName());
                    progressDialog.setProgress(progress);
                    progressDialog.show();
                }
            });
        }

        @Override
        public void onTransferFinished(final File file) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.cancel();
                    if (file != null && file.exists()) {
                        Glide.with(ReceiveFileActivity.this).load(file.getPath()).into(iv_image);
                    }
                }
            });
        }
    };

    private static final String TAG = "ReceiveFileActivity";

    private ImageView iv_image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_file);
        initView();
        wifiP2pManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        if (wifiP2pManager == null) {
            finish();
            return;
        }
        channel = wifiP2pManager.initialize(this, getMainLooper(), this);
        broadcastReceiver = new DirectBroadcastReceiver(wifiP2pManager, channel, this);
        registerReceiver(broadcastReceiver, DirectBroadcastReceiver.getIntentFilter());
        bindService();
    }

    private void initView() {
        setTitle("接收文件");
        iv_image = findViewById(R.id.iv_image);
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setTitle("正在接收文件");
        progressDialog.setMax(100);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wifiServerService != null) {
            wifiServerService.setProgressChangListener(null);
            unbindService(serviceConnection);
        }
        unregisterReceiver(broadcastReceiver);
        removeGroup();
        stopService(new Intent(this, WifiServerService.class));
    }

    @Override
    public void wifiP2pEnabled(boolean enabled) {
        Log.e(TAG, "wifiP2pEnabled: " + enabled);
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        Log.e(TAG, "onConnectionInfoAvailable");
        Log.e(TAG, "isGroupOwner：" + wifiP2pInfo.isGroupOwner);
        Log.e(TAG, "groupFormed：" + wifiP2pInfo.groupFormed);
        if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
            if (wifiServerService != null) {
                startService(new Intent(this, WifiServerService.class));
            }
        }
    }

    @Override
    public void onDisconnection() {
        Log.e(TAG, "onDisconnection");
    }

    @Override
    public void onSelfDeviceAvailable(WifiP2pDevice wifiP2pDevice) {
        Log.e(TAG, "onSelfDeviceAvailable");
    }

    @Override
    public void onPeersAvailable(Collection<WifiP2pDevice> wifiP2pDeviceList) {
        Log.e(TAG, "onPeersAvailable");
    }

    @Override
    public void onChannelDisconnected() {
        Log.e(TAG, "onChannelDisconnected");
    }

    public void createGroup(View view) {
        showLoadingDialog("正在创建群组");
        wifiP2pManager.createGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.e(TAG, "createGroup onSuccess");
                dismissLoadingDialog();
                showToast("onSuccess");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "createGroup onFailure: " + reason);
                dismissLoadingDialog();
                showToast("onFailure");
            }
        });
    }

    public void removeGroup(View view) {
        removeGroup();
    }

    private void bindService() {
        Intent intent = new Intent(ReceiveFileActivity.this, WifiServerService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    private void removeGroup() {
        wifiP2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.e(TAG, "removeGroup onSuccess");
                showToast("onSuccess");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "removeGroup onFailure");
                showToast("onFailure");
            }
        });
    }

}