package com.xhr.FileUpDown.download;

/**
 * Created by xhrong on 15-10-28.
 */
public class ThreadInfo {
    private int id;
    private String downloadId;
    private String url;
    private long start;
    private long end;
    private long finished;
    private int progress;

    public ThreadInfo() {
    }

    public ThreadInfo(int id, String downloadId, String url, long start, long end, long finished) {
        this.id = id;
        this.downloadId = downloadId;
        this.url = url;
        this.start = start;
        this.end = end;
        this.finished = finished;
    }

    public ThreadInfo(int id, String downloadId, String url, int finished) {
        this.id = id;
        this.url = url;
        this.downloadId = downloadId;
        this.finished = 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDownloadId() {
        return this.downloadId;
    }

    public void setDownloadId(String downloadId) {
        this.downloadId = downloadId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public long getFinished() {
        return finished;
    }

    public void setFinished(long finished) {
        this.finished = finished;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }
}
