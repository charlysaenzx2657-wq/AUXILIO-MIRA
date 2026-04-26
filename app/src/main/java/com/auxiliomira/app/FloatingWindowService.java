package com.auxiliomira.app;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class FloatingWindowService extends Service {

    private static final String TAG = "FloatingService";
    private static final String CHANNEL_ID = "auxilio_float";

    private WindowManager wm;
    private View floatingRoot;
    private LinearLayout bubble;
    private LinearLayout panel;
    private TextView tvStatus;
    private ProgressBar progressLoad;
    private ImageView ivIcon;
    private boolean expandido = false;
    private WindowManager.LayoutParams params;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private CheckBox cbTouch, cbGyro, cbFps, cbCpu, cbRed, cbTalk, cbBat, cbAudio, cbMedia, cbScroll, cbRecoil;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            crearNotificacion();
            crearVentana();
            iniciarPulsoBurbuja();
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        return START_STICKY;
    }

    private void crearNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "AUXILIO MIRA", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(ch);
        }
        Intent stopIntent = new Intent(this, FloatingWindowService.class);
        stopIntent.setAction("STOP");
        int piFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent pi = PendingIntent.getService(this, 0, stopIntent, piFlags);
        Notification n;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            n = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("AUXILIO MIRA flotante")
                .setContentText("Activo")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setColor(0xFFFF0000)
                .setColorized(true)
                .addAction(android.R.drawable.ic_delete, "Cerrar", pi)
                .build();
        } else {
            n = new Notification.Builder(this)
                .setContentTitle("AUXILIO MIRA flotante")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        }
        startForeground(1, n);
    }

    private void crearVentana() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics dm = getResources().getDisplayMetrics();

        // ── BURBUJA con efecto 3D ──
        int bSize = (int)(dm.density * 54);
        bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setGravity(Gravity.CENTER);
        bubble.setPadding(0, 0, 0, 0);

        // Gradient radial estilo 3D
        GradientDrawable bgB = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{0xFFFF3300, 0xFFCC0000, 0xFF660000});
        bgB.setShape(GradientDrawable.OVAL);
        bgB.setStroke(4, 0xFFFFFFFF);
        bubble.setBackground(bgB);
        bubble.setElevation(12f);

        ivIcon = new ImageView(this);
        ivIcon.setImageResource(R.mipmap.ic_launcher);
        int iconSize = (int)(dm.density * 36);
        LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(iconSize, iconSize);
        ivIcon.setLayoutParams(ivLp);
        ivIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        bubble.addView(ivIcon);

        LinearLayout.LayoutParams bubbleLp = new LinearLayout.LayoutParams(bSize, bSize);
        bubble.setLayoutParams(bubbleLp);

        // ── PANEL con efecto 3D y bordes redondeados ──
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setVisibility(View.GONE);
        GradientDrawable bgP = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xF0150000, 0xF0050000, 0xF0000000});
        bgP.setStroke(3, 0xFFFF0000);
        bgP.setCornerRadius(20);
        panel.setBackground(bgP);
        panel.setPadding(12, 12, 12, 12);
        panel.setElevation(20f);

        // ── Header con efecto neon ──
        LinearLayout headerBar = new LinearLayout(this);
        headerBar.setOrientation(LinearLayout.HORIZONTAL);
        headerBar.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable bgHdr = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{0xFF330000, 0xFF660000, 0xFF330000});
        bgHdr.setCornerRadius(8);
        headerBar.setBackground(bgHdr);
        headerBar.setPadding(10, 6, 10, 6);

        TextView tvT = new TextView(this);
        tvT.setText("◉ AUXILIO MIRA");
        tvT.setTextSize(15);
        tvT.setTypeface(null, Typeface.BOLD);
        tvT.setTextColor(0xFFFFFFFF);
        tvT.setShadowLayer(8f, 0, 0, 0xFFFF0000);
        tvT.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, -2, 1f);
        tvT.setLayoutParams(tlp);
        headerBar.addView(tvT);
        panel.addView(headerBar);

        // ── Status bar con borde glow ──
        LinearLayout statusBox = new LinearLayout(this);
        statusBox.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bgSt = new GradientDrawable();
        bgSt.setColor(0xFF1A0000);
        bgSt.setStroke(1, 0xFFFF4400);
        bgSt.setCornerRadius(6);
        statusBox.setBackground(bgSt);
        statusBox.setPadding(8, 6, 8, 6);
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(-1, -2);
        stLp.setMargins(0, 6, 0, 4);
        statusBox.setLayoutParams(stLp);

        tvStatus = new TextView(this);
        tvStatus.setText("⚡ Selecciona modulos");
        tvStatus.setTextSize(11);
        tvStatus.setTypeface(null, Typeface.BOLD);
        tvStatus.setTextColor(0xFFFFCC00);
        tvStatus.setGravity(Gravity.CENTER);
        statusBox.addView(tvStatus);

        progressLoad = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressLoad.setMax(100);
        progressLoad.setProgress(0);
        progressLoad.setVisibility(View.GONE);
        progressLoad.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFFF0000));
        statusBox.addView(progressLoad);
        panel.addView(statusBox);

        // ── 2 columnas ──
        LinearLayout twoCols = new LinearLayout(this);
        twoCols.setOrientation(LinearLayout.HORIZONTAL);

        // Col izquierda
        ScrollView svLeft = new ScrollView(this);
        LinearLayout colLeft = new LinearLayout(this);
        colLeft.setOrientation(LinearLayout.VERTICAL);
        colLeft.setPadding(6, 6, 6, 6);

        TextView tvSel = secTit("MODULOS");
        colLeft.addView(tvSel);

        cbTouch  = cb("👆 Puntero", AllCommands.CMDS_TOUCH_POINTER.length);
        cbGyro   = cb("🌀 Giro", AllCommands.CMDS_GYRO_SENSOR.length);
        cbFps    = cb("🖥️ FPS Max", AllCommands.CMDS_FPS_PANTALLA.length);
        cbCpu    = cb("⚡ CPU/RAM", AllCommands.CMDS_RENDIMIENTO_CPU.length);
        cbRed    = cb("🌐 Anti-Lag", AllCommands.CMDS_RED_LATENCIA.length);
        cbTalk   = cb("♿ Talk OFF", AllCommands.CMDS_TALKBACK.length);
        cbBat    = cb("🔋 Anti-Therm", AllCommands.CMDS_BATERIA.length);
        cbAudio  = cb("🔊 Audio", AllCommands.CMDS_AUDIO.length);
        cbMedia  = cb("🔵 Media", AllCommands.CMDS_MODS_MEDIA.length);
        cbScroll = cb("📜 Scroll", AllCommands.CMDS_SCROLL.length);
        cbRecoil = cb("🎯 Anti-Recoil", AllCommands.CMDS_ANTIRECOIL.length);

        colLeft.addView(cbTouch); colLeft.addView(cbGyro); colLeft.addView(cbFps);
        colLeft.addView(cbCpu); colLeft.addView(cbRed); colLeft.addView(cbTalk);
        colLeft.addView(cbBat); colLeft.addView(cbAudio); colLeft.addView(cbMedia);
        colLeft.addView(cbScroll); colLeft.addView(cbRecoil);

        svLeft.addView(colLeft);
        LinearLayout.LayoutParams lpLeft = new LinearLayout.LayoutParams(0, -1, 1.3f);
        lpLeft.setMargins(0, 0, 6, 0);
        svLeft.setLayoutParams(lpLeft);

        // Col derecha
        ScrollView svRight = new ScrollView(this);
        LinearLayout colRight = new LinearLayout(this);
        colRight.setOrientation(LinearLayout.VERTICAL);
        colRight.setPadding(6, 6, 6, 6);

        TextView tvAcc = secTit("ACCIONES");
        colRight.addView(tvAcc);

        Button btnApl = btn3D("APLICAR", 0xFFFF3300, 0xFFCC0000, 0xFFFFFFFF);
        btnApl.setOnClickListener(v -> aplicarSeleccionados());
        colRight.addView(btnApl);

        Button btnTodo = btn3D("⚡ TODO", 0xFFFF6600, 0xFF883300, 0xFFFFFFFF);
        btnTodo.setOnClickListener(v -> aplicarTodo());
        colRight.addView(btnTodo);

        colRight.addView(sep());

        Button btnCal = btn3D("🎯 Calibrar", 0xFF0066FF, 0xFF003388, 0xFFFFFFFF);
        btnCal.setOnClickListener(v -> calibrar());
        colRight.addView(btnCal);

        Button btnCache = btn3D("🗑 Cache", 0xFFCCAA00, 0xFF665500, 0xFFFFFFFF);
        btnCache.setOnClickListener(v -> {
            actualizarStatus("🗑 Limpiando cache...", 0xFFFFCC00);
            ShizukuHelper.limpiarCache(this);
            actualizarStatus("✅ Cache limpiado", 0xFF00FF00);
            NotifHelper.notificar(this, "🗑 Cache limpiado", "Sistema liberado de archivos temporales");
        });
        colRight.addView(btnCache);

        Button btnPerm = btn3D("🔍 Diagnostico", 0xFFAA00FF, 0xFF550088, 0xFFFFFFFF);
        btnPerm.setOnClickListener(v -> verificarPermiso());
        colRight.addView(btnPerm);

        colRight.addView(sep());

        Button btnFF = btn3D("🔥 Free Fire", 0xFFFF6600, 0xFF884400, 0xFFFFFFFF);
        btnFF.setOnClickListener(v -> abrirJuego("com.dts.freefireth", "Free Fire"));
        colRight.addView(btnFF);

        Button btnFFM = btn3D("⚡ FF MAX", 0xFFFFAA00, 0xFF885500, 0xFFFFFFFF);
        btnFFM.setOnClickListener(v -> abrirJuego("com.dts.freefiremax", "Free Fire MAX"));
        colRight.addView(btnFFM);

        colRight.addView(sep());

        Button btnClose = btn3D("✕ Cerrar", 0xFF666666, 0xFF222222, 0xFFFFFFFF);
        btnClose.setOnClickListener(v -> stopSelf());
        colRight.addView(btnClose);

        svRight.addView(colRight);
        LinearLayout.LayoutParams lpRight = new LinearLayout.LayoutParams(0, -1, 1f);
        svRight.setLayoutParams(lpRight);

        twoCols.addView(svLeft);
        twoCols.addView(svRight);

        int panelH = (int)(dm.heightPixels * 0.48f);
        twoCols.setLayoutParams(new LinearLayout.LayoutParams(-1, panelH));
        panel.addView(twoCols);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(bubble);
        root.addView(panel);
        floatingRoot = root;

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
            (int)(dm.widthPixels * 0.82f),
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 20;
        params.y = 150;

        bubble.setOnTouchListener(new View.OnTouchListener() {
            int ix, iy; float tx, ty; long down; boolean moved;
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        ix = params.x; iy = params.y;
                        tx = e.getRawX(); ty = e.getRawY();
                        down = System.currentTimeMillis(); moved = false;
                        ivIcon.setScaleX(0.85f); ivIcon.setScaleY(0.85f);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int)(e.getRawX()-tx), dy = (int)(e.getRawY()-ty);
                        if (Math.abs(dx)>8||Math.abs(dy)>8) moved = true;
                        params.x = ix+dx; params.y = iy+dy;
                        try { wm.updateViewLayout(floatingRoot, params); } catch (Exception ex){}
                        return true;
                    case MotionEvent.ACTION_UP:
                        ivIcon.setScaleX(1f); ivIcon.setScaleY(1f);
                        if (!moved && System.currentTimeMillis()-down < 350) togglePanel();
                        return true;
                }
                return false;
            }
        });

        wm.addView(floatingRoot, params);
    }

    private void iniciarPulsoBurbuja() {
        // Animación de pulso continuo en la burbuja
        ObjectAnimator pulso = ObjectAnimator.ofFloat(ivIcon, "alpha", 1f, 0.7f, 1f);
        pulso.setDuration(1800);
        pulso.setRepeatCount(ValueAnimator.INFINITE);
        pulso.setInterpolator(new LinearInterpolator());
        pulso.start();
    }

    private void togglePanel() {
        expandido = !expandido;
        params.flags = expandido
            ? WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            : WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        try { wm.updateViewLayout(floatingRoot, params); } catch (Exception e){}
        panel.setVisibility(expandido ? View.VISIBLE : View.GONE);
        // Animación de aparición
        if (expandido) {
            panel.setAlpha(0f);
            panel.animate().alpha(1f).setDuration(250).start();
            panel.setScaleY(0.7f);
            panel.animate().scaleY(1f).setDuration(280).start();
        }
    }

    private void actualizarStatus(String msg, int color) {
        uiHandler.post(() -> {
            if (tvStatus != null) {
                tvStatus.setText(msg);
                tvStatus.setTextColor(color);
            }
        });
    }

    private void mostrarProgreso(boolean visible) {
        uiHandler.post(() -> {
            if (progressLoad != null) progressLoad.setVisibility(visible ? View.VISIBLE : View.GONE);
        });
    }

    private void setProgreso(int actual, int total) {
        uiHandler.post(() -> {
            if (progressLoad != null) {
                progressLoad.setMax(total);
                progressLoad.setProgress(actual);
            }
        });
    }

    private void verificarPermiso() {
        String diag = ShizukuHelper.estadoPermisoDetallado(this);
        actualizarStatus("🔍 Ver notificacion para detalle", 0xFFCC88FF);
        NotifHelper.notificarImportante(this, "🔍 Diagnostico Permisos", diag);
        Toast.makeText(this, "Diagnostico enviado a notificaciones", Toast.LENGTH_SHORT).show();
    }

    private void aplicarSeleccionados() {
        List<String> lista = new ArrayList<>();
        StringBuilder modulosSel = new StringBuilder();
        if (cbTouch.isChecked())  { for (String c:AllCommands.CMDS_TOUCH_POINTER) lista.add(c); modulosSel.append("Puntero "); }
        if (cbGyro.isChecked())   { for (String c:AllCommands.CMDS_GYRO_SENSOR) lista.add(c); modulosSel.append("Giro "); }
        if (cbFps.isChecked())    { for (String c:AllCommands.CMDS_FPS_PANTALLA) lista.add(c); modulosSel.append("FPS "); }
        if (cbCpu.isChecked())    { for (String c:AllCommands.CMDS_RENDIMIENTO_CPU) lista.add(c); modulosSel.append("CPU "); }
        if (cbRed.isChecked())    { for (String c:AllCommands.CMDS_RED_LATENCIA) lista.add(c); modulosSel.append("Red "); }
        if (cbTalk.isChecked())   { for (String c:AllCommands.CMDS_TALKBACK) lista.add(c); modulosSel.append("Talk "); }
        if (cbBat.isChecked())    { for (String c:AllCommands.CMDS_BATERIA) lista.add(c); modulosSel.append("Bat "); }
        if (cbAudio.isChecked())  { for (String c:AllCommands.CMDS_AUDIO) lista.add(c); modulosSel.append("Audio "); }
        if (cbMedia.isChecked())  { for (String c:AllCommands.CMDS_MODS_MEDIA) lista.add(c); modulosSel.append("Media "); }
        if (cbScroll.isChecked()) { for (String c:AllCommands.CMDS_SCROLL) lista.add(c); modulosSel.append("Scroll "); }
        if (cbRecoil.isChecked()) { for (String c:AllCommands.CMDS_ANTIRECOIL) lista.add(c); modulosSel.append("Recoil "); }

        if (lista.isEmpty()) {
            actualizarStatus("⚠ Selecciona al menos uno", 0xFFFFAA00);
            return;
        }

        actualizarStatus("⚡ Aplicando " + lista.size() + " cmds...", 0xFFFFCC00);
        mostrarProgreso(true);
        final int[] aplicados = {0};
        final int[] errores = {0};
        final String mods = modulosSel.toString();

        ShizukuHelper.aplicarComandosAsync(this, lista.toArray(new String[0]),
            new ShizukuHelper.ProgressCallback() {
                public void onProgress(int a, int t, String c) {
                    aplicados[0] = a;
                    setProgreso(a, t);
                    actualizarStatus("⚡ " + a + "/" + t, 0xFFFFCC00);
                }
                public void onComplete(int t) {
                    mostrarProgreso(false);
                    if (aplicados[0] == 0) {
                        actualizarStatus("❌ 0 aplicados - toca Diagnostico", 0xFFFF0000);
                        NotifHelper.notificarImportante(FloatingWindowService.this,
                            "❌ Error al aplicar",
                            "Ningun comando se aplico. Verifica permisos con Diagnostico.");
                    } else {
                        actualizarStatus("✅ " + aplicados[0] + "/" + t + " aplicados", 0xFF00FF00);
                        NotifHelper.notificarImportante(FloatingWindowService.this,
                            "✅ Optimizacion aplicada",
                            aplicados[0] + " de " + t + " comandos aplicados\n\nModulos: " + mods +
                            (errores[0]>0 ? "\n\n" + errores[0] + " errores" : ""));
                    }
                }
                public void onError(String c, String e) { errores[0]++; }
            }, uiHandler);
    }

    private void aplicarTodo() {
        List<String> all = new ArrayList<>();
        for (String c:AllCommands.CMDS_TOUCH_POINTER) all.add(c);
        for (String c:AllCommands.CMDS_GYRO_SENSOR) all.add(c);
        for (String c:AllCommands.CMDS_FPS_PANTALLA) all.add(c);
        for (String c:AllCommands.CMDS_RENDIMIENTO_CPU) all.add(c);
        for (String c:AllCommands.CMDS_RED_LATENCIA) all.add(c);
        for (String c:AllCommands.CMDS_TALKBACK) all.add(c);
        for (String c:AllCommands.CMDS_BATERIA) all.add(c);
        for (String c:AllCommands.CMDS_AUDIO) all.add(c);
        for (String c:AllCommands.CMDS_MODS_MEDIA) all.add(c);
        for (String c:AllCommands.CMDS_SCROLL) all.add(c);
        for (String c:AllCommands.CMDS_ANTIRECOIL) all.add(c);

        actualizarStatus("⚡ Aplicando TODO " + all.size() + " cmds...", 0xFFFFCC00);
        mostrarProgreso(true);
        final int[] aplicados = {0};
        final int[] errores = {0};

        ShizukuHelper.aplicarComandosAsync(this, all.toArray(new String[0]),
            new ShizukuHelper.ProgressCallback() {
                public void onProgress(int a, int t, String c) {
                    aplicados[0] = a;
                    setProgreso(a, t);
                    actualizarStatus("⚡ " + a + "/" + t, 0xFFFFCC00);
                }
                public void onComplete(int t) {
                    mostrarProgreso(false);
                    if (aplicados[0] == 0) {
                        actualizarStatus("❌ 0 aplicados", 0xFFFF0000);
                        NotifHelper.notificarImportante(FloatingWindowService.this,
                            "❌ Error", "Ningun comando aplicado. Toca Diagnostico.");
                    } else {
                        actualizarStatus("✅ TODO: " + aplicados[0] + "/" + t, 0xFF00FF00);
                        NotifHelper.notificarImportante(FloatingWindowService.this,
                            "✅ TODO Optimizado",
                            aplicados[0] + " comandos aplicados\nReinicia Free Fire");
                    }
                }
                public void onError(String c, String e) { errores[0]++; }
            }, uiHandler);
    }

    private void calibrar() {
        actualizarStatus("🔍 Analizando dispositivo...", 0xFF00AAFF);
        mostrarProgreso(true);
        SensibilidadCalibrator.ResultadoCalib r = SensibilidadCalibrator.calibrar(this);
        actualizarStatus("🎯 " + r.perfil, 0xFF00AAFF);
        ShizukuHelper.aplicarComandosAsync(this, r.comandos.toArray(new String[0]),
            new ShizukuHelper.ProgressCallback() {
                public void onProgress(int a, int t, String c) {
                    setProgreso(a, t);
                    actualizarStatus("🎯 " + a + "/" + t, 0xFF00AAFF);
                }
                public void onComplete(int t) {
                    mostrarProgreso(false);
                    actualizarStatus("✅ Calibracion: " + r.perfil, 0xFF00FF00);
                    NotifHelper.notificarImportante(FloatingWindowService.this,
                        "🎯 Calibracion aplicada",
                        "Perfil: " + r.perfil + "\n\n" + t + " comandos aplicados");
                }
                public void onError(String c, String e) {}
            }, uiHandler);
    }

    private void abrirJuego(String pkg, String nombre) {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                actualizarStatus("🎮 Abriendo " + nombre, 0xFFFF6600);
            } else {
                actualizarStatus("⚠ " + nombre + " no instalado", 0xFFFFAA00);
            }
        } catch (Exception e) { Toast.makeText(this,"Error",Toast.LENGTH_SHORT).show(); }
    }

    private TextView secTit(String t) {
        TextView v = new TextView(this);
        v.setText(t); v.setTextSize(11);
        v.setTypeface(null, Typeface.BOLD);
        v.setTextColor(0xFFFF6600);
        v.setShadowLayer(4f, 0, 0, 0xFFFF0000);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,4,0,4); v.setLayoutParams(lp);
        return v;
    }
    private CheckBox cb(String t, int count) {
        CheckBox c = new CheckBox(this);
        c.setText(t + " (" + count + ")");
        c.setTextSize(11);
        c.setTextColor(0xFFEEEEEE);
        c.setButtonTintList(android.content.res.ColorStateList.valueOf(0xFFFF4400));
        c.setPadding(4, 0, 0, 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,2,0,2); c.setLayoutParams(lp);
        return c;
    }
    private Button btn3D(String t, int top, int bot, int fg) {
        Button b = new Button(this);
        b.setText(t);
        b.setTextSize(11);
        b.setTextColor(fg);
        b.setTypeface(null, Typeface.BOLD);
        b.setShadowLayer(3f, 1, 1, 0x80000000);
        b.setPadding(0, 12, 0, 12);
        // Fondo 3D con gradient
        GradientDrawable bg = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{top, bot});
        bg.setCornerRadius(8);
        bg.setStroke(1, 0x80FFFFFF);
        b.setBackground(bg);
        b.setElevation(4f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,3,0,3); b.setLayoutParams(lp);
        return b;
    }
    private View sep() {
        View v = new View(this);
        GradientDrawable bg = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{0x00000000, 0xFFFF0000, 0x00000000});
        v.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,2);
        lp.setMargins(0,6,0,6); v.setLayoutParams(lp);
        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingRoot != null && wm != null) {
            try { wm.removeView(floatingRoot); } catch (Exception ignored) {}
        }
    }
}
