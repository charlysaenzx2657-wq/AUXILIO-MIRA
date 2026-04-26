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

import rikka.shizuku.Shizuku;

public class ShizukuHelper {

    private static final String TAG = "ShizukuHelper";
    private static final String SHIZUKU_PKG = "moe.shizuku.privileged.api";
    private static final int REQUEST_CODE = 7777;
    private static boolean listenerRegistrado = false;

    public interface ProgressCallback {
        void onProgress(int actual, int total, String cmd);
        void onComplete(int total);
        void onError(String cmd, String error);
    }

    public static void inicializar(Context ctx) {
        if (listenerRegistrado) return;
        try {
            Shizuku.addRequestPermissionResultListener((requestCode, grantResult) -> {
                Log.i(TAG, "Permiso Shizuku result: " + grantResult);
            });
            Shizuku.addBinderReceivedListenerSticky(() -> {
                Log.i(TAG, "Shizuku binder recibido");
                solicitarPermisoSiNecesario();
            });
            Shizuku.addBinderDeadListener(() -> {
                Log.w(TAG, "Shizuku binder muerto");
            });
            listenerRegistrado = true;
        } catch (Throwable t) {
            Log.w(TAG, "No se pudo registrar listeners Shizuku: " + t.getMessage());
        }
    }

    public static void solicitarPermisoSiNecesario() {
        try {
            if (Shizuku.isPreV11()) return;
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) return;
            if (Shizuku.shouldShowRequestPermissionRationale()) return;
            Shizuku.requestPermission(REQUEST_CODE);
        } catch (Throwable t) {
            Log.w(TAG, "solicitarPermiso: " + t.getMessage());
        }
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

    public static boolean tienePermiso(Context ctx) {
        try {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        } catch (Throwable ignored) {}
        try {
            int result = ctx.checkCallingOrSelfPermission(
                "android.permission.WRITE_SECURE_SETTINGS");
            if (result == PackageManager.PERMISSION_GRANTED) return true;
        } catch (Throwable ignored) {}
        try {
            ContentResolver cr = ctx.getContentResolver();
            String original = Settings.Secure.getString(cr, "user_setup_complete");
            Settings.Secure.putString(cr, "user_setup_complete",
                original != null ? original : "1");
            return true;
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean tieneRoot() {
        try {
            Process p = Runtime.getRuntime().exec("which su");
            p.waitFor();
            return p.exitValue() == 0;
        } catch (Exception e) { return false; }
    }

    public static boolean shizukuActivo(Context ctx) {
        if (!shizukuInstalado(ctx)) return false;
        try {
            return Shizuku.pingBinder();
        } catch (Throwable ignored) {}
        try {
            Class<?> sm = Class.forName("android.os.ServiceManager");
            Method getService = sm.getMethod("getService", String.class);
            IBinder b = (IBinder) getService.invoke(null, "shizuku");
            if (b != null && b.pingBinder()) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    public static String estadoPermisoDetallado(Context ctx) {
        boolean shizukuInst = shizukuInstalado(ctx);
        boolean shizukuRun = shizukuActivo(ctx);
        boolean permPM = false;
        boolean permSecure = false;
        boolean permShizuku = false;
        boolean shizukuRegistrada = false;

        try {
            permPM = ctx.checkCallingOrSelfPermission(
                "android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable ignored) {}

        try {
            ContentResolver cr = ctx.getContentResolver();
            String original = Settings.Secure.getString(cr, "user_setup_complete");
            Settings.Secure.putString(cr, "user_setup_complete",
                original != null ? original : "1");
            permSecure = true;
        } catch (Throwable ignored) {}

        try {
            if (Shizuku.pingBinder()) {
                shizukuRegistrada = true;
                permShizuku = (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED);
            }
        } catch (Throwable ignored) {}

        StringBuilder sb = new StringBuilder();
        sb.append("Shizuku instalado: ").append(shizukuInst ? "SI" : "NO").append("\n");
        sb.append("Shizuku corriendo: ").append(shizukuRun ? "SI" : "NO").append("\n");
        sb.append("App registrada en Shizuku: ").append(shizukuRegistrada ? "SI" : "NO").append("\n");
        sb.append("Permiso PackageManager: ").append(permPM ? "SI" : "NO").append("\n");
        sb.append("Permiso Settings: ").append(permSecure ? "SI" : "NO").append("\n");
        sb.append("Permiso Shizuku: ").append(permShizuku ? "SI" : "NO").append("\n\n");

        if (permPM || permSecure || permShizuku) {
            sb.append("RESULTADO: PUEDES INYECTAR");
        } else if (shizukuRegistrada) {
            sb.append("RESULTADO: Toca CONECTAR SHIZUKU para autorizar");
        } else if (shizukuRun) {
            sb.append("RESULTADO: Reinicia AUXILIO MIRA y autoriza el popup");
        } else if (shizukuInst) {
            sb.append("RESULTADO: Inicia el servicio Shizuku");
        } else {
            sb.append("RESULTADO: Instala Shizuku");
        }
        return sb.toString();
    }

    public static void aplicarComandosAsync(Context ctx, String[] comandos,
                                             ProgressCallback cb, Handler ui) {
        new Thread(() -> {
            int total = comandos.length;
            boolean tieneRoot = tieneRoot();
            boolean tieneShizukuPerm = false;
            try {
                tieneShizukuPerm = Shizuku.pingBinder() &&
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
            } catch (Throwable ignored) {}
            Log.i(TAG, "root=" + tieneRoot + " shizukuPerm=" + tieneShizukuPerm);

            for (int i = 0; i < total; i++) {
                final int idx = i;
                String cmd = comandos[i].trim();
                boolean ok = false;
                String err = null;

                try { ok = aplicarConContentResolver(ctx, cmd); }
                catch (Throwable t) { err = t.getMessage(); }

                if (!ok && tieneShizukuPerm) {
                    try { ok = aplicarConShizukuRish(cmd); }
                    catch (Throwable t) { err = t.getMessage(); }
                }

                if (!ok && tieneRoot) {
                    try { ok = aplicarConSu(cmd); }
                    catch (Throwable t) { err = t.getMessage(); }
                }

                final boolean okFinal = ok;
                final String cmdFinal = cmd;
                final String errFinal = err;
                if (cb != null) ui.post(() -> {
                    if (okFinal) cb.onProgress(idx + 1, total, cmdFinal);
                    else cb.onError(cmdFinal, errFinal != null ? errFinal : "no aplicado");
                });
                try { Thread.sleep(3); } catch (Exception ignored) {}
            }
            if (cb != null) ui.post(() -> cb.onComplete(total));
        }).start();
    }

    public static void aplicarComandos(Context ctx, String[] comandos, ProgressCallback cb) {
        Handler h = new Handler(ctx.getMainLooper());
        aplicarComandosAsync(ctx, comandos, cb, h);
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
        } catch (Throwable e) { return false; }
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
     * Ejecuta comando vía rish (binario que Shizuku instala en /data/local/tmp).
     * Si Shizuku está corriendo y autorizado, rish funciona automáticamente.
     */
    private static boolean aplicarConShizukuRish(String cmd) {
        // Método 1: rish directo
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh","-c","rish -c \"" + cmd + "\" 2>&1"});
            p.waitFor();
            if (p.exitValue() == 0) return true;
        } catch (Exception ignored) {}

        // Método 2: via reflection (newProcess es privado, pero accesible por reflexión)
        try {
            Method newProcess = Shizuku.class.getDeclaredMethod("newProcess",
                String[].class, String[].class, String.class);
            newProcess.setAccessible(true);
            String[] args = {"sh", "-c", cmd};
            Object proc = newProcess.invoke(null, args, null, null);
            if (proc != null) {
                Method waitFor = proc.getClass().getMethod("waitFor");
                Object exitCode = waitFor.invoke(proc);
                return exitCode instanceof Integer && (Integer) exitCode == 0;
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
