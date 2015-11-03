package com.xhr.FileUpDown.upload;

import android.content.Context;
import android.content.Intent;
import com.xhr.FileUpDown.download.DownloadInfo;
import com.xhr.FileUpDown.upload.utils.NameValue;
import com.xhr.FileUpDown.NotificationConfig;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements an HTTP Multipart upload request.
 *
 * @author iflytek (Alex Gotev)
 * @author eliasnaur
 *
 */
public class UploadRequest{

    private NotificationConfig notificationConfig;
    private String method = "POST";
    private final Context context;
    private String customUserAgent;
    private int maxRetries;
    private final String uploadId;
    private final String url;
    private final ArrayList<NameValue> headers;
    private final ArrayList<NameValue> parameters;

    private final ArrayList<UploadFile> filesToUpload;

    /**
     * Creates a new multipart upload request.
     *
     * @param context application context
     * @param uploadId unique ID to assign to this upload request.
     *                 It's used in the broadcast receiver when receiving updates.
     * @param serverUrl URL of the server side script that handles the multipart form upload
     */
    public UploadRequest(final Context context, final String uploadId, final String serverUrl) {
        this.context = context;
        this.uploadId = uploadId;
        notificationConfig = new NotificationConfig();
        url = serverUrl;
        headers = new ArrayList<NameValue>();
        maxRetries = 0;
        filesToUpload = new ArrayList<UploadFile>();
        parameters = new ArrayList<NameValue>();
    }

    /**
     * Start the background file upload service.
     *
     * @throws IllegalArgumentException if one or more arguments passed are invalid
     * @throws MalformedURLException if the server URL is not valid
     */
    public void startUpload() throws IllegalArgumentException, MalformedURLException {
        this.validate();
        final Intent intent = new Intent(this.getContext(), UploadService.class);
        this.initializeIntent(intent);
        intent.setAction(UploadService.getActionUpload());
        getContext().startService(intent);
    }

    /**
     * Write any upload request data to the intent used to start the upload service.
     *
     * @param intent the intent used to start the upload service
     */
    protected void initializeIntent(Intent intent) {
        intent.setAction(UploadService.getActionUpload());
        intent.putExtra(UploadService.PARAM_NOTIFICATION_CONFIG, getNotificationConfig());
        intent.putExtra(UploadService.PARAM_ID, getUploadId());
        intent.putExtra(UploadService.PARAM_URL, getServerUrl());
        intent.putExtra(UploadService.PARAM_METHOD, getMethod());
        intent.putExtra(UploadService.PARAM_CUSTOM_USER_AGENT, getCustomUserAgent());
        intent.putExtra(UploadService.PARAM_MAX_RETRIES, getMaxRetries());
        intent.putParcelableArrayListExtra(UploadService.PARAM_REQUEST_HEADERS, getHeaders());
        intent.putParcelableArrayListExtra(UploadService.PARAM_FILES, getFilesToUpload());
        intent.putParcelableArrayListExtra(UploadService.PARAM_REQUEST_PARAMETERS, getParameters());
    }


    /**
     * Sets custom notification configuration.
     *
     * @param iconResourceID     ID of the notification icon.
     *                           You can use your own app's R.drawable.your_resource
     * @param title              Notification title
     * @param message            Text displayed in the notification when the upload is in progress
     * @param completed          Text displayed in the notification when the upload is completed successfully
     * @param error              Text displayed in the notification when an error occurs
     * @param autoClearOnSuccess true if you want to automatically clear the notification when
     *                           the upload gets completed successfully
     */
    public void setNotificationConfig(final DownloadInfo downloadInfo,final int iconResourceID, final String title, final String message,
                                      final String completed, final String error, final String cancel,final String pause, final boolean autoClearOnSuccess) {
        notificationConfig = new NotificationConfig(downloadInfo,iconResourceID, title, message, completed, error, cancel,pause,
                autoClearOnSuccess);
    }

    /**
     * Validates the upload request and throws exceptions if one or more parameters are
     * not properly set.
     *
     * @throws IllegalArgumentException       if request protocol or URL are not correctly set
     * @throws java.net.MalformedURLException if the provided server URL is not valid
     */
    public void validate() throws IllegalArgumentException, MalformedURLException {
        if (url == null || "".equals(url)) {
            throw new IllegalArgumentException("Request URL cannot be either null or empty");
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("Specify either http:// or https:// as protocol");
        }

        // Check if the URL is valid
        new URL(url);

        if (filesToUpload.isEmpty()) {
            throw new IllegalArgumentException("You have to add at least one file to upload");
        }
    }

    /**
     * Adds a file to this upload request.
     *
     * @param path Absolute path to the file that you want to upload
     * @param parameterName Name of the form parameter that will contain file's data
     * @param fileName File name seen by the server side script
     * @param contentType Content type of the file. Set this to null if you don't want to set a
     *                    content type.
     */
    public void addFileToUpload(final String path, final String parameterName, final String fileName,
                                final String contentType) {
        filesToUpload.add(new UploadFile(path, parameterName, fileName, contentType));
    }

    /**
     * Adds a parameter to this upload request.
     *
     * @param paramName parameter name
     * @param paramValue parameter value
     */
    public void addParameter(final String paramName, final String paramValue) {
        parameters.add(new NameValue(paramName, paramValue));
    }

    /**
     * Adds a parameter with multiple values to this upload request.
     *
     * @param paramName parameter name
     * @param array values
     */
    public void addArrayParameter(final String paramName, final String... array) {
        for (String value : array) {
            parameters.add(new NameValue(paramName, value));
        }
    }

    /**
     * Adds a parameter with multiple values to this upload request.
     *
     * @param paramName parameter name
     * @param list values
     */
    public void addArrayParameter(final String paramName, final List<String> list) {
        for (String value : list) {
            parameters.add(new NameValue(paramName, value));
        }
    }

    /**
     * Gets the list of the parameters.
     *
     * @return
     */
    protected ArrayList<NameValue> getParameters() {
        return parameters;
    }

    /**
     * Gets the list of the files that has to be uploaded.
     *
     * @return
     */
    protected ArrayList<UploadFile> getFilesToUpload() {
        return filesToUpload;
    }

    /**
     * Adds a header to this upload request.
     *
     * @param headerName  header name
     * @param headerValue header value
     */
    public void addHeader(final String headerName, final String headerValue) {
        headers.add(new NameValue(headerName, headerValue));
    }

    /**
     * Sets the HTTP method to use. By default it's set to POST.
     *
     * @param method new HTTP method to use
     */
    public void setMethod(final String method) {
        if (method != null && method.length() > 0)
            this.method = method;
    }

    /**
     * Gets the HTTP method to use.
     *
     * @return
     */
    protected String getMethod() {
        return method;
    }

    /**
     * Gets the upload ID of this request.
     *
     * @return
     */
    protected String getUploadId() {
        return uploadId;
    }

    /**
     * Gets the URL of the server side script that will handle the multipart form upload.
     *
     * @return
     */
    protected String getServerUrl() {
        return url;
    }

    /**
     * Gets the list of the headers.
     *
     * @return
     */
    protected ArrayList<NameValue> getHeaders() {
        return headers;
    }

    /**
     * Gets the upload notification configuration.
     *
     * @return
     */
    protected NotificationConfig getNotificationConfig() {
        return notificationConfig;
    }

    /**
     * Gets the application context.
     *
     * @return
     */
    protected Context getContext() {
        return context;
    }

    /**
     * Gets the custom user agent defined for this upload request.
     *
     * @return string representing the user agent or null if it's not defined
     */
    public final String getCustomUserAgent() {
        return customUserAgent;
    }

    /**
     * Sets the custom user agent to use for this upload request.
     * Note! If you set the "User-Agent" header by using the "addHeader" method,
     * that setting will be overwritten by the value set with this method.
     *
     * @param customUserAgent custom user agent string
     */
    public final void setCustomUserAgent(String customUserAgent) {
        this.customUserAgent = customUserAgent;
    }

    /**
     * Sets the intent to be executed when the user taps on the upload progress notification.
     *
     * @param intent
     */
    public final void setNotificationClickIntent(Intent intent) {
        notificationConfig.setClickIntent(intent);
    }

    /**
     * Get the maximum number of retries that the library will do if an error occurs, before returning an error.
     *
     * @return
     */
    public final int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Sets the maximum number of retries that the library will do if an error occurs, before returning an error.
     *
     * @param maxRetries
     */
    public final void setMaxRetries(int maxRetries) {
        if (maxRetries < 0)
            this.maxRetries = 0;
        else
            this.maxRetries = maxRetries;
    }
}
