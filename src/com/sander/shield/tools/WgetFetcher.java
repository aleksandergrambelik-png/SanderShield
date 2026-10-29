package com.sander.shield.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WgetFetcher {
    public interface WgetCallback {
        void onFetchResult(String result);
    }

    public void fetch(final String urlString, final WgetCallback cb) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setInstanceFollowRedirects(true); // Auto Redirect
                    
                    int responseCode = conn.getResponseCode();
                    String finalUrl = conn.getURL().toString();
                    String serverHeader = conn.getHeaderField("Server");
                    if (serverHeader == null) serverHeader = "Unknown";

                    if (responseCode >= 200 && responseCode < 300) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String inputLine;
                        StringBuilder content = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) content.append(inputLine).append("\n");
                        in.close();
                        
                        String result = content.toString();
                        if (result.length() > 300) result = result.substring(0, 300) + "...[TRUNCATED]";
                        
                        cb.onFetchResult("{\"event\":\"wget_success\",\"code\":" + responseCode + ",\"server\":\"" + serverHeader + "\",\"final_url\":\"" + finalUrl + "\",\"data\":\"" + result.replace("\"", "'").replace("\n", "\\n") + "\"}\n");
                    } else {
                        cb.onFetchResult("{\"event\":\"wget_error\",\"code\":" + responseCode + ",\"msg\":\"HTTP Error\"}\n");
                    }
                } catch (Exception e) {
                    cb.onFetchResult("{\"event\":\"wget_error\",\"msg\":\"" + e.getMessage() + "\"}\n");
                }
            }
        }).start();
    }
}
