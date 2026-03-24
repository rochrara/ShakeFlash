/*
 * Copyright 2026 RRARA
 * Licensed under the Apache License, Version 2.0
 */
package com.rrara.shakeflash;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class ShakeService extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private CameraManager cameraManager;
    private String cameraId;
    private boolean isFlashOn = false;
    private float last_x, last_y, last_z;
    private long lastUpdate = 0;
    private int shakeCount = 0;
    private long lastShakeTime = 0;
    private long lastToggleTime = 0;

    private static final int SHAKE_THRESHOLD = 8000;
    private static final int SHAKE_COUNT_TARGET = 8;
    private static final int SHAKE_WINDOW = 1000;
    private static final int MIN_TOGGLE_INTERVAL = 1000;

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        try {
            cameraId = cameraManager.getCameraIdList()[0];
        } catch (Exception e) {
            e.printStackTrace();
        }

        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }

        startMyForeground();
    }

    private void startMyForeground() {
        String channelId = "shake_flash_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "ShakeFlash Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("ShakeFlash czuwa")
                .setContentText("Potrząśnij telefonem")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA);
        } else {
            startForeground(1, notification);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long curTime = System.currentTimeMillis();

        if ((curTime - lastUpdate) > 50) {
            long diffTime = (curTime - lastUpdate);
            lastUpdate = curTime;

            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;

            if (speed > SHAKE_THRESHOLD) {
                shakeCount++;
                if (shakeCount >= SHAKE_COUNT_TARGET) {
                    toggleFlash();
                    shakeCount = 0;
                }
                lastShakeTime = curTime;
            }

            last_x = x;
            last_y = y;
            last_z = z;
        }

        if (System.currentTimeMillis() - lastShakeTime > SHAKE_WINDOW) {
            shakeCount = 0;
        }
    }

    private void toggleFlash() {
        long now = System.currentTimeMillis();
        if (now - lastToggleTime < MIN_TOGGLE_INTERVAL) return;

        try {
            isFlashOn = !isFlashOn;
            cameraManager.setTorchMode(cameraId, isFlashOn);
            lastToggleTime = now;
        } catch (Exception e) {
            try {
                cameraId = cameraManager.getCameraIdList()[0];
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
        try {
            cameraManager.setTorchMode(cameraId, false);
        } catch (Exception ignored) {}
    }
}