package com.adyrsoft.soul.service;

import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import com.adyrsoft.soul.data.Entry;
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

    public LocalFileSystemTask(FileOperation op, Uri srcWD, List<Uri> srcs, Uri dst, TaskListener listener) {
        super(op, srcWD, srcs, dst, listener);
    }

    @Override
    protected void copy(Uri srcWD, List<Uri> srcs, Uri dst) throws InterruptedException {
        LocalFSEntryDuplicator entryDuplicator = new LocalFSEntryDuplicator(getStreamDuplicator(), new StreamDuplicator.OnDuplicationProgressListener() {
            @Override
            public void onDuplicationProgress(int bytesCopied) {
                incrementProcessedBytes(bytesCopied);
                onProgressUpdate();
            }
        });

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
                    entryDuplicator.copyEntry(srcEntry, dstEntry, overwrite);
                    retry = false;
                    error = FileSystemErrorType.NONE;
                } catch (LocalFSEntryDuplicator.FileCopyFailedException e) {
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
    protected void move(Uri srcWD, List<Uri> srcs, Uri dst) throws InterruptedException {
        setTotalFiles(srcs.size());
        for (Uri src : srcs) {
            boolean retry;
            do {
                retry = false;
                File srcFile = new File(src.getPath());
                String relativePath = new File(srcWD.getPath())
                        .toURI()
                        .relativize(srcFile.toURI())
                        .getPath();
                File dstFile = new File(dst.getPath(), relativePath);

                if(!srcFile.renameTo(dstFile)) {
                    FileSystemErrorType errorType;
                    if (!srcFile.getParentFile().canWrite()) {
                        errorType = FileSystemErrorType.SOURCE_NOT_WRITABLE;
                    } else if (dstFile.getParentFile().exists() && !dstFile.getParentFile().canWrite()) {
                        errorType = FileSystemErrorType.DEST_NOT_WRITABLE;
                    } else {
                        errorType = FileSystemErrorType.UNKNOWN;
                    }

                    Solution s = onError(src, Uri.fromFile(dstFile), errorType);
                    switch(errorType) {
                        case SOURCE_NOT_WRITABLE:
                            switch(s.getAction()) {
                                case RETRY_CONTINUE:
                                    retry = true;
                                    srcFile.getParentFile().setWritable(true, false);
                                    break;
                                case IGNORE:
                                    break;
                                case CANCEL:
                                    throw new InterruptedException("User interrupted the task");
                            }
                            break;
                        case DEST_NOT_WRITABLE:
                            switch(s.getAction()) {
                                case RETRY_CONTINUE:
                                    retry = true;
                                    dstFile.getParentFile().setWritable(true, false);
                                    break;
                                case IGNORE:
                                    break;
                                case CANCEL:
                                    throw new InterruptedException("User interrupted the task");
                            }
                            break;
                        default:
                            switch(s.getAction()) {
                                case RETRY_CONTINUE:
                                    retry = true;
                                    break;
                                case IGNORE:
                                    break;
                                case CANCEL:
                                    throw new InterruptedException("User interrupted the task");
                            }
                            break;
                    }
                }
            } while(retry);
            incrementProcessedFiles(1);
        }
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

    @Override
    protected void createFolder(Uri folderUri) throws InterruptedException {
        setTotalFiles(1);
        File newDir = new File(folderUri.getPath());

        if (!newDir.exists() && !newDir.mkdir()) {
            onError(null, folderUri, FileSystemErrorType.DEST_NOT_WRITABLE);
        }

        incrementProcessedFiles(1);
        onProgressUpdate();
    }

    @Override
    protected void query(Uri resource) throws InterruptedException {
        File[] files = new File(resource.getPath()).listFiles();

        ArrayList<Entry> entries = new ArrayList<>();
        if (files != null && files.length > 0) {
            for(File file : files) {
                Entry entry;
                if (file.isDirectory()) {
                    entry = new Entry(Uri.parse(file.toURI().toString()), Entry.EntryType.CONTAINER);
                } else {
                    entry = new Entry(Uri.parse(file.toURI().toString()), Entry.EntryType.FILE);
                }
                entries.add(entry);
            }
        }

        setOutput(entries);
    }
}
