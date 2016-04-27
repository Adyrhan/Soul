package com.adyrsoft.soul.service;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.adyrsoft.soul.utils.FileUtils;
import com.adyrsoft.soul.utils.StreamDuplicationFailedException;
import com.adyrsoft.soul.utils.StreamDuplicator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Duplicates local file system entries
 */
public class LocalFSEntryDuplicator {
    private StreamDuplicator mStreamDuplicator;
    private StreamDuplicator.OnDuplicationProgressListener mListener;

    public LocalFSEntryDuplicator (StreamDuplicator streamDuplicator, StreamDuplicator.OnDuplicationProgressListener listener) {
        mStreamDuplicator = streamDuplicator;
        mListener = listener;
    }

    public void copyEntry(File srcEntry, File dstEntry, boolean overwrite) throws FileCopyFailedException, InterruptedException {
        if (srcEntry.isDirectory()) {
            copyDirectory(dstEntry);
        } else {
            File parentFolder = dstEntry.getParentFile();

            copyDirectory(parentFolder);
            copyFile(srcEntry, dstEntry, overwrite);
        }
    }

    private void copyDirectory(File dstEntry) throws FileCopyFailedException {
        if (!dstEntry.exists()) {
            if (!dstEntry.mkdirs()) {
                throw new FileCopyFailedException(new FileNotReadable(dstEntry));
            }
        }
    }

    private void copyFile(File srcEntry, File dstEntry, boolean overwrite) throws InterruptedException, FileCopyFailedException {
        FileInputStream srcStream = null;
        FileOutputStream dstStream = null;
        Uri srcUri = Uri.fromFile(srcEntry);
        Uri dstUri = Uri.fromFile(dstEntry);

        try {
            if (!srcEntry.exists()) {
                throw new FileCopyFailedException(new FileNotFoundException("Couldn't find file "+srcEntry.getPath()));
            }

            if (dstEntry.exists() && !overwrite) {
                throw new FileCopyFailedException(new FileAlreadyExists(dstEntry));
            }

            srcStream = OpenFileInputStream(srcEntry);
            dstStream = OpenFileOutputStream(dstEntry);

            mStreamDuplicator.duplicate(srcStream, dstStream, mListener);
        } catch (StreamDuplicationFailedException | FileNotReadable | FileNotWritable e) {
            throw new FileCopyFailedException(e);
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

    public static class FileCopyFailedException extends Exception {
        public FileCopyFailedException(Exception e) {
            super(e);
        }

        public FileCopyFailedException() {}
    }
}
