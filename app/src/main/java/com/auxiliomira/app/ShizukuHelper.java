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
import java.util.concurrent.atomic.AtomicInteger;

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
            Shizuku.addRequestPermissionResultListener((rc, gr) -> Log.i(TAG, "Permiso: " + gr));
            Shizuku.addBinderReceivedListenerSticky(() -> { Log.i(TAG, "Binder OK"); solicitarPermisoSiNecesario(); });
            Shizuku.addBinderDeadListener(() -> Log.w(TAG, "Binder muerto"));
            listenerRegistrado = true;
        } catch (Throwable t) { Log.w(TAG, "init: " + t.getMessage()); }
    }

    public static void solicitarPermisoSiNecesario() {
        try {
            if (Shizuku.isPreV11()) return;
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) return;
            if (Shizuku.shouldShowRequestPermissionRationale()) return;
            Shizuku.requestPermission(REQUEST_CODE);
        } catch (Throwable t) { Log.w(TAG, "solicitarPermiso: " + t.getMessage()); }
    }

    public static boolean shizukuInstalado(Context ctx) {
        try { ctx.getPackageManager().getPackageInfo(SHIZUKU_PKG, 0); return true; }
        catch (PackageManager.NameNotFoundException e) { return false; }
    }

    public static void abrirShizuku(Context ctx) {
        try {
            Intent i = ctx.getPackageManager().getLaunchIntentForPackage(SHIZUKU_PKG);
            if (i != null) { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(i); }
            else {
                Intent s = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + SHIZUKU_PKG));
                s.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(s);
            }
        } catch (Exception e) { Log.w(TAG, "abrirShizuku: " + e); }
    }

    public static boolean tienePermiso(Context ctx) {
        try {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) return true;
        } catch (Throwable ignored) {}
        try {
            if (ctx.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED) return true;
        } catch (Throwable ignored) {}
        try {
            ContentResolver cr = ctx.getContentResolver();
            String original = Settings.Secure.getString(cr, "user_setup_complete");
            Settings.Secure.putString(cr, "user_setup_complete", original != null ? original : "1");
            return true;
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean tieneRoot() {
        try { Process p = Runtime.getRuntime().exec("which su"); p.waitFor(); return p.exitValue() == 0; }
        catch (Exception e) { return false; }
    }

    public static boolean shizukuActivo(Context ctx) {
        if (!shizukuInstalado(ctx)) return false;
        try { return Shizuku.pingBinder(); } catch (Throwable ignored) {}
        try {
            Class<?> sm = Class.forName("android.os.ServiceManager");
            Method gs = sm.getMethod("getService", String.class);
            IBinder b = (IBinder) gs.invoke(null, "shizuku");
            if (b != null && b.pingBinder()) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    public static String estadoPermisoDetallado(Context ctx) {
        boolean shInst = shizukuInstalado(ctx), shRun = shizukuActivo(ctx);
        boolean pPM = false, pSec = false, pSh = false, shReg = false;
        try { pPM = ctx.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED; } catch (Throwable ignored) {}
        try {
            ContentResolver cr = ctx.getContentResolver();
            String o = Settings.Secure.getString(cr, "user_setup_complete");
            Settings.Secure.putString(cr, "user_setup_complete", o != null ? o : "1");
            pSec = true;
        } catch (Throwable ignored) {}
        try {
            if (Shizuku.pingBinder()) { shReg = true; pSh = (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED); }
        } catch (Throwable ignored) {}
        StringBuilder sb = new StringBuilder();
        sb.append("Shizuku instalado: ").append(shInst?"SI":"NO").append("\n");
        sb.append("Shizuku corriendo: ").append(shRun?"SI":"NO").append("\n");
        sb.append("App registrada: ").append(shReg?"SI":"NO").append("\n");
        sb.append("Permiso PM: ").append(pPM?"SI":"NO").append("\n");
        sb.append("Permiso Settings: ").append(pSec?"SI":"NO").append("\n");
        sb.append("Permiso Shizuku: ").append(pSh?"SI":"NO").append("\n\n");
        if (pPM || pSec || pSh) sb.append("RESULTADO: PUEDES INYECTAR");
        else if (shReg) sb.append("RESULTADO: Toca CONECTAR SHIZUKU");
        else if (shRun) sb.append("RESULTADO: Reinicia y autoriza popup");
        else if (shInst) sb.append("RESULTADO: Inicia Shizuku");
        else sb.append("RESULTADO: Instala Shizuku");
        return sb.toString();
    }

    /**
     * Inyección ULTRA rápida y verificada.
     * - Detecta comandos shell (wm, cmd, pm, am) y los manda a Shizuku rish
     * - Para "settings put", verifica leyendo de vuelta
     * - Sin sleep entre cmds
     * - Reporta progreso cada 10 cmds para no saturar UI
     */
    public static void aplicarComandosAsync(Context ctx, String[] comandos,
                                             ProgressCallback cb, Handler ui) {
        new Thread(() -> {
            int total = comandos.length;
            boolean tieneRoot = tieneRoot();
            boolean tieneShPerm = false;
            try {
                tieneShPerm = Shizuku.pingBinder() &&
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
            } catch (Throwable ignored) {}
            Log.i(TAG, "Inyectando " + total + " | root=" + tieneRoot + " shPerm=" + tieneShPerm);

            int aplicados = 0;
            int errores = 0;

            for (int i = 0; i < total; i++) {
                final int idx = i;
                String cmd = comandos[i].trim();
                boolean ok = false;
                String err = null;

                try {
                    // Detectar tipo de comando
                    if (cmd.startsWith("settings put ")) {
                        ok = aplicarSettingPut(ctx, cmd);
                    } else if (cmd.startsWith("wm ") || cmd.startsWith("cmd ") ||
                               cmd.startsWith("pm ") || cmd.startsWith("am ")) {
                        // Comando shell - solo Shizuku/su pueden ejecutarlo
                        if (tieneShPerm) ok = aplicarConShizukuRish(cmd);
                        if (!ok && tieneRoot) ok = aplicarConSu(cmd);
                    } else {
                        // Genérico - intentar shell
                        if (tieneShPerm) ok = aplicarConShizukuRish(cmd);
                        if (!ok && tieneRoot) ok = aplicarConSu(cmd);
                    }
                } catch (Throwable t) { err = t.getMessage(); }

                if (ok) aplicados++; else errores++;

                final boolean okF = ok;
                final String cmdF = cmd;
                final String errF = err;
                final int aplF = aplicados;

                // Reportar progreso cada 10 cmds para no saturar
                if (cb != null && (i % 10 == 0 || i == total - 1)) {
                    ui.post(() -> {
                        if (okF) cb.onProgress(idx + 1, total, cmdF);
                        else cb.onError(cmdF, errF != null ? errF : "no aplicado");
                    });
                }
            }

            final int aplFinal = aplicados;
            if (cb != null) ui.post(() -> {
                cb.onProgress(aplFinal, total, "");
                cb.onComplete(total);
            });
        }).start();
    }

    public static void aplicarComandos(Context ctx, String[] comandos, ProgressCallback cb) {
        Handler h = new Handler(ctx.getMainLooper());
        aplicarComandosAsync(ctx, comandos, cb, h);
    }

    /**
     * Aplica "settings put X Y Z" usando ContentResolver (ultra rápido).
     * Si la clave no existe en el sistema, falla silenciosamente.
     */
    private static boolean aplicarSettingPut(Context ctx, String cmd) {
        try {
            String[] partes = cmd.split(" ", 5);
            if (partes.length < 4) return false;
            String ns = partes[2];
            String key = partes[3];
            String val = partes.length > 4 ? partes[4] : "";
            ContentResolver cr = ctx.getContentResolver();
            switch (ns) {
                case "system": return Settings.System.putString(cr, key, val);
                case "secure": return Settings.Secure.putString(cr, key, val);
                case "global": return Settings.Global.putString(cr, key, val);
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
            os.flush(); os.close();
            p.waitFor();
            return p.exitValue() == 0;
        } catch (Exception e) { return false; }
    }

    private static boolean aplicarConShizukuRish(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh","-c","rish -c \"" + cmd + "\" 2>&1"});
            p.waitFor();
            if (p.exitValue() == 0) return true;
        } catch (Exception ignored) {}
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
        String[] cmd = {
            "settings put global dropbox_age_seconds 0",
            "settings put global dropbox_max_files 0",
            "settings put global dropbox_max_kb 0",
            "settings put global event_log_max_rows 0",
            "settings put global fstrim_mandatory_interval 0",
            "settings put global package_verifier_enable 0",
            "settings put global netstats_enabled 0",
        };
        for (String c : cmd) try { aplicarSettingPut(ctx, c); } catch (Throwable ignored) {}
        try {
            java.io.File c1 = ctx.getCacheDir(); if (c1 != null) deleteDir(c1);
            java.io.File c2 = ctx.getExternalCacheDir(); if (c2 != null) deleteDir(c2);
        } catch (Exception ignored) {}
    }

    private static void deleteDir(java.io.File dir) {
        if (dir != null && dir.isDirectory()) {
            java.io.File[] files = dir.listFiles();
            if (files != null) for (java.io.File f : files) {
                if (f.isDirectory()) deleteDir(f); else f.delete();
            }
        }
    }
}
