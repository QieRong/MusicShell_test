package com.example.musicshell.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.musicshell.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 专辑网格适配器。
 *
 * <p>在 GridView 中展示专辑卡片，每个卡片显示封面占位图、专辑名、歌曲数。</p>
 */
public class AlbumAdapter extends BaseAdapter {

    /**
     * 专辑点击回调接口。
     */
    public interface OnAlbumClickListener {
        void onAlbumClick(@NonNull String albumName, int songCount);
    }

    @NonNull
    private final Context context;
    @NonNull
    private final LayoutInflater inflater;
    @NonNull
    private final List<AlbumItem> albums = new ArrayList<>();
    @Nullable
    private OnAlbumClickListener clickListener;

    public AlbumAdapter(@NonNull Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    /**
     * 设置专辑点击监听器。
     */
    public void setOnAlbumClickListener(@Nullable OnAlbumClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * 更新专辑数据。
     *
     * @param albumMap 专辑名 → 歌曲数
     */
    public void updateData(@NonNull Map<String, Integer> albumMap) {
        albums.clear();
        for (Map.Entry<String, Integer> entry : albumMap.entrySet()) {
            albums.add(new AlbumItem(entry.getKey(), entry.getValue()));
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return albums.size();
    }

    @Override
    public AlbumItem getItem(int position) {
        return albums.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        View itemView = convertView;
        if (itemView == null) {
            itemView = inflater.inflate(R.layout.item_album, parent, false);
            holder = new ViewHolder(itemView);
            itemView.setTag(holder);
        } else {
            holder = (ViewHolder) itemView.getTag();
        }

        AlbumItem album = getItem(position);

        // 设置封面占位图（灰色方块 + 音符图标）
        holder.coverImage.setImageResource(R.drawable.ic_music_note);
        holder.coverImage.setBackgroundColor(context.getColor(R.color.album_cover_placeholder));

        // 设置专辑名
        holder.nameText.setText(album.name);

        // 设置歌曲数
        holder.countText.setText(context.getString(R.string.album_song_count_format, album.songCount));

        // 点击事件
        itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onAlbumClick(album.name, album.songCount);
            }
        });

        return itemView;
    }

    /**
     * 专辑数据项。
     */
    public static class AlbumItem {
        public final String name;
        public final int songCount;

        public AlbumItem(@NonNull String name, int songCount) {
            this.name = name;
            this.songCount = songCount;
        }
    }

    private static class ViewHolder {
        final ImageView coverImage;
        final TextView nameText;
        final TextView countText;

        ViewHolder(@NonNull View itemView) {
            coverImage = itemView.findViewById(R.id.image_album_cover);
            nameText = itemView.findViewById(R.id.text_album_name);
            countText = itemView.findViewById(R.id.text_album_count);
        }
    }
}
