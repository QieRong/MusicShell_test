package com.example.musicshell.media;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 通过系统 MediaStore 扫描本地音频。
 *
 * <p>扫描结果只来自系统媒体库索引，不访问网络，不读取文件绝对路径。</p>
 */
public class LocalAudioScanner {

    /**
     * 扫描用户设备中已被系统索引的本地音频。
     *
     * @param context 用于获取 ContentResolver 的上下文
     * @return 本地音频列表，查询失败时返回空列表
     */
    @NonNull
    public List<LocalAudioTrack> scan(@NonNull Context context) {
        List<LocalAudioTrack> tracks = new ArrayList<>();
        ContentResolver resolver = context.getApplicationContext().getContentResolver();
        Uri collection = getAudioCollectionUri();
        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND "
                + MediaStore.Audio.Media.DURATION + " > 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC";

        try (Cursor cursor = resolver.query(collection, projection, selection, null, sortOrder)) {
            if (cursor == null) {
                return tracks;
            }

            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                Uri contentUri = ContentUris.withAppendedId(collection, id);
                String title = safeText(cursor.getString(titleColumn));
                String artist = safeText(cursor.getString(artistColumn));
                String album = safeText(cursor.getString(albumColumn));
                long durationMs = cursor.getLong(durationColumn);

                tracks.add(new LocalAudioTrack(id, contentUri, title, artist, album, durationMs));
            }
        }

        return tracks;
    }

    @NonNull
    private Uri getAudioCollectionUri() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        }
        return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    }

    @NonNull
    private String safeText(String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        return value.trim();
    }
}
