package com.ymuzikant.networkscanner.utils.ports;

import org.json.JSONObject;

/**
 * Created by Yaron Muzikant on 23-Mar-17.
 */

public abstract class PortExtraInfoFetcher {
    public abstract int getPort();

    public abstract JSONObject getPortExtraInfo(String ip);
}