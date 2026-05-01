package com.example.musicshell.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.musicshell.R;
import com.example.musicshell.media.LocalAudioTrack;

import java.util.ArrayList;
import java.util.List;

/**
 * 歌曲列表 Fragment。
 *
 * <p>展示本地音乐列表，支持搜索和点击播放。</p>
 */
public class SongsFragment extends Fragment {

    private ListView listView;
    private TextView countText;
    private EditText searchEdit;
    private ImageView clearSearchButton;
    private View searchEmptyLayout;
    private LocalAudioAdapter adapter;
    private List<LocalAudioTrack> allTracks = new ArrayList<>();
    private OnTrackClickListener trackClickListener;

    /**
     * 歌曲点击回调接口。
     */
    public interface OnTrackClickListener {
        void onTrackClick(@NonNull LocalAudioTrack track, int position);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_songs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        listView = view.findViewById(R.id.list_songs);
        countText = view.findViewById(R.id.text_song_count);
        searchEdit = view.findViewById(R.id.edit_search);
        clearSearchButton = view.findViewById(R.id.image_clear_search);
        searchEmptyLayout = view.findViewById(R.id.layout_search_empty);
        
        adapter = new LocalAudioAdapter(requireContext());
        listView.setAdapter(adapter);
        
        // 设置点击事件
        adapter.setOnTrackClickListener((track, position) -> {
            if (trackClickListener != null) {
                trackClickListener.onTrackClick(track, position);
            }
        });
        
        // 设置搜索功能
        setupSearch();
        
        // 更新显示
        updateDisplay();
    }

    /**
     * 设置歌曲点击监听器。
     */
    public void setOnTrackClickListener(@Nullable OnTrackClickListener listener) {
        this.trackClickListener = listener;
    }

    /**
     * 更新歌曲列表。
     *
     * @param tracks 新的歌曲列表
     */
    public void updateTracks(@NonNull List<LocalAudioTrack> tracks) {
        allTracks.clear();
        allTracks.addAll(tracks);
        updateDisplay();
    }

    /**
     * 设置当前播放的歌曲 ID。
     */
    public void setCurrentPlayingTrackId(long trackId) {
        if (adapter != null) {
            adapter.setCurrentPlayingTrackId(trackId);
        }
    }

    /**
     * 设置搜索功能。
     */
    private void setupSearch() {
        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                clearSearchButton.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                filterTracks(query);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        clearSearchButton.setOnClickListener(v -> {
            searchEdit.setText("");
        });
    }

    /**
     * 过滤歌曲列表（搜索功能）。
     *
     * @param query 搜索关键词
     */
    public void filterTracks(@NonNull String query) {
        if (query.isEmpty()) {
            adapter.submitList(allTracks);
            listView.setVisibility(View.VISIBLE);
            searchEmptyLayout.setVisibility(View.GONE);
        } else {
            List<LocalAudioTrack> filtered = new ArrayList<>();
            String lowerQuery = query.toLowerCase();
            for (LocalAudioTrack track : allTracks) {
                if (track.getTitle().toLowerCase().contains(lowerQuery) ||
                    track.getArtist().toLowerCase().contains(lowerQuery) ||
                    track.getAlbum().toLowerCase().contains(lowerQuery)) {
                    filtered.add(track);
                }
            }
            adapter.submitList(filtered);
            
            // 空结果提示
            if (filtered.isEmpty()) {
                listView.setVisibility(View.GONE);
                searchEmptyLayout.setVisibility(View.VISIBLE);
            } else {
                listView.setVisibility(View.VISIBLE);
                searchEmptyLayout.setVisibility(View.GONE);
            }
        }
        updateCountDisplay();
    }

    private void updateDisplay() {
        if (adapter != null) {
            adapter.submitList(allTracks);
        }
        updateCountDisplay();
    }

    private void updateCountDisplay() {
        if (countText != null) {
            int count = adapter != null ? adapter.getCount() : 0;
            countText.setText(getString(R.string.song_count_format, count));
        }
    }
}
