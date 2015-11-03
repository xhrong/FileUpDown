package com.xhr.FileUpDown.download;

import android.text.TextUtils;
import com.xhr.FileUpDown.download.utils.FileUtils;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 下载前先尝试连接一下，如果连接成功，则后续开启下载线程
 *
 * @author xhrong
 * @date 2015.10.28
 */
public class ConnectThread implements Runnable {
    private DownloadInfo mDownloadInfo;
    private String downloadId;
    private DownloadTask downloadTask;
    private HttpURLConnection mHttpConn;

    private boolean rangeFlag = true;

    public ConnectThread(DownloadTask downloadTask, DownloadInfo downloadInfo) {
        this.downloadId = downloadInfo.getId();
        this.mDownloadInfo = downloadInfo;
        this.downloadTask = downloadTask;
    }

    protected void setRangeFlag(boolean flag) {
        this.rangeFlag = flag;
    }
    protected boolean getRangeFlag(){
        return this.rangeFlag;
    }


    @Override
    public void run() {
        try {
            broadcastStart();

            URL url = new URL(mDownloadInfo.getUrl());
            mHttpConn = (HttpURLConnection) url.openConnection();
            mHttpConn.setConnectTimeout(DownloadService.CONNECT_TIME_OUT);
            mHttpConn.setReadTimeout(DownloadService.READ_TIME_OUT);
            mHttpConn.setRequestMethod(DownloadService.GET);
            long length = -1;
            boolean isSupportRange = false;
            if (mHttpConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                String headerLength = mHttpConn.getHeaderField("Content-Length");
                if (TextUtils.isEmpty(headerLength) || headerLength.equals("0") || headerLength.equals("-1")) {
                    length = mHttpConn.getContentLength();
                } else {
                    length = Long.parseLong(headerLength);
                }
                String acceptRanges = mHttpConn.getHeaderField("Accept-Ranges");
                if (!TextUtils.isEmpty(acceptRanges)) {
                    isSupportRange = acceptRanges.equals("bytes");
                }
            }
            if (length <= 0) {
                throw new Exception("The file to download is invalid: length<=0");
            } else {
                mDownloadInfo.setLength(length);
                //这个方式并不准确,要通过重试机制切换
                mDownloadInfo.setIsSupportRange(isSupportRange && getRangeFlag());

                if (!mDownloadInfo.getDir().exists()) {
                    if (FileUtils.isSDMounted()) {
                        mDownloadInfo.getDir().mkdir();
                    } else {
                        throw new Exception("Local Sdcard error: cannot create file");
                    }
                }
                this.downloadTask.download(mDownloadInfo);

            }
        } catch (Exception e) {
            broadcastError(e);
        } finally {
            mHttpConn.disconnect();
        }
    }

    private void broadcastError(Exception ex) {
        this.downloadTask.broadcastError(this.downloadId, ex);
    }

    private void broadcastStart() {
        this.downloadTask.broadcastStart(this.downloadId);
    }

}
