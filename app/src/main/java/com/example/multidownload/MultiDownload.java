package com.example.multidownload;

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

    private String downloadUrl;
    private String storagePath;
    private int threadCount;

    private static final int TIMEOUT = 5000;

    public MultiDownload(String downloadUrl, String storagePath, int threadCount) {
        this.downloadUrl = downloadUrl;
        this.storagePath = storagePath;
        this.threadCount = threadCount;
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
                if (progressFile.exists()) {
                    DataInputStream dis = new DataInputStream(new FileInputStream(progressFile));
                    startIndex += dis.readInt();
                    dis.close();
                }

                HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(TIMEOUT);
                conn.setReadTimeout(TIMEOUT);
                conn.setRequestProperty("Range", "bytes=" + startIndex + "-" + endIndex);

                if (conn.getResponseCode() == HttpURLConnection.HTTP_PARTIAL) {
                    InputStream is = conn.getInputStream();
                    byte[] buffer = new byte[10240000];
                    int length;
                    int total = 0;

                    RandomAccessFile raf = new RandomAccessFile(new File(storagePath), "rwd");
                    raf.seek(startIndex);

                    RandomAccessFile progressRaf = new RandomAccessFile(progressFile, "rwd");

                    while((length = is.read(buffer)) != -1){
                        raf.write(buffer, 0, length);
                        total += length;

                        System.out.println(threadId+"    "+total);
                        progressRaf.seek(0);
                        progressRaf.writeInt(total);
                    }

                    raf.close();
                    progressRaf.close();

                    System.out.println("download finish:"+threadId);
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
