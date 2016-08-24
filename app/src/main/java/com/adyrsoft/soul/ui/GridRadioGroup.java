package com.adyrsoft.soul.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.view.View;
import android.widget.GridLayout;
import android.widget.RadioButton;


public class GridRadioGroup extends GridLayout {
    private static final String TAG = GridRadioGroup.class.getSimpleName();
    private int mCheckedId = -1;

    public GridRadioGroup(Context context) {
        super(context);
    }

    public GridRadioGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GridRadioGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public GridRadioGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public int getCheckedRadioButtonId() {
        return mCheckedId;
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);

        for(int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (view instanceof RadioButton) {
                final RadioButton button = (RadioButton)view;
                button.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        check(button.getId());
                    }
                });
            }
        }
    }

    public void check(int id) {
        mCheckedId = id;
        View view = findViewById(id);
        if (view instanceof RadioButton) {
            RadioButton button = (RadioButton)view;
            button.setChecked(true);
            uncheckOthers(button);
        }
    }

    private void uncheckOthers(RadioButton button) {
        for(int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (view instanceof RadioButton) {
                RadioButton button1 = (RadioButton)view;
                if (!button.equals(button1)) {
                    button1.setChecked(false);
                }
            }
        }
    }
}