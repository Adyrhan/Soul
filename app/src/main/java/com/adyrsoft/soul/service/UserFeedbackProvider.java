package com.adyrsoft.soul.service;

/**
 * Allows the background running FileSystemTask to wait for a user response on error.
 */

public class UserFeedbackProvider {
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
    }
}
