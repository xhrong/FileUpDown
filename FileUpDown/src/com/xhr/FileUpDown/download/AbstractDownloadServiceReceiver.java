package com.xhr.FileUpDown.download;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * DownloadService的BroadcastReceiver，接收并处理下载过程发出的各种状态通知。
 *
 * @author xhrong
 * @data 2015.10.28
 */
public abstract class AbstractDownloadServiceReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent != null) {
            if (DownloadService.getActionBroadcast().equals(intent.getAction())) {
                final int status = intent.getIntExtra(DownloadStatus.STATUS, 0);
                final String downloadId = intent.getStringExtra(DownloadService.PARAM_ID);
                switch (status) {
                    case DownloadStatus.STATUS_START:
                        onStarted(downloadId);
                        break;
                    case DownloadStatus.STATUS_PROGRESS:
                        final int progress = intent.getIntExtra(DownloadService.PROGRESS, 0);
                        onProgress(downloadId, progress);
                        break;
                    case DownloadStatus.STATUS_COMPLETED:
                        final int responseCode = intent.getIntExtra(DownloadService.SERVER_RESPONSE_CODE, 0);
                        final String responseMsg = intent.getStringExtra(DownloadService.SERVER_RESPONSE_MESSAGE);
                        onCompleted(downloadId, responseCode, responseMsg);
                        break;
                    case DownloadStatus.STATUS_PAUSE:
                        onPause(downloadId);
                        break;
                    case DownloadStatus.STATUS_CANCEL:
                        final Exception ex1 = (Exception) intent.getSerializableExtra(DownloadService.CANCEL_EXCEPTION);
                        onCancel(downloadId, ex1);
                        break;
                    case DownloadStatus.STATUS_ERROR:
                        final Exception ex2 = (Exception) intent.getSerializableExtra(DownloadService.ERROR_EXCEPTION);
                        onError(downloadId, ex2);
                        break;
                    default:
                        break;
                }
            }
        }

    }

    /**
     * Register this download receiver.
     * It's recommended to register the receiver in Activity's onResume method.
     *
     * @param context context in which to register this receiver
     */
    public void register(final Context context) {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DownloadService.getActionBroadcast());
        context.registerReceiver(this, intentFilter);
    }

    /**
     * Unregister this download receiver.
     * It's recommended to unregister the receiver in Activity's onPause method.
     *
     * @param context context in which to unregister this receiver
     */
    public void unregister(final Context context) {
        context.unregisterReceiver(this);
    }


    /**
     * call when download started
     *
     * @param downloadId unique ID of the upload request
     */
    public void onStarted(String downloadId) {
    }

    /**
     * call when download task paused
     *
     * @param downloadId unique ID of the upload request
     */
    public void onPause(String downloadId) {
    }

    /**
     * Called when the upload progress changes.
     *
     * @param uploadId unique ID of the upload request
     * @param progress value from 0 to 100
     */
    public void onProgress(final String uploadId, final int progress) {
    }


    /**
     * Called when an error happens during the upload.
     *
     * @param uploadId  unique ID of the upload request
     * @param exception exception that caused the error
     */
    public void onError(final String uploadId, final Exception exception) {
    }

    /**
     * Called when user cancel  the upload.
     *
     * @param uploadId  unique ID of the upload request
     * @param exception exception that caused the cancel
     */
    public void onCancel(final String uploadId, final Exception exception) {

    }


    /**
     * Called when the upload is completed successfully.
     *
     * @param uploadId              unique ID of the upload request
     * @param serverResponseCode    status code returned by the server
     * @param serverResponseMessage string containing the response received from the server
     */
    public void onCompleted(final String uploadId, final int serverResponseCode,
                            final String serverResponseMessage) {
    }
}
