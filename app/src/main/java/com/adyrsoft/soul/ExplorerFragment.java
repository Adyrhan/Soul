package com.adyrsoft.soul;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A placeholder fragment containing a simple view.
 */
public class ExplorerFragment extends Fragment {
    private static final String DIRECTORY_FILES = "DIRECTORY_FILES";
    private static final String DIRECTORY_PARENT = "DIRECTORY_PARENT";

    private GridView mGridView;
    private FileGridAdapter mFileGridAdapter;
    private File mCurrentDir;

    public ExplorerFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_explorer, container, false);
        mGridView = (GridView)rootView.findViewById(R.id.file_grid);
        mFileGridAdapter = new FileGridAdapter(getActivity());
        mGridView.setAdapter(mFileGridAdapter);

        if (savedInstanceState != null) {
            ArrayList<String> files = savedInstanceState.getStringArrayList(DIRECTORY_FILES);
            for(String file : files) {
                mFileGridAdapter.add(new File(file));
            }

            mCurrentDir = new File(savedInstanceState.getString(DIRECTORY_PARENT));
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
        if (parentPath.indexOf(externalStoragePath) >= 0) {
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
            View v = convertView;

            if (v == null || !(v instanceof LinearLayout)) {
                v = mInflater.inflate(R.layout.file_grid_item_icon, parent, false);
                ImageView iconView = (ImageView)v.findViewById(R.id.icon_view);
                TextView fileNameView = (TextView)v.findViewById(R.id.file_name_view);
                FileGridViewHolder viewHolder = new FileGridViewHolder();
                viewHolder.fileNameView = fileNameView;
                viewHolder.iconView = iconView;
                v.setTag(viewHolder);
            }

            FileGridViewHolder viewHolder = (FileGridViewHolder)v.getTag();

            File file = getItem(position);

            viewHolder.fileObject = file;

            ImageView iconView = viewHolder.iconView;
            TextView fileNameView = viewHolder.fileNameView;

            if (file.isDirectory()) {
                iconView.setImageResource(R.drawable.ic_folder_black_48dp);
            } else {
                iconView.setImageResource(R.drawable.ic_insert_drive_file_black_48dp);
            }

            fileNameView.setText(file.getName());

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    File file = ((FileGridViewHolder)v.getTag()).fileObject;
                    if (file.isDirectory()) {
                        setCurrentDirectory(file);
                    }
                }
            });

            return v;
        }
    }

    class FileGridViewHolder {
        public ImageView iconView;
        public TextView fileNameView;
        public File fileObject;
    }
}
