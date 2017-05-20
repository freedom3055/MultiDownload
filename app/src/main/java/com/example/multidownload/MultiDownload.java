package com.example.multidownload;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by you on 2017/4/18.
 */

public class MultiDownload implements Runnable {

    private static final boolean DEBUG = true;
    private static final String TAG = MultiDownload.class.getSimpleName();
    private String downloadUrl;
    private String storagePath;
    private int threadCount;
    private OnProgressListener listener;
    private long currentProgress;
    private long totalProgress;

    private static final int TIMEOUT = 5000;

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            //super.handleMessage(msg);
            if (listener != null){
                currentProgress +=msg.arg1;
                listener.onProgressChange(currentProgress*1.0f/totalProgress);
            }
        }
    };



    public MultiDownload(String downloadUrl, String storagePath, int threadCount, OnProgressListener listener) {
        this.downloadUrl = downloadUrl;
        this.storagePath = storagePath;
        this.threadCount = threadCount;
        this.listener = listener;
    }

    public void startDownload() {
        new Thread(this).start();
    }

    @Override
    public void run() {

        try {
            URL url = new URL(downloadUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {

                int downloadFileLength = conn.getContentLength();
                RandomAccessFile raf = new RandomAccessFile(new File(storagePath), "rwd");
                raf.setLength(downloadFileLength);
                raf.close();

                if(DEBUG)Log.v(TAG,"download file size:"+downloadFileLength);

                totalProgress = downloadFileLength;
                int size = downloadFileLength / threadCount;
                for (int i = 0; i < threadCount; i++) {
                    int startIndex = i * size;
                    int endIndex = (i + 1) * size - 1;

                    if (i == threadCount - 1) {
                        endIndex = downloadFileLength - 1;
                    }

                    new DownloadThread(startIndex, endIndex, i).start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private class DownloadThread extends Thread {
        private int startIndex;
        private int endIndex;
        private int threadId;

        public DownloadThread(int startIndex, int endIndex, int threadId) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.threadId = threadId;
        }

        @Override
        public void run() {
            try {
                File progressFile = new File(new File(storagePath).getParent()+"/"+threadId);
                long downloadedLength = 0;
                if (progressFile.exists()) {
                    DataInputStream dis = new DataInputStream(new FileInputStream(progressFile));
                    downloadedLength = dis.readLong();
                    startIndex += downloadedLength;
                    dis.close();

                    Message msg = handler.obtainMessage();
                    msg.arg1 = (int)downloadedLength;
                    handler.sendMessage(msg);
                }

                HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(TIMEOUT);
                conn.setReadTimeout(TIMEOUT);
                conn.setRequestProperty("Range", "bytes=" + startIndex + "-" + endIndex);

                if (conn.getResponseCode() == HttpURLConnection.HTTP_PARTIAL) {
                    InputStream is = conn.getInputStream();
                    byte[] buffer = new byte[1024*10];
                    int length;
                    long total = downloadedLength;

                    RandomAccessFile raf = new RandomAccessFile(new File(storagePath), "rwd");
                    raf.seek(startIndex);

                    RandomAccessFile progressRaf = new RandomAccessFile(progressFile, "rwd");

                    while((length = is.read(buffer)) != -1){
                        raf.write(buffer, 0, length);
                        total += length;


                        progressRaf.seek(0);
                        progressRaf.writeLong(total);

                        Message msg = handler.obtainMessage();
                        msg.arg1 = length;
                        handler.sendMessage(msg);
                    }

                    raf.close();
                    progressRaf.close();

                    if(DEBUG)Log.v(TAG,"download finish:"+threadId);
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public interface OnProgressListener{
        void onProgressChange(float progress);
    }


}
