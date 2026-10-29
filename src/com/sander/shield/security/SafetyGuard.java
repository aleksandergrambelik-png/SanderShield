package com.sander.shield.security;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import java.security.MessageDigest;
import java.io.File;

public class SafetyGuard {
    
    // 1. LISENSI ASLI LO
public static final String LICENSE_OWNER = "Copyright (C) 2026 aleksandergrambelik-png (Aleksander Titan Pabiyuri)";
    public static final String LICENSE_TYPE = "GNU Affero General Public License v3.0 (AGPLv3)";
    public static final String LICENSE_INFO = "This software is free and open-source. You MUST keep it open-source and credit the author. Closed-source commercial use is strictly prohibited.";
    
    public interface GuardCallback {
        void onResult(String json, int colorType);
    }

    public void verifyIntegrity(Context ctx, GuardCallback cb) {
        try {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            
            // Tampilkan Lisensi
            json.append("  \"license_owner\": \"").append(LICENSE_OWNER).append("\",\n");
            json.append("  \"license_type\": \"").append(LICENSE_TYPE).append("\",\n");
            json.append("  \"license_info\": \"").append(LICENSE_INFO).append("\",\n");

            // A. Cek Signature APK (Anti Repack)
            boolean isSignatureValid = false;
            try {
                PackageInfo pInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), PackageManager.GET_SIGNATURES);
                Signature[] signatures = pInfo.signatures;
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(signatures[0].toByteArray());
                byte[] digest = md.digest();
                StringBuilder hexString = new StringBuilder();
                for (byte b : digest) hexString.append(String.format("%02x", b));
                
                String currentHash = hexString.toString();
                
                // INI HASH SIGNATURE KEYSTORE LO (BUKAN APK)
                // Ganti string di bawah ini dengan hash keystore lo yang asli
String expectedHash = "98f229735d9c0ecf40262c826400237678020cb8f4e7350833ddb61e43de2aa1";                 
                
isSignatureValid = currentHash.equals(expectedHash);
                
                json.append("  \"apk_signature\": \"").append(isSignatureValid ? "VALID" : "TAMPERED").append("\",\n");
                json.append("  \"sha256_signature\": \"").append(currentHash.substring(0, 32)).append("...\",\n");
            } catch (Exception e) {
                json.append("  \"apk_signature\": \"UNKNOWN\",\n");
            }

            // B. Cek Root
            boolean isRooted = false;
            String[] rootPaths = {"/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su"};
            for (String path : rootPaths) {
                if (new File(path).exists()) { isRooted = true; break; }
            }
            json.append("  \"device_rooted\": ").append(isRooted).append(",\n");

            json.append("  \"status\": \"").append(isSignatureValid ? "SECURE" : "COMPROMISED").append("\"\n");
            json.append("}\n");

            cb.onResult(json.toString(), isSignatureValid ? 3 : 1);

        } catch (Exception e) {
            cb.onResult("{\"error\":\"" + e.getMessage() + "\"}\n", 1);
        }
    }
}
