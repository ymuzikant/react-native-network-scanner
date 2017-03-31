package com.ymuzikant.networkscanner.utils;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Yaron Muzikant on 30-Mar-17.
 */

public class NsdDiscovery {

    private static final String TAG = "NsdDiscovery";
    private NsdManager mNsdManager;

    /*private static String[] SERVICE_TYPES = {
            "_echo._tcp",
            "_googlecast._tcp",
            "_workstation._tcp",
            "_http._tcp",
            "_ftp._tcp",
            "_ipp._tcp",
            "_daap._tcp",
            "_netbios-ns._tcp",
            "_telnet._tcp",
            "_services._dns-sd._udp"
    };*/

    private final Map<String, NsdManager.DiscoveryListener> mActiveListeners = new ConcurrentHashMap<>();
    private final Map<String, NsdServiceInfo> mFoundServices = new ConcurrentHashMap<>();

    private NsdManager.DiscoveryListener mServiceDiscoveryListener = new NsdManager.DiscoveryListener() {
        @Override
        public void onStartDiscoveryFailed(String s, int i) {
            Log.d(TAG, "onStartDiscoveryFailed() called with: s = [" + s + "], i = [" + i + "]");
        }

        @Override
        public void onStopDiscoveryFailed(String s, int i) {
            Log.d(TAG, "onStopDiscoveryFailed() called with: s = [" + s + "], i = [" + i + "]");
        }

        @Override
        public void onDiscoveryStarted(String s) {
            Log.d(TAG, "onDiscoveryStarted() called with: s = [" + s + "]");
        }

        @Override
        public void onDiscoveryStopped(String s) {
            Log.d(TAG, "onDiscoveryStopped() called with: s = [" + s + "]");
        }

        @Override
        public void onServiceFound(NsdServiceInfo nsdServiceInfo) {
            Log.d(TAG, "onServiceFound() called with: nsdServiceInfo = [" + nsdServiceInfo + "]");

            synchronized (mActiveListeners) {
                if (!mActiveListeners.containsKey(getServiceFullName(nsdServiceInfo))) {
                    startServiceDiscovery(nsdServiceInfo);
                }
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo nsdServiceInfo) {
            Log.d(TAG, "onServiceLost() called with: nsdServiceInfo = [" + nsdServiceInfo + "]");
        }
    };

    private String getServiceFullName(NsdServiceInfo nsdServiceInfo) {
        return nsdServiceInfo.getServiceName() + "." + (nsdServiceInfo.getServiceType().contains("_tcp") ? "_tcp" : "_udp");
    }

    public NsdDiscovery(@NonNull Context context) {
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }

    public void startDiscovery() {
        Log.d(TAG, "Start discovery...");

        // Find all services published on the network
        mNsdManager.discoverServices("_services._dns-sd._udp", NsdManager.PROTOCOL_DNS_SD, mServiceDiscoveryListener);
    }

    public void stopDiscovery() {
        Log.d(TAG, "Stop discovery...");

        mNsdManager.stopServiceDiscovery(mServiceDiscoveryListener);

        synchronized (mActiveListeners) {
            for (NsdManager.DiscoveryListener activeListener : mActiveListeners.values()) {
                mNsdManager.stopServiceDiscovery(activeListener);
            }

            mActiveListeners.clear();
        }
    }

    public Collection<NsdServiceInfo> getFoundServices() {
        return mFoundServices.values();
    }

    private void startServiceDiscovery(NsdServiceInfo nsdServiceInfo) {
        NsdManager.DiscoveryListener serviceDiscoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String s, int i) {
                Log.d(TAG, "onStartDiscoveryFailed() called with: s = [" + s + "], i = [" + i + "]");

                synchronized (mActiveListeners) {
                    mActiveListeners.remove(s);
                }
            }

            @Override
            public void onStopDiscoveryFailed(String s, int i) {
                Log.d(TAG, "onStopDiscoveryFailed() called with: s = [" + s + "], i = [" + i + "]");
            }

            @Override
            public void onDiscoveryStarted(String s) {
                Log.d(TAG, "onDiscoveryStarted() called with: s = [" + s + "]");
            }

            @Override
            public void onDiscoveryStopped(String s) {
                Log.d(TAG, "onDiscoveryStopped() called with: s = [" + s + "]");
            }

            @Override
            public void onServiceFound(NsdServiceInfo nsdServiceInfo) {
                Log.d(TAG, "onServiceFound() called with: nsdServiceInfo = [" + nsdServiceInfo + "]");

                synchronized (mFoundServices) {
                    if (!mFoundServices.containsKey(nsdServiceInfo.toString())) {
                        mNsdManager.resolveService(nsdServiceInfo, new NsdManager.ResolveListener() {
                            @Override
                            public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int i) {
                                Log.d(TAG, "onResolveFailed() called with: nsdServiceInfo = [" + nsdServiceInfo + "], i = [" + i + "]");
                            }

                            @Override
                            public void onServiceResolved(NsdServiceInfo nsdServiceInfo) {
                                Log.d(TAG, "onServiceResolved() called with: nsdServiceInfo = [" + nsdServiceInfo + "]");

                                mFoundServices.put(nsdServiceInfo.toString(), nsdServiceInfo);
                            }
                        });
                    }
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo nsdServiceInfo) {
                Log.d(TAG, "onServiceLost() called with: nsdServiceInfo = [" + nsdServiceInfo + "]");
            }
        };

        String serviceTypeFullName = getServiceFullName(nsdServiceInfo);
        synchronized (mActiveListeners) {
            mActiveListeners.put(serviceTypeFullName, serviceDiscoveryListener);
        }

        mNsdManager.discoverServices(serviceTypeFullName, NsdManager.PROTOCOL_DNS_SD, serviceDiscoveryListener);
    }
}