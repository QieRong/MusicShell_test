package com.example.musicshell;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * MusicShell 的主入口。
 *
 * <p>负责展示首次启动协议，并承载当前阶段的合规空壳首页。</p>
 */
public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "musicshell_prefs";
    private static final String KEY_AGREEMENT_ACCEPTED = "agreement_accepted";

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        bindHomeActions();

        if (!preferences.getBoolean(KEY_AGREEMENT_ACCEPTED, false)) {
            showAgreementDialog();
        }
    }

    private void bindHomeActions() {
        MaterialButton scanButton = findViewById(R.id.button_scan_local_music);
        MaterialButton aboutButton = findViewById(R.id.button_about);

        scanButton.setOnClickListener(view -> Toast.makeText(
                this,
                R.string.scan_music_next_step,
                Toast.LENGTH_SHORT
        ).show());

        aboutButton.setOnClickListener(view -> showAboutDialog());
    }

    private void showAgreementDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.agreement_title)
                .setMessage(R.string.agreement_full_text)
                .setCancelable(false)
                .setNegativeButton(R.string.exit_app, (dialog, which) -> finish())
                .setPositiveButton(R.string.agreement_accept, (dialog, which) -> {
                    // 首次启动必须落盘记录，避免用户未确认就进入后续功能。
                    preferences.edit().putBoolean(KEY_AGREEMENT_ACCEPTED, true).apply();
                })
                .show();
    }

    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about_title)
                .setMessage(getAboutText())
                .setPositiveButton(R.string.dialog_confirm, null)
                .show();
    }

    @NonNull
    private String getAboutText() {
        return getString(R.string.about_body) + "\n\n" + getString(R.string.agreement_full_text);
    }
}
