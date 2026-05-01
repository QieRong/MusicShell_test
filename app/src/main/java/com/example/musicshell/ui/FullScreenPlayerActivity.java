package com.example.musicshell.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.palette.graphics.Palette;

import com.example.musicshell.R;
import com.example.musicshell.media.LocalAudioTrack;
import com.example.musicshell.media.MusicPlayerController;
import com.example.musicshell.media.PlaybackService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

/**
 * 全屏播放页。
 *
 * <p>展示当前播放歌曲的封面、歌名、歌手、进度条、控制按钮和播放模式切换。</p>
 */
public class FullScreenPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_ARTIST = "extra_artist";
    public static final String EXTRA_CONTENT_URI = "extra_content_uri";

    /** 进度更新间隔（毫秒） */
    private static final int PROGRESS_UPDATE_INTERVAL = 500;

    // 视图
    private View layoutRoot;
    private ShapeableImageView coverImage;
    private TextView titleText;
    private TextView artistText;
    private SeekBar seekBar;
    private TextView currentTimeText;
    private TextView totalTimeText;
    private MaterialButton playPauseButton;
    private MaterialButton playModeButton;

    // 服务绑定
    private PlaybackService playbackService;
    private boolean serviceBound = false;

    // 进度更新
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            progressHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL);
        }
    };
    private boolean isUserSeeking = false;

    // 播放模式
    private MusicPlayerController.PlayMode currentPlayMode = MusicPlayerController.PlayMode.REPEAT_ALL;

    /** 服务连接回调 */
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlaybackService.PlaybackBinder binder = (PlaybackService.PlaybackBinder) service;
            playbackService = binder.getService();
            serviceBound = true;
            // 初始化 UI
            updateUI();
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
        setContentView(R.layout.activity_full_screen_player);

        // 初始化视图
        initViews();

        // 绑定播放服务
        bindPlaybackService();

        // 设置点击事件
        setupClickListeners();

        // 加载封面
        loadCoverImage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 启动进度更新
        progressHandler.post(progressRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 停止进度更新
        progressHandler.removeCallbacks(progressRunnable);
    }

    @Override
    protected void onDestroy() {
        // 停止进度更新
        progressHandler.removeCallbacks(progressRunnable);
        // 解绑服务
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
        super.onDestroy();
    }

    /**
     * 初始化视图。
     */
    private void initViews() {
        layoutRoot = findViewById(R.id.layout_root);
        coverImage = findViewById(R.id.image_album_cover);
        titleText = findViewById(R.id.text_title);
        artistText = findViewById(R.id.text_artist);
        seekBar = findViewById(R.id.seekbar_progress);
        currentTimeText = findViewById(R.id.text_current_time);
        totalTimeText = findViewById(R.id.text_total_time);
        playPauseButton = findViewById(R.id.button_play_pause);
        playModeButton = findViewById(R.id.button_play_mode);

        // 设置标题和歌手
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String artist = getIntent().getStringExtra(EXTRA_ARTIST);
        if (title != null) {
            titleText.setText(title);
        }
        if (artist != null) {
            artistText.setText(artist);
        }
    }

    /**
     * 绑定播放服务。
     */
    private void bindPlaybackService() {
        Intent intent = new Intent(this, PlaybackService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * 设置点击事件。
     */
    private void setupClickListeners() {
        // 返回按钮
        findViewById(R.id.button_back).setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // 播放/暂停按钮
        playPauseButton.setOnClickListener(v -> {
            if (playbackService != null) {
                LocalAudioTrack currentTrack = playbackService.getCurrentTrack();
                if (currentTrack != null) {
                    playbackService.playOrPause(currentTrack);
                    updatePlayPauseButton();
                }
            }
        });

        // 上一首按钮
        findViewById(R.id.button_previous).setOnClickListener(v -> {
            if (playbackService != null) {
                playbackService.playPrevious();
                updateUI();
            }
        });

        // 下一首按钮
        findViewById(R.id.button_next).setOnClickListener(v -> {
            if (playbackService != null) {
                playbackService.playNext();
                updateUI();
            }
        });

        // 播放模式切换按钮
        playModeButton.setOnClickListener(v -> {
            switchPlayMode();
        });

        // SeekBar 拖动监听
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
                progressHandler.removeCallbacks(progressRunnable);
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentTimeText.setText(formatTime(progress));
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;
                if (playbackService != null) {
                    playbackService.seekTo(seekBar.getProgress());
                }
                progressHandler.post(progressRunnable);
            }
        });
    }

    /**
     * 加载封面图片。
     */
    private void loadCoverImage() {
        String contentUriString = getIntent().getStringExtra(EXTRA_CONTENT_URI);
        if (contentUriString == null) {
            // 没有封面，显示默认占位图
            showDefaultCover();
            return;
        }

        Uri contentUri = Uri.parse(contentUriString);
        Bitmap coverBitmap = null;

        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(this, contentUri);
            byte[] artBytes = retriever.getEmbeddedPicture();
            if (artBytes != null) {
                coverBitmap = android.graphics.BitmapFactory.decodeByteArray(artBytes, 0, artBytes.length);
            }
            retriever.release();
        } catch (Exception e) {
            // 提取失败
        }

        if (coverBitmap != null) {
            coverImage.setImageBitmap(coverBitmap);
            // 使用 Palette 提取主色并设置渐变背景
            extractColorAndSetBackground(coverBitmap);
        } else {
            showDefaultCover();
        }
    }

    /**
     * 显示默认封面占位图。
     */
    private void showDefaultCover() {
        coverImage.setImageResource(R.drawable.ic_music_note);
        coverImage.setBackgroundColor(getColor(R.color.album_cover_placeholder));
        // 使用默认背景色
        setDefaultBackground();
    }

    /**
     * 使用 Palette 提取主色并设置渐变背景。
     */
    private void extractColorAndSetBackground(Bitmap bitmap) {
        Palette.from(bitmap).generate(palette -> {
            if (palette != null) {
                int dominantColor = palette.getDominantColor(getColor(R.color.musicshell_primary));
                setGradientBackground(dominantColor);
            } else {
                setDefaultBackground();
            }
        });
    }

    /**
     * 设置渐变背景。
     */
    private void setGradientBackground(int topColor) {
        int bottomColor;
        // 根据深色模式选择底部颜色
        int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            bottomColor = getColor(R.color.player_background_dark);
        } else {
            bottomColor = getColor(R.color.player_background_light);
        }

        GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{topColor, bottomColor}
        );
        layoutRoot.setBackground(gradientDrawable);
    }

    /**
     * 设置默认背景。
     */
    private void setDefaultBackground() {
        int bottomColor;
        int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            bottomColor = getColor(R.color.player_background_dark);
        } else {
            bottomColor = getColor(R.color.player_background_light);
        }

        int topColor = getColor(R.color.musicshell_primary);
        GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{topColor, bottomColor}
        );
        layoutRoot.setBackground(gradientDrawable);
    }

    /**
     * 更新 UI。
     */
    private void updateUI() {
        if (playbackService == null) {
            return;
        }

        LocalAudioTrack currentTrack = playbackService.getCurrentTrack();
        if (currentTrack != null) {
            titleText.setText(currentTrack.getTitle());
            artistText.setText(currentTrack.getArtist());
        }

        updatePlayPauseButton();
        updatePlayModeButton();
    }

    /**
     * 更新播放/暂停按钮图标。
     */
    private void updatePlayPauseButton() {
        if (playbackService != null) {
            boolean isPlaying = playbackService.isPlaying();
            playPauseButton.setIconResource(
                    isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
        }
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

        seekBar.setMax(duration);
        seekBar.setProgress(currentPosition);

        currentTimeText.setText(formatTime(currentPosition));
        totalTimeText.setText(formatTime(duration));
    }

    /**
     * 切换播放模式。
     *
     * <p>列表循环 → 单曲循环 → 随机播放 → 列表循环</p>
     */
    private void switchPlayMode() {
        switch (currentPlayMode) {
            case REPEAT_ALL:
                currentPlayMode = MusicPlayerController.PlayMode.REPEAT_ONE;
                Toast.makeText(this, R.string.play_mode_repeat_one, Toast.LENGTH_SHORT).show();
                break;
            case REPEAT_ONE:
                currentPlayMode = MusicPlayerController.PlayMode.SHUFFLE;
                Toast.makeText(this, R.string.play_mode_shuffle, Toast.LENGTH_SHORT).show();
                break;
            case SHUFFLE:
                currentPlayMode = MusicPlayerController.PlayMode.REPEAT_ALL;
                Toast.makeText(this, R.string.play_mode_repeat, Toast.LENGTH_SHORT).show();
                break;
        }

        // 更新播放控制器的播放模式
        if (playbackService != null) {
            playbackService.setPlayMode(currentPlayMode);
        }

        updatePlayModeButton();
    }

    /**
     * 更新播放模式按钮图标。
     */
    private void updatePlayModeButton() {
        switch (currentPlayMode) {
            case REPEAT_ALL:
                playModeButton.setIconResource(R.drawable.ic_repeat);
                break;
            case REPEAT_ONE:
                playModeButton.setIconResource(R.drawable.ic_repeat_one);
                break;
            case SHUFFLE:
                playModeButton.setIconResource(R.drawable.ic_shuffle);
                break;
        }
    }

    /**
     * 格式化时间（毫秒转为 m:ss 格式）。
     */
    @NonNull
    private String formatTime(int millis) {
        int totalSeconds = Math.max(0, millis / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * 启动全屏播放页。
     *
     * @param context    上下文
     * @param title      歌名
     * @param artist     歌手
     * @param contentUri 音频文件 URI
     */
    public static void start(@NonNull Context context, @NonNull String title, @NonNull String artist, @NonNull String contentUri) {
        Intent intent = new Intent(context, FullScreenPlayerActivity.class);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_ARTIST, artist);
        intent.putExtra(EXTRA_CONTENT_URI, contentUri);
        context.startActivity(intent);
    }
}
