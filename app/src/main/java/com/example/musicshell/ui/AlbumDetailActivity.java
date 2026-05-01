package com.example.musicshell.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
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
 * 专辑详情页。
 *
 * <p>展示指定专辑下的所有歌曲。</p>
 */
public class AlbumDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ALBUM_NAME = "extra_album_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // 获取专辑名
        String albumName = getIntent().getStringExtra(EXTRA_ALBUM_NAME);
        if (albumName == null || albumName.isEmpty()) {
            finish();
            return;
        }

        // 设置标题栏
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(albumName);
        toolbar.setNavigationOnClickListener(v -> finish());

        // 显示封面区域
        View layoutCover = findViewById(R.id.layout_cover);
        layoutCover.setVisibility(View.VISIBLE);

        // 设置封面占位图
        ImageView coverImage = findViewById(R.id.image_cover);
        coverImage.setImageResource(R.drawable.ic_music_note);
        coverImage.setBackgroundColor(getColor(R.color.album_cover_placeholder));

        // 设置专辑名
        TextView titleText = findViewById(R.id.text_title);
        titleText.setText(albumName);

        // 获取该专辑的歌曲
        MusicDataManager dataManager = MusicDataManager.getInstance();
        List<LocalAudioTrack> tracks = dataManager.getTracksByAlbum(albumName);

        // 设置歌曲数
        TextView countText = findViewById(R.id.text_count);
        countText.setText(getString(R.string.album_song_count_format, tracks.size()));

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
