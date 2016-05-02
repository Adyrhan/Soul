package com.adyrsoft.soul.service;

import android.net.Uri;

/**
 * Information about an error while running a FileSystemTask
 */
public class ErrorInfo {
    private FileSystemTask mTask;
    private FileSystemErrorType mErrorType;
    private Uri mSourceUri;
    private Uri mDestinyUri;
    private UserFeedbackProvider mFeedbackProvider;

    public ErrorInfo(Builder builder) {
        mTask = builder.getTask();
        mErrorType = builder.getErrorType();
        mSourceUri = builder.getSourceUri();
        mDestinyUri = builder.getDestinyUri();
        mFeedbackProvider = builder.getFeedbackProvider();
    }

    public FileSystemTask getTask() {
        return mTask;
    }

    public FileSystemErrorType getErrorType() {
        return mErrorType;
    }

    public Uri getSourceUri() {
        return mSourceUri;
    }

    public Uri getDestinyUri() {
        return mDestinyUri;
    }

    public UserFeedbackProvider getFeedbackProvider() {
        return mFeedbackProvider;
    }

    public static class Builder {
        private FileSystemTask mTask;
        private FileSystemErrorType mErrorType;
        private Uri mSourceUri;
        private Uri mDestinyUri;
        private UserFeedbackProvider mFeedbackProvider;

        public FileSystemTask getTask() {
            return mTask;
        }

        public FileSystemErrorType getErrorType() {
            return mErrorType;
        }

        public Uri getSourceUri() {
            return mSourceUri;
        }

        public Uri getDestinyUri() {
            return mDestinyUri;
        }

        public UserFeedbackProvider getFeedbackProvider() {
            return mFeedbackProvider;
        }

        public Builder setTask(FileSystemTask task) {
            mTask = task;
            return this;
        }

        public Builder setErrorType(FileSystemErrorType errorType) {
            mErrorType = errorType;
            return this;
        }

        public Builder setSourceUri(Uri sourceUri) {
            mSourceUri = sourceUri;
            return this;
        }

        public Builder setDestinyUri(Uri destinyUri) {
            mDestinyUri = destinyUri;
            return this;
        }

        public Builder setFeedbackProvider(UserFeedbackProvider feedbackProvider) {
            mFeedbackProvider = feedbackProvider;
            return this;
        }

        public ErrorInfo create() {
            return new ErrorInfo(this);
        }
    }
}
