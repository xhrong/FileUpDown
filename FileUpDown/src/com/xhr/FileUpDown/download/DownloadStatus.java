package com.xhr.FileUpDown.download;

/**
 * Created by Aspsine on 2015/7/15.
 */
public class DownloadStatus {

    public static final String STATUS = "status";

    public static final int STATUS_START = 100;
  //  public static final int STATUS_CONNECTED = 101;
    public static final int STATUS_PROGRESS = 102;
    public static final int STATUS_COMPLETED = 103;
    public static final int STATUS_PAUSE = 104;
    public static final int STATUS_CANCEL = 105;
    public static final int STATUS_ERROR = 106;
  //  public static final int STATUS_RETRY=107;

    private int status;
    private long length;
    private long finished;
    private boolean isRangeSupport;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public long getFinished() {
        return finished;
    }

    public void setFinished(long finished) {
        this.finished = finished;
    }

    public boolean isRangeSupport() {
        return isRangeSupport;
    }

    public void setIsRangeSupport(boolean isRangeSupport) {
        this.isRangeSupport = isRangeSupport;
    }
}
