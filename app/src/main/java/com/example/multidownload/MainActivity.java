package com.example.multidownload;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

public class MainActivity extends Activity implements View.OnClickListener {

    private Button button;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = (Button) findViewById(R.id.button);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        button.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        System.out.println("download start");
        String url = "http://dldir1.qq.com/qqfile/qq/TIM1.0.5/20303/TIM1.0.5.exe";
        new MultiDownload(url, "/sdcard/QQ.exe",8).startDownload();
    }
}
