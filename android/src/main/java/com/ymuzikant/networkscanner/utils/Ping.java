package com.ymuzikant.networkscanner.utils;

import android.util.Log;

/**
 * Created by Yaron Muzikant on 14-Mar-17.
 */

public class Ping {
    private static final String TAG = "Ping";

    public boolean ping(String host) {
        Log.d(TAG, "ping() called with: host = [" + host + "]");

        Runtime runtime = Runtime.getRuntime();
        try {
            Process pingProcess = runtime.exec("/system/bin/ping -c 1 " + host);
            int pingResult = pingProcess.waitFor();

            Log.v(TAG, "Ping " + host + " result: " + pingResult);
            if (pingResult == 0) {
                return true;
            } else {
                return false;
            }
        } catch (Exception ex) {
            Log.i(TAG, "Failed ping to host " + host + "[" + ex.getMessage() + "]");
        }
        return false;
    }
}
