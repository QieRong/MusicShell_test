package com.example.musicshell.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.musicshell.R;
import com.example.musicshell.media.LocalAudioTrack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 本地音频列表适配器。
 *
 * <p>只展示歌名、歌手、专辑和时长，不展示文件路径。
 * 支持点击事件回调和当前播放歌曲高亮。</p>
 */
public class LocalAudioAdapter extends BaseAdapter {

    /**
     * 列表项点击回调接口。
     */
    public interface OnTrackClickListener {
        /**
         * 点击歌曲时回调。
         *
         * @param track 被点击的歌曲
         * @param position 在列表中的位置
         */
        void onTrackClick(@NonNull LocalAudioTrack track, int position);
    }

    @NonNull
    private final LayoutInflater inflater;
    @NonNull
    private final Context context;
    @NonNull
    private final List<LocalAudioTrack> tracks = new ArrayList<>();
    @Nullable
    private OnTrackClickListener clickListener;
    private long currentPlayingTrackId = -1;

    public LocalAudioAdapter(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.inflater = LayoutInflater.from(context);
    }

    /**
     * 设置列表项点击监听器。
     */
    public void setOnTrackClickListener(@Nullable OnTrackClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * 设置当前正在播放的歌曲 ID，用于高亮显示。
     *
     * @param trackId 歌曲 ID，传 -1 表示无歌曲播放
     */
    public void setCurrentPlayingTrackId(long trackId) {
        if (currentPlayingTrackId != trackId) {
            currentPlayingTrackId = trackId;
            notifyDataSetChanged();
        }
    }

    public void submitList(@NonNull List<LocalAudioTrack> nextTracks) {
        tracks.clear();
        tracks.addAll(nextTracks);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return tracks.size();
    }

    @Override
    public LocalAudioTrack getItem(int position) {
        return tracks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        View itemView = convertView;
        if (itemView == null) {
            itemView = inflater.inflate(R.layout.item_local_audio, parent, false);
            holder = new ViewHolder(itemView);
            itemView.setTag(holder);
        } else {
            holder = (ViewHolder) itemView.getTag();
        }

        LocalAudioTrack track = getItem(position);
        holder.titleText.setText(getDisplayTitle(track));
        holder.metaText.setText(getMetaText(track));

        // 高亮当前正在播放的歌曲
        boolean isCurrentPlaying = track.getId() == currentPlayingTrackId;
        holder.titleText.setTextColor(context.getColor(
                isCurrentPlaying ? R.color.player_highlight : R.color.item_title));

        // 设置点击事件
        itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onTrackClick(track, position);
            }
        });

        return itemView;
    }

    @NonNull
    private String getDisplayTitle(@NonNull LocalAudioTrack track) {
        if (TextUtils.isEmpty(track.getTitle())) {
            return context.getString(R.string.unknown_title);
        }
        return track.getTitle();
    }

    @NonNull
    private String getMetaText(@NonNull LocalAudioTrack track) {
        String artist = TextUtils.isEmpty(track.getArtist())
                ? context.getString(R.string.unknown_artist)
                : track.getArtist();
        String album = TextUtils.isEmpty(track.getAlbum())
                ? context.getString(R.string.unknown_album)
                : track.getAlbum();
        return artist + " · " + album + " · " + formatDuration(track.getDurationMs());
    }

    @NonNull
    private String formatDuration(long durationMs) {
        long totalSeconds = Math.max(0, durationMs / 1000);
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    private static class ViewHolder {
        final TextView titleText;
        final TextView metaText;

        ViewHolder(@NonNull View itemView) {
            titleText = itemView.findViewById(R.id.text_track_title);
            metaText = itemView.findViewById(R.id.text_track_meta);
        }
    }
}
