package com.ymuzikant.networkscanner.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by Yaron Muzikant on 15-Mar-17.
 */

public class PortScanner {
    public ArrayList<Integer> scanPorts(String ip, int... ports) {
        ArrayList<Integer> openPorts = new ArrayList<>();

        if (ports != null && ip != null) {
            for (int port : ports) {
                try {
                    // Try to create the Socket on the given port.
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(ip, port), 1000);

                    // If we arrive here, the port is open!
                    openPorts.add(port);

                    // Don't forget to close it
                    socket.close();
                } catch (IOException e) {
                    // Failed to open the port.
                }
            }
        }

        return openPorts;
    }
}
