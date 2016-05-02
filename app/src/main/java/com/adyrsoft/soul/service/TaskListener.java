package com.adyrsoft.soul.service;

import android.net.Uri;

/**
 * Listener for every client that wants to be updated on the status of FileSystemTasks running
 * in background.
 */
public interface TaskListener {
    void onProgressUpdate(FileSystemTask task, ProgressInfo info);
    void onTaskFinished(FileSystemTask task, TaskResult result);
    void onError(FileSystemTask task, ErrorInfo errorInfo);
}
