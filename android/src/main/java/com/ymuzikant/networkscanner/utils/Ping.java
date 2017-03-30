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
        Process pingProcess = null;
        try {
            // TODO: Make sure "timeout" exists on device and in the expected path (or search for it in multiple paths)
            // TODO: Make sure "ping" exists on device and in the expected path (or search for it in multiple paths)
            pingProcess = runtime.exec("/system/bin/timeout 0.5 /system/bin/ping -w 1 -c 1 " + host);
            int pingResult = pingProcess.waitFor();

            Log.v(TAG, "Ping " + host + " result: " + pingResult);
            if (pingResult == 0) {
                return true;
            } else {
                return false;
            }
        } catch (Exception ex) {
            Log.i(TAG, "Failed ping to host " + host + "[" + ex.getMessage() + "]", ex);
        } finally {
            if (pingProcess != null) {
                pingProcess.destroy();
            }
        }

        return false;
    }
}
