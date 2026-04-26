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
        modulos.add(new Modulo("Velocidad del Puntero",   "Optimiza touch y velocidad del dedo",  AllCommands.CMDS_TOUCH_POINTER));
        modulos.add(new Modulo("Giroscopio Pro",           "Calibra giroscopio y sensores",        AllCommands.CMDS_GYRO_SENSOR));
        modulos.add(new Modulo("FPS y Pantalla Max",       "Maximiza FPS y GPU",                   AllCommands.CMDS_FPS_PANTALLA));
        modulos.add(new Modulo("Rendimiento CPU y RAM",    "Libera RAM y optimiza CPU",            AllCommands.CMDS_RENDIMIENTO_CPU));
        modulos.add(new Modulo("Red y Anti-Lag",           "Reduce ping y latencia",               AllCommands.CMDS_RED_LATENCIA));
        modulos.add(new Modulo("Talkback OFF Total",       "Desactiva accesibilidad",              AllCommands.CMDS_TALKBACK));
        modulos.add(new Modulo("Anti-Thermal Gaming",      "Evita throttling termico",             AllCommands.CMDS_BATERIA));
        modulos.add(new Modulo("Audio Sin Vibracion",      "Optimiza audio",                       AllCommands.CMDS_AUDIO));
        modulos.add(new Modulo("Modificacion Media",       "Foco azul + velocidad extrema",        AllCommands.CMDS_MODS_MEDIA));
        modulos.add(new Modulo("Scroll Pro",               "Scroll optimizado en HUD",             AllCommands.CMDS_SCROLL));
        modulos.add(new Modulo("Anti-Recoil",              "Detecta y compensa recoil",            AllCommands.CMDS_ANTIRECOIL));
        buildUI();
    }

    private void buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF0A0A0A);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, 0, 0, 80);

        root.addView(buildHeader());
        root.addView(buildEstadoApps());
        root.addView(buildStatusBox());
        root.addView(secTitulo("  MODULOS DE OPTIMIZACION"));
        for (Modulo mod : modulos) root.addView(buildModuloCard(mod));
        root.addView(secTitulo("  HERRAMIENTAS"));
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
        h.setPadding(24, 48, 24, 20);
        h.setBackgroundColor(0xFF120000);
        TextView logo = tv("AUXILIO MIRA", 28, 0xFFFF0000, true);
        logo.setGravity(Gravity.CENTER);
        TextView sub = tv("FREE FIRE SYSTEM OPTIMIZER", 10, 0xFFFF4444, false);
        sub.setGravity(Gravity.CENTER);
        TextView total = tv("1967 COMANDOS - 11 MODULOS", 10, 0xFFFF6600, true);
        total.setGravity(Gravity.CENTER);
        total.setPadding(0,8,0,0);
        h.addView(logo); h.addView(sub); h.addView(total);
        return h;
    }

    private View buildEstadoApps() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(16, 10, 16, 6);
        box.setBackgroundColor(0xFF0D0D0D);

        TextView titulo = tv("  ESTADO DE APPS", 10, 0xFFFF6600, true);
        titulo.setPadding(8, 0, 0, 6);
        box.addView(titulo);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        boolean tieneShizuku = appInstalada(PKG_SHIZUKU);
        boolean tieneFF      = appInstalada(PKG_FF);
        boolean tieneFFMax   = appInstalada(PKG_FFMAX);
        boolean tienePermiso = ShizukuHelper.tienePermiso(this);

        row.addView(badgeApp("Shizuku", tieneShizuku, tienePermiso ? "Con permiso" : (tieneShizuku ? "Sin permiso" : "No instalado")));
        row.addView(badgeApp("Free Fire", tieneFF, tieneFF ? "Instalado" : "No instalado"));
        row.addView(badgeApp("FF MAX", tieneFFMax, tieneFFMax ? "Instalado" : "No instalado"));

        box.addView(row);

        TextView tvRestr = tv("Para quitar restriccion: Ajustes - Apps - AUXILIO MIRA - Bateria - Sin restricciones", 9, 0xFFFFAA00, false);
        tvRestr.setPadding(8, 6, 8, 0);
        box.addView(tvRestr);

        return box;
    }

    private View badgeApp(String nombre, boolean instalada, String estado) {
        LinearLayout badge = new LinearLayout(this);
        badge.setOrientation(LinearLayout.VERTICAL);
        badge.setGravity(Gravity.CENTER);
        badge.setBackgroundColor(instalada ? 0xFF001500 : 0xFF150000);
        badge.setPadding(10, 8, 10, 8);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(3, 0, 3, 0);
        badge.setLayoutParams(lp);
        TextView tvN = tv(nombre, 9, 0xFFFFFFFF, true);
        tvN.setGravity(Gravity.CENTER);
        TextView tvE = tv(estado, 8, instalada ? 0xFF00FF00 : 0xFFFF4444, false);
        tvE.setGravity(Gravity.CENTER);
        badge.addView(tvN); badge.addView(tvE);
        return badge;
    }

    private boolean appInstalada(String pkg) {
        try { getPackageManager().getPackageInfo(pkg, 0); return true; }
        catch (PackageManager.NameNotFoundException e) { return false; }
    }

    private View buildStatusBox() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(24,12,24,12);
        box.setBackgroundColor(0xFF0A0A0A);
        tvStatus = tv("Activa modulos y toca APLICAR", 12, 0xFFAAAAAA, false);
        tvStatus.setGravity(Gravity.CENTER);
        tvProgress = tv("0%", 20, 0xFFFF0000, true);
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
        card.setPadding(16,12,16,12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(12,4,12,4);
        card.setLayoutParams(lp);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1f));
        TextView titulo = tv(mod.titulo, 13, 0xFFFFFFFF, true);
        TextView desc   = tv(mod.descripcion, 10, 0xFF888888, false);
        TextView count  = tv(mod.comandos.length+" cmds", 9, 0xFFFF4400, false);
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
        box.setPadding(12,4,12,4);
        Button btnCalib = btn("CALIBRAR SENSIBILIDAD", 0xFF001A33, 0xFF00AAFF);
        btnCalib.setOnClickListener(v -> mostrarCalibracion());
        Button btnCache = btn("LIMPIAR CACHE", 0xFF1A1A00, 0xFFFFFF00);
        btnCache.setOnClickListener(v -> limpiarCache());
        Button btnFloat = btn("VENTANA FLOTANTE", 0xFF001A00, 0xFF00FF00);
        btnFloat.setOnClickListener(v -> abrirVentanaFlotante());
        Button btnShizuku = btn("CONECTAR SHIZUKU", 0xFF0A001A, 0xFFCC88FF);
        btnShizuku.setOnClickListener(v -> conectarShizuku());
        box.addView(btnCalib); box.addView(btnCache); box.addView(btnFloat); box.addView(btnShizuku);
        return box;
    }

    private View buildBotonesFF() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(12,4,12,4);

        boolean tieneFF    = appInstalada(PKG_FF);
        boolean tieneFFMax = appInstalada(PKG_FFMAX);

        Button btnFF = btn(tieneFF ? "FREE FIRE" : "FF (no instalado)", 0xFF1A0000, tieneFF ? 0xFFFF4400 : 0xFF666666);
        btnFF.setOnClickListener(v -> abrirJuego(PKG_FF, "Free Fire"));
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0,-2,1f);
        lp1.setMargins(0,0,6,0);

        Button btnFFMax = btn(tieneFFMax ? "FF MAX" : "FF MAX (no instalado)", 0xFF1A0A00, tieneFFMax ? 0xFFFF8800 : 0xFF666666);
        btnFFMax.setOnClickListener(v -> abrirJuego(PKG_FFMAX, "Free Fire MAX"));

        row.addView(btnFF, lp1);
        row.addView(btnFFMax, new LinearLayout.LayoutParams(0,-2,1f));
        return row;
    }

    private View buildAcciones() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(12,8,12,8);
        btnAplicar = btn("APLICAR AUXILIO MIRA", 0xFFCC0000, 0xFFFFFFFF);
        btnAplicar.setTextSize(16);
        btnAplicar.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,0,0,8);
        btnAplicar.setOnClickListener(v -> pedirConfirmacion());
        Button btnADB = btn("VER COMANDO ADB / SHIZUKU", 0xFF001A00, 0xFF00FF00);
        btnADB.setOnClickListener(v -> mostrarADB());
        box.addView(btnAplicar, lp);
        box.addView(btnADB);
        return box;
    }

    private View buildFooter() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(20,6,20,6);
        TextView warn = tv("Ajustes - Apps - AUXILIO MIRA - Bateria - Sin restricciones", 9, 0xFFFF8800, false);
        warn.setGravity(Gravity.CENTER);
        TextView ver = tv("v4.0 - SENSIS GOOD FF studio - @sensisgoodffoficial", 8, 0xFF444444, false);
        ver.setGravity(Gravity.CENTER);
        box.addView(warn); box.addView(ver);
        return box;
    }

    private void pedirConfirmacion() {
        int total = 0;
        StringBuilder sb = new StringBuilder();
        for (Modulo m : modulos) {
            if (m.activo) { total += m.comandos.length; sb.append("- ").append(m.titulo).append("\n"); }
        }
        if (total == 0) { Toast.makeText(this,"Activa al menos un modulo",Toast.LENGTH_SHORT).show(); return; }
        final int t = total;
        new AlertDialog.Builder(this)
            .setTitle("AUXILIO MIRA")
            .setMessage("Se aplicaran "+t+" comandos:\n\n"+sb)
            .setPositiveButton("APLICAR", (d,w) -> iniciarInyeccion())
            .setNegativeButton("CANCELAR", null).show();
    }

    private void iniciarInyeccion() {
        if (!ShizukuHelper.tienePermiso(this) && !ShizukuHelper.tieneRoot() && !ShizukuHelper.shizukuActivo(this)) {
            new AlertDialog.Builder(this)
                .setTitle("Sin permiso")
                .setMessage("Necesitas Shizuku o ADB.")
                .setPositiveButton("ABRIR SHIZUKU", (d,w) -> conectarShizuku())
                .setNeutralButton("VER ADB", (d,w) -> mostrarADB())
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
        tvStatus.setText("Aplicando "+cmdsPendientes.size()+" comandos...");
        ShizukuHelper.aplicarComandosAsync(this, cmdsPendientes.toArray(new String[0]),
            new ShizukuHelper.ProgressCallback() {
                public void onProgress(int a, int t, String c) {
                    progressBar.setProgress(a);
                    tvProgress.setText((a*100/t)+"%");
                    tvStatus.setText(a+" / "+t);
                }
                public void onComplete(int t) { onCompletado(t); }
                public void onError(String c, String e) {}
            }, handler);
    }

    private void onCompletado(int total) {
        btnAplicar.setEnabled(true);
        tvProgress.setText("100%");
        tvStatus.setText(total+" comandos aplicados.");
        new AlertDialog.Builder(this)
            .setTitle("AUXILIO MIRA")
            .setMessage("Completado! "+total+" configuraciones aplicadas.\n\nReinicia Free Fire.")
            .setPositiveButton("FREE FIRE", (d,w) -> abrirJuego(PKG_FF,"Free Fire"))
            .setNeutralButton("FF MAX", (d,w) -> abrirJuego(PKG_FFMAX,"Free Fire MAX"))
            .setNegativeButton("CERRAR", null).show();
    }

    private void mostrarCalibracion() {
        tvStatus.setText("Analizando...");
        handler.postDelayed(() -> {
            SensibilidadCalibrator.ResultadoCalib r = SensibilidadCalibrator.calibrar(this);
            new AlertDialog.Builder(this)
                .setTitle("Calibracion")
                .setMessage(r.resumen)
                .setPositiveButton("APLICAR", (d,w) ->
                    ShizukuHelper.aplicarComandosAsync(this, r.comandos.toArray(new String[0]),
                        new ShizukuHelper.ProgressCallback() {
                            public void onProgress(int a, int t, String c) { tvStatus.setText(a+"/"+t); }
                            public void onComplete(int t) { tvStatus.setText("Calibracion aplicada"); }
                            public void onError(String c, String e) {}
                        }, handler))
                .setNegativeButton("CANCELAR", null).show();
            tvStatus.setText("Listo");
        }, 600);
    }

    private void limpiarCache() {
        new AlertDialog.Builder(this)
            .setTitle("Limpiar Cache")
            .setMessage("Limpiar cache del sistema?")
            .setPositiveButton("LIMPIAR", (d,w) -> {
                tvStatus.setText("Limpiando...");
                handler.postDelayed(() -> {
                    ShizukuHelper.limpiarCache(this);
                    tvStatus.setText("Cache limpiado");
                    Toast.makeText(this,"Cache limpiado",Toast.LENGTH_SHORT).show();
                }, 400);
            })
            .setNegativeButton("CANCELAR", null).show();
    }

    private void abrirVentanaFlotante() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:"+getPackageName())), 1001);
            Toast.makeText(this,"Activa Mostrar sobre otras apps",Toast.LENGTH_LONG).show();
            return;
        }
        try {
            startService(new Intent(this, FloatingWindowService.class));
        } catch (Exception e) {
            Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 1001 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            abrirVentanaFlotante();
        }
    }

    private void conectarShizuku() {
        if (!appInstalada(PKG_SHIZUKU)) {
            new AlertDialog.Builder(this)
                .setTitle("Shizuku no instalado")
                .setMessage("Instalarlo desde Play Store?")
                .setPositiveButton("INSTALAR", (d,w) -> ShizukuHelper.abrirShizuku(this))
                .setNegativeButton("CANCELAR", null).show();
            return;
        }
        if (ShizukuHelper.tienePermiso(this)) {
            new AlertDialog.Builder(this)
                .setTitle("Shizuku activo")
                .setMessage("Ya tienes permiso! Puedes inyectar sin PC.")
                .setPositiveButton("OK", null).show();
        } else {
            new AlertDialog.Builder(this)
                .setTitle("Shizuku sin permiso")
                .setMessage("Abre Shizuku y autoriza esta app.")
                .setPositiveButton("ABRIR SHIZUKU", (d,w) -> ShizukuHelper.abrirShizuku(this))
                .setNegativeButton("OK", null).show();
        }
    }

    private void abrirJuego(String pkg, String nombre) {
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i != null) { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); }
        else new AlertDialog.Builder(this)
            .setTitle(nombre+" no instalado")
            .setMessage("Instalar desde Play Store?")
            .setPositiveButton("INSTALAR", (d,w) -> {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+pkg))); }
                catch (Exception e) { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id="+pkg))); }
            })
            .setNegativeButton("CANCELAR", null).show();
    }

    private void mostrarADB() {
        new AlertDialog.Builder(this)
            .setTitle("Shizuku / ADB")
            .setMessage("SHIZUKU: instalalo y autoriza esta app.\n\nADB:\nadb shell pm grant com.auxiliomira.app android.permission.WRITE_SECURE_SETTINGS")
            .setPositiveButton("OK", null).show();
    }

    private TextView tv(String t, float s, int c, boolean bold) {
        TextView v = new TextView(this); v.setText(t); v.setTextSize(s); v.setTextColor(c);
        if (bold) v.setTypeface(null, Typeface.BOLD);
        return v;
    }
    private Button btn(String t, int bg, int fg) {
        Button b = new Button(this); b.setText(t); b.setTextSize(13); b.setTextColor(fg); b.setBackgroundColor(bg);
        b.setPadding(0,16,0,16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,4,0,4); b.setLayoutParams(lp);
        return b;
    }
    private TextView secTitulo(String t) {
        TextView v = tv(t, 10, 0xFFFF6600, true); v.setPadding(20,14,20,4); return v;
    }
}
