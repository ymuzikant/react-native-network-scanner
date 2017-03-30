package com.ymuzikant.networkscanner;

import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.ymuzikant.networkscanner.utils.NetworkDevice;
import com.ymuzikant.networkscanner.utils.NetworkScanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Yaron Muzikant on 13-Mar-17.
 */

public class NetworkScannerModule extends ReactContextBaseJavaModule {
    private static final String TAG = "NetworkScannerModule";

    public NetworkScannerModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "NetworkScanner";
    }

    @ReactMethod
    public void scan(final Callback onScanFinished, final Callback onError) {
        final DeviceEventManagerModule.RCTDeviceEventEmitter eventEmitter =
                getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);

        NetworkScanner networkScanner = new NetworkScanner();
        networkScanner.scan(getReactApplicationContext(), new NetworkScanner.OnScanEventListener() {
            @Override
            public void onDeviceFound(NetworkDevice device) {
                WritableNativeMap networkDeviceInfo = deviceToWriteableMap(device);

                eventEmitter.emit("deviceFound", networkDeviceInfo);
            }

            @Override
            public void onScanCompleted(ArrayList<NetworkDevice> devices) {
                if (onScanFinished != null) {
                    WritableNativeArray devicesArray = new WritableNativeArray();

                    for (NetworkDevice device : devices) {
                        Log.d(TAG, "onScanCompleted() device = [" + device.toString() + "]");

                        WritableNativeMap networkDeviceInfo = deviceToWriteableMap(device);

                        devicesArray.pushMap(networkDeviceInfo);
                    }

                    onScanFinished.invoke(devicesArray);
                }
            }

            @Override
            public void onError(Exception err) {
                if (onError != null) {
                    onError.invoke(err.getMessage());
                }
            }

            @Override
            public void onProgress(int progress, int total) {
                WritableNativeMap progressInfo = new WritableNativeMap();
                progressInfo.putInt("progress", progress);
                progressInfo.putInt("total", total);

                eventEmitter.emit("progress", progressInfo);
            }
        });
    }

    private WritableNativeMap deviceToWriteableMap(NetworkDevice device) {
        WritableNativeMap networkDeviceInfo = new WritableNativeMap();
        networkDeviceInfo.putString("ip", device.getIP());

        if (device.getMac() != null) {
            networkDeviceInfo.putString("mac", device.getMac());
        }

        if (device.getHostname() != null) {
            networkDeviceInfo.putString("hostname", device.getHostname());
        }

        if (device.getOpenPorts() != null && device.getOpenPorts().size() > 0) {
            WritableNativeArray openPortsArray = new WritableNativeArray();

            for (Integer openPort : device.getOpenPorts()) {
                openPortsArray.pushInt(openPort);
            }
            networkDeviceInfo.putArray("openPorts", openPortsArray);
        }

        if (device.getPortExtraInfoMap() != null && device.getPortExtraInfoMap().size() > 0) {
            WritableNativeArray portsExtraInfoArray = new WritableNativeArray();

            for (Map.Entry<Integer, JSONObject> portExtraInfoEntry : device.getPortExtraInfoMap().entrySet()) {
                WritableNativeMap portExtraInfo = new WritableNativeMap();
                portExtraInfo.putInt("port", portExtraInfoEntry.getKey());
                portExtraInfo.putMap("info", jsonToMap(portExtraInfoEntry.getValue()));

                portsExtraInfoArray.pushMap(portExtraInfo);
            }
            networkDeviceInfo.putArray("portsInfo", portsExtraInfoArray);
        }

        if (device.getSNMPInfo() != null) {
            networkDeviceInfo.putMap("snmpInfo", jsonToMap(device.getSNMPInfo()));
        }

        return networkDeviceInfo;
    }

    private WritableMap jsonToMap(JSONObject jsonObject) {
        WritableNativeMap map = new WritableNativeMap();

        try {
            if (jsonObject != null) {
                Iterator<?> keys = jsonObject.keys();

                while (keys.hasNext()) {
                    String key = (String) keys.next();

                    if (jsonObject.get(key) instanceof JSONObject) {
                        map.putMap(key, jsonToMap(jsonObject.getJSONObject(key)));
                    } else if (jsonObject.get(key) instanceof JSONArray) {
                        map.putArray(key, jsonArrayToArray(jsonObject.getJSONArray(key)));
                    } else {
                        map.putString(key, jsonObject.get(key).toString());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }

    private WritableNativeArray jsonArrayToArray(JSONArray jsonArray) {
        WritableNativeArray outputArray = new WritableNativeArray();

        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                Object item = jsonArray.get(i);

                if (item instanceof JSONObject) {
                    outputArray.pushMap(jsonToMap((JSONObject) item));
                } else if (item instanceof JSONArray) {
                    outputArray.pushArray(jsonArrayToArray((JSONArray) item));
                } else {
                    outputArray.pushString(item.toString());
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return outputArray;
    }
}