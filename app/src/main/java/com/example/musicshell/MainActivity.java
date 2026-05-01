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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.musicshell.media.LocalAudioScanner;
import com.example.musicshell.media.LocalAudioTrack;
import com.example.musicshell.media.PlaybackService;
import com.example.musicshell.ui.AlbumsFragment;
import com.example.musicshell.ui.ArtistsFragment;
import com.example.musicshell.ui.SettingsFragment;
import com.example.musicshell.ui.SongsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MusicShell 的主入口。
 *
 * <p>负责首次启动协议、本地音频读取权限、Fragment 管理和底部导航。</p>
 */
public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "musicshell_prefs";
    private static final String KEY_AGREEMENT_ACCEPTED = "agreement_accepted";
    private static final int REQUEST_AUDIO_PERMISSION = 1001;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1002;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService scanExecutor = Executors.newSingleThreadExecutor();
    private final LocalAudioScanner audioScanner = new LocalAudioScanner();

    private SharedPreferences preferences;
    
    // 搜索相关
    private EditText searchEdit;
    private ImageView clearSearchButton;
    
    // Fragment 相关
    private SongsFragment songsFragment;
    private AlbumsFragment albumsFragment;
    private ArtistsFragment artistsFragment;
    private SettingsFragment settingsFragment;
    private Fragment currentFragment;
    
    // 底部导航
    private BottomNavigationView bottomNavigation;
    
    // 悬浮播放器
    private MaterialCardView miniPlayerCard;
    private MaterialButton miniPlayerPlayPauseButton;
    private com.google.android.material.imageview.ShapeableImageView miniPlayerAlbumCover;
    private android.widget.TextView miniPlayerTitle;
    private android.widget.TextView miniPlayerArtist;
    
    // 服务绑定相关
    private PlaybackService playbackService;
    private boolean serviceBound = false;
    
    // 数据
    private List<LocalAudioTrack> allTracks;

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

        // 初始化视图
        initViews();
        
        // 初始化 Fragment
        initFragments();
        
        // 设置底部导航
        setupBottomNavigation();
        
        // 设置搜索功能
        setupSearch();

        // 检查协议
        if (!preferences.getBoolean(KEY_AGREEMENT_ACCEPTED, false)) {
            showAgreementDialog();
        } else {
            // 自动扫描本地音乐
            scanLocalMusic();
        }
    }

    @Override
    protected void onDestroy() {
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
                scanLocalMusic();
            } else {
                showPermissionDeniedDialog();
            }
        }
    }

    /**
     * 初始化视图。
     */
    private void initViews() {
        searchEdit = findViewById(R.id.edit_search);
        clearSearchButton = findViewById(R.id.image_clear_search);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        
        // 悬浮播放器
        miniPlayerCard = findViewById(R.id.card_mini_player);
        miniPlayerPlayPauseButton = findViewById(R.id.button_mini_player_play_pause);
        miniPlayerAlbumCover = findViewById(R.id.image_album_cover);
        miniPlayerTitle = findViewById(R.id.text_mini_player_title);
        miniPlayerArtist = findViewById(R.id.text_mini_player_artist);
        
        // 播放/暂停按钮点击事件
        miniPlayerPlayPauseButton.setOnClickListener(v -> {
            if (playbackService != null) {
                LocalAudioTrack currentTrack = playbackService.getCurrentTrack();
                if (currentTrack != null) {
                    playbackService.playOrPause(currentTrack);
                    updateMiniPlayerUI();
                }
            }
        });
    }

    /**
     * 初始化 Fragment。
     */
    private void initFragments() {
        songsFragment = new SongsFragment();
        albumsFragment = new AlbumsFragment();
        artistsFragment = new ArtistsFragment();
        settingsFragment = new SettingsFragment();
        
        // 设置歌曲点击监听
        songsFragment.setOnTrackClickListener((track, position) -> {
            if (playbackService != null) {
                playbackService.playOrPause(track);
                updateMiniPlayerUI();
            }
        });
        
        // 默认显示歌曲 Fragment
        switchFragment(songsFragment);
    }

    /**
     * 设置底部导航。
     */
    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_songs) {
                switchFragment(songsFragment);
                return true;
            } else if (itemId == R.id.nav_albums) {
                switchFragment(albumsFragment);
                return true;
            } else if (itemId == R.id.nav_artists) {
                switchFragment(artistsFragment);
                return true;
            } else if (itemId == R.id.nav_settings) {
                switchFragment(settingsFragment);
                return true;
            }
            return false;
        });
    }

    /**
     * 设置搜索功能。
     */
    private void setupSearch() {
        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                clearSearchButton.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                
                // 过滤歌曲列表
                if (songsFragment != null) {
                    songsFragment.filterTracks(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        clearSearchButton.setOnClickListener(v -> {
            searchEdit.setText("");
        });
    }

    /**
     * 切换 Fragment。
     */
    private void switchFragment(@NonNull Fragment fragment) {
        if (fragment == currentFragment) {
            return;
        }
        
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        
        currentFragment = fragment;
    }

    /**
     * 绑定播放服务。
     */
    private void bindPlaybackService() {
        Intent intent = new Intent(this, PlaybackService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * 扫描本地音乐。
     */
    private void scanLocalMusic() {
        if (!hasAudioReadPermission()) {
            requestPermissions(new String[]{getAudioReadPermission()}, REQUEST_AUDIO_PERMISSION);
            return;
        }
        
        scanExecutor.execute(() -> {
            try {
                List<LocalAudioTrack> tracks = audioScanner.scan(this);
                mainHandler.post(() -> onScanComplete(tracks));
            } catch (RuntimeException e) {
                mainHandler.post(() -> showScanFailedDialog());
            }
        });
    }

    /**
     * 扫描完成回调。
     */
    private void onScanComplete(@NonNull List<LocalAudioTrack> tracks) {
        allTracks = tracks;
        
        // 更新歌曲 Fragment
        if (songsFragment != null) {
            songsFragment.updateTracks(tracks);
        }
        
        // 更新专辑 Fragment
        if (albumsFragment != null) {
            albumsFragment.updateTracks(tracks);
        }
        
        // 更新歌手 Fragment
        if (artistsFragment != null) {
            artistsFragment.updateTracks(tracks);
        }
        
        // 设置播放列表到服务
        if (playbackService != null) {
            playbackService.setPlaylist(tracks);
        }
        
        // 请求通知权限
        requestNotificationPermission();
    }

    /**
     * 更新迷你播放器 UI。
     */
    private void updateMiniPlayerUI() {
        if (playbackService == null) {
            miniPlayerCard.setVisibility(View.GONE);
            return;
        }
        
        LocalAudioTrack currentTrack = playbackService.getCurrentTrack();
        if (currentTrack == null) {
            miniPlayerCard.setVisibility(View.GONE);
            return;
        }
        
        miniPlayerCard.setVisibility(View.VISIBLE);
        miniPlayerTitle.setText(currentTrack.getTitle());
        miniPlayerArtist.setText(currentTrack.getArtist());
        
        boolean isPlaying = playbackService.isPlaying();
        miniPlayerPlayPauseButton.setIconResource(
                isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
        
        // 更新歌曲 Fragment 的高亮
        if (songsFragment != null) {
            songsFragment.setCurrentPlayingTrackId(currentTrack.getId());
        }
    }

    /**
     * 检查音频读取权限。
     */
    private boolean hasAudioReadPermission() {
        return checkSelfPermission(getAudioReadPermission()) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 获取音频读取权限。
     */
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

    /**
     * 显示协议对话框。
     */
    private void showAgreementDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.agreement_title)
                .setMessage(R.string.agreement_full_text)
                .setCancelable(false)
                .setNegativeButton(R.string.exit_app, (dialog, which) -> finish())
                .setPositiveButton(R.string.agreement_accept, (dialog, which) -> {
                    preferences.edit().putBoolean(KEY_AGREEMENT_ACCEPTED, true).apply();
                    scanLocalMusic();
                })
                .show();
    }

    /**
     * 显示权限被拒绝对话框。
     */
    private void showPermissionDeniedDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.audio_permission_denied)
                .setMessage(R.string.audio_permission_denied)
                .setPositiveButton(R.string.dialog_confirm, null)
                .show();
    }

    /**
     * 显示扫描失败对话框。
     */
    private void showScanFailedDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.scan_music_failed)
                .setMessage(R.string.scan_music_failed)
                .setPositiveButton(R.string.dialog_confirm, null)
                .show();
    }
}
