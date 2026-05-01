package com.example.musicshell.ui;

import android.content.Intent;
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
import com.example.musicshell.media.MusicDataManager;

import java.util.List;
import java.util.Map;

/**
 * 专辑浏览 Fragment。
 *
 * <p>按专辑分组展示本地音乐，以卡片网格形式展示。</p>
 */
public class AlbumsFragment extends Fragment {

    private GridView gridView;
    private TextView countText;
    private TextView emptyText;
    private AlbumAdapter adapter;

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
        emptyText = view.findViewById(R.id.text_album_empty);

        // 初始化适配器
        adapter = new AlbumAdapter(requireContext());
        gridView.setAdapter(adapter);

        // 设置专辑点击监听
        adapter.setOnAlbumClickListener((albumName, songCount) -> {
            Intent intent = new Intent(requireContext(), AlbumDetailActivity.class);
            intent.putExtra(AlbumDetailActivity.EXTRA_ALBUM_NAME, albumName);
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
        Map<String, Integer> albums = dataManager.getAllAlbums();

        if (adapter != null) {
            adapter.updateData(albums);
        }

        if (countText != null) {
            countText.setText(getString(R.string.album_count_format, albums.size()));
        }

        // 空状态处理
        if (emptyText != null && gridView != null) {
            if (albums.isEmpty()) {
                emptyText.setVisibility(View.VISIBLE);
                gridView.setVisibility(View.GONE);
            } else {
                emptyText.setVisibility(View.GONE);
                gridView.setVisibility(View.VISIBLE);
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
