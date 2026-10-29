package com.sander.shield.wifi;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import java.util.List;

public class WifiScanner {
    public interface WifiCallback {
        void onResult(String json, int colorType);
    }

    public void scanWifi(Context ctx, WifiCallback cb) {
        try {
            WifiManager wifiManager = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            
            if (!wifiManager.isWifiEnabled()) {
                cb.onResult("{\"event\":\"wifi_error\",\"msg\":\"WiFi HP lo mati bro. Nyalain dulu!\"}\n", 1);
                return;
            }

            // Minta scan (bakal ngasih hasil terakhir jika belum pernah scan)
            wifiManager.startScan();
            List<ScanResult> results = wifiManager.getScanResults();

            if (results == null || results.isEmpty()) {
                cb.onResult("{\"event\":\"wifi_empty\",\"msg\":\"Gagal dapet sinyal. Pastiin GPS nyala & izin lokasi aktif.\"}\n", 2);
                return;
            }

            StringBuilder json = new StringBuilder();
            json.append("[\n");
            boolean first = true;

            for (ScanResult result : results) {
                if (!first) json.append(",\n");
                
                json.append("  {\n");
                json.append("    \"ssid\": \"").append(result.SSID.replace("\"", "'")).append("\",\n");
                json.append("    \"bssid\": \"").append(result.BSSID).append("\",\n");
                json.append("    \"rssi\": ").append(result.level).append(",\n");
                json.append("    \"capabilities\": \"").append(result.capabilities).append("\"\n");
                json.append("  }");
                
                first = false;
            }

            json.append("\n]\n");
            cb.onResult(json.toString(), 3); // 3 = Green

        } catch (SecurityException e) {
            cb.onResult("{\"event\":\"error\",\"msg\":\"Izin Lokasi ditolak! Scan WiFi butuh izin Lokasi.\"}\n", 1);
        } catch (Exception e) {
            cb.onResult("{\"event\":\"error\",\"msg\":\"" + e.getMessage() + "\"}\n", 1);
        }
    }
}
