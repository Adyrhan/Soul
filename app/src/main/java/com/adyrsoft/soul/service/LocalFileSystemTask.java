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
import java.util.Collections;
import java.util.List;

/**
 * Subclass of FileSystemTask that represents a file system task on the local file system
 */
public class LocalFileSystemTask extends FileSystemTask {
    public static final String TAG = LocalFileSystemTask.class.getName();

    public LocalFileSystemTask(FileOperation op, Uri srcWD, List<Uri> srcs, Uri dst, TaskListener listener, Handler uiHandler) {
        super(op, srcWD, srcs, dst, listener, uiHandler);
    }

    @Override
    protected void copy(Uri srcWD, List<Uri> srcs, Uri dst) throws InterruptedException {
        List<Uri> expandedSrcs = expandFileList(srcs);

        setTotalFiles(expandedSrcs.size());

        for(Uri entry : expandedSrcs) {
            File srcEntry = new File(entry.getPath());
            String relativePath = new File(srcWD.getPath())
                    .toURI()
                    .relativize(srcEntry.toURI())
                    .getPath();

            File dstEntry = new File(dst.getPath(), relativePath);

            setSource(entry);
            setDest(Uri.fromFile(dstEntry));

            boolean retry = false;
            boolean overwrite = false;

            Solution errorSolution = null;
            FileSystemErrorType error = FileSystemErrorType.NONE;

            do {
                try {
                    copyEntry(srcEntry, dstEntry, overwrite);
                } catch (FileCopyFailedException e) {
                    Throwable cause = e.getCause();

                    if (cause instanceof FileNotReadable) {
                        error = FileSystemErrorType.SOURCE_NOT_READABLE;
                        errorSolution = onError(entry, Uri.fromFile(dstEntry), error);
                    } else if (cause instanceof FileNotWritable) {
                        error = FileSystemErrorType.DEST_NOT_WRITABLE;
                        errorSolution = onError(entry, Uri.fromFile(dstEntry), error);
                    } else if (cause instanceof StreamDuplicationFailedException) {
                        Throwable subCause = e.getCause();

                        if (subCause instanceof StreamReadFailureException) {
                            error = FileSystemErrorType.READ_ERROR;
                            errorSolution = onError(entry, Uri.fromFile(dstEntry), error);
                        } else {
                            error = FileSystemErrorType.WRITE_ERROR;
                            errorSolution = onError(entry, Uri.fromFile(dstEntry), error);
                        }
                    } else if (cause instanceof FileNotFoundException) {
                        error = FileSystemErrorType.SOURCE_DOESNT_EXIST;
                        errorSolution = onError(entry, Uri.fromFile(dstEntry), error);
                    } else if (cause instanceof FileAlreadyExists) {
                        error = FileSystemErrorType.DEST_ALREADY_EXISTS;
                        errorSolution = onError(entry, Uri.fromFile(dstEntry), error);
                    }
                }

                switch (error) {
                    case NONE:
                        break;
                    case DEST_ALREADY_EXISTS:
                        switch(errorSolution.getAction()) {
                            case RETRY_CONTINUE:
                                retry = true;
                                overwrite = true;
                                break;
                            case IGNORE:
                                break;
                            case CANCEL:
                                throw new InterruptedException("User cancelled the task");
                        }
                        break;
                    case SOURCE_DOESNT_EXIST:
                        switch(errorSolution.getAction()) {
                            case RETRY_CONTINUE:
                                retry = true;
                                break;
                            case IGNORE:
                                break;
                            case CANCEL:
                                throw new InterruptedException("User cancelled the task");
                        }
                        break;
                    case SOURCE_NOT_READABLE:
                        switch(errorSolution.getAction()) {
                            case RETRY_CONTINUE:
                                retry = true;
                                break;
                            case IGNORE:
                                break;
                            case CANCEL:
                                throw new InterruptedException("User cancelled the task");
                        }
                        break;
                    case DEST_NOT_WRITABLE:
                        switch(errorSolution.getAction()) {
                            case RETRY_CONTINUE:
                                retry = true;
                                break;
                            case IGNORE:
                                break;
                            case CANCEL:
                                throw new InterruptedException("User cancelled the task");
                        }
                        break;
                    case READ_ERROR:
                        throw new InterruptedException("Task isn't processable due to read error");
                    case WRITE_ERROR:
                        throw new InterruptedException("Task isn't processable due to read error");
                    case UNKNOWN:
                        throw new InterruptedException("Task isn't processable due to unknown error");
                    case AUTHENTICATION_ERROR:
                        break;
                }
            } while (retry);

            incrementProcessedFiles(1);
            onProgressUpdate();
        }
    }

    private void copyEntry(File srcEntry, File dstEntry, boolean overwrite) throws FileCopyFailedException, InterruptedException {
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

            StreamDuplicator duplicator = getStreamDuplicator();

            duplicator.duplicate(srcStream, dstStream, new StreamDuplicator.OnDuplicationProgressListener() {
                @Override
                public void onDuplicationProgress(int bytesCopied) {
                    incrementProcessedBytes(bytesCopied);
                    onProgressUpdate();
                }
            });
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

    private List<Uri> expandFileList(List<Uri> srcs) throws InterruptedException {
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
    protected void remove(Uri srcWD, List<Uri> srcs) throws InterruptedException {
        List<Uri> expanded = expandFileList(srcs);
        Collections.reverse(expanded);

        setTotalFiles(expanded.size());

        for (Uri entry : expanded) {
            File fileEntry = new File(entry.getPath());

            setSource(entry);

            if (!fileEntry.delete()) {
                if (!fileEntry.exists()) {
                    onError(entry, null, FileSystemErrorType.SOURCE_DOESNT_EXIST);
                } else {
                    onError(entry, null, FileSystemErrorType.UNKNOWN);
                }
            }

            incrementProcessedFiles(1);
            onProgressUpdate();
        }
    }

    private class FileCopyFailedException extends Exception {
        public FileCopyFailedException(Exception e) {
            super(e);
        }

        public FileCopyFailedException() {}
    }

    private class DirectoryCreationFailedException extends Exception {
        public DirectoryCreationFailedException(Exception e) {
            super(e);
        }

        public DirectoryCreationFailedException() {}
    }
}
