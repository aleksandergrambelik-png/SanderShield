package com.sander.shield.vuln;

import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class VulnScanner {
    public interface VulnCallback {
        void onScanResult(String result, int colorType); // 1=Red, 2=Yellow, 3=Green
    }

    public void scanHeaders(final String urlString, final VulnCallback cb) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setInstanceFollowRedirects(true);

                    Map<String, List<String>> headers = conn.getHeaderFields();
                    StringBuilder output = new StringBuilder();
                    int findings = 0;
                    
                    // Timestamp ISO 8601
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    String timestamp = sdf.format(new Date());

                    // 1. Server
                    String server = getHeader(headers, "Server");
                    if (server != null && server.contains("/")) {
                        String cve = "CVE-Unknown";
                        if (server.contains("Apache/2.4.49")) cve = "CVE-2021-41773";
                        else if (server.contains("Apache/2.4.50")) cve = "CVE-2021-42013";
                        else if (server.contains("nginx/1.18.0")) cve = "CVE-2021-23017";
                        
                        output.append(formatJson(timestamp, "Info Disclosure", "CRITICAL", "Server", server, cve, "Version number exposed allows specific exploit targeting."));
                        findings++;
                    }

                    // 2. X-Powered-By
                    String poweredBy = getHeader(headers, "X-Powered-By");
                    if (poweredBy != null) {
                        output.append(formatJson(timestamp, "Tech Stack Leak", "HIGH", "X-Powered-By", poweredBy, "N/A", "Backend technology exposed."));
                        findings++;
                    }

                    // 3. HSTS
                    if (getHeader(headers, "Strict-Transport-Security") == null) {
                        output.append(formatJson(timestamp, "Missing Security Header", "WARNING", "Strict-Transport-Security", "NULL", "N/A", "HSTS missing enables SSL Stripping attacks."));
                        findings++;
                    }

                    // 4. X-Frame-Options
                    if (getHeader(headers, "X-Frame-Options") == null) {
                        output.append(formatJson(timestamp, "Clickjacking Risk", "HIGH", "X-Frame-Options", "NULL", "N/A", "Missing header allows iframe hijacking."));
                        findings++;
                    }

                    // 5. CSP
                    if (getHeader(headers, "Content-Security-Policy") == null) {
                        output.append(formatJson(timestamp, "XSS Risk", "HIGH", "Content-Security-Policy", "NULL", "N/A", "Missing CSP allows script injection."));
                        findings++;
                    }

                    // 6. CORS
                    String cors = getHeader(headers, "Access-Control-Allow-Origin");
                    if (cors != null && cors.equals("*")) {
                        output.append(formatJson(timestamp, "CORS Misconfiguration", "HIGH", "Access-Control-Allow-Origin", "*", "N/A", "Wildcard CORS allows data theft."));
                        findings++;
                    }

                    // 7. Set-Cookie
                    String cookie = getHeader(headers, "Set-Cookie");
                    if (cookie != null && (!cookie.toLowerCase().contains("httponly") || !cookie.toLowerCase().contains("secure"))) {
                        output.append(formatJson(timestamp, "Session Hijacking", "MEDIUM", "Set-Cookie", cookie, "N/A", "Cookie missing Secure/HttpOnly flags."));
                        findings++;
                    }

                    // 8. Debug Token
                    String debug = getHeader(headers, "X-Debug-Token");
                    if (debug != null) {
                        output.append(formatJson(timestamp, "Dev Mode Leak", "CRITICAL", "X-Debug-Token", debug, "N/A", "Debug token exposed."));
                        findings++;
                    }

                    // 9. Allow Methods
                    String allow = getHeader(headers, "Allow");
                    if (allow != null && (allow.contains("PUT") || allow.contains("TRACE"))) {
                        output.append(formatJson(timestamp, "Method Enumeration", "HIGH", "Allow", allow, "N/A", "Dangerous HTTP methods allowed."));
                        findings++;
                    }

                    // 10. ASP.NET
                    String aspNet = getHeader(headers, "X-AspNet-Version");
                    if (aspNet != null) {
                        output.append(formatJson(timestamp, "Info Disclosure", "LOW", "X-AspNet-Version", aspNet, "N/A", ".NET version exposed."));
                        findings++;
                    }

                    // 11. CMS
                    String gen = getHeader(headers, "X-Generator");
                    if (gen != null) {
                        output.append(formatJson(timestamp, "CMS Detection", "INFO", "X-Generator", gen, "N/A", "CMS version exposed."));
                        findings++;
                    }

                    // 12. Referrer
                    if (getHeader(headers, "Referrer-Policy") == null) {
                        output.append(formatJson(timestamp, "Info Leak", "LOW", "Referrer-Policy", "NULL", "N/A", "Missing Referrer-Policy."));
                        findings++;
                    }

                    if (findings == 0) {
                        cb.onScanResult("{\"event\":\"scan_clean\",\"msg\":\"No obvious header vulnerabilities found.\"}\n", 3);
                    } else {
                        cb.onScanResult("[\n" + output.toString() + "\n]\n", 1);
                    }

                } catch (Exception e) {
                    cb.onScanResult("{\"event\":\"vuln_error\",\"msg\":\"" + e.getMessage() + "\"}\n", 1);
                }
            }
        }).start();
    }

    private String formatJson(String timestamp, String type, String severity, String headerName, String rawValue, String cveRef, String reason) {
        StringBuilder sb = new StringBuilder();
        sb.append("  {\n");
        sb.append("    \"event\": \"vuln_confirmed\",\n");
        sb.append("    \"type\": \"").append(type).append("\",\n");
        sb.append("    \"severity\": \"").append(severity).append("\",\n");
        sb.append("    \"evidence\": {\n");
        sb.append("      \"header_name\": \"").append(headerName).append("\",\n");
        sb.append("      \"raw_value\": \"").append(rawValue.replace("\"", "'")).append("\",\n");
        sb.append("      \"cve_reference\": \"").append(cveRef).append("\",\n");
        sb.append("      \"reason\": \"").append(reason).append("\"\n");
        sb.append("    },\n");
        sb.append("    \"timestamp\": \"").append(timestamp).append("\"\n");
        sb.append("  },\n");
        return sb.toString();
    }

    private String getHeader(Map<String, List<String>> headers, String name) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue().get(0);
            }
        }
        return null;
    }
}
