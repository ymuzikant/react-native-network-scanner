package com.ymuzikant.networkscanner.utils.ports;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Created by Yaron Muzikant on 23-Mar-17.
 */

public class HTTPExtraInfoFetcher extends PortExtraInfoFetcher {
    private static final String TAG = "HTTPExtraInfoFetcher";

    @Override
    public int getPort() {
        return 80;
    }

    @Override
    public JSONObject getPortExtraInfo(String ip) {
        Log.d(TAG, "getPortExtraInfo() called with: ip = [" + ip + "], port = [" + getPort() + "]");

        try {
            HttpURLConnection conn = getHttpURLConnectionWithTimeout(ip);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);

            // Get response status
            int status = conn.getResponseCode();

            // TODO: Handle redirect (301/302)

            // Get response body
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            StringBuilder responseBody = new StringBuilder();
            while ((line = in.readLine()) != null) {
                responseBody.append(line);
            }
            in.close();

            // Get response headers
            Map<String, List<String>> map = conn.getHeaderFields();
            JSONArray headers = new JSONArray();
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                JSONObject currHeader = new JSONObject();
                currHeader.put("name", entry.getKey());
                currHeader.put("value", entry.getValue());
                headers.put(currHeader);
            }

            JSONObject portExtraInfo = new JSONObject();
            portExtraInfo.put("status", status);
            portExtraInfo.put("responseBody", responseBody.toString());
            portExtraInfo.put("headers", headers);

            return portExtraInfo;
        } catch (Exception e) {
            JSONObject portExtraInfo = new JSONObject();
            try {
                portExtraInfo.put("error", e.getMessage());
                return portExtraInfo;
            } catch (JSONException e1) {
                e1.printStackTrace();
            }

        }

        return null;
    }

    private HttpURLConnection getHttpURLConnectionWithTimeout(String ip) throws IOException {
        URL url = new URL(getScheme() + "://" + ip);
        HttpURLConnection conn = getHttpURLConnection(url);

        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);

        return conn;
    }

    protected HttpURLConnection getHttpURLConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }

    protected String getScheme() {
        return "http";
    }
}