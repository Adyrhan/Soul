package com.adyrsoft.soul;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ServiceTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.adyrsoft.soul.service.FileSystemTask;
import com.adyrsoft.soul.service.FileTransferService;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

/**
 * Created by Adrian on 01/11/2016.
 */

@RunWith(AndroidJUnit4.class)
@MediumTest
public class LocalFileSystemCopyTests {
    private static final String TAG = LocalFileSystemCopyTests.class.getSimpleName();
    private Context mTestContext;
    private static File sTestRoot;
    private static File sTestDir;
    private Context mAppContext;
    private FileTransferService mService;
    private static File sCopyDir;

    @Rule
    public final ServiceTestRule mServiceTestRule = new ServiceTestRule();

    @BeforeClass
    public static void beforeAll() throws IOException {
        File externalDir = Environment.getExternalStorageDirectory();
        sTestRoot = new File(externalDir, "testRoot");
        Assert.assertTrue(sTestRoot.mkdirs());
        sTestDir = generateTestDirectoryStructure(sTestRoot);
        sCopyDir = new File(sTestRoot, "copydir");
        Assert.assertTrue(sCopyDir.mkdirs());
    }

    @Before
    public void beforeEach() {
        mTestContext = InstrumentationRegistry.getContext();
        mAppContext = InstrumentationRegistry.getTargetContext();
    }

    @AfterClass
    public static void afterAll() {
        FileUtils.deleteQuietly(sTestRoot);
    }

    public static File generateTestDirectoryStructure(File parent) throws IOException {
        File testDir = new File(parent, "testdir");
        File folder1 = new File(testDir, "Folder 1");
        File folder2 = new File(testDir, "Folder 2");
        File folder3 = new File(folder1, "Folder 3");
        File folder4 = new File(folder2, "Folder 4");
        File folder5 = new File(folder1, "Folder 5");
        File folder6 = new File(folder2, "Folder 6");
        File folder7 = new File(folder1, "Folder 7");
        File folder8 = new File(folder2, "Folder 8");

        File testFile = new File(testDir, "test.txt");

        Assert.assertTrue(testDir.mkdirs());

        FileOutputStream testFileOS = new FileOutputStream(testFile);
        testFileOS.write("This is a test file.".getBytes());
        testFileOS.close();

        ArrayList<File> folders = new ArrayList<>();
        folders.add(folder3);
        folders.add(folder4);
        folders.add(folder5);
        folders.add(folder6);
        folders.add(folder7);
        folders.add(folder8);

        for (File folder : folders) {
            Assert.assertTrue(folder.mkdirs());
            File file1 = new File(folder, "file1.jpg");
            File file2 = new File(folder, "file2.txt");
            File file3 = new File(folder, "file3.wav");

            Assert.assertTrue(file1.createNewFile());
            Assert.assertTrue(file2.createNewFile());
            Assert.assertTrue(file3.createNewFile());
        }

        return testDir;
    }

    @Test
    public void copyProducesIdenticalDirectoryStructure() throws IllegalAccessException, ClassNotFoundException, InstantiationException, InterruptedException, ExecutionException, TimeoutException {
        doBindService();

        ArrayList<Uri> mToCopy = new ArrayList<>();
        for (File file : sTestDir.listFiles()) {
            mToCopy.add(Uri.fromFile(file));
        }

        FileSystemTask task = mService.copy(Uri.fromFile(sTestDir), mToCopy, Uri.fromFile(sCopyDir));
        Future fut = task.getTaskFuture();
        fut.get();

        checkDirectoriesAreIdentical(sTestDir, sCopyDir);
    }

    private void checkDirectoriesAreIdentical(File srcDir, File targetDir) {
        for (File file : srcDir.listFiles()) {
            File targetFile = new File(targetDir, file.getName());

            Assert.assertTrue("The file at " + targetFile.getAbsolutePath() + " isn't found, or doesn't share the properties of the original at " + file.getAbsolutePath(),
                    targetFile.exists() && file.isFile() == targetFile.isFile() && file.isDirectory() == targetFile.isDirectory());

            if (file.isDirectory()) {
                checkDirectoriesAreIdentical(file, targetFile);
            }
        }
    }

    private void doBindService() throws TimeoutException {
        Intent serviceIntent = new Intent(mAppContext, FileTransferService.class);
        mService = ((FileTransferService.FileTransferBinder)mServiceTestRule.bindService(serviceIntent)).getService();
    }
}
