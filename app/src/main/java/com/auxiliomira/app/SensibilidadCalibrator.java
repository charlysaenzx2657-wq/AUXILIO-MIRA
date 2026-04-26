package com.auxiliomira.app;

import android.content.Context;
import android.content.ContentResolver;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.os.Build;
import java.util.ArrayList;
import java.util.List;

public class SensibilidadCalibrator {

    public static class ResultadoCalib {
        public int pointerSpeed;
        public int gyroSensitivity;
        public int touchSensitivity;
        public int reportRate;
        public int gyroSampleRate;
        public String perfil;
        public List<String> comandos = new ArrayList<>();
        public String resumen;
    }

    public static ResultadoCalib calibrar(Context ctx) {
        ResultadoCalib r = new ResultadoCalib();
        ContentResolver cr = ctx.getContentResolver();

        // ── Recolectar info del dispositivo ──
        DisplayMetrics dm = new DisplayMetrics();
        ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getMetrics(dm);

        float density = dm.density;           // ej: 2.0 = 320dpi
        int dpi = dm.densityDpi;              // dpi exacto
        int screenW = dm.widthPixels;
        int screenH = dm.heightPixels;
        float refreshRate = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getRefreshRate();

        // RAM total aproximada
        android.app.ActivityManager.MemoryInfo memInfo = new android.app.ActivityManager.MemoryInfo();
        ((android.app.ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE))
                .getMemoryInfo(memInfo);
        long ramMB = memInfo.totalMem / (1024 * 1024);

        // ── Lógica de calibración ──
        // Pantalla pequeña (<720p) → sensi más alta
        // Pantalla grande (>1080p) → sensi moderada
        // RAM baja (<3GB) → menos agresivo
        // RAM alta (>6GB) → modo pro

        boolean pantallaGrande = screenW >= 1080 || screenH >= 1920;
        boolean ramAlta = ramMB >= 4096;
        boolean altaRefresh = refreshRate >= 90;

        if (pantallaGrande && ramAlta && altaRefresh) {
            r.perfil = "PRO GAMING (Pantalla grande + RAM alta + 90Hz)";
            r.pointerSpeed = 7;
            r.gyroSensitivity = 9;
            r.touchSensitivity = 12;
            r.reportRate = 240;
            r.gyroSampleRate = 400;
        } else if (pantallaGrande && ramAlta) {
            r.perfil = "AVANZADO (Pantalla grande + RAM alta)";
            r.pointerSpeed = 6;
            r.gyroSensitivity = 8;
            r.touchSensitivity = 10;
            r.reportRate = 240;
            r.gyroSampleRate = 200;
        } else if (!pantallaGrande && ramAlta) {
            r.perfil = "COMPACTO PRO (Pantalla pequeña + RAM alta)";
            r.pointerSpeed = 7;
            r.gyroSensitivity = 7;
            r.touchSensitivity = 10;
            r.reportRate = 240;
            r.gyroSampleRate = 200;
        } else {
            r.perfil = "EQUILIBRADO (Dispositivo estándar)";
            r.pointerSpeed = 5;
            r.gyroSensitivity = 6;
            r.touchSensitivity = 8;
            r.reportRate = 120;
            r.gyroSampleRate = 200;
        }

        // ── Generar comandos personalizados ──
        r.comandos.add("settings put system pointer_speed " + r.pointerSpeed);
        r.comandos.add("settings put system gyroscope_sensitivity " + r.gyroSensitivity);
        r.comandos.add("settings put system touch_sensitivity_level " + r.touchSensitivity);
        r.comandos.add("settings put system touch_report_rate " + r.reportRate);
        r.comandos.add("settings put system gyro_sample_rate " + r.gyroSampleRate);
        r.comandos.add("settings put system gyro_update_rate " + r.gyroSampleRate);
        r.comandos.add("settings put system motion_event_sample_rate " + (r.reportRate / 2));
        r.comandos.add("settings put system touch_boost_hz " + (r.reportRate * 2));
        r.comandos.add("settings put system fling_velocity " + (r.pointerSpeed * 1000));
        r.comandos.add("settings put system maximum_fling_velocity " + (r.pointerSpeed * 1200));
        r.comandos.add("settings put system minimum_fling_velocity " + (r.pointerSpeed * 5));
        r.comandos.add("settings put system touch_game_boost_level " + (r.touchSensitivity / 4));
        r.comandos.add("settings put system touch_response_boost " + (r.touchSensitivity / 6));
        r.comandos.add("settings put system touch_velocity_scale " + String.format("%.1f", r.touchSensitivity / 8.0f));
        r.comandos.add("settings put system gyro_fast_response " + (r.gyroSensitivity / 3));
        r.comandos.add("settings put system sensor_perf_mode " + (ramAlta ? 3 : 1));
        r.comandos.add("settings put system touch_latency_reduction " + (altaRefresh ? 3 : 1));
        r.comandos.add("settings put system touch_prediction_duration " + (altaRefresh ? 2 : 5));
        r.comandos.add("settings put global window_animation_scale 0.5");
        r.comandos.add("settings put global transition_animation_scale 0.5");
        r.comandos.add("settings put global animator_duration_scale 0.5");
        r.comandos.add("settings put system peak_refresh_rate " + (int) refreshRate);
        r.comandos.add("settings put system min_refresh_rate 60");
        r.comandos.add("settings put system touch_precision_mode " + (ramAlta ? 3 : 1));
        r.comandos.add("settings put system touch_dynamic_sensitivity " + (r.touchSensitivity / 4));
        r.comandos.add("settings put system gyro_deadzone 0");
        r.comandos.add("settings put system gyro_smoothing 0");
        r.comandos.add("settings put system gyro_prediction 1");
        r.comandos.add("settings put system sensor_fusion_enabled 1");
        r.comandos.add("settings put system gyro_drift_compensation 1");

        // ── Resumen legible ──
        r.resumen = "📱 Dispositivo analizado:\n" +
                "• Pantalla: " + screenW + "x" + screenH + " (" + dpi + " dpi)\n" +
                "• RAM: " + ramMB + " MB\n" +
                "• Refresco: " + (int) refreshRate + " Hz\n" +
                "• Android: " + Build.VERSION.RELEASE + "\n" +
                "• Modelo: " + Build.MODEL + "\n\n" +
                "🎯 Perfil detectado: " + r.perfil + "\n\n" +
                "⚙️ Valores calibrados:\n" +
                "• Puntero: " + r.pointerSpeed + "/7\n" +
                "• Giroscopio: " + r.gyroSensitivity + "/10\n" +
                "• Touch: " + r.touchSensitivity + "/15\n" +
                "• Report rate: " + r.reportRate + " Hz\n" +
                "• Gyro sample: " + r.gyroSampleRate + " Hz\n\n" +
                "✅ " + r.comandos.size() + " comandos listos para aplicar";

        return r;
    }
}
