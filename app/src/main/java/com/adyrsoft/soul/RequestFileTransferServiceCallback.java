package com.adyrsoft.soul;

import com.adyrsoft.soul.service.FileTransferService;

/**
 * Created by Adrian on 14/03/2016.
 */
public interface RequestFileTransferServiceCallback {
    void onServiceReady(FileTransferService transferService);
}
