package com.adyrsoft.soul.service;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FileTransferService extends Service implements TaskListener {

    public interface TaskProgressListener {
        void onSubscription(HashMap<FileSystemTask, ProgressInfo> pendingTasks);
        void onProgressUpdate(FileSystemTask task, ProgressInfo info);
        void onTaskFinished(FileSystemTask task, TaskResult result);
    }

    public interface TaskErrorListener {
        void onError(FileSystemTask task, ErrorInfo errorInfo);
    }

    public class FileTransferBinder extends Binder {
        public FileTransferService getService() {
            return FileTransferService.this;
        }
    }

    private static final String TAG = FileTransferService.class.getName();
    private final IBinder mBinder = new FileTransferBinder();

    private boolean mReportingErrors;
    private Handler mHandler;
    private ExecutorService mExecutor;
    private TaskErrorListener mErrorListener;
    private ArrayList<TaskProgressListener> mClientListeners = new ArrayList<>();
    private HashMap<FileSystemTask,ProgressInfo> mTaskStatusCache = new HashMap<>();
    private LinkedList<ErrorInfo> mTaskErrorQueue = new LinkedList<>();

    @Override
    public void onProgressUpdate(FileSystemTask task, ProgressInfo info) {
        mTaskStatusCache.put(task, info);

        if (mClientListeners.size() > 0) {
            for (TaskProgressListener listener : mClientListeners) {
                listener.onProgressUpdate(task, info);
            }
        }
    }

    @Override
    public void onTaskFinished(FileSystemTask task, TaskResult result) {
        mTaskStatusCache.remove(task);

        if (mClientListeners.size() > 0) {
            for (TaskProgressListener listener : mClientListeners) {
                listener.onTaskFinished(task, result);
            }
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

        if (mErrorListener != null && !mReportingErrors) {
            mReportingErrors = true;
            reportError();
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

    public void addTaskProgressListener(@NonNull TaskProgressListener listener) {
        mClientListeners.add(listener);
        onSubscription(listener);
    }

    /**
     * Sets the only error listener that the server allows. To remove it just pass null as parameter.
     * @param listener the listener instance
     */
    public void setTaskErrorListener(TaskErrorListener listener) {
        mErrorListener = listener;
        if (mErrorListener != null) {
            reportError();
        }
    }

    public void removeTaskProgressListener(@NonNull TaskProgressListener listener) {
        mClientListeners.remove(listener);
    }

    private void addToQueue(LocalFileSystemTask task) {
        mTaskStatusCache.put(task, new ProgressInfo.Builder().create());
        Future future = mExecutor.submit(task);
        task.setTaskFuture(future);
    }

    private void onSubscription(TaskProgressListener listener) {
        reportAllTasks(listener);
    }

    private void reportError() {
        if (mTaskErrorQueue.peek() != null && mErrorListener != null) {
            ErrorInfo errorInfo = mTaskErrorQueue.peek();
            mErrorListener.onError(errorInfo.getTask(), errorInfo);
        } else {
            mReportingErrors = false;
        }
    }

    private void reportAllTasks(@NonNull TaskProgressListener listener) {
        HashMap<FileSystemTask, ProgressInfo> taskStatusCache = copyTaskStatusCache();
        listener.onSubscription(taskStatusCache);
    }

    private HashMap<FileSystemTask, ProgressInfo> copyTaskStatusCache() {
        HashMap<FileSystemTask, ProgressInfo> copy = new HashMap<>();
        copy.putAll(mTaskStatusCache);
        return copy;
    }
}
