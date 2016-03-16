package com.adyrsoft.soul.service;

import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import java.util.List;

/**
 * Created by Adrian on 15/03/2016.
 */
public class LocalFileSystemTask extends FileSystemTask {
    public static final String TAG = LocalFileSystemTask.class.getName();

    public LocalFileSystemTask(FileOperation op, List<Uri> srcs, Uri dst, TaskListener listener, Handler uiHandler) {
        super(op, srcs, dst, listener, uiHandler);
    }

    @Override
    protected void copy(List<Uri> srcs, Uri dst) {
        int processedFiles = 0;
        for(Uri src : srcs) {
            try {
                Thread.sleep(1000);
                processedFiles++;
                onProgressUpdate(srcs.size(), processedFiles, 0, 0);
            } catch (InterruptedException e) {
                Log.e(TAG, "Copy interrumpted by another thread");
            }
        }
    }

    @Override
    protected void move(List<Uri> srcs, Uri dst) {

    }

    @Override
    protected void remove(List<Uri> srcs) {

    }
}
