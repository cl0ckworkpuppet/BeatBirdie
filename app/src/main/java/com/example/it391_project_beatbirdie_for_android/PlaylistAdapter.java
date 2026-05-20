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

        if (playlist.getCustomThumbnailPath() != null) {
            Glide.with(holder.itemView.getContext())
                    .load(playlist.getCustomThumbnailPath())
                    .placeholder(R.drawable.ic_music_note)
                    .into(holder.ivThumbnail);
        } else {
            List<String> paths = AppDatabase.getInstance(holder.itemView.getContext()).playlistDao().getSongPathsForPlaylist(playlist.getId());
            Uri coverUri = findEarliestSongWithCover(holder.itemView.getContext(), paths);
            
            if (coverUri != null) {
                Glide.with(holder.itemView.getContext())
                        .load(new AudioCoverModel(coverUri))
                        .placeholder(R.drawable.ic_music_note)
                        .error(R.drawable.ic_music_note)
                        .into(holder.ivThumbnail);
            } else {
                holder.ivThumbnail.setImageResource(R.drawable.ic_music_note);
            }
        }

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

    private Uri findEarliestSongWithCover(android.content.Context context, List<String> paths) {
        if (paths == null || paths.isEmpty()) return null;
        
        android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
        try {
            for (String path : paths) {
                try {
                    retriever.setDataSource(path);
                    byte[] art = retriever.getEmbeddedPicture();
                    if (art != null) {
                        return getSongUriByPath(context, path);
                    }
                } catch (Exception ignored) {
                    // Skip songs that fail to load or have no art
                }
            }
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {}
        }
        return null;
    }

    private Uri getSongUriByPath(android.content.Context context, String path) {
        android.net.Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {android.provider.MediaStore.Audio.Media._ID};
        String selection = android.provider.MediaStore.Audio.Media.DATA + "=?";
        String[] selectionArgs = {path};
        try (android.database.Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(0);
                return android.content.ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
            }
        }
        return null;
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
