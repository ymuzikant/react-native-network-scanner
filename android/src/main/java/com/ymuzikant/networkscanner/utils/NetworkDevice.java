package com.ymuzikant.networkscanner.utils;

import java.util.ArrayList;

/**
 * Created by Yaron Muzikant on 14-Mar-17.
 */
public class NetworkDevice {
    private String IP;
    private String mac;
    private String hostname;
    private ArrayList<Integer> openPorts;

    public NetworkDevice(String ip, String mac) {
        this.IP = ip;
        this.mac = mac;
    }

    public NetworkDevice(String ip) {
        this.IP = ip;
        this.mac = null;
    }

    @Override
    public String toString() {
        return IP + ";" + mac + ";" + hostname + ";" + openPorts;
    }

    public String getIP() {
        return IP;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getHostname() {
        return hostname;
    }

    public void setOpenPorts(ArrayList<Integer> openPorts) {
        this.openPorts = openPorts;
    }

    public ArrayList<Integer> getOpenPorts() {
        return openPorts;
    }
}