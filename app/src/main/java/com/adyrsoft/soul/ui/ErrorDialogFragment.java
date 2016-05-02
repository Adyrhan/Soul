package com.adyrsoft.soul.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;

import com.adyrsoft.soul.R;

/**
 * General purpose error dialog. Allows to show an error description and an OK button.
 */
public class ErrorDialogFragment extends DialogFragment{

    private AlertDialog mDialog;
    private String mMsg;

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mDialog = new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_error_24dp)
                .setTitle(R.string.error)
                .setMessage(mMsg)
                .setPositiveButton(android.R.string.ok, null)
                .create();

        return mDialog;
    }

    public void setErrorMessage(String msg) {
        mMsg = msg;
        if (mDialog != null) {
            mDialog.setMessage(mMsg);
        }
    }

}
