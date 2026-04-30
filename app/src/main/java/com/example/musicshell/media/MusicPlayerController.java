package com.example.musicshell.media;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

/**
 * 本地音乐播放控制器。
 *
 * <p>基于系统 {@link MediaPlayer}，封装播放、暂停、切换歌曲等操作。
 * 使用 {@link MediaPlayer#prepareAsync()} 异步准备，避免阻塞 UI 线程。</p>
 *
 * <p>生命周期：Activity 创建时初始化，.onDestroy() 时必须调用 {@link #release()} 释放资源。</p>
 */
public class MusicPlayerController {

    /**
     * 播放状态回调接口。
     */
    public interface PlaybackCallback {
        /** 播放开始或恢复播放时回调 */
        void onPlaybackStarted(@NonNull LocalAudioTrack track);
        /** 播放暂停时回调 */
        void onPlaybackPaused(@NonNull LocalAudioTrack track);
        /** 播放停止（切歌或释放）时回调 */
        void onPlaybackStopped();
        /** 播放出错时回调 */
        void onPlaybackError(@NonNull LocalAudioTrack track, @NonNull String errorMessage);
    }

    @NonNull
    private final Context context;
    @Nullable
    private MediaPlayer mediaPlayer;
    @Nullable
    private LocalAudioTrack currentTrack;
    private boolean isPrepared = false;
    @Nullable
    private PlaybackCallback callback;

    public MusicPlayerController(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * 设置播放状态回调。
     */
    public void setCallback(@Nullable PlaybackCallback callback) {
        this.callback = callback;
    }

    /**
     * 获取当前正在播放的歌曲，未播放时返回 null。
     */
    @Nullable
    public LocalAudioTrack getCurrentTrack() {
        return currentTrack;
    }

    /**
     * 判断当前是否正在播放（已准备且正在播放）。
     */
    public boolean isPlaying() {
        return mediaPlayer != null && isPrepared && mediaPlayer.isPlaying();
    }

    /**
     * 播放指定歌曲。
     *
     * <p>如果点击的是当前正在播放的歌曲，则暂停播放；
     * 如果点击的是其他歌曲，释放旧实例后播放新歌曲。</p>
     *
     * @param track 要播放的本地音频
     */
    public void playOrPause(@NonNull LocalAudioTrack track) {
        // 点击当前正在播放的歌曲，切换暂停/继续
        if (currentTrack != null && currentTrack.getId() == track.getId()) {
            togglePauseResume();
            return;
        }

        // 切换到新歌曲，释放旧实例后播放
        releasePlayer();
        startPlayback(track);
    }

    /**
     * 暂停当前播放。
     */
    public void pause() {
        if (mediaPlayer != null && isPrepared && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            if (currentTrack != null && callback != null) {
                callback.onPlaybackPaused(currentTrack);
            }
        }
    }

    /**
     * 继续播放。
     */
    public void resume() {
        if (mediaPlayer != null && isPrepared && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            if (currentTrack != null && callback != null) {
                callback.onPlaybackStarted(currentTrack);
            }
        }
    }

    /**
     * 释放播放器资源。Activity.onDestroy() 时必须调用。
     */
    public void release() {
        releasePlayer();
        callback = null;
    }

    /**
     * 切换暂停/继续状态。
     */
    private void togglePauseResume() {
        if (mediaPlayer == null || !isPrepared) {
            return;
        }

        if (mediaPlayer.isPlaying()) {
            pause();
        } else {
            resume();
        }
    }

    /**
     * 开始播放新歌曲。
     */
    private void startPlayback(@NonNull LocalAudioTrack track) {
        currentTrack = track;
        isPrepared = false;

        mediaPlayer = new MediaPlayer();
        configureAudioAttributes(mediaPlayer);

        mediaPlayer.setOnPreparedListener(mp -> {
            isPrepared = true;
            mp.start();
            if (callback != null) {
                callback.onPlaybackStarted(track);
            }
        });

        mediaPlayer.setOnCompletionListener(mp -> {
            // 播放完成，通知停止状态
            if (callback != null) {
                callback.onPlaybackStopped();
            }
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            isPrepared = false;
            String errorMsg = "播放出错 (what=" + what + ", extra=" + extra + ")";
            if (callback != null) {
                callback.onPlaybackError(track, errorMsg);
            }
            // 出错后释放资源
            releasePlayer();
            return true;
        });

        try {
            Uri contentUri = track.getContentUri();
            mediaPlayer.setDataSource(context, contentUri);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            if (callback != null) {
                callback.onPlaybackError(track, "无法读取音频文件");
            }
            releasePlayer();
        }
    }

    /**
     * 配置音频属性，设置为音乐类型。
     */
    private void configureAudioAttributes(@NonNull MediaPlayer player) {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        player.setAudioAttributes(attributes);
    }

    /**
     * 释放 MediaPlayer 实例并重置状态。
     */
    private void releasePlayer() {
        if (mediaPlayer != null) {
            try {
                if (isPrepared) {
                    mediaPlayer.stop();
                }
            } catch (IllegalStateException ignored) {
                // 忽略 stop 时的异常
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isPrepared = false;
        LocalAudioTrack previousTrack = currentTrack;
        currentTrack = null;

        if (previousTrack != null && callback != null) {
            callback.onPlaybackStopped();
        }
    }
}
