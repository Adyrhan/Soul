package com.adyrsoft.soul;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * This fragment is a full blown file explorer. It changes the menu options on the activity to show
 * extra operations.
 */
public class ExplorerFragment extends Fragment implements DirectoryPathView.OnPathSegmentSelectedListener, RequestFileTransferServiceCallback {
    private enum ExplorerState {
        NAVIGATION,
        SELECTION,
        UNREADY
    }

    public static final String DIRECTORY_PARENT = "DIRECTORY_PARENT";

    private static final String EXPLORER_STATE = "EXPLORER_STATE";
    private static final String TAG = ExplorerFragment.class.getName();
    private static final String DIRECTORY_FILES = "DIRECTORY_FILES";
    private static final String SELECTED_FILES = "SELECTED_FILES";

    private GridView mGridView;
    private FileGridAdapter mFileGridAdapter;
    private DirectoryPathView mPathView;
    private File mCurrentDir;
    private Uri mSrcWD;
    private ExplorerState mExplorerState;
    private HashSet<File> mSelectedFileSet = new HashSet<>();
    private FileTransferService mService;
    private Menu mMenu;
    private TaskProgressDialogFragment mProgressDialogFragment;
    public List<Uri> mToCopy = new ArrayList<>();


    public ExplorerFragment() {
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

            mCurrentDir = new File(savedInstanceState.getString(DIRECTORY_PARENT));
            mPathView.setCurrentDirectory(mCurrentDir);

            ArrayList<String> selectedFiles = savedInstanceState.getStringArrayList(SELECTED_FILES);
            for(String file: selectedFiles) {
                mSelectedFileSet.add(new File(file));
            }

            ArrayList<String> files = savedInstanceState.getStringArrayList(DIRECTORY_FILES);
            for(String file : files) {
                mFileGridAdapter.add(new File(file));
            }
        } else if (getArguments() != null ){
            String currentDirPath = getArguments().getString(DIRECTORY_PARENT);
            if (currentDirPath != null) {
                mCurrentDir = new File(currentDirPath);
            }
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

                return true;
            case R.id.copy:
                mToCopy.clear();

                for(File file : mSelectedFileSet) {
                    mToCopy.add(Uri.fromFile(file));
                }

                mSrcWD = Uri.fromFile(mCurrentDir);

                stateChange(ExplorerState.NAVIGATION);

                Toast.makeText(getActivity(), "Selected to copy " + mToCopy.size() + " files", Toast.LENGTH_SHORT).show();

                return true;

            case R.id.paste:
                mService.copy(mSrcWD, mToCopy, Uri.fromFile(mCurrentDir));
                return true;

            case R.id.remove:
                ArrayList<Uri> toRemove = new ArrayList<>();
                for (File fileEntry : mSelectedFileSet) {
                    toRemove.add(Uri.fromFile(fileEntry));
                }

                mService.remove(Uri.fromFile(mCurrentDir), toRemove);

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void createFolder(String folderName) {
        File newFolder = new File(mCurrentDir, folderName);
        if (newFolder.exists()) {
            Toast.makeText(getActivity(), "Folder already exists", Toast.LENGTH_LONG).show();
        } else {
            boolean success = newFolder.mkdirs();
            if (!success) {
                Toast.makeText(getActivity(), "Couldn't create folder", Toast.LENGTH_LONG).show();
            } else {
                refresh();
            }
        }
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
        File externalStorageRoot = Environment.getExternalStorageDirectory();

        String externalStoragePath = externalStorageRoot.getPath();
        String parentPath = mCurrentDir.getParent();

        if (parentPath != null && parentPath.indexOf(externalStoragePath) >= 0) {
            setCurrentDirectory(mCurrentDir.getParentFile());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);

        ArrayList<String> files = new ArrayList<>();
        for(int i = 0; i < mFileGridAdapter.getCount(); i++) {
            files.add(mFileGridAdapter.getItem(i).getPath());
        }
        out.putStringArrayList(DIRECTORY_FILES, files);

        out.putString(DIRECTORY_PARENT, mCurrentDir.getPath());

        ArrayList<String> selectedFiles = new ArrayList<>();
        for(File file : mSelectedFileSet) {
            selectedFiles.add(file.getPath());
        }
        out.putStringArrayList(SELECTED_FILES, selectedFiles);

        out.putInt(EXPLORER_STATE, mExplorerState.ordinal());
    }

    public void setCurrentDirectory(@NonNull String path) {
        File file = new File(path);
        setCurrentDirectory(file);
    }

    public void setCurrentDirectory(@NonNull File dir) {
        if (mExplorerState == ExplorerState.UNREADY) {
            return;
        }

        if (!dir.isDirectory()) {
            throw new IllegalArgumentException(dir.getPath() + " is not a directory");
        }

        mCurrentDir = dir;

        refresh();

        mPathView.setCurrentDirectory(dir);
    }

    public void refresh() {
        File[] files = mCurrentDir.listFiles();

        mFileGridAdapter.clear();

        if (files != null && files.length > 0) {
            mFileGridAdapter.addAll(Arrays.asList(files));
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
    public void OnPathSegmentSelected(DirectoryPathView pathView, View segmentView, File path) {
        setCurrentDirectory(path);
    }

    class FileGridAdapter extends ArrayAdapter<File> {
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

            File file = getItem(position);

            if (file.isDirectory()) {
                v.setIconResource(R.drawable.ic_folder_24dp);
            } else {
                v.setIconResource(R.drawable.ic_insert_drive_file_24px);
            }

            v.setFileName(file.getName());

            v.setTag(file);

            if (mSelectedFileSet.contains(file)) {
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
                    File file = ((File) v.getTag());

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

                    File file = ((File) v.getTag());

                    boolean select = !mSelectedFileSet.contains(file);

                    if (select) {
                        v.setSelected(true);
                        mSelectedFileSet.add(file);
                    }
                    return true;
                }
            });

            return v;
        }

    }

    private void switchSelectedFileState(File file, View v) {
        boolean select = !mSelectedFileSet.contains(file);

        v.setSelected(select);

        if (select) {
            mSelectedFileSet.add(file);
        } else {
            mSelectedFileSet.remove(file);
        }
    }

    private void navigateOrOpenFile(File file) {
        if (file.isDirectory()) {
            setCurrentDirectory(file);
        } else {
            try {
                String mimeType = URLConnection.guessContentTypeFromStream(new FileInputStream(file));

                if (mimeType == null) {
                    MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
                    String fileName = file.getName();
                    String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
                    mimeType = mimeTypeMap.getMimeTypeFromExtension(extension);
                }

                if (mimeType == null) {
                    Toast.makeText(getActivity(), "Unknown file type", Toast.LENGTH_LONG).show();
                } else {
                    Log.d(TAG, "Guessed mimetype for file is " + mimeType);
                    Intent fileOpenIntent = new Intent(Intent.ACTION_VIEW);

                    fileOpenIntent.setDataAndType(Uri.fromFile(file), mimeType);

                    startActivity(fileOpenIntent);
                }

            } catch (FileNotFoundException e) {
                Toast.makeText(getActivity(), "Couldn't open the file. It has been deleted.", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Toast.makeText(getActivity(), "Read error", Toast.LENGTH_LONG).show();
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, null, e);
                Toast.makeText(getActivity(), "There isn't an app installed that can handle this file type", Toast.LENGTH_LONG).show();
            }

        }
    }


}
