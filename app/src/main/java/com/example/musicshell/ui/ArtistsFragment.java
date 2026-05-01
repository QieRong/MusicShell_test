package com.example.musicshell.ui;

import android.content.Intent;
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
import com.example.musicshell.media.MusicDataManager;

import java.util.List;
import java.util.Map;

/**
 * 歌手浏览 Fragment。
 *
 * <p>按歌手分组展示本地音乐，以列表形式展示。</p>
 */
public class ArtistsFragment extends Fragment {

    private ListView listView;
    private TextView countText;
    private TextView emptyText;
    private ArtistAdapter adapter;

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
        emptyText = view.findViewById(R.id.text_artist_empty);

        // 初始化适配器
        adapter = new ArtistAdapter(requireContext());
        listView.setAdapter(adapter);

        // 设置歌手点击监听
        adapter.setOnArtistClickListener((artistName, songCount) -> {
            Intent intent = new Intent(requireContext(), ArtistDetailActivity.class);
            intent.putExtra(ArtistDetailActivity.EXTRA_ARTIST_NAME, artistName);
            startActivity(intent);
        });

        // 更新显示
        updateDisplay();
    }

    /**
     * 更新显示。
     */
    public void updateDisplay() {
        MusicDataManager dataManager = MusicDataManager.getInstance();
        Map<String, Integer> artists = dataManager.getAllArtists();

        if (adapter != null) {
            adapter.updateData(artists);
        }

        if (countText != null) {
            countText.setText(getString(R.string.artist_count_format, artists.size()));
        }

        // 空状态处理
        if (emptyText != null && listView != null) {
            if (artists.isEmpty()) {
                emptyText.setVisibility(View.VISIBLE);
                listView.setVisibility(View.GONE);
            } else {
                emptyText.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次恢复时更新数据
        updateDisplay();
    }
}
