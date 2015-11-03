package com.example.demo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.xhr.FileUpDown.NotificationConfig;
import com.xhr.FileUpDown.download.AbstractDownloadServiceReceiver;
import com.xhr.FileUpDown.download.DownloadInfo;
import com.xhr.FileUpDown.download.DownloadRequest;
import com.xhr.FileUpDown.download.DownloadService;

public class MyActivity extends Activity {


    Button startDownloadBtn = null;
    Button pauseDownloadBtn = null;
    Button cancelDownloadBtn=null;
    ProgressBar downloadProgressBar = null;

    private final AbstractDownloadServiceReceiver downloadServiceReceiver = new AbstractDownloadServiceReceiver() {
        @Override
        public void onCompleted(String downloadId, int serverResponseCode, String serverResponseMessage) {
            super.onCompleted(downloadId, serverResponseCode, serverResponseMessage);
            Log.e("onCompleted", serverResponseMessage);
            downloadProgressBar.setProgress(100);
            Toast.makeText(MyActivity.this, "onCompleted", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel(String downloadId, Exception exception) {
            super.onCancel(downloadId, exception);
            Log.e("onCancel", exception.toString());
            downloadProgressBar.setProgress(0);
            Toast.makeText(MyActivity.this, "onCancel", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(String downloadId, Exception exception) {
            super.onError(downloadId, exception);
            Log.e("onError", exception.toString());
            Toast.makeText(MyActivity.this, "onError", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProgress(String downloadId, int progress) {
            super.onProgress(downloadId, progress);
            downloadProgressBar.setProgress(progress);
        }

        @Override
        public void onPause(String downloadId) {
            super.onPause(downloadId);
            Toast.makeText(MyActivity.this, "paused", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStarted(String downloadId) {
            super.onStarted(downloadId);
            Log.e("AbstractDownloadServiceReceiver", "Started");
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        initView();
    }

    private void initView() {
        startDownloadBtn = (Button) findViewById(R.id.startDownload);
        downloadProgressBar = (ProgressBar) findViewById(R.id.downloadProgress);
        pauseDownloadBtn = (Button) findViewById(R.id.pauseDownload);
        cancelDownloadBtn=(Button)findViewById(R.id.cancelDownload);

        startDownloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startDownload();
            }
        });

        pauseDownloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pause();
            }
        });

        cancelDownloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancel();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        downloadServiceReceiver.register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
      //  downloadServiceReceiver.unregister(this);
    }

    final String url = "http://img4.imgtn.bdimg.com/it/u=3685097454,3631803668&fm=21&gp=0.jpg";
  //  final String url = "http://192.168.62.9:8080/a.a";
    private void startDownload() {


        DownloadInfo info = new DownloadInfo();
        info.setId(url);
        info.setName(FileUtil.getFileName(url));
        info.setUrl(url);
        info.setMaxRetries(2);

        int nId = (int) (System.currentTimeMillis() % 10000000);
        NotificationConfig config = new NotificationConfig(info, R.drawable.ic_launcher,
                getString(R.string.app_name),
                "正在下载",
                "下载成功",
                "下载失败",
                "下载取消",
                "pause",
                false);
        DownloadRequest request = new DownloadRequest(MyActivity.this, info, config);
        try {
            DownloadService.startDownload(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pause() {
        DownloadService.pause(url);
    }

    private void cancel(){
        DownloadService.cancel(url);
    }
}
