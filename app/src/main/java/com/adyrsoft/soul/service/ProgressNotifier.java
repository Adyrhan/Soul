package com.adyrsoft.soul.service;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Pair;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

/**
 * Created by Adrian on 17/06/2016.
 */
public class ProgressNotifier extends ProgressListener {
    private enum MessageType {
        TASK_UPDATE,
        TASK_FINISHED
    }

    private static class Message {
        private MessageType mType;
        private ProgressInfo mProgressInfo;
        private TaskResult mTaskResult;

        public MessageType getType() {
            return mType;
        }

        public void setType(MessageType type) {
            mType = type;
        }

        public ProgressInfo getProgressInfo() {
            return mProgressInfo;
        }

        public void setProgressInfo(ProgressInfo progressInfo) {
            mProgressInfo = progressInfo;
        }

        public TaskResult getTaskResult() {
            return mTaskResult;
        }

        public void setTaskResult(TaskResult taskResult) {
            mTaskResult = taskResult;
        }
    }

    private static final long INTERVAL_MSEC = (long)(1000 / (float)15);
    private HashMap<FileSystemTask, Message> mUpdates = new HashMap<>();
    private Thread mThread;
    private boolean mShouldStop;
    private TaskListener mListener;
    private Handler mUIHandler;

    private Runnable mMainLoop = new Runnable() {
        @Override
        public void run() {
            while (!mShouldStop) {
                long iterStartTime = System.currentTimeMillis();

                dispatchLoop();

                long elapsedTime = System.currentTimeMillis() - iterStartTime;
                while (elapsedTime < INTERVAL_MSEC && !mShouldStop) {
                    try {
                        Thread.sleep(INTERVAL_MSEC - elapsedTime);
                        elapsedTime = System.currentTimeMillis() - iterStartTime;
                    } catch (InterruptedException e) {
                        mShouldStop = true;
                    }
                }
            }
        }
    };

    public ProgressNotifier(@NonNull Handler uiHandler, TaskListener taskListener) {
        mUIHandler = uiHandler;
        mListener = taskListener;
    }

    @Override
    public void onProgressUpdate(FileSystemTask task, ProgressInfo info) {
        notifyTaskUpdate(task, info);
    }

    @Override
    public void onTaskFinished(FileSystemTask task, TaskResult result) {
        notifyTaskFinished(task, result);
    }

    public synchronized void notifyTaskUpdate(FileSystemTask task, ProgressInfo info) {
        Message msg = new Message();
        msg.setType(MessageType.TASK_UPDATE);
        msg.setProgressInfo(info);
        mUpdates.put(task, msg);
    }

    public synchronized void notifyTaskFinished(FileSystemTask task, TaskResult result) {
        Message msg = new Message();
        msg.setType(MessageType.TASK_FINISHED);
        msg.setTaskResult(result);
        mUpdates.put(task, msg);
    }

    public void start() {
        if (mThread == null || !mThread.isAlive()) {
            mThread = new Thread(mMainLoop);
            mThread.start();
        }
    }

    private synchronized void dispatchLoop() {
        HashSet<Map.Entry<FileSystemTask, Message>> safeCopy = new HashSet<>(mUpdates.entrySet());

        for(Map.Entry<FileSystemTask, Message> entry : safeCopy) {
            final FileSystemTask task = entry.getKey();
            final Message msg = entry.getValue();

            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    switch(msg.getType()) {
                        case TASK_UPDATE:
                            mListener.onProgressUpdate(task, msg.getProgressInfo());
                            break;
                        case TASK_FINISHED:
                            mListener.onTaskFinished(task, msg.getTaskResult());
                            break;
                    }
                }
            });

            mUpdates.remove(task);
        }
    }

    public void stop() {
        if (mThread != null && mThread.isAlive()) {
            mThread.interrupt();
            mShouldStop = true;
        }
    }

    public void awaitTermination(long time, TimeUnit unit) throws InterruptedException {
        long requestTime = System.currentTimeMillis();
        long waitTime = time * unit.toMillis(time);
        long elapsedTime = 0;
        do {
            Thread.sleep(waitTime - elapsedTime);
            elapsedTime = System.currentTimeMillis() - requestTime;
        } while(mThread.isAlive() && elapsedTime < waitTime);
    }
}
