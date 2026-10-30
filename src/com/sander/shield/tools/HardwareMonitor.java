package com.sander.shield.tools;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

public class HardwareMonitor {
    public interface MonitorCallback {
        void onUpdate(String json);
    }

    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneListener;
    private MonitorCallback callback;
    private int lastSignal = -113; // Default nilai minimum

    public void startMonitoring(Context ctx, MonitorCallback cb) {
        this.callback = cb;
        this.telephonyManager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        
        // Baca Baterai
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = ctx.registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int batteryPct = (int) ((level / (float) scale) * 100);

        // Baca Signal
        if (phoneListener == null) {
            phoneListener = new PhoneStateListener() {
                @Override
                public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                    super.onSignalStrengthsChanged(signalStrength);
                    int dbm = -113;
                    if (signalStrength.isGsm()) {
                        dbm = signalStrength.getGsmSignalStrength();
                        if (dbm != 99) dbm = -113 + 2 * dbm;
                    } else {
                        dbm = signalStrength.getCdmaDbm();
                    }
                    lastSignal = dbm;
                }
            };
            telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        }

        if (callback != null) {
            callback.onUpdate("{\"event\":\"hw_monitor\",\"battery\":" + batteryPct + ",\"signal_dbm\":" + lastSignal + ",\"cpu_abi\":\"" + android.os.Build.SUPPORTED_ABIS[0] + "\"}\n");
        }
    }

    public void stopMonitoring() {
        if (telephonyManager != null && phoneListener != null) {
            telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_NONE);
            phoneListener = null;
        }
    }
}
