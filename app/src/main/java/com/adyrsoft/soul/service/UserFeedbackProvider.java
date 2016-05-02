package com.adyrsoft.soul.service;

import android.support.annotation.NonNull;

/**
 * Allows the background running FileSystemTask to wait for a user response on error.
 */

public class UserFeedbackProvider {
    private OnFeedbackProvided mCallback;

    public interface OnFeedbackProvided {
        void onFeedbackProvided();
    }
    private Solution mFeedback;

    public Solution fetchFeedback() throws InterruptedException {
        synchronized (this) {
            while(mFeedback == null) {
                wait();
            }

            Solution feedback = mFeedback;
            mFeedback = null;
            return feedback;
        }
    }

    public void provideFeedback(Solution feedback) {
        synchronized (this) {
            mFeedback = feedback;
            notify();
        }
        if (mCallback != null) {
            mCallback.onFeedbackProvided();
        }
    }

    public void setCallback(OnFeedbackProvided callback) {
        mCallback = callback;
    }
}
