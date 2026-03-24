/*
 * Copyright 2026 RRARA
 * Licensed under the Apache License, Version 2.0
 */
package com.rrara.shakeflash;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private View indicator, squareCamera, squareNotify;
    private TextView statusTxt;
    private Button btnStart, btnStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        indicator = findViewById(R.id.statusIndicator);
        squareCamera = findViewById(R.id.squareCamera);
        squareNotify = findViewById(R.id.squareNotify);
        statusTxt = findViewById(R.id.statusText);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);

        btnStart.setOnClickListener(v -> {
            if (areAllPermissionsGranted()) {
                startShakeService();
            } else {
                Toast.makeText(this, "Błąd: Najpierw wyraź wszystkie zgody (czerwone pola)!", Toast.LENGTH_LONG).show();
                askEverything();
            }
        });

        findViewById(R.id.layoutCamera).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                askSingle(Manifest.permission.CAMERA);
            }
        });

        findViewById(R.id.layoutNotify).setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    askSingle(Manifest.permission.POST_NOTIFICATIONS);
                }
            }
        });

        btnStop.setOnClickListener(v -> {
            stopService(new Intent(this, ShakeService.class));
            updateUI();
        });

        updateUI();
    }

    private boolean areAllPermissionsGranted() {
        boolean cameraOk = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean notifyOk = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifyOk = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return cameraOk && notifyOk;
    }

    private void askEverything() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.CAMERA);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), 100);
    }

    private void askSingle(String perm) {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, perm)) {
            showSettingsDialog();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{perm}, 100);
        }
    }

    private void showSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Zgody zablokowane")
                .setMessage("Otwórz ustawienia i ręcznie zezwól na uprawnienia.")
                .setPositiveButton("Ustawienia", (d, w) -> {
                    Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    i.setData(Uri.fromParts("package", getPackageName(), null));
                    startActivity(i);
                })
                .setNegativeButton("Anuluj", null).show();
    }

    private void startShakeService() {
        ContextCompat.startForegroundService(this, new Intent(this, ShakeService.class));
        updateUI();
        Toast.makeText(this, "ShakeFlash aktywowany!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int rc, String[] p, int[] gr) {
        super.onRequestPermissionsResult(rc, p, gr);
        updateUI();
        if (areAllPermissionsGranted()) {
            startShakeService();
        }
    }

    private void updateUI() {
        boolean running = isMyServiceRunning(ShakeService.class);
        indicator.setBackgroundColor(running ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));
        statusTxt.setText(running ? "CZUWANIE: WŁĄCZONE" : "CZUWANIE: WYŁĄCZONE");

        boolean camOk = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        squareCamera.setBackgroundColor(camOk ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));

        boolean notifyOk = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifyOk = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        squareNotify.setBackgroundColor(notifyOk ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));

        btnStart.setVisibility(running ? View.GONE : View.VISIBLE);
        btnStop.setVisibility(running ? View.VISIBLE : View.GONE);
    }

    private boolean isMyServiceRunning(Class<?> sc) {
        ActivityManager m = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo s : m.getRunningServices(Integer.MAX_VALUE)) {
            if (sc.getName().equals(s.service.getClassName())) return true;
        }
        return false;
    }

    @Override protected void onResume() { super.onResume(); updateUI(); }
}