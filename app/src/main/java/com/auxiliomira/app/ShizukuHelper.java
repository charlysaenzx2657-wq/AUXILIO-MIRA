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

    public static boolean tienePermiso(Context ctx) {
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
        try {
            Class<?> shizukuClass = Class.forName("rikka.shizuku.Shizuku");
            Method checkPerm = shizukuClass.getMethod("checkSelfPermission");
            Object result = checkPerm.invoke(null);
            if (result instanceof Integer && (Integer) result == 0) return true;
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
            Class<?> shizukuClass = Class.forName("rikka.shizuku.Shizuku");
            Method pingBinder = shizukuClass.getMethod("pingBinder");
            Object result = pingBinder.invoke(null);
            if (result != null && (Boolean) result) return true;
        } catch (Throwable ignored) {}
        try {
            Class<?> sm = Class.forName("android.os.ServiceManager");
            Method getService = sm.getMethod("getService", String.class);
            IBinder b = (IBinder) getService.invoke(null, "shizuku");
            if (b != null && b.pingBinder()) return true;
        } catch (Throwable ignored) {}
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh","-c","ls /data/local/tmp/shizuku 2>/dev/null"});
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            if (br.readLine() != null) { p.waitFor(); return true; }
            p.waitFor();
        } catch (Exception ignored) {}
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh","-c","ps -A | grep shizuku"});
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String l = br.readLine();
            p.waitFor();
            if (l != null && l.contains("shizuku")) return true;
        } catch (Exception ignored) {}
        return false;
    }

    public static String estadoPermisoDetallado(Context ctx) {
        boolean shizukuInst = shizukuInstalado(ctx);
        boolean shizukuRun = shizukuActivo(ctx);
        boolean permPM = false;
        boolean permSecure = false;
        boolean permShizuku = false;
        boolean rishWorks = false;

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
            Class<?> shizukuClass = Class.forName("rikka.shizuku.Shizuku");
            Method checkPerm = shizukuClass.getMethod("checkSelfPermission");
            Object r = checkPerm.invoke(null);
            permShizuku = (r instanceof Integer && (Integer) r == 0);
        } catch (Throwable ignored) {}

        // Probar rish directo
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh","-c","rish -c \"echo test\" 2>/dev/null"});
            p.waitFor();
            rishWorks = (p.exitValue() == 0);
        } catch (Exception ignored) {}

        StringBuilder sb = new StringBuilder();
        sb.append("Shizuku instalado: ").append(shizukuInst ? "SI" : "NO").append("\n");
        sb.append("Shizuku corriendo: ").append(shizukuRun ? "SI" : "NO").append("\n");
        sb.append("Permiso PackageManager: ").append(permPM ? "SI" : "NO").append("\n");
        sb.append("Permiso Settings escritura: ").append(permSecure ? "SI" : "NO").append("\n");
        sb.append("Permiso via Shizuku API: ").append(permShizuku ? "SI" : "NO").append("\n");
        sb.append("rish funcional: ").append(rishWorks ? "SI" : "NO").append("\n\n");

        if (permPM || permSecure || permShizuku || rishWorks) {
            sb.append("RESULTADO: PUEDES INYECTAR");
        } else if (shizukuRun) {
            sb.append("RESULTADO: Abre Shizuku y autoriza esta app (icono +)");
        } else if (shizukuInst) {
            sb.append("RESULTADO: Inicia el servicio Shizuku primero");
        } else {
            sb.append("RESULTADO: Instala Shizuku o usa ADB");
        }

        return sb.toString();
    }

    /**
     * Aplica comandos INTENTANDO TODOS LOS MÉTODOS para cada uno.
     * No pre-verifica permisos — intenta aplicar y reporta lo que sí funciona.
     * Esto resuelve el problema de ROMs que cachean el estado del permiso.
     */
    public static void aplicarComandosAsync(Context ctx, String[] comandos,
                                             ProgressCallback cb, Handler ui) {
        new Thread(() -> {
            int total = comandos.length;
            // Detectar capacidades una vez al inicio
            boolean tieneRoot = tieneRoot();
            boolean tieneShizuku = shizukuActivo(ctx);
            Log.i(TAG, "Capacidades: root=" + tieneRoot + " shizuku=" + tieneShizuku);

            for (int i = 0; i < total; i++) {
                final int idx = i;
                String cmd = comandos[i].trim();
                boolean ok = false;
                String err = null;

                // INTENTAR LOS 3 MÉTODOS EN ORDEN, no importa lo que diga el "permiso"
                // Método 1: ContentResolver (más rápido, requiere WRITE_SECURE_SETTINGS)
                try {
                    ok = aplicarConContentResolver(ctx, cmd);
                } catch (Throwable t) { err = t.getMessage(); }

                // Método 2: rish/Shizuku si disponible y CR falló
                if (!ok && tieneShizuku) {
                    try {
                        ok = aplicarConShizuku(ctx, cmd);
                    } catch (Throwable t) { err = t.getMessage(); }
                }

                // Método 3: su si hay root y los anteriores fallaron
                if (!ok && tieneRoot) {
                    try {
                        ok = aplicarConSu(cmd);
                    } catch (Throwable t) { err = t.getMessage(); }
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

    private static boolean aplicarConShizuku(Context ctx, String cmd) {
        // Método 1: rish binario
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh","-c","rish -c \"" + cmd + "\" 2>&1"});
            p.waitFor();
            if (p.exitValue() == 0) return true;
        } catch (Exception ignored) {}

        // Método 2: Shizuku newProcess via reflection
        try {
            Class<?> shizukuClass = Class.forName("rikka.shizuku.Shizuku");
            Method newProcess = shizukuClass.getMethod("newProcess",
                String[].class, String[].class, String.class);
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
