package com.sander.shield.tools;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

public class StealthDefenseEngine {

    private int[] PORTS = {2121, 2222, 3306, 8080, 33890};
    private int THRESHOLD = 2;
    private long WINDOW_MS = 5000;
    private int DELAY_MS = 1500;
    private int MAX_DURATION_MS = 120000;
    private int MAX_TARPIT_THREADS = 50;
    
    private final long BOT_TIME_THRESHOLD_MS = 100;
    private final String[] BAD_USER_AGENTS = {
        "python-requests", "curl", "wget", "masscan", "zgrab", 
        "nmap", "sqlmap", "nikto", "go-http-client", "libwww-perl",
        "java/", "okhttp", "axios", "postman"
    };

    private final Pattern SQLI_PATTERN = Pattern.compile("(?i)(union.*select|select.*from|insert.*into|delete.*from|drop.*table|update.*set|'.*or.*'.*=.*')");
    private final Pattern XSS_PATTERN = Pattern.compile("(?i)(<script.*?>|javascript:|onerror=|onload=|<img.*?src=)");
    private final Pattern LFI_PATTERN = Pattern.compile("(?i)(\\.\\./|\\.\\.\\\\|/etc/passwd|/etc/shadow|c:\\\\windows)");

    private final AtomicLong cConn = new AtomicLong(0);
    private final AtomicLong cTarpit = new AtomicLong(0);
    private final AtomicLong cThreat = new AtomicLong(0);
    private final AtomicLong cBot = new AtomicLong(0);
    private final AtomicLong cPayload = new AtomicLong(0);
    
    private final Map<String, Long> ipFirstHit = new ConcurrentHashMap<>();
    private final Map<String, Set<Integer>> ipPorts = new ConcurrentHashMap<>();
    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();
    
    private final String[] MYSQL_DISTROS = {"ubuntu0.16.04.1", "ubuntu0.20.04.1", "ubuntu0.22.04.1", "debian0.10.1", "kali0.6.1"};
    private final String[] EBPF_VERSIONS = {"6.1.0", "6.2.0", "6.3.0", "6.4.0", "6.5.0"};
    private final Random rand = new Random();

    private boolean isRunning = false;
    private ServerSocket[] serverSockets;
    private DefenseCallback callback;

    public interface DefenseCallback {
        void onLog(String message);
        void onStats(String statsJson);
    }

    public void startDefense(DefenseCallback cb) {
        this.callback = cb;
        this.isRunning = true;
        this.serverSockets = new ServerSocket[PORTS.length];

        cb.onLog("{\"event\":\"engine_start\",\"msg\":\"StealthDefense v2.6 Active\"}\n");

        // Thread buat Stats
        new Thread(new Runnable() {
            @Override public void run() {
                while (isRunning) {
                    try {
                        Thread.sleep(3000);
                        String stats = "{\"event\":\"stats\",\"conn\":" + cConn.get() + ",\"tarpit\":" + cTarpit.get() + 
                                       ",\"threat\":" + cThreat.get() + ",\"bot\":" + cBot.get() + ",\"payload\":" + cPayload.get() + "}\n";
                        callback.onStats(stats);
                    } catch (Exception e) {}
                }
            }
        }).start();

        // Buka Port
        for (int i = 0; i < PORTS.length; i++) {
            final int port = PORTS[i];
            new Thread(new Runnable() {
                @Override public void run() { listen(port); }
            }).start();
        }
    }

    public void stopDefense() {
        isRunning = false;
        if (serverSockets != null) {
            for (ServerSocket ss : serverSockets) {
                try { if (ss != null) ss.close(); } catch (Exception e) {}
            }
        }
        if (callback != null) callback.onLog("{\"event\":\"engine_stop\"}\n");
    }

    private void listen(int port) {
        try {
            ServerSocket ss = new ServerSocket(port);
            serverSockets[Arrays.binarySearch(PORTS, port)] = ss;
            if (callback != null) callback.onLog("{\"event\":\"listen\",\"port\":" + port + "}\n");
            
            while (isRunning) {
                try {
                    Socket s = ss.accept();
                    cConn.incrementAndGet();
                    new Thread(new Runnable() {
                        @Override public void run() { process(s, port); }
                    }).start();
                } catch (Exception e) {
                    if (!isRunning) break;
                }
            }
        } catch (Exception e) {
            if (callback != null) callback.onLog("{\"error\":\"Cannot bind port " + port + "\"}\n");
        }
    }

    private void process(Socket s, int port) {
        String ip = s.getInetAddress().getHostAddress();
        
        if (blacklist.contains(ip)) {
            tarpit(s, ip);
            return;
        }

        boolean isThreat = false;
        long now = System.currentTimeMillis();
        String requestData = readRequest(s, port);
        
        if (requestData != null && isMaliciousPayload(requestData)) {
            cPayload.incrementAndGet();
            if (callback != null) callback.onLog("{\"event\":\"payload_detected\",\"ip\":\"" + ip + "\"}\n");
            blacklist.add(ip);
            cThreat.incrementAndGet();
            tarpit(s, ip);
            return;
        }

        if (detectBot(s, port, requestData)) {
            isThreat = true;
            cBot.incrementAndGet();
            if (callback != null) callback.onLog("{\"event\":\"bot_detected\",\"ip\":\"" + ip + "\"}\n");
            blacklist.add(ip);
            cThreat.incrementAndGet();
            tarpit(s, ip);
            return;
        }

        ipPorts.putIfAbsent(ip, ConcurrentHashMap.newKeySet());
        ipFirstHit.putIfAbsent(ip, now);
        
        if (now - ipFirstHit.get(ip) > WINDOW_MS) {
            ipPorts.get(ip).clear();
            ipFirstHit.put(ip, now);
        }
        
        ipPorts.get(ip).add(port);
        
        if (ipPorts.get(ip).size() >= THRESHOLD) {
            isThreat = true;
            blacklist.add(ip);
            cThreat.incrementAndGet();
            if (callback != null) callback.onLog("{\"event\":\"threat_detected\",\"ip\":\"" + ip + "\"}\n");
        }

        if (isThreat) {
            tarpit(s, ip);
        } else {
            try {
                s.getOutputStream().write(getBanner(port).getBytes());
                s.getOutputStream().flush();
                s.close();
            } catch (Exception e) {}
        }
    }

    private String readRequest(Socket s, int port) {
        try {
            s.setSoTimeout(2000);
            InputStream in = s.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder request = new StringBuilder();
            String line;
            boolean firstLine = true;
            long ttfbStart = 0;

            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (firstLine) { ttfbStart = System.currentTimeMillis(); firstLine = false; }
                request.append(line).append("\n");
            }
            long ttfb = System.currentTimeMillis() - ttfbStart;
            return request.toString() + "|TTFB:" + ttfb;
        } catch (Exception e) { return null; }
    }

    private boolean isMaliciousPayload(String input) {
        if (input == null || input.isEmpty()) return false;
        String decoded = input.toLowerCase();
        return SQLI_PATTERN.matcher(decoded).find() || XSS_PATTERN.matcher(decoded).find() || LFI_PATTERN.matcher(decoded).find();
    }

    private boolean detectBot(Socket s, int port, String requestData) {
        try {
            if (port == 8080) {
                if (requestData == null) return false;
                boolean isBadUA = false;
                for (String line : requestData.split("\n")) {
                    if (line.toLowerCase().startsWith("user-agent:")) {
                        String ua = line.substring(11).trim().toLowerCase();
                        for (String bad : BAD_USER_AGENTS) {
                            if (ua.contains(bad)) { isBadUA = true; break; }
                        }
                    }
                }
                long ttfb = 9999;
                if (requestData.contains("|TTFB:")) {
                    try { ttfb = Long.parseLong(requestData.substring(requestData.indexOf("|TTFB:") + 6)); } catch (Exception e) {}
                }
                if (isBadUA || ttfb < BOT_TIME_THRESHOLD_MS) return true;
            } else {
                s.setSoTimeout(500);
                if (s.getInputStream().available() > 0) return true;
            }
        } catch (Exception e) { return false; }
        return false;
    }

private void tarpit(Socket s, String ip) {
    if (cTarpit.get() >= MAX_TARPIT_THREADS) {
        try { s.close(); } catch (Exception e) {}
        return;
    }
    cTarpit.incrementAndGet();
    
    try {
        String userAgent = "Unknown";
        try {
            s.setSoTimeout(300);
            BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.toLowerCase().startsWith("user-agent:")) {
                    userAgent = line.substring(11).trim();
                    break;
                }
            }
        } catch (Exception e) {}

        if (userAgent.length() > 25) userAgent = userAgent.substring(0, 25) + "...";
        String fingerprintJson = "{\"event\":\"TARPIT\",\"ip\":\"" + ip + "\",\"ua\":\"" + userAgent.replace("\"", "") + "\"}\n";
        if (callback != null) callback.onLog(fingerprintJson);

        OutputStream out = s.getOutputStream();
        out.write(getBanner(s.getLocalPort()).getBytes());
        out.flush();
        
        long start = System.currentTimeMillis();
        byte[] buf = new byte[1];
        InputStream in = s.getInputStream();
        
        while (isRunning) {
            if (System.currentTimeMillis() - start > MAX_DURATION_MS) break;
            if (in.read(buf) == -1) break;
            out.write('.');
            out.flush();
            Thread.sleep(DELAY_MS);
        }
    } catch (Exception e) {
    } finally {
        try { s.close(); } catch (Exception e) {}
        cTarpit.decrementAndGet();
    }
}
    private String getBanner(int p) {
        if (p == 2121) return "220 (vsFTPd 3.0.3)\r\n";
        if (p == 2222) {
            String distro = MYSQL_DISTROS[rand.nextInt(MYSQL_DISTROS.length)].split("0")[0];
            return "SSH-2.0-OpenSSH_7.2p2 " + distro + "\r\n";
        }
        if (p == 3306) {
            return "5.7.32-" + MYSQL_DISTROS[rand.nextInt(MYSQL_DISTROS.length)] + "-ebpf-" + EBPF_VERSIONS[rand.nextInt(EBPF_VERSIONS.length)] + "\u0000";
        }
        if (p == 8080) {
            return "HTTP/1.1 200 OK\r\nServer: Microsoft-IIS/10.0\r\nX-Powered-By: eBPF/" + EBPF_VERSIONS[rand.nextInt(EBPF_VERSIONS.length)] + "\r\n<html><body><h1>401 Unauthorized</h1></body></html>";
        }
        return "220 Service Ready\r\n";
    }
}
