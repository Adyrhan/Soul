package com.adyrsoft.soul.service;

/**
 * More specific abstract implementation of TaskListener, for those
 * objects who don't handle errors (ProgressNotifier)
 */
public abstract class ProgressListener implements TaskListener {
    @Override
    public void onError(FileSystemTask task, ErrorInfo errorInfo) {
        // do nothing
    }
}
