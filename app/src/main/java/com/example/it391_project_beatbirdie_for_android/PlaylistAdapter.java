package com.example.it391_project_beatbirdie_for_android;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private List<Playlist> playlists;
    private OnPlaylistClickListener listener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
        void onPlaylistLongClick(Playlist playlist, View view);
    }

    public PlaylistAdapter(List<Playlist> playlists, OnPlaylistClickListener listener) {
        this.playlists = playlists;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_item, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        holder.tvName.setText(playlist.getName());
        
        String sizeText = holder.itemView.getContext().getString(R.string.playlist_size_format, playlist.getSongCount());
        holder.tvSize.setText(sizeText);

        // Robust loading using pre-calculated thumbnailUri to prevent UI lag.
        // Nested loading handled in PlaylistActivity ensures we have a Uri ready to go.
        Glide.with(holder.itemView.getContext())
                .load(playlist.getThumbnailUri())
                .placeholder(R.drawable.queue_music_24px)
                .error(R.drawable.queue_music_24px)
                .fallback(R.drawable.queue_music_24px)
                .dontAnimate()
                .into(holder.ivThumbnail);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlaylistClick(playlist);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onPlaylistLongClick(playlist, v);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView tvName, tvSize;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.ivPlaylistThumbnail);
            tvName = itemView.findViewById(R.id.tvPlaylistName);
            tvSize = itemView.findViewById(R.id.tvPlaylistSize);
        }
    }
}
