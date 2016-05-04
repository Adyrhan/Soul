package com.adyrsoft.soul.ui;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.adyrsoft.soul.R;
import com.adyrsoft.soul.service.Solution;
import com.adyrsoft.soul.service.UserFeedbackProvider;

/**
 * Error dialog meant to be used in case of a recoverable error happening while processing a FileSystemTask
 */
public class FileSystemErrorDialog extends DialogFragment {
    private View mRootView;
    private Uri mAffectedFile;
    private String mErrorDescription;
    private TextView mAffectedFileView;
    private TextView mErrorDescriptionView;
    private Solution mSolution;
    private UserFeedbackProvider mFeedbackProvider;
    private String mRetryButtonLabel;
    private String mIgnoreButtonLabel;
    private String mCancelButtonLabel;

    public FileSystemErrorDialog() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (mFeedbackProvider == null) {
            throw new IllegalStateException("An UserFeedbackProvider object must be set before showing this dialog");
        }

        mRootView = inflater.inflate(R.layout.filesystem_error_dialog, container, false);

        String retryButtonLabel = (mRetryButtonLabel != null) ?
                mRetryButtonLabel :
                getActivity().getString(R.string.retry_filesystem_error_dialog_button_label);

        String ignoreButtonLabel = (mIgnoreButtonLabel != null) ?
                mIgnoreButtonLabel :
                getActivity().getString(R.string.ignore_filesystem_error_dialog_button_label);

        String cancelButtonLabel = (mCancelButtonLabel != null) ?
                mCancelButtonLabel :
                getActivity().getString(R.string.cancel_filesystem_error_dialog_button_label);

        mAffectedFileView = (TextView) mRootView.findViewById(R.id.affected_file);
        mErrorDescriptionView = (TextView) mRootView.findViewById(R.id.error_description);

        Button retryButton = (Button) mRootView.findViewById(R.id.retry_button);
        Button ignoreButton = (Button) mRootView.findViewById(R.id.ignore_button);
        Button cancelButton = (Button) mRootView.findViewById(R.id.cancel_button);

        if (mAffectedFile != null) {
            mAffectedFileView.setText(mAffectedFile.getPath());
        }

        if (mErrorDescription != null) {
            mErrorDescriptionView.setText(mErrorDescription);
        }

        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSolution = new Solution();
                mSolution.setAction(Solution.Action.RETRY_CONTINUE);
                sendFeedback();
                dismiss();
            }
        });

        retryButton.setText(retryButtonLabel);

        ignoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSolution = new Solution();
                mSolution.setAction(Solution.Action.IGNORE);
                sendFeedback();
                dismiss();
            }
        });

        ignoreButton.setText(ignoreButtonLabel);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSolution = new Solution();
                mSolution.setAction(Solution.Action.CANCEL);
                sendFeedback();
                dismiss();
            }
        });

        cancelButton.setText(cancelButtonLabel);

        return mRootView;
    }

    private void sendFeedback() {
        mFeedbackProvider.provideFeedback(mSolution);
    }


    public void setUserFeedbackProvider(UserFeedbackProvider provider) {
        mFeedbackProvider = provider;
    }

    public void setErrorDescription(String msg) {
        mErrorDescription = msg;
        if (mErrorDescriptionView != null) {
            mErrorDescriptionView.setText(mErrorDescription);
        }
    }

    public void setAffectedFile(Uri fileUri) {
        mAffectedFile = fileUri;
        if (mAffectedFileView != null) {
            mAffectedFileView.setText(mAffectedFile.getPath());
        }
    }

    public void setRetryButtonLabel(String retryButtonLabel) { mRetryButtonLabel = retryButtonLabel; }
    public void setIgnoreButtonLabel(String ignoreButtonLabel) { mIgnoreButtonLabel = ignoreButtonLabel; }
    public void setCancelButtonLabel(String cancelButtonLabel) { mCancelButtonLabel = cancelButtonLabel; }

}
