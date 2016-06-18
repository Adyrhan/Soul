package com.adyrsoft.soul.service;

import android.net.Uri;

/**
 * Holds information about the progress of a FileSystemTask
 */
public class ProgressInfo {
    private int mTotalBytes;
    private int mProcessedFiles;
    private int mProcessedBytes;
    private int mTotalFiles;
    private Uri mDest;
    private Uri mSource;

    public int getTotalBytes() {
        return mTotalBytes;
    }

    public int getProcessedFiles() {
        return mProcessedFiles;
    }

    public int getProcessedBytes() {
        return mProcessedBytes;
    }

    public int getTotalFiles() {
        return mTotalFiles;
    }

    public Uri getDest() {
        return mDest;
    }

    public Uri getSource() {
        return mSource;
    }


    public ProgressInfo(ProgressInfo progressInfo) {
        mSource = progressInfo.mSource;
        mDest = progressInfo.mDest;
        mProcessedBytes = progressInfo.mProcessedBytes;
        mProcessedFiles = progressInfo.mProcessedFiles;
        mTotalBytes = progressInfo.mTotalBytes;
        mTotalFiles = progressInfo.mTotalFiles;
    }

    public ProgressInfo(Builder builder) {
        mSource = builder.getSource();
        mDest = builder.getDest();
        mTotalFiles = builder.getTotalFiles();
        mTotalBytes = builder.getTotalBytes();
        mProcessedFiles = builder.getProcessedFiles();
        mProcessedBytes = builder.getProcessedBytes();
    }

    public static class Builder {
        private Uri mSource;
        private Uri mDest;
        private int mTotalFiles;
        private int mProcessedFiles;
        private int mTotalBytes;
        private int mProcessedBytes;

        public Builder() { }

        public Builder(Builder builder) {
            mSource = builder.mSource;
            mDest = builder.mDest;
            mTotalBytes = builder.mTotalBytes;
            mTotalFiles = builder.mTotalFiles;
            mProcessedBytes = builder.mProcessedBytes;
            mProcessedFiles = builder.mProcessedFiles;
        }

        public Builder setSource(Uri source) {
            mSource = source;
            return this;
        }

        public Uri getSource() { return mSource; }

        public Uri getDest() {
            return mDest;
        }

        public Builder setDest(Uri dest) {
            mDest = dest;
            return this;
        }

        public int getTotalFiles() {
            return mTotalFiles;
        }

        public Builder setTotalFiles(int totalFiles) {
            mTotalFiles = totalFiles;
            return this;
        }

        public int getProcessedFiles() {
            return mProcessedFiles;
        }

        public Builder setProcessedFiles(int processedFiles) {
            mProcessedFiles = processedFiles;
            return this;
        }

        public int getTotalBytes() {
            return mTotalBytes;
        }

        public Builder setTotalBytes(int totalBytes) {
            mTotalBytes = totalBytes;
            return this;
        }

        public int getProcessedBytes() {
            return mProcessedBytes;
        }

        public Builder setProcessedBytes(int processedBytes) {
            mProcessedBytes = processedBytes;
            return this;
        }

        public ProgressInfo create() {
            return new ProgressInfo(this);
        }
    }
}
