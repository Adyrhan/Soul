package com.adyrsoft.soul.service;

/**
 * Error types related to the running of a FileSystemTask. Used to report to clients through
 * a TaskListener
 */
public enum FileSystemErrorType {
    NONE,
    DEST_ALREADY_EXISTS,
    SOURCE_DOESNT_EXIST,
    SOURCE_NOT_READABLE,
    DEST_NOT_WRITABLE,
    READ_ERROR,
    WRITE_ERROR,
    UNKNOWN,
    AUTHENTICATION_ERROR
}
