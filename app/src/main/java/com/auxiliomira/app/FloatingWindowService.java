package com.auxiliomira.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.CheckBox;
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
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .addAction(android.R.drawable.ic_delete, "Cerrar", pi)
                .build();
        } else {
            n = new Notification.Builder(this)
                .setContentTitle("AUXILIO MIRA flotante")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .build();
        }
        startForeground(1, n);
    }

    private void crearVentana() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics dm = getResources().getDisplayMetrics();

        bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setGravity(Gravity.CENTER);
        bubble.setPadding(16, 10, 16, 10);
        GradientDrawable bgB = new GradientDrawable();
        bgB.setShape(GradientDrawable.OVAL);
        bgB.setColor(0xEECC0000);
        bgB.setStroke(3, 0xFFFFFFFF);
        bubble.setBackground(bgB);
        TextView tvB = new TextView(this);
        tvB.setText("AM");
        tvB.setTextSize(11);
        tvB.setTypeface(null, Typeface.BOLD);
        tvB.setTextColor(Color.WHITE);
        tvB.setGravity(Gravity.CENTER);
        bubble.addView(tvB);

        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setVisibility(View.GONE);
        GradientDrawable bgP = new GradientDrawable();
        bgP.setColor(0xF0050000);
        bgP.setStroke(2, 0xFFFF0000);
        bgP.setCornerRadius(14);
        panel.setBackground(bgP);
        panel.setPadding(10, 10, 10, 10);

        TextView tvT = new TextView(this);
        tvT.setText("AUXILIO MIRA");
        tvT.setTextSize(11);
        tvT.setTypeface(null, Typeface.BOLD);
        tvT.setTextColor(0xFFFF0000);
        tvT.setGravity(Gravity.CENTER);
        panel.addView(tvT);

        ScrollView sv = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(4, 6, 4, 6);

        TextView tvSel = tv("Selecciona modulos:", 9, 0xFFFF6600);
        content.addView(tvSel);

        cbTouch  = cb("Puntero (" + AllCommands.CMDS_TOUCH_POINTER.length + ")");
        cbGyro   = cb("Giroscopio (" + AllCommands.CMDS_GYRO_SENSOR.length + ")");
        cbFps    = cb("FPS Max (" + AllCommands.CMDS_FPS_PANTALLA.length + ")");
        cbCpu    = cb("CPU/RAM (" + AllCommands.CMDS_RENDIMIENTO_CPU.length + ")");
        cbRed    = cb("Anti-Lag (" + AllCommands.CMDS_RED_LATENCIA.length + ")");
        cbTalk   = cb("Talkback OFF (" + AllCommands.CMDS_TALKBACK.length + ")");
        cbBat    = cb("Anti-Thermal (" + AllCommands.CMDS_BATERIA.length + ")");
        cbAudio  = cb("Audio (" + AllCommands.CMDS_AUDIO.length + ")");
        cbMedia  = cb("Mod Media (" + AllCommands.CMDS_MODS_MEDIA.length + ")");
        cbScroll = cb("Scroll (" + AllCommands.CMDS_SCROLL.length + ")");
        cbRecoil = cb("Anti-Recoil (" + AllCommands.CMDS_ANTIRECOIL.length + ")");

        content.addView(cbTouch); content.addView(cbGyro); content.addView(cbFps);
        content.addView(cbCpu); content.addView(cbRed); content.addView(cbTalk);
        content.addView(cbBat); content.addView(cbAudio); content.addView(cbMedia);
        content.addView(cbScroll); content.addView(cbRecoil);

        content.addView(sep());

        Button btnApl = btnV("APLICAR SELECCION", 0xFFCC0000, 0xFFFFFFFF);
        btnApl.setOnClickListener(v -> aplicarSeleccionados());
        content.addView(btnApl);

        Button btnTodo = btnV("APLICAR TODO", 0xFF880000, 0xFFFFFFFF);
        btnTodo.setOnClickListener(v -> aplicarTodo());
        content.addView(btnTodo);

        content.addView(sep());

        Button btnCal = btnV("Calibrar Sensi", 0xFF001A33, 0xFF00AAFF);
        btnCal.setOnClickListener(v -> calibrar());
        content.addView(btnCal);

        Button btnCache = btnV("Limpiar Cache", 0xFF1A1A00, 0xFFFFFF00);
        btnCache.setOnClickListener(v -> {
            ShizukuHelper.limpiarCache(this);
            Toast.makeText(this, "Cache limpiado", Toast.LENGTH_SHORT).show();
        });
        content.addView(btnCache);

        content.addView(sep());

        Button btnFF = btnV("Free Fire", 0xFF1A0000, 0xFFFF4400);
        btnFF.setOnClickListener(v -> abrirJuego("com.dts.freefireth", "Free Fire"));
        content.addView(btnFF);

        Button btnFFM = btnV("FF MAX", 0xFF1A0A00, 0xFFFF8800);
        btnFFM.setOnClickListener(v -> abrirJuego("com.dts.freefiremax", "Free Fire MAX"));
        content.addView(btnFFM);

        content.addView(sep());

        Button btnClose = btnV("Cerrar", 0xFF1A1A1A, 0xFF888888);
        btnClose.setOnClickListener(v -> stopSelf());
        content.addView(btnClose);

        sv.addView(content);

        int maxH = (int)(dm.heightPixels * 0.55f);
        sv.setLayoutParams(new LinearLayout.LayoutParams(-1, maxH));
        panel.addView(sv);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(bubble);
        root.addView(panel);
        floatingRoot = root;

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
            (int)(dm.widthPixels * 0.52f),
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
        if (lista.isEmpty()) { Toast.makeText(this,"Selecciona al menos uno",Toast.LENGTH_SHORT).show(); return; }
        Toast.makeText(this,"Aplicando "+lista.size()+" cmds",Toast.LENGTH_SHORT).show();
        ShizukuHelper.aplicarComandosAsync(this, lista.toArray(new String[0]),
            new ShizukuHelper.ProgressCallback() {
                public void onProgress(int a, int t, String c) {}
                public void onComplete(int t) { Toast.makeText(FloatingWindowService.this,t+" cmds aplicados",Toast.LENGTH_SHORT).show(); }
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
        for (String c:AllCommands.CMDS_ANTIRECOIL) all.add(c);
        Toast.makeText(this,"Aplicando todo...",Toast.LENGTH_SHORT).show();
        ShizukuHelper.aplicarComandosAsync(this, all.toArray(new String[0]),
            new ShizukuHelper.ProgressCallback() {
                public void onProgress(int a, int t, String c) {}
                public void onComplete(int t) { Toast.makeText(FloatingWindowService.this,"Todo aplicado: "+t,Toast.LENGTH_SHORT).show(); }
                public void onError(String c, String e) {}
            }, uiHandler);
    }

    private void calibrar() {
        SensibilidadCalibrator.ResultadoCalib r = SensibilidadCalibrator.calibrar(this);
        Toast.makeText(this,r.perfil,Toast.LENGTH_SHORT).show();
        ShizukuHelper.aplicarComandosAsync(this, r.comandos.toArray(new String[0]),
            new ShizukuHelper.ProgressCallback() {
                public void onProgress(int a, int t, String c) {}
                public void onComplete(int t) { Toast.makeText(FloatingWindowService.this,"Calibracion lista",Toast.LENGTH_SHORT).show(); }
                public void onError(String c, String e) {}
            }, uiHandler);
    }

    private void abrirJuego(String pkg, String nombre) {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i != null) { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); }
            else Toast.makeText(this,nombre+" no instalado",Toast.LENGTH_SHORT).show();
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
        c.setText(t); c.setTextSize(9); c.setTextColor(0xFFDDDDDD);
        c.setButtonTintList(android.content.res.ColorStateList.valueOf(0xFFFF4400));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,1,0,1); c.setLayoutParams(lp);
        return c;
    }
    private Button btnV(String t, int bg, int fg) {
        Button b = new Button(this);
        b.setText(t); b.setTextSize(9); b.setTextColor(fg); b.setBackgroundColor(bg);
        b.setPadding(0,10,0,10);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,3,0,3); b.setLayoutParams(lp);
        return b;
    }
    private View sep() {
        View v = new View(this);
        v.setBackgroundColor(0xFF330000);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,1);
        lp.setMargins(0,5,0,5); v.setLayoutParams(lp);
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
