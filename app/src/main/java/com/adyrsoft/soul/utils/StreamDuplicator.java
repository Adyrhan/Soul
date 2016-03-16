package com.adyrsoft.soul.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Adrian on 08/03/2016.
 */
public class StreamDuplicator {
    // Used to notify copy progress to caller
    public interface OnDuplicationProgressListener {
        void onDuplicationProgress(int bytesCopied);
    }

    // Default buffer size
    public static final int BUFFER_SIZE = 512;

    public void duplicate(InputStream is, OutputStream os) throws IOException {
        duplicate(is, os, BUFFER_SIZE, null);
    }

    public void duplicate(InputStream is, OutputStream os, int bufferSize) throws IOException {
        duplicate(is, os , bufferSize, null);
    }

    public void duplicate(InputStream is, OutputStream os, OnDuplicationProgressListener listener) throws IOException {
        duplicate(is, os, BUFFER_SIZE, listener);
    }

    public void duplicate(InputStream is, OutputStream os, int bufferSize, OnDuplicationProgressListener listener) throws IOException {
        if (bufferSize < 1) {
            throw new IllegalArgumentException("bufferSize cannot be smaller than 1");
        }

        byte[] buffer = new byte[bufferSize];
        int bytesRead;

        while((bytesRead = is.read(buffer, 0, bufferSize)) != -1) {
            os.write(buffer, 0, bytesRead);

            if (listener != null) {
                listener.onDuplicationProgress(bytesRead);
            }
        }
    }
}
