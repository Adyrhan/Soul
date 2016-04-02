package com.adyrsoft.soul;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.adyrsoft.soul.service.FileTransferService;

/**
 * Created by Adrian on 08/03/2016.
 */
public class SoulApplication extends Application {
    private static final String TAG = SoulApplication.class.getName();
    private FileTransferService mTransferService;
    private Object mMonitor = new Object();

    public void requestFileTransferService(RequestFileTransferServiceCallback callback) {
        if (callback == null) {
            return;
        }

        if (mTransferService != null) {
            callback.onServiceReady(mTransferService);
        } else {
            doBindService(callback);
        }

    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    private void doBindService(final RequestFileTransferServiceCallback callback) {
        Intent serviceIntent = new Intent(this, FileTransferService.class);
        ServiceConnection mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mTransferService = ((FileTransferService.FileTransferBinder)service).getService();
                callback.onServiceReady(mTransferService);
                Log.d(TAG, "Binded to FileTransferService");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mTransferService = null;
                Log.d(TAG, "Unbinded from FileTransferService");
            }
        };

        bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }
}
