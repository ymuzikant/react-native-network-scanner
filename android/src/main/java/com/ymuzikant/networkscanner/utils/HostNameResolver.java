package com.ymuzikant.networkscanner.utils;

import android.util.Log;

import java.net.InetAddress;

import jcifs.netbios.NbtAddress;

/**
 * Created by Yaron Muzikant on 14-Mar-17.
 */

public class HostNameResolver {
    private static final String TAG = "HostNameResolver";

    public String resolve(String ip) {
        String hostname = getNetbiosHostname(ip);

        if (hostname == null) {
            hostname = getHostnameWithInetAddress(ip);
        }

        return hostname;
    }

    private String getHostnameWithInetAddress(String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            return addr.getCanonicalHostName();
        } catch (Exception e) {
            Log.e(TAG, "getHostnameWithInetAddress", e);
        }

        return ip;
    }

    private String getNetbiosHostname(String ip) {
        try {
            NbtAddress[] nbts = NbtAddress.getAllByAddress(ip);
            return nbts[0].getHostName();
        } catch (Exception e) {
            return null;
        }
    }
}