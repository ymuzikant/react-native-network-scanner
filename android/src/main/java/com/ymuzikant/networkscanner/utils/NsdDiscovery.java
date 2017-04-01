package com.ymuzikant.networkscanner.utils;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;

/**
 * Created by Yaron Muzikant on 30-Mar-17.
 */

public class NsdDiscovery {

    private static final String TAG = "NsdDiscovery";

    private JmDNS mJmdns;
    private final NsdManager mNsdManager;

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

    private final Map<String, ServiceListener> mJmDnsActiveListeners = new ConcurrentHashMap<>();
    private final Map<String, ServiceInfo> mJmDnsFoundServices = new ConcurrentHashMap<>();

    private ServiceTypeListener mJmDnsServiceTypeListener = new ServiceTypeListener() {
        @Override
        public void serviceTypeAdded(ServiceEvent event) {
            Log.d(TAG, "serviceTypeAdded() called with: event = [" + event + "]");

            if (!mJmDnsActiveListeners.containsKey(event.getType())) {
                ServiceListener serviceListener = new ServiceListener() {
                    @Override
                    public void serviceAdded(ServiceEvent event) {
                        Log.d(TAG, "serviceAdded() called with: event = [" + event + "]");

                        mJmDnsFoundServices.put(getServiceEventKey(event), event.getInfo());
                    }

                    @Override
                    public void serviceRemoved(ServiceEvent event) {
                        Log.d(TAG, "serviceRemoved() called with: event = [" + event + "]");
                    }

                    @Override
                    public void serviceResolved(ServiceEvent event) {
                        Log.d(TAG, "serviceResolved() called with: event = [" + event + "]");

                        // Overwrite info of same service added in serviceAdded callback
                        mJmDnsFoundServices.put(getServiceEventKey(event), event.getInfo());
                    }

                    @NonNull
                    private String getServiceEventKey(ServiceEvent event) {
                        return event.getName() + event.getType();
                    }
                };

                mJmDnsActiveListeners.put(event.getType(), serviceListener);
                mJmdns.addServiceListener(event.getType(), serviceListener);
            }
        }

        @Override
        public void subTypeForServiceTypeAdded(ServiceEvent event) {
            Log.d(TAG, "subTypeForServiceTypeAdded() called with: event = [" + event + "]");
        }
    };

    private String getServiceFullName(NsdServiceInfo nsdServiceInfo) {
        return nsdServiceInfo.getServiceName() + "." + (nsdServiceInfo.getServiceType().contains("_tcp") ? "_tcp" : "_udp");
    }

    public NsdDiscovery(@NonNull Context context) {
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);

        try {
            mJmdns = JmDNS.create(InetAddress.getLocalHost());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startDiscovery() {
        Log.d(TAG, "Start discovery...");

        // Stop active listeners
        stopNsdDiscoveryListeners();
        stopJmDnsActiveListeners();

        // Find all services published on the network
        mNsdManager.discoverServices("_services._dns-sd._udp", NsdManager.PROTOCOL_DNS_SD, mServiceDiscoveryListener);

        try {
            mJmdns.addServiceTypeListener(mJmDnsServiceTypeListener);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopDiscovery() {
        Log.d(TAG, "Stop discovery...");

        mNsdManager.stopServiceDiscovery(mServiceDiscoveryListener);
        stopNsdDiscoveryListeners();

        mJmdns.removeServiceTypeListener(mJmDnsServiceTypeListener);
        stopJmDnsActiveListeners();
    }

    private void stopJmDnsActiveListeners() {
        synchronized (mJmDnsActiveListeners) {
            for (Map.Entry<String, ServiceListener> serviceListenerEntry : mJmDnsActiveListeners.entrySet()) {
                Log.d(TAG, "jmDns stopDiscovery() called for service [" + serviceListenerEntry.getKey() + "]");

                mJmdns.removeServiceListener(serviceListenerEntry.getKey(), serviceListenerEntry.getValue());
            }

            mJmDnsActiveListeners.clear();
        }
    }

    private void stopNsdDiscoveryListeners() {
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

    public Collection<ServiceInfo> getFoundServices2() {
        return mJmDnsFoundServices.values();
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