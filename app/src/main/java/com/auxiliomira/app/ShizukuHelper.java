package com.auxiliomira.app;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;

public class ShizukuHelper {

    private static final String TAG = "ShizukuHelper";
    private static final String SHIZUKU_PKG = "moe.shizuku.privileged.api";

    public interface ProgressCallback {
        void onProgress(int actual, int total, String cmd);
        void onComplete(int total);
        void onError(String cmd, String error);
    }

    public static boolean shizukuInstalado(Context ctx) {
        try {
            ctx.getPackageManager().getPackageInfo(SHIZUKU_PKG, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

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

    /**
     * Verifica si la app puede escribir en Settings.Secure (lo que Shizuku otorga).
     */
    public static boolean tienePermiso(Context ctx) {
        try {
            String tmpKey = "_aux_mira_test_" + System.currentTimeMillis();
            Settings.Secure.putString(ctx.getContentResolver(), tmpKey, "1");
            String r = Settings.Secure.getString(ctx.getContentResolver(), tmpKey);
            return "1".equals(r);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean tieneRoot() {
        try {
            Process p = Runtime.getRuntime().exec("which su");
            p.waitFor();
            return p.exitValue() == 0;
        } catch (Exception e) { return false; }
    }

    /**
     * Detecta si el servicio Shizuku está corriendo, intentando varias formas:
     * 1) Via reflection de la clase Shizuku si está enlazada
     * 2) Via binder de "shizuku_service" en el ServiceManager
     * 3) Via el binario "rish" en /data/local/tmp
     * 4) Via process listing de "moe.shizuku"
     */
    public static boolean shizukuActivo(Context ctx) {
        if (!shizukuInstalado(ctx)) return false;
        // 1) Reflection (si la app incluyera el SDK)
        try {
            Class<?> shizukuClass = Class.forName("rikka.shizuku.Shizuku");
            Method pingBinder = shizukuClass.getMethod("pingBinder");
            Object result = pingBinder.invoke(null);
            if (result != null && (Boolean) result) return true;
        } catch (Throwable ignored) {}

        // 2) Binder en ServiceManager
        try {
            Class<?> sm = Class.forName("android.os.ServiceManager");
            Method getService = sm.getMethod("getService", String.class);
            IBinder b = (IBinder) getService.invoke(null, "shizuku");
            if (b != null && b.pingBinder()) return true;
        } catch (Throwable ignored) {}

        // 3) Binario rish
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh","-c","ls /data/local/tmp/shizuku 2>/dev/null"});
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            if (br.readLine() != null) { p.waitFor(); return true; }
            p.waitFor();
        } catch (Exception ignored) {}

        // 4) ps listing
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh","-c","ps -A | grep shizuku"});
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String l = br.readLine();
            p.waitFor();
            if (l != null && l.contains("shizuku")) return true;
        } catch (Exception ignored) {}

        return false;
    }

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
                String err = null;
                try {
                    switch (modo) {
                        case 1: ok = aplicarConContentResolver(ctx, cmd); break;
                        case 2: ok = aplicarConSu(cmd); break;
                        case 3: ok = aplicarConShizuku(ctx, cmd); break;
                        default: err = "Sin permiso"; break;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error: " + cmd, e);
                    err = e.getMessage();
                }
                final boolean okFinal = ok;
                final String cmdFinal = cmd;
                final String errFinal = err;
                if (cb != null) ui.post(() -> {
                    if (okFinal) cb.onProgress(idx + 1, total, cmdFinal);
                    else cb.onError(cmdFinal, errFinal != null ? errFinal : "no aplicado");
                });
                try { Thread.sleep(4); } catch (Exception ignored) {}
            }
            if (cb != null) ui.post(() -> cb.onComplete(total));
        }).start();
    }

    public static void aplicarComandos(Context ctx, String[] comandos, ProgressCallback cb) {
        Handler h = new Handler(ctx.getMainLooper());
        aplicarComandosAsync(ctx, comandos, cb, h);
    }

    private static int detectarMejorModo(Context ctx) {
        if (tienePermiso(ctx)) return 1;
        if (tieneRoot()) return 2;
        if (shizukuActivo(ctx)) return 3;
        return 0;
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
        } catch (Throwable e) { Log.w(TAG, "CR: " + e.getMessage()); }
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

    private static boolean aplicarConShizuku(Context ctx, String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh","-c","rish -c \"" + cmd + "\""});
            p.waitFor();
            if (p.exitValue() == 0) return true;
        } catch (Exception ignored) {}
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
            try { aplicarConContentResolver(ctx, c); } catch (Throwable ignored) {}
        }
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
