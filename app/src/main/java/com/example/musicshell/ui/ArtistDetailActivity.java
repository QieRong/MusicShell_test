package com.example.musicshell.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.musicshell.R;
import com.example.musicshell.media.LocalAudioTrack;
import com.example.musicshell.media.MusicDataManager;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

/**
 * 歌手详情页。
 *
 * <p>展示指定歌手下的所有歌曲。</p>
 */
public class ArtistDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ARTIST_NAME = "extra_artist_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // 获取歌手名
        String artistName = getIntent().getStringExtra(EXTRA_ARTIST_NAME);
        if (artistName == null || artistName.isEmpty()) {
            finish();
            return;
        }

        // 设置标题栏
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(artistName);
        toolbar.setNavigationOnClickListener(v -> finish());

        // 隐藏封面区域（歌手详情不需要封面）
        View layoutCover = findViewById(R.id.layout_cover);
        layoutCover.setVisibility(View.GONE);

        // 获取该歌手的歌曲
        MusicDataManager dataManager = MusicDataManager.getInstance();
        List<LocalAudioTrack> tracks = dataManager.getTracksByArtist(artistName);

        // 设置歌曲列表
        ListView listView = findViewById(R.id.list_songs);
        LocalAudioAdapter adapter = new LocalAudioAdapter(this);
        adapter.submitList(tracks);
        listView.setAdapter(adapter);

        // 点击歌曲播放
        adapter.setOnTrackClickListener((track, position) -> {
            // TODO: 播放歌曲
        });
    }
}
