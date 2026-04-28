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
import android.widget.HorizontalScrollView;
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

    private CheckBox cbTouch, cbGyro, cbFps, cbCpu, cbRed, cbTalk, cbBat, cbAudio, cbMedia, cbScroll, cbRecoil, cbReso;

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
                .setContentText("Activo").setSmallIcon(R.mipmap.ic_launcher)
                .setColor(0xFFFF0000).setColorized(true)
                .addAction(android.R.drawable.ic_delete, "Cerrar", pi).build();
        } else {
            n = new Notification.Builder(this).setContentTitle("AUXILIO MIRA flotante")
                .setSmallIcon(R.mipmap.ic_launcher).build();
        }
        startForeground(1, n);
    }

    private void crearVentana() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics dm = getResources().getDisplayMetrics();

        // BURBUJA cuadrada
        int bSize = (int)(dm.density * 54);
        bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setGravity(Gravity.CENTER);
        bubble.setPadding(0, 0, 0, 0);
        GradientDrawable bgB = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
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
        bubble.setLayoutParams(new LinearLayout.LayoutParams(bSize, bSize));

        // PANEL - HORIZONTAL/ACOSTADO (más ancho que alto)
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setVisibility(View.GONE);
        GradientDrawable bgP = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xF0150000, 0xF0050000, 0xF0000000});
        bgP.setStroke(3, 0xFFFF0000);
        bgP.setCornerRadius(20);
        panel.setBackground(bgP);
        panel.setPadding(10, 8, 10, 8);
        panel.setElevation(20f);

        // Header
        TextView tvT = new TextView(this);
        tvT.setText("◉ AUXILIO MIRA");
        tvT.setTextSize(13); tvT.setTypeface(null, Typeface.BOLD);
        tvT.setTextColor(0xFFFFFFFF); tvT.setShadowLayer(6f, 0, 0, 0xFFFF0000);
        tvT.setGravity(Gravity.CENTER); tvT.setPadding(0, 0, 0, 4);
        panel.addView(tvT);

        // Status
        LinearLayout statusBox = new LinearLayout(this);
        statusBox.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bgSt = new GradientDrawable();
        bgSt.setColor(0xFF1A0000); bgSt.setStroke(1, 0xFFFF4400); bgSt.setCornerRadius(6);
        statusBox.setBackground(bgSt); statusBox.setPadding(8, 4, 8, 4);
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(-1, -2);
        stLp.setMargins(0, 2, 0, 4); statusBox.setLayoutParams(stLp);
        tvStatus = new TextView(this);
        tvStatus.setText("⚡ Selecciona modulos");
        tvStatus.setTextSize(11); tvStatus.setTypeface(null, Typeface.BOLD);
        tvStatus.setTextColor(0xFFFFCC00); tvStatus.setGravity(Gravity.CENTER);
        statusBox.addView(tvStatus);
        progressLoad = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressLoad.setMax(100); progressLoad.setVisibility(View.GONE);
        progressLoad.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFFF0000));
        statusBox.addView(progressLoad);
        panel.addView(statusBox);

        // Layout HORIZONTAL: 3 columnas (módulos | extra | acciones)
        LinearLayout cols = new LinearLayout(this);
        cols.setOrientation(LinearLayout.HORIZONTAL);

        // Col 1: Módulos
        ScrollView svM = new ScrollView(this);
        LinearLayout colM = new LinearLayout(this);
        colM.setOrientation(LinearLayout.VERTICAL);
        colM.addView(tv("MODULOS:", 9, 0xFFFF6600));
        cbTouch  = cb("👆 Puntero", AllCommands.CMDS_TOUCH_POINTER.length);
        cbGyro   = cb("🌀 Giro", AllCommands.CMDS_GYRO_SENSOR.length);
        cbFps    = cb("🖥 FPS", AllCommands.CMDS_FPS_PANTALLA.length);
        cbCpu    = cb("⚡ CPU", AllCommands.CMDS_RENDIMIENTO_CPU.length);
        cbRed    = cb("🌐 Red", AllCommands.CMDS_RED_LATENCIA.length);
        cbTalk   = cb("♿ Talk", AllCommands.CMDS_TALKBACK.length);
        colM.addView(cbTouch); colM.addView(cbGyro); colM.addView(cbFps);
        colM.addView(cbCpu); colM.addView(cbRed); colM.addView(cbTalk);
        svM.addView(colM);

        // Col 2: Más módulos
        ScrollView svE = new ScrollView(this);
        LinearLayout colE = new LinearLayout(this);
        colE.setOrientation(LinearLayout.VERTICAL);
        colE.addView(tv("EXTRA:", 9, 0xFFFF6600));
        cbBat    = cb("🔋 Bat", AllCommands.CMDS_BATERIA.length);
        cbAudio  = cb("🔊 Audio", AllCommands.CMDS_AUDIO.length);
        cbMedia  = cb("🔵 Media", AllCommands.CMDS_MODS_MEDIA.length);
        cbScroll = cb("📜 Scroll", AllCommands.CMDS_SCROLL.length);
        cbRecoil = cb("🎯 Recoil", AllCommands.CMDS_ANTIRECOIL.length);
        cbReso   = cb("📺 Reso", AllCommands.CMDS_RESOLUCION.length);
        colE.addView(cbBat); colE.addView(cbAudio); colE.addView(cbMedia);
        colE.addView(cbScroll); colE.addView(cbRecoil); colE.addView(cbReso);
        svE.addView(colE);

        // Col 3: Acciones
        ScrollView svA = new ScrollView(this);
        LinearLayout colA = new LinearLayout(this);
        colA.setOrientation(LinearLayout.VERTICAL);
        colA.addView(tv("ACCIONES:", 9, 0xFFFF6600));

        Button btnApl = btnV("APLICAR", 0xFFFF3300, 0xFFCC0000);
        btnApl.setOnClickListener(v -> aplicarSeleccionados());
        colA.addView(btnApl);

        Button btnTodo = btnV("⚡ TODO", 0xFFFF6600, 0xFF883300);
        btnTodo.setOnClickListener(v -> aplicarTodo());
        colA.addView(btnTodo);

        Button btnCal = btnV("🎯 Calibrar", 0xFF0066FF, 0xFF003388);
        btnCal.setOnClickListener(v -> calibrar());
        colA.addView(btnCal);

        Button btnCache = btnV("🗑 Cache", 0xFFCCAA00, 0xFF665500);
        btnCache.setOnClickListener(v -> {
            actualizarStatus("🗑 Limpiando...", 0xFFFFCC00);
            ShizukuHelper.limpiarCache(this);
            actualizarStatus("✅ Cache limpiado", 0xFF00FF00);
            NotifHelper.notificar(this, "🗑 Cache", "Limpiado");
        });
        colA.addView(btnCache);

        Button btnPerm = btnV("🔍 Diag", 0xFFAA00FF, 0xFF550088);
        btnPerm.setOnClickListener(v -> verificarPermiso());
        colA.addView(btnPerm);

        Button btnFF = btnV("🔥 FF", 0xFFFF6600, 0xFF884400);
        btnFF.setOnClickListener(v -> abrirJuego("com.dts.freefireth", "FF"));
        colA.addView(btnFF);

        Button btnFFM = btnV("⚡ FFMax", 0xFFFFAA00, 0xFF885500);
        btnFFM.setOnClickListener(v -> abrirJuego("com.dts.freefiremax", "FF MAX"));
        colA.addView(btnFFM);

        Button btnClose = btnV("✕ Cerrar", 0xFF666666, 0xFF222222);
        btnClose.setOnClickListener(v -> stopSelf());
        colA.addView(btnClose);
        svA.addView(colA);

        // Configurar las 3 columnas
        LinearLayout.LayoutParams lpC1 = new LinearLayout.LayoutParams(0, -2, 1f);
        lpC1.setMargins(0, 0, 4, 0); svM.setLayoutParams(lpC1);
        LinearLayout.LayoutParams lpC2 = new LinearLayout.LayoutParams(0, -2, 1f);
        lpC2.setMargins(0, 0, 4, 0); svE.setLayoutParams(lpC2);
        LinearLayout.LayoutParams lpC3 = new LinearLayout.LayoutParams(0, -2, 1.1f);
        svA.setLayoutParams(lpC3);

        cols.addView(svM); cols.addView(svE); cols.addView(svA);

        // ALTURA: tipo barra horizontal — más bajo que ancho
        int panelH = (int)(dm.heightPixels * 0.32f);
        cols.setLayoutParams(new LinearLayout.LayoutParams(-1, panelH));
        panel.addView(cols);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(bubble); root.addView(panel);
        floatingRoot = root;

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;

        // ANCHO: 90% del ancho de pantalla
        params = new WindowManager.LayoutParams(
            (int)(dm.widthPixels * 0.90f),
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 20; params.y = 150;

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
        ObjectAnimator pulso = ObjectAnimator.ofFloat(ivIcon, "alpha", 1f, 0.7f, 1f);
        pulso.setDuration(1800);
        pulso.setRepeatCount(ValueAnimator.INFINITE);
        pulso.setInterpolator(new LinearInterpolator());
        pulso.start();
    }

    private void togglePanel() {
        expandido = !expandido;
        params.flags = expandido ? WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL : WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        try { wm.updateViewLayout(floatingRoot, params); } catch (Exception e){}
        panel.setVisibility(expandido ? View.VISIBLE : View.GONE);
        if (expandido) {
            panel.setAlpha(0f);
            panel.animate().alpha(1f).setDuration(250).start();
        }
    }

    private void actualizarStatus(String msg, int color) {
        uiHandler.post(() -> { if (tvStatus != null) { tvStatus.setText(msg); tvStatus.setTextColor(color); } });
    }
    private void mostrarProgreso(boolean v) {
        uiHandler.post(() -> { if (progressLoad != null) progressLoad.setVisibility(v ? View.VISIBLE : View.GONE); });
    }
    private void setProgreso(int a, int t) {
        uiHandler.post(() -> { if (progressLoad != null) { progressLoad.setMax(t); progressLoad.setProgress(a); } });
    }

    private void verificarPermiso() {
        String diag = ShizukuHelper.estadoPermisoDetallado(this);
        actualizarStatus("🔍 Ver notificacion", 0xFFCC88FF);
        NotifHelper.notificarImportante(this, "🔍 Diagnostico", diag);
    }

    private void aplicarSeleccionados() {
        List<String> lista = new ArrayList<>();
        if (cbTouch.isChecked())  for (String c:AllCommands.CMDS_TOUCH_POINTER) lista.add(c);
        if (cbGyro.isChecked())   for (String c:AllCommands.CMDS_GYRO_SENSOR) lista.add(c);
        if (cbFps.isChecked())    for (String c:AllCommands.CMDS_FPS_PANTALLA) lista.add(c);
        if (cbCpu.isChecked())    for (String c:AllCommands.CMDS_RENDIMIENTO_CPU) lista.add(c);
        if (cbRed.isChecked())    for (String c:AllCommands.CMDS_RED_LATENCIA) lista.add(c);
        if (cbTalk.isChecked())   for (String c:AllCommands.CMDS_TALKBACK) lista.add(c);
        if (cbBat.isChecked())    for (String c:AllCommands.CMDS_BATERIA) lista.add(c);
        if (cbAudio.isChecked())  for (String c:AllCommands.CMDS_AUDIO) lista.add(c);
        if (cbMedia.isChecked())  for (String c:AllCommands.CMDS_MODS_MEDIA) lista.add(c);
        if (cbScroll.isChecked()) for (String c:AllCommands.CMDS_SCROLL) lista.add(c);
        if (cbRecoil.isChecked()) for (String c:AllCommands.CMDS_ANTIRECOIL) lista.add(c);
        if (cbReso.isChecked())   for (String c:AllCommands.CMDS_RESOLUCION) lista.add(c);
        if (lista.isEmpty()) { actualizarStatus("⚠ Selecciona al menos uno", 0xFFFFAA00); return; }
        actualizarStatus("⚡ Aplicando " + lista.size() + " (lotes 50)...", 0xFFFFCC00);
        mostrarProgreso(true);
        final int[] aplicados = {0};
        ShizukuHelper.aplicarComandosAsync(this, lista.toArray(new String[0]),
            new ShizukuHelper.ProgressCallback() {
                public void onProgress(int a, int t, String c) {
                    aplicados[0] = a;
                    setProgreso(a, t);
                    actualizarStatus("⚡ " + a + "/" + t, 0xFFFFCC00);
                }
                public void onComplete(int t) {
                    aplicados[0] = t;
                    mostrarProgreso(false);
                    if (aplicados[0] == 0) {
                        actualizarStatus("❌ 0 - Diag.", 0xFFFF0000);
                        NotifHelper.notificarImportante(FloatingWindowService.this, "❌ Error", "0 cmds aplicados");
                    } else {
                        actualizarStatus("✅ " + aplicados[0] + "/" + t, 0xFF00FF00);
                        NotifHelper.notificarImportante(FloatingWindowService.this, "✅ Aplicado", aplicados[0] + " cmds");
                    }
                }
                public void onError(String c, String e) {}
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
        actualizarStatus("⚡ TODO " + all.size() + "...", 0xFFFFCC00);
        mostrarProgreso(true);
        final int[] aplicados = {0};
        ShizukuHelper.aplicarComandosAsync(this, all.toArray(new String[0]),
            new ShizukuHelper.ProgressCallback() {
                public void onProgress(int a, int t, String c) {
                    aplicados[0] = a;
                    setProgreso(a, t);
                    actualizarStatus("⚡ " + a + "/" + t, 0xFFFFCC00);
                }
                public void onComplete(int t) {
                    aplicados[0] = t;
                    mostrarProgreso(false);
                    actualizarStatus("✅ TODO: " + aplicados[0], 0xFF00FF00);
                    NotifHelper.notificarImportante(FloatingWindowService.this, "✅ TODO", aplicados[0] + " cmds aplicados");
                }
                public void onError(String c, String e) {}
            }, uiHandler);
    }

    private void calibrar() {
        SensibilidadCalibrator.ResultadoCalib r = SensibilidadCalibrator.calibrar(this);
        actualizarStatus("🎯 " + r.perfil, 0xFF00AAFF);
        ShizukuHelper.aplicarComandosAsync(this, r.comandos.toArray(new String[0]),
            new ShizukuHelper.ProgressCallback() {
                public void onProgress(int a, int t, String c) {}
                public void onComplete(int t) {
                    actualizarStatus("✅ " + r.perfil, 0xFF00FF00);
                    NotifHelper.notificarImportante(FloatingWindowService.this, "🎯 Calibracion", r.perfil);
                }
                public void onError(String c, String e) {}
            }, uiHandler);
    }

    private void abrirJuego(String pkg, String nombre) {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i != null) { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); actualizarStatus("🎮 " + nombre, 0xFFFF6600); }
            else actualizarStatus("⚠ " + nombre + " no inst", 0xFFFFAA00);
        } catch (Exception e) {}
    }

    private TextView tv(String t, float s, int c) {
        TextView v = new TextView(this);
        v.setText(t); v.setTextSize(s); v.setTextColor(c);
        v.setTypeface(null, Typeface.BOLD);
        return v;
    }
    private CheckBox cb(String t, int n) {
        CheckBox c = new CheckBox(this);
        c.setText(t + " (" + n + ")"); c.setTextSize(10);
        c.setTextColor(0xFFEEEEEE); c.setPadding(2, 0, 0, 0);
        c.setButtonTintList(android.content.res.ColorStateList.valueOf(0xFFFF4400));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 1, 0, 1); c.setLayoutParams(lp);
        return c;
    }
    private Button btnV(String t, int top, int bot) {
        Button b = new Button(this); b.setText(t);
        b.setTextSize(10); b.setTextColor(0xFFFFFFFF);
        b.setTypeface(null, Typeface.BOLD);
        b.setShadowLayer(2f, 1, 1, 0x80000000);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{top, bot});
        bg.setCornerRadius(8); bg.setStroke(1, 0x60FFFFFF);
        b.setBackground(bg); b.setElevation(3f);
        b.setPadding(0, 8, 0, 8);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 2, 0, 2); b.setLayoutParams(lp);
        return b;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingRoot != null && wm != null) {
            try { wm.removeView(floatingRoot); } catch (Exception ignored) {}
        }
    }
}
