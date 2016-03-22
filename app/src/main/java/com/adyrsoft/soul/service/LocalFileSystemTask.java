package com.adyrsoft.soul.service;

import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import com.adyrsoft.soul.utils.FileUtils;
import com.adyrsoft.soul.utils.StreamDuplicationFailedException;
import com.adyrsoft.soul.utils.StreamDuplicator;
import com.adyrsoft.soul.utils.StreamReadFailureException;
import com.adyrsoft.soul.utils.StreamWriteFailureException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Adrian on 15/03/2016.
 */
public class LocalFileSystemTask extends FileSystemTask {
    public static final String TAG = LocalFileSystemTask.class.getName();

    public LocalFileSystemTask(FileOperation op, Uri srcWD, List<Uri> srcs, Uri dst, TaskListener listener, Handler uiHandler) {
        super(op, srcWD, srcs, dst, listener, uiHandler);
    }

    @Override
    protected void copy(Uri srcWD, List<Uri> srcs, Uri dst) {
        List<Uri> expandedSrcs = expandFileList(srcs);

        setTotalFiles(expandedSrcs.size());

        for(Uri entry : expandedSrcs) {
            File srcEntry = new File(entry.getPath());
            String relativePath = new File(srcWD.getPath())
                    .toURI()
                    .relativize(srcEntry.toURI())
                    .getPath();

            File dstEntry = new File(dst.getPath(), relativePath);

            if (srcEntry.isDirectory()) {
                if (!dstEntry.exists()) {
                    if (!dstEntry.mkdirs()) {
                        onError(entry, Uri.fromFile(dstEntry), FileSystemErrorType.DEST_NOT_WRITABLE);
                    }
                }
            } else {
                File parentFolder = dstEntry.getParentFile();
                if (!parentFolder.exists()) {
                    if(!parentFolder.mkdirs()) {
                        onError(entry, Uri.fromFile(parentFolder), FileSystemErrorType.DEST_NOT_WRITABLE);
                    }
                }
                copyFile(srcEntry, dstEntry);
            }
            incrementProcessedFiles(1);
            onProgressUpdate(getTotalFiles(), getProcessedFiles(), getTotalBytes(), getProcessedBytes());
        }
    }

    private void copyFile(File srcEntry, File dstEntry) {
        FileInputStream srcStream = null;
        FileOutputStream dstStream = null;
        Uri srcUri = Uri.fromFile(srcEntry);
        Uri dstUri = Uri.fromFile(dstEntry);

        try {
            srcStream = OpenFileInputStream(srcEntry);
            dstStream = OpenFileOutputStream(dstEntry);

            dstEntry.getParentFile().mkdirs();

            StreamDuplicator duplicator = getStreamDuplicator();

            duplicator.duplicate(srcStream, dstStream, new StreamDuplicator.OnDuplicationProgressListener() {
                @Override
                public void onDuplicationProgress(int bytesCopied) {
                    incrementProcessedBytes(bytesCopied);
                    onProgressUpdate(getTotalFiles(), getProcessedFiles(), getTotalBytes(), getProcessedBytes());
                }
            });
        } catch (FileNotWritable e) {
            onError(srcUri, dstUri, FileSystemErrorType.DEST_NOT_WRITABLE);
            Log.e(TAG, "Error opening file for writing", e);
        } catch (FileNotReadable e) {
            onError(srcUri, dstUri, FileSystemErrorType.SOURCE_NOT_READABLE);
            Log.e(TAG, "Error opening file for reading", e);
        } catch (StreamDuplicationFailedException e) {
            Throwable cause = e.getCause();

            if (cause == null) {
                onError(srcUri, dstUri, FileSystemErrorType.UNKNOWN);
            } else if (cause instanceof StreamReadFailureException) {
                onError(srcUri, dstUri, FileSystemErrorType.READ_ERROR);
            } else if (cause instanceof StreamWriteFailureException) {
                onError(srcUri, dstUri, FileSystemErrorType.WRITE_ERROR);
            }
            Log.e(TAG, "Error copying file", e);
        } finally {
            FileUtils.closeSilently(srcStream);
            FileUtils.closeSilently(dstStream);
        }
    }

    @NonNull
    private FileOutputStream OpenFileOutputStream(File dstEntry) throws FileNotWritable {
        try {
            return new FileOutputStream(dstEntry);
        } catch (FileNotFoundException e) {
            throw new FileNotWritable(dstEntry);
        }
    }

    @NonNull
    private FileInputStream OpenFileInputStream(File srcEntry) throws FileNotReadable {
        try {
            return new FileInputStream(srcEntry);
        } catch (FileNotFoundException e) {
            throw new FileNotReadable(srcEntry);
        }
    }

    private List<Uri> expandFileList(List<Uri> srcs) {
        List<Uri> expanded = new ArrayList<>();
        List<Uri> unexpanded = new ArrayList<>();

        unexpanded.addAll(srcs);

        for(int i = 0; i < unexpanded.size(); i++) {
            Uri entry = unexpanded.get(i);
            try {
                File fileEntry = new File(entry.getPath());

                if (!FileUtils.isSymlink(fileEntry)) {
                    expanded.add(entry);

                    if (fileEntry.isDirectory()) {
                        File[] entryArray = fileEntry.listFiles();

                        if (entryArray.length != 0) {
                            List<File> dirEntries = Arrays.asList(entryArray);

                            for(File childEntry : dirEntries) {
                                unexpanded.add(Uri.fromFile(childEntry));
                            }
                        }
                    }
                }
            } catch (IOException e){
                onError(entry, null, FileSystemErrorType.SOURCE_NOT_READABLE);
            }
        }
        return expanded;
    }

    @Override
    protected void move(Uri srcWD, List<Uri> srcs, Uri dst) {

    }

    @Override
    protected void remove(Uri srcWD, List<Uri> srcs) {

    }
}
