package com.adyrsoft.soul.service;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.LinkedList;
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
    private FileTransferListener mClientListener;
    private HashMap<FileSystemTask,ProgressInfo> mTaskStatusCache = new HashMap<>();
    private LinkedList<ErrorInfo> mTaskErrorQueue = new LinkedList<>();

    @Override
    public void onProgressUpdate(FileSystemTask task, ProgressInfo info) {
        mTaskStatusCache.put(task, info);

        if (mClientListener != null) {
            mClientListener.onProgressUpdate(task, info);
        }
    }

    @Override
    public void onTaskFinished(FileSystemTask task, TaskResult result) {
        mTaskStatusCache.remove(task);

        if (mClientListener != null) {
            mClientListener.onTaskFinished(task, result);
        }
    }

    @Override
    public void onError(FileSystemTask task, ErrorInfo errorInfo) {
        UserFeedbackProvider feedbackProvider = errorInfo.getFeedbackProvider();

        mTaskErrorQueue.add(errorInfo);

        feedbackProvider.setCallback(new UserFeedbackProvider.OnFeedbackProvided() {
            @Override
            public void onFeedbackProvided() {
                mTaskErrorQueue.poll();
                reportError();
            }
        });

        if (mClientListener != null) {
            mClientListener.onError(task, errorInfo);
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
        addToQueue(task);
        return task;
    }

    public FileSystemTask move(Uri srcWD, List<Uri> srcs, Uri dest) {
        LocalFileSystemTask task = new LocalFileSystemTask(FileOperation.MOVE, srcWD, srcs, dest, this, mHandler);
        addToQueue(task);
        return task;
    }

    public FileSystemTask remove(Uri srcWD, List<Uri> srcs) {
        LocalFileSystemTask task = new LocalFileSystemTask(FileOperation.REMOVE, srcWD, srcs, null, this, mHandler);
        addToQueue(task);
        return task;
    }

    private void addToQueue(LocalFileSystemTask task) {
        mTaskStatusCache.put(task, new ProgressInfo.Builder().create());
        Future future = mExecutor.submit(task);
        task.setTaskFuture(future);
    }

    public void setClientTaskListener(@NonNull FileTransferListener listener) {
        mClientListener = listener;
        onSubscription();
    }

    private void onSubscription() {
        reportAllTasks();
        reportError();
    }

    private void reportError() {
        if (mTaskErrorQueue.peek() != null && mClientListener != null) {
            ErrorInfo errorInfo = mTaskErrorQueue.peek();
            mClientListener.onError(errorInfo.getTask(), errorInfo);
        }
    }

    private void reportAllTasks() {
        HashMap<FileSystemTask, ProgressInfo> taskStatusCache = copyTaskStatusCache();
        mClientListener.onSubscription(taskStatusCache);
    }

    private HashMap<FileSystemTask, ProgressInfo> copyTaskStatusCache() {
        HashMap<FileSystemTask, ProgressInfo> copy = new HashMap<>();
        copy.putAll(mTaskStatusCache);
        return copy;
    }

    public void removeCallback() {
        mClientListener = null;
    }

}
