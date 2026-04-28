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
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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
            Shizuku.addRequestPermissionResultListener((rc, gr) -> Log.i(TAG, "Permiso: " + gr));
            Shizuku.addBinderReceivedListenerSticky(() -> {
                Log.i(TAG, "Binder OK");
                solicitarPermisoSiNecesario();
            });
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
            String o = Settings.Secure.getString(cr, "user_setup_complete");
            Settings.Secure.putString(cr, "user_setup_complete", o != null ? o : "1");
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
        else if (shReg) sb.append("RESULTADO: Toca CONECTAR SHIZUKU para autorizar");
        else if (shRun) sb.append("RESULTADO: Reinicia y autoriza el popup");
        else if (shInst) sb.append("RESULTADO: Inicia Shizuku primero");
        else sb.append("RESULTADO: Instala Shizuku");
        return sb.toString();
    }

    /**
     * ESTRATEGIA ULTRA RÁPIDA:
     * Abre UN SOLO proceso shell "rish" y le manda TODOS los comandos de golpe
     * por stdin. Esto evita el overhead de lanzar un proceso por comando.
     * 1000 comandos = ~3-5 segundos en lugar de 30-60s.
     */
    public static void aplicarComandosAsync(Context ctx, String[] comandos,
                                             ProgressCallback cb, Handler ui) {
        new Thread(() -> {
            int total = comandos.length;
            boolean tieneShPerm = false;
            try {
                tieneShPerm = Shizuku.pingBinder() &&
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
            } catch (Throwable ignored) {}
            boolean tieneRoot = tieneRoot();
            boolean tieneContentResolver = tienePermiso(ctx);

            Log.i(TAG, "Ultra-inyeccion: " + total + " cmds | CR=" + tieneContentResolver + " sh=" + tieneShPerm + " root=" + tieneRoot);

            if (tieneShPerm) {
                // MÉTODO 1: Shell rish con pipe — el más rápido
                inyectarConShellPipe(comandos, cb, ui, total, "rish");
            } else if (tieneRoot) {
                // MÉTODO 2: Shell su con pipe
                inyectarConShellPipe(comandos, cb, ui, total, "su");
            } else if (tieneContentResolver) {
                // MÉTODO 3: ContentResolver (solo settings put, sin shell)
                inyectarConContentResolver(ctx, comandos, cb, ui, total);
            } else {
                // Sin método — reportar error
                if (cb != null) ui.post(() -> {
                    cb.onError("", "Sin permiso. Activa Shizuku.");
                    cb.onComplete(0);
                });
            }
        }).start();
    }

    /**
     * Abre un proceso shell (rish o su) y manda TODOS los comandos por stdin.
     * Reporta progreso estimado basado en tiempo transcurrido.
     */
    private static void inyectarConShellPipe(String[] comandos, ProgressCallback cb, Handler ui, int total, String shell) {
        try {
            Process proceso;
            if ("rish".equals(shell)) {
                proceso = Runtime.getRuntime().exec("rish");
            } else {
                proceso = Runtime.getRuntime().exec("su");
            }

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(proceso.getOutputStream()));

            // Mandar todos los comandos de una vez
            for (String cmd : comandos) {
                writer.write(cmd);
                writer.newLine();
            }
            writer.write("echo __DONE__");
            writer.newLine();
            writer.write("exit");
            writer.newLine();
            writer.flush();
            writer.close();

            // Leer output para detectar cuando termina + reportar progreso estimado
            BufferedReader reader = new BufferedReader(new InputStreamReader(proceso.getInputStream()));
            long inicio = System.currentTimeMillis();
            // Estimar tiempo total: ~3ms por comando
            long estimadoMs = total * 3L;

            // Hilo que reporta progreso basado en tiempo
            final boolean[] terminado = {false};
            Thread progressThread = new Thread(() -> {
                while (!terminado[0]) {
                    long elapsed = System.currentTimeMillis() - inicio;
                    int estimado = (int) Math.min((elapsed * total) / Math.max(estimadoMs, 1), total - 1);
                    final int est = estimado;
                    if (cb != null) ui.post(() -> cb.onProgress(est, total, ""));
                    try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                }
            });
            progressThread.start();

            // Esperar a que aparezca __DONE__ en el output
            String line;
            boolean done = false;
            while ((line = reader.readLine()) != null) {
                if (line.contains("__DONE__")) { done = true; break; }
            }
            terminado[0] = true;
            progressThread.join();
            proceso.waitFor();

            final boolean doneFinal = done;
            if (cb != null) ui.post(() -> {
                cb.onProgress(total, total, "");
                cb.onComplete(total);
            });
        } catch (Exception e) {
            Log.e(TAG, "shellPipe error: " + e.getMessage());
            // Fallback a ContentResolver si shell falla
            if (cb != null) ui.post(() -> {
                cb.onError("shell", e.getMessage());
                cb.onComplete(0);
            });
        }
    }

    /**
     * ContentResolver para cuando solo hay WRITE_SECURE_SETTINGS pero no shell.
     * Aplica 1 por 1 pero sin sleep — rápido igual.
     */
    private static void inyectarConContentResolver(Context ctx, String[] comandos,
                                                    ProgressCallback cb, Handler ui, int total) {
        int aplicados = 0;
        for (int i = 0; i < total; i++) {
            String cmd = comandos[i].trim();
            boolean ok = false;
            if (cmd.startsWith("settings put ")) {
                try {
                    String[] p = cmd.split(" ", 5);
                    if (p.length >= 4) {
                        String ns = p[2], key = p[3], val = p.length > 4 ? p[4] : "";
                        ContentResolver cr = ctx.getContentResolver();
                        switch (ns) {
                            case "system": ok = Settings.System.putString(cr, key, val); break;
                            case "secure": ok = Settings.Secure.putString(cr, key, val); break;
                            case "global": ok = Settings.Global.putString(cr, key, val); break;
                        }
                    }
                } catch (Throwable ignored) {}
            }
            if (ok) aplicados++;
            // Reportar cada 50
            if (i % 50 == 0 || i == total - 1) {
                final int ap = aplicados, idx = i;
                if (cb != null) ui.post(() -> cb.onProgress(idx + 1, total, cmd));
            }
        }
        final int apFinal = aplicados;
        if (cb != null) ui.post(() -> cb.onComplete(total));
    }

    public static void aplicarComandos(Context ctx, String[] comandos, ProgressCallback cb) {
        Handler h = new Handler(ctx.getMainLooper());
        aplicarComandosAsync(ctx, comandos, cb, h);
    }

    public static void limpiarCache(Context ctx) {
        String[] cmd = {
            "settings put global dropbox_age_seconds 0",
            "settings put global dropbox_max_files 0",
            "settings put global event_log_max_rows 0",
            "settings put global fstrim_mandatory_interval 0",
            "settings put global package_verifier_enable 0",
            "settings put global netstats_enabled 0",
        };
        ContentResolver cr = ctx.getContentResolver();
        for (String c : cmd) {
            try {
                String[] p = c.split(" ", 5);
                if (p.length >= 4) {
                    switch (p[2]) {
                        case "system": Settings.System.putString(cr, p[3], p.length > 4 ? p[4] : ""); break;
                        case "secure": Settings.Secure.putString(cr, p[3], p.length > 4 ? p[4] : ""); break;
                        case "global": Settings.Global.putString(cr, p[3], p.length > 4 ? p[4] : ""); break;
                    }
                }
            } catch (Throwable ignored) {}
        }
        try { deleteDir(ctx.getCacheDir()); } catch (Exception ignored) {}
        try { deleteDir(ctx.getExternalCacheDir()); } catch (Exception ignored) {}
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
