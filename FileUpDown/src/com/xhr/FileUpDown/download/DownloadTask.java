package com.xhr.FileUpDown.download;

import com.xhr.FileUpDown.NotificationConfig;
import com.xhr.FileUpDown.download.db.DataBaseManager;
import com.xhr.FileUpDown.download.utils.ListUtils;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Aspsine on 2015/4/20.
 */
public class DownloadTask {

    private static final int THREAD_NUM = 3;
    private final DownloadInfo mDownloadInfo;
    private final DataBaseManager mDBManager;
    private final ExecutorService mExecutorService;
    public NotificationConfig notificationConfig;
    private DownloadService service;
    private ConnectThread mConnectThread;
    private List<DownloadThread> mDownloadThreads = new LinkedList<DownloadThread>();
    private int mStatus = -1;
    private ExecutorService downloadTaskPool;

    private int maxRetries;

    public DownloadTask(DownloadService service, DownloadInfo downloadInfo, DataBaseManager dbManager, ExecutorService executorService, NotificationConfig notificationConfig) {
        this.mDownloadInfo = downloadInfo;
        this.mExecutorService = executorService;
        this.mDBManager = dbManager;
        this.service = service;
        this.notificationConfig = notificationConfig;
        this.downloadTaskPool = Executors.newFixedThreadPool(THREAD_NUM);
        maxRetries = downloadInfo.getMaxRetries();
    }

    /**
     * 启动下载任务
     */
    public void start() {
        if (!isStarted()) {
            mDownloadInfo.setFinished(0);
            mDownloadInfo.setLength(0);
            mConnectThread = new ConnectThread(this, mDownloadInfo);
            mExecutorService.execute(mConnectThread);
        }
    }

    /**
     * retry when download task fail
     */
    public void retry() {
        mDownloadInfo.setFinished(0);
        mDownloadInfo.setLength(0);
        //设置为start状态，避免开启新任务
        mStatus = DownloadStatus.STATUS_START;
        mExecutorService.execute(mConnectThread);
    }

    /**
     * 暂停下载任务
     */
    public void pause() {
        if (ListUtils.isEmpty(mDownloadThreads) || isAllPaused()) {
            return;
        }
        for (DownloadThread task : mDownloadThreads) {
            task.pause();
        }
    }

    public void cancel() {
        if (ListUtils.isEmpty(mDownloadThreads) || isAllCanceled()) {
            return;
        }
        for (DownloadThread task : mDownloadThreads) {
            task.cancel();
        }
    }

    public synchronized boolean isStarted() {
        return mStatus == DownloadStatus.STATUS_START
                || mStatus == DownloadStatus.STATUS_PROGRESS;
    }

    private boolean isAllPaused() {
        boolean allPaused = true;
        for (DownloadThread task : mDownloadThreads) {
            if (!task.isPaused()) {
                allPaused = false;
                break;
            }
        }
        return allPaused;
    }

    private boolean isAllCanceled() {
        boolean allCanceled = true;
        for (DownloadThread task : mDownloadThreads) {
            if (!task.isCanceled()) {
                allCanceled = false;
                break;
            }
        }
        return allCanceled;
    }

    /**
     * check if all threads finished download
     *
     * @return
     */
    private boolean isAllFinished() {
        boolean allFinished = true;
        for (DownloadThread task : mDownloadThreads) {
            if (!task.isComplete()) {
                allFinished = false;
                break;
            }
        }
        return allFinished;
    }

    private boolean isAllFailure() {
        boolean allFailure = true;
        for (DownloadThread task : mDownloadThreads) {
            if (!task.isFailure()) {
                allFailure = false;
                break;
            }
        }
        return allFailure;
    }

    protected void broadcastPause(String downloadId) {
        if (isAllPaused()) {
            mStatus = DownloadStatus.STATUS_PAUSE;
            this.service.broadcastPause(downloadId);
        }
    }

    protected void broadcastError(String downloadId, Exception ex) {
        if (isAllFailure()) {
            mStatus = DownloadStatus.STATUS_ERROR;
            //重试机制.通过变化RangeFlag实现MultiDownload和SingleDownload两种方式自动切换，避免只尝试一种方式而导致假失败
            //即使用户设置了尝试次数为0时，也默认重试一下。
            if (maxRetries >= 0) {
                maxRetries--;
                mConnectThread.setRangeFlag(!mConnectThread.getRangeFlag());
                mDownloadThreads.clear();
                retry();
            } else {
                this.service.broadcastError(downloadId, ex);
            }
        }
    }

    protected void broadcastProgress(String downloadId, final long downloadBytes, final long totalBytes) {
        mStatus = DownloadStatus.STATUS_PROGRESS;
        this.service.broadcastProgress(downloadId, downloadBytes, totalBytes);
    }

    protected void broadcastComplete(String downloadId, final int responseCode, final String responseMessage) {
        if (isAllFinished()) {
            mDBManager.delete(mDownloadInfo.getUrl());
            mStatus = DownloadStatus.STATUS_COMPLETED;
            this.service.broadcastCompleted(downloadId, responseCode, responseMessage);
        }
    }

    protected void broadcastCancel(String downloadId, Exception ex) {
        if (isAllCanceled()) {
            mDBManager.delete(mDownloadInfo.getUrl());
            File file = new File(mDownloadInfo.getDir(), mDownloadInfo.getName());
            if (file.exists() && file.isFile()) {
                file.delete();
            }
            mStatus = DownloadStatus.STATUS_CANCEL;
            this.service.broadcastCancel(downloadId, ex);
        }
    }

    protected void broadcastStart(String downloadId) {
        mStatus = DownloadStatus.STATUS_START;
        this.service.broadcastStarted(downloadId);
    }


    protected void download(DownloadInfo downloadInfo) {

        if (downloadInfo.isSupportRange()) {
            //multi thread
            List<ThreadInfo> threadInfos = getMultiThreadInfos();
            // init finished
            int finished = 0;
            for (ThreadInfo threadInfo : threadInfos) {
                finished += threadInfo.getFinished();
            }
            mDownloadInfo.setFinished(finished);
            // init tasks
            for (ThreadInfo threadInfo : threadInfos) {
                DownloadThread task = new MultiDownloadThread(this, downloadInfo, threadInfo, mDBManager);
                mDownloadThreads.add(task);
            }
        } else {
            //single thread
            ThreadInfo threadInfo = getSingleThreadInfo();
            DownloadThread task = new SingleDownloadThread(this, downloadInfo, threadInfo);
            mDownloadThreads.add(task);
        }
        // start tasks
        for (DownloadThread downloadThread : mDownloadThreads) {
            downloadTaskPool.execute(downloadThread);
        }
    }

    private List<ThreadInfo> getMultiThreadInfos() {
        // init threadInfo from db
        List<ThreadInfo> threadInfos = mDBManager.getThreadInfos(mDownloadInfo.getUrl());
        if (threadInfos.isEmpty()) {
            for (int i = 0; i < THREAD_NUM; i++) {
                // calculate average
                final long average = mDownloadInfo.getLength() / THREAD_NUM;
                long end = 0;
                long start = average * i;
                if (i == THREAD_NUM - 1) {
                    end = mDownloadInfo.getLength();
                } else {
                    end = start + average - 1;
                }
                ThreadInfo threadInfo = new ThreadInfo(i, mDownloadInfo.getId(), mDownloadInfo.getUrl(), start, end, 0);
                threadInfos.add(threadInfo);
            }
        }
        return threadInfos;
    }

    public ThreadInfo getSingleThreadInfo() {
        ThreadInfo threadInfo = new ThreadInfo(0, mDownloadInfo.getId(), mDownloadInfo.getUrl(), 0);
        return threadInfo;
    }


}
