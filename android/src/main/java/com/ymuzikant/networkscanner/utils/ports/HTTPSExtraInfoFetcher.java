package com.ymuzikant.networkscanner.utils.ports;

/**
 * Created by Yaron Muzikant on 23-Mar-17.
 */

public class HTTPSExtraInfoFetcher extends HTTPExtraInfoFetcher {
    @Override
    public int getPort() {
        return 443;
    }

    @Override
    protected String getScheme() {
        return "https";
    }
}