package com.ymuzikant.networkscanner.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

/**
 * Created by Yaron Muzikant on 14-Mar-17.
 */
public class NetworkDevice {
    private String IP;
    private String mac;
    private String hostname;
    private ArrayList<Integer> openPorts;
    private Hashtable<Integer, JSONObject> portExtraInfoMap;
    private JSONObject snmpInfo;

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
        return toJson().toString();
    }

    public JSONObject toJson() {
        JSONObject deviceJson = new JSONObject();

        try {
            if (IP != null) {
                deviceJson.put("ip", IP);
            }

            if (mac != null) {
                deviceJson.put("mac", mac);
            }

            if (hostname != null) {
                deviceJson.put("hostname", hostname);
            }

            if (openPorts != null) {
                JSONArray openPortsJsonArr = new JSONArray();
                for (Integer openPort : openPorts) {
                    openPortsJsonArr.put(openPort);
                }
                deviceJson.put("openPorts", openPortsJsonArr);
            }

            if (portExtraInfoMap != null) {
                JSONArray portsExtraInfoJsonArr = new JSONArray();
                for (Map.Entry<Integer, JSONObject> portExtraInfoEntry : portExtraInfoMap.entrySet()) {
                    JSONObject portExtraInfo = new JSONObject();
                    portExtraInfo.put("port", portExtraInfoEntry.getKey());
                    portExtraInfo.put("info", portExtraInfoEntry.getValue());

                    portsExtraInfoJsonArr.put(portExtraInfo);
                }
                deviceJson.put("portsInfo", portsExtraInfoJsonArr);
            }

            if (snmpInfo != null) {
                deviceJson.put("snmpInfo", snmpInfo);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return deviceJson;
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

    public void setPortExtraInfo(Integer port, JSONObject portExtraInfo) {
        if (portExtraInfoMap == null) {
            portExtraInfoMap = new Hashtable<>();
        }

        portExtraInfoMap.put(port, portExtraInfo);
    }

    public Hashtable<Integer, JSONObject> getPortExtraInfoMap() {
        return portExtraInfoMap;
    }

    public void setSNMPInfo(JSONObject snmpInfo) {
        this.snmpInfo = snmpInfo;
    }

    public JSONObject getSNMPInfo() {
        return snmpInfo;
    }
}