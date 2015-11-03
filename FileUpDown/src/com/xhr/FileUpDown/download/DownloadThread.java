package com.xhr.FileUpDown.download;


import com.xhr.FileUpDown.download.utils.IOCloseUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

/**
 * Created by xhrong on 2015/10/27.
 */
public abstract class DownloadThread implements Runnable {

    private String downloadId;
    private final DownloadInfo mDownloadInfo;
    private final ThreadInfo mThreadInfo;
    private DownloadTask downloadTask;

    private HttpURLConnection mHttpConn;

    private volatile int mStatus;

    private boolean cancelFlag=false;
    private boolean pauseFlag=false;

    DownloadThread(DownloadTask downloadTask, DownloadInfo mDownloadInfo, ThreadInfo mThreadInfo) {
        this.downloadId = mDownloadInfo.getId();
        this.mDownloadInfo = mDownloadInfo;
        this.mThreadInfo = mThreadInfo;
        this.downloadTask = downloadTask;
    }

    protected void setStatus(int status) {
        this.mStatus = status;
    }

    protected int getStatus() {
        return mStatus;
    }


    public void cancel() {
        cancelFlag=true;
    //    mStatus = DownloadStatus.STATUS_CANCEL;
//        Thread.currentThread().interrupt();
//        if (mHttpConn != null) {
//            mHttpConn.disconnect();
//        }
    }


    public void pause() {
        pauseFlag=true;
  //      mStatus = DownloadStatus.STATUS_PAUSE;
        //    Thread.currentThread().interrupt();
//        if (mHttpConn != null) {
//            mHttpConn.disconnect();
//        }
    }

    public boolean isStarted() {
        return mStatus == DownloadStatus.STATUS_START;
    }

    public boolean isDownloading() {
        return mStatus == DownloadStatus.STATUS_PROGRESS;
    }

    public boolean isComplete() {
        return mStatus == DownloadStatus.STATUS_COMPLETED;
    }

    public boolean isPaused() {
        return mStatus == DownloadStatus.STATUS_PAUSE;
    }

    public boolean isCanceled() {
        return mStatus == DownloadStatus.STATUS_CANCEL;
    }

    public boolean isFailure() {
        return mStatus == DownloadStatus.STATUS_ERROR;
    }

    public void run() {
        insertIntoDB(mThreadInfo);
        InputStream inputStream = null;
        RandomAccessFile raf = null;
        try {
            URL url = new URL(mThreadInfo.getUrl());
            mHttpConn = (HttpURLConnection) url.openConnection();
            mHttpConn.setConnectTimeout(DownloadService.CONNECT_TIME_OUT);
            mHttpConn.setRequestMethod(DownloadService.GET);
            setHttpHeader(getHttpHeaders(mThreadInfo), mHttpConn);
            raf = getFile(mThreadInfo, mDownloadInfo);
            final int responseCode = mHttpConn.getResponseCode();
            final String responseMessage = mHttpConn.getResponseMessage();

            if (responseCode == getResponseCode()) {
                inputStream = new BufferedInputStream(mHttpConn.getInputStream());
                byte[] buffer = new byte[1024 * 16];
                int len = -1;
                while ((len = inputStream.read(buffer)) != -1 && !(cancelFlag || pauseFlag)) {
                    raf.write(buffer, 0, len);
                    mThreadInfo.setFinished(mThreadInfo.getFinished() + len);
                    mDownloadInfo.setFinished(mDownloadInfo.getFinished() + len);
                    mStatus = DownloadStatus.STATUS_PROGRESS;
                    synchronized (downloadTask) {
                        broadcastProgress(mDownloadInfo.getFinished(), mDownloadInfo.getLength());
                    }
                }
                if (!(cancelFlag || pauseFlag)) {// complete
                    mStatus = DownloadStatus.STATUS_COMPLETED;
                    synchronized (downloadTask) {
                        broadcastComplete(responseCode, responseMessage);
                    }
                    return;
                } else {
                    if (cancelFlag) { // cancel
                        mStatus=DownloadStatus.STATUS_CANCEL;
                        synchronized (downloadTask) {
                            broadcastCancel(new Exception("the download task is cancel"));
                        }
                        return;
                    } else if (pauseFlag) {// pause
                        updateDBProgress(mThreadInfo);
                        mStatus=DownloadStatus.STATUS_PAUSE;
                        synchronized (downloadTask) {
                            broadcastPause();
                        }
                        return;
                    } else {
                        mStatus = DownloadStatus.STATUS_ERROR;
                        synchronized (downloadTask) {
                            broadcastError(new Exception("something error occur"));
                        }
                    }
                }
            } else {
                throw new Exception("unSupported response code:" + responseCode);
            }
        } catch (Exception e) {
            updateDBProgress(mThreadInfo);
            mStatus = DownloadStatus.STATUS_ERROR;
            synchronized (downloadTask) {
                broadcastError(e);
            }
        } finally {
            mHttpConn.disconnect();
            try {
                IOCloseUtils.close(inputStream);
                IOCloseUtils.close(raf);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void broadcastPause() {
        this.downloadTask.broadcastPause(this.downloadId);
    }

    private void broadcastError(Exception ex) {
        this.downloadTask.broadcastError(this.downloadId, ex);
    }

    private void broadcastProgress(final long downloadBytes, final long totalBytes) {
        this.downloadTask.broadcastProgress(this.downloadId, downloadBytes, totalBytes);
    }

    private void broadcastComplete(final int responseCode, final String responseMessage) {
        this.downloadTask.broadcastComplete(this.downloadId, responseCode, responseMessage);
    }

    private void broadcastCancel(Exception ex) {
        this.downloadTask.broadcastCancel(this.downloadId, ex);
    }


    private void setHttpHeader(Map<String, String> headers, URLConnection connection) {
        if (headers != null) {
            for (String key : headers.keySet()) {
                connection.setRequestProperty(key, headers.get(key));
            }
        }
    }

    private synchronized Thread currentThread() {
        return Thread.currentThread();
    }

    protected abstract void insertIntoDB(ThreadInfo info);

    protected abstract int getResponseCode();

    protected abstract void updateDBProgress(ThreadInfo info);

    protected abstract Map<String, String> getHttpHeaders(ThreadInfo info);

    protected abstract RandomAccessFile getFile(ThreadInfo threadInfo, DownloadInfo downloadInfo) throws IOException;

}