package com.example.musicshell.media;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地音乐播放控制器。
 *
 * <p>基于系统 {@link MediaPlayer}，封装播放、暂停、切换歌曲等操作。
 * 使用 {@link MediaPlayer#prepareAsync()} 异步准备，避免阻塞 UI 线程。</p>
 *
 * <p>支持播放列表管理、上一首/下一首切换、进度查询与跳转、播放模式切换。</p>
 *
 * <p>生命周期：Activity 创建时初始化，onDestroy() 时必须调用 {@link #release()} 释放资源。</p>
 */
public class MusicPlayerController {

    /**
     * 播放模式枚举。
     */
    public enum PlayMode {
        /** 列表循环 */
        REPEAT_ALL,
        /** 单曲循环 */
        REPEAT_ONE,
        /** 随机播放 */
        SHUFFLE
    }

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
        /** 歌曲自动切换（上一首/下一首）时回调 */
        void onTrackChanged(@NonNull LocalAudioTrack track);
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

    /** 当前播放列表 */
    @NonNull
    private List<LocalAudioTrack> playlist = new ArrayList<>();
    /** 当前播放索引，-1 表示无歌曲 */
    private int currentIndex = -1;
    /** 当前播放模式 */
    @NonNull
    private PlayMode playMode = PlayMode.REPEAT_ALL;

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
     * 设置播放列表并记录当前播放歌曲的索引。
     *
     * @param tracks 播放列表
     */
    public void setPlaylist(@NonNull List<LocalAudioTrack> tracks) {
        this.playlist.clear();
        this.playlist.addAll(tracks);
        // 如果当前有歌曲在播放，更新索引
        if (currentTrack != null) {
            this.currentIndex = findTrackIndex(currentTrack.getId());
        } else {
            this.currentIndex = -1;
        }
    }

    /**
     * 设置播放模式。
     *
     * @param mode 播放模式
     */
    public void setPlayMode(@NonNull PlayMode mode) {
        this.playMode = mode;
    }

    /**
     * 获取当前播放模式。
     *
     * @return 当前播放模式
     */
    @NonNull
    public PlayMode getPlayMode() {
        return playMode;
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
     * 获取当前播放位置（毫秒）。未播放时返回 0。
     */
    public int getCurrentPosition() {
        if (mediaPlayer != null && isPrepared) {
            try {
                return mediaPlayer.getCurrentPosition();
            } catch (IllegalStateException ignored) {
                // 播放器状态异常时返回 0
            }
        }
        return 0;
    }

    /**
     * 获取当前歌曲总时长（毫秒）。未播放时返回 0。
     */
    public int getDuration() {
        if (mediaPlayer != null && isPrepared) {
            try {
                return mediaPlayer.getDuration();
            } catch (IllegalStateException ignored) {
                // 播放器状态异常时返回 0
            }
        }
        return 0;
    }

    /**
     * 跳转到指定位置（毫秒）。
     *
     * @param positionMs 目标位置（毫秒）
     */
    public void seekTo(int positionMs) {
        if (mediaPlayer != null && isPrepared) {
            try {
                mediaPlayer.seekTo(positionMs);
            } catch (IllegalStateException ignored) {
                // 忽略异常
            }
        }
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
        currentIndex = findTrackIndex(track.getId());
        startPlayback(track);
    }

    /**
     * 播放上一首。
     *
     * <p>边界处理：第一首歌时循环到最后一首。</p>
     */
    public void playPrevious() {
        if (playlist.isEmpty()) {
            return;
        }

        int newIndex;
        if (currentIndex <= 0) {
            // 已是第一首，循环到最后一首
            newIndex = playlist.size() - 1;
        } else {
            newIndex = currentIndex - 1;
        }

        playAtIndex(newIndex);
    }

    /**
     * 播放下一首。
     *
     * <p>根据播放模式决定下一首：
     * - REPEAT_ALL：最后一首循环到第一首
     * - REPEAT_ONE：重新播放当前歌曲
     * - SHUFFLE：随机选择下一首</p>
     */
    public void playNext() {
        if (playlist.isEmpty()) {
            return;
        }

        int newIndex;

        switch (playMode) {
            case REPEAT_ONE:
                // 单曲循环：重新播放当前歌曲
                newIndex = currentIndex;
                break;

            case SHUFFLE:
                // 随机播放：随机选择一首（排除当前歌曲）
                if (playlist.size() == 1) {
                    newIndex = 0;
                } else {
                    do {
                        newIndex = (int) (Math.random() * playlist.size());
                    } while (newIndex == currentIndex);
                }
                break;

            case REPEAT_ALL:
            default:
                // 列表循环：最后一首循环到第一首
                if (currentIndex >= playlist.size() - 1) {
                    newIndex = 0;
                } else {
                    newIndex = currentIndex + 1;
                }
                break;
        }

        playAtIndex(newIndex);
    }

    /**
     * 获取播放列表大小。
     */
    public int getPlaylistSize() {
        return playlist.size();
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
     * 根据索引播放歌曲，释放旧实例后播放新歌曲。
     */
    private void playAtIndex(int index) {
        if (index < 0 || index >= playlist.size()) {
            return;
        }

        currentIndex = index;
        LocalAudioTrack track = playlist.get(index);
        releasePlayer();
        startPlayback(track);

        // 通知歌曲切换
        if (callback != null) {
            callback.onTrackChanged(track);
        }
    }

    /**
     * 查找歌曲在播放列表中的索引。
     *
     * @param trackId 歌曲 ID
     * @return 索引，未找到返回 -1
     */
    private int findTrackIndex(long trackId) {
        for (int i = 0; i < playlist.size(); i++) {
            if (playlist.get(i).getId() == trackId) {
                return i;
            }
        }
        return -1;
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
            // 播放完成，自动播放下一首
            if (callback != null) {
                callback.onPlaybackStopped();
            }
            playNext();
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
