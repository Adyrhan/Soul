package com.adyrsoft.soul;

import com.adyrsoft.soul.utils.StreamDuplicationFailedException;
import com.adyrsoft.soul.utils.StreamDuplicator;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Adrian on 07/03/2016.
 */
public class StreamDuplicatorTest {
    private static final int DATA_SIZE = 4096;

    @Test
    public void inputDataIsSameAsOutput() throws StreamDuplicationFailedException {
        byte[] inputData = new byte[DATA_SIZE];

        for(int i = 0; i < DATA_SIZE; i++) {
            inputData[i] = (byte)(Math.random() * 255);
        }

        ByteArrayInputStream is = new ByteArrayInputStream(inputData);
        ByteArrayOutputStream os = new ByteArrayOutputStream(DATA_SIZE);
        StreamDuplicator duplicator = new StreamDuplicator();

        duplicator.duplicate(is, os);

        Assert.assertArrayEquals("Array bytes not equal", inputData, os.toByteArray());
    }

    @Test
    public void notifiesOfCopiedData() throws StreamDuplicationFailedException {
        byte[] inputData = new byte[DATA_SIZE];

        for(int i = 0; i < DATA_SIZE; i++) {
            inputData[i] = (byte)(Math.random() * 255);
        }

        ByteArrayInputStream is = new ByteArrayInputStream(inputData);
        ByteArrayOutputStream os = new ByteArrayOutputStream(DATA_SIZE);
        StreamDuplicator duplicator = new StreamDuplicator();

        final AtomicInteger totalCopied = new AtomicInteger();
        StreamDuplicator.OnDuplicationProgressListener listener = new StreamDuplicator.OnDuplicationProgressListener() {
            @Override
            public void onDuplicationProgress(int bytesCopied) {
                totalCopied.addAndGet(bytesCopied);
            }
        };

        duplicator.duplicate(is, os, listener);

        Assert.assertEquals("Copied data length doesn't match DATA_SIZE", DATA_SIZE, totalCopied.get());

        Assert.assertArrayEquals("Array bytes not equal", inputData, os.toByteArray());
    }


}


