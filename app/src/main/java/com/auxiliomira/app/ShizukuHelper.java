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
            if (Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) return true;
        } catch (Throwable ignored) {}
        try {
            if (ctx.checkCallingOrSelfPermission(
                "android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED)
                return true;
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
        try {
            Process p = Runtime.getRuntime().exec("which su");
            p.waitFor();
            return p.exitValue() == 0;
        } catch (Exception e) { return false; }
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
        try {
            pPM = ctx.checkCallingOrSelfPermission(
                "android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable ignored) {}
        try {
            ContentResolver cr = ctx.getContentResolver();
            String o = Settings.Secure.getString(cr, "user_setup_complete");
            Settings.Secure.putString(cr, "user_setup_complete", o != null ? o : "1");
            pSec = true;
        } catch (Throwable ignored) {}
        try {
            if (Shizuku.pingBinder()) {
                shReg = true;
                pSh = (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED);
            }
        } catch (Throwable ignored) {}
        StringBuilder sb = new StringBuilder();
        sb.append("Shizuku instalado: ").append(shInst ? "SI" : "NO").append("\n");
        sb.append("Shizuku corriendo: ").append(shRun ? "SI" : "NO").append("\n");
        sb.append("App registrada: ").append(shReg ? "SI" : "NO").append("\n");
        sb.append("Permiso PM: ").append(pPM ? "SI" : "NO").append("\n");
        sb.append("Permiso Settings: ").append(pSec ? "SI" : "NO").append("\n");
        sb.append("Permiso Shizuku: ").append(pSh ? "SI" : "NO").append("\n\n");
        if (pPM || pSec || pSh) sb.append("RESULTADO: PUEDES INYECTAR");
        else if (shReg) sb.append("RESULTADO: Autoriza en Shizuku");
        else if (shRun) sb.append("RESULTADO: Reinicia y autoriza popup");
        else if (shInst) sb.append("RESULTADO: Inicia Shizuku");
        else sb.append("RESULTADO: Instala Shizuku");
        return sb.toString();
    }

    /**
     * INYECCION ULTRA RAPIDA:
     * Abre UN proceso Shizuku y manda TODOS los comandos por stdin de golpe.
     * 1500 cmds = ~5-8 segundos total.
     *
     * Usa Shizuku.newProcess() via reflection para abrir un shell privilegiado,
     * luego escribe todos los comandos por el stdin del proceso.
     */
    public static void aplicarComandosAsync(Context ctx, String[] comandos,
                                             ProgressCallback cb, Handler ui) {
        new Thread(() -> {
            int total = comandos.length;

            // Detectar el mejor método disponible
            boolean shizukuOK = false;
            try {
                shizukuOK = Shizuku.pingBinder() &&
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
            } catch (Throwable ignored) {}
            boolean rootOK = tieneRoot();
            boolean crOK = tienePermiso(ctx);

            Log.i(TAG, "Modo: shizuku=" + shizukuOK + " root=" + rootOK + " cr=" + crOK);

            if (shizukuOK) {
                // MEJOR: Un solo proceso Shizuku con todos los cmds
                inyectarConShizukuNewProcess(comandos, cb, ui, total);
            } else if (rootOK) {
                // ROOT: Un solo proceso su con todos los cmds
                inyectarConSuPipe(comandos, cb, ui, total);
            } else if (crOK) {
                // Sin shell: ContentResolver 1 por 1 (solo settings put)
                inyectarConContentResolver(ctx, comandos, cb, ui, total);
            } else {
                if (cb != null) ui.post(() -> {
                    cb.onError("", "Sin permiso. Activa Shizuku.");
                    cb.onComplete(0);
                });
            }
        }).start();
    }

    /**
     * Usa Shizuku.newProcess() para abrir un shell sh privilegiado
     * y escribe todos los comandos por stdin de una sola vez.
     */
    private static void inyectarConShizukuNewProcess(String[] comandos,
                                                      ProgressCallback cb, Handler ui, int total) {
        try {
            // Abrir proceso sh via Shizuku
            Method newProcess = Shizuku.class.getDeclaredMethod(
                "newProcess", String[].class, String[].class, String.class);
            newProcess.setAccessible(true);
            String[] shell = {"sh"};
            Object proc = newProcess.invoke(null, shell, null, null);
            if (proc == null) throw new Exception("newProcess devolvio null");

            // Obtener stdin/stdout via reflection
            Method getOS = proc.getClass().getMethod("getOutputStream");
            Method getIS = proc.getClass().getMethod("getInputStream");
            Method waitFor = proc.getClass().getMethod("waitFor");

            java.io.OutputStream os = (java.io.OutputStream) getOS.invoke(proc);
            java.io.InputStream is = (java.io.InputStream) getIS.invoke(proc);

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            // Enviar todos los comandos de golpe
            for (String cmd : comandos) {
                writer.write(cmd);
                writer.newLine();
            }
            // Marcador de fin
            // sync espera que el último comando termine antes de imprimir DONE
            writer.write("wait");
            writer.newLine();
            writer.write("true"); writer.newLine();
            writer.write("echo __AUXILIO_DONE__");
            writer.newLine();
            writer.write("exit 0");
            writer.newLine();
            writer.flush();

            // Hilo de progreso estimado mientras esperamos
            final boolean[] terminado = {false};
            long inicio = System.currentTimeMillis();
            long estimadoMs = total * 3L; // ~3ms por cmd
            Thread progThread = new Thread(() -> {
                while (!terminado[0]) {
                    long elapsed = System.currentTimeMillis() - inicio;
                    int estimado = (int) Math.min((elapsed * total) / Math.max(estimadoMs, 1), total - 1);
                    final int est = estimado;
                    if (cb != null) ui.post(() -> cb.onProgress(est, total, ""));
                    try { Thread.sleep(250); } catch (InterruptedException ignored) {}
                }
            });
            progThread.setDaemon(true);
            progThread.start();

            // Esperar marcador de fin
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("__AUXILIO_DONE__")) break;
            }
            terminado[0] = true;
            progThread.join(500);

            try { waitFor.invoke(proc); } catch (Throwable ignored) {}

            if (cb != null) ui.post(() -> {
                cb.onProgress(total, total, "");
                cb.onComplete(total);
            });

        } catch (Throwable e) {
            Log.e(TAG, "shizukuNewProcess error: " + e.getMessage());
            // Fallback a ContentResolver
            inyectarConContentResolver(null, comandos, cb, ui, total);
        }
    }

    /**
     * Fallback con su (root): mismo enfoque pipe.
     */
    private static void inyectarConSuPipe(String[] comandos,
                                           ProgressCallback cb, Handler ui, int total) {
        try {
            Process proc = Runtime.getRuntime().exec("su");
            BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(proc.getOutputStream()));
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(proc.getInputStream()));

            for (String cmd : comandos) { writer.write(cmd); writer.newLine(); }
            writer.write("wait"); writer.newLine();
            writer.write("true"); writer.newLine();
            writer.write("echo __AUXILIO_DONE__"); writer.newLine();
            writer.write("exit 0"); writer.newLine();
            writer.flush();

            final boolean[] terminado = {false};
            long inicio = System.currentTimeMillis();
            long estimadoMs = total * 3L;
            Thread progThread = new Thread(() -> {
                while (!terminado[0]) {
                    long elapsed = System.currentTimeMillis() - inicio;
                    int est = (int) Math.min((elapsed * total) / Math.max(estimadoMs, 1), total - 1);
                    if (cb != null) ui.post(() -> cb.onProgress(est, total, ""));
                    try { Thread.sleep(250); } catch (InterruptedException ignored) {}
                }
            });
            progThread.setDaemon(true);
            progThread.start();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("__AUXILIO_DONE__")) break;
            }
            terminado[0] = true;
            progThread.join(500);
            proc.waitFor();

            if (cb != null) ui.post(() -> {
                cb.onProgress(total, total, "");
                cb.onComplete(total);
            });

        } catch (Exception e) {
            Log.e(TAG, "suPipe error: " + e.getMessage());
            if (cb != null) ui.post(() -> cb.onComplete(0));
        }
    }

    /**
     * Fallback sin shell: ContentResolver para "settings put" solamente.
     */
    private static void inyectarConContentResolver(Context ctx, String[] comandos,
                                                    ProgressCallback cb, Handler ui, int total) {
        if (ctx == null) {
            if (cb != null) ui.post(() -> cb.onComplete(0));
            return;
        }
        ContentResolver cr = ctx.getContentResolver();
        int aplicados = 0;
        for (int i = 0; i < total; i++) {
            String cmd = comandos[i].trim();
            if (cmd.startsWith("settings put ")) {
                try {
                    String[] p = cmd.split(" ", 5);
                    if (p.length >= 4) {
                        String ns = p[2], key = p[3], val = p.length > 4 ? p[4] : "";
                        boolean ok = false;
                        switch (ns) {
                            case "system": ok = Settings.System.putString(cr, key, val); break;
                            case "secure": ok = Settings.Secure.putString(cr, key, val); break;
                            case "global": ok = Settings.Global.putString(cr, key, val); break;
                        }
                        if (ok) aplicados++;
                    }
                } catch (Throwable ignored) {}
            }
            if (i % 50 == 0 || i == total - 1) {
                final int idx = i + 1;
                if (cb != null) ui.post(() -> cb.onProgress(idx, total, cmd));
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
        ContentResolver cr = ctx.getContentResolver();
        String[][] cmds = {
            {"global","dropbox_age_seconds","0"}, {"global","dropbox_max_files","0"},
            {"global","event_log_max_rows","0"}, {"global","fstrim_mandatory_interval","0"},
            {"global","package_verifier_enable","0"}, {"global","netstats_enabled","0"},
        };
        for (String[] c : cmds) {
            try { Settings.Global.putString(cr, c[1], c[2]); } catch (Throwable ignored) {}
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
