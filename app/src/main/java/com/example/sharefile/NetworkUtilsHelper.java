package com.example.sharefile;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;

public class NetworkUtilsHelper {

    private static final String TAG = "NetworkUtilsHelper";

    public static String getLocalIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null && wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                return intToIp(wifiInfo.getIpAddress());
            }
        }

        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress inetAddress : Collections.list(networkInterface.getInetAddresses())) {
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            Log.e(TAG, "Failed to get local IP address", e);
        }

        return null;
    }

    @SuppressLint("DefaultLocale")
    private static String intToIp(int mask) {
        return String.format("%d.%d.%d.%d",
                (int) (mask & 0xff),
                (int) ((mask >> 8) & 0xff),
                (int) ((mask >> 16) & 0xff),
                (int) ((mask >> 24) & 0xff)
        );
    }
    // 将IP地址转换为整数表示
    public static int ipToInt(String ipAddress) throws UnknownHostException {
        byte[] addrBytes = InetAddress.getByName(ipAddress).getAddress();
        return ByteBuffer.wrap(addrBytes).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    public static String getSubnetMask(Context context) {
        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress inetAddress = interfaceAddress.getAddress();
                    if (!inetAddress.isLoopbackAddress()) {
                        if (inetAddress instanceof Inet4Address) {
                            // 对于 IPv4 地址，计算子网掩码
                            int prefixLength = interfaceAddress.getNetworkPrefixLength();
                            return convertNumericSubnetToDottedDecimal(prefixLength);
                        } else if (inetAddress instanceof Inet6Address) {
                            // 对于 IPv6 地址，你可能不需要或无法以相同的方式计算子网掩码
                            // 因为 IPv6 使用的是不同的子网划分方式（CIDR 表示法）
                            // 这里可以返回 null 或者一个表示 IPv6 不适用子网掩码的字符串
                            // 例如：return "IPv6 does not use subnet masks in the same way as IPv4";
                            // 但在这个例子中，我们简单地跳过 IPv6 地址
                        }
                    }
                }
            }
        } catch (SocketException e) {
            Log.e(TAG, "Failed to get subnet mask", e);
        }
        return null; // 如果没有找到有效的 IPv4 地址，则返回 null
    }

    // 将子网前缀长度转换为点分十进制格式的子网掩码字符串
    public static String convertNumericSubnetToDottedDecimal(int prefixLength) {
        if (prefixLength < 0 || prefixLength > 32) {
            throw new IllegalArgumentException("Invalid prefix length for IPv4: " + prefixLength);
        }

        long mask = ~((1L << (32 - prefixLength)) - 1);
        return String.format("%d.%d.%d.%d",
                (int) ((mask >> 24) & 0xff),
                (int) ((mask >> 16) & 0xff),
                (int) ((mask >> 8) & 0xff),
                (int) (mask & 0xff));
    }


    public static void main(Context context) {
        String ipAddress = NetworkUtilsHelper.getLocalIpAddress(context);
        String subnetMask = NetworkUtilsHelper.getSubnetMask(context);

        Log.i("NetworkInfo", "=====IP Address: " + ipAddress);
        Log.i("NetworkInfo", "=====Subnet Mask: " + subnetMask);
    }

}