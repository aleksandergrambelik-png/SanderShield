package com.sander.shield.tools;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;

public class SensorReader {
    private SensorManager sensorManager;
    private SensorCallback callback;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRunning = false;
    private float lastX = 0, lastY = 0, lastZ = 0;
    
    // Simpan listener di variabel biar bisa di-unregister
    private SensorEventListener listener; 

    public interface SensorCallback {
        void onSensorData(String json);
    }

    public void startReading(Context context, SensorCallback cb) {
        this.callback = cb;
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (accel == null) {
            cb.onSensorData("{\"event\":\"error\",\"msg\":\"No Accelerometer\"}\n");
            return;
        }

        isRunning = true;

        listener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                lastX = event.values[0];
                lastY = event.values[1];
                lastZ = event.values[2];
            }
            @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };

        sensorManager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_NORMAL);
        pollSensor();
    }

    private void pollSensor() {
        if (!isRunning) return;

        String json = "{\"event\":\"sensor_poll\",\"x\":" + String.format("%.2f", lastX) + 
                      ",\"y\":" + String.format("%.2f", lastY) + 
                      ",\"z\":" + String.format("%.2f", lastZ) + "}\n";
        callback.onSensorData(json);

        handler.postDelayed(new Runnable() {
            @Override public void run() { pollSensor(); }
        }, 5000); 
    }

    public void stopReading() {
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
        if (sensorManager != null && listener != null) {
            // Ini yang bener: Unregister pakai variabel listener
            sensorManager.unregisterListener(listener); 
        }
    }
    
    public boolean isRunning() { return isRunning; }
}
