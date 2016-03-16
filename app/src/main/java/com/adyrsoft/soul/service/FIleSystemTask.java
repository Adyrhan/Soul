package com.adyrsoft.soul.service;

import android.net.Uri;
import android.os.Handler;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by Adrian on 15/03/2016.
 */
public abstract class FileSystemTask implements Runnable {
    private final Handler mUiHandler;
    private FileOperation mOp;
    private List<Uri> mSrcs;
    private Uri mDst;
    private TaskListener mListener;
    private Future mFuture;

    public FileSystemTask(FileOperation op, List<Uri> srcs, Uri dst, TaskListener listener, Handler uiHandler) {
        if (srcs == null) {
            throw new NullPointerException("srcs cannot be null");
        }

        if (op == null) {
            throw new NullPointerException("op cannot be null");
        }

        if ((op == FileOperation.COPY || op == FileOperation.MOVE) && dst == null) {
            throw new NullPointerException("dst cannot be null for copy and move operations");
        }

        if (uiHandler == null) {
            throw new NullPointerException("uiHandler cannot be null");
        }

        mOp = op;
        mSrcs = srcs;
        mDst = dst;
        mListener = listener;
        mUiHandler = uiHandler;
    }

    @Override
    public void run() {
        switch (mOp) {
            case COPY:
                copy(mSrcs, mDst);
                break;
            case MOVE:
                move(mSrcs, mDst);
                break;
            case REMOVE:
                remove(mSrcs);
                break;
        }
    }

    public void setTaskFuture(Future future) {
        if (mFuture == null) {
            mFuture = future;
        }
    }

    public Future getTaskFuture() { return mFuture; }

    protected void onProgressUpdate(final int totalFiles, final int filesProcessed, final int totalBytes, final int bytesProcessed) {
        final FileSystemTask thisTask = this;
        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onProgressUpdate(thisTask, totalFiles, filesProcessed, totalBytes, bytesProcessed);
                }
            }
        });
    }

    protected void onError(final Uri src, final Uri dst, final FileSystemErrorType errorType) {
        final FileSystemTask thisTask = this;
        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onError(thisTask, src, dst, errorType);
                }
            }
        });
    }

    protected abstract void copy(List<Uri> srcs, Uri dst);
    protected abstract void move(List<Uri> srcs, Uri dst);
    protected abstract void remove(List<Uri> srcs);
}
