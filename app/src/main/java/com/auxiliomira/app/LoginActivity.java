package com.auxiliomira.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity {

    private static final String KEY = "SENSIX08";
    private static final String PREFS = "auxilio_prefs";
    private static final String KEY_LOGGED = "logged_in";

    private TextView tvStatus;
    private EditText etKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Si ya hizo login antes, va directo al main
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_LOGGED, false)) {
            iraMain();
            return;
        }

        buildUI();
    }

    private void buildUI() {
        // Fondo con gradiente rojo→negro
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        GradientDrawable bgRoot = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xFF1A0000, 0xFF000000, 0xFF1A0000});
        root.setBackground(bgRoot);
        root.setPadding(40, 40, 40, 40);

        // ── Logo grande ──
        TextView logo = new TextView(this);
        logo.setText("◉");
        logo.setTextSize(80);
        logo.setTextColor(0xFFFF0000);
        logo.setShadowLayer(20f, 0, 0, 0xFFFF0000);
        logo.setGravity(Gravity.CENTER);
        root.addView(logo);

        TextView titulo = new TextView(this);
        titulo.setText("AUXILIO MIRA");
        titulo.setTextSize(32);
        titulo.setTypeface(null, Typeface.BOLD);
        titulo.setTextColor(0xFFFFFFFF);
        titulo.setShadowLayer(15f, 0, 0, 0xFFFF0000);
        titulo.setGravity(Gravity.CENTER);
        titulo.setPadding(0, 8, 0, 4);
        root.addView(titulo);

        TextView sub = new TextView(this);
        sub.setText("FREE FIRE SYSTEM OPTIMIZER");
        sub.setTextSize(11);
        sub.setTextColor(0xFFFF6600);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 0, 0, 32);
        root.addView(sub);

        // ── Card de login ──
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bgCard = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xFF200000, 0xFF100000});
        bgCard.setStroke(3, 0xFFFF0000);
        bgCard.setCornerRadius(24);
        card.setBackground(bgCard);
        card.setElevation(20f);
        card.setPadding(28, 28, 28, 28);

        TextView lblKey = new TextView(this);
        lblKey.setText("🔐 INGRESA LA KEY DE ACCESO");
        lblKey.setTextSize(14);
        lblKey.setTypeface(null, Typeface.BOLD);
        lblKey.setTextColor(0xFFFF6600);
        lblKey.setGravity(Gravity.CENTER);
        lblKey.setPadding(0, 0, 0, 16);
        card.addView(lblKey);

        etKey = new EditText(this);
        etKey.setHint("KEY");
        etKey.setHintTextColor(0xFF555555);
        etKey.setTextColor(0xFFFFFFFF);
        etKey.setTextSize(20);
        etKey.setTypeface(null, Typeface.BOLD);
        etKey.setGravity(Gravity.CENTER);
        etKey.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_CLASS_TEXT);
        etKey.setLetterSpacing(0.3f);
        GradientDrawable bgInput = new GradientDrawable();
        bgInput.setColor(0xFF000000);
        bgInput.setStroke(2, 0xFFFF4400);
        bgInput.setCornerRadius(12);
        etKey.setBackground(bgInput);
        etKey.setPadding(20, 24, 20, 24);
        card.addView(etKey);

        // Botón
        Button btnLogin = new Button(this);
        btnLogin.setText("🔓  DESBLOQUEAR");
        btnLogin.setTextSize(16);
        btnLogin.setTypeface(null, Typeface.BOLD);
        btnLogin.setTextColor(0xFFFFFFFF);
        btnLogin.setShadowLayer(4f, 1, 1, 0x80000000);
        GradientDrawable bgBtn = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xFFFF3300, 0xFFCC0000, 0xFF880000});
        bgBtn.setStroke(2, 0x80FFFFFF);
        bgBtn.setCornerRadius(12);
        btnLogin.setBackground(bgBtn);
        btnLogin.setElevation(8f);
        btnLogin.setPadding(0, 20, 0, 20);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, -2);
        blp.setMargins(0, 20, 0, 0);
        btnLogin.setLayoutParams(blp);
        btnLogin.setOnClickListener(v -> validar());
        card.addView(btnLogin);

        tvStatus = new TextView(this);
        tvStatus.setText(" ");
        tvStatus.setTextSize(12);
        tvStatus.setTextColor(0xFFFF4444);
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setPadding(0, 14, 0, 0);
        card.addView(tvStatus);

        root.addView(card);

        // Footer
        TextView ver = new TextView(this);
        ver.setText("v4.0 | SENSIS GOOD FF studio");
        ver.setTextSize(10);
        ver.setTextColor(0xFF555555);
        ver.setGravity(Gravity.CENTER);
        ver.setPadding(0, 28, 0, 0);
        root.addView(ver);

        setContentView(root);
    }

    private void validar() {
        String input = etKey.getText().toString().trim();
        if (input.equals(KEY)) {
            tvStatus.setText("✅ ACCESO CONCEDIDO");
            tvStatus.setTextColor(0xFF00FF00);
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean(KEY_LOGGED, true).apply();
            new Handler().postDelayed(this::iraMain, 600);
        } else {
            tvStatus.setText("❌ KEY INCORRECTA");
            tvStatus.setTextColor(0xFFFF0000);
            etKey.setText("");
            // Vibración de error
            try {
                ((android.os.Vibrator) getSystemService(VIBRATOR_SERVICE))
                    .vibrate(android.os.VibrationEffect.createOneShot(200,
                        android.os.VibrationEffect.DEFAULT_AMPLITUDE));
            } catch (Throwable ignored) {}
            Toast.makeText(this, "Key incorrecta", Toast.LENGTH_SHORT).show();
        }
    }

    private void iraMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
