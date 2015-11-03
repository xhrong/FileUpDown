package com.example.demo;

import android.app.Application;
import com.xhr.FileUpDown.download.DownloadService;

/**
 * @author iflytek (Aleksandar Gotev)
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Set your application namespace to avoid conflicts with other apps
        // using this library
        DownloadService.initSetting("com.xhrong.fileDownload", 2,"",true);
    }
}
