package com.xhr.FileUpDown.download;

import android.content.Context;
import android.content.Intent;
import com.xhr.FileUpDown.NotificationConfig;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * 下载请求，用于构造下载数据结构
 * @author  xhrong
 * @date 2015.10.28
 */
public class DownloadRequest {

    private final DownloadInfo mDownloadInfo;
    public NotificationConfig notificationConfig;
    private Context mContext;

    public DownloadRequest(Context context, DownloadInfo downloadInfo, NotificationConfig notificationConfig) {
        this.mDownloadInfo = downloadInfo;
        this.mContext = context;
        this.notificationConfig = notificationConfig;
    }

    /**
     * 启动下载Service
     * @throws IllegalArgumentException
     * @throws MalformedURLException
     */
    public void startDownload() throws IllegalArgumentException, MalformedURLException {
        validate(mDownloadInfo.getUrl());
        final Intent intent = new Intent(mContext, DownloadService.class);
        intent.putExtra(DownloadService.PARAM_NOTIFICATION_CONFIG,  this.notificationConfig);
        intent.putExtra(DownloadService.PARAM_ID, this.mDownloadInfo.getId());
        intent.putExtra(DownloadService.PARAM_URL, this.mDownloadInfo.getUrl());
        intent.putExtra(DownloadService.PARAM_DIR,this.mDownloadInfo.getDir());
        intent.putExtra(DownloadService.PARAM_NAME,this.mDownloadInfo.getName());
        intent.putExtra(DownloadService.PARAM_MAX_RETRIES, this.mDownloadInfo.getMaxRetries());
        intent.setAction(DownloadService.getActionDownload());
        mContext.startService(intent);
    }

    /**
     * Validates the upload request and throws exceptions if one or more parameters are
     * not properly set.
     *
     * @throws IllegalArgumentException       if request protocol or URL are not correctly set
     * @throws java.net.MalformedURLException if the provided server URL is not valid
     */
    private void validate(String url) throws IllegalArgumentException, MalformedURLException {
        if (url == null || "".equals(url)) {
            throw new IllegalArgumentException("Request URL cannot be either null or empty");
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("Specify either http:// or https:// as protocol");
        }

        // Check if the URL is valid
        new URL(url);
    }
}
