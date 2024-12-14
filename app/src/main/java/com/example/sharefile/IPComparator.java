package com.example.sharefile;

import android.content.Context;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class IPComparator {

    // 将IP地址转换为整数表示
    public static int ipToInt(String ipAddress) throws UnknownHostException {
        byte[] addrBytes = InetAddress.getByName(ipAddress).getAddress();
        return ByteBuffer.wrap(addrBytes).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    // 判断两个IP地址是否在同一网段
    public static boolean areInSameSubnet(String ip1, String subnetMask, String ip2) throws UnknownHostException {
        int ip1Int = ipToInt(ip1);
        int ip2Int = ipToInt(ip2);
        int maskInt = ipToInt(subnetMask.replace(".", "255.")); // 将子网掩码转换为整数，例如"255.255.255.0"

        // 获取网络地址
        int network1 = ip1Int & maskInt;
        int network2 = ip2Int & maskInt;

        // 比较网络地址是否相同
        return network1 == network2;
    }

    public static boolean areInSameSubnet2(Context context, String ip1) {
        try {
            //String ip1 = "192.168.1.10";
            String subnetMask = NetworkUtilsHelper.getSubnetMask(context);
            String ipAddress = NetworkUtilsHelper.getLocalIpAddress(context);

            boolean sameSubnet = areInSameSubnet(ip1, subnetMask, ipAddress);
            System.out.println("Are " + ip1 + " and " + ipAddress + " in the same subnet? " + sameSubnet);
            return sameSubnet;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void main(String[] args) {
        try {
            String ip1 = "192.168.1.10";
            String subnetMask = "255.255.255.0";
            String ip2 = "192.168.1.20";

            boolean sameSubnet = areInSameSubnet(ip1, subnetMask, ip2);
            System.out.println("Are " + ip1 + " and " + ip2 + " in the same subnet? " + sameSubnet);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }


}