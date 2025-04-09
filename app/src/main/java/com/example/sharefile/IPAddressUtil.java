package com.example.sharefile;

import android.content.Context;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class IPAddressUtil {

    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface interface1 : interfaces) {
                List<InetAddress> addresses = Collections.list(interface1.getInetAddresses());
                for (InetAddress address : addresses) {
                    if (!address.isLoopbackAddress()) {
                        String sAddr = address.getHostAddress();
                        boolean isIPv4 = IPAddressUtil.isIPv4Address(sAddr);

                        if (useIPv4) {
                            if (isIPv4) {
                                return sAddr;
                            }
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim == -1 ? sAddr : sAddr.substring(0, delim);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static String getLocalIPAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && IPAddressUtil.isIPv4Address(inetAddress.getHostAddress())) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // Utility method to check if the address is IPv4
    public static boolean isIPv4Address(String ipAddress) {
        try {
            long result = 0;
            boolean ipv4 = true;
            String[] octets = ipAddress.split("\\.");
            for (String octet : octets) {
                int value = Integer.parseInt(octet);
                result = (result << 8) | (value & 0xFF);
                if (value > 255 || value < 0) {
                    ipv4 = false;
                }
            }
            return ipv4 && result != 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    // WiFi-specific IP address retrieval
    public static String getWifiIPAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            return String.format("%d.%d.%d.%d",
                    (ipAddress & 0xff),
                    (ipAddress >> 8 & 0xff),
                    (ipAddress >> 16 & 0xff),
                    (ipAddress >> 24 & 0xff));
        }
        return null;
    }


    public static void main(Context context){ //在同一网段

        // 获取IPv4地址
        String ipv4Address = IPAddressUtil.getIPAddress(true);
        // 获取本地IP地址（默认IPv4）
        String localIPAddress = IPAddressUtil.getLocalIPAddress();
        // 获取WiFi连接时的IP地址
        String wifiIPAddress = IPAddressUtil.getWifiIPAddress(context);

        System.out.println("=============ipv4Address "+ipv4Address);
        System.out.println("=============localIPAddress "+localIPAddress);
        System.out.println("=============wifiIPAddress "+wifiIPAddress);

    }

}