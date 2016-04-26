package com.adyrsoft.soul.service;

import java.io.File;

/**
 * Used by LocalFileSystemTask to report to the error handler the existence of the file to be written to.
 */
public class FileAlreadyExists extends Exception {
    public FileAlreadyExists(File dstEntry) {
        super("File " + dstEntry.getPath() + " already exists");
    }
}
