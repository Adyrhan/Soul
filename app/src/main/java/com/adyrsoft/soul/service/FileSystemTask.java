package com.adyrsoft.soul.service;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import com.adyrsoft.soul.utils.StreamDuplicator;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Object that represents a batch of file operations of the same kind.
 * Implementations of this class also implement the file operations using the
 * appropriate protocol or local file system, which will be run by the FileTransferService.
 */
public abstract class FileSystemTask implements Runnable {
    public enum State {
        PENDING,
        WORKING,
        FINISHED
    }

    private FileOperation mOp;
    private Uri mSrcWD;
    private List<Uri> mSrcs;
    private Uri mDst;
    private TaskListener mListener;
    private Future mFuture;
    private StreamDuplicator mStreamDuplicator;
    private int mTotalFiles;
    private int mProcessedFiles;
    private int mProcessedBytes;
    private int mTotalBytes;
    private Uri mSource;
    private Uri mDest;
    private TaskResult mTaskResult;
    private State mState;

    public FileSystemTask(FileOperation op, Uri srcWD, List<Uri> srcs, Uri dst, TaskListener listener) {
        init(op, srcWD, srcs, dst, listener, null);
    }

    FileSystemTask(FileOperation op, Uri srcWD, List<Uri> srcs, Uri dst, TaskListener listener, StreamDuplicator duplicator) {
        init(op, srcWD, srcs, dst, listener, duplicator);
    }

    private void init(FileOperation op, Uri srcWD, List<Uri> srcs, Uri dst, TaskListener listener, StreamDuplicator duplicator) {
        if (srcs == null) {
            throw new NullPointerException("srcs cannot be null");
        }

        if (op == null) {
            throw new NullPointerException("op cannot be null");
        }

        if ((op == FileOperation.COPY || op == FileOperation.MOVE) && dst == null) {
            throw new NullPointerException("dst cannot be null for copy and move operations");
        }

        if (srcWD == null) {
            throw new NullPointerException("srcWD cannot be null");
        }

        if (duplicator == null) {
            duplicator = new StreamDuplicator();
        }

        mOp = op;
        mSrcWD = srcWD;
        mSrcs = srcs;
        mDst = dst;
        mListener = listener;
        mStreamDuplicator = duplicator;
        mState = State.PENDING;
    }

    public StreamDuplicator getStreamDuplicator() {
        return mStreamDuplicator;
    }

    @Override
    public void run() {
        try {
            mState = State.WORKING;
            switch (mOp) {
                case COPY:
                    copy(mSrcWD, mSrcs, mDst);
                    break;
                case MOVE:
                    move(mSrcWD, mSrcs, mDst);
                    break;
                case REMOVE:
                    remove(mSrcWD, mSrcs);
                    break;
            }

            onTaskFinished();
        } catch (InterruptedException e) {
            mTaskResult = TaskResult.CANCELED;
            onTaskFinished();
        }
    }

    public void setTaskFuture(Future future) {
        if (mFuture == null) {
            mFuture = future;
        }
    }

    public Future getTaskFuture() { return mFuture; }

    public FileOperation getFileOperation() { return mOp; }

    public State getState() { return mState; }

    protected void onProgressUpdate() {
        final ProgressInfo info = new ProgressInfo.Builder()
                .setSource(getSource())
                .setDest(getDest())
                .setProcessedBytes(getProcessedBytes())
                .setProcessedFiles(getProcessedFiles())
                .setTotalFiles(getTotalFiles())
                .setTotalBytes(getTotalBytes())
                .create();

        final FileSystemTask thisTask = this;
        if (mListener != null) {
            mListener.onProgressUpdate(thisTask, info);
        }
    }

    protected void onTaskFinished() {
        mState = State.FINISHED;

        if (mListener != null) {
            mListener.onTaskFinished(this, mTaskResult);
        }
    }

    protected Solution onError(final Uri src, final Uri dst, final FileSystemErrorType errorType) throws InterruptedException {
        final FileSystemTask thisTask = this;
        final UserFeedbackProvider feedbackProvider = new UserFeedbackProvider();
        final ErrorInfo errorInfo = new ErrorInfo.Builder()
                .setErrorType(errorType)
                .setTask(this)
                .setSourceUri(src)
                .setDestinyUri(dst)
                .setFeedbackProvider(feedbackProvider)
                .create();

        if (mListener != null) {
            mListener.onError(thisTask, errorInfo);
        }
        return feedbackProvider.fetchFeedback();
    }

    protected abstract void copy(Uri srcWD, List<Uri> srcs, Uri dst) throws InterruptedException;
    protected abstract void move(Uri srcWD, List<Uri> srcs, Uri dst) throws InterruptedException;
    protected abstract void remove(Uri srcWD, List<Uri> srcs) throws InterruptedException;

    protected int getTotalFiles() { return mTotalFiles; }

    protected void setTotalFiles(int totalFiles) { mTotalFiles = totalFiles; }

    protected int getProcessedFiles() {
        return mProcessedFiles;
    }

    protected void setProcessedFiles(int processedFiles) {
        mProcessedFiles = processedFiles;
    }

    protected void incrementProcessedBytes(int processedBytes) {
        mProcessedBytes += processedBytes;
    }

    protected void incrementProcessedFiles(int processedFiles) {
        mProcessedFiles += processedFiles;
    }

    protected int getProcessedBytes() { return mProcessedBytes; }

    protected void setProcessedBytes(int processedBytes) {
        mProcessedBytes = processedBytes;
    }

    protected int getTotalBytes() {
        return mTotalBytes;
    }

    protected void setTotalBytes(int totalBytes) {
        mTotalBytes = totalBytes;
    }

    protected Uri getSource() { return mSource; }

    protected void setSource(Uri source) { mSource = source; }

    protected Uri getDest() { return mDest; }

    protected void setDest(Uri dest) { mDest = dest; }
}
