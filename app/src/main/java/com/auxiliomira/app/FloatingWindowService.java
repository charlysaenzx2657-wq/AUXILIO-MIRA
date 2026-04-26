package com.auxiliomira.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class FloatingWindowService extends Service {

    private static final String TAG = "FloatingService";
    private static final String CHANNEL_ID = "auxilio_mira_float";

    private WindowManager wm;
    private View floatingRoot;
    private LinearLayout bubble;
    private LinearLayout panel;
    private boolean expandido = false;
    private WindowManager.LayoutParams params;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            crearNotificacion();
            crearVentana();
            Toast.makeText(this, "🪟 Ventana flotante activa", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error crear flotante: " + e.getMessage(), e);
            Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    private void crearNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "Auxilio MIRA Flotante",
                NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Ventana flotante activa");
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(ch);
        }

        Intent stopIntent = new Intent(this, FloatingWindowService.class);
        stopIntent.setAction("STOP");
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent pi = PendingIntent.getService(this, 0, stopIntent, flags);

        Notification n;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            n = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("◉ AUXILIO MIRA activo")
                .setContentText("Ventana flotante en pantalla")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .addAction(android.R.drawable.ic_delete, "Cerrar", pi)
                .build();
        } else {
            n = new Notification.Builder(this)
                .setContentTitle("◉ AUXILIO MIRA activo")
                .setContentText("Ventana flotante")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .build();
        }
        startForeground(1, n);
    }

    private void crearVentana() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        // ═══════ BURBUJA (siempre visible) ═══════
        bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setGravity(Gravity.CENTER);
        bubble.setPadding(28, 18, 28, 18);

        GradientDrawable bgBubble = new GradientDrawable();
        bgBubble.setShape(GradientDrawable.OVAL);
        bgBubble.setColor(0xFFCC0000);
        bgBubble.setStroke(4, 0xFFFFFFFF);
        bubble.setBackground(bgBubble);

        TextView tvBubble = new TextView(this);
        tvBubble.setText("◉");
        tvBubble.setTextSize(22);
        tvBubble.setTypeface(null, Typeface.BOLD);
        tvBubble.setTextColor(Color.WHITE);
        tvBubble.setGravity(Gravity.CENTER);
        bubble.addView(tvBubble);

        // ═══════ PANEL EXPANDIDO ═══════
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(0xEE000000);
        panel.setPadding(16, 16, 16, 16);
        panel.setVisibility(View.GONE);

        // Borde rojo
        GradientDrawable bgPanel = new GradientDrawable();
        bgPanel.setColor(0xEE0A0000);
        bgPanel.setStroke(3, 0xFFFF0000);
        bgPanel.setCornerRadius(20);
        panel.setBackground(bgPanel);

        // Header
        TextView header = new TextView(this);
        header.setText("◉  AUXILIO MIRA");
        header.setTextSize(16);
        header.setTypeface(null, Typeface.BOLD);
        header.setTextColor(0xFFFF0000);
        header.setGravity(Gravity.CENTER);
        header.setPadding(0, 4, 0, 4);
        panel.addView(header);

        TextView sub = new TextView(this);
        sub.setText("v3.0 — 1967 comandos");
        sub.setTextSize(10);
        sub.setTextColor(0xFFFF6600);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 0, 0, 12);
        panel.addView(sub);

        // ScrollView interno con todos los botones
        ScrollView scroll = new ScrollView(this);
        LinearLayout scrollContent = new LinearLayout(this);
        scrollContent.setOrientation(LinearLayout.VERTICAL);

        // Botón APLICAR TODO
        scrollContent.addView(crearBtn("🔴  APLICAR TODO", 0xFFCC0000, 0xFFFFFFFF, true,
            v -> aplicarTodos()));

        scrollContent.addView(separador());

        // Cada módulo individual
        Object[][] mods = {
            {"👆 Puntero", AllCommands.CMDS_TOUCH_POINTER},
            {"🌀 Giroscopio", AllCommands.CMDS_GYRO_SENSOR},
            {"🖥️ FPS Max", AllCommands.CMDS_FPS_PANTALLA},
            {"⚡ CPU/RAM", AllCommands.CMDS_RENDIMIENTO_CPU},
            {"🌐 Anti-Lag", AllCommands.CMDS_RED_LATENCIA},
            {"♿ Talkback OFF", AllCommands.CMDS_TALKBACK},
            {"🔋 Anti-Thermal", AllCommands.CMDS_BATERIA},
            {"🔊 Audio", AllCommands.CMDS_AUDIO},
            {"🔵 Mod Media", AllCommands.CMDS_MODS_MEDIA},
            {"📜 Scroll Pro", AllCommands.CMDS_SCROLL},
            {"🎯 Anti-Recoil", AllCommands.CMDS_ANTIRECOIL},
        };
        for (Object[] mod : mods) {
            String nombre = (String) mod[0];
            String[] cmds = (String[]) mod[1];
            scrollContent.addView(crearBtn(nombre + "  (" + cmds.length + ")",
                0xFF1A0000, 0xFFFF6644, false,
                v -> aplicarLista(cmds, nombre)));
        }

        scrollContent.addView(separador());

        // Calibración
        scrollContent.addView(crearBtn("🎯 CALIBRAR SENSI",
            0xFF001A33, 0xFF00AAFF, false, v -> calibrarSensi()));

        // Limpiar caché
        scrollContent.addView(crearBtn("🗑️ LIMPIAR CACHÉ",
            0xFF1A1A00, 0xFFFFFF00, false, v -> {
                ShizukuHelper.limpiarCache(this);
                Toast.makeText(this, "✅ Caché limpiado", Toast.LENGTH_SHORT).show();
            }));

        scrollContent.addView(separador());

        // Free Fire
        scrollContent.addView(crearBtn("🔥 FREE FIRE",
            0xFF1A0000, 0xFFFF4400, false, v -> abrirJuego("com.dts.freefireth")));
        scrollContent.addView(crearBtn("⚡ FREE FIRE MAX",
            0xFF1A0A00, 0xFFFF8800, false, v -> abrirJuego("com.dts.freefiremax")));

        scrollContent.addView(separador());

        // Cerrar
        scrollContent.addView(crearBtn("✕ CERRAR FLOTANTE",
            0xFF1A1A1A, 0xFF888888, false, v -> stopSelf()));

        scroll.addView(scrollContent);

        // Limitar altura del scroll
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int maxH = (int)(dm.heightPixels * 0.65);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(-1, maxH);
        scroll.setLayoutParams(scrollLp);

        panel.addView(scroll);

        // Contenedor raíz
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(bubble);
        root.addView(panel);
        floatingRoot = root;

        // Tipo de overlay
        int type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 30;
        params.y = 200;

        // Listener para drag/click en la burbuja
        bubble.setOnTouchListener(new View.OnTouchListener() {
            int initX, initY;
            float touchX, touchY;
            long downTime;
            boolean moved = false;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initX = params.x;
                        initY = params.y;
                        touchX = e.getRawX();
                        touchY = e.getRawY();
                        downTime = System.currentTimeMillis();
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int)(e.getRawX() - touchX);
                        int dy = (int)(e.getRawY() - touchY);
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) moved = true;
                        params.x = initX + dx;
                        params.y = initY + dy;
                        try { wm.updateViewLayout(floatingRoot, params); } catch (Exception ex) {}
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved && System.currentTimeMillis() - downTime < 300) {
                            togglePanel();
                        }
                        return true;
                }
                return false;
            }
        });

        wm.addView(floatingRoot, params);
    }

    private void togglePanel() {
        expandido = !expandido;
        panel.setVisibility(expandido ? View.VISIBLE : View.GONE);
    }

    private View separador() {
        View v = new View(this);
        v.setBackgroundColor(0xFF330000);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 2);
        lp.setMargins(8, 8, 8, 8);
        v.setLayoutParams(lp);
        return v;
    }

    private Button crearBtn(String txt, int bg, int fg, boolean bold, View.OnClickListener cl) {
        Button b = new Button(this);
        b.setText(txt);
        b.setTextSize(12);
        b.setTextColor(fg);
        b.setBackgroundColor(bg);
        if (bold) b.setTypeface(null, Typeface.BOLD);
        b.setPadding(0, 16, 0, 16);
        b.setOnClickListener(cl);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 4, 0, 4);
        b.setLayoutParams(lp);
        return b;
    }

    private void aplicarTodos() {
        java.util.List<String> all = new java.util.ArrayList<>();
        for (String c : AllCommands.CMDS_TOUCH_POINTER) all.add(c);
        for (String c : AllCommands.CMDS_GYRO_SENSOR) all.add(c);
        for (String c : AllCommands.CMDS_FPS_PANTALLA) all.add(c);
        for (String c : AllCommands.CMDS_RENDIMIENTO_CPU) all.add(c);
        for (String c : AllCommands.CMDS_RED_LATENCIA) all.add(c);
        for (String c : AllCommands.CMDS_TALKBACK) all.add(c);
        for (String c : AllCommands.CMDS_BATERIA) all.add(c);
        for (String c : AllCommands.CMDS_AUDIO) all.add(c);
        for (String c : AllCommands.CMDS_MODS_MEDIA) all.add(c);
        for (String c : AllCommands.CMDS_SCROLL) all.add(c);
        for (String c : AllCommands.CMDS_ANTIRECOIL) all.add(c);
        aplicarLista(all.toArray(new String[0]), "TODOS");
    }

    private void aplicarLista(String[] cmds, String nombre) {
        if (!ShizukuHelper.tienePermiso(this)) {
            Toast.makeText(this, "⚠ Sin permiso. Activa Shizuku/ADB", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, "⚡ Aplicando " + nombre + "...", Toast.LENGTH_SHORT).show();
        ShizukuHelper.aplicarComandosAsync(this, cmds,
            new ShizukuHelper.ProgressCallback() {
                public void onProgress(int a, int t, String c) {}
                public void onComplete(int t) {
                    Toast.makeText(FloatingWindowService.this,
                        "✅ " + nombre + ": " + t + " cmds", Toast.LENGTH_SHORT).show();
                }
                public void onError(String c, String e) {}
            }, uiHandler);
    }

    private void calibrarSensi() {
        SensibilidadCalibrator.ResultadoCalib r = SensibilidadCalibrator.calibrar(this);
        Toast.makeText(this, "🎯 " + r.perfil, Toast.LENGTH_SHORT).show();
        aplicarLista(r.comandos.toArray(new String[0]), "Calibración");
    }

    private void abrirJuego(String pkg) {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            } else {
                Toast.makeText(this, "⚠ No instalado", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "❌ Error", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingRoot != null && wm != null) {
            try { wm.removeView(floatingRoot); } catch (Exception ignored) {}
        }
    }
}
