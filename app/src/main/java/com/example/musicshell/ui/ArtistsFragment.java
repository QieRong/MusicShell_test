package com.example.musicshell.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
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
 * 歌手浏览 Fragment。
 *
 * <p>按歌手分组展示本地音乐。</p>
 */
public class ArtistsFragment extends Fragment {

    private ListView listView;
    private TextView countText;
    private List<LocalAudioTrack> allTracks = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_artists, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        listView = view.findViewById(R.id.list_artists);
        countText = view.findViewById(R.id.text_artist_count);
        
        // 更新显示
        updateDisplay();
    }

    /**
     * 更新歌曲列表（用于按歌手分组）。
     *
     * @param tracks 新的歌曲列表
     */
    public void updateTracks(@NonNull List<LocalAudioTrack> tracks) {
        allTracks.clear();
        allTracks.addAll(tracks);
        updateDisplay();
    }

    private void updateDisplay() {
        // 按歌手分组
        Map<String, List<LocalAudioTrack>> artistMap = new HashMap<>();
        for (LocalAudioTrack track : allTracks) {
            String artist = track.getArtist();
            if (artist.isEmpty()) {
                artist = getString(R.string.unknown_artist);
            }
            if (!artistMap.containsKey(artist)) {
                artistMap.put(artist, new ArrayList<>());
            }
            artistMap.get(artist).add(track);
        }

        // 更新歌手数量
        if (countText != null) {
            countText.setText(getString(R.string.artist_count_format, artistMap.size()));
        }

        // TODO: 设置歌手列表适配器
        // 目前先显示空状态
    }
}
