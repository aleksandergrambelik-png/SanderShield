package com.sander.shield.vuln;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CveChecker {
    public interface CveCallback {
        void onResult(String result, int colorType);
    }

    public void checkCve(final String cveId, final CveCallback cb) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://services.nvd.nist.gov/rest/json/cves/2.0?cveId=" + cveId);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setRequestProperty("User-Agent", "SanderShield/1.0");

                    if (conn.getResponseCode() != 200) {
                        cb.onResult("[!] ERROR: NVD API menolak request.", 1);
                        return;
                    }

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) content.append(inputLine);
                    in.close();

                    String json = content.toString();

                    if (json.contains("\"totalResults\":0")) {
                        cb.onResult("[!] CVE " + cveId + " TIDAK DITEMUKAN.", 2);
                        return;
                    }

                    String desc = "No description.";
                    int descIndex = json.indexOf("\"value\":\"");
                    if (descIndex != -1) {
                        int end = json.indexOf("\"", descIndex + 9);
                        if (end != -1) desc = json.substring(descIndex + 9, end);
                    }

                    String severity = "UNKNOWN";
                    int sevIndex = json.indexOf("\"baseSeverity\":\"");
                    if (sevIndex != -1) {
                        int end = json.indexOf("\"", sevIndex + 16);
                        if (end != -1) severity = json.substring(sevIndex + 16, end);
                    }

                    String score = "0.0";
                    int scoreIndex = json.indexOf("\"baseScore\":");
                    if (scoreIndex != -1) {
                        int end = json.indexOf(",", scoreIndex + 12);
                        if (end != -1) score = json.substring(scoreIndex + 12, end);
                    }

                    String out = "\n[+] CVE FOUND: " + cveId + "\n    [+] Severity: " + severity + " (CVSS: " + score + ")\n    [+] Desc: " + desc + "\n";
                    int color = severity.equals("CRITICAL") ? 1 : (severity.equals("HIGH") ? 2 : 3);
                    cb.onResult(out, color);

                } catch (Exception e) {
                    cb.onResult("[!] ERROR: " + e.getMessage(), 1);
                }
            }
        }).start();
    }
}
