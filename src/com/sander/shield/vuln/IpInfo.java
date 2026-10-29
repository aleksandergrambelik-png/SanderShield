package com.sander.shield.vuln;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class IpInfo {
    public interface IpCallback {
        void onResult(String result, int colorType);
    }

    public void lookup(final String ip, final IpCallback cb) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("http://ip-api.com/json/" + ip);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) content.append(inputLine);
                    in.close();

                    String json = content.toString();
                    
                    // Parsing manual (Zero Dependency)
                    String country = getValue(json, "country");
                    String city = getValue(json, "city");
                    String isp = getValue(json, "isp");

                    if (json.contains("\"status\":\"fail\"")) {
                        cb.onResult("[!] ERROR: IP tidak valid atau private.", 1);
                    } else {
                        cb.onResult("{\"event\":\"ip_info\",\"ip\":\"" + ip + "\",\"country\":\"" + country + "\",\"city\":\"" + city + "\",\"isp\":\"" + isp + "\"}\n", 3);
                    }
                } catch (Exception e) {
                    cb.onResult("[!] ERROR: " + e.getMessage(), 1);
                }
            }
        }).start();
    }

    private String getValue(String json, String key) {
        int idx = json.indexOf("\"" + key + "\":\"");
        if (idx == -1) return "Unknown";
        int start = idx + key.length() + 4;
        int end = json.indexOf("\"", start);
        return (end != -1) ? json.substring(start, end) : "Unknown";
    }
}
