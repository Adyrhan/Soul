package com.adyrsoft.soul.ui;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.adyrsoft.soul.R;
import com.adyrsoft.soul.service.Solution;
import com.adyrsoft.soul.service.UserFeedbackProvider;

/**
 * Error dialog meant to be used in case of a recoverable error happening while processing a FileSystemTask
 */
public class FileSystemErrorDialog extends DialogFragment {
    public interface OnFeedbackProvidedCallback {
        void onFeedbackProvided();
    }

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
    private OnFeedbackProvidedCallback mFeedbackProvidedCallback;

    public FileSystemErrorDialog() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (mFeedbackProvider == null) {
            throw new IllegalStateException("An UserFeedbackProvider object must be set before showing this dialog");
        }

        setCancelable(false);

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

        RadioButton retryButton = (RadioButton) mRootView.findViewById(R.id.retry_option);
        RadioButton ignoreButton = (RadioButton) mRootView.findViewById(R.id.ignore_option);
        RadioButton cancelButton = (RadioButton) mRootView.findViewById(R.id.cancel_option);

        final Button okButton = (Button) mRootView.findViewById(R.id.ok);

        if (mAffectedFile != null) {
            mAffectedFileView.setText(mAffectedFile.getPath());
        }

        if (mErrorDescription != null) {
            mErrorDescriptionView.setText(mErrorDescription);
        }

        final RadioGroup actionGroup = (RadioGroup) mRootView.findViewById(R.id.radio_action);

        okButton.setEnabled(false);

        actionGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId != -1) {
                    okButton.setEnabled(true);
                }
            }
        });

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSolution = new Solution();

                int checkedId = actionGroup.getCheckedRadioButtonId();

                switch(checkedId) {
                    case R.id.retry_option:
                        mSolution.setAction(Solution.Action.RETRY_CONTINUE);
                        break;
                    case R.id.ignore_option:
                        mSolution.setAction(Solution.Action.IGNORE);
                        break;
                    case R.id.cancel_option:
                        mSolution.setAction(Solution.Action.CANCEL);
                        break;
                }

                sendFeedback();
                if (mFeedbackProvidedCallback != null) {
                    mFeedbackProvidedCallback.onFeedbackProvided();
                }
                dismiss();
            }
        });


        retryButton.setText(retryButtonLabel);
        ignoreButton.setText(ignoreButtonLabel);
        cancelButton.setText(cancelButtonLabel);

        return mRootView;
    }

    private void sendFeedback() {
        mFeedbackProvider.provideFeedback(mSolution);
    }


    public void setUserFeedbackProvider(UserFeedbackProvider provider) {
        mFeedbackProvider = provider;
    }

    public void setOnFeedbackProvidedCallback(OnFeedbackProvidedCallback callback) {
        mFeedbackProvidedCallback = callback;
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
