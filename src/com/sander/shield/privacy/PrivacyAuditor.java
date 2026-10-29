package com.sander.shield.privacy;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;

public class PrivacyAuditor {
    public interface AuditCallback {
        void onResult(String json, int colorType);
    }

    public void auditPrivacy(Context ctx, AuditCallback cb) {
        try {
            StringBuilder json = new StringBuilder();
            json.append("[\n");

            // 1. Check MAC Address Randomization
            WifiManager wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wm.getConnectionInfo();
            String mac = wifiInfo.getMacAddress();
            
            json.append("  {\n");
            json.append("    \"module\": \"MacAddressAudit\",\n");
            if (mac == null || mac.equals("02:00:00:00:00:00")) {
                json.append("    \"status\": \"SECURE\",\n");
                json.append("    \"reason\": \"MAC Address is randomized (Android 10+)\"\n");
            } else {
                json.append("    \"status\": \"WARNING\",\n");
                json.append("    \"reason\": \"Original MAC Address exposed: ").append(mac).append("\"\n");
            }
            json.append("  },\n");

            // 2. Check Phone Number Exposure (Basic)
            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            String number = tm.getLine1Number();
            
            json.append("  {\n");
            json.append("    \"module\": \"PhoneNumberAudit\",\n");
            if (number == null || number.isEmpty()) {
                json.append("    \"status\": \"SECURE\",\n");
                json.append("    \"reason\": \"Phone number is hidden from apps\"\n");
            } else {
                json.append("    \"status\": \"WARNING\",\n");
                json.append("    \"reason\": \"Phone number exposed: ").append(number).append("\"\n");
            }
            json.append("  }\n");

            json.append("]\n");
            cb.onResult(json.toString(), 3); // 3 = Green/Cyan

        } catch (Exception e) {
            cb.onResult("{\"error\":\"" + e.getMessage() + "\"}\n", 1);
        }
    }
}
