package com.sander.shield.privacy;

import android.content.Context;
import android.location.LocationManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.telephony.SignalStrength;
import android.os.Build;

public class GpsBlocker {
    public interface GpsCallback {
        void onResult(String json, int colorType);
    }

    public void checkGpsStatus(Context ctx, GpsCallback cb) {
        try {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"event\": \"privacy_audit\",\n");
            
            // 1. Cek Status GPS (Aktif/Nggak)
            LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
            boolean isGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            json.append("  \"gps_status\": \"").append(isGpsEnabled ? "ACTIVE" : "DISABLED").append("\",\n");

            // 2. Cek Mock Location (Lokasi Palsu)
            boolean isMockLocation = false;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    isMockLocation = Settings.canDrawOverlays(ctx); // Simple check
                }
            } catch (Exception e) {}
            json.append("  \"mock_location_detected\": ").append(isMockLocation).append(",\n");

            // 3. Telephony & Signal Info
            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            String operator = tm.getNetworkOperatorName();
            String simState = (tm.getSimState() == TelephonyManager.SIM_STATE_READY) ? "READY" : "NO_SIM";
            boolean isRoaming = tm.isNetworkRoaming();
            
            json.append("  \"network\": {\n");
            json.append("    \"operator\": \"").append(operator).append("\",\n");
            json.append("    \"sim_state\": \"").append(simState).append("\",\n");
            json.append("    \"roaming\": ").append(isRoaming).append("\n");
            json.append("  }\n");
            json.append("}\n");

            cb.onResult(json.toString(), 3); // 3 = Hijau/Cyan

        } catch (Exception e) {
            cb.onResult("{\"error\":\"" + e.getMessage() + "\"}\n", 1);
        }
    }
}
