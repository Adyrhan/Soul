package com.adyrsoft.soul.service;

import android.os.Bundle;

/**
 * Represents the feedback of an user to a given file operation error
 */
public final class Solution {
    public enum Action {
        // Retries or resumes the processing of a task entry. Would tell the service to
        // override in case of an already existing destiny file
        RETRY_CONTINUE,

        // Ignores the entry, and resumes the processing of the task.
        IGNORE,

        // Stops the processing of the whole task.
        CANCEL
    }

    private Action mAction;
    private Bundle mOptions;

    public void setAction(Action action) { mAction = action; }
    public Action getAction() { return mAction; }

    public void setOptions(Bundle options) { mOptions = options; }
    public Bundle getOptions() { return mOptions; }
}
