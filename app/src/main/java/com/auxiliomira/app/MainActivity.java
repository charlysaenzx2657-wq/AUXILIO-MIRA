package com.auxiliomira.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final String TAG = "AUXILIOMIRA";
    private static final String PKG_FF    = "com.dts.freefireth";
    private static final String PKG_FFMAX = "com.dts.freefiremax";
    private static final String PKG_SHIZUKU = "moe.shizuku.privileged.api";

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
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 2001);
        }
        NotifHelper.crearCanales(this);
        try { ShizukuHelper.inicializar(this); ShizukuHelper.solicitarPermisoSiNecesario(); }
        catch (Throwable t) { Log.w(TAG, "Shizuku init: " + t.getMessage()); }

        modulos.add(new Modulo("👆 Velocidad del Puntero", "Optimiza touch", AllCommands.CMDS_TOUCH_POINTER));
        modulos.add(new Modulo("🌀 Giroscopio Pro", "Calibra giroscopio", AllCommands.CMDS_GYRO_SENSOR));
        modulos.add(new Modulo("🖥️ FPS y Pantalla Max", "Maximiza FPS y GPU", AllCommands.CMDS_FPS_PANTALLA));
        modulos.add(new Modulo("⚡ Rendimiento CPU/RAM", "Libera RAM y CPU", AllCommands.CMDS_RENDIMIENTO_CPU));
        modulos.add(new Modulo("🌐 Red y Anti-Lag", "Reduce ping", AllCommands.CMDS_RED_LATENCIA));
        modulos.add(new Modulo("♿ Talkback OFF", "Desactiva accesibilidad", AllCommands.CMDS_TALKBACK));
        modulos.add(new Modulo("🔋 Anti-Thermal", "Evita throttling", AllCommands.CMDS_BATERIA));
        modulos.add(new Modulo("🔊 Audio Sin Vibracion", "Optimiza audio", AllCommands.CMDS_AUDIO));
        modulos.add(new Modulo("🔵 Modificacion Media", "Foco azul + cursor", AllCommands.CMDS_MODS_MEDIA));
        modulos.add(new Modulo("📜 Scroll Pro", "Scroll optimizado", AllCommands.CMDS_SCROLL));
        modulos.add(new Modulo("🎯 Anti-Recoil", "Compensa recoil del juego", AllCommands.CMDS_ANTIRECOIL));
        modulos.add(new Modulo("📺 Resolucion Estirada", "Pantalla mas ancha (21:9)", AllCommands.CMDS_RESOLUCION));
        buildUI();
    }

    private void buildUI() {
        ScrollView scroll = new ScrollView(this);
        GradientDrawable bgRoot = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xFF0A0000, 0xFF000000, 0xFF0A0000});
        scroll.setBackground(bgRoot);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, 0, 0, 80);
        root.addView(buildHeader());
        root.addView(buildEstadoApps());
        root.addView(buildStatusBox());
        root.addView(secTitulo("  ⚡ MODULOS DE OPTIMIZACION"));
        for (Modulo mod : modulos) root.addView(buildModuloCard(mod));
        root.addView(secTitulo("  🛠 HERRAMIENTAS"));
        root.addView(buildEspeciales());
        root.addView(secTitulo("  🎮 ABRIR JUEGO"));
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
        GradientDrawable bgH = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xFF330000, 0xFF120000, 0xFF000000});
        h.setBackground(bgH);
        TextView logo = new TextView(this);
        logo.setText("◉  AUXILIO MIRA");
        logo.setTextSize(32); logo.setTypeface(null, Typeface.BOLD);
        logo.setTextColor(0xFFFFFFFF); logo.setShadowLayer(20f, 0, 0, 0xFFFF0000);
        logo.setGravity(Gravity.CENTER);
        TextView sub = new TextView(this);
        sub.setText("FREE FIRE SYSTEM OPTIMIZER");
        sub.setTextSize(11); sub.setTextColor(0xFFFF6600); sub.setLetterSpacing(0.2f);
        sub.setGravity(Gravity.CENTER);
        TextView total = new TextView(this);
        total.setText("▶ 1895 COMANDOS · 12 MODULOS");
        total.setTextSize(11); total.setTypeface(null, Typeface.BOLD);
        total.setTextColor(0xFFFFCC00); total.setGravity(Gravity.CENTER);
        total.setPadding(0, 10, 0, 0);
        h.addView(logo); h.addView(sub); h.addView(total);
        return h;
    }

    private View buildEstadoApps() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(16, 12, 16, 12);
        box.setBackgroundColor(0xFF0D0000);
        box.addView(secTit("  📱 ESTADO DE APPS"));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 6, 0, 0);
        boolean tShi = appInst(PKG_SHIZUKU), tFF = appInst(PKG_FF), tFFm = appInst(PKG_FFMAX);
        boolean shRun = ShizukuHelper.shizukuActivo(this);
        row.addView(badge("Shizuku", tShi, shRun ? "✅ Activo" : (tShi ? "⚠ Inactivo" : "❌ No inst.")));
        row.addView(badge("Free Fire", tFF, tFF ? "✅ OK" : "❌ No inst."));
        row.addView(badge("FF MAX", tFFm, tFFm ? "✅ OK" : "❌ No inst."));
        box.addView(row);
        return box;
    }

    private View badge(String n, boolean inst, String estado) {
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL); b.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{inst ? 0xFF003300 : 0xFF330000, inst ? 0xFF001100 : 0xFF110000});
        bg.setStroke(2, inst ? 0xFF00FF00 : 0xFFFF4444); bg.setCornerRadius(12);
        b.setBackground(bg); b.setElevation(4f); b.setPadding(10, 12, 10, 12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(4, 0, 4, 0); b.setLayoutParams(lp);
        TextView tn = new TextView(this); tn.setText(n);
        tn.setTextSize(12); tn.setTypeface(null, Typeface.BOLD);
        tn.setTextColor(0xFFFFFFFF); tn.setGravity(Gravity.CENTER);
        TextView te = new TextView(this); te.setText(estado);
        te.setTextSize(10); te.setTextColor(inst ? 0xFF00FF00 : 0xFFFF6644);
        te.setGravity(Gravity.CENTER);
        b.addView(tn); b.addView(te);
        return b;
    }

    private boolean appInst(String pkg) {
        try { getPackageManager().getPackageInfo(pkg, 0); return true; }
        catch (PackageManager.NameNotFoundException e) { return false; }
    }

    private View buildStatusBox() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(20, 14, 20, 14);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF0A0000); bg.setStroke(2, 0xFFFF4400); bg.setCornerRadius(12);
        box.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(16, 12, 16, 4); box.setLayoutParams(lp);
        tvStatus = new TextView(this); tvStatus.setText("⚡ Activa modulos y toca APLICAR");
        tvStatus.setTextSize(13); tvStatus.setTypeface(null, Typeface.BOLD);
        tvStatus.setTextColor(0xFFFFCC00); tvStatus.setGravity(Gravity.CENTER);
        tvProgress = new TextView(this); tvProgress.setText("0%");
        tvProgress.setTextSize(24); tvProgress.setTypeface(null, Typeface.BOLD);
        tvProgress.setTextColor(0xFFFF0000); tvProgress.setShadowLayer(10f, 0, 0, 0xFFFF0000);
        tvProgress.setGravity(Gravity.CENTER); tvProgress.setVisibility(View.GONE);
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
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xFF1A0000, 0xFF0D0000});
        bg.setStroke(1, 0xFF330000); bg.setCornerRadius(12);
        card.setBackground(bg); card.setElevation(6f);
        card.setPadding(18, 14, 18, 14);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(12, 4, 12, 4); card.setLayoutParams(lp);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        TextView t = new TextView(this); t.setText(mod.titulo);
        t.setTextSize(14); t.setTypeface(null, Typeface.BOLD); t.setTextColor(0xFFFFFFFF);
        TextView d = new TextView(this); d.setText(mod.descripcion);
        d.setTextSize(11); d.setTextColor(0xFF888888);
        TextView c = new TextView(this); c.setText(mod.comandos.length + " cmds");
        c.setTextSize(10); c.setTypeface(null, Typeface.BOLD); c.setTextColor(0xFFFF6600);
        col.addView(t); col.addView(d); col.addView(c);
        Switch sw = new Switch(this); mod.sw = sw;
        sw.setOnCheckedChangeListener((b2, ck) -> {
            mod.activo = ck;
            GradientDrawable bgN = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{ck ? 0xFF330000 : 0xFF1A0000, ck ? 0xFF1A0000 : 0xFF0D0000});
            bgN.setStroke(ck ? 2 : 1, ck ? 0xFFFF0000 : 0xFF330000); bgN.setCornerRadius(12);
            card.setBackground(bgN);
            t.setTextColor(ck ? 0xFFFF6644 : 0xFFFFFFFF);
        });
        row.addView(col); row.addView(sw);
        card.addView(row);
        return card;
    }

    private View buildEspeciales() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(12, 4, 12, 4);
        Button btnCalib = btn3D("🎯  CALIBRAR SENSIBILIDAD", 0xFF0066FF, 0xFF003388);
        btnCalib.setOnClickListener(v -> mostrarCalibracion());
        Button btnObtSensi = btn3D("✨  OBTENER SENSI PERSONALIZADA", 0xFF00CCAA, 0xFF005544);
        btnObtSensi.setOnClickListener(v -> mostrarObtenerSensi());
        Button btnCache = btn3D("🗑  LIMPIAR CACHE", 0xFFCCAA00, 0xFF665500);
        btnCache.setOnClickListener(v -> limpiarCache());
        Button btnFloat = btn3D("🪟  VENTANA FLOTANTE", 0xFF00AA00, 0xFF005500);
        btnFloat.setOnClickListener(v -> abrirVentanaFlotante());
        Button btnShizuku = btn3D("🔌  CONECTAR SHIZUKU", 0xFFAA00FF, 0xFF550088);
        btnShizuku.setOnClickListener(v -> conectarShizuku());
        box.addView(btnCalib); box.addView(btnObtSensi);
        box.addView(btnCache); box.addView(btnFloat); box.addView(btnShizuku);
        return box;
    }

    private View buildBotonesFF() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL); row.setPadding(12, 4, 12, 4);
        boolean tFF = appInst(PKG_FF), tFFm = appInst(PKG_FFMAX);
        Button bFF = btn3D(tFF ? "🔥 FREE FIRE" : "🔥 FF (no inst)", 0xFFFF6600, 0xFF883300);
        bFF.setOnClickListener(v -> abrirJuego(PKG_FF, "Free Fire"));
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, -2, 1f);
        lp1.setMargins(0, 0, 6, 0);
        Button bFFm = btn3D(tFFm ? "⚡ FF MAX" : "⚡ FF MAX (no inst)", 0xFFFFAA00, 0xFF885500);
        bFFm.setOnClickListener(v -> abrirJuego(PKG_FFMAX, "Free Fire MAX"));
        row.addView(bFF, lp1);
        row.addView(bFFm, new LinearLayout.LayoutParams(0, -2, 1f));
        return row;
    }

    private View buildAcciones() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL); box.setPadding(12, 12, 12, 8);
        btnAplicar = btn3D("🔴   APLICAR AUXILIO MIRA", 0xFFFF3300, 0xFF880000);
        btnAplicar.setTextSize(17);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, 8);
        btnAplicar.setOnClickListener(v -> pedirConfirmacion());
        Button btnADB = btn3D("📋   VER COMANDO ADB / SHIZUKU", 0xFF00AA00, 0xFF005500);
        btnADB.setOnClickListener(v -> mostrarADB());
        box.addView(btnAplicar, lp); box.addView(btnADB);
        return box;
    }

    private View buildFooter() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL); box.setPadding(20, 10, 20, 10);
        TextView w = new TextView(this);
        w.setText("⚠ Ajustes - Apps - AUXILIO MIRA - Bateria - Sin restricciones");
        w.setTextSize(10); w.setTextColor(0xFFFF8800); w.setGravity(Gravity.CENTER);
        TextView v = new TextView(this);
        v.setText("v5.0 | SENSIS GOOD FF studio | @sensisgoodffoficial");
        v.setTextSize(9); v.setTextColor(0xFF555555); v.setGravity(Gravity.CENTER);
        box.addView(w); box.addView(v);
        return box;
    }

    private void pedirConfirmacion() {
        int total = 0;
        StringBuilder sb = new StringBuilder();
        for (Modulo m : modulos) {
            if (m.activo) { total += m.comandos.length; sb.append("• ").append(m.titulo).append("\n"); }
        }
        if (total == 0) { Toast.makeText(this, "Activa al menos un modulo", Toast.LENGTH_SHORT).show(); return; }
        final int t = total;
        new AlertDialog.Builder(this)
            .setTitle("AUXILIO MIRA")
            .setMessage("Se aplicaran " + t + " comandos en lotes de 50 (rapido):\n\n" + sb)
            .setPositiveButton("✅ APLICAR", (d, w) -> iniciarInyeccion())
            .setNegativeButton("CANCELAR", null).show();
    }

    private void iniciarInyeccion() {
        cmdsPendientes.clear();
        StringBuilder mods = new StringBuilder();
        for (Modulo m : modulos) {
            if (m.activo) {
                for (String c : m.comandos) cmdsPendientes.add(c);
                mods.append(m.titulo).append(" ");
            }
        }
        btnAplicar.setEnabled(false);
        progressBar.setMax(cmdsPendientes.size());
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
        tvProgress.setVisibility(View.VISIBLE);
        tvStatus.setText("⚡ Aplicando " + cmdsPendientes.size() + " cmds en lotes de 50...");
        final int[] aplicados = {0};
        final String modsFinal = mods.toString();
        ShizukuHelper.aplicarComandosAsync(this, cmdsPendientes.toArray(new String[0]),
            new ShizukuHelper.ProgressCallback() {
                public void onProgress(int a, int t, String c) {
                    aplicados[0] = a;
                    progressBar.setProgress(a);
                    tvProgress.setText((a * 100 / t) + "%");
                    tvStatus.setText("⚡ " + a + " / " + t);
                }
                public void onComplete(int t) { onCompletado(aplicados[0], t, modsFinal); }
                public void onError(String c, String e) {}
            }, handler);
    }

    private void onCompletado(int aplicados, int total, String mods) {
        btnAplicar.setEnabled(true);
        if (aplicados == 0) {
            tvProgress.setText("❌ 0%");
            tvStatus.setText("❌ Sin permiso. Activa Shizuku");
            NotifHelper.notificarImportante(this, "❌ Error", "Ningun comando aplicado.");
        } else {
            tvProgress.setText("✅ 100%");
            tvStatus.setText("✅ " + aplicados + " comandos aplicados");
            NotifHelper.notificarImportante(this, "✅ Optimizacion aplicada",
                aplicados + "/" + total + " cmds\n" + mods + "\n\nReinicia Free Fire");
        }
        new AlertDialog.Builder(this)
            .setTitle("🎯 AUXILIO MIRA")
            .setMessage(aplicados + "/" + total + " comandos aplicados.\n\nReinicia Free Fire.")
            .setPositiveButton("🔥 FREE FIRE", (d, w) -> abrirJuego(PKG_FF, "Free Fire"))
            .setNeutralButton("⚡ FF MAX", (d, w) -> abrirJuego(PKG_FFMAX, "Free Fire MAX"))
            .setNegativeButton("CERRAR", null).show();
    }

    private void mostrarCalibracion() {
        tvStatus.setText("🔍 Analizando...");
        handler.postDelayed(() -> {
            SensibilidadCalibrator.ResultadoCalib r = SensibilidadCalibrator.calibrar(this);
            new AlertDialog.Builder(this)
                .setTitle("🎯 Calibracion Base")
                .setMessage(r.resumen)
                .setPositiveButton("APLICAR", (d, w) ->
                    ShizukuHelper.aplicarComandosAsync(this, r.comandos.toArray(new String[0]),
                        new ShizukuHelper.ProgressCallback() {
                            public void onProgress(int a, int t, String c) { tvStatus.setText("🎯 " + a + "/" + t); }
                            public void onComplete(int t) {
                                tvStatus.setText("✅ " + r.perfil);
                                NotifHelper.notificarImportante(MainActivity.this, "🎯 Calibracion", r.perfil);
                            }
                            public void onError(String c, String e) {}
                        }, handler))
                .setNegativeButton("CANCELAR", null).show();
            tvStatus.setText("⚡ Listo");
        }, 600);
    }

    /**
     * NUEVA OPCIÓN: Obtener sensibilidad personalizada con preguntas al usuario.
     */
    private void mostrarObtenerSensi() {
        // Pregunta 1: Nivel de sensibilidad
        String[] niveles = {"BAJA (precision)", "MEDIA (equilibrio)", "ALTA (rapida)", "MUY ALTA (extrema)"};
        new AlertDialog.Builder(this)
            .setTitle("✨ Obtener Sensi Personalizada")
            .setSingleChoiceItems(niveles, 1, null)
            .setPositiveButton("SIGUIENTE", (d, w) -> {
                int nivel = ((AlertDialog) d).getListView().getCheckedItemPosition() + 1;
                preguntarDpi(nivel);
            })
            .setNegativeButton("CANCELAR", null).show();
    }

    private void preguntarDpi(int nivel) {
        new AlertDialog.Builder(this)
            .setTitle("¿Ajustar DPI?")
            .setMessage("¿Quieres que tambien se ajuste el DPI del dispositivo?\n\n(Recomendado SI para mejor experiencia)")
            .setPositiveButton("SI", (d, w) -> preguntarDpiCustom(nivel))
            .setNegativeButton("NO", (d, w) -> generarSensi(nivel, false, 0))
            .setNeutralButton("CANCELAR", null).show();
    }

    private void preguntarDpiCustom(int nivel) {
        EditText et = new EditText(this);
        et.setHint("420");
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this)
            .setTitle("DPI personalizado")
            .setMessage("Ingresa el DPI deseado (320-560 recomendado).\nDeja vacio para usar el del dispositivo.")
            .setView(et)
            .setPositiveButton("APLICAR", (d, w) -> {
                String s = et.getText().toString().trim();
                int dpi = 0;
                try { dpi = s.isEmpty() ? 0 : Integer.parseInt(s); } catch (Exception e) {}
                if (dpi != 0 && (dpi < 200 || dpi > 700)) {
                    Toast.makeText(this, "DPI fuera de rango seguro", Toast.LENGTH_SHORT).show();
                    return;
                }
                generarSensi(nivel, true, dpi);
            })
            .setNegativeButton("CANCELAR", null).show();
    }

    private void generarSensi(int nivel, boolean conDpi, int dpiCustom) {
        SensibilidadCalibrator.ResultadoCalib r = SensibilidadCalibrator.calibrarPersonalizada(this, nivel, conDpi, dpiCustom);
        new AlertDialog.Builder(this)
            .setTitle("✨ Tu Sensi Personalizada")
            .setMessage(r.resumen)
            .setPositiveButton("APLICAR AJUSTES", (d, w) -> {
                tvStatus.setText("⚡ Aplicando sensi...");
                ShizukuHelper.aplicarComandosAsync(this, r.comandos.toArray(new String[0]),
                    new ShizukuHelper.ProgressCallback() {
                        public void onProgress(int a, int t, String c) { tvStatus.setText("⚡ " + a + "/" + t); }
                        public void onComplete(int t) {
                            tvStatus.setText("✅ " + r.perfil);
                            NotifHelper.notificarImportante(MainActivity.this,
                                "✨ Sensi aplicada", r.perfil + "\nGeneral: " + r.sensiGeneral);
                        }
                        public void onError(String c, String e) {}
                    }, handler);
            })
            .setNeutralButton("VOLVER", (d, w) -> mostrarObtenerSensi())
            .setNegativeButton("CERRAR", null).show();
    }

    private void limpiarCache() {
        new AlertDialog.Builder(this).setTitle("🗑 Limpiar Cache").setMessage("Limpiar?")
            .setPositiveButton("LIMPIAR", (d, w) -> {
                tvStatus.setText("🗑 Limpiando...");
                handler.postDelayed(() -> {
                    ShizukuHelper.limpiarCache(this);
                    tvStatus.setText("✅ Cache limpiado");
                    NotifHelper.notificar(this, "🗑 Cache limpiado", "Sistema liberado");
                }, 400);
            }).setNegativeButton("CANCELAR", null).show();
    }

    private void abrirVentanaFlotante() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())), 1001);
            return;
        }
        try { startService(new Intent(this, FloatingWindowService.class)); }
        catch (Exception e) { Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show(); }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 1001 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this))
            abrirVentanaFlotante();
    }

    private void conectarShizuku() {
        String diag = ShizukuHelper.estadoPermisoDetallado(this);
        new AlertDialog.Builder(this).setTitle("🔍 Estado Shizuku").setMessage(diag)
            .setPositiveButton("ABRIR SHIZUKU", (d, w) -> ShizukuHelper.abrirShizuku(this))
            .setNegativeButton("OK", null).show();
        NotifHelper.notificarImportante(this, "🔍 Diagnostico", diag);
    }

    private void abrirJuego(String pkg, String nombre) {
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i != null) { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); }
        else new AlertDialog.Builder(this).setTitle(nombre + " no instalado")
            .setMessage("Instalar?")
            .setPositiveButton("INSTALAR", (d, w) -> {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg))); }
                catch (Exception e) { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + pkg))); }
            }).setNegativeButton("CANCELAR", null).show();
    }

    private void mostrarADB() {
        new AlertDialog.Builder(this).setTitle("📋 Shizuku / ADB")
            .setMessage("SHIZUKU:\n1. Instala Shizuku\n2. Activalo\n3. Autoriza esta app\n\nADB:\nadb shell pm grant com.auxiliomira.app android.permission.WRITE_SECURE_SETTINGS")
            .setPositiveButton("OK", null).show();
    }

    private TextView secTit(String t) {
        TextView v = new TextView(this); v.setText(t);
        v.setTextSize(12); v.setTypeface(null, Typeface.BOLD);
        v.setTextColor(0xFFFF6600); v.setShadowLayer(6f, 0, 0, 0xFFFF0000);
        return v;
    }

    private Button btn3D(String t, int top, int bot) {
        Button b = new Button(this); b.setText(t); b.setTextSize(14);
        b.setTextColor(0xFFFFFFFF); b.setTypeface(null, Typeface.BOLD);
        b.setShadowLayer(3f, 1, 1, 0x80000000);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{top, bot});
        bg.setCornerRadius(12); bg.setStroke(2, 0x60FFFFFF);
        b.setBackground(bg); b.setElevation(6f); b.setPadding(0, 18, 0, 18);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 5, 0, 5); b.setLayoutParams(lp);
        return b;
    }

    private TextView secTitulo(String t) {
        TextView v = new TextView(this); v.setText(t);
        v.setTextSize(12); v.setTypeface(null, Typeface.BOLD);
        v.setTextColor(0xFFFF6600); v.setShadowLayer(6f, 0, 0, 0xFFFF0000);
        v.setPadding(20, 18, 20, 4);
        return v;
    }
}
