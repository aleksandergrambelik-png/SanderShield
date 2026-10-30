package com.sander.shield;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.HorizontalScrollView;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.content.Context;
import android.content.ClipboardManager;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.Manifest;
import android.os.Build;

import com.sander.shield.tools.GpsTracker;
import com.sander.shield.tools.SensorReader;
import com.sander.shield.tools.WgetFetcher;
import com.sander.shield.tools.StealthDefenseEngine;
import com.sander.shield.tools.CameraTool;
import com.sander.shield.tools.HexViewer;
import com.sander.shield.tools.HardwareMonitor;
import com.sander.shield.vuln.VulnScanner;
import com.sander.shield.vuln.CveChecker;
import com.sander.shield.vuln.IpInfo;
import com.sander.shield.privacy.AntiTracker;
import com.sander.shield.privacy.PrivacyAuditor;
import com.sander.shield.privacy.GpsBlocker;
import com.sander.shield.wifi.WifiScanner;
import com.sander.shield.security.SafetyGuard;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity {
    
    LinearLayout controlPanel;
    TextView tvTerminal;
    ScrollView terminalScroll;
    EditText inputTerminal;
    Handler handler = new Handler(Looper.getMainLooper());
    
    StealthDefenseEngine defenseEngine = null;
    GpsTracker gpsTracker = new GpsTracker();
    SensorReader sensorReader = new SensorReader();
    WgetFetcher wgetFetcher = new WgetFetcher();
    VulnScanner vulnScanner = new VulnScanner();
    CveChecker cveChecker = new CveChecker();
    IpInfo ipInfo = new IpInfo();
    AntiTracker antiTracker = new AntiTracker();
    PrivacyAuditor privacyAuditor = new PrivacyAuditor();
    GpsBlocker gpsBlocker = new GpsBlocker();
    WifiScanner wifiScanner = new WifiScanner();
    SafetyGuard safetyGuard = new SafetyGuard();
    HexViewer hexViewer = new HexViewer();
    HardwareMonitor hardwareMonitor = new HardwareMonitor();
    
    boolean isPhisingBotActive = false;

    int colorGreen = Color.parseColor("#00FF00");
    int colorCyan = Color.parseColor("#00FFFF");
    int colorWhite = Color.parseColor("#FFFFFF");
    int colorRed = Color.parseColor("#FF0000");
    int colorYellow = Color.parseColor("#FFFF00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            }, 100);
        }

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.BLACK);
        
        buildUI(mainLayout);
        setContentView(mainLayout);
        
        String banner = 
            "  ____                 _   ____  _     _      _     _ \n" +
            " / ___|  __ _ _ __   __| | / ___|| |__ (_) ___| | __| |\n" +
            " \\___ \\ / _` | '_ \\ / _` | \\___ \\| '_ \\| |/ _ \\ |/ _` |\n" +
            "  ___) | (_| | | | | (_| |  ___) | | | | |  __/ | (_| |\n" +
            " |____/ \\__,_|_| |_|\\__,_| |____/|_| |_|_|\\___|_|\\__,_|\n" +
            "                        [ v38.0 ULTIMATE ]\n";
            
        appendLog(banner, colorGreen);
        appendLog("Source: github.com/aleksandergrambelik-png/SanderShield\n\n", colorCyan);
        showMenu("RECON"); 
    }

    private void buildUI(LinearLayout mainLayout) {
        // TOP NAV
        HorizontalScrollView topNavScroll = new HorizontalScrollView(this);
        topNavScroll.setBackgroundColor(Color.parseColor("#0A0A0A"));
        topNavScroll.setPadding(10, 10, 10, 10);
        LinearLayout topNav = new LinearLayout(this);
        topNav.setOrientation(LinearLayout.HORIZONTAL);
        
        topNav.addView(createTopMenuBtn("[ RECON ]", new View.OnClickListener() { @Override public void onClick(View v) { showMenu("RECON"); } }));
        topNav.addView(createTopMenuBtn("[ DEFENSE ]", new View.OnClickListener() { @Override public void onClick(View v) { showMenu("DEFENSE"); } }));
        topNav.addView(createTopMenuBtn("[ WEB-VULN ]", new View.OnClickListener() { @Override public void onClick(View v) { showMenu("WEB-VULN"); } }));
        topNav.addView(createTopMenuBtn("[ PRIVACY ]", new View.OnClickListener() { @Override public void onClick(View v) { showMenu("PRIVACY"); } }));
        topNav.addView(createTopMenuBtn("[ SENSORS ]", new View.OnClickListener() { @Override public void onClick(View v) { showMenu("SENSORS"); } }));
        topNav.addView(createTopMenuBtn("[ INFO ]", new View.OnClickListener() { @Override public void onClick(View v) { showMenu("INFO"); } }));

        topNavScroll.addView(topNav);
        mainLayout.addView(topNavScroll, new LinearLayout.LayoutParams(-1, -2));

        // CONTROL PANEL
        HorizontalScrollView ctrlScroll = new HorizontalScrollView(this);
        ctrlScroll.setBackgroundColor(Color.parseColor("#151515"));
        ctrlScroll.setPadding(10, 10, 10, 10);
        controlPanel = new LinearLayout(this);
        controlPanel.setOrientation(LinearLayout.HORIZONTAL);
        ctrlScroll.addView(controlPanel);
        mainLayout.addView(ctrlScroll, new LinearLayout.LayoutParams(-1, -2));

        // TERMINAL LOG
        terminalScroll = new ScrollView(this);
        terminalScroll.setBackgroundColor(Color.parseColor("#050505"));
        tvTerminal = new TextView(this);
        tvTerminal.setTypeface(Typeface.MONOSPACE); 
        tvTerminal.setTextColor(colorCyan); 
        tvTerminal.setTextSize(11);
        tvTerminal.setPadding(10, 10, 10, 10);
        terminalScroll.addView(tvTerminal);
        mainLayout.addView(terminalScroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));

        // INPUT TERMINAL
        inputTerminal = new EditText(this);
        inputTerminal.setHint("$ command... (help)");
        inputTerminal.setTypeface(Typeface.MONOSPACE); 
        inputTerminal.setTextColor(colorGreen); 
        inputTerminal.setHintTextColor(Color.GRAY); 
        inputTerminal.setTextSize(12);
        inputTerminal.setBackgroundColor(Color.parseColor("#111111")); 
        inputTerminal.setPadding(10, 10, 10, 10);
        inputTerminal.setSingleLine(true); 
        inputTerminal.setImeOptions(EditorInfo.IME_ACTION_DONE); 
        inputTerminal.setInputType(InputType.TYPE_CLASS_TEXT);
        
        inputTerminal.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {
                    String cmd = inputTerminal.getText().toString().trim();
                    if (!cmd.isEmpty()) { executeCommand(cmd); inputTerminal.setText(""); }
                    return true;
                }
                return false;
            }
        });
        mainLayout.addView(inputTerminal, new LinearLayout.LayoutParams(-1, -2));
    }

    private TextView createTopMenuBtn(String text, View.OnClickListener listener) {
        TextView tv = new TextView(this); 
        tv.setText(text); 
        tv.setTypeface(Typeface.MONOSPACE); 
        tv.setTextColor(colorWhite); 
        tv.setTextSize(12); 
        tv.setPadding(0, 0, 20, 0); 
        tv.setOnClickListener(listener); 
        return tv;
    }

    private TextView createCtrlBtn(String text, int textColor, View.OnClickListener listener) {
        TextView tv = new TextView(this); 
        tv.setText(text); 
        tv.setTypeface(Typeface.MONOSPACE); 
        tv.setTextColor(textColor); 
        tv.setTextSize(10); 
        tv.setPadding(12, 8, 12, 8); 
        tv.setBackgroundColor(Color.parseColor("#222222")); 
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.setMargins(0, 0, 10, 0); 
        tv.setLayoutParams(params);
        tv.setOnClickListener(listener); 
        return tv;
    }

    private void appendLog(final String text, final int color) {
        handler.post(new Runnable() {
            @Override public void run() {
                SpannableString spannable = new SpannableString(text);
                spannable.setSpan(new ForegroundColorSpan(color), 0, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                String current = tvTerminal.getText().toString();
                if (current.split("\n").length > 200) current = ""; 
                
                tvTerminal.setText(current + spannable + "\n"); 
                terminalScroll.post(new Runnable() { @Override public void run() { terminalScroll.fullScroll(View.FOCUS_DOWN); } });
            }
        });
    }

    private void showMenu(String menuName) {
        controlPanel.removeAllViews();
        appendLog("root@sander:~# " + menuName.toLowerCase() + " --load\n", colorCyan);

        if (menuName.equals("RECON")) {
            controlPanel.addView(createCtrlBtn("PING CF", colorWhite, new View.OnClickListener() { @Override public void onClick(View v) { runPing("1.1.1.1"); } }));
            controlPanel.addView(createCtrlBtn("SCAN GW", colorGreen, new View.OnClickListener() { @Override public void onClick(View v) { runSanderScan("192.168.1.1"); } }));
            controlPanel.addView(createCtrlBtn("IP INFO", colorCyan, new View.OnClickListener() { @Override public void onClick(View v) { appendLog("> Type: ipinfo 8.8.8.8\n", colorYellow); } }));
        } 
        else if (menuName.equals("DEFENSE")) {
            String phisingStatus = isPhisingBotActive ? "ON" : "OFF";
            int phisingColor = isPhisingBotActive ? colorGreen : colorRed;
            controlPanel.addView(createCtrlBtn("ANTI-PHISING: " + phisingStatus, phisingColor, new View.OnClickListener() { 
                @Override public void onClick(View v) { togglePhisingBot(); } 
            }));

            String stealthStatus = (defenseEngine != null) ? "ON" : "OFF";
            int stealthColor = (defenseEngine != null) ? colorGreen : colorRed;
            controlPanel.addView(createCtrlBtn("ANTI-SCAN: " + stealthStatus, stealthColor, new View.OnClickListener() { 
                @Override public void onClick(View v) { toggleStealth(); } 
            }));

            controlPanel.addView(createCtrlBtn("SAFETY GUARD", colorWhite, new View.OnClickListener() { 
                @Override public void onClick(View v) { 
                    appendLog("> Verifying APK Integrity & License...\n", colorYellow);
                    safetyGuard.verifyIntegrity(MainActivity.this, new SafetyGuard.GuardCallback() {
                        @Override public void onResult(String json, int colorType) {
                            appendLog(json, colorType == 1 ? colorRed : colorGreen);
                        }
                    });
                } 
            }));
        }
        else if (menuName.equals("WEB-VULN")) {
            controlPanel.addView(createCtrlBtn("VULN SCAN", colorRed, new View.OnClickListener() { @Override public void onClick(View v) { appendLog("> Type: vuln https://target.com\n", colorYellow); } }));
            controlPanel.addView(createCtrlBtn("CHECK CVE", colorYellow, new View.OnClickListener() { @Override public void onClick(View v) { appendLog("> Type: cve CVE-2021-44228\n", colorYellow); } }));
        }
        else if (menuName.equals("PRIVACY")) {
            controlPanel.addView(createCtrlBtn("SCAN TRACKER", colorRed, new View.OnClickListener() { 
                @Override public void onClick(View v) { 
                    appendLog("> Scanning for Trackers...\n", colorYellow);
                    antiTracker.scanTrackers(MainActivity.this, new AntiTracker.TrackerCallback() {
                        @Override public void onResult(String json, int colorType) {
                            appendLog(json, colorType == 1 ? colorRed : colorGreen);
                        }
                    });
                } 
            }));
            controlPanel.addView(createCtrlBtn("SCAN WIFI", colorCyan, new View.OnClickListener() { 
                @Override public void onClick(View v) { 
                    appendLog("> Scanning WiFi Around...\n", colorYellow);
                    wifiScanner.scanWifi(MainActivity.this, new WifiScanner.WifiCallback() {
                        @Override public void onResult(String json, int colorType) {
                            appendLog(json, colorType == 1 ? colorRed : (colorType == 2 ? colorYellow : colorGreen));
                        }
                    });
                } 
            }));
            controlPanel.addView(createCtrlBtn("ANTI-TRACKER", colorRed, new View.OnClickListener() { 
                @Override public void onClick(View v) { 
                    appendLog("> Auditing Privacy...\n", colorYellow);
                    privacyAuditor.auditPrivacy(MainActivity.this, new PrivacyAuditor.AuditCallback() {
                        @Override public void onResult(String json, int colorType) {
                            appendLog(json, colorType == 1 ? colorRed : colorGreen);
                        }
                    });
                } 
            }));
            controlPanel.addView(createCtrlBtn("GPS GUARD", colorCyan, new View.OnClickListener() { 
                @Override public void onClick(View v) { 
                    appendLog("> Checking GPS & Network Signal...\n", colorYellow);
                    gpsBlocker.checkGpsStatus(MainActivity.this, new GpsBlocker.GpsCallback() {
                        @Override public void onResult(String json, int colorType) {
                            appendLog(json, colorType == 1 ? colorRed : colorGreen);
                        }
                    });
                } 
            }));
        }
        else if (menuName.equals("SENSORS")) {
            String gpsStat = gpsTracker.isRunning() ? "ON" : "OFF";
            int gpsColor = gpsTracker.isRunning() ? colorGreen : colorRed;
            controlPanel.addView(createCtrlBtn("GPS: " + gpsStat, gpsColor, new View.OnClickListener() { @Override public void onClick(View v) { toggleGps(); } }));

            String sensStat = sensorReader.isRunning() ? "ON" : "OFF";
            int sensColor = sensorReader.isRunning() ? colorGreen : colorRed;
            controlPanel.addView(createCtrlBtn("SENSOR: " + sensStat, sensColor, new View.OnClickListener() { @Override public void onClick(View v) { toggleSensor(); } }));
        }
        else if (menuName.equals("INFO")) {
            controlPanel.addView(createCtrlBtn("PHONE INFO", colorWhite, new View.OnClickListener() { @Override public void onClick(View v) { getPhoneInfo(); } }));
            controlPanel.addView(createCtrlBtn("WIFI INFO", colorWhite, new View.OnClickListener() { @Override public void onClick(View v) { getWifiInfo(); } }));
            controlPanel.addView(createCtrlBtn("CAMERA", colorWhite, new View.OnClickListener() { @Override public void onClick(View v) { openCamera(); } }));
            controlPanel.addView(createCtrlBtn("HEX VIEWER", colorCyan, new View.OnClickListener() { 
                @Override public void onClick(View v) { 
                    appendLog("> Type: hex /sdcard/Download/yourfile.jpg\n", colorYellow);
                } 
            }));
            controlPanel.addView(createCtrlBtn("HW MONITOR", colorGreen, new View.OnClickListener() { 
                @Override public void onClick(View v) { 
                    appendLog("> Starting Hardware Monitor...\n", colorYellow);
                    hardwareMonitor.startMonitoring(MainActivity.this, new HardwareMonitor.MonitorCallback() {
                        @Override public void onUpdate(String json) { appendLog(json, colorGreen); }
                    });
                } 
            }));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != android.content.pm.PackageManager.PERMISSION_GRANTED) { allGranted = false; break; }
            }
            if (allGranted) appendLog("[+] Permissions Granted.\n", colorGreen);
            else appendLog("[!] Permissions Denied. Some features won't work.\n", colorRed);
        }
    }

    private void togglePhisingBot() {
        isPhisingBotActive = !isPhisingBotActive;
        if (isPhisingBotActive) {
            appendLog("[+] ANTI-PHISING BOT: ACTIVE\n", colorGreen);
            startPhisingBot();
        } else {
            appendLog("[!] ANTI-PHISING BOT: INACTIVE\n", colorYellow);
        }
        showMenu("DEFENSE");
    }

    private void startPhisingBot() {
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                if (isPhisingBotActive) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard.hasPrimaryClip()) {
                        String text = clipboard.getPrimaryClip().getItemAt(0).getText().toString().toLowerCase();
                        if (text.contains("login") || text.contains("hadiah") || text.contains(".xyz")) {
                            appendLog("[BOT] PHISING DETECTED: " + text + "\n", colorRed);
                        }
                    }
                    startPhisingBot();
                }
            }
        }, 5000);
    }

    private void toggleStealth() {
        if (defenseEngine == null) {
            appendLog("[+] ANTI-SCAN (STEALTH) ACTIVE\n", colorGreen);
            defenseEngine = new StealthDefenseEngine();
            defenseEngine.startDefense(new StealthDefenseEngine.DefenseCallback() {
                @Override public void onLog(final String message) { appendLog(message, colorCyan); }
                @Override public void onStats(final String statsJson) {}
            });
        } else {
            defenseEngine.stopDefense();
            defenseEngine = null;
            appendLog("[!] ANTI-SCAN INACTIVE\n", colorYellow);
        }
        showMenu("DEFENSE");
    }

    private void toggleGps() {
        if (gpsTracker.isRunning()) { gpsTracker.stopTracking(); appendLog("[!] GPS Stopped\n", colorYellow); }
        else { appendLog("> Requesting GPS...\n", colorYellow); gpsTracker.startTracking(this, new GpsTracker.GpsCallback() {
            @Override public void onLocation(String json) { appendLog(json, colorGreen); }
            @Override public void onError(String error) { appendLog("{\"error\":\"" + error + "\"}\n", colorRed); }
        }); }
        showMenu("SENSORS");
    }

    private void toggleSensor() {
        if (sensorReader.isRunning()) { sensorReader.stopReading(); appendLog("[!] Sensor Stopped\n", colorYellow); }
        else { appendLog("> Reading Sensor...\n", colorYellow); sensorReader.startReading(this, new SensorReader.SensorCallback() {
            @Override public void onSensorData(String json) { appendLog(json, colorGreen); }
        }); }
        showMenu("SENSORS");
    }

    private void getPhoneInfo() {
        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            String number = tm.getLine1Number();
            String operator = tm.getNetworkOperatorName();
            appendLog("{\"event\":\"phone_info\",\"number\":\"" + (number != null ? number : "Unknown") + "\",\"operator\":\"" + operator + "\"}\n", colorGreen);
        } catch (Exception e) { appendLog("{\"error\":\"Permission Denied\"}\n", colorRed); }
    }

    private void getWifiInfo() {
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            String ssid = wm.getConnectionInfo().getSSID();
            int ip = wm.getConnectionInfo().getIpAddress();
            String ipString = String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
            appendLog("{\"event\":\"wifi_info\",\"ssid\":\"" + ssid + "\",\"ip\":\"" + ipString + "\"}\n", colorGreen);
        } catch (Exception e) { appendLog("{\"error\":\"Failed to read WiFi\"}\n", colorRed); }
    }

    private void openCamera() {
        try { startActivityForResult(CameraTool.getCameraIntent(), 100); appendLog("> Opening Camera...\n", colorYellow); } 
        catch (Exception e) { appendLog("{\"error\":\"Camera not available\"}\n", colorRed); }
    }

    private void runPing(final String target) {
        appendLog("> Pinging " + target + "...\n", colorYellow);
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    long start = System.currentTimeMillis();
                    boolean reachable = InetAddress.getByName(target).isReachable(3000);
                    long time = System.currentTimeMillis() - start;
                    if (reachable) appendLog("{\"event\":\"ping_success\",\"target\":\"" + target + "\",\"time_ms\":" + time + "}\n", colorGreen);
                    else appendLog("{\"event\":\"ping_timeout\",\"target\":\"" + target + "\"}\n", colorRed);
                } catch (Exception e) { appendLog("{\"event\":\"ping_error\",\"msg\":\"" + e.getMessage() + "\"}\n", colorRed); }
            }
        }).start();
    }

    private void runSanderScan(final String target) {
        appendLog("> Scanning " + target + "...\n", colorYellow);
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    boolean isAlive = false;
                    for (int p : new int[]{80,443,22}) {
                        if (probe(target, p, 200)) { isAlive = true; break; }
                    }
                    if (!isAlive) { appendLog("{\"event\":\"host_dead\"}\n", colorRed); return; }
                    appendLog("{\"event\":\"host_alive\"}\n", colorGreen);
                    
                    ExecutorService pool = Executors.newFixedThreadPool(100);
                    final AtomicInteger totalOpen = new AtomicInteger(0);
                    int[] ports = {21,22,23,25,53,80,110,111,135,139,143,443,445,3306,3389,8080,5555};

                    for (final int port : ports) {
                        pool.submit(new Runnable() {
                            @Override public void run() {
                                if (probe(target, port, 300)) {
                                    totalOpen.incrementAndGet();
                                    appendLog("{\"port\":" + port + ",\"status\":\"open\"}\n", colorGreen);
                                }
                            }
                        });
                    }
                    pool.shutdown();
                    pool.awaitTermination(10, TimeUnit.SECONDS);
                    appendLog("{\"event\":\"scan_complete\",\"open\":" + totalOpen.get() + "}\n", colorCyan);
                } catch (Exception e) { appendLog("{\"error\":\"" + e.getMessage() + "\"}\n", colorRed); }
            }
        }).start();
    }

    private boolean probe(String host, int port, int timeout) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), timeout);
            return true;
        } catch (Exception e) { return false; }
    }

    private void executeCommand(String cmdLine) {
        String[] parts = cmdLine.split("\\s+");
        String cmd = parts[0].toLowerCase();
        appendLog("root@sander:~# " + cmdLine + "\n", colorCyan);

        if (cmd.equals("help")) {
            appendLog("Commands:\n- clear\n- ping <host>\n- scan <ip>\n- ipinfo <ip>\n- wget <url>\n- vuln <url>\n- cve <CVE-ID>\n- tracker\n- wifiscan\n- antitracker\n- gpsguard\n- safety\n- hex <path>\n- gps on/off\n- sensor on/off\n- phoneinfo\n- wifiinfo\n", colorWhite);
        } else if (cmd.equals("clear")) {
            tvTerminal.setText("root@sander:~# \n");
        } else if (cmd.equals("ping")) {
            runPing(parts.length > 1 ? parts[1] : "8.8.8.8");
        } else if (cmd.equals("scan")) {
            runSanderScan(parts.length > 1 ? parts[1] : "127.0.0.1");
        } else if (cmd.equals("ipinfo") && parts.length > 1) {
            appendLog("> Fetching IP info...\n", colorYellow);
            ipInfo.lookup(parts[1], new IpInfo.IpCallback() {
                @Override public void onResult(String result, int colorType) { 
                    appendLog(result, colorType == 1 ? colorRed : colorGreen); 
                }
            });
        } else if (cmd.equals("wget") && parts.length > 1) {
            appendLog("> Fetching " + parts[1] + "...\n", colorYellow);
            wgetFetcher.fetch(parts[1], new WgetFetcher.WgetCallback() {
                @Override public void onFetchResult(String result) { appendLog(result, result.contains("success") ? colorGreen : colorRed); }
            });
        } else if (cmd.equals("vuln") && parts.length > 1) {
            String targetUrl = parts[1];
            if (!targetUrl.startsWith("http")) targetUrl = "https://" + targetUrl;
            appendLog("> Fetching headers...\n> Analyzing evidence...\n", colorYellow);
            vulnScanner.scanHeaders(targetUrl, new VulnScanner.VulnCallback() {
                @Override public void onScanResult(String result, int colorType) { 
                    appendLog(result, colorType == 1 ? colorRed : (colorType == 2 ? colorYellow : colorGreen)); 
                }
            });
        } else if (cmd.equals("cve") && parts.length > 1) {
            String cveId = parts[1].toUpperCase();
            appendLog("> Checking NVD for " + cveId + "...\n", colorYellow);
            cveChecker.checkCve(cveId, new CveChecker.CveCallback() {
                @Override public void onResult(String result, int colorType) {
                    appendLog(result, colorType == 1 ? colorRed : (colorType == 2 ? colorYellow : colorGreen));
                }
            });
        } else if (cmd.equals("tracker")) {
            appendLog("> Scanning for Trackers...\n", colorYellow);
            antiTracker.scanTrackers(this, new AntiTracker.TrackerCallback() {
                @Override public void onResult(String json, int colorType) {
                    appendLog(json, colorType == 1 ? colorRed : colorGreen);
                }
            });
        } else if (cmd.equals("wifiscan")) {
            appendLog("> Scanning WiFi Around...\n", colorYellow);
            wifiScanner.scanWifi(this, new WifiScanner.WifiCallback() {
                @Override public void onResult(String json, int colorType) {
                    appendLog(json, colorType == 1 ? colorRed : (colorType == 2 ? colorYellow : colorGreen));
                }
            });
        } else if (cmd.equals("antitracker")) {
            appendLog("> Scanning for Privacy Leaks...\n", colorYellow);
            privacyAuditor.auditPrivacy(this, new PrivacyAuditor.AuditCallback() {
                @Override public void onResult(String json, int colorType) {
                    appendLog(json, colorType == 1 ? colorRed : colorGreen);
                }
            });
        } else if (cmd.equals("gpsguard")) {
            appendLog("> Checking GPS & Network Signal...\n", colorYellow);
            gpsBlocker.checkGpsStatus(this, new GpsBlocker.GpsCallback() {
                @Override public void onResult(String json, int colorType) {
                    appendLog(json, colorType == 1 ? colorRed : colorGreen);
                }
            });
        } else if (cmd.equals("safety")) {
            appendLog("> Verifying APK Integrity & License...\n", colorYellow);
            safetyGuard.verifyIntegrity(this, new SafetyGuard.GuardCallback() {
                @Override public void onResult(String json, int colorType) {
                    appendLog(json, colorType == 1 ? colorRed : colorGreen);
                }
            });
        } else if (cmd.equals("hex") && parts.length > 1) {
            String path = parts[1];
            appendLog("> Reading HEX from: " + path + "\n", colorYellow);
            hexViewer.readFile(path, new HexViewer.HexCallback() {
                @Override public void onResult(String hexOutput) { 
                    appendLog(hexOutput, colorGreen); 
                }
                @Override public void onError(String error) { 
                    appendLog("{\"error\":\"" + error + "\"}\n", colorRed); 
                }
            });
        } else if (cmd.equals("phoneinfo")) {
            getPhoneInfo();
        } else if (cmd.equals("wifiinfo")) {
            getWifiInfo();
        } else if (cmd.equals("gps") && parts.length > 1) {
            if(parts[1].equals("on") && !gpsTracker.isRunning()) toggleGps();
            else if(parts[1].equals("off") && gpsTracker.isRunning()) toggleGps();
        } else if (cmd.equals("sensor") && parts.length > 1) {
            if(parts[1].equals("on") && !sensorReader.isRunning()) toggleSensor();
            else if(parts[1].equals("off") && sensorReader.isRunning()) toggleSensor();
        } else {
            appendLog("sh: " + cmd + ": command not found\n", colorRed);
        }
    }
}
