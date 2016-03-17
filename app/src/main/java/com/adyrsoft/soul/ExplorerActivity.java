package com.adyrsoft.soul;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.adyrsoft.soul.service.FileSystemErrorType;
import com.adyrsoft.soul.service.FileSystemTask;
import com.adyrsoft.soul.service.TaskListener;
import com.adyrsoft.soul.ui.TaskProgressDialogFragment;

public class ExplorerActivity extends AppCompatActivity implements RequestFileTransferServiceCallback, TaskListener{
    public static final int BACK_PRESS_DELAY_MILLIS = 2000;
    private static final String STATE_INITIALIZED = "STATE_INITIALIZED";
    private static final String TAG = ExplorerActivity.class.getName();
    private static final String TAG_PROGRESS_DIALOG_FRAGMENT = "progressdialog";
    private Toolbar mToolbar;
    private int mBackCount;
    private boolean mInitialized;
    private FileTransferService mService;
    private TaskProgressDialogFragment mProgressDialogFragment;

    private Runnable resetBackCountPress = new Runnable() {
        @Override
        public void run() {
            mBackCount = 0;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explorer);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        Log.d(TAG, "onCreate called");
        if(savedInstanceState != null) {
            mInitialized = savedInstanceState.getBoolean(STATE_INITIALIZED);
            Log.d(TAG, "loading saved state");
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        ((SoulApplication)getApplication()).requestFileTransferService(this);
    }

    @Override
    protected void onPause() {
        mService.removeCallback();
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putBoolean(STATE_INITIALIZED, mInitialized);
        Log.d(TAG, "Saving state");
    }

    @Override
    public void onProgressUpdate(FileSystemTask task, int totalFiles, int filesProcessed, int totalBytes, int bytesProcessed) {
        mProgressDialogFragment = (TaskProgressDialogFragment)getSupportFragmentManager()
                .findFragmentByTag(TAG_PROGRESS_DIALOG_FRAGMENT);

        if (mProgressDialogFragment == null) {
            mProgressDialogFragment = new TaskProgressDialogFragment();
            mProgressDialogFragment.setMax(totalFiles);
            mProgressDialogFragment.show(getSupportFragmentManager(), TAG_PROGRESS_DIALOG_FRAGMENT);
        }

        if (totalFiles == filesProcessed) {
            mProgressDialogFragment.dismiss();
            ExplorerFragment explorerFragment = (ExplorerFragment)getSupportFragmentManager().findFragmentById(R.id.fragment);
            explorerFragment.refresh();
        } else {
            mProgressDialogFragment.setProgress(filesProcessed);
        }
    }

    @Override
    public void onError(FileSystemTask task, Uri srcFile, Uri dstFile, FileSystemErrorType errorType) {

    }

    @Override
    public void onBackPressed() {
        ExplorerFragment explorerFragment = (ExplorerFragment)getSupportFragmentManager().findFragmentById(R.id.fragment);
        if (explorerFragment.onBackPressed()) {
            mToolbar.removeCallbacks(resetBackCountPress);
            mBackCount++;
            if (mBackCount < 2) {
                Toast.makeText(this, R.string.press_back_again_message, Toast.LENGTH_SHORT).show();
                mToolbar.postDelayed(resetBackCountPress, BACK_PRESS_DELAY_MILLIS);
            } else {
                mBackCount = 0;
                super.onBackPressed();
            }
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_explorer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onServiceReady(FileTransferService transferService) {
        mService = transferService;
        mService.setClientTaskListener(this);
        ExplorerFragment explorerFragment = (ExplorerFragment)getSupportFragmentManager().findFragmentById(R.id.fragment);
        explorerFragment.setFileTransferService(transferService);

        if (!mInitialized) {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                explorerFragment.setCurrentDirectory(Environment.getExternalStorageDirectory());
            }
            mInitialized = true;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
