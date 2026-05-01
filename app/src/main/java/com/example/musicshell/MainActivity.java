package com.example.musicshell;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.musicshell.media.LocalAudioScanner;
import com.example.musicshell.media.LocalAudioTrack;
import com.example.musicshell.media.PlaybackService;
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
 * 通过绑定 PlaybackService 实现后台播放、通知栏控制等功能。</p>
 */
public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "musicshell_prefs";
    private static final String KEY_AGREEMENT_ACCEPTED = "agreement_accepted";
    private static final int REQUEST_AUDIO_PERMISSION = 1001;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1002;
    /** 进度更新间隔（毫秒） */
    private static final int PROGRESS_UPDATE_INTERVAL = 500;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService scanExecutor = Executors.newSingleThreadExecutor();
    private final LocalAudioScanner audioScanner = new LocalAudioScanner();

    private SharedPreferences preferences;
    private MaterialButton scanButton;
    private ProgressBar scanProgress;
    private TextView scanStatusText;
    private ListView audioListView;
    private LocalAudioAdapter audioAdapter;

    // 服务绑定相关
    private PlaybackService playbackService;
    private boolean serviceBound = false;

    // 迷你播放栏视图
    private MaterialCardView miniPlayerCard;
    private TextView miniPlayerTitle;
    private TextView miniPlayerArtist;
    private MaterialButton miniPlayerPlayPauseButton;
    private MaterialButton miniPlayerPreviousButton;
    private MaterialButton miniPlayerNextButton;
    private SeekBar miniPlayerSeekBar;
    private TextView miniPlayerCurrentTime;
    private TextView miniPlayerTotalTime;

    /** 进度更新定时器 */
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            progressHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL);
        }
    };
    /** 用户是否正在拖动 SeekBar */
    private boolean isUserSeeking = false;

    /** 服务连接回调 */
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlaybackService.PlaybackBinder binder = (PlaybackService.PlaybackBinder) service;
            playbackService = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            playbackService = null;
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // 绑定播放服务
        bindPlaybackService();

        bindHomeViews();
        bindHomeActions();

        if (!preferences.getBoolean(KEY_AGREEMENT_ACCEPTED, false)) {
            showAgreementDialog();
        }
    }

    @Override
    protected void onDestroy() {
        // 停止进度更新定时器
        progressHandler.removeCallbacks(progressRunnable);
        // 解绑服务
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
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
        if (requestCode == REQUEST_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocalAudioScan();
            } else {
                showPermissionDeniedState();
            }
        } else if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            // 通知权限不影响核心功能，仅记录
        }
    }

    /**
     * 绑定播放服务。
     */
    private void bindPlaybackService() {
        Intent intent = new Intent(this, PlaybackService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
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
        miniPlayerPreviousButton = findViewById(R.id.button_mini_player_previous);
        miniPlayerNextButton = findViewById(R.id.button_mini_player_next);
        miniPlayerSeekBar = findViewById(R.id.seekbar_mini_player);
        miniPlayerCurrentTime = findViewById(R.id.text_mini_player_current_time);
        miniPlayerTotalTime = findViewById(R.id.text_mini_player_total_time);
    }

    private void bindHomeActions() {
        MaterialButton aboutButton = findViewById(R.id.button_about);

        scanButton.setOnClickListener(view -> scanLocalMusicWithPermission());
        aboutButton.setOnClickListener(view -> showAboutDialog());

        // 列表项点击播放
        audioAdapter.setOnTrackClickListener((track, position) -> {
            if (playbackService != null) {
                playbackService.playOrPause(track);
                // 启动进度更新
                progressHandler.post(progressRunnable);
                // 更新 UI
                updateMiniPlayerState(track, playbackService.isPlaying());
                audioAdapter.setCurrentPlayingTrackId(track.getId());
            }
        });

        // 迷你播放栏播放/暂停按钮
        miniPlayerPlayPauseButton.setOnClickListener(view -> {
            if (playbackService != null) {
                LocalAudioTrack currentTrack = playbackService.getCurrentTrack();
                if (currentTrack != null) {
                    playbackService.playOrPause(currentTrack);
                    // 更新 UI
                    updateMiniPlayerState(currentTrack, playbackService.isPlaying());
                }
            }
        });

        // 上一首按钮
        miniPlayerPreviousButton.setOnClickListener(view -> {
            if (playbackService != null) {
                playbackService.playPrevious();
            }
        });

        // 下一首按钮
        miniPlayerNextButton.setOnClickListener(view -> {
            if (playbackService != null) {
                playbackService.playNext();
            }
        });

        // SeekBar 拖动监听
        miniPlayerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 用户开始拖动，暂停进度更新
                isUserSeeking = true;
                progressHandler.removeCallbacks(progressRunnable);
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 拖动时实时更新时间显示
                if (fromUser) {
                    miniPlayerCurrentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 用户松手，跳转到指定位置并恢复进度更新
                isUserSeeking = false;
                if (playbackService != null) {
                    playbackService.seekTo(seekBar.getProgress());
                }
                progressHandler.post(progressRunnable);
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

    /**
     * 请求通知权限（Android 13+）。
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
            }
        }
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

        // 设置播放列表到服务
        if (playbackService != null) {
            playbackService.setPlaylist(tracks);
        }

        if (tracks.isEmpty()) {
            audioListView.setVisibility(View.GONE);
            scanStatusText.setText(R.string.scan_music_empty);
        } else {
            audioListView.setVisibility(View.VISIBLE);
            scanStatusText.setText(getString(R.string.scan_music_success, tracks.size()));
        }
        scanStatusText.setVisibility(View.VISIBLE);

        // 请求通知权限
        requestNotificationPermission();
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

        // 更新按钮状态
        updateButtonStates();
    }

    /**
     * 隐藏迷你播放栏。
     */
    private void hideMiniPlayer() {
        miniPlayerCard.setVisibility(View.GONE);
    }

    /**
     * 更新进度条和时间显示。
     */
    private void updateProgress() {
        if (playbackService == null || isUserSeeking) {
            return;
        }

        int currentPosition = playbackService.getCurrentPosition();
        int duration = playbackService.getDuration();

        // 更新 SeekBar
        miniPlayerSeekBar.setMax(duration);
        miniPlayerSeekBar.setProgress(currentPosition);

        // 更新时间显示
        miniPlayerCurrentTime.setText(formatTime(currentPosition));
        miniPlayerTotalTime.setText(formatTime(duration));

        // 检查播放状态，更新 UI
        LocalAudioTrack currentTrack = playbackService.getCurrentTrack();
        if (currentTrack != null) {
            boolean isPlaying = playbackService.isPlaying();
            updateMiniPlayerState(currentTrack, isPlaying);
            audioAdapter.setCurrentPlayingTrackId(currentTrack.getId());
        } else {
            hideMiniPlayer();
            audioAdapter.setCurrentPlayingTrackId(-1);
            progressHandler.removeCallbacks(progressRunnable);
        }
    }

    /**
     * 更新按钮状态（上一首/下一首/播放暂停）。
     *
     * <p>规则：
     * - 播放列表为空时，三按钮全部禁用
     * - 播放列表只有一首时，上一首/下一首禁用
     * - 暂停状态下，上一首/下一首仍可用</p>
     */
    private void updateButtonStates() {
        if (playbackService == null) {
            return;
        }

        int playlistSize = playbackService.getPlaylistSize();
        boolean hasPlaylist = playlistSize > 0;
        boolean hasMultipleTracks = playlistSize > 1;

        // 上一首/下一首：列表不为空且有多首歌时可用
        miniPlayerPreviousButton.setEnabled(hasMultipleTracks);
        miniPlayerNextButton.setEnabled(hasMultipleTracks);

        // 播放/暂停：列表不为空时可用
        miniPlayerPlayPauseButton.setEnabled(hasPlaylist);
    }

    /**
     * 格式化时间（毫秒转为 m:ss 格式）。
     *
     * @param millis 毫秒数
     * @return 格式化后的时间字符串
     */
    @NonNull
    private String formatTime(int millis) {
        int totalSeconds = Math.max(0, millis / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
