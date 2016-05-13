package com.adyrsoft.soul;

import android.os.Bundle;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.adyrsoft.soul.service.ErrorInfo;
import com.adyrsoft.soul.service.FileSystemTask;
import com.adyrsoft.soul.service.FileTransferService;
import com.adyrsoft.soul.service.ProgressInfo;
import com.adyrsoft.soul.service.TaskResult;
import com.adyrsoft.soul.ui.FileSystemErrorDialog;
import com.adyrsoft.soul.ui.TaskProgressDialogFragment;

import java.util.HashMap;

public class ExplorerActivity extends AppCompatActivity implements RequestFileTransferServiceCallback, FileTransferService.TaskProgressListener, FileTransferService.TaskErrorListener {
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
    private FileSystemErrorDialog mErrorDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_explorer);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        if (savedInstanceState != null) {
            mInitialized = savedInstanceState.getBoolean(STATE_INITIALIZED);
            Log.d(TAG, "loading saved state");
        }

        final DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);

        Button backgroundTasksButton = (Button) findViewById(R.id.background_tasks_button);
        backgroundTasksButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.closeDrawers();
            }
        });

        FragmentTabHost tabHost = (FragmentTabHost) findViewById(R.id.fragment_tab_host);
        tabHost.setup(this, getSupportFragmentManager(), R.id.fragment_content);
        tabHost.addTab(tabHost.newTabSpec("explorer").setIndicator("Local"), ExplorerFragment.class, null);

    }

    @Override
    protected void onResume() {
        super.onResume();
        ((SoulApplication)getApplication()).requestFileTransferService(this);
    }

    @Override
    protected void onPause() {
        mService.removeTaskProgressListener(this);
        mService.setTaskErrorListener(null);

        if (mProgressDialogFragment != null) {
            mProgressDialogFragment.dismiss();
            mProgressDialogFragment = null;
        }

        if (mErrorDialog != null) {
            mErrorDialog.dismiss();
            mErrorDialog = null;
        }

        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putBoolean(STATE_INITIALIZED, mInitialized);
        Log.d(TAG, "Saving state");
    }

    @Override
    public void onProgressUpdate(FileSystemTask task, ProgressInfo info) {
        if (mProgressDialogFragment == null) {
            mProgressDialogFragment = (TaskProgressDialogFragment)getSupportFragmentManager().findFragmentByTag(TAG_PROGRESS_DIALOG_FRAGMENT);

            if (mProgressDialogFragment == null) {
                Log.d(TAG, "Showing progress dialog");
                mProgressDialogFragment = new TaskProgressDialogFragment();
                mProgressDialogFragment.show(getSupportFragmentManager(), TAG_PROGRESS_DIALOG_FRAGMENT);
            }
        }

        int totalFiles = info.getTotalFiles();
        int processedFiles = info.getProcessedFiles();

        mProgressDialogFragment.setMax(totalFiles);
        mProgressDialogFragment.setProgress(processedFiles);

        if (totalFiles == processedFiles && totalFiles != 0) {
            ExplorerFragment explorerFragment = (ExplorerFragment)getSupportFragmentManager().findFragmentById(R.id.fragment);
            explorerFragment.refresh();

            mProgressDialogFragment.dismiss();
            mProgressDialogFragment = null;
        }
    }

    @Override
    public void onTaskFinished(FileSystemTask task, TaskResult result) {
        if (mProgressDialogFragment != null) {
            mProgressDialogFragment.dismiss();
            mProgressDialogFragment = null;
        }
    }

    @Override
    public void onError(FileSystemTask task, ErrorInfo errorInfo) {
        mErrorDialog = new FileSystemErrorDialog();
        mErrorDialog.setUserFeedbackProvider(errorInfo.getFeedbackProvider());

        switch(errorInfo.getErrorType()) {
            case DEST_ALREADY_EXISTS:
                mErrorDialog.setErrorDescription(getString(R.string.destiny_file_already_exists_error_desc));
                mErrorDialog.setAffectedFile(errorInfo.getDestinyUri());
                mErrorDialog.setRetryButtonLabel(getString(R.string.overwrite_button_label));
                mErrorDialog.setIgnoreButtonLabel(getString(R.string.keep_button_label));
                break;
            case SOURCE_DOESNT_EXIST:
                mErrorDialog.setErrorDescription(getString(R.string.source_file_missing_error_desc));
                mErrorDialog.setAffectedFile(errorInfo.getSourceUri());
                break;
            case SOURCE_NOT_READABLE:
                mErrorDialog.setErrorDescription(getString(R.string.source_unreadable_error_desc));
                mErrorDialog.setAffectedFile(errorInfo.getSourceUri());
                break;
            case DEST_NOT_WRITABLE:
                mErrorDialog.setErrorDescription(getString(R.string.destiny_unwritable_error_desc));
                mErrorDialog.setAffectedFile(errorInfo.getDestinyUri());
                break;
            case READ_ERROR:
                mErrorDialog.setErrorDescription(getString(R.string.read_error_desc));
                break;
            case WRITE_ERROR:
                mErrorDialog.setErrorDescription(getString(R.string.write_error_desc));
                break;
            case UNKNOWN:
                mErrorDialog.setErrorDescription(getString(R.string.unknown_error_desc));
                break;
            case AUTHENTICATION_ERROR:
                mErrorDialog.setErrorDescription(getString(R.string.remote_host_auth_error_desc));
                break;
        }

        Log.d(TAG, "Showing error dialog");
        mErrorDialog.show(getSupportFragmentManager(), "errordialog");
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
        mService.addTaskProgressListener(this);
        mService.setTaskErrorListener(this);
//        ExplorerFragment explorerFragment = (ExplorerFragment)getSupportFragmentManager().findFragmentById(R.id.fragment);
//        explorerFragment.setFileTransferService(transferService);
//
//        if (!mInitialized) {
//            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//                explorerFragment.setCurrentDirectory(Environment.getExternalStorageDirectory());
//            }
//            mInitialized = true;
//        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSubscription(HashMap<FileSystemTask, ProgressInfo> pendingTasks) {
        for(FileSystemTask task : pendingTasks.keySet()) {
            onProgressUpdate(task, pendingTasks.get(task));
        }
    }
}
