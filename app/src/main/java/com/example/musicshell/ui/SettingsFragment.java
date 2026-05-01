package com.example.musicshell.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.musicshell.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * 设置 Fragment。
 *
 * <p>包含主题切换、关于等功能。</p>
 */
public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 主题切换
        RadioGroup themeGroup = view.findViewById(R.id.radio_group_theme);
        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_theme_system) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            } else if (checkedId == R.id.radio_theme_light) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else if (checkedId == R.id.radio_theme_dark) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
        });
        
        // 关于按钮
        view.findViewById(R.id.button_about).setOnClickListener(v -> {
            showAboutDialog();
        });
    }

    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.about_title)
                .setMessage(getString(R.string.about_body) + "\n\n" + getString(R.string.agreement_full_text))
                .setPositiveButton(R.string.dialog_confirm, null)
                .show();
    }
}
