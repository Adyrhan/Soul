package com.adyrsoft.soul.ui;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.adyrsoft.soul.R;
import com.adyrsoft.soul.data.Entry;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by Adrian on 01/03/2016.
 */
public class DirectoryPathView extends FrameLayout {

    public interface OnPathSegmentSelectedListener {
        void OnPathSegmentSelected(DirectoryPathView pathView, View segmentView, Entry path);
    }

    private Context mContext;
    private AttributeSet mAttrs;
    private int mDefStyleAttr;
    private LinearLayout mContainer;
    private HorizontalScrollView mScrollView;
    private float mScreenDensity;
    private HashSet<OnPathSegmentSelectedListener> mListenerSet = new HashSet<>();

    public DirectoryPathView(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public DirectoryPathView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mAttrs = attrs;
        init();
    }

    public DirectoryPathView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        mAttrs = attrs;
        mDefStyleAttr = defStyleAttr;
        init();
    }

    public void addOnPathSegmentSelectedListener(OnPathSegmentSelectedListener listener) {
        mListenerSet.add(listener);
    }

    public void removeOnPathSegmentListener(OnPathSegmentSelectedListener listener) {
        mListenerSet.remove(listener);
    }

    private void init() {
        mScreenDensity = mContext.getResources().getDisplayMetrics().density;

        inflate(mContext, R.layout.directory_path_view_layout, this);

        Resources.Theme theme = mContext.getTheme();
        TypedArray a = theme.obtainStyledAttributes(mAttrs, R.styleable.DirectoryPathView, 0, 0);

        mContainer = (LinearLayout)findViewById(R.id.container_layout);
        mScrollView = (HorizontalScrollView)findViewById(R.id.scroll);

        if (isInEditMode()) {
            setCurrentDirectory(new Entry(Uri.parse("/foo/bar/lorem/ipsum"), Entry.EntryType.CONTAINER));
        }
    }

    private List<View> buildViewList(Entry path) {
        if (path != null) {
            Uri pathUri = path.getUri();
            File parentFile = new File(pathUri.getPath()).getParentFile();

            List<View> views = null;

            if (parentFile == null) {
                views = buildViewList(null);
            } else {
                Uri parentUri = new Uri.Builder()
                        .scheme(pathUri.getScheme())
                        .path(parentFile.getPath())
                        .encodedAuthority(pathUri.getEncodedAuthority())
                        .encodedFragment(pathUri.getEncodedFragment())
                        .encodedQuery(pathUri.getEncodedQuery())
                        .build();

                Entry parentPath = new Entry(parentUri, Entry.EntryType.CONTAINER);
                views = buildViewList(parentPath);
            }

            LinearLayout.LayoutParams separatorLayoutParams = new LinearLayout.LayoutParams(
                    (int)(mScreenDensity * 25f), LinearLayout.LayoutParams.MATCH_PARENT
            );

            LinearLayout.LayoutParams componentLayoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT
            );

            ImageView pathSeparatorView = new ImageView(mContext);
            pathSeparatorView.setImageResource(R.drawable.path_separator_light);
            pathSeparatorView.setLayoutParams(separatorLayoutParams);


            String folderName = path.getName();

            if (folderName.isEmpty()) {
                folderName = getContext().getString(R.string.root_path_segment_name);
            }

            TextView pathComponentView = new TextView(mContext);

            pathComponentView.setGravity(Gravity.CENTER_VERTICAL);

            pathComponentView.setText(folderName);
            pathComponentView.setTextColor(Color.WHITE);
            pathComponentView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            pathComponentView.setTypeface(Typeface.create("sans-serif-thin", Typeface.BOLD_ITALIC));

            pathComponentView.setTag(path);

            pathComponentView.setPadding((int) (5 * mScreenDensity), 0, (int) (5 * mScreenDensity), 0);


            if (!views.isEmpty()) {
                views.add(pathSeparatorView);
            } else {
                componentLayoutParams.leftMargin = (int)(mScreenDensity * 10f);
            }

            pathComponentView.setLayoutParams(componentLayoutParams);

            final DirectoryPathView pathView = this;
            pathComponentView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mListenerSet.isEmpty()) {
                        for (OnPathSegmentSelectedListener listener : mListenerSet) {
                            Entry path = (Entry)v.getTag();
                            listener.OnPathSegmentSelected(pathView, v, path);
                        }
                    }
                }
            });

            views.add(pathComponentView);
            return views;
        } else {
            ArrayList<View> viewList = new ArrayList<>();
            return viewList;
        }
    }

    public void setCurrentDirectory(Entry path) {
        mContainer.removeAllViews();
        List<View> views = buildViewList(path);
        int i = 0;
        for(View view : views) {
            if (i == views.size() - 1) {
                float density = mContext.getResources().getDisplayMetrics().density;

                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams)view.getLayoutParams();
                layoutParams.rightMargin = (int)(density * 10f);
                view.setLayoutParams(layoutParams);
            }
            mContainer.addView(view);
            i++;
        }
        invalidate();
        requestLayout();
        ViewTreeObserver vto = mScrollView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mScrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mScrollView.fullScroll(View.FOCUS_RIGHT);
            }
        });
    }


}
