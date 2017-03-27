package com.ymuzikant.networkscanner.utils;

import com.ymuzikant.networkscanner.utils.ports.PortExtraInfoFetcher;
import com.ymuzikant.networkscanner.utils.ports.PortExtraInfoFetcherFactory;

import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Yaron Muzikant on 15-Mar-17.
 */

public class PortScanner {
    private static final String TAG = "PortScanner";

    public ArrayList<Integer> scanPorts(final String ip, int... ports) {
        final ArrayList<Integer> openPorts = new ArrayList<>();

        if (ports != null && ip != null) {
            ThreadPoolExecutor portScanThreads = new ThreadPoolExecutor(ports.length, ports.length, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(ports.length));

            for (final int port : ports) {
                portScanThreads.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Try to create the Socket on the given port.
                            Socket socket = new Socket();
                            socket.connect(new InetSocketAddress(ip, port), 1000);

                            synchronized (openPorts) {
                                // If we arrive here, the port is open!
                                openPorts.add(port);
                            }

                            // Don't forget to close it
                            socket.close();
                        } catch (IOException e) {
                            // Failed to open the port.
                        }
                    }
                });
            }

            // Wait for all ports scan
            portScanThreads.shutdown();

            try {
                portScanThreads.awaitTermination(ports.length * 1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return openPorts;
    }

    public JSONObject getPortExtraInfo(String ip, int port) {
        PortExtraInfoFetcher portHandler = PortExtraInfoFetcherFactory.getHandlerForPort(port);

        if (portHandler != null) {
            return portHandler.getPortExtraInfo(ip);
        }
        return null;
    }
}
