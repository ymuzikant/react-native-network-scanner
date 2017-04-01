package com.ymuzikant.networkscanner.utils;

import android.accounts.NetworkErrorException;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.jmdns.ServiceInfo;

/**
 * Created by Yaron Muzikant on 14-Mar-17.
 */

public class NetworkScanner {
    private static final String TAG = "NetworkScanner";

    private static final int CORE_POOL_SIZE = 10;
    private static final int MAXIMUM_POOL_SIZE = 25;

    private final int[] COMMON_PORTS = {80, 443, 22, 21, 25, 53, 3389, 23, 5000, 135, 139, 445, 5353, 67 ,68, 107, 110, 631};

    private Ping pinger = new Ping();
    private HostNameResolver hostNameResolver = new HostNameResolver();
    private PortScanner portScanner = new PortScanner();
    private SNMPHandler snmpHandler = new SNMPHandler();
    private NsdDiscovery mNsdDiscovery;

    public interface OnScanEventListener {
        void onDeviceFound(NetworkDevice device);

        void onScanCompleted(ArrayList<NetworkDevice> devices);

        void onError(Exception err);

        void onProgress(int progress, int total);
    }

    public void scan(final Context context, final OnScanEventListener onScanEventListener) {
        if (onScanEventListener == null) {
            return;
        }

        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if (wifiManager.isWifiEnabled() && wifiManager.getConnectionInfo() != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mNsdDiscovery = new NsdDiscovery(context);

                        // Get subnet mask and scan all IPs in subnet
                        try {
                            DhcpInfo dhcp = wifiManager.getDhcpInfo();

                            byte[] dhcpIPBytes = extractBytesFromDhcpIPAddress(dhcp.ipAddress);
                            InetAddress inetAddress = InetAddress.getByAddress(dhcpIPBytes);
                            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(inetAddress);
                            for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                                if (address.getAddress() instanceof Inet4Address) {
                                    scanSubnet(dhcp, address, onScanEventListener);
                                }
                            }
                        } catch (IOException e) {
                            Log.e(TAG, e.getMessage());
                        }
                    } catch (Exception ex) {
                        onScanEventListener.onError(ex);

                        Log.e(TAG, "Failed scan", ex);
                    }
                }
            }).start();
        } else {
            onScanEventListener.onError(new NetworkErrorException("Not connected to wifi"));
        }
    }

    private void scanSubnet(DhcpInfo dhcp, InterfaceAddress address, final OnScanEventListener onScanEventListener) {
        final ArrayList<NetworkDevice> devices = new ArrayList<>();

        short subnetPrefixLength = address.getNetworkPrefixLength();
        final int numOfDevicesInSubnet = (int) (Math.pow(2, 32 - subnetPrefixLength) - 2);

        if (numOfDevicesInSubnet <= 0) {
            Log.w(TAG, "No devices available for scan in subnet. scan aborted");

            onScanEventListener.onScanCompleted(devices);

            return;
        }

        // Initialize progress
        onScanEventListener.onProgress(0, numOfDevicesInSubnet);

        // Start NSD
        mNsdDiscovery.startDiscovery();

        // Build subnet mask
        int mask = buildMask(subnetPrefixLength);

        int currIP = dhcp.ipAddress;
        int maskedIP = mask & currIP;
        int[] currIPParts = getIPParts(maskedIP);

        Log.d(TAG, address + ", " + subnetPrefixLength + " ==> " + mask + " [" + getIpAddress(mask) + "]");

        DeviceScanCountDownLatch pingCounter = new DeviceScanCountDownLatch(new DeviceScanCountDownLatch.OnCountDownListener() {
            @Override
            public void onCountDown(int count) {
                onScanEventListener.onProgress(numOfDevicesInSubnet - count, numOfDevicesInSubnet);
            }
        }, numOfDevicesInSubnet);

        ThreadPoolExecutor threadsManager = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
                10, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(Math.max(CORE_POOL_SIZE, numOfDevicesInSubnet - (MAXIMUM_POOL_SIZE - CORE_POOL_SIZE))));

        for (int i = 0; i < numOfDevicesInSubnet; i++) {
            incrementIP(currIPParts);

            int nextIP = buildIPFromParts(currIPParts);
            String nextIPDisplay = getIpAddress(nextIP);

            if ((nextIP & mask) == maskedIP) {
                scanIP(nextIPDisplay, threadsManager, pingCounter, devices, onScanEventListener);
            } else {
                Log.d(TAG, "IP not matching subnet mask [" + nextIPDisplay + "]");
                pingCounter.countDown();
            }
        }

        // Wait for all pings to complete
        try {
            pingCounter.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        threadsManager.shutdown();

        // Get device details from ARP table
        ARP arp = new ARP();
        ArrayList<NetworkDevice> arpDevices = arp.getDevices();

        for (NetworkDevice arpDevice : arpDevices) {
            for (NetworkDevice networkDevice : devices) {
                if (arpDevice.getIP().equals(networkDevice.getIP())) {
                    networkDevice.setMac(arpDevice.getMac());
                    break;
                }
            }
        }

        // Get all resolved services by NSD
        mNsdDiscovery.stopDiscovery();
        for (NsdServiceInfo nsdServiceInfo : mNsdDiscovery.getFoundServices()) {
            // TODO: Update devices according to found services

            Log.d(TAG, "Service found [" + nsdServiceInfo + "]");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // TODO: Handle TXT record of resolved service
                // http://stackoverflow.com/questions/25984894/android-mdns-txt-record

                for (Map.Entry<String, byte[]> nsdServiceInfoAttributeEntry : nsdServiceInfo.getAttributes().entrySet()) {
                    Log.d(TAG, "\t" + nsdServiceInfoAttributeEntry.getKey() + " ===> " + new String(nsdServiceInfoAttributeEntry.getValue()));
                }
            }
        }

        for (ServiceInfo serviceInfo : mNsdDiscovery.getFoundServices2()) {
            Log.d(TAG, "Service found [" + serviceInfo + "]");
        }

        onScanEventListener.onScanCompleted(devices);

        Log.i(TAG, "scan finished");
    }

    private void scanIP(final String ip, ThreadPoolExecutor threadsManager, final CountDownLatch pingCounter, final ArrayList<NetworkDevice> devices, final OnScanEventListener onScanEventListener) {
        threadsManager.execute(new Runnable() {
            @Override
            public void run() {
                boolean isAvailable = pinger.ping(ip);

                if (isAvailable) {
                    NetworkDevice networkDevice = new NetworkDevice(ip);

                    String hostname = hostNameResolver.resolve(ip);
                    networkDevice.setHostname(hostname);

                    // Run port scan for device
                    ArrayList<Integer> openPorts = portScanner.scanPorts(ip, COMMON_PORTS);
                    if (openPorts != null && openPorts.size() > 0) {
                        networkDevice.setOpenPorts(openPorts);

                        for (Integer openPort : openPorts) {
                            JSONObject portExtraInfo = portScanner.getPortExtraInfo(ip, openPort);
                            if (portExtraInfo != null) {
                                networkDevice.setPortExtraInfo(openPort, portExtraInfo);
                            }
                        }
                    }

                    // Fetch SNMP info
                    JSONObject snmpInfo = snmpHandler.getSNMPInfo(ip);
                    if (snmpInfo != null) {
                        networkDevice.setSNMPInfo(snmpInfo);
                    }

                    devices.add(networkDevice);

                    onScanEventListener.onDeviceFound(networkDevice);
                }

                pingCounter.countDown();
            }
        });
    }

    private int buildMask(int netPrefix) {
        int[] maskParts = new int[4];
        int currMaskLength = 32;
        for (int i = 0; i < 4; i++) {
            int currClassMaskLength = Math.min(8, Math.max(currMaskLength - netPrefix, 0));
            maskParts[i] = (int) (Math.pow(2, currClassMaskLength) - 1);

            currMaskLength -= 8;
        }

        return ~buildIPFromParts(maskParts);
    }

    private int buildIPFromParts(int[] ipParts) {
        return ipParts[3] + (ipParts[2] << 8) + (ipParts[1] << 16) + (ipParts[0] << 24);
    }

    private void incrementIP(int[] ip) {
        for (int i = 0; i < 4; ++i) {
            if (ip[i] == 255) {
                ip[i] = 1;
            } else {
                ++ip[i];
                break;
            }
        }
    }

    private int[] getIPParts(int ip) {
        return new int[]{
                ip >> 24 & 0xff,
                ip >> 16 & 0xff,
                ip >> 8 & 0xff,
                ip & 0xff};
    }

    private byte[] extractBytesFromDhcpIPAddress(int ip) {
        return new byte[]{
                (byte) (ip & 0xff),
                (byte) (ip >> 8 & 0xff),
                (byte) (ip >> 16 & 0xff),
                (byte) (ip >> 24 & 0xff)};
    }

    @SuppressLint("DefaultLocale")
    private String getIpAddress(int ip) {
        return String.format("%d.%d.%d.%d",
                (ip & 0xff),
                (ip >> 8 & 0xff),
                (ip >> 16 & 0xff),
                (ip >> 24 & 0xff));
    }

    private static class DeviceScanCountDownLatch extends CountDownLatch {
        private final OnCountDownListener onCountDownListener;

        interface OnCountDownListener {
            void onCountDown(int count);
        }

        DeviceScanCountDownLatch(OnCountDownListener onCountDownListener, int count) {
            super(count);

            this.onCountDownListener = onCountDownListener;
        }

        @Override
        public void countDown() {
            synchronized (this) {
                if (onCountDownListener != null) {
                    onCountDownListener.onCountDown((int) (getCount() - 1));
                }

                super.countDown();
            }
        }
    }
}