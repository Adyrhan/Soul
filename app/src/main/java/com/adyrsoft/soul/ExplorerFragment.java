package com.adyrsoft.soul;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.Toast;

import com.adyrsoft.soul.ui.DirectoryPathView;
import com.adyrsoft.soul.ui.FileGridItemView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A placeholder fragment containing a simple view.
 */
public class ExplorerFragment extends Fragment implements DirectoryPathView.OnPathSegmentSelectedListener {
    private static final String TAG = ExplorerFragment.class.getName();
    private static final String DIRECTORY_FILES = "DIRECTORY_FILES";
    private static final String DIRECTORY_PARENT = "DIRECTORY_PARENT";

    private GridView mGridView;
    private FileGridAdapter mFileGridAdapter;
    private DirectoryPathView mPathView;
    private File mCurrentDir;

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

        if (savedInstanceState != null) {
            ArrayList<String> files = savedInstanceState.getStringArrayList(DIRECTORY_FILES);
            for(String file : files) {
                mFileGridAdapter.add(new File(file));
            }

            mCurrentDir = new File(savedInstanceState.getString(DIRECTORY_PARENT));
            mPathView.setCurrentDirectory(mCurrentDir);
        }

        return rootView;
    }

    public boolean onBackPressed() {
        return !changeCWDToParentDir();
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
    }

    public void setCurrentDirectory(String path) {
        File file = new File(path);
        setCurrentDirectory(file);
    }

    public void setCurrentDirectory(File dir) {
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException(dir.getPath() + " is not a directory");
        }

        mCurrentDir = dir;

        File[] files = dir.listFiles();

        mFileGridAdapter.clear();

        if (files != null && files.length > 0) {
            mFileGridAdapter.addAll(Arrays.asList(files));
        }

        mFileGridAdapter.notifyDataSetChanged();
        mPathView.setCurrentDirectory(dir);
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
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null || !(convertView instanceof FileGridItemView)) {
                convertView = new FileGridItemView(mContext);
            }

            FileGridItemView v = (FileGridItemView)convertView;

            File file = getItem(position);

            if (file.isDirectory()) {
                v.setIconResource(R.drawable.ic_folder_24dp);
            } else {
                v.setIconResource(R.drawable.ic_insert_drive_file_24px);
            }

            v.setFileName(file.getName());

            v.setTag(file);

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    File file = ((File) v.getTag());

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
            });

            return v;
        }

    }


}
