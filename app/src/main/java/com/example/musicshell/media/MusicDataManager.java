package com.example.musicshell.media;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 音乐数据管理器（单例模式）。
 *
 * <p>统一管理本地歌曲数据，提供按专辑/歌手分组查询功能。
 * 避免重复扫描，所有 Fragment 和 Activity 共享同一份数据。</p>
 */
public class MusicDataManager {

    private static volatile MusicDataManager instance;

    /** 全部歌曲列表 */
    @NonNull
    private List<LocalAudioTrack> allTracks = new ArrayList<>();

    /** 按专辑名分组的歌曲映射 */
    @NonNull
    private Map<String, List<LocalAudioTrack>> albumMap = new HashMap<>();

    /** 按歌手名分组的歌曲映射 */
    @NonNull
    private Map<String, List<LocalAudioTrack>> artistMap = new HashMap<>();

    private MusicDataManager() {}

    /**
     * 获取单例实例。
     */
    @NonNull
    public static MusicDataManager getInstance() {
        if (instance == null) {
            synchronized (MusicDataManager.class) {
                if (instance == null) {
                    instance = new MusicDataManager();
                }
            }
        }
        return instance;
    }

    /**
     * 更新全部歌曲列表，并重新构建分组映射。
     *
     * @param tracks 新的歌曲列表
     */
    public void updateTracks(@NonNull List<LocalAudioTrack> tracks) {
        allTracks.clear();
        allTracks.addAll(tracks);
        buildMaps();
    }

    /**
     * 获取全部歌曲列表。
     */
    @NonNull
    public List<LocalAudioTrack> getAllTracks() {
        return new ArrayList<>(allTracks);
    }

    /**
     * 获取按专辑名分组的歌曲映射。
     *
     * @return 专辑名 → 歌曲列表
     */
    @NonNull
    public Map<String, List<LocalAudioTrack>> getAlbumMap() {
        return new HashMap<>(albumMap);
    }

    /**
     * 获取按歌手名分组的歌曲映射。
     *
     * @return 歌手名 → 歌曲列表
     */
    @NonNull
    public Map<String, List<LocalAudioTrack>> getArtistMap() {
        return new HashMap<>(artistMap);
    }

    /**
     * 获取所有专辑名和歌曲数。
     *
     * @return 专辑名 → 歌曲数
     */
    @NonNull
    public Map<String, Integer> getAllAlbums() {
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<String, List<LocalAudioTrack>> entry : albumMap.entrySet()) {
            result.put(entry.getKey(), entry.getValue().size());
        }
        return result;
    }

    /**
     * 获取所有歌手名和歌曲数。
     *
     * @return 歌手名 → 歌曲数
     */
    @NonNull
    public Map<String, Integer> getAllArtists() {
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<String, List<LocalAudioTrack>> entry : artistMap.entrySet()) {
            result.put(entry.getKey(), entry.getValue().size());
        }
        return result;
    }

    /**
     * 获取指定专辑的歌曲列表。
     *
     * @param albumName 专辑名
     * @return 歌曲列表，不存在时返回空列表
     */
    @NonNull
    public List<LocalAudioTrack> getTracksByAlbum(@Nullable String albumName) {
        if (albumName == null || albumName.isEmpty()) {
            return new ArrayList<>();
        }
        List<LocalAudioTrack> tracks = albumMap.get(albumName);
        return tracks != null ? new ArrayList<>(tracks) : new ArrayList<>();
    }

    /**
     * 获取指定歌手的歌曲列表。
     *
     * @param artistName 歌手名
     * @return 歌曲列表，不存在时返回空列表
     */
    @NonNull
    public List<LocalAudioTrack> getTracksByArtist(@Nullable String artistName) {
        if (artistName == null || artistName.isEmpty()) {
            return new ArrayList<>();
        }
        List<LocalAudioTrack> tracks = artistMap.get(artistName);
        return tracks != null ? new ArrayList<>(tracks) : new ArrayList<>();
    }

    /**
     * 构建按专辑/歌手分组的映射。
     */
    private void buildMaps() {
        albumMap.clear();
        artistMap.clear();

        for (LocalAudioTrack track : allTracks) {
            // 按专辑分组
            String album = track.getAlbum();
            if (album == null || album.isEmpty()) {
                album = "未知专辑";
            }
            if (!albumMap.containsKey(album)) {
                albumMap.put(album, new ArrayList<>());
            }
            albumMap.get(album).add(track);

            // 按歌手分组
            String artist = track.getArtist();
            if (artist == null || artist.isEmpty()) {
                artist = "未知艺术家";
            }
            if (!artistMap.containsKey(artist)) {
                artistMap.put(artist, new ArrayList<>());
            }
            artistMap.get(artist).add(track);
        }
    }
}
