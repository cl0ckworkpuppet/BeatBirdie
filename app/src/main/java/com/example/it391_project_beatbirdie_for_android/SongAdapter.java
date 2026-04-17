package com.example.it391_project_beatbirdie_for_android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;
import me.zhanghai.android.fastscroll.PopupTextProvider;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> implements PopupTextProvider {

    private List<Song> songs;
    private OnSongClickListener listener;

    public interface OnSongClickListener {
        void onSongClick(int position);
    }

    public SongAdapter(List<Song> songs, OnSongClickListener listener) {
        this.songs = songs;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, artist;
        ImageView albumCover;

        public ViewHolder(View view, final OnSongClickListener listener) {
            super(view);
            title = view.findViewById(R.id.songTitle);
            artist = view.findViewById(R.id.songArtist);
            albumCover = view.findViewById(R.id.albumCover);
            
            view.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onSongClick(position);
                    }
                }
            });
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.song_box, parent, false);
        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.title.setText(song.getTitle());
        String subtitle = song.getArtist() + " - " + song.getAlbum();
        holder.artist.setText(subtitle);
        
        // Use Glide with the MediaStore AlbumArtUri. 
        // This is MUCH faster than MediaMetadataRetriever because it uses system-cached thumbnails.
        Glide.with(holder.itemView.getContext())
            .load(song.getAlbumArtUri())
            .placeholder(R.drawable.ic_music_note)
            .error(R.drawable.ic_music_note)
            .into(holder.albumCover);
    }

    @NonNull
    @Override
    public String getPopupText(View view, int position) {
        String title = songs.get(position).getTitle();
        if (title == null || title.trim().isEmpty()) {
            return "&";
        }
        
        char c = title.trim().charAt(0);
        if (Character.isLetter(c)) {
            return String.valueOf(Character.toUpperCase(c));
        } else if (Character.isDigit(c)) {
            return "#";
        } else {
            return "&";
        }
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }
}
