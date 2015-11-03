package com.xhr.FileUpDown.download.db;

import android.content.Context;
import com.xhr.FileUpDown.download.ThreadInfo;

import java.util.List;

/**
 * Created by aspsine on 15-4-19.
 */
public class DataBaseManager {
    private static DataBaseManager sDataBaseManager;
    private final ThreadInfoDao mThreadInfoDao;

    public static DataBaseManager getInstance(Context context) {
        if (sDataBaseManager == null) {
            sDataBaseManager = new DataBaseManager(context);
        }
        return sDataBaseManager;
    }

    private DataBaseManager(Context context) {
        mThreadInfoDao = new ThreadInfoDao(context);
    }

    public synchronized void insert(ThreadInfo threadInfo) {
        mThreadInfoDao.insert(threadInfo);
    }

    public synchronized void delete(String downloadId) {
        mThreadInfoDao.delete(downloadId);
    }

    public synchronized void update(String downloadId, int threadId, long finished) {
        mThreadInfoDao.update(downloadId, threadId, finished);
    }

    public List<ThreadInfo> getThreadInfos(String downloadId) {
        return mThreadInfoDao.getThreadInfos(downloadId);
    }

    public boolean exists(String downloadId, int threadId) {
        return mThreadInfoDao.exists(downloadId, threadId);
    }
}
