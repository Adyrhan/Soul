package com.adyrsoft.soul.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.adyrsoft.soul.R;
import com.adyrsoft.soul.RequestFileTransferServiceCallback;
import com.adyrsoft.soul.SoulApplication;
import com.adyrsoft.soul.service.FileSystemTask;
import com.adyrsoft.soul.service.FileTransferService;
import com.adyrsoft.soul.service.ProgressInfo;
import com.adyrsoft.soul.service.TaskResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Adrian on 16/06/2016.
 */
public class BackgroundTasksFragment extends Fragment implements RequestFileTransferServiceCallback, FileTransferService.TaskProgressListener {
    private static final String TAG = BackgroundTasksFragment.class.getSimpleName();
    private RecyclerView mRecyclerView;
    private TaskRecyclerAdapter mAdapter = new TaskRecyclerAdapter();
    private TextView mNoItemsNotice;
    private FileTransferService mTransferService;

    private RecyclerView.AdapterDataObserver mDataObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            notifyEmptyListMaybe();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            notifyEmptyListMaybe();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            notifyEmptyListMaybe();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            notifyEmptyListMaybe();
        }
    };

    private void notifyEmptyListMaybe() {
        int visibility = mAdapter.getItemCount() > 0 ? View.GONE : View.VISIBLE;
        mNoItemsNotice.setVisibility(visibility);
        Log.d(TAG, "There are " + mAdapter.getItemCount() + "tasks in adapter");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_background_tasks, viewGroup, false);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mNoItemsNotice = (TextView) rootView.findViewById(R.id.no_items_notice);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.registerAdapterDataObserver(mDataObserver);
        ((SoulApplication) getActivity().getApplication()).requestFileTransferService(this);
    }

    @Override
    public void onServiceReady(FileTransferService transferService) {
        mTransferService = transferService;
        mTransferService.addTaskProgressListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mTransferService != null) {
            mTransferService.removeTaskProgressListener(this);
        }
        mAdapter.unregisterAdapterDataObserver(mDataObserver);
    }

    @Override
    public void onSubscription(HashMap<FileSystemTask, ProgressInfo> pendingTasks) {
        for (Map.Entry<FileSystemTask, ProgressInfo> entry : pendingTasks.entrySet()) {
            mAdapter.updateTask(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void onProgressUpdate(FileSystemTask task, ProgressInfo info) {
        mAdapter.updateTask(task, info);
    }

    @Override
    public void onTaskFinished(FileSystemTask task, TaskResult result) {
        mAdapter.removeTask(task);
    }

    public static class TaskRecyclerAdapter extends RecyclerView.Adapter<ProgressInfoViewHolder> {
        private HashMap<FileSystemTask, ProgressInfo> mTaskProgressTable = new HashMap<>();
        private ArrayList<FileSystemTask> mTaskList = new ArrayList<>();

        @Override
        public ProgressInfoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater
                    .from(parent.getContext())
                    .inflate(R.layout.task_progress_recyclerview_item, parent, false);

            return new ProgressInfoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ProgressInfoViewHolder holder, int position) {
            FileSystemTask task = mTaskList.get(position);
            ProgressInfo info = mTaskProgressTable.get(task);

            String taskOp = "";
            switch(task.getFileOperation()) {
                case COPY:
                    taskOp = "Copying";
                    break;
                case MOVE:
                    taskOp = "Moving";
                    break;
                case REMOVE:
                    taskOp = "Removing";
                    break;
            }

            String fileWord = info.getTotalFiles() > 1 ? "files" : "file";
            String taskDesc = String.format(Locale.US, "%s %d %s from", taskOp, info.getTotalFiles(), fileWord);

            holder.taskDescription.setText(taskDesc);
            holder.from.setText(info.getSource().toString());
            holder.to.setText(info.getDest().toString());

            if (info.getDest() == null) {
                holder.toLabel.setVisibility(View.GONE);
                holder.to.setVisibility(View.GONE);
            }

            holder.progressBar.setMax(info.getTotalFiles());
            holder.progressBar.setProgress(info.getProcessedFiles());
        }

        @Override
        public int getItemCount() {
            return mTaskList.size();
        }

        public void updateTask(FileSystemTask task, ProgressInfo info) {
            mTaskProgressTable.put(task, info);
            if (mTaskList.indexOf(task) == -1) {
                mTaskList.add(task);
                notifyItemInserted(mTaskList.size()-1);
            } else {
                notifyItemChanged(mTaskList.indexOf(task));
            }
        }

        public void removeTask(FileSystemTask task) {
            int index = mTaskList.indexOf(task);
            mTaskProgressTable.remove(task);
            mTaskList.remove(task);
            notifyItemRemoved(index);
        }

        public void clear() {
            mTaskProgressTable.clear();
            mTaskList.clear();
            notifyDataSetChanged();
        }
    }

    public static class ProgressInfoViewHolder extends RecyclerView.ViewHolder {
        public View rootView;
        public ProgressBar progressBar;
        public TextView from;
        public TextView to;
        public TextView toLabel;
        public TextView taskDescription;

        public ProgressInfoViewHolder(View view) {
            super(view);
            rootView = view;
            progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
            from = (TextView) view.findViewById(R.id.from);
            to = (TextView) view.findViewById(R.id.to);
            toLabel = (TextView) view.findViewById(R.id.to_label);
            taskDescription = (TextView) view.findViewById(R.id.task_description);
        }
    }
}
