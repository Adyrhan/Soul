package com.adyrsoft.soul.service;

import com.adyrsoft.soul.data.Entry;

import java.util.List;

public interface QueryResultCallback {
    void onQueryCompleted(List<Entry> entries);
    void onQueryFailed(Exception ex);
}
