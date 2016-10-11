package com.adyrsoft.soul;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.FileObserver;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Toast;

import com.adyrsoft.soul.data.Entry;
import com.adyrsoft.soul.data.Entry.EntryType;
import com.adyrsoft.soul.service.FileOperation;
import com.adyrsoft.soul.service.FileSystemTask;
import com.adyrsoft.soul.service.FileTransferService;
import com.adyrsoft.soul.service.ProgressInfo;
import com.adyrsoft.soul.service.QueryResultCallback;
import com.adyrsoft.soul.service.TaskResult;
import com.adyrsoft.soul.ui.DirectoryPathView;
import com.adyrsoft.soul.ui.FileGridItemView;
import com.adyrsoft.soul.ui.GridRadioGroup;
import com.adyrsoft.soul.ui.TaskProgressDialogFragment;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * File explorer user interface. All file operations and queries are done on the background service.
 * It changes the menu options on the activity to show extra operations.
 */
public class ExplorerFragment extends Fragment implements DirectoryPathView.OnPathSegmentSelectedListener, RequestFileTransferServiceCallback, FileTransferService.TaskProgressListener {
    private static final String EXPLORER_PREFERENCES = "EXPLORER_PREFERENCES";
    private static final String PREF_ORDER_BY = "PREF_ORDER_BY";
    private static final String PREF_ASCENDING = "PREF_ASCENDING";

    public interface OnOrderSelectedListener {
        void onOrderSelected(Entry.Field orderBy, boolean ascending);
    }

    public static class RefreshCallback implements QueryResultCallback {
        private final WeakReference<ExplorerFragment> mWeakFragment;

        public RefreshCallback(ExplorerFragment fragment) {
            mWeakFragment = new WeakReference<>(fragment);
        }

        @Override
        public void onQueryCompleted(List<Entry> entries) {
            ExplorerFragment fragment = mWeakFragment.get();
            if (fragment != null) {
                fragment.updateEntries(entries);
            }
        }

        @Override
        public void onQueryFailed(Exception ex) {

        }
    }

    public static class OrderDialogFragment extends DialogFragment {
        public static final String ARG_ORDER_BY = "ARG_ORDER_BY";
        public static final String ARG_ASCENDING = "ARG_ASCENDING";

        private static final String CHECKED_ID = "CHECKED_ID";

        private GridRadioGroup mRadioGroup;
        private OnOrderSelectedListener mListener;

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            View root = LayoutInflater.from(getActivity()).inflate(R.layout.order_dialog_layout, null);
            mRadioGroup = (GridRadioGroup)root.findViewById(R.id.radio_group);

            if (savedInstanceState != null) {
                int checkedId = savedInstanceState.getInt(CHECKED_ID, -1);
                if (checkedId >= 0) {
                    mRadioGroup.check(checkedId);
                }
            } else {
                Bundle args = getArguments();
                if (args != null) {
                    int ordinal = args.getInt(ARG_ORDER_BY, -1);
                    if (ordinal >= 0) {
                        Entry.Field orderBy = Entry.Field.values()[ordinal];
                        boolean ascending = args.getBoolean(ARG_ASCENDING);
                        int checkedId;
                        switch(orderBy) {
                            case NAME:
                                checkedId = ascending ? R.id.name_asc : R.id.name_desc;
                                break;
                            case EXTENSION:
                                checkedId = ascending ? R.id.ext_asc : R.id.ext_desc;
                                break;
                            default:
                                checkedId = -1;
                        }

                        if (checkedId >= 0) {
                            mRadioGroup.check(checkedId);
                        }
                    }
                }
            }

            return new AlertDialog.Builder(getActivity())
                    .setView(root)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (mListener != null) {
                                boolean ascending = false;
                                Entry.Field orderBy = null;
                                switch(mRadioGroup.getCheckedRadioButtonId()) {
                                    case R.id.name_asc:
                                        ascending = true;
                                        orderBy = Entry.Field.NAME;
                                        break;
                                    case R.id.name_desc:
                                        ascending = false;
                                        orderBy = Entry.Field.NAME;
                                        break;
                                    case R.id.ext_asc:
                                        ascending = true;
                                        orderBy = Entry.Field.EXTENSION;
                                        break;
                                    case R.id.ext_desc:
                                        ascending = false;
                                        orderBy = Entry.Field.EXTENSION;
                                        break;
                                    default:
                                        ascending = false;
                                        orderBy = Entry.Field.NAME;
                                }
                                mListener.onOrderSelected(orderBy, ascending);
                            }
                        }
                    })
                    .create();
        }

        @Override
        public void onSaveInstanceState(Bundle out) {
            out.putInt(CHECKED_ID, mRadioGroup.getCheckedRadioButtonId());
        }

        public void setOnOrderSelectedListener(OnOrderSelectedListener listener) {
            mListener = listener;
        }
    }

    private enum ExplorerState {
        NAVIGATION,
        SELECTION,
        UNREADY
    }

    public interface OnNewTaskCallback {
        void OnNewTaskCreated(FileSystemTask mFileSystemTask);
    }

    public static final String FILE_SCHEME = "file://";
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
    private List<Uri> mFileClipboard = new ArrayList<>();
    private FileOperation mClipboardOperation;
    private OnNewTaskCallback mOnNewTaskCallback;
    private RefreshCallback mRefreshCallback = new RefreshCallback(this);
    private Entry.Field mOrderBy;
    private boolean mAscendingOrder;

    private OnOrderSelectedListener mOnOrderSelectedListener = new OnOrderSelectedListener() {
        @Override
        public void onOrderSelected(Entry.Field orderBy, boolean ascending) {
            mOrderBy = orderBy;
            mAscendingOrder = ascending;

            ArrayList<Entry> entries = new ArrayList<>();
            for (int i = 0; i < mFileGridAdapter.getCount(); i++) {
                entries.add(mFileGridAdapter.getItem(i));
            }

            updateEntries(entries);
        }
    };

    public ExplorerFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnNewTaskCallback) {
            mOnNewTaskCallback = (OnNewTaskCallback)context;
        }

    }

    public FileGridAdapter getFileGridAdapter() { return mFileGridAdapter; }

    @Override
    public void onSubscription(HashMap<FileSystemTask, ProgressInfo> pendingTasks) {

    }

    @Override
    public void onProgressUpdate(FileSystemTask task, ProgressInfo info) {

    }

    @Override
    public void onTaskFinished(FileSystemTask task, TaskResult result) {
        if ((task.getSrcWD() != null && task.getSrcWD().equals(mCurrentDir.getUri())) ||
                (task.getDst() != null && task.getDst().equals(mCurrentDir.getUri()))) {
            if (task.getFileOperation() == FileOperation.CREATE_FOLDER) {
                Toast.makeText(getActivity(), "Folder created!", Toast.LENGTH_SHORT).show();
            }

            if (task.getFileOperation() != FileOperation.QUERY) {
                refresh();
            }
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
        } else {
            if (getArguments() != null ) {
                mCurrentDir = getArguments().getParcelable(DIRECTORY_PARENT);
            }

            SharedPreferences preferences = getActivity().getSharedPreferences(EXPLORER_PREFERENCES, Context.MODE_PRIVATE);

            mOrderBy = Entry.Field.values()[preferences.getInt(PREF_ORDER_BY, Entry.Field.NAME.ordinal())];
            mAscendingOrder = preferences.getBoolean(PREF_ASCENDING, true);
        }

        setHasOptionsMenu(true);

        requestFileTransferService();

        return rootView;
    }

    private void updateEntries(List<Entry> entries) {
        mFileGridAdapter.clear();
        if (entries != null && entries.size() > 0) {
            Entry.Comparator comparator = new Entry.Comparator(mOrderBy, mAscendingOrder);

            ArrayList<Entry> files = new ArrayList<>();
            ArrayList<Entry> containers = new ArrayList<>();

            for(Entry entry : entries) {
                switch(entry.getType()) {
                    case FILE:
                        files.add(entry);
                        break;
                    case CONTAINER:
                        containers.add(entry);
                        break;
                }
            }

            Collections.sort(files, comparator);
            Collections.sort(containers, comparator);

            ArrayList<Entry> sortedEntries = new ArrayList<>(containers);
            sortedEntries.addAll(files);

            mFileGridAdapter.addAll(sortedEntries);
        }
        mFileGridAdapter.notifyDataSetChanged();
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
        mService.addTaskProgressListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        switch(mExplorerState) {
            case NAVIGATION:
                menuInflater.inflate(R.menu.menu_explorer_navigation, menu);

                if (mFileClipboard.size() > 0) {
                    MenuItem pasteItem = menu.findItem(R.id.paste);
                    pasteItem.setVisible(true);
                }

                break;
            case SELECTION:
                menuInflater.inflate(R.menu.menu_explorer_selection, menu);
                MenuItem renameItem = menu.findItem(R.id.rename);
                if (mSelectedFileSet.size() > 1) {
                    renameItem.setVisible(false);
                }
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

            case R.id.move:
                prepareMove();
                return true;

            case R.id.paste:
                switch(mClipboardOperation) {
                    case COPY:
                        startCopy();
                        break;
                    case MOVE:
                        startMove();
                        break;
                }
                return true;

            case R.id.remove:
                startRemove();
                return true;

            case R.id.rename:
                prepareRename();
                return true;

            case R.id.order:
                OrderDialogFragment orderDialogFragment = new OrderDialogFragment();

                Bundle args = new Bundle();
                args.putBoolean(OrderDialogFragment.ARG_ASCENDING, mAscendingOrder);
                args.putInt(OrderDialogFragment.ARG_ORDER_BY, mOrderBy.ordinal());

                orderDialogFragment.setArguments(args);
                orderDialogFragment.setOnOrderSelectedListener(mOnOrderSelectedListener);
                orderDialogFragment.show(getFragmentManager(), "orderDialogFragment");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void prepareRename() {
        final Entry selected = mSelectedFileSet.toArray(new Entry[mSelectedFileSet.size()])[0];
        final EditText editText = new EditText(getActivity());
        editText.setSingleLine(true);
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(255)});
        editText.setText(selected.getUri().getLastPathSegment());

        AlertDialog renameDialog = new AlertDialog.Builder(getActivity())
                .setTitle("Rename file")
                .setView(editText)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startRename(selected, editText.getText().toString());
                    }
                })
                .create();
        renameDialog.show();
    }

    private void startRename(Entry selected, String newName) {
        ArrayList<String> pathSegments = new ArrayList<>(selected.getUri().getPathSegments());
        pathSegments.remove(pathSegments.size() - 1);
        pathSegments.add(newName);

        Uri dstUri = selected.getUri().buildUpon().path(null).build();
        for (String pathSegment : pathSegments) {
            dstUri = Uri.withAppendedPath(dstUri, pathSegment);
        }

        FileSystemTask newTask = mService.rename(mCurrentDir.getUri(), selected.getUri(), dstUri);

        mSelectedFileSet.clear();
        stateChange(ExplorerState.NAVIGATION);

        if(mOnNewTaskCallback != null) {
            mOnNewTaskCallback.OnNewTaskCreated(newTask);
        }
    }

    private void prepareMove() {
        mFileClipboard.clear();

        for(Entry entry : mSelectedFileSet) {
            mFileClipboard.add(entry.getUri());
        }
        mClipboardOperation = FileOperation.MOVE;
        mSelectedFileSet.clear();
        mSrcWD = mCurrentDir.getUri();
        stateChange(ExplorerState.NAVIGATION);
        Toast.makeText(getActivity(), "You selected to copy " + mFileClipboard.size() + " files", Toast.LENGTH_SHORT).show();
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
        mFileClipboard.clear();

        for(Entry entry : mSelectedFileSet) {
            mFileClipboard.add(entry.getUri());
        }
        mClipboardOperation = FileOperation.COPY;
        mSelectedFileSet.clear();
        mSrcWD = mCurrentDir.getUri();
        stateChange(ExplorerState.NAVIGATION);
        Toast.makeText(getActivity(), "You selected to copy " + mFileClipboard.size() + " files", Toast.LENGTH_SHORT).show();
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
        FileSystemTask newTask = mService.copy(mSrcWD, mFileClipboard, mCurrentDir.getUri());
        mSrcWD = null;
        mFileClipboard.clear();
        getActivity().invalidateOptionsMenu();

        if(mOnNewTaskCallback != null) {
            mOnNewTaskCallback.OnNewTaskCreated(newTask);
        }
    }

    private void startMove() {
        FileSystemTask newTask = mService.move(mSrcWD, mFileClipboard, mCurrentDir.getUri());
        mSrcWD = null;
        mFileClipboard.clear();
        getActivity().invalidateOptionsMenu();

        if(mOnNewTaskCallback != null) {
            mOnNewTaskCallback.OnNewTaskCreated(newTask);
        }
    }

    private void createFolder(String folderName) {
        mService.createFolder(mCurrentDir.getUri(), folderName);
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

        mCurrentDir = entry;
        refresh();
        mPathView.setCurrentDirectory(mCurrentDir);
    }

    public void refresh() {
        mService.query(mCurrentDir.getUri(), mRefreshCallback);
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

                    switchSelectedFileState(entryTag, v);
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

        getActivity().invalidateOptionsMenu();
    }

    private void navigateOrOpenFile(Entry entry) {
        if (entry.getType().equals(EntryType.CONTAINER)) {
            setCurrentDirectory(entry);
        } else {
            openFile(entry);
        }
    }

    private void openFile(final Entry entry) {
        String mimeType = URLConnection.guessContentTypeFromName(entry.getUri().toString());
        if (mimeType != null) {
            openFileWithType(entry, mimeType);
        } else {
            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setTitle("Select resource type")
                    .setItems(R.array.file_types, new DialogInterface.OnClickListener() {
                        public static final int VIDEO_TYPE = 0;
                        public static final int AUDIO_TYPE = 1;
                        public static final int IMAGE_TYPE = 2;
                        public static final int TEXT_TYPE = 3;
                        public static final int OTHER_TYPE = 4;

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch(which) {
                                case VIDEO_TYPE:
                                    openFileWithType(entry, "video/*");
                                    break;
                                case AUDIO_TYPE:
                                    openFileWithType(entry, "audio/*");
                                    break;
                                case IMAGE_TYPE:
                                    openFileWithType(entry, "image/*");
                                    break;
                                case TEXT_TYPE:
                                    openFileWithType(entry, "text/*");
                                    break;
                                default:
                                    openFileWithType(entry, "*/*");

                            }
                        }
                    })
                    .create();
            dialog.show();
        }
    }

    private void openFileWithType(Entry entry, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(entry.getUri(), mimeType);
        startActivity(intent);
    }
}
