package com.adyrsoft.soul.service;

import java.io.File;
import java.io.IOException;

/**
 * Created by Adrian on 18/03/2016.
 */
public class FileNotWritable extends IOException {
    public FileNotWritable(File file) {
        this(file, "", null);
    }

    public FileNotWritable(File file, String msg) {
        this(file, msg, null);
    }

    public FileNotWritable(File file, String msg, Exception e) {
        super("File at " + file.getPath() + " cannot be open for writing. " + msg, e);
    }
}
