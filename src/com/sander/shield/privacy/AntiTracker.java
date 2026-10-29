package com.sander.shield.privacy;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import java.util.List;

public class AntiTracker {
    public interface TrackerCallback {
        void onResult(String json, int colorType);
    }

    public void scanTrackers(Context ctx, TrackerCallback cb) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    PackageManager pm = ctx.getPackageManager();
                    List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
                    StringBuilder json = new StringBuilder();
                    json.append("[\n");
                    boolean hasFinding = false;

                    for (ApplicationInfo pkg : packages) {
                        try {
                            PackageInfo pInfo = pm.getPackageInfo(pkg.packageName, PackageManager.GET_PERMISSIONS);
                            if (pInfo.requestedPermissions != null) {
                                boolean hasBgLoc = false, hasAudio = false, hasSms = false;

                                for (String perm : pInfo.requestedPermissions) {
                                    if (perm.equals("android.permission.ACCESS_BACKGROUND_LOCATION")) hasBgLoc = true;
                                    if (perm.equals("android.permission.RECORD_AUDIO")) hasAudio = true;
                                    if (perm.equals("android.permission.READ_SMS")) hasSms = true;
                                }

                                // LOGIKA ANTI-TRACKER
                                // Kalau minta lokasi background + audio + sms sekaligus, ini bahaya!
                                if (hasBgLoc && hasAudio && hasSms) {
                                    if (hasFinding) json.append(",\n");
                                    json.append("  {\n");
                                    json.append("    \"severity\": \"CRITICAL\",\n");
                                    json.append("    \"type\": \"Tracker/Spyware\",\n");
                                    json.append("    \"package\": \"").append(pkg.packageName).append("\",\n");
                                    json.append("    \"reason\": \"Minta Background Location + Audio + SMS\"\n");
                                    json.append("  }");
                                    hasFinding = true;
                                }
                            }
                        } catch (Exception e) {}
                    }
                    json.append("\n]\n");

                    if (hasFinding) {
                        cb.onResult(json.toString(), 1); // 1 = Red
                    } else {
                        cb.onResult("{\"event\":\"scan_clean\",\"msg\":\"Tidak ada tracker mencurigakan.\"}\n", 3); // 3 = Green
                    }

                } catch (Exception e) {
                    cb.onResult("{\"event\":\"error\",\"msg\":\"" + e.getMessage() + "\"}\n", 1);
                }
            }
        }).start();
    }
}
