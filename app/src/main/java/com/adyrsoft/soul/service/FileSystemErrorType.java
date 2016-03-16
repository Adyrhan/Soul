package com.adyrsoft.soul.service;

/**
 * Created by Adrian on 15/03/2016.
 */
public enum FileSystemErrorType {
    DEST_ALREADY_EXISTS,
    FOLDER_ALREADY_EXISTS,
    SOURCE_DOESNT_EXIST,
    SOURCE_NOT_READABLE,
    DEST_NOT_WRITABLE,
    READ_ERROR,
    WRITE_ERROR,
    AUTHENTICATION_ERROR
}
