package com.adyrsoft.soul.ui;

import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.app.ProgressDialog;
import android.os.Bundle;

/**
 * Created by Adrian on 15/03/2016.
 */
public class TaskProgressDialogFragment extends DialogFragment {
    private static final String STATE_MAX_PROGRESS = "STATE_MAX_PROGRESS";
    private static final String STATE_PROGRESS = "STATE_PROGRESS";
    private ProgressDialog mDialog;
    private int mMaxProgress;
    private int mProgress;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mProgress = savedInstanceState.getInt(STATE_PROGRESS);
            mMaxProgress = savedInstanceState.getInt(STATE_MAX_PROGRESS);
        }

        setCancelable(false);

        mDialog = new ProgressDialog(getActivity());
        mDialog.setTitle("Copying files");
        mDialog.setMessage("Please wait...");
        mDialog.setIndeterminate(false);
        mDialog.setCancelable(false);
        mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mDialog.setMax(mMaxProgress);

        return mDialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        mDialog.setProgress(mProgress);
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt(STATE_MAX_PROGRESS, mMaxProgress);
        out.putInt(STATE_PROGRESS, mProgress);
    }

    public void setMax(int max) {
        mMaxProgress = max;
        if (mDialog != null) {
            mDialog.setMax(max);
        }
    }

    public void setProgress(int progress) {
        mProgress = progress;
        if (mDialog != null) {
            mDialog.setProgress(progress);
        }
    }
}
