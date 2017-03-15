package com.ymuzikant.networkscanner;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.ymuzikant.networkscanner.utils.NetworkDevice;
import com.ymuzikant.networkscanner.utils.NetworkScanner;

import java.util.ArrayList;

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
                WritableNativeMap networkDeviceInfo = new WritableNativeMap();
                networkDeviceInfo.putString("ip", device.getIP());

                eventEmitter.emit("deviceFound", networkDeviceInfo);
            }

            @Override
            public void onScanCompleted(ArrayList<NetworkDevice> devices) {
                if (onScanFinished != null) {
                    WritableNativeArray devicesArray = new WritableNativeArray();

                    for (NetworkDevice device : devices) {
                        WritableNativeMap networkDeviceInfo = new WritableNativeMap();
                        networkDeviceInfo.putString("ip", device.getIP());

                        if (device.getMac() != null) {
                            networkDeviceInfo.putString("mac", device.getMac());
                        }

                        if (device.getHostname() != null) {
                            networkDeviceInfo.putString("hostname", device.getHostname());
                        }

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
}