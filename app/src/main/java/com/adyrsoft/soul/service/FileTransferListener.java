package com.adyrsoft.soul.service;

import java.util.HashMap;

/**
 * Created by Adrian on 04/04/2016.
 */
public interface FileTransferListener extends TaskListener {
    void onSubscription(HashMap<FileSystemTask, ProgressInfo> pendingTasks);
}
