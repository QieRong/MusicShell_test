package com.example.musicshell.media;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;

import com.example.musicshell.MainActivity;
import com.example.musicshell.R;

import java.util.List;

/**
 * 后台音乐播放服务。
 *
 * <p>使用前台服务保持音乐在后台播放，通过 MediaStyle 通知显示播放控制。</p>
 *
 * <p>生命周期：
 * - 绑定模式：Activity 绑定时启动，解绑时停止
 * - 前台模式：播放开始时提升为前台服务，播放停止时降级</p>
 */
public class PlaybackService extends Service implements MusicPlayerController.PlaybackCallback {

    private static final String TAG = "PlaybackService";
    private static final String CHANNEL_ID = "musicshell_playback_channel";
    private static final int NOTIFICATION_ID = 1;

    /** 服务绑定器 */
    private final IBinder binder = new PlaybackBinder();

    /** 播放控制器 */
    private MusicPlayerController playerController;
    /** MediaSession */
    private MediaSessionCompat mediaSession;
    /** 通知管理器 */
    private NotificationManager notificationManager;

    /**
     * 服务绑定器类。
     */
    public class PlaybackBinder extends Binder {
        /**
         * 获取服务实例。
         *
         * @return PlaybackService 实例
         */
        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        // 初始化通知管理器
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createNotificationChannel();
        
        // 初始化 MediaSession
        mediaSession = new MediaSessionCompat(this, TAG);
        mediaSession.setCallback(new MediaSessionCallback());
        mediaSession.setActive(true);
        
        // 初始化播放控制器
        playerController = new MusicPlayerController(this);
        playerController.setCallback(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 处理 MediaButton 事件
        MediaButtonReceiver.handleIntent(mediaSession, intent);
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        // 释放资源
        if (playerController != null) {
            playerController.release();
        }
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
        super.onDestroy();
    }

    // ========== 公开方法，供 Activity 通过绑定调用 ==========

    /**
     * 设置播放列表。
     *
     * @param tracks 播放列表
     */
    public void setPlaylist(@NonNull List<LocalAudioTrack> tracks) {
        if (playerController != null) {
            playerController.setPlaylist(tracks);
        }
    }

    /**
     * 播放或暂停指定歌曲。
     *
     * @param track 要播放的歌曲
     */
    public void playOrPause(@NonNull LocalAudioTrack track) {
        if (playerController != null) {
            playerController.playOrPause(track);
        }
    }

    /**
     * 播放上一首。
     */
    public void playPrevious() {
        if (playerController != null) {
            playerController.playPrevious();
        }
    }

    /**
     * 播放下一首。
     */
    public void playNext() {
        if (playerController != null) {
            playerController.playNext();
        }
    }

    /**
     * 暂停播放。
     */
    public void pause() {
        if (playerController != null) {
            playerController.pause();
        }
    }

    /**
     * 继续播放。
     */
    public void resume() {
        if (playerController != null) {
            playerController.resume();
        }
    }

    /**
     * 跳转到指定位置。
     *
     * @param positionMs 位置（毫秒）
     */
    public void seekTo(int positionMs) {
        if (playerController != null) {
            playerController.seekTo(positionMs);
        }
    }

    /**
     * 获取当前播放的歌曲。
     *
     * @return 当前歌曲，未播放时返回 null
     */
    @Nullable
    public LocalAudioTrack getCurrentTrack() {
        return playerController != null ? playerController.getCurrentTrack() : null;
    }

    /**
     * 判断是否正在播放。
     *
     * @return true 表示正在播放
     */
    public boolean isPlaying() {
        return playerController != null && playerController.isPlaying();
    }

    /**
     * 获取当前播放位置。
     *
     * @return 位置（毫秒）
     */
    public int getCurrentPosition() {
        return playerController != null ? playerController.getCurrentPosition() : 0;
    }

    /**
     * 获取当前歌曲总时长。
     *
     * @return 时长（毫秒）
     */
    public int getDuration() {
        return playerController != null ? playerController.getDuration() : 0;
    }

    /**
     * 获取播放列表大小。
     *
     * @return 列表大小
     */
    public int getPlaylistSize() {
        return playerController != null ? playerController.getPlaylistSize() : 0;
    }

    // ========== MusicPlayerController.PlaybackCallback 实现 ==========

    @Override
    public void onPlaybackStarted(@NonNull LocalAudioTrack track) {
        updateNotification(track, true);
        updatePlaybackState(true);
    }

    @Override
    public void onPlaybackPaused(@NonNull LocalAudioTrack track) {
        updateNotification(track, false);
        updatePlaybackState(false);
    }

    @Override
    public void onPlaybackStopped() {
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public void onPlaybackError(@NonNull LocalAudioTrack track, @NonNull String errorMessage) {
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public void onTrackChanged(@NonNull LocalAudioTrack track) {
        updateNotification(track, true);
        updatePlaybackState(true);
    }

    // ========== 通知相关方法 ==========

    /**
     * 创建通知渠道（Android 8.0+）。
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.notification_channel_description));
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * 更新通知。
     *
     * @param track 当前歌曲
     * @param isPlaying 是否正在播放
     */
    private void updateNotification(@NonNull LocalAudioTrack track, boolean isPlaying) {
        // 创建点击通知打开 Activity 的 PendingIntent
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 创建上一首 PendingIntent
        Intent prevIntent = new Intent(this, PlaybackService.class);
        prevIntent.setAction("ACTION_PREVIOUS");
        PendingIntent prevPendingIntent = PendingIntent.getService(
                this, 1, prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 创建播放/暂停 PendingIntent
        Intent playPauseIntent = new Intent(this, PlaybackService.class);
        playPauseIntent.setAction(isPlaying ? "ACTION_PAUSE" : "ACTION_PLAY");
        PendingIntent playPausePendingIntent = PendingIntent.getService(
                this, 2, playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 创建下一首 PendingIntent
        Intent nextIntent = new Intent(this, PlaybackService.class);
        nextIntent.setAction("ACTION_NEXT");
        PendingIntent nextPendingIntent = PendingIntent.getService(
                this, 3, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 构建通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentTitle(track.getTitle())
                .setContentText(track.getArtist())
                .setContentIntent(openPendingIntent)
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this, PlaybackStateCompat.ACTION_STOP))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(isPlaying)
                .addAction(R.drawable.ic_skip_previous, getString(R.string.previous_track), prevPendingIntent)
                .addAction(
                        isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_arrow,
                        isPlaying ? getString(R.string.pause) : getString(R.string.play),
                        playPausePendingIntent
                )
                .addAction(R.drawable.ic_skip_next, getString(R.string.next_track), nextPendingIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2)
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(
                                this, PlaybackStateCompat.ACTION_STOP)))
                .setPriority(NotificationCompat.PRIORITY_LOW);

        Notification notification = builder.build();

        // 提升为前台服务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    /**
     * 更新 MediaSession 播放状态。
     *
     * @param isPlaying 是否正在播放
     */
    private void updatePlaybackState(boolean isPlaying) {
        int state = isPlaying
                ? PlaybackStateCompat.STATE_PLAYING
                : PlaybackStateCompat.STATE_PAUSED;

        long position = playerController != null ? playerController.getCurrentPosition() : 0;

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
        stateBuilder.setState(state, position, 1.0f);
        stateBuilder.setActions(
                PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                PlaybackStateCompat.ACTION_SEEK_TO
        );

        mediaSession.setPlaybackState(stateBuilder.build());
    }

    // ========== MediaSession 回调 ==========

    /**
     * MediaSession 回调处理。
     */
    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            if (playerController != null) {
                playerController.resume();
            }
        }

        @Override
        public void onPause() {
            if (playerController != null) {
                playerController.pause();
            }
        }

        @Override
        public void onSkipToNext() {
            if (playerController != null) {
                playerController.playNext();
            }
        }

        @Override
        public void onSkipToPrevious() {
            if (playerController != null) {
                playerController.playPrevious();
            }
        }

        @Override
        public void onStop() {
            if (playerController != null) {
                playerController.pause();
            }
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
        }

        @Override
        public void onSeekTo(long pos) {
            if (playerController != null) {
                playerController.seekTo((int) pos);
            }
        }
    }
}
