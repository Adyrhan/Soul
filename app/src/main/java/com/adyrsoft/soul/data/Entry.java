package com.adyrsoft.soul.data;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.util.Comparator;

/**
 * Represents the entry in an abstract filesystem. This can be in the local filesystem, smb/cifs, ssh...etc.
 */
public class Entry implements Parcelable {
    public enum EntryType {
        FILE,
        CONTAINER // Folder, host, workgroup...etc.
    }

    public enum Field {
        NAME,
        EXTENSION
    }

    public static class Comparator implements java.util.Comparator<Entry> {
        private final boolean mAscending;
        private final Field mField;

        public Comparator(Field field, boolean ascending) {
            mField = field;
            mAscending = ascending;
        }

        @Override
        public int compare(Entry lhs, Entry rhs) {
            int order;
            switch(mField) {
                case NAME:
                    order = lhs.getName().compareTo(rhs.getName());
                    return mAscending ? order : -order;
                case EXTENSION:
                    boolean lhsExt = lhs.getExtension() != null;
                    boolean rhsExt = rhs.getExtension() != null;
                    if (lhsExt && rhsExt) {
                        order = lhs.getExtension().compareTo(rhs.getExtension());
                        return mAscending ? order : -order;
                    } else if (lhsExt) {
                        return mAscending ? 1 : -1;
                    } else if (rhsExt) {
                        return mAscending ? -1 : 1;
                    } else {
                        return 0;
                    }
            }
            return 0;
        }
    }

    private Uri mUri;
    private EntryType mType;
    private String mName;
    private String mExt;

    public Entry(Uri resourceUri, EntryType type) {
        mType = type;
        mUri = Uri.parse(resourceUri.toString());
        File path = new File(resourceUri.getPath());
        mName = path.getName();

        int dotIdx = path.getName().lastIndexOf(".");
        if (dotIdx >= 0) {
            mExt = path.getName().substring(dotIdx);
        }
    }

    public String getName() {
        return mName;
    }

    public String getExtension() {
        return mExt;
    }

    public Uri getUri() {
        return mUri;
    }

    public EntryType getType() {
        return mType;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mUri, flags);
        dest.writeInt(this.mType == null ? -1 : this.mType.ordinal());
        dest.writeString(this.mName);
        dest.writeString(this.mExt);
    }

    protected Entry(Parcel in) {
        this.mUri = in.readParcelable(Uri.class.getClassLoader());
        int tmpMType = in.readInt();
        this.mType = tmpMType == -1 ? null : EntryType.values()[tmpMType];
        this.mName = in.readString();
        this.mExt = in.readString();
    }

    public static final Parcelable.Creator<Entry> CREATOR = new Parcelable.Creator<Entry>() {
        @Override
        public Entry createFromParcel(Parcel source) {
            return new Entry(source);
        }

        @Override
        public Entry[] newArray(int size) {
            return new Entry[size];
        }
    };
}
