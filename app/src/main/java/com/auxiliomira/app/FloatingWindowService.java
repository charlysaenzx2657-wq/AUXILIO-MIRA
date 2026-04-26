package com.auxiliomira.app;

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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
    private TextView tvStatus;        // mensajes de estado dentro del panel
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

        // ── BURBUJA cuadrada con icono ──
        int bSize = (int)(dm.density * 48);
        bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setGravity(Gravity.CENTER);
        bubble.setPadding(0, 0, 0, 0);

        GradientDrawable bgB = new GradientDrawable();
        bgB.setShape(GradientDrawable.OVAL);
        bgB.setColor(0xEECC0000);
        bgB.setStroke(3, 0xFFFFFFFF);
        bubble.setBackground(bgB);

        ImageView ivIcon = new ImageView(this);
        ivIcon.setImageResource(R.mipmap.ic_launcher);
        int iconSize = (int)(dm.density * 32);
        LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(iconSize, iconSize);
        ivIcon.setLayoutParams(ivLp);
        ivIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        bubble.addView(ivIcon);

        LinearLayout.LayoutParams bubbleLp = new LinearLayout.LayoutParams(bSize, bSize);
        bubble.setLayoutParams(bubbleLp);

        // ── PANEL HORIZONTAL (acostado) ──
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setVisibility(View.GONE);
        GradientDrawable bgP = new GradientDrawable();
        bgP.setColor(0xF0050000);
        bgP.setStroke(2, 0xFFFF0000);
        bgP.setCornerRadius(14);
        panel.setBackground(bgP);
        panel.setPadding(8, 8, 8, 8);

        // Título
        TextView tvT = new TextView(this);
        tvT.setText("AUXILIO MIRA");
        tvT.setTextSize(11);
        tvT.setTypeface(null, Typeface.BOLD);
        tvT.setTextColor(0xFFFF0000);
        tvT.setGravity(Gravity.CENTER);
        panel.addView(tvT);

        // ── Status bar (avisos de aplicación) ──
        tvStatus = new TextView(this);
        tvStatus.setText("Selecciona modulos");
        tvStatus.setTextSize(9);
        tvStatus.setTextColor(0xFFAAAAAA);
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setPadding(0, 4, 0, 4);
        tvStatus.setBackgroundColor(0xFF1A0000);
        panel.addView(tvStatus);

        // ── Layout de 2 COLUMNAS lado a lado ──
        LinearLayout twoCols = new LinearLayout(this);
        twoCols.setOrientation(LinearLayout.HORIZONTAL);

        // Col izquierda: módulos en scroll
        ScrollView svLeft = new ScrollView(this);
        LinearLayout colLeft = new LinearLayout(this);
        colLeft.setOrientation(LinearLayout.VERTICAL);
        colLeft.setPadding(4, 6, 4, 6);

        TextView tvSel = tv("Modulos:", 9, 0xFFFF6600);
        colLeft.addView(tvSel);

        cbTouch  = cb("Puntero (" + AllCommands.CMDS_TOUCH_POINTER.length + ")");
        cbGyro   = cb("Giro (" + AllCommands.CMDS_GYRO_SENSOR.length + ")");
        cbFps    = cb("FPS (" + AllCommands.CMDS_FPS_PANTALLA.length + ")");
        cbCpu    = cb("CPU/RAM (" + AllCommands.CMDS_RENDIMIENTO_CPU.length + ")");
        cbRed    = cb("Anti-Lag (" + AllCommands.CMDS_RED_LATENCIA.length + ")");
        cbTalk   = cb("Talk OFF (" + AllCommands.CMDS_TALKBACK.length + ")");
        cbBat    = cb("Anti-Therm (" + AllCommands.CMDS_BATERIA.length + ")");
        cbAudio  = cb("Audio (" + AllCommands.CMDS_AUDIO.length + ")");
        cbMedia  = cb("Media (" + AllCommands.CMDS_MODS_MEDIA.length + ")");
        cbScroll = cb("Scroll (" + AllCommands.CMDS_SCROLL.length + ")");
        cbRecoil = cb("Anti-Recoil (" + AllCommands.CMDS_ANTIRECOIL.length + ")");

        colLeft.addView(cbTouch); colLeft.addView(cbGyro); colLeft.addView(cbFps);
        colLeft.addView(cbCpu); colLeft.addView(cbRed); colLeft.addView(cbTalk);
        colLeft.addView(cbBat); colLeft.addView(cbAudio); colLeft.addView(cbMedia);
        colLeft.addView(cbScroll); colLeft.addView(cbRecoil);

        svLeft.addView(colLeft);
        LinearLayout.LayoutParams lpLeft = new LinearLayout.LayoutParams(0, -1, 1.2f);
        lpLeft.setMargins(0, 0, 4, 0);
        svLeft.setLayoutParams(lpLeft);

        // Col derecha: botones acción
        ScrollView svRight = new ScrollView(this);
        LinearLayout colRight = new LinearLayout(this);
        colRight.setOrientation(LinearLayout.VERTICAL);
        colRight.setPadding(4, 6, 4, 6);

        TextView tvAcc = tv("Acciones:", 9, 0xFFFF6600);
        colRight.addView(tvAcc);

        Button btnApl = btnV("APLICAR", 0xFFCC0000, 0xFFFFFFFF);
        btnApl.setOnClickListener(v -> aplicarSeleccionados());
        colRight.addView(btnApl);

        Button btnTodo = btnV("TODO", 0xFF880000, 0xFFFFFFFF);
        btnTodo.setOnClickListener(v -> aplicarTodo());
        colRight.addView(btnTodo);

        colRight.addView(sep());

        Button btnCal = btnV("Calibrar", 0xFF001A33, 0xFF00AAFF);
        btnCal.setOnClickListener(v -> calibrar());
        colRight.addView(btnCal);

        Button btnCache = btnV("Cache", 0xFF1A1A00, 0xFFFFFF00);
        btnCache.setOnClickListener(v -> {
            actualizarStatus("Limpiando cache...");
            ShizukuHelper.limpiarCache(this);
            actualizarStatus("OK Cache limpiado");
            Toast.makeText(this, "Cache limpiado", Toast.LENGTH_SHORT).show();
        });
        colRight.addView(btnCache);

        Button btnPerm = btnV("Permiso?", 0xFF0A001A, 0xFFCC88FF);
        btnPerm.setOnClickListener(v -> verificarPermiso());
        colRight.addView(btnPerm);

        colRight.addView(sep());

        Button btnFF = btnV("Free Fire", 0xFF1A0000, 0xFFFF4400);
        btnFF.setOnClickListener(v -> abrirJuego("com.dts.freefireth", "Free Fire"));
        colRight.addView(btnFF);

        Button btnFFM = btnV("FF MAX", 0xFF1A0A00, 0xFFFF8800);
        btnFFM.setOnClickListener(v -> abrirJuego("com.dts.freefiremax", "Free Fire MAX"));
        colRight.addView(btnFFM);

        colRight.addView(sep());

        Button btnClose = btnV("Cerrar", 0xFF1A1A1A, 0xFF888888);
        btnClose.setOnClickListener(v -> stopSelf());
        colRight.addView(btnClose);

        svRight.addView(colRight);
        LinearLayout.LayoutParams lpRight = new LinearLayout.LayoutParams(0, -1, 1f);
        svRight.setLayoutParams(lpRight);

        twoCols.addView(svLeft);
        twoCols.addView(svRight);

        // Altura del área scrollable - tipo "tabla" media
        int panelH = (int)(dm.heightPixels * 0.42f);
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

        // Ancho 78% de pantalla = "acostado" tipo tabla
        params = new WindowManager.LayoutParams(
            (int)(dm.widthPixels * 0.78f),
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
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int)(e.getRawX()-tx), dy = (int)(e.getRawY()-ty);
                        if (Math.abs(dx)>8||Math.abs(dy)>8) moved = true;
                        params.x = ix+dx; params.y = iy+dy;
                        try { wm.updateViewLayout(floatingRoot, params); } catch (Exception ex){}
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved && System.currentTimeMillis()-down < 350) togglePanel();
                        return true;
                }
                return false;
            }
        });

        wm.addView(floatingRoot, params);
    }

    private void togglePanel() {
        expandido = !expandido;
        params.flags = expandido
            ? WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            : WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        try { wm.updateViewLayout(floatingRoot, params); } catch (Exception e){}
        panel.setVisibility(expandido ? View.VISIBLE : View.GONE);
    }

    private void actualizarStatus(String msg) {
        uiHandler.post(() -> {
            if (tvStatus != null) tvStatus.setText(msg);
        });
    }

    private void verificarPermiso() {
        boolean tienePermiso = ShizukuHelper.tienePermiso(this);
        boolean shizukuActivo = ShizukuHelper.shizukuActivo(this);
        boolean shizukuInstalado = ShizukuHelper.shizukuInstalado(this);

        String msg;
        if (tienePermiso) {
            msg = "OK Permiso ACTIVO - puedes inyectar";
            tvStatus.setTextColor(0xFF00FF00);
        } else if (shizukuActivo) {
            msg = "Shizuku activo, falta autorizar la app";
            tvStatus.setTextColor(0xFFFFAA00);
        } else if (shizukuInstalado) {
            msg = "Shizuku instalado pero NO corriendo";
            tvStatus.setTextColor(0xFFFF6600);
        } else {
            msg = "Shizuku NO instalado";
            tvStatus.setTextColor(0xFFFF0000);
        }
        tvStatus.setText(msg);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
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
            actualizarStatus("Selecciona al menos uno");
            Toast.makeText(this,"Selecciona al menos uno",Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ShizukuHelper.tienePermiso(this)) {
            actualizarStatus("ERROR: Sin permiso. Toca Permiso?");
            Toast.makeText(this,"Sin permiso. Activa Shizuku",Toast.LENGTH_LONG).show();
            return;
        }

        actualizarStatus("Aplicando " + lista.size() + " cmds...");
        final int[] aplicados = {0};
        final int[] errores = {0};

        ShizukuHelper.aplicarComandosAsync(this, lista.toArray(new String[0]),
            new ShizukuHelper.ProgressCallback() {
                public void onProgress(int a, int t, String c) {
                    aplicados[0] = a;
                    actualizarStatus("Aplicando " + a + "/" + t);
                }
                public void onComplete(int t) {
                    String resumen = "OK " + aplicados[0] + "/" + t + " aplicados " + (errores[0]>0 ? "("+errores[0]+" err)" : "") + " - " + modulosSel.toString();
                    actualizarStatus(resumen);
                    Toast.makeText(FloatingWindowService.this, "OK " + aplicados[0] + " cmds aplicados", Toast.LENGTH_LONG).show();
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

        if (!ShizukuHelper.tienePermiso(this)) {
            actualizarStatus("ERROR: Sin permiso. Toca Permiso?");
            Toast.makeText(this, "Sin permiso. Activa Shizuku", Toast.LENGTH_LONG).show();
            return;
        }

        actualizarStatus("Aplicando TODO " + all.size() + " cmds...");
        final int[] errores = {0};

        ShizukuHelper.aplicarComandosAsync(this, all.toArray(new String[0]),
            new ShizukuHelper.ProgressCallback() {
                public void onProgress(int a, int t, String c) {
                    actualizarStatus("Aplicando " + a + "/" + t);
                }
                public void onComplete(int t) {
                    actualizarStatus("OK TODO aplicado: " + t + " cmds " + (errores[0]>0 ? "("+errores[0]+" err)" : ""));
                    Toast.makeText(FloatingWindowService.this, "OK Todo aplicado: " + t, Toast.LENGTH_LONG).show();
                }
                public void onError(String c, String e) { errores[0]++; }
            }, uiHandler);
    }

    private void calibrar() {
        if (!ShizukuHelper.tienePermiso(this)) {
            actualizarStatus("ERROR: Sin permiso. Toca Permiso?");
            return;
        }
        actualizarStatus("Analizando dispositivo...");
        SensibilidadCalibrator.ResultadoCalib r = SensibilidadCalibrator.calibrar(this);
        actualizarStatus("Aplicando: " + r.perfil);
        ShizukuHelper.aplicarComandosAsync(this, r.comandos.toArray(new String[0]),
            new ShizukuHelper.ProgressCallback() {
                public void onProgress(int a, int t, String c) { actualizarStatus("Calibrando " + a + "/" + t); }
                public void onComplete(int t) {
                    actualizarStatus("OK Calibracion: " + r.perfil);
                    Toast.makeText(FloatingWindowService.this, "OK Calibracion lista", Toast.LENGTH_LONG).show();
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
                actualizarStatus("Abriendo " + nombre);
            } else {
                actualizarStatus(nombre + " no instalado");
                Toast.makeText(this,nombre+" no instalado",Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) { Toast.makeText(this,"Error",Toast.LENGTH_SHORT).show(); }
    }

    private TextView tv(String t, float size, int color) {
        TextView v = new TextView(this);
        v.setText(t); v.setTextSize(size); v.setTextColor(color);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,3,0,1); v.setLayoutParams(lp);
        return v;
    }
    private CheckBox cb(String t) {
        CheckBox c = new CheckBox(this);
        c.setText(t); c.setTextSize(8); c.setTextColor(0xFFDDDDDD);
        c.setButtonTintList(android.content.res.ColorStateList.valueOf(0xFFFF4400));
        c.setPadding(2, 0, 0, 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,1,0,1); c.setLayoutParams(lp);
        return c;
    }
    private Button btnV(String t, int bg, int fg) {
        Button b = new Button(this);
        b.setText(t); b.setTextSize(9); b.setTextColor(fg); b.setBackgroundColor(bg);
        b.setPadding(0,8,0,8);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,2,0,2); b.setLayoutParams(lp);
        return b;
    }
    private View sep() {
        View v = new View(this);
        v.setBackgroundColor(0xFF330000);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,1);
        lp.setMargins(0,4,0,4); v.setLayoutParams(lp);
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
