package com.example.musicshell.media;

import android.net.Uri;

import androidx.annotation.NonNull;

/**
 * 本地音频条目。
 *
 * <p>只保存系统媒体库公开的基础元数据，不保存文件绝对路径。</p>
 */
public class LocalAudioTrack {

    private final long id;
    @NonNull
    private final Uri contentUri;
    @NonNull
    private final String title;
    @NonNull
    private final String artist;
    @NonNull
    private final String album;
    private final long durationMs;

    public LocalAudioTrack(
            long id,
            @NonNull Uri contentUri,
            @NonNull String title,
            @NonNull String artist,
            @NonNull String album,
            long durationMs
    ) {
        this.id = id;
        this.contentUri = contentUri;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.durationMs = durationMs;
    }

    public long getId() {
        return id;
    }

    @NonNull
    public Uri getContentUri() {
        return contentUri;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getArtist() {
        return artist;
    }

    @NonNull
    public String getAlbum() {
        return album;
    }

    public long getDurationMs() {
        return durationMs;
    }
}
