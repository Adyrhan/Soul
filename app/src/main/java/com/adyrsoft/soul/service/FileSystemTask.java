package com.adyrsoft.soul.service;

import android.net.Uri;
import android.os.Handler;

import com.adyrsoft.soul.utils.StreamDuplicator;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by Adrian on 15/03/2016.
 */
public abstract class FileSystemTask implements Runnable {
    private Handler mUiHandler;
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

    public FileSystemTask(FileOperation op, Uri srcWD, List<Uri> srcs, Uri dst, TaskListener listener, Handler uiHandler) {
        init(op, srcWD, srcs, dst, listener, uiHandler, null);
    }

    FileSystemTask(FileOperation op, Uri srcWD, List<Uri> srcs, Uri dst, TaskListener listener, Handler uiHandler, StreamDuplicator duplicator) {
        init(op, srcWD, srcs, dst, listener, uiHandler, duplicator);
    }

    private void init(FileOperation op, Uri srcWD, List<Uri> srcs, Uri dst, TaskListener listener, Handler uiHandler, StreamDuplicator duplicator) {
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
        mUiHandler = uiHandler;
        mStreamDuplicator = duplicator;
    }

    public StreamDuplicator getStreamDuplicator() {
        return mStreamDuplicator;
    }

    @Override
    public void run() {
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

    protected abstract void copy(Uri srcWD, List<Uri> srcs, Uri dst);
    protected abstract void move(Uri srcWD, List<Uri> srcs, Uri dst);
    protected abstract void remove(Uri srcWD, List<Uri> srcs);

    protected int getTotalFiles() {
        return mTotalFiles;
    }

    protected void setTotalFiles(int totalFiles) {
        mTotalFiles = totalFiles;
    }

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

    protected int getProcessedBytes() {
        return mProcessedBytes;
    }


    protected void setProcessedBytes(int processedBytes) {
        mProcessedBytes = processedBytes;
    }

    protected int getTotalBytes() {
        return mTotalBytes;
    }

    protected void setTotalBytes(int totalBytes) {
        mTotalBytes = totalBytes;
    }
}
