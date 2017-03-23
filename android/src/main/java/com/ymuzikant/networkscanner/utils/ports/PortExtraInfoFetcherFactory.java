package com.ymuzikant.networkscanner.utils.ports;

/**
 * Created by Yaron Muzikant on 23-Mar-17.
 */

public class PortExtraInfoFetcherFactory {
    private static PortExtraInfoFetcher portHandlers[] = new PortExtraInfoFetcher[]{
            new HTTPExtraInfoFetcher(),
            new HTTPSExtraInfoFetcher()
    };

    public static PortExtraInfoFetcher getHandlerForPort(int port) {
        for (PortExtraInfoFetcher portHandler : portHandlers) {
            if (portHandler.getPort() == port) {
                return portHandler;
            }
        }

        return null;
    }
}
