package com.xhr.FileUpDown.download;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;
import com.xhr.FileUpDown.NotificationConfig;
import com.xhr.FileUpDown.download.db.DataBaseManager;
import com.xhr.FileUpDown.download.utils.StringUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service to download files in background using HTTP with notification center progress
 * display.
 *
 * @author xhrong
 * @date 2015.10.28
 */
public class DownloadService extends Service {

    private static final String TAG = "DownloadService";
    //服务的命名空间
    private static String NAMESPACE = "com.xhr";
    //最大同时上传线程数
    private static int THREAD_POOL_SIZE = 3;
    //默认文件保存路径
    private static String DEFAULT_SAVE_PATH = "/sdcard/";

    private static boolean SHOW_NOTIFICATION = false;

    public static final int CONNECT_TIME_OUT = 10 * 1000;
    public static final int READ_TIME_OUT = 10 * 1000;
    public static final String GET = "GET";

    private static final String ACTION_DOWNLOAD_SUFFIX = ".downloadservice.action.download";
    protected static final String PARAM_NOTIFICATION_CONFIG = "notificationConfig";
    protected static final String PARAM_ID = "id";
    protected static final String PARAM_URL = "url";
    protected static final String PARAM_NAME = "name";
    protected static final String PARAM_DIR = "dir";
    protected static final String PARAM_MAX_RETRIES = "maxRetries";
    /**
     * The minimum interval between progress reports in milliseconds.
     * If the upload Tasks report more frequently, we will throttle notifications.
     * We aim for 6 updates per second.
     */
    protected static final long PROGRESS_REPORT_INTERVAL = 1000;
    private static final String BROADCAST_ACTION_SUFFIX = ".downloadservice.broadcast.status";
    public static final String DOWNLOAD_ID = "id";
    public static final String PROGRESS = "progress";
    public static final String PROGRESS_UPLOADED_BYTES = "progressDownloadedBytes";
    public static final String PROGRESS_TOTAL_BYTES = "progressTotalBytes";
    public static final String ERROR_EXCEPTION = "errorException";
    public static final String CANCEL_EXCEPTION = "cancelException";
    public static final String SERVER_RESPONSE_CODE = "serverResponseCode";
    public static final String SERVER_RESPONSE_MESSAGE = "serverResponseMessage";

    private NotificationManager notificationManager;
    private Builder notification;
    private long lastProgressNotificationTime;
    private static Map<String, DownloadTask> mDownloadTasks = new HashMap<String, DownloadTask>();
    private ExecutorService mExecutorService;
    private DataBaseManager mDBManager;


    /**
     * 初始化下载组件的包名、线程池大小及默认文件保存路径。
     * 在Service实例化之前调用，建议在Application里调用
     *
     * @param nameSpace
     * @param threadPoolSize
     * @param defaultSavePath
     */
    public static void initSetting(String nameSpace, int threadPoolSize, String defaultSavePath, boolean showNotification) {
        NAMESPACE = StringUtils.isEmpty(nameSpace) ? NAMESPACE : nameSpace;
        THREAD_POOL_SIZE = threadPoolSize > 0 ? THREAD_POOL_SIZE : threadPoolSize;
        DEFAULT_SAVE_PATH = StringUtils.isEmpty(defaultSavePath) ? DEFAULT_SAVE_PATH : defaultSavePath;
        SHOW_NOTIFICATION = showNotification;
    }

    public DownloadService() {
        mExecutorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    protected static String getActionDownload() {
        return NAMESPACE + ACTION_DOWNLOAD_SUFFIX;
    }

    protected static String getActionBroadcast() {
        return NAMESPACE + BROADCAST_ACTION_SUFFIX;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mDBManager = DataBaseManager.getInstance(this);
        mExecutorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notification = new Builder(this);
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
            final String downloadId = intent.getStringExtra(DownloadService.PARAM_ID);
            final String url = intent.getStringExtra(DownloadService.PARAM_URL);
            final String dirPath = intent.getStringExtra(DownloadService.PARAM_DIR);
            final String fileName = intent.getStringExtra(DownloadService.PARAM_NAME);
            final int maxRetries = intent.getIntExtra(DownloadService.PARAM_MAX_RETRIES, 0);
            final NotificationConfig notificationConfig = intent.getParcelableExtra(DownloadService.PARAM_NOTIFICATION_CONFIG);

            if (getActionDownload().equals(action)) {
                final DownloadInfo downloadInfo;
                final DownloadTask downloadTask;
                if (mDownloadTasks.containsKey(downloadId)) {
                    Log.i("DownloadManager", "use cached request");
                    downloadTask = mDownloadTasks.get(downloadId);
                } else {
                    Log.i("DownloadManager", "use new request");
                    File dir = null;
                    if (dirPath != null && !dirPath.equals("")) {
                        dir = new File(dirPath);
                    } else {
                        dir = new File(DEFAULT_SAVE_PATH);
                    }
                    downloadInfo = new DownloadInfo(downloadId, fileName, url, dir);
                    downloadInfo.setMaxRetries(maxRetries);
                    downloadTask = new DownloadTask(this, downloadInfo, mDBManager, mExecutorService, notificationConfig);
                    mDownloadTasks.put(downloadId, downloadTask);
                }
                if (!downloadTask.isStarted()) {
                    lastProgressNotificationTime = 0;
                    downloadTask.start();
                    createNotification(downloadId);
                } else {
                    Log.i("DownloadManager", fileName + " : has started!");
                }
            }
        }
        return Service.START_STICKY;
    }


    /**
     * start a download Task
     *
     * @param request
     * @throws IllegalArgumentException
     * @throws MalformedURLException
     */
    public static void startDownload(DownloadRequest request)
            throws IllegalArgumentException, MalformedURLException {
        request.startDownload();
    }

    /**
     * <p>Core method: pause the downloading task.
     * <p/>
     * <p>Pause the downloading task and record the progress data in database.
     * Once you invoke{@link #startDownload(DownloadRequest)} method again,
     * the task will be resumed automatically from where you had paused.
     *
     * @param downloadId the url of the download task you want to pause
     */
    public static void pause(String downloadId) {
        DownloadTask downloadTask = mDownloadTasks.get(downloadId);
        if (downloadTask != null) {
            downloadTask.pause();
        } else {
            Log.i("DownloadManager", "pause " + downloadId + " request == null");
        }

    }

    /**
     * <p>Core method: pause all downloading task
     * <p>detail see{@link #pause(String)}
     */
    public static void pauseAll() {
        for (DownloadTask downloadTask : mDownloadTasks.values()) {
            if (downloadTask != null && downloadTask.isStarted()) {
                downloadTask.pause();
            }
        }
    }

    /**
     * <p>Core method: cancel the download task.
     * <p/>
     * <p>The difference between {@link #pause(String url)} and {@link #cancel(String url)}
     * is that {@link #cancel(String url)} release the reference of the thread task, and
     * {@link #cancel(String url)} will delete the unfinished file created in the download
     * path you have configured in {@link #(java.io.File)} and
     * delete the download progress data in database.
     * <p/>
     * <p>Note: if your downloading task is connecting the server you can only invoke {@link #cancel(String url)}
     * to cancel {@link ConnectThread} task.
     *
     * @param downloadId the url of the download task you want to cancel
     */
    public static void cancel(String downloadId) {
        DownloadTask downloadTask = mDownloadTasks.get(downloadId);
        if (downloadTask != null) {
            downloadTask.cancel();
        } else {
            Log.i("DownloadManager", "cancel " + downloadId + " request == null");
        }
        //  mDownloadTasks.remove(downloadId);
    }

    /**
     * <p>Core method: cancel all downloading task
     * <p>detail see{@link #cancel(String)}
     */
    public static void cancelAll() {
        for (DownloadTask downloadTask : mDownloadTasks.values()) {
            if (downloadTask != null && downloadTask.isStarted()) {
                downloadTask.cancel();
            }
        }
    }

    protected void broadcastStarted(final String downloadId) {
        final Intent intent = new Intent(getActionBroadcast());
        intent.putExtra(DOWNLOAD_ID, downloadId);
        intent.putExtra(DownloadStatus.STATUS, DownloadStatus.STATUS_START);
        sendBroadcast(intent);
    }

    protected void broadcastPause(final String downloadId) {
        updateNotificationPause(downloadId);

        final Intent intent = new Intent(getActionBroadcast());
        intent.putExtra(DOWNLOAD_ID, downloadId);
        intent.putExtra(DownloadStatus.STATUS, DownloadStatus.STATUS_PAUSE);
        sendBroadcast(intent);
        mDownloadTasks.remove(downloadId);
    }

    protected void broadcastProgress(final String downloadId, final long downloadBytes, final long totalBytes) {

        long currentTime = System.currentTimeMillis();
        if (currentTime < lastProgressNotificationTime + PROGRESS_REPORT_INTERVAL) {
            return;
        }

        lastProgressNotificationTime = currentTime;

        updateNotificationProgress(downloadId, (int) downloadBytes, (int) totalBytes);

        final Intent intent = new Intent(getActionBroadcast());
        intent.putExtra(DOWNLOAD_ID, downloadId);
        intent.putExtra(DownloadStatus.STATUS, DownloadStatus.STATUS_PROGRESS);

        final int percentsProgress = (int) (downloadBytes * 100 / totalBytes);
        intent.putExtra(PROGRESS, percentsProgress);

        intent.putExtra(PROGRESS_UPLOADED_BYTES, downloadBytes);
        intent.putExtra(PROGRESS_TOTAL_BYTES, totalBytes);
        sendBroadcast(intent);
    }

    protected void broadcastCompleted(final String downloadId, final int responseCode, final String responseMessage) {

        final String filteredMessage;
        if (responseMessage == null) {
            filteredMessage = "";
        } else {
            filteredMessage = responseMessage;
        }

        if (responseCode >= 200 && responseCode <= 299)
            updateNotificationCompleted(downloadId);
        else
            updateNotificationError(downloadId);

        final Intent intent = new Intent(getActionBroadcast());
        intent.putExtra(DOWNLOAD_ID, downloadId);
        intent.putExtra(DownloadStatus.STATUS, DownloadStatus.STATUS_COMPLETED);
        intent.putExtra(SERVER_RESPONSE_CODE, responseCode);
        intent.putExtra(SERVER_RESPONSE_MESSAGE, filteredMessage);
        sendBroadcast(intent);
        mDownloadTasks.remove(downloadId);
    }

    protected void broadcastError(final String downloadId, final Exception exception) {

        updateNotificationError(downloadId);

        final Intent intent = new Intent(getActionBroadcast());
        intent.setAction(getActionBroadcast());
        intent.putExtra(DOWNLOAD_ID, downloadId);
        intent.putExtra(DownloadStatus.STATUS, DownloadStatus.STATUS_ERROR);
        intent.putExtra(ERROR_EXCEPTION, exception);
        sendBroadcast(intent);

        mDownloadTasks.remove(downloadId);
    }

    protected void broadcastCancel(final String downloadId, final Exception exception) {

        updateNotificationCancel(downloadId);

        final Intent intent = new Intent(getActionBroadcast());
        intent.setAction(getActionBroadcast());
        intent.putExtra(DOWNLOAD_ID, downloadId);
        intent.putExtra(DownloadStatus.STATUS, DownloadStatus.STATUS_CANCEL);
        intent.putExtra(CANCEL_EXCEPTION, exception);
        sendBroadcast(intent);
        mDownloadTasks.remove(downloadId);
    }


    private void createNotification(final String downloadId) {
        if (!SHOW_NOTIFICATION) return;
        DownloadTask downloadTask = mDownloadTasks.get(downloadId);
        if (downloadTask != null) {
            NotificationConfig notificationConfig = downloadTask.notificationConfig;
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

    private void showNotification(final String downloadId, String nType) {
        if (!SHOW_NOTIFICATION) return;
        DownloadTask downloadTask = mDownloadTasks.get(downloadId);
        if (downloadTask != null) {
            NotificationConfig notificationConfig = downloadTask.notificationConfig;
            if (!notificationConfig.isAutoClearOnSuccess()) {
                notification.setContentTitle(notificationConfig.getTitle())

                        .setContentIntent(notificationConfig.getPendingIntent(this))
                        .setSmallIcon(notificationConfig.getIconResourceID())
                        .setProgress(0, 0, false)
                        .setOngoing(false);
                if (nType.equals("complete")) {
                    notification.setContentText(notificationConfig.getCompleted());
                } else if (nType.equals("pause")) {
                    notification.setContentText(notificationConfig.getPause());
                } else if (nType.equals("error")) {
                    notification.setContentText(notificationConfig.getError());
                } else if (nType.equals("cancel")) {
                    notification.setContentText(notificationConfig.getCancel());
                }
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

    private void updateNotificationPause(final String downloadId) {
        showNotification(downloadId, "pause");
    }

    private void updateNotificationProgress(final String downloadId, int uploadedBytes, int totalBytes) {
        if (!SHOW_NOTIFICATION) return;
        DownloadTask downloadTask = mDownloadTasks.get(downloadId);
        if (downloadTask != null) {
            NotificationConfig notificationConfig = downloadTask.notificationConfig;
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

    private void updateNotificationCompleted(final String downloadId) {
        showNotification(downloadId, "complete");
        mDownloadTasks.remove(downloadId);
    }


    private void updateNotificationError(final String downloadId) {
        showNotification(downloadId, "error");

    }

    private void updateNotificationCancel(final String downloadId) {
        showNotification(downloadId, "cancel");
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

    private DownloadTask getDownloadRequest(String downloadId) {
        return mDownloadTasks.get(downloadId);
    }


}
