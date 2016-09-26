package com.adyrsoft.soul.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.adyrsoft.soul.R;

import java.io.File;

/**
 * Created by Adrian on 04/03/2016.
 */

public class FileGridItemView extends LinearLayout {
    private int mDefStyleAttr;
    private AttributeSet mAttributeSet;
    private float mScreenDensity;
    private ImageView mIconView;
    private TextView mFileNameView;

    public FileGridItemView(Context context) {
        super(context);
        init();
    }

    public FileGridItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mAttributeSet = attrs;
        init();
    }

    public FileGridItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mAttributeSet = attrs;
        mDefStyleAttr = defStyleAttr;
        init();
    }

    private void init() {
        mScreenDensity = getContext().getResources().getDisplayMetrics().density;

        AbsListView.LayoutParams params = new AbsListView.LayoutParams(
                AbsListView.LayoutParams.MATCH_PARENT, (int)(85 * mScreenDensity)
        );

        setPadding((int)(5 * mScreenDensity), 0, (int)(5 * mScreenDensity), 0);
        setGravity(Gravity.CENTER_HORIZONTAL);
        setOrientation(VERTICAL);
        setBackgroundResource(R.drawable.file_grid_item_view_drawable);

        inflate(getContext(), R.layout.file_grid_item_icon, this);

        mIconView = (ImageView)findViewById(R.id.icon_view);
        mFileNameView = (TextView)findViewById(R.id.file_name_view);

        setLayoutParams(params);

        invalidate();
        requestLayout();
    }

    public void setFileName(String fileName) {
        mFileNameView.setText(fileName);
    }

    public void setIconResource(int resId) {
        mIconView.setImageResource(resId);
    }

    public void setIcon(Drawable drawable) {
        mIconView.setImageDrawable(drawable);
    }

    public void setIconUri(Uri uri) {
        mIconView.setImageURI(uri);
    }

}
