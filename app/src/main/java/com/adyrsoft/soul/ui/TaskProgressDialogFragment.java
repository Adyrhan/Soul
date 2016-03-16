package com.adyrsoft.soul.ui;

import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.app.ProgressDialog;
import android.os.Bundle;

/**
 * Created by Adrian on 15/03/2016.
 */
public class TaskProgressDialogFragment extends DialogFragment {
    private ProgressDialog mDialog;
    private int mMaxProgress;
    private int mProgress;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mDialog = new ProgressDialog(getActivity());
        mDialog.setTitle("Copying files");
        mDialog.setMessage("Please wait...");
        mDialog.setIndeterminate(false);
        mDialog.setCancelable(false);
        mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mDialog.setMax(mMaxProgress);
        mDialog.setProgress(mProgress);

        return mDialog;
    }

    public void setMax(int max) {
        if (mDialog != null) {
            mDialog.setMax(max);
        } else {
            mMaxProgress = max;
        }
    }

    public void setProgress(int progress) {
        if (mDialog != null) {
            mDialog.setProgress(progress);
        } else {
            mProgress = progress;
        }
    }
}
