package com.auxiliomira.app;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;

/**
 * ShizukuHelper — Inyecta settings sin necesitar PC.
 *
 * Estrategia:
 * 1) Si tiene WRITE_SECURE_SETTINGS → usa ContentResolver directo (más rápido)
 * 2) Si Shizuku está corriendo → ejecuta vía ShizukuShell (sin root, sin PC)
 * 3) Si tiene root → usa su
 * 4) Como último recurso → muestra error y pide setup
 */
public class ShizukuHelper {

    private static final String TAG = "ShizukuHelper";
    private static final String SHIZUKU_PKG = "moe.shizuku.privileged.api";

    public interface ProgressCallback {
        void onProgress(int actual, int total, String cmd);
        void onComplete(int total);
        void onError(String cmd, String error);
    }

    /** Verifica si Shizuku está instalado en el dispositivo */
    public static boolean shizukuInstalado(Context ctx) {
        try {
            ctx.getPackageManager().getPackageInfo(SHIZUKU_PKG, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /** Intenta abrir Shizuku para que el usuario lo configure */
    public static void abrirShizuku(Context ctx) {
        try {
            Intent i = ctx.getPackageManager().getLaunchIntentForPackage(SHIZUKU_PKG);
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
            } else {
                Intent store = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + SHIZUKU_PKG));
                store.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(store);
            }
        } catch (Exception e) { Log.w(TAG, "abrirShizuku: " + e); }
    }

    /** ¿Puede inyectar settings? (vía ContentResolver) */
    public static boolean tienePermiso(Context ctx) {
        try {
            String tmpKey = "_aux_mira_test_" + System.currentTimeMillis();
            Settings.Secure.putString(ctx.getContentResolver(), tmpKey, "1");
            Settings.Secure.putString(ctx.getContentResolver(), tmpKey, null);
            return true;
        } catch (SecurityException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /** ¿Hay root? (busca su binario) */
    public static boolean tieneRoot() {
        try {
            Process p = Runtime.getRuntime().exec("which su");
            p.waitFor();
            return p.exitValue() == 0;
        } catch (Exception e) { return false; }
    }

    /**
     * Aplica comandos de forma asíncrona usando el mejor método disponible:
     * 1. ContentResolver directo (si hay WRITE_SECURE_SETTINGS)
     * 2. su (si hay root)
     * 3. shizuku exec (si Shizuku está corriendo y la app vinculada)
     */
    public static void aplicarComandosAsync(Context ctx, String[] comandos,
                                             ProgressCallback cb, Handler ui) {
        new Thread(() -> {
            int modo = detectarMejorModo(ctx);
            Log.i(TAG, "Modo de inyección: " + modo);

            int total = comandos.length;
            for (int i = 0; i < total; i++) {
                final int idx = i;
                String cmd = comandos[i].trim();
                boolean ok = false;
                try {
                    switch (modo) {
                        case 1: ok = aplicarConContentResolver(ctx, cmd); break;
                        case 2: ok = aplicarConSu(cmd); break;
                        case 3: ok = aplicarConShizuku(ctx, cmd); break;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error: " + cmd, e);
                }
                final boolean okFinal = ok;
                final String cmdFinal = cmd;
                if (cb != null) ui.post(() -> {
                    if (okFinal) cb.onProgress(idx + 1, total, cmdFinal);
                    else cb.onError(cmdFinal, "no aplicado");
                });
                try { Thread.sleep(6); } catch (Exception ignored) {}
            }
            if (cb != null) ui.post(() -> cb.onComplete(total));
        }).start();
    }

    /** Versión sincrona simplificada */
    public static void aplicarComandos(Context ctx, String[] comandos, ProgressCallback cb) {
        Handler h = new Handler(ctx.getMainLooper());
        aplicarComandosAsync(ctx, comandos, cb, h);
    }

    /**
     * Detecta el mejor modo de inyección:
     * 1 = ContentResolver (WRITE_SECURE_SETTINGS otorgado)
     * 2 = root (su)
     * 3 = Shizuku
     * 0 = ninguno
     */
    private static int detectarMejorModo(Context ctx) {
        if (tienePermiso(ctx)) return 1;
        if (tieneRoot()) return 2;
        if (shizukuActivo(ctx)) return 3;
        return 0;
    }

    /** Verifica si Shizuku está corriendo y la app puede usarlo */
    public static boolean shizukuActivo(Context ctx) {
        if (!shizukuInstalado(ctx)) return false;
        try {
            // Intenta llamar a la API de Shizuku via reflection
            // (sin agregar la dep en build.gradle, para mantener APK ligera)
            Class<?> shizukuClass = Class.forName("rikka.shizuku.Shizuku");
            Method pingBinder = shizukuClass.getMethod("pingBinder");
            Object result = pingBinder.invoke(null);
            return result != null && (Boolean) result;
        } catch (ClassNotFoundException e) {
            // SDK Shizuku no compilado en la app. Probamos vía exec del binario.
            return shizukuActivoViaExec();
        } catch (Exception e) {
            return shizukuActivoViaExec();
        }
    }

    /** Detecta Shizuku ejecutando "sh -c 'command -v rish'" o similar */
    private static boolean shizukuActivoViaExec() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{
                "sh", "-c", "ls /data/local/tmp/shizuku 2>/dev/null || pidof shizuku_starter"
            });
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = br.readLine();
            p.waitFor();
            return line != null && !line.isEmpty();
        } catch (Exception e) { return false; }
    }

    private static boolean aplicarConContentResolver(Context ctx, String cmd) {
        try {
            String[] partes = cmd.split(" ", 5);
            if (partes.length < 4) return false;
            String ns = partes[2];
            String key = partes[3];
            String val = partes.length > 4 ? partes[4] : "";
            ContentResolver cr = ctx.getContentResolver();
            switch (ns) {
                case "system": Settings.System.putString(cr, key, val); return true;
                case "secure": Settings.Secure.putString(cr, key, val); return true;
                case "global": Settings.Global.putString(cr, key, val); return true;
            }
        } catch (Exception e) { Log.w(TAG, "CR: " + e.getMessage()); }
        return false;
    }

    private static boolean aplicarConSu(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec("su");
            OutputStream os = p.getOutputStream();
            os.write((cmd + "\n").getBytes());
            os.write("exit\n".getBytes());
            os.flush();
            os.close();
            p.waitFor();
            return p.exitValue() == 0;
        } catch (Exception e) { return false; }
    }

    /**
     * Ejecuta vía Shizuku usando el binario "rish" si existe, o vía reflection.
     * Para que funcione 100% con SDK oficial habría que agregar:
     *   implementation 'dev.rikka.shizuku:api:13.1.5'
     *   implementation 'dev.rikka.shizuku:provider:13.1.5'
     * Como mantenemos APK ligera, intentamos los métodos disponibles.
     */
    private static boolean aplicarConShizuku(Context ctx, String cmd) {
        // Método 1: rish binario
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", "rish -c \"" + cmd + "\""});
            p.waitFor();
            if (p.exitValue() == 0) return true;
        } catch (Exception ignored) {}

        // Método 2: shizuku exec via socket
        try {
            Class<?> shizukuClass = Class.forName("rikka.shizuku.Shizuku");
            Method newProcess = shizukuClass.getMethod("newProcess",
                String[].class, String[].class, String.class);
            String[] args = {"sh", "-c", cmd};
            Object proc = newProcess.invoke(null, args, null, null);
            if (proc != null) {
                Method waitFor = proc.getClass().getMethod("waitFor");
                waitFor.invoke(proc);
                return true;
            }
        } catch (Exception ignored) {}

        return false;
    }

    /** Limpia caché del sistema vía settings + cache propia */
    public static void limpiarCache(Context ctx) {
        String[] cmdCache = {
            "settings put global dropbox_age_seconds 0",
            "settings put global dropbox_max_files 0",
            "settings put global dropbox_max_kb 0",
            "settings put global event_log_max_rows 0",
            "settings put global fstrim_mandatory_interval 0",
            "settings put global package_verifier_enable 0",
            "settings put global netstats_enabled 0",
            "settings put global wifi_verbose_logging_enabled 0",
            "settings put global bluetooth_verbose_logging_enabled 0",
        };
        for (String c : cmdCache) {
            try { aplicarConContentResolver(ctx, c); } catch (Exception ignored) {}
        }
        // Borrar caché de la propia app
        try {
            java.io.File cacheDir = ctx.getCacheDir();
            if (cacheDir != null) deleteDir(cacheDir);
            java.io.File extCache = ctx.getExternalCacheDir();
            if (extCache != null) deleteDir(extCache);
        } catch (Exception ignored) {}
    }

    private static void deleteDir(java.io.File dir) {
        if (dir != null && dir.isDirectory()) {
            java.io.File[] files = dir.listFiles();
            if (files != null) for (java.io.File f : files) {
                if (f.isDirectory()) deleteDir(f);
                else f.delete();
            }
        }
    }
}
