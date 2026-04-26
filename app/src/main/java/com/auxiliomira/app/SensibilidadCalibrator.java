package com.auxiliomira.app;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import java.util.ArrayList;
import java.util.List;

public class SensibilidadCalibrator {

    public static class ResultadoCalib {
        public String perfil;
        public String resumen;
        public List<String> comandos = new ArrayList<>();
        public int sensiGeneral, sensiPunto1x, sensiPunto2x, sensiPunto4x, sensiSnipe, sensiBoton;
        public int dpi;
    }

    /** Calibración SUAVE - sin causar recoil */
    public static ResultadoCalib calibrar(Context ctx) {
        ResultadoCalib r = new ResultadoCalib();
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        Display d = wm.getDefaultDisplay();
        d.getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;
        int density = dm.densityDpi;
        long ramMB = ramTotal(ctx) / (1024 * 1024);
        float refresh = d.getRefreshRate();

        // Detección de perfil
        boolean amoled = density >= 480 && refresh >= 90;
        boolean gama_baja = ramMB < 4096;

        if (amoled) {
            r.perfil = "Pro Gaming AMOLED";
            r.sensiGeneral = 95; r.sensiPunto1x = 92; r.sensiPunto2x = 88; r.sensiPunto4x = 75; r.sensiSnipe = 50; r.sensiBoton = 55;
        } else if (gama_baja) {
            r.perfil = "Compacto Pro";
            r.sensiGeneral = 110; r.sensiPunto1x = 105; r.sensiPunto2x = 100; r.sensiPunto4x = 85; r.sensiSnipe = 60; r.sensiBoton = 50;
        } else {
            r.perfil = "Equilibrado";
            r.sensiGeneral = 100; r.sensiPunto1x = 95; r.sensiPunto2x = 90; r.sensiPunto4x = 80; r.sensiSnipe = 55; r.sensiBoton = 52;
        }

        r.comandos.add("settings put system pointer_speed 5");
        r.comandos.add("settings put system gyroscope_sensitivity 5");
        r.comandos.add("settings put system touch_sensitivity_level 8");
        r.comandos.add("settings put system touch_report_rate 240");

        StringBuilder sb = new StringBuilder();
        sb.append("📱 Pantalla: ").append(width).append("x").append(height).append("\n");
        sb.append("🔧 Densidad: ").append(density).append(" dpi\n");
        sb.append("⚡ RAM: ").append(ramMB).append(" MB\n");
        sb.append("🖥️ Refresh: ").append((int)refresh).append(" Hz\n\n");
        sb.append("🎯 Perfil: ").append(r.perfil).append("\n\n");
        sb.append("Sensi recomendada (Free Fire):\n");
        sb.append("• General: ").append(r.sensiGeneral).append("\n");
        sb.append("• Punto rojo: ").append(r.sensiPunto1x).append("\n");
        sb.append("• Mira 2x: ").append(r.sensiPunto2x).append("\n");
        sb.append("• Mira 4x: ").append(r.sensiPunto4x).append("\n");
        sb.append("• Sniper: ").append(r.sensiSnipe).append("\n");
        sb.append("• Botón disparo: ").append(r.sensiBoton);
        r.resumen = sb.toString();
        return sb.length() > 0 ? r : r;
    }

    /**
     * Calibración PERSONALIZADA con preguntas del usuario.
     * @param nivelSensi 1=baja, 2=media, 3=alta, 4=muy alta
     * @param incluirDpi true si quiere ajustar DPI también
     * @param dpiCustom valor custom de DPI (0 = usar el del dispositivo)
     */
    public static ResultadoCalib calibrarPersonalizada(Context ctx, int nivelSensi, boolean incluirDpi, int dpiCustom) {
        ResultadoCalib r = new ResultadoCalib();
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;
        int density = dm.densityDpi;
        long ramMB = ramTotal(ctx) / (1024 * 1024);

        // Multiplicador según nivel
        float mult;
        switch (nivelSensi) {
            case 1: mult = 0.65f; r.perfil = "Sensibilidad BAJA"; break;
            case 2: mult = 0.85f; r.perfil = "Sensibilidad MEDIA"; break;
            case 3: mult = 1.0f;  r.perfil = "Sensibilidad ALTA"; break;
            case 4: mult = 1.15f; r.perfil = "Sensibilidad MUY ALTA"; break;
            default: mult = 0.85f; r.perfil = "Media";
        }

        // Base por hardware - SIN valores extremos para no causar recoil
        int baseGeneral = ramMB < 4096 ? 110 : (density >= 480 ? 95 : 100);

        r.sensiGeneral  = clamp((int)(baseGeneral * mult), 50, 200);
        r.sensiPunto1x  = clamp((int)(r.sensiGeneral * 0.97f), 50, 200);
        r.sensiPunto2x  = clamp((int)(r.sensiGeneral * 0.92f), 50, 200);
        r.sensiPunto4x  = clamp((int)(r.sensiGeneral * 0.80f), 50, 200);
        r.sensiSnipe    = clamp((int)(r.sensiGeneral * 0.55f), 30, 100);
        r.sensiBoton    = clamp((int)(40 + (mult * 12)), 27, 57);

        r.dpi = incluirDpi ? (dpiCustom > 0 ? dpiCustom : density) : 0;

        // Comandos suaves de calibración
        int pointerSpeed = nivelSensi <= 2 ? 4 : (nivelSensi == 3 ? 6 : 7);
        int gyroSens = nivelSensi <= 2 ? 5 : (nivelSensi == 3 ? 7 : 9);

        r.comandos.add("settings put system pointer_speed " + pointerSpeed);
        r.comandos.add("settings put system gyroscope_sensitivity " + gyroSens);
        r.comandos.add("settings put system touch_sensitivity_level " + (4 + nivelSensi));
        r.comandos.add("settings put system touch_report_rate 240");
        r.comandos.add("settings put system motion_event_sample_rate 125");
        r.comandos.add("settings put system gyro_sample_rate 200");
        r.comandos.add("settings put system touch_velocity_scale " + (0.8f + (nivelSensi * 0.15f)));
        r.comandos.add("settings put system touch_high_precision 1");
        r.comandos.add("settings put system touch_jitter_filter 1");

        if (incluirDpi && dpiCustom > 0) {
            r.comandos.add("wm density " + dpiCustom);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🎯 ").append(r.perfil).append("\n\n");
        sb.append("📱 Pantalla: ").append(width).append("x").append(height).append("\n");
        sb.append("🔧 DPI base: ").append(density);
        if (incluirDpi && dpiCustom > 0) sb.append(" → ").append(dpiCustom);
        sb.append("\n");
        sb.append("⚡ RAM: ").append(ramMB).append(" MB\n\n");
        sb.append("=== SENSI FREE FIRE ===\n");
        sb.append("• General: ").append(r.sensiGeneral).append("\n");
        sb.append("• Punto rojo: ").append(r.sensiPunto1x).append("\n");
        sb.append("• Mira 2x: ").append(r.sensiPunto2x).append("\n");
        sb.append("• Mira 4x: ").append(r.sensiPunto4x).append("\n");
        sb.append("• Sniper: ").append(r.sensiSnipe).append("\n");
        sb.append("• Botón disparo: ").append(r.sensiBoton).append("\n\n");
        sb.append("Aplicar para ajustes del sistema");
        r.resumen = sb.toString();
        return r;
    }

    private static long ramTotal(Context ctx) {
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            return mi.totalMem;
        } catch (Exception e) { return 4L * 1024 * 1024 * 1024; }
    }

    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
}
