package com.adyrsoft.soul.service;

import java.io.File;
import java.io.IOException;

/**
 * Created by Adrian on 18/03/2016.
 */
public class FileNotReadable extends IOException {
    public FileNotReadable(File file) {
        this(file, "", null);
    }

    public FileNotReadable(File file, String msg) {
        this(file, msg, null);
    }

    public FileNotReadable(File file, String msg, Exception e) {
        super("File at " + file.getPath() + " cannot be open for reading. " + msg, e);
    }
}
