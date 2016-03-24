package com.adyrsoft.soul.service;

import android.net.Uri;

/**
 * Created by Adrian on 15/03/2016.
 */
public interface TaskListener {
    void onProgressUpdate(FileSystemTask task, ProgressInfo info);
    void onError(FileSystemTask task, Uri srcFile, Uri dstFile, FileSystemErrorType errorType);
}
