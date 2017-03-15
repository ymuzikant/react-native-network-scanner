package com.ymuzikant.networkscanner.utils;

import java.net.InetAddress;

/**
 * Created by Yaron Muzikant on 14-Mar-17.
 */

public class HostNameResolver {
    public String resolve(String ip) {
        String[] ipParts = ip.split("\\.");
        byte[] ipAddr = new byte[]{
                Integer.valueOf(ipParts[0]).byteValue(),
                Integer.valueOf(ipParts[1]).byteValue(),
                Integer.valueOf(ipParts[2]).byteValue(),
                Integer.valueOf(ipParts[3]).byteValue()};

        try {
            InetAddress addr = InetAddress.getByAddress(ipAddr);
            return addr.getCanonicalHostName();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}