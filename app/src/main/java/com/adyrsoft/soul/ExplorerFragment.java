package com.adyrsoft.soul;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Toast;

import com.adyrsoft.soul.data.Entry;
import com.adyrsoft.soul.data.Entry.EntryType;
import com.adyrsoft.soul.service.FileSystemTask;
import com.adyrsoft.soul.service.FileTransferService;
import com.adyrsoft.soul.ui.DirectoryPathView;
import com.adyrsoft.soul.ui.FileGridItemView;
import com.adyrsoft.soul.ui.TaskProgressDialogFragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * This fragment is a full blown file explorer. It changes the menu options on the activity to show
 * extra operations.
 */
public class ExplorerFragment extends Fragment implements DirectoryPathView.OnPathSegmentSelectedListener, RequestFileTransferServiceCallback {

    public static final String FILE_SCHEME = "file://";

    private enum ExplorerState {
        NAVIGATION,
        SELECTION,
        UNREADY
    }

    public interface OnNewTaskCallback {
        void OnNewTaskCreated(FileSystemTask mFileSystemTask);
    }

    public static final String DIRECTORY_PARENT = "DIRECTORY_PARENT";

    private static final String EXPLORER_STATE = "EXPLORER_STATE";
    private static final String TAG = ExplorerFragment.class.getSimpleName();
    private static final String DIRECTORY_FILES = "DIRECTORY_FILES";
    private static final String SELECTED_FILES = "SELECTED_FILES";

    private GridView mGridView;
    private FileGridAdapter mFileGridAdapter;
    private DirectoryPathView mPathView;
    private Entry mCurrentDir;
    private Uri mSrcWD;
    private ExplorerState mExplorerState;
    private HashSet<Entry> mSelectedFileSet = new HashSet<>();
    private FileTransferService mService;
    private Menu mMenu;
    private TaskProgressDialogFragment mProgressDialogFragment;
    private List<Uri> mToCopy = new ArrayList<>();
    private ExplorerFileObserver mFileObserver;
    private OnNewTaskCallback mOnNewTaskCallback;

    public ExplorerFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnNewTaskCallback) {
            mOnNewTaskCallback = (OnNewTaskCallback)context;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_explorer, container, false);
        mGridView = (GridView)rootView.findViewById(R.id.file_grid);
        mPathView = (DirectoryPathView)rootView.findViewById(R.id.directory_path_view);

        mFileGridAdapter = new FileGridAdapter(getActivity());

        mGridView.setAdapter(mFileGridAdapter);

        mPathView.addOnPathSegmentSelectedListener(this);

        mExplorerState = ExplorerState.UNREADY;

        if (savedInstanceState != null) {
            ExplorerState savedState = ExplorerState.values()[savedInstanceState.getInt(EXPLORER_STATE)];
            stateChange(savedState);

            mCurrentDir = savedInstanceState.getParcelable(DIRECTORY_PARENT);
            mPathView.setCurrentDirectory(mCurrentDir);

            ArrayList<Entry> selectedFiles = savedInstanceState.getParcelableArrayList(SELECTED_FILES);
            mSelectedFileSet.addAll(selectedFiles);

            ArrayList<Entry> entries = savedInstanceState.getParcelableArrayList(DIRECTORY_FILES);
            for(Entry entry : entries) {
                mFileGridAdapter.add(entry);
            }
        } else if (getArguments() != null ){
            mCurrentDir = getArguments().getParcelable(DIRECTORY_PARENT);
        }

        setHasOptionsMenu(true);

        requestFileTransferService();

        return rootView;
    }

    private void requestFileTransferService() {
        ((SoulApplication) getActivity().getApplication()).requestFileTransferService(this);
    }

    @Override
    public void onServiceReady(FileTransferService transferService) {
        mService = transferService;
        if (mExplorerState == ExplorerState.UNREADY) {
            stateChange(ExplorerState.NAVIGATION);
            if (mCurrentDir != null) {
                setCurrentDirectory(mCurrentDir);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mFileObserver != null) {
            mFileObserver.stopWatching();
            mFileObserver = null;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        switch(mExplorerState) {
            case NAVIGATION:
                menuInflater.inflate(R.menu.menu_explorer_navigation, menu);

                if (mToCopy.size() > 0) {
                    MenuItem pasteItem = menu.findItem(R.id.paste);
                    pasteItem.setVisible(true);
                }

                break;
            case SELECTION:
                menuInflater.inflate(R.menu.menu_explorer_selection, menu);
                break;
        }
        mMenu = menu;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.create_folder:
                showCreateFolderDialog();
                return true;
            case R.id.copy:
                prepareCopy();
                return true;

            case R.id.paste:
                startCopy();
                return true;

            case R.id.remove:
                startRemove();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void showCreateFolderDialog() {
        final EditText folderNameET = new EditText(getActivity());
        folderNameET.setSingleLine(true);

        final InputMethodManager imm =
                (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        AlertDialog newFolderDialog = new AlertDialog.Builder(getActivity())
                .setTitle("Create new folder")
                .setMessage("Name your new folder")
                .setView(folderNameET)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String folderName = folderNameET.getText().toString();
                        createFolder(folderName);
                        imm.hideSoftInputFromWindow(folderNameET.getWindowToken(), 0);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        imm.hideSoftInputFromWindow(folderNameET.getWindowToken(), 0);
                    }
                }).create();

        newFolderDialog.show();

        folderNameET.requestFocus();

        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    private void prepareCopy() {
        mToCopy.clear();

        for(Entry entry : mSelectedFileSet) {
            mToCopy.add(entry.getUri());
        }

        mSrcWD = mCurrentDir.getUri();
        stateChange(ExplorerState.NAVIGATION);
        Toast.makeText(getActivity(), "You selected to copy " + mToCopy.size() + " files", Toast.LENGTH_SHORT).show();
    }

    private void startRemove() {
        ArrayList<Uri> toRemove = new ArrayList<>();
        for (Entry fileEntry : mSelectedFileSet) {
            toRemove.add(fileEntry.getUri());
        }

        FileSystemTask newTask = mService.remove(mCurrentDir.getUri(), toRemove);
        mSelectedFileSet.clear();
        stateChange(ExplorerState.NAVIGATION);

        if(mOnNewTaskCallback != null) {
            mOnNewTaskCallback.OnNewTaskCreated(newTask);
        }
    }

    private void startCopy() {
        FileSystemTask newTask = mService.copy(mSrcWD, mToCopy, mCurrentDir.getUri());
        mSelectedFileSet.clear();
        mSrcWD = null;
        mToCopy.clear();

        if(mOnNewTaskCallback != null) {
            mOnNewTaskCallback.OnNewTaskCreated(newTask);
        }
    }

    // TODO: Replace implementation of createFolder with one that uses the FileTransferService
    private void createFolder(String folderName) {
//        File newFolder = new File(mCurrentDir, folderName);
//        if (newFolder.exists()) {
//            Toast.makeText(getActivity(), "Folder already exists", Toast.LENGTH_LONG).show();
//        } else {
//            boolean success = newFolder.mkdirs();
//            if (!success) {
//                Toast.makeText(getActivity(), "Couldn't create folder", Toast.LENGTH_LONG).show();
//            } else {
//                refresh();
//            }
//        }
    }

    public boolean onBackPressed() {
        switch(mExplorerState) {
            case NAVIGATION:
                return !changeCWDToParentDir();
            case SELECTION:
                stateChange(ExplorerState.NAVIGATION);
                return false;
            default:
                return false;
        }
    }

    private boolean changeCWDToParentDir() {
//        if (mCurrentDir.getUri().getScheme().equals(FILE_SCHEME)) {
//
//        }
//        File externalStorageRoot = Environment.getExternalStorageDirectory();
//
//        String externalStoragePath = externalStorageRoot.getPath();
//        String parentPath = mCurrentDir.getParent();
//
//        if (parentPath != null && parentPath.indexOf(externalStoragePath) >= 0) {
//            setCurrentDirectory(mCurrentDir.getParentFile());
//            return true;
//        } else {
//            return false;
//        }
        File parentFile = new File(mCurrentDir.getUri().getPath()).getParentFile();
        boolean changeToParent = (parentFile != null);
        if (changeToParent) {
            Uri currentUri = mCurrentDir.getUri();
            Uri parentUri = new Uri.Builder()
                    .scheme(currentUri.getScheme())
                    .path(parentFile.getPath())
                    .encodedFragment((currentUri.getEncodedFragment()))
                    .encodedQuery(currentUri.getEncodedQuery())
                    .encodedAuthority(currentUri.getEncodedAuthority())
                    .build();
            Entry parentEntry = new Entry(parentUri, EntryType.CONTAINER);
            setCurrentDirectory(parentEntry);
        }
        return changeToParent;
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);

        ArrayList<Entry> entries = new ArrayList<>();
        for(int i = 0; i < mFileGridAdapter.getCount(); i++) {
            entries.add(mFileGridAdapter.getItem(i));
        }
        out.putParcelableArrayList(DIRECTORY_FILES, entries);

        out.putParcelable(DIRECTORY_PARENT, mCurrentDir);

        ArrayList<Entry> selectedFiles = new ArrayList<>();
        selectedFiles.addAll(mSelectedFileSet);
        out.putParcelableArrayList(SELECTED_FILES, selectedFiles);

        out.putInt(EXPLORER_STATE, mExplorerState.ordinal());
    }

    public void setCurrentDirectory(@NonNull String path) {
        Entry entry = new Entry(Uri.parse(path), EntryType.CONTAINER);
        setCurrentDirectory(entry);
    }

    public void setCurrentDirectory(@NonNull Entry entry) {
        if (mExplorerState == ExplorerState.UNREADY) {
            return;
        }

        if (entry.getType() != EntryType.CONTAINER) {
            throw new IllegalArgumentException(entry.getUri().getPath() + " is not a directory");
        }

        if (mFileObserver != null) {
            mFileObserver.stopWatching();
        }

        if (entry.getUri().getScheme().equals(FILE_SCHEME)) {
            mFileObserver = new ExplorerFileObserver(entry.getUri().getPath());
            mFileObserver.startWatching();
        }

        mCurrentDir = entry;
        refresh();
        mPathView.setCurrentDirectory(mCurrentDir);
    }

    public void refresh() {
        File[] files = new File(mCurrentDir.getUri().getPath()).listFiles();

        // TODO: This code block should be moved once we do the file system queries in background
        ArrayList<Entry> entries = new ArrayList<>();
        if (files != null && files.length > 0) {
            for(File file : files) {
                Entry entry = null;
                if (file.isDirectory()) {
                    entry = new Entry(Uri.parse(file.toURI().toString()), EntryType.CONTAINER);
                } else {
                    entry = new Entry(Uri.parse(file.toURI().toString()), EntryType.FILE);
                }
                entries.add(entry);
            }
        }

        mFileGridAdapter.clear();

        if (files != null && files.length > 0) {
            mFileGridAdapter.addAll(entries);
        }

        mFileGridAdapter.notifyDataSetChanged();
    }

    private void stateChange(ExplorerState state) {
        if (mExplorerState == state) {
            return;
        }

        mExplorerState = state;

        switch(state) {
            case NAVIGATION:
                mSelectedFileSet.clear();
                mGridView.dispatchSetSelected(false);
                getActivity().invalidateOptionsMenu();
                mGridView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
                break;
            case SELECTION:
                mGridView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
                getActivity().invalidateOptionsMenu();
                break;
            case UNREADY:
                break;
        }

    }

    @Override
    public void OnPathSegmentSelected(DirectoryPathView pathView, View segmentView, Entry path) {
        setCurrentDirectory(path);
    }

    class FileGridAdapter extends ArrayAdapter<Entry> {
        private Context mContext;
        private LayoutInflater mInflater;

        public FileGridAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_1);
            mContext = context;
            mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null || !(convertView instanceof FileGridItemView)) {
                convertView = new FileGridItemView(mContext);
            }

            final FileGridItemView v = (FileGridItemView)convertView;

            Entry entry = getItem(position);

            if (entry.getType() == EntryType.CONTAINER) {
                v.setIconResource(R.drawable.ic_folder_24dp);
            } else {
                v.setIconResource(R.drawable.ic_insert_drive_file_24px);
            }

            v.setFileName(entry.getName());

            v.setTag(entry);

            if (mSelectedFileSet.contains(entry)) {
                mGridView.post(new Runnable() {
                    @Override
                    public void run() {
                        v.setSelected(true);
                    }
                });
            }

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Entry file = ((Entry) v.getTag());

                    switch (mExplorerState) {
                        case NAVIGATION:
                            navigateOrOpenFile(file);
                            break;
                        case SELECTION:
                            switchSelectedFileState(file, v);
                            break;
                    }
                }
            });

            v.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    stateChange(ExplorerState.SELECTION);

                    Entry entryTag = ((Entry) v.getTag());

                    boolean select = !mSelectedFileSet.contains(entryTag);

                    if (select) {
                        v.setSelected(true);
                        mSelectedFileSet.add(entryTag);
                    }
                    return true;
                }
            });

            return v;
        }

    }

    private void switchSelectedFileState(Entry file, View v) {
        boolean select = !mSelectedFileSet.contains(file);

        v.setSelected(select);

        if (select) {
            mSelectedFileSet.add(file);
        } else {
            mSelectedFileSet.remove(file);
        }
    }

    private void navigateOrOpenFile(Entry entry) {
        if (entry.getType() == EntryType.CONTAINER) {
            setCurrentDirectory(entry);
        } else {
//            try {
//                // TODO: Figure out a better way to find the file's mime type
//                String mimeType = URLConnection.guessContentTypeFromStream(new FileInputStream(entry));
//
//                if (mimeType == null) {
//                    MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
//                    String fileName = entry.getName();
//                    String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
//                    mimeType = mimeTypeMap.getMimeTypeFromExtension(extension);
//                }
//
//                if (mimeType == null) {
//                    Toast.makeText(getActivity(), "Unknown file type", Toast.LENGTH_LONG).show();
//                } else {
//                    Log.d(TAG, "Guessed mimetype for file is " + mimeType);
//                    Intent fileOpenIntent = new Intent(Intent.ACTION_VIEW);
//
//                    fileOpenIntent.setDataAndType(entry.getUri(), mimeType);
//
//                    startActivity(fileOpenIntent);
//                }
//
//            } catch (FileNotFoundException e) {
//                Toast.makeText(getActivity(), "Couldn't open the file. It has been deleted.", Toast.LENGTH_LONG).show();
//            } catch (IOException e) {
//                Toast.makeText(getActivity(), "Read error", Toast.LENGTH_LONG).show();
//            } catch (ActivityNotFoundException e) {
//                Log.e(TAG, null, e);
//                Toast.makeText(getActivity(), "There isn't an app installed that can handle this file type", Toast.LENGTH_LONG).show();
//            }

        }
    }

    public class ExplorerFileObserver extends FileObserver {
        private Handler mUIHandler = new Handler();

        public ExplorerFileObserver(String path) {
            super(path, CREATE | DELETE | MOVED_FROM | MOVED_TO);
        }

        @Override
        public void onEvent(int event, String path) {
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    refresh();
                }
            });
        }
    }


}
