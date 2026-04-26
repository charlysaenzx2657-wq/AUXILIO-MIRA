package com.auxiliomira.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.graphics.Typeface;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final String TAG = "AUXILIOMIRA";
    private static final String PKG_FF    = "com.dts.freefireth";
    private static final String PKG_FFMAX = "com.dts.freefiremax";

    static class Modulo {
        String titulo, descripcion;
        String[] comandos;
        boolean activo = false;
        Switch sw;
        Modulo(String t, String d, String[] c) { titulo=t; descripcion=d; comandos=c; }
    }

    private final List<Modulo> modulos = new ArrayList<>();
    private TextView tvStatus, tvProgress;
    private ProgressBar progressBar;
    private Button btnAplicar;
    private final Handler handler = new Handler();
    private final List<String> cmdsPendientes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        modulos.add(new Modulo("👆 Velocidad del Puntero",   "Optimiza touch, sensibilidad y velocidad",            AllCommands.CMDS_TOUCH_POINTER));
        modulos.add(new Modulo("🌀 Giroscopio Pro",           "Calibra giroscopio y sensores de movimiento",         AllCommands.CMDS_GYRO_SENSOR));
        modulos.add(new Modulo("🖥️ FPS y Pantalla Max",       "Maximiza FPS, refresco y GPU",                        AllCommands.CMDS_FPS_PANTALLA));
        modulos.add(new Modulo("⚡ Rendimiento CPU y RAM",    "Libera RAM y optimiza CPU para gaming",               AllCommands.CMDS_RENDIMIENTO_CPU));
        modulos.add(new Modulo("🌐 Red y Anti-Lag",           "Reduce ping y latencia en Free Fire",                 AllCommands.CMDS_RED_LATENCIA));
        modulos.add(new Modulo("♿ Talkback OFF Total",        "Desactiva accesibilidad",                             AllCommands.CMDS_TALKBACK));
        modulos.add(new Modulo("🔋 Anti-Thermal Gaming",      "Evita throttling térmico en partidas largas",         AllCommands.CMDS_BATERIA));
        modulos.add(new Modulo("🔊 Audio Sin Vibración",      "Optimiza audio y desactiva vibraciones",              AllCommands.CMDS_AUDIO));
        modulos.add(new Modulo("🔵 Modificación Media",       "Foco azul + velocidad extrema + cursor alterado",     AllCommands.CMDS_MODS_MEDIA));
        modulos.add(new Modulo("📜 Scroll Pro",               "Scroll y deslizamientos optimizados en HUD",          AllCommands.CMDS_SCROLL));
        modulos.add(new Modulo("🎯 Anti-Recoil",              "Detecta velocidades altas y compensa recoil",         AllCommands.CMDS_ANTIRECOIL));
        buildUI();
    }

    private void buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF0A0A0A);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, 0, 0, 100);
        root.addView(buildHeader());
        root.addView(buildStatusBox());
        root.addView(secTitulo("  MÓDULOS DE OPTIMIZACIÓN"));
        for (Modulo mod : modulos) root.addView(buildModuloCard(mod));
        root.addView(secTitulo("  HERRAMIENTAS ESPECIALES"));
        root.addView(buildEspeciales());
        root.addView(secTitulo("  ABRIR JUEGO"));
        root.addView(buildBotonesFF());
        root.addView(buildAcciones());
        root.addView(buildFooter());
        scroll.addView(root);
        setContentView(scroll);
    }

    private View buildHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setGravity(Gravity.CENTER);
        h.setPadding(24, 56, 24, 24);
        h.setBackgroundColor(0xFF120000);
        TextView logo = tv("◉  AUXILIO MIRA", 30, 0xFFFF0000, true);
        logo.setGravity(Gravity.CENTER);
        TextView sub = tv("FREE FIRE SYSTEM OPTIMIZER", 11, 0xFFFF4444, false);
        sub.setGravity(Gravity.CENTER);
        TextView total = tv("▶  1967 COMANDOS · 11 MÓDULOS", 11, 0xFFFF6600, true);
        total.setGravity(Gravity.CENTER);
        total.setPadding(0,10,0,0);
        h.addView(logo); h.addView(sub); h.addView(total);
        return h;
    }

    private View buildStatusBox() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(24,14,24,14);
        box.setBackgroundColor(0xFF0D0D0D);
        tvStatus = tv("⚡ Activa módulos y toca APLICAR", 13, 0xFFAAAAAA, false);
        tvStatus.setGravity(Gravity.CENTER);
        tvProgress = tv("0%", 22, 0xFFFF0000, true);
        tvProgress.setGravity(Gravity.CENTER);
        tvProgress.setVisibility(View.GONE);
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFFF0000));
        progressBar.setVisibility(View.GONE);
        box.addView(tvStatus); box.addView(tvProgress); box.addView(progressBar);
        return box;
    }

    private View buildModuloCard(Modulo mod) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFF120000);
        card.setPadding(20,14,20,14);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(16,5,16,5);
        card.setLayoutParams(lp);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1f));
        TextView titulo = tv(mod.titulo, 14, 0xFFFFFFFF, true);
        TextView desc   = tv(mod.descripcion, 11, 0xFF888888, false);
        TextView count  = tv(mod.comandos.length + " comandos", 10, 0xFFFF4400, false);
        col.addView(titulo); col.addView(desc); col.addView(count);
        Switch sw = new Switch(this);
        mod.sw = sw;
        sw.setOnCheckedChangeListener((b, checked) -> {
            mod.activo = checked;
            card.setBackgroundColor(checked ? 0xFF1A0000 : 0xFF120000);
            titulo.setTextColor(checked ? 0xFFFF4444 : 0xFFFFFFFF);
        });
        row.addView(col); row.addView(sw);
        card.addView(row);
        return card;
    }

    private View buildEspeciales() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(16,4,16,4);

        Button btnCalib = btn("🎯  CALIBRAR SENSIBILIDAD", 0xFF001A33, 0xFF00AAFF);
        btnCalib.setOnClickListener(v -> mostrarCalibracion());
        Button btnCache = btn("🗑️  LIMPIAR CACHÉ DEL SISTEMA", 0xFF1A1A00, 0xFFFFFF00);
        btnCache.setOnClickListener(v -> limpiarCache());
        Button btnFloat = btn("🪟  ACTIVAR VENTANA FLOTANTE", 0xFF001A00, 0xFF00FF00);
        btnFloat.setOnClickListener(v -> abrirVentanaFlotante());
        Button btnShizuku = btn("🔌  CONECTAR CON SHIZUKU", 0xFF0A001A, 0xFFCC88FF);
        btnShizuku.setOnClickListener(v -> conectarShizuku());

        box.addView(btnCalib); box.addView(btnCache); box.addView(btnFloat); box.addView(btnShizuku);
        return box;
    }

    private View buildBotonesFF() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(16,4,16,4);
        Button btnFF = btn("🔥 FREE FIRE", 0xFF1A0000, 0xFFFF4400);
        btnFF.setOnClickListener(v -> abrirJuego(PKG_FF, "Free Fire"));
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0,-2,1f);
        lp1.setMargins(0,0,6,0);
        Button btnFFMax = btn("⚡ FREE FIRE MAX", 0xFF1A0A00, 0xFFFF8800);
        btnFFMax.setOnClickListener(v -> abrirJuego(PKG_FFMAX, "Free Fire MAX"));
        row.addView(btnFF, lp1);
        row.addView(btnFFMax, new LinearLayout.LayoutParams(0,-2,1f));
        return row;
    }

    private View buildAcciones() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(16,10,16,10);
        btnAplicar = btn("🔴   APLICAR AUXILIO MIRA", 0xFFCC0000, 0xFFFFFFFF);
        btnAplicar.setTextSize(17);
        btnAplicar.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,0,0,10);
        btnAplicar.setOnClickListener(v -> pedirConfirmacion());
        Button btnADB = btn("📋   VER COMANDO ADB / SHIZUKU", 0xFF001A00, 0xFF00FF00);
        btnADB.setOnClickListener(v -> mostrarADB());
        box.addView(btnAplicar, lp);
        box.addView(btnADB);
        return box;
    }

    private View buildFooter() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(24,8,24,8);
        TextView warn = tv("⚠ Requiere WRITE_SECURE_SETTINGS via Shizuku o ADB", 10, 0xFFFF8800, false);
        warn.setGravity(Gravity.CENTER);
        TextView ver = tv("v3.0 | SENSIS GOOD FF studio | @sensisgoodffoficial", 9, 0xFF444444, false);
        ver.setGravity(Gravity.CENTER);
        box.addView(warn); box.addView(ver);
        return box;
    }

    // ──────── LÓGICA ────────

    private void pedirConfirmacion() {
        int total = 0;
        StringBuilder sb = new StringBuilder();
        for (Modulo m : modulos) {
            if (m.activo) { total += m.comandos.length; sb.append("• ").append(m.titulo).append("\n"); }
        }
        if (total == 0) { Toast.makeText(this,"⚠ Activa al menos un módulo",Toast.LENGTH_SHORT).show(); return; }
        final int t = total;
        new AlertDialog.Builder(this)
            .setTitle("AUXILIO MIRA")
            .setMessage("Se aplicarán "+t+" comandos:\n\n"+sb+"\nSin root requerido. Reversible.")
            .setPositiveButton("✅ APLICAR", (d,w) -> iniciarInyeccion())
            .setNegativeButton("CANCELAR", null).show();
    }

    private void iniciarInyeccion() {
        if (!ShizukuHelper.tienePermiso(this) && !ShizukuHelper.tieneRoot()
                && !ShizukuHelper.shizukuActivo(this)) {
            new AlertDialog.Builder(this)
                .setTitle("⚠ Sin permiso")
                .setMessage("Necesitas activar uno de estos:\n\n" +
                    "• Shizuku (recomendado, sin PC)\n" +
                    "• ADB desde PC (una sola vez)\n" +
                    "• Root\n\n" +
                    "¿Abrir Shizuku ahora?")
                .setPositiveButton("ABRIR SHIZUKU", (d,w) -> conectarShizuku())
                .setNeutralButton("VER COMANDO ADB", (d,w) -> mostrarADB())
                .setNegativeButton("CANCELAR", null).show();
            return;
        }
        cmdsPendientes.clear();
        for (Modulo m : modulos) if (m.activo) for (String c : m.comandos) cmdsPendientes.add(c);
        btnAplicar.setEnabled(false);
        progressBar.setMax(cmdsPendientes.size());
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
        tvProgress.setVisibility(View.VISIBLE);
        tvStatus.setText("⚡ Aplicando "+cmdsPendientes.size()+" comandos...");
        ShizukuHelper.aplicarComandosAsync(this, cmdsPendientes.toArray(new String[0]),
            new ShizukuHelper.ProgressCallback() {
                public void onProgress(int a, int t, String c) {
                    progressBar.setProgress(a);
                    tvProgress.setText((a*100/t)+"%");
                    tvStatus.setText("⚡ Cmd "+a+" / "+t);
                }
                public void onComplete(int t) { onCompletado(t); }
                public void onError(String c, String e) {}
            }, handler);
    }

    private void onCompletado(int total) {
        btnAplicar.setEnabled(true);
        tvProgress.setText("✅ 100%");
        tvStatus.setText("✅ "+total+" comandos aplicados.");
        new AlertDialog.Builder(this)
            .setTitle("🎯 AUXILIO MIRA")
            .setMessage("¡Completado!\n\n"+total+" configuraciones aplicadas.\n\nReinicia Free Fire.")
            .setPositiveButton("🔥 FREE FIRE", (d,w) -> abrirJuego(PKG_FF,"Free Fire"))
            .setNeutralButton("⚡ FF MAX", (d,w) -> abrirJuego(PKG_FFMAX,"Free Fire MAX"))
            .setNegativeButton("CERRAR", null).show();
    }

    private void mostrarCalibracion() {
        tvStatus.setText("🔍 Analizando dispositivo...");
        handler.postDelayed(() -> {
            SensibilidadCalibrator.ResultadoCalib r = SensibilidadCalibrator.calibrar(this);
            new AlertDialog.Builder(this)
                .setTitle("🎯 Calibración de Sensibilidad")
                .setMessage(r.resumen)
                .setPositiveButton("✅ APLICAR", (d,w) ->
                    ShizukuHelper.aplicarComandosAsync(this, r.comandos.toArray(new String[0]),
                        new ShizukuHelper.ProgressCallback() {
                            public void onProgress(int a, int t, String c) { tvStatus.setText("🎯 Calibrando "+a+"/"+t); }
                            public void onComplete(int t) { tvStatus.setText("✅ Calibración aplicada: "+r.perfil); }
                            public void onError(String c, String e) {}
                        }, handler))
                .setNegativeButton("CANCELAR", null).show();
            tvStatus.setText("⚡ Análisis completo");
        }, 800);
    }

    private void limpiarCache() {
        new AlertDialog.Builder(this)
            .setTitle("🗑️ Limpiar Caché")
            .setMessage("¿Limpiar caché del sistema?\n\nLibera espacio para Free Fire.")
            .setPositiveButton("✅ LIMPIAR", (d,w) -> {
                tvStatus.setText("🗑️ Limpiando caché...");
                handler.postDelayed(() -> {
                    ShizukuHelper.limpiarCache(this);
                    tvStatus.setText("✅ Caché limpiado");
                    Toast.makeText(this,"✅ Caché limpiado",Toast.LENGTH_SHORT).show();
                }, 500);
            })
            .setNegativeButton("CANCELAR", null).show();
    }

    private void abrirVentanaFlotante() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:"+getPackageName())), 1001);
            Toast.makeText(this,"Activa 'Mostrar sobre otras apps' y vuelve",Toast.LENGTH_LONG).show();
            return;
        }
        try {
            startService(new Intent(this, FloatingWindowService.class));
            Toast.makeText(this,"🪟 Ventana flotante activada",Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this,"❌ Error: "+e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int reqCode, int resCode, Intent data) {
        super.onActivityResult(reqCode, resCode, data);
        if (reqCode == 1001) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                abrirVentanaFlotante();
            }
        }
    }

    private void conectarShizuku() {
        if (!ShizukuHelper.shizukuInstalado(this)) {
            new AlertDialog.Builder(this)
                .setTitle("Shizuku no instalado")
                .setMessage("Shizuku permite inyectar comandos sin PC.\n\n¿Instalarlo desde Play Store?")
                .setPositiveButton("INSTALAR", (d,w) -> ShizukuHelper.abrirShizuku(this))
                .setNegativeButton("CANCELAR", null).show();
            return;
        }
        if (ShizukuHelper.shizukuActivo(this)) {
            if (ShizukuHelper.tienePermiso(this)) {
                new AlertDialog.Builder(this)
                    .setTitle("✅ Shizuku conectado")
                    .setMessage("Shizuku está activo y la app tiene permisos.\n\n¡Ya puedes inyectar sin PC!")
                    .setPositiveButton("OK", null).show();
            } else {
                new AlertDialog.Builder(this)
                    .setTitle("⚠ Shizuku activo pero sin permiso")
                    .setMessage("Shizuku está corriendo pero la app no tiene WRITE_SECURE_SETTINGS.\n\n" +
                        "Abre Shizuku → autoriza esta app, o ejecuta:\n\n" +
                        "rish pm grant com.auxiliomira.app android.permission.WRITE_SECURE_SETTINGS")
                    .setPositiveButton("ABRIR SHIZUKU", (d,w) -> ShizukuHelper.abrirShizuku(this))
                    .setNegativeButton("OK", null).show();
            }
        } else {
            new AlertDialog.Builder(this)
                .setTitle("⚠ Shizuku no está corriendo")
                .setMessage("Abre la app Shizuku y actívala (vía ADB inalámbrico o pareo wireless).\n\n¿Abrir Shizuku?")
                .setPositiveButton("ABRIR", (d,w) -> ShizukuHelper.abrirShizuku(this))
                .setNegativeButton("CANCELAR", null).show();
        }
    }

    private void abrirJuego(String pkg, String nombre) {
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i != null) { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); }
        else new AlertDialog.Builder(this)
            .setTitle(nombre+" no instalado")
            .setMessage("¿Instalarlo desde Play Store?")
            .setPositiveButton("INSTALAR", (d,w) -> {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+pkg))); }
                catch (Exception e) { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id="+pkg))); }
            })
            .setNegativeButton("CANCELAR", null).show();
    }

    private void mostrarADB() {
        new AlertDialog.Builder(this)
            .setTitle("📋 Activar — Shizuku o ADB")
            .setMessage(
                "OPCIÓN 1 — Shizuku (sin PC, recomendado):\n" +
                "1. Instala Shizuku desde Play Store\n" +
                "2. Ábrelo y actívalo (pareo inalámbrico)\n" +
                "3. En AUXILIO MIRA toca CONECTAR SHIZUKU\n" +
                "4. ¡Listo!\n\n" +
                "OPCIÓN 2 — ADB desde PC (una sola vez):\n" +
                "adb shell pm grant com.auxiliomira.app android.permission.WRITE_SECURE_SETTINGS")
            .setPositiveButton("OK", null).show();
    }

    private TextView tv(String text, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }
    private Button btn(String text, int bg, int fg) {
        Button b = new Button(this);
        b.setText(text); b.setTextSize(14); b.setTextColor(fg); b.setBackgroundColor(bg);
        b.setPadding(0,18,0,18);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,4,0,4);
        b.setLayoutParams(lp);
        return b;
    }
    private TextView secTitulo(String text) {
        TextView t = tv(text, 11, 0xFFFF6600, true);
        t.setPadding(24,18,24,6);
        return t;
    }
}
