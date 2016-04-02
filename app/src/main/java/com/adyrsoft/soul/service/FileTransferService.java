package com.adyrsoft.soul.service;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FileTransferService extends Service implements TaskListener {
    public class FileTransferBinder extends Binder {
        public FileTransferService getService() {
            return FileTransferService.this;
        }
    }

    private static final String TAG = FileTransferService.class.getName();
    private final IBinder mBinder = new FileTransferBinder();

    private Handler mHandler;
    private ExecutorService mExecutor;
    private TaskListener mClientListener;

    @Override
    public void onProgressUpdate(FileSystemTask task, ProgressInfo info) {
        if (mClientListener != null) {
            mClientListener.onProgressUpdate(task, info);
        }
    }

    @Override
    public void onError(FileSystemTask task, Uri srcFile, Uri dstFile, FileSystemErrorType errorType) {
        if (mClientListener != null) {
            mClientListener.onError(task, srcFile, dstFile, errorType);
        }
    }

    public FileTransferService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        mHandler = new Handler();
        mExecutor = Executors.newCachedThreadPool();
    }

    public FileSystemTask copy(Uri srcWD, List<Uri> srcs, Uri dest) {
        LocalFileSystemTask task = new LocalFileSystemTask(FileOperation.COPY, srcWD, srcs, dest, this, mHandler);
        Future future = mExecutor.submit(task);
        task.setTaskFuture(future);
        return task;
    }

    public FileSystemTask move(Uri srcWD, List<Uri> srcs, Uri dest) {
        LocalFileSystemTask task = new LocalFileSystemTask(FileOperation.MOVE, srcWD, srcs, dest, this, mHandler);
        Future future = mExecutor.submit(task);
        task.setTaskFuture(future);
        return task;
    }

    public FileSystemTask remove(Uri srcWD, List<Uri> srcs) {
        LocalFileSystemTask task = new LocalFileSystemTask(FileOperation.REMOVE, srcWD, srcs, null, this, mHandler);
        Future future = mExecutor.submit(task);
        task.setTaskFuture(future);
        return task;
    }

    public void setClientTaskListener(TaskListener listener) { mClientListener = listener; }

    public void removeCallback() {
        mClientListener = null;
    }

}
