package com.xhr.FileUpDown.upload;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import com.xhr.FileUpDown.NotificationConfig;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service to upload files in background using HTTP POST with notification center progress
 * display.
 *
 * @author iflytek (Aleksandar Gotev)
 * @author eliasnaur
 * @author cankov
 */
public class UploadService extends Service {

    private static final String TAG = "UploadService";

    //服务的命名空间
    private static String NAMESPACE = "com.xhr";
    //最大同时上传线程数
    private static int THREAD_POOL_SIZE = 1;

    private static final String ACTION_UPLOAD_SUFFIX = ".uploadservice.action.upload";
    protected static final String PARAM_NOTIFICATION_CONFIG = "notificationConfig";
    protected static final String PARAM_ID = "id";
    protected static final String PARAM_URL = "url";
    protected static final String PARAM_METHOD = "method";
    protected static final String PARAM_FILES = "files";

    protected static final String PARAM_REQUEST_HEADERS = "requestHeaders";
    protected static final String PARAM_REQUEST_PARAMETERS = "requestParameters";
    protected static final String PARAM_CUSTOM_USER_AGENT = "customUserAgent";
    protected static final String PARAM_MAX_RETRIES = "maxRetries";

    /**
     * The minimum interval between progress reports in milliseconds.
     * If the upload Tasks report more frequently, we will throttle notifications.
     * We aim for 6 updates per second.
     */
    protected static final long PROGRESS_REPORT_INTERVAL = 166;

    private static final String BROADCAST_ACTION_SUFFIX = ".uploadservice.broadcast.status";
    public static final String UPLOAD_ID = "id";
    public static final String PROGRESS = "progress";
    public static final String PROGRESS_UPLOADED_BYTES = "progressUploadedBytes";
    public static final String PROGRESS_TOTAL_BYTES = "progressTotalBytes";
    public static final String ERROR_EXCEPTION = "errorException";
    public static final String CANCEL_EXCEPTION = "cancelException";
    public static final String SERVER_RESPONSE_CODE = "serverResponseCode";
    public static final String SERVER_RESPONSE_MESSAGE = "serverResponseMessage";

    private NotificationManager notificationManager;
    private Builder notification;
    private PowerManager.WakeLock wakeLock;

    private long lastProgressNotificationTime;

    private UploadTask newTask;
    private ExecutorService fixedThreadPool;

    private static Map<String, UploadTask> httpUploadTaskMap = new HashMap<String, UploadTask>();

    public static void initSetting(String nameSpace, int threadPoolSize) {
        NAMESPACE = nameSpace;
        THREAD_POOL_SIZE = threadPoolSize;
    }

    public static String getActionUpload() {
        return NAMESPACE + ACTION_UPLOAD_SUFFIX;
    }

    public static String getActionBroadcast() {
        return NAMESPACE + BROADCAST_ACTION_SUFFIX;
    }


    public UploadService() {
        fixedThreadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notification = new NotificationCompat.Builder(this);
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null) {
            final String action = intent.getAction();

            if (getActionUpload().equals(action)) {
                newTask = new HttpUploadTask(this, intent);

                lastProgressNotificationTime = 0;
                wakeLock.acquire();

                httpUploadTaskMap.put(newTask.uploadId, newTask);
                createNotification(newTask.uploadId);

                fixedThreadPool.execute(new Runnable() {

                    @Override
                    public void run() {
                        newTask.run();
                    }
                });
            }
        }
        return Service.START_STICKY;
    }

    /**
     * Start the background file upload service.
     * You can use the startUpload instance method of the HttpUploadRequest directly.
     * The method is here for backward compatibility.
     *
     * @throws IllegalArgumentException       if one or more arguments passed are invalid
     * @throws java.net.MalformedURLException if the server URL is not valid
     */
    public static void startUpload(UploadRequest request)
            throws IllegalArgumentException, MalformedURLException {
        request.startUpload();
    }

    /**
     * 取消上传任务
     *
     * @param uploadId
     */
    public static void cancelUpload(String uploadId) {
        if (httpUploadTaskMap.containsKey(uploadId)) {
            UploadTask task = httpUploadTaskMap.get(uploadId);
            task.cancel();
            httpUploadTaskMap.remove(uploadId);
        }
    }


    void broadcastProgress(final String uploadId, final long uploadedBytes, final long totalBytes) {

        long currentTime = System.currentTimeMillis();
        if (currentTime < lastProgressNotificationTime + PROGRESS_REPORT_INTERVAL) {
            return;
        }

        lastProgressNotificationTime = currentTime;

        updateNotificationProgress(uploadId, (int) uploadedBytes, (int) totalBytes);

        final Intent intent = new Intent(getActionBroadcast());
        intent.putExtra(UPLOAD_ID, uploadId);
        intent.putExtra(UploadStatus.STATUS, UploadStatus.STATUS_IN_PROGRESS);

        final int percentsProgress = (int) (uploadedBytes * 100 / totalBytes);
        intent.putExtra(PROGRESS, percentsProgress);

        intent.putExtra(PROGRESS_UPLOADED_BYTES, uploadedBytes);
        intent.putExtra(PROGRESS_TOTAL_BYTES, totalBytes);
        sendBroadcast(intent);
    }

    void broadcastCompleted(final String uploadId, final int responseCode, final String responseMessage) {

        final String filteredMessage;
        if (responseMessage == null) {
            filteredMessage = "";
        } else {
            filteredMessage = responseMessage;
        }

        if (responseCode >= 200 && responseCode <= 299)
            updateNotificationCompleted(uploadId);
        else
            updateNotificationError(uploadId);

        final Intent intent = new Intent(getActionBroadcast());
        intent.putExtra(UPLOAD_ID, uploadId);
        intent.putExtra(UploadStatus.STATUS, UploadStatus.STATUS_COMPLETED);
        intent.putExtra(SERVER_RESPONSE_CODE, responseCode);
        intent.putExtra(SERVER_RESPONSE_MESSAGE, filteredMessage);
        sendBroadcast(intent);
        wakeLock.release();
    }

    void broadcastError(final String uploadId, final Exception exception) {

        updateNotificationError(uploadId);

        final Intent intent = new Intent(getActionBroadcast());
        intent.setAction(getActionBroadcast());
        intent.putExtra(UPLOAD_ID, uploadId);
        intent.putExtra(UploadStatus.STATUS, UploadStatus.STATUS_ERROR);
        intent.putExtra(ERROR_EXCEPTION, exception);
        sendBroadcast(intent);
        wakeLock.release();
    }

    void broadcastCancel(final String uploadId, final Exception exception) {

        updateNotificationCancel(uploadId);

        final Intent intent = new Intent(getActionBroadcast());
        intent.setAction(getActionBroadcast());
        intent.putExtra(UPLOAD_ID, uploadId);
        intent.putExtra(UploadStatus.STATUS, UploadStatus.STATUS_CANCEL);
        intent.putExtra(CANCEL_EXCEPTION, exception);
        sendBroadcast(intent);
        wakeLock.release();
    }


    private void createNotification(final String uploadId) {

        UploadTask task = getUploadTask(uploadId);
        if (task != null) {
            NotificationConfig notificationConfig = task.notificationConfig;
            notification.setContentTitle(notificationConfig.getTitle())
                    .setContentText(notificationConfig.getMessage())
                    .setContentIntent(notificationConfig.getPendingIntent(this))
                    .setSmallIcon(notificationConfig.getIconResourceID())
                    .setProgress(100, 0, true).setOngoing(false);

            //不能使用startForeground(id, notification.build())，这样只能产生一个Notification
            Notification nt = notification.build();
            nt.flags |= Notification.FLAG_FOREGROUND_SERVICE;
            notificationManager.notify(notificationConfig.getId(), nt);
        }
    }

    private void updateNotificationProgress(final String uploadId, int uploadedBytes, int totalBytes) {
        UploadTask task = getUploadTask(uploadId);
        if (task != null) {
            NotificationConfig notificationConfig = task.notificationConfig;
            notification.setContentTitle(notificationConfig.getTitle())
                    .setContentText(notificationConfig.getMessage())
                    .setContentIntent(notificationConfig.getPendingIntent(this))
                    .setSmallIcon(notificationConfig.getIconResourceID())
                    .setProgress(totalBytes, uploadedBytes, false)
                    .setOngoing(false);

            //不能使用startForeground(id, notification.build())，这样只能产生一个Notification
            Notification nt = notification.build();
            nt.flags |= Notification.FLAG_FOREGROUND_SERVICE;
            notificationManager.notify(notificationConfig.getId(), nt);

        }
    }

    private void updateNotificationCompleted(final String uploadId) {
        UploadTask task = getUploadTask(uploadId);
        if (task != null) {
            NotificationConfig notificationConfig = task.notificationConfig;
            if (!notificationConfig.isAutoClearOnSuccess()) {
                notification.setContentTitle(notificationConfig.getTitle())
                        .setContentText(notificationConfig.getCompleted())
                        .setContentIntent(notificationConfig.getPendingIntent(this))
                        .setSmallIcon(notificationConfig.getIconResourceID())
                        .setProgress(0, 0, false)
                        .setOngoing(false);
                setRingtone(notificationConfig);

                Notification nt = notification.build();
                removeForegroundNotification(notificationConfig.getId());
                nt.flags |= Notification.FLAG_AUTO_CANCEL;
                notificationManager.notify(notificationConfig.getId(), notification.build());

            }
        }
    }

    private void setRingtone(NotificationConfig notificationConfig) {
        if (notificationConfig.isRingTone()) {
            notification.setSound(RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_NOTIFICATION));
            notification.setOnlyAlertOnce(false);
        }

    }

    private void updateNotificationError(final String uploadId) {
        UploadTask task = getUploadTask(uploadId);
        if (task != null) {
            NotificationConfig notificationConfig = task.notificationConfig;
            notification.setContentTitle(notificationConfig.getTitle())
                    .setContentText(notificationConfig.getError())
                    .setContentIntent(notificationConfig.getPendingIntent(this))
                    .setSmallIcon(notificationConfig.getIconResourceID())
                    .setProgress(0, 0, false).setOngoing(false);
            setRingtone(notificationConfig);

            Notification nt = notification.build();
            removeForegroundNotification(notificationConfig.getId());
            nt.flags |= Notification.FLAG_AUTO_CANCEL;
            notificationManager.notify(notificationConfig.getId(), notification.build());
        }


    }

    private void updateNotificationCancel(final String uploadId) {
        UploadTask task = getUploadTask(uploadId);
        if (task != null) {
            NotificationConfig notificationConfig = task.notificationConfig;
            notification.setContentTitle(notificationConfig.getTitle())
                    .setContentText(notificationConfig.getCancel())
                    .setContentIntent(notificationConfig.getPendingIntent(this))
                    .setSmallIcon(notificationConfig.getIconResourceID())
                    .setProgress(0, 0, false).setOngoing(false);
            setRingtone(notificationConfig);

            Notification nt = notification.build();
            removeForegroundNotification(notificationConfig.getId());
            nt.flags |= Notification.FLAG_AUTO_CANCEL;
            notificationManager.notify(notificationConfig.getId(), notification.build());
        }
    }

    /**
     * 只有用这种方式才能删除flags=Notification.FLAG_FOREGROUND_SERVICE的前台Notification
     *
     * @param id
     */
    private void removeForegroundNotification(int id) {
        Notification nt = new Notification();
        startForeground(id, nt);
        stopForeground(true);
    }

    private UploadTask getUploadTask(String uploadId) {
        if (httpUploadTaskMap.containsKey(uploadId)) {
            return httpUploadTaskMap.get(uploadId);
        }
        return null;
    }
}
