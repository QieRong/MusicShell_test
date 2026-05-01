package com.example.musicshell.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.musicshell.R;
import com.example.musicshell.media.LocalAudioTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 专辑浏览 Fragment。
 *
 * <p>按专辑分组展示本地音乐。</p>
 */
public class AlbumsFragment extends Fragment {

    private GridView gridView;
    private TextView countText;
    private List<LocalAudioTrack> allTracks = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_albums, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        gridView = view.findViewById(R.id.grid_albums);
        countText = view.findViewById(R.id.text_album_count);
        
        // 更新显示
        updateDisplay();
    }

    /**
     * 更新歌曲列表（用于按专辑分组）。
     *
     * @param tracks 新的歌曲列表
     */
    public void updateTracks(@NonNull List<LocalAudioTrack> tracks) {
        allTracks.clear();
        allTracks.addAll(tracks);
        updateDisplay();
    }

    private void updateDisplay() {
        // 按专辑分组
        Map<String, List<LocalAudioTrack>> albumMap = new HashMap<>();
        for (LocalAudioTrack track : allTracks) {
            String album = track.getAlbum();
            if (album.isEmpty()) {
                album = getString(R.string.unknown_album);
            }
            if (!albumMap.containsKey(album)) {
                albumMap.put(album, new ArrayList<>());
            }
            albumMap.get(album).add(track);
        }

        // 更新专辑数量
        if (countText != null) {
            countText.setText(getString(R.string.album_count_format, albumMap.size()));
        }

        // TODO: 设置专辑网格适配器
        // 目前先显示空状态
    }
}
