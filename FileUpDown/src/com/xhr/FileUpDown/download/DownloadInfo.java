package com.xhr.FileUpDown.download;

import java.io.File;
import java.io.Serializable;

/**
 * 下载任务数据结构，Id是任务唯一标示
 * @author  xhrong
 * @date 2015.10.28
 */
public class DownloadInfo implements Serializable {
    private String id;
    private String name;
    private String url;
    private File dir;
    private int progress;
    private long length;
    private long finished;
    private boolean isSupportRange=true;
    private int maxRetries;

    public DownloadInfo(String id, String name, String url, File dir) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.dir = dir;
    }

    public DownloadInfo() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public File getDir() {
        return dir;
    }

    public void setDir(File dir) {
        this.dir = dir;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public int getProgress() {
        return progress;
    }

    public synchronized void setProgress(int progress) {
        this.progress = progress;
    }

    public long getFinished() {
        return finished;
    }

    public synchronized void setFinished(long finished) {
        this.finished = finished;
    }

    public boolean isSupportRange() {
        return isSupportRange;
    }

    public void setIsSupportRange(boolean isSupportRange) {
        this.isSupportRange = isSupportRange;
    }

    public void setMaxRetries(int times) {
        if (maxRetries < 0)
            this.maxRetries = 0;
        else
            this.maxRetries = times;
    }

    public int getMaxRetries() {
        return this.maxRetries;
    }
}
