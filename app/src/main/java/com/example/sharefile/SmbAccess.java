package com.example.sharefile;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import jcifs.CIFSContext;
import jcifs.CIFSException;
import jcifs.config.BaseConfiguration;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;

public class SmbAccess {

    public static List<SmbFile> accessSharedFolder(String smbUrl, String username, String password) throws Exception {

        List<SmbFile> list = new ArrayList<>();

        CIFSContext authContext = getCIFSContext(username, password);
        // 访问共享文件夹
//             smbUrl = "smb://192.168.31.99/SharedFolder/Signal/Android/Signal-latest";
        System.out.println("smbUrl= " + smbUrl);
        SmbFile smbFile = new SmbFile(smbUrl, authContext);

        // 列出文件
        for (SmbFile file : smbFile.listFiles()) {
            list.add(file);
            System.out.println("File: " + file.getName());
        }

        return list;
    }

    private static CIFSContext getCIFSContext(String username, String password) throws CIFSException {
        // 创建配置属性
        Properties properties = new Properties();
        properties.setProperty("jcifs.smb.client.responseTimeout", "30000"); // 设置响应超时
        properties.setProperty("jcifs.smb.client.soTimeout", "30000"); // 设置套接字超时

        // 创建配置对象
        BaseConfiguration config = new PropertyConfiguration(properties);

        // 创建 BaseContext
        CIFSContext baseContext = new BaseContext(config);

        // 设置认证信息
        NtlmPasswordAuthenticator auth = new NtlmPasswordAuthenticator(username, password);
        CIFSContext authContext = baseContext.withCredentials(auth);
        return authContext;
    }


    public static void downloadFile(SmbFile smbFile, String localFilePath, Callback callback) {
        try (InputStream inputStream = new SmbFileInputStream(smbFile);
             OutputStream outputStream = new FileOutputStream(localFilePath)) {

            float length = smbFile.length();
            float progess = 0;

            byte[] buffer = new byte[4096];
            int bytesRead;

            callback.progress(length, 0);
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                progess += bytesRead;
                callback.progress(length, progess);
            }
            callback.success();
        } catch (Exception e) {
            e.printStackTrace();
            callback.fail();
        }
    }

    interface Callback {
        void success();

        void fail();

        void progress(float length, float progess);//进度

    }

}
