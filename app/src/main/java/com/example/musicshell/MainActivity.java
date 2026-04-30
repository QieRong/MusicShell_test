package com.example.musicshell;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.musicshell.media.LocalAudioScanner;
import com.example.musicshell.media.LocalAudioTrack;
import com.example.musicshell.media.MusicPlayerController;
import com.example.musicshell.ui.LocalAudioAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MusicShell 的主入口。
 *
 * <p>负责首次启动协议、本地音频读取权限和当前阶段的本地音乐扫描展示。
 * 集成播放器控制器，实现点击歌曲播放/暂停的最小闭环。</p>
 */
public class MainActivity extends AppCompatActivity implements MusicPlayerController.PlaybackCallback {

    private static final String PREFS_NAME = "musicshell_prefs";
    private static final String KEY_AGREEMENT_ACCEPTED = "agreement_accepted";
    private static final int REQUEST_AUDIO_PERMISSION = 1001;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService scanExecutor = Executors.newSingleThreadExecutor();
    private final LocalAudioScanner audioScanner = new LocalAudioScanner();

    private SharedPreferences preferences;
    private MaterialButton scanButton;
    private ProgressBar scanProgress;
    private TextView scanStatusText;
    private ListView audioListView;
    private LocalAudioAdapter audioAdapter;
    private MusicPlayerController playerController;
    private MaterialCardView miniPlayerCard;
    private TextView miniPlayerTitle;
    private TextView miniPlayerArtist;
    private MaterialButton miniPlayerPlayPauseButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        playerController = new MusicPlayerController(this);
        playerController.setCallback(this);

        bindHomeViews();
        bindHomeActions();

        if (!preferences.getBoolean(KEY_AGREEMENT_ACCEPTED, false)) {
            showAgreementDialog();
        }
    }

    @Override
    protected void onDestroy() {
        if (playerController != null) {
            playerController.release();
        }
        scanExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_AUDIO_PERMISSION) {
            return;
        }

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocalAudioScan();
        } else {
            showPermissionDeniedState();
        }
    }

    private void bindHomeViews() {
        scanButton = findViewById(R.id.button_scan_local_music);
        scanProgress = findViewById(R.id.progress_scan);
        scanStatusText = findViewById(R.id.text_scan_status);
        audioListView = findViewById(R.id.list_local_audio);
        audioAdapter = new LocalAudioAdapter(this);
        audioListView.setAdapter(audioAdapter);

        // 迷你播放栏视图
        miniPlayerCard = findViewById(R.id.card_mini_player);
        miniPlayerTitle = findViewById(R.id.text_mini_player_title);
        miniPlayerArtist = findViewById(R.id.text_mini_player_artist);
        miniPlayerPlayPauseButton = findViewById(R.id.button_mini_player_play_pause);
    }

    private void bindHomeActions() {
        MaterialButton aboutButton = findViewById(R.id.button_about);

        scanButton.setOnClickListener(view -> scanLocalMusicWithPermission());
        aboutButton.setOnClickListener(view -> showAboutDialog());

        // 列表项点击播放
        audioAdapter.setOnTrackClickListener((track, position) -> {
            if (playerController != null) {
                playerController.playOrPause(track);
            }
        });

        // 迷你播放栏播放/暂停按钮
        miniPlayerPlayPauseButton.setOnClickListener(view -> {
            if (playerController != null) {
                LocalAudioTrack currentTrack = playerController.getCurrentTrack();
                if (currentTrack != null) {
                    playerController.playOrPause(currentTrack);
                }
            }
        });
    }

    private void scanLocalMusicWithPermission() {
        if (hasAudioReadPermission()) {
            startLocalAudioScan();
            return;
        }
        requestPermissions(new String[]{getAudioReadPermission()}, REQUEST_AUDIO_PERMISSION);
    }

    private boolean hasAudioReadPermission() {
        return checkSelfPermission(getAudioReadPermission()) == PackageManager.PERMISSION_GRANTED;
    }

    @NonNull
    private String getAudioReadPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_AUDIO;
        }
        return Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private void startLocalAudioScan() {
        showLoadingState();
        scanExecutor.execute(() -> {
            try {
                List<LocalAudioTrack> tracks = audioScanner.scan(this);
                mainHandler.post(() -> showScanResult(tracks));
            } catch (RuntimeException exception) {
                mainHandler.post(this::showScanFailedState);
            }
        });
    }

    private void showLoadingState() {
        scanButton.setEnabled(false);
        scanProgress.setVisibility(View.VISIBLE);
        scanStatusText.setText(R.string.scan_music_loading);
        scanStatusText.setVisibility(View.VISIBLE);
    }

    private void showScanResult(@NonNull List<LocalAudioTrack> tracks) {
        scanButton.setEnabled(true);
        scanProgress.setVisibility(View.GONE);
        audioAdapter.submitList(tracks);

        if (tracks.isEmpty()) {
            audioListView.setVisibility(View.GONE);
            scanStatusText.setText(R.string.scan_music_empty);
        } else {
            audioListView.setVisibility(View.VISIBLE);
            scanStatusText.setText(getString(R.string.scan_music_success, tracks.size()));
        }
        scanStatusText.setVisibility(View.VISIBLE);
    }

    private void showScanFailedState() {
        scanButton.setEnabled(true);
        scanProgress.setVisibility(View.GONE);
        audioListView.setVisibility(View.GONE);
        scanStatusText.setText(R.string.scan_music_failed);
        scanStatusText.setVisibility(View.VISIBLE);
    }

    private void showPermissionDeniedState() {
        scanProgress.setVisibility(View.GONE);
        audioListView.setVisibility(View.GONE);
        scanStatusText.setText(R.string.audio_permission_denied);
        scanStatusText.setVisibility(View.VISIBLE);
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

    // ========== MusicPlayerController.PlaybackCallback 实现 ==========

    @Override
    public void onPlaybackStarted(@NonNull LocalAudioTrack track) {
        updateMiniPlayerState(track, true);
        audioAdapter.setCurrentPlayingTrackId(track.getId());
    }

    @Override
    public void onPlaybackPaused(@NonNull LocalAudioTrack track) {
        updateMiniPlayerState(track, false);
    }

    @Override
    public void onPlaybackStopped() {
        hideMiniPlayer();
        audioAdapter.setCurrentPlayingTrackId(-1);
    }

    @Override
    public void onPlaybackError(@NonNull LocalAudioTrack track, @NonNull String errorMessage) {
        hideMiniPlayer();
        audioAdapter.setCurrentPlayingTrackId(-1);
        showPlaybackError(errorMessage);
    }

    // ========== 迷你播放栏状态管理 ==========

    /**
     * 更新迷你播放栏的显示状态。
     *
     * @param track 当前播放的歌曲
     * @param isPlaying 是否正在播放
     */
    private void updateMiniPlayerState(@NonNull LocalAudioTrack track, boolean isPlaying) {
        miniPlayerCard.setVisibility(View.VISIBLE);
        miniPlayerTitle.setText(track.getTitle());
        miniPlayerArtist.setText(track.getArtist());
        miniPlayerPlayPauseButton.setIconResource(
                isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
    }

    /**
     * 隐藏迷你播放栏。
     */
    private void hideMiniPlayer() {
        miniPlayerCard.setVisibility(View.GONE);
    }

    /**
     * 显示播放错误提示。
     */
    private void showPlaybackError(@NonNull String errorMessage) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.playback_error_title)
                .setMessage(getString(R.string.playback_error, errorMessage))
                .setPositiveButton(R.string.dialog_confirm, null)
                .show();
    }
}
