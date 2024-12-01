package com.example.sharefile;

import jcifs.CIFSContext;
import jcifs.CIFSException;
import jcifs.config.BaseConfiguration;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.concurrent.*;

public class SmbFileMultiThreadedDownload {

    private static int NUMBER_OF_THREADS = 4;
    private static final int BUFFER_SIZE = 8192 * 10; // 8KB buffer
    //    private static String SMB_URL = "smb://server/share/path/to/largefile.ext";
//    private static String TEMP_DIR = "C:/temp/";
//    private static String FINAL_FILE_NAME = "C:/final/downloaded_largefile.ext";
//    private static final String username = "Administrator";
//    private static final String password = "123456";
    private static Callback callback;

    public static void main(String username, String password, String SMB_URL, String TEMP_DIR, String FINAL_FILE_NAME, Callback callback) {
        SmbFileMultiThreadedDownload.callback = callback;
        CIFSContext cifsContext = getCIFSContext(username, password);
        List<Future<?>> futures = new ArrayList<>();
        long fileSize = getFileSize(SMB_URL, cifsContext);
        if (fileSize > 1024 * 1024) {
            NUMBER_OF_THREADS = 10;
        }
        long partSize = fileSize / NUMBER_OF_THREADS;
        System.out.println("===============下载开始 fileSize: " + fileSize / 1024 / 1024 + "M");
        ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        long currentTimeMillis1 = System.currentTimeMillis();
        try {
            for (int i = 0; i < NUMBER_OF_THREADS; i++) {
                long start = i * partSize;
                long end = (i == NUMBER_OF_THREADS - 1) ? fileSize : start + partSize - 1;
                futures.add(executor.submit(new DownloadTask(SMB_URL, TEMP_DIR, i, start, end, cifsContext)));
            }

            // 等待所有任务完成
            for (Future<?> future : futures) {
                future.get();
            }

            // 合并文件部分
            mergeFileParts(TEMP_DIR, FINAL_FILE_NAME, NUMBER_OF_THREADS);
            long currentTimeMillis2 = System.currentTimeMillis();
            long time = (currentTimeMillis2 - currentTimeMillis1) / 1000;
            String msg = "===============下载完成 用时: " + time + "s" + "  平均: " + fileSize * 1.0f / 1024 / 1024 / time + "M/s";
            callback.success(msg);
            System.out.println(msg);
        } catch (Exception e) {
            String msg = "===============下载失败";
            callback.success(msg);
            System.out.println(msg);
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    private static long getFileSize(String smbUrl, CIFSContext cifsContext) {
        try {
            SmbFile smbFile = new SmbFile(smbUrl, cifsContext);
            return smbFile.length();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static class DownloadTask implements Callable<Void> {
        private final String smbUrl;
        private final String tempDir;
        private final int partNumber;
        private final long start;
        private final long end;
        private CIFSContext cifsContext;

        public DownloadTask(String smbUrl, String tempDir, int partNumber, long start, long end, CIFSContext cifsContext) {
            this.smbUrl = smbUrl;
            this.tempDir = tempDir;
            this.partNumber = partNumber;
            this.start = start;
            this.end = end;
            this.cifsContext = cifsContext;
        }

        @Override
        public Void call() throws Exception {
            String partFileName = tempDir + "file_part_" + partNumber + ".tmp";
            try (SmbFile smbFile = new SmbFile(smbUrl, cifsContext);
                 SmbFileInputStream smbFileInputStream = new SmbFileInputStream(smbFile);
                 RandomAccessFile raf = new RandomAccessFile(partFileName, "rw")) {

                URL url = smbFile.getURL();

                System.out.println(url.getProtocol() + "://" + url.getAuthority() + url.getPath() + partFileName + "  start:" + start + "  end:" + end);

//                smbFileInputStream.seek(start);
                smbFileInputStream.skip(start);
                byte[] buffer = new byte[BUFFER_SIZE];
                long bytesReadTotal = 0;

                while (bytesReadTotal < (end - start + 1)) {
                    int bytesRead = smbFileInputStream.read(buffer, 0, (int) Math.min(BUFFER_SIZE, (end - start + 1) - bytesReadTotal));
                    if (bytesRead == -1) break;
                    raf.write(buffer, 0, bytesRead);
                    bytesReadTotal += bytesRead;
                    if (partNumber == 0) {
                        callback.progress(bytesReadTotal * 1.0f / end);
                    }
                }
            }
            return null;
        }
    }

    private static void mergeFileParts(String tempDir, String finalFileName, int numberOfParts) throws IOException {
        File tempDirFile = new File(tempDir);
        File[] partFiles = tempDirFile.listFiles((dir, name) -> name.startsWith("file_part_") && name.endsWith(".tmp"));

        if (partFiles == null || partFiles.length != numberOfParts) {
            throw new IOException("Partial files are missing or the number of parts is incorrect.");
        }
        TreeSet<String> filenameSet = new TreeSet<>();
        HashMap<String, File> map = new HashMap<>();
        for (File partFile : partFiles) {
            String name = partFile.getName();
            filenameSet.add(name);
            map.put(name, partFile);
        }

        try (FileOutputStream fos = new FileOutputStream(finalFileName)) {
            for (String name : filenameSet) {
                File partFile = map.get(name);
                System.out.println("mergeFileParts " + partFile.getName());
                try (FileInputStream fis = new FileInputStream(partFile)) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
                // 可选：删除部分文件以节省空间
                partFile.delete();
            }

        }
    }

    private static CIFSContext getCIFSContext(String username, String password) {
        try {
            // 创建配置属性
            Properties properties = new Properties();
            properties.setProperty("jcifs.smb.client.responseTimeout", "30000"); // 设置响应超时
            properties.setProperty("jcifs.smb.client.soTimeout", "30000"); // 设置套接字超时

            // 创建配置对象
            BaseConfiguration config = null;

            config = new PropertyConfiguration(properties);


            // 创建 BaseContext
            CIFSContext baseContext = new BaseContext(config);

            // 设置认证信息
            NtlmPasswordAuthenticator auth = new NtlmPasswordAuthenticator(username, password);
            CIFSContext authContext = baseContext.withCredentials(auth);
            return authContext;
        } catch (CIFSException e) {
            e.printStackTrace();
            return null;
        }
    }

    interface Callback {
        void success(String msg);

        void fail(String msg);

        void progress(float progress);
    }

}