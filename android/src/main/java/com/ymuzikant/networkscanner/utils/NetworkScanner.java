package com.ymuzikant.networkscanner.utils;

import android.accounts.NetworkErrorException;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Yaron Muzikant on 14-Mar-17.
 */

public class NetworkScanner {
    private static final String TAG = "NetworkScanner";

    private Ping pinger = new Ping();
    private HostNameResolver hostNameResolver = new HostNameResolver();
    private PortScanner portScanner = new PortScanner();

    public interface OnScanEventListener {
        void onDeviceFound(NetworkDevice device);

        void onScanCompleted(ArrayList<NetworkDevice> devices);

        void onError(Exception err);
    }

    public void scan(Context context, final OnScanEventListener onScanEventListener) {
        if (onScanEventListener == null) {
            return;
        }

        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if (wifiManager.isWifiEnabled() && wifiManager.getConnectionInfo() != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
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

    private void scanSubnet(DhcpInfo dhcp, InterfaceAddress address, OnScanEventListener onScanEventListener) {
        final ArrayList<NetworkDevice> devices = new ArrayList<>();

        short subnetPrefixLength = address.getNetworkPrefixLength();
        int numOfDevicesInSubnet = (int) (Math.pow(2, 32 - subnetPrefixLength) - 2);

        if (numOfDevicesInSubnet <= 0) {
            Log.w(TAG, "No devices available for scan in subnet. scan aborted");

            return;
        }

        int mask = buildMask(subnetPrefixLength);

        int currIP = dhcp.ipAddress;
        int maskedIP = mask & currIP;
        int[] currIPParts = getIPParts(maskedIP);

        Log.d(TAG, address + ", " + subnetPrefixLength + " ==> " + mask + " [" + getIpAddress(mask) + "]");

        CountDownLatch pingCounter = new CountDownLatch(numOfDevicesInSubnet);
        ThreadPoolExecutor threadsManager =
                new ThreadPoolExecutor(20, 20, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(numOfDevicesInSubnet));

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
                    ArrayList<Integer> openPorts = portScanner.scanPorts(ip, 80, 443);
                    if (openPorts != null && openPorts.size() > 0) {
                        networkDevice.setOpenPorts(openPorts);
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
}