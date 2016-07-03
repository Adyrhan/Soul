package com.adyrsoft.soul;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.adyrsoft.soul.service.ErrorInfo;
import com.adyrsoft.soul.service.FileSystemTask;
import com.adyrsoft.soul.service.FileTransferService;
import com.adyrsoft.soul.service.ProgressInfo;
import com.adyrsoft.soul.service.TaskResult;
import com.adyrsoft.soul.ui.BackgroundTasksFragment;
import com.adyrsoft.soul.ui.DynamicFragmentPagerAdapter;
import com.adyrsoft.soul.ui.FileSystemErrorDialog;
import com.adyrsoft.soul.ui.TaskProgressDialogFragment;

import java.util.ArrayList;
import java.util.HashMap;

public class ExplorerActivity extends AppCompatActivity implements ExplorerFragment.OnNewTaskCallback, RequestFileTransferServiceCallback, FileTransferService.TaskProgressListener, FileTransferService.TaskErrorListener {
    public static final int BACK_PRESS_DELAY_MILLIS = 2000;
    private static final String TAG = ExplorerActivity.class.getName();
    private static final String TAG_PROGRESS_DIALOG_FRAGMENT = "progressdialog";
    private static final String STATE_NUM_FRAGMENTS = "STATE_NUM_FRAGMENTS";
    private Toolbar mToolbar;
    private int mBackCount;
    private FileTransferService mService;
    private TaskProgressDialogFragment mProgressDialogFragment;

    private Runnable resetBackCountPress = new Runnable() {
        @Override
        public void run() {
            mBackCount = 0;
        }
    };

    private FileSystemErrorDialog mErrorDialog;
    private ExplorerPagerAdapter mFragmentAdapter;
    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private TabLayout.Tab mAddTab;
    private FragmentManager mFragmentManager;
    private Handler mHandler;
    private boolean mReportProgress;
    private FileSystemTask mProgressDialogTarget;
    private boolean mErrorReported;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_explorer);

        mHandler = new Handler();

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mFragmentManager = getSupportFragmentManager();

        final DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);

        Button backgroundTasksButton = (Button) findViewById(R.id.background_tasks_button);
        backgroundTasksButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = mFragmentAdapter.getFirstPositionForClass(BackgroundTasksFragment.class);

                if (position == ExplorerPagerAdapter.NOT_FOUND) {
                    FragmentEntry entry = new FragmentEntry();
                    entry.setFragment(new BackgroundTasksFragment());
                    entry.setTitle("Tasks");
                    mFragmentAdapter.addFragment(entry);
                    mTabLayout.setupWithViewPager(mViewPager);
                    addPlusTab();
                    mViewPager.setCurrentItem(mFragmentAdapter.getCount()-1);
                } else {
                    mViewPager.setCurrentItem(position);
                }

                drawerLayout.closeDrawers();
            }
        });

        mFragmentAdapter = new ExplorerPagerAdapter(getSupportFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mViewPager.setAdapter(mFragmentAdapter);

        mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
        mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

        Button closeTabButton = (Button) findViewById(R.id.close_tab_button);
        closeTabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFragmentAdapter.getCount() > 1) {
                    int deleteTargetItemIndex = mViewPager.getCurrentItem();
                    int newCurrentItem;

                    if (deleteTargetItemIndex + 1 < mFragmentAdapter.getCount()) {
                        newCurrentItem = deleteTargetItemIndex + 1;
                    } else {
                        newCurrentItem = deleteTargetItemIndex - 1;
                    }

                    mViewPager.setCurrentItem(newCurrentItem);

                    mFragmentAdapter.removeFragmentAt(deleteTargetItemIndex);
                    mTabLayout.setupWithViewPager(mViewPager);
                    addPlusTab();

                    int finalCurrentItem = Math.min(deleteTargetItemIndex, newCurrentItem);
                    mViewPager.setCurrentItem(finalCurrentItem);
                }
            }
        });

        if (savedInstanceState != null) {
            int numFragments = savedInstanceState.getInt(STATE_NUM_FRAGMENTS);

            for(int i = 0; i < numFragments; i++) {
                addExplorerTab();
            }
        } else {
            addExplorerTab();
        }

        Log.d(TAG, "activity onCreate called");
    }

    private void addExplorerTab() {
        Bundle explorerBundle = new Bundle();
        explorerBundle.putString(ExplorerFragment.DIRECTORY_PARENT, Environment.getExternalStorageDirectory().getPath());

        ExplorerFragment explorerFragment = new ExplorerFragment();
        explorerFragment.setArguments(explorerBundle);

        FragmentEntry fragmentEntry = new FragmentEntry();
        fragmentEntry.setFragment(explorerFragment);
        fragmentEntry.setTitle("Local");

        mFragmentAdapter.addFragment(fragmentEntry);
        mTabLayout.setupWithViewPager(mViewPager);

        addPlusTab();
    }

    private void addPlusTab() {
        TextView addTabLabel = new TextView(this);
        addTabLabel.setText("+");
        addTabLabel.setTextColor(getResources().getColor(R.color.colorAccent));
        addTabLabel.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addTabLabel.setGravity(Gravity.CENTER);

        addTabLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTabLayout.removeTabAt(mTabLayout.getChildCount() - 1);
                addExplorerTab();
            }
        });

        mTabLayout.addTab(mTabLayout.newTab().setCustomView(addTabLabel), false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((SoulApplication)getApplication()).requestFileTransferService(this);
    }

    @Override
    protected void onPause() {
        if (mService != null) {
            mService.removeTaskProgressListener(this);
            mService.setTaskErrorListener(null);
        }

        releaseProgressDialog();

        if (mErrorDialog != null) {
            mErrorDialog.dismiss();
            mErrorDialog = null;
        }

        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt(STATE_NUM_FRAGMENTS, mFragmentAdapter.getCount());
        Log.d(TAG, "Saving state");
    }

    @Override
    public void onProgressUpdate(FileSystemTask task, ProgressInfo info) {
        if (!mReportProgress) return;

        if (mProgressDialogFragment == null) {
            mProgressDialogFragment = (TaskProgressDialogFragment)getSupportFragmentManager().findFragmentByTag(TAG_PROGRESS_DIALOG_FRAGMENT);

            if (mProgressDialogFragment == null) {
                Log.d(TAG, "Showing progress dialog");
                mProgressDialogFragment = new TaskProgressDialogFragment();
                mProgressDialogFragment.setOnHideButtonClick(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        releaseProgressDialog();
                    }
                });

                if (!mErrorReported) {
                    mProgressDialogFragment.show(getSupportFragmentManager(), TAG_PROGRESS_DIALOG_FRAGMENT);
                    mFragmentManager.executePendingTransactions();
                }
            }
        }

        if (mProgressDialogTarget == task) {
            int totalFiles = info.getTotalFiles();
            int processedFiles = info.getProcessedFiles();

            mProgressDialogFragment.setMax(totalFiles);
            mProgressDialogFragment.setProgress(processedFiles);
        }
    }

    @Override
    public void onTaskFinished(FileSystemTask task, TaskResult result) {
        releaseProgressDialog();
    }

    private void releaseProgressDialog() {
        if (mProgressDialogFragment != null) {
            mProgressDialogFragment.dismiss();
            mProgressDialogFragment = null;
        }
    }

    @Override
    public void onError(FileSystemTask task, ErrorInfo errorInfo) {
        mErrorReported = true;

        if (mProgressDialogFragment != null && mProgressDialogFragment.isAdded()) {
            mProgressDialogFragment.dismiss();
            mFragmentManager.executePendingTransactions();
        }

        mErrorDialog = new FileSystemErrorDialog();
        mErrorDialog.setUserFeedbackProvider(errorInfo.getFeedbackProvider());
        mErrorDialog.setOnFeedbackProvidedCallback(new FileSystemErrorDialog.OnFeedbackProvidedCallback() {
            @Override
            public void onFeedbackProvided() {
                if (mReportProgress) {
                    mErrorReported = false;
                    mProgressDialogFragment.show(getSupportFragmentManager(), TAG_PROGRESS_DIALOG_FRAGMENT);
                    mFragmentManager.executePendingTransactions();
                }
            }
        });

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
        mFragmentManager.executePendingTransactions();
    }

    @Override
    public void onBackPressed() {
        ExplorerFragment explorerFragment = (ExplorerFragment) mFragmentAdapter.getItem(mViewPager.getCurrentItem());
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

    @Override
    public void OnNewTaskCreated(FileSystemTask mFileSystemTask) {
        mReportProgress = true;
        mProgressDialogTarget = mFileSystemTask;
    }

    private class ExplorerPagerAdapter extends DynamicFragmentPagerAdapter{
        // Value returned by getFirstPositionForClass in case there isn't such a fragment of the class given
        public static final int NOT_FOUND = -1;

        private ArrayList<FragmentEntry> mFragmentEntries = new ArrayList<>();

        public ExplorerPagerAdapter(FragmentManager supportFragmentManager) {
            super(supportFragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentEntries.get(position).getFragment();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentEntries.get(position).getTitle();
        }

        @Override
        public int getCount() {
            if (mFragmentEntries == null) return 0;
            return mFragmentEntries.size();
        }

        public void addFragment(FragmentEntry fragmentEntry) {
            mFragmentEntries.add(fragmentEntry);
            notifyDataSetChanged();
        }

        public void removeFragment(FragmentEntry fragmentEntry) {
            removeFragmentAt(mFragmentEntries.indexOf(fragmentEntry));
        }

        public void removeFragmentAt(int position) {
            mFragmentEntries.remove(position);
            notifyDataSetChanged();
        }

        public int getFirstPositionForClass(Class<? extends Fragment> cls) {
            for(int i = 0; i < mFragmentEntries.size(); i++) {
                Fragment fragment = mFragmentEntries.get(i).getFragment();
                if (fragment.getClass().equals(cls)) {
                    return i;
                }
            }

            return NOT_FOUND;
        }
    }

    private class FragmentEntry {
        private Fragment mFragment;
        private String mTitle;

        public Fragment getFragment() {
            return mFragment;
        }

        public void setFragment(Fragment fragment) {
            mFragment = fragment;
        }

        public String getTitle() {
            return mTitle;
        }

        public void setTitle(String title) {
            mTitle = title;
        }
    }
}
