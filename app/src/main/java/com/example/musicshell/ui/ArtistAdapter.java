package com.example.musicshell.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.musicshell.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 歌手列表适配器。
 *
 * <p>在 ListView 中展示歌手列表，每行显示圆形首字母头像、歌手名、歌曲数。</p>
 */
public class ArtistAdapter extends BaseAdapter {

    /**
     * 歌手点击回调接口。
     */
    public interface OnArtistClickListener {
        void onArtistClick(@NonNull String artistName, int songCount);
    }

    /** 预设的8种颜色 */
    private static final int[] AVATAR_COLORS = {
            0xFF4CAF50, // 绿色
            0xFF2196F3, // 蓝色
            0xFFFF9800, // 橙色
            0xFF9C27B0, // 紫色
            0xFFE91E63, // 粉色
            0xFF00BCD4, // 青色
            0xFFFF5722, // 深橙色
            0xFF607D8B  // 蓝灰色
    };

    @NonNull
    private final Context context;
    @NonNull
    private final LayoutInflater inflater;
    @NonNull
    private final List<ArtistItem> artists = new ArrayList<>();
    @Nullable
    private OnArtistClickListener clickListener;

    public ArtistAdapter(@NonNull Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    /**
     * 设置歌手点击监听器。
     */
    public void setOnArtistClickListener(@Nullable OnArtistClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * 更新歌手数据。
     *
     * @param artistMap 歌手名 → 歌曲数
     */
    public void updateData(@NonNull Map<String, Integer> artistMap) {
        artists.clear();
        for (Map.Entry<String, Integer> entry : artistMap.entrySet()) {
            artists.add(new ArtistItem(entry.getKey(), entry.getValue()));
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return artists.size();
    }

    @Override
    public ArtistItem getItem(int position) {
        return artists.get(position);
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
            itemView = inflater.inflate(R.layout.item_artist, parent, false);
            holder = new ViewHolder(itemView);
            itemView.setTag(holder);
        } else {
            holder = (ViewHolder) itemView.getTag();
        }

        ArtistItem artist = getItem(position);

        // 设置首字母头像
        String initial = getInitial(artist.name);
        int color = getAvatarColor(artist.name);
        holder.avatarText.setText(initial);
        
        // 设置圆形背景颜色
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(color);
        holder.avatarText.setBackground(background);

        // 设置歌手名
        holder.nameText.setText(artist.name);

        // 设置歌曲数
        holder.countText.setText(context.getString(R.string.artist_song_count_format, artist.songCount));

        // 点击事件
        itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onArtistClick(artist.name, artist.songCount);
            }
        });

        return itemView;
    }

    /**
     * 获取名称的首字母。
     */
    @NonNull
    private String getInitial(@NonNull String name) {
        if (name.isEmpty()) {
            return "?";
        }
        String firstChar = name.substring(0, 1).toUpperCase();
        // 如果是中文字符，直接返回
        if (firstChar.matches("[\\u4e00-\\u9fa5]")) {
            return firstChar;
        }
        // 如果是英文字母，返回大写
        if (firstChar.matches("[A-Za-z]")) {
            return firstChar.toUpperCase();
        }
        // 其他字符返回 #
        return "#";
    }

    /**
     * 根据歌手名哈希值分配颜色。
     */
    private int getAvatarColor(@NonNull String name) {
        int hash = Math.abs(name.hashCode());
        return AVATAR_COLORS[hash % AVATAR_COLORS.length];
    }

    /**
     * 歌手数据项。
     */
    public static class ArtistItem {
        public final String name;
        public final int songCount;

        public ArtistItem(@NonNull String name, int songCount) {
            this.name = name;
            this.songCount = songCount;
        }
    }

    private static class ViewHolder {
        final TextView avatarText;
        final TextView nameText;
        final TextView countText;

        ViewHolder(@NonNull View itemView) {
            avatarText = itemView.findViewById(R.id.text_artist_avatar);
            nameText = itemView.findViewById(R.id.text_artist_name);
            countText = itemView.findViewById(R.id.text_artist_count);
        }
    }
}
