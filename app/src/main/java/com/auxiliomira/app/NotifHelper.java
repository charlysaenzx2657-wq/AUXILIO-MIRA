package com.auxiliomira.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class NotifHelper {

    private static final String CH_ESTADO = "auxilio_estado";
    private static final String CH_AVISOS = "auxilio_avisos";
    private static int counter = 100;

    public static void crearCanales(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            NotificationChannel chEstado = new NotificationChannel(
                CH_ESTADO, "AUXILIO MIRA - Estado", NotificationManager.IMPORTANCE_LOW);
            chEstado.setDescription("Estado de la app");
            chEstado.enableLights(true);
            chEstado.setLightColor(0xFFFF0000);
            nm.createNotificationChannel(chEstado);

            NotificationChannel chAvisos = new NotificationChannel(
                CH_AVISOS, "AUXILIO MIRA - Avisos", NotificationManager.IMPORTANCE_HIGH);
            chAvisos.setDescription("Confirmaciones de optimizaciones");
            chAvisos.enableLights(true);
            chAvisos.setLightColor(0xFFFF0000);
            chAvisos.enableVibration(true);
            chAvisos.setVibrationPattern(new long[]{0, 80, 60, 80});
            nm.createNotificationChannel(chAvisos);
        }
    }

    public static void notificar(Context ctx, String titulo, String mensaje) {
        notificarAvanzado(ctx, titulo, mensaje, false);
    }

    public static void notificarImportante(Context ctx, String titulo, String mensaje) {
        notificarAvanzado(ctx, titulo, mensaje, true);
    }

    private static void notificarAvanzado(Context ctx, String titulo, String mensaje, boolean importante) {
        crearCanales(ctx);
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        Intent openIntent = new Intent(ctx, MainActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int piFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, openIntent, piFlags);

        Notification n;
        String channel = importante ? CH_AVISOS : CH_ESTADO;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder b = new Notification.Builder(ctx, channel)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setStyle(new Notification.BigTextStyle().bigText(mensaje))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setColor(0xFFFF0000)
                .setColorized(true)
                .setContentIntent(pi)
                .setAutoCancel(true);
            if (importante) {
                b.setPriority(Notification.PRIORITY_HIGH);
                b.setVibrate(new long[]{0, 80, 60, 80});
            }
            n = b.build();
        } else {
            n = new Notification.Builder(ctx)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();
        }

        nm.notify(++counter, n);
    }
}
