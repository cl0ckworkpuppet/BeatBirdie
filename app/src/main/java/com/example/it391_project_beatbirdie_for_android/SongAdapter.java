package com.example.it391_project_beatbirdie_for_android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {

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
        
        // Log album info to help diagnose duplicate-cover issues
        Log.d("SongAdapter", "Binding pos=" + position + " title=" + song.getTitle() + " artist=" + song.getArtist() + " album=" + song.getAlbum() + " albumId=" + song.getAlbumId());

        // Prefer embedded artwork first, then MediaStore album art, else placeholder
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        boolean loaded = false;
        try {
            mmr.setDataSource(holder.itemView.getContext(), song.getUri());
            byte[] art = mmr.getEmbeddedPicture();
            if (art != null && art.length > 0) {
                Glide.with(holder.itemView.getContext())
                    .asBitmap()
                    .load(art)
                    .placeholder(R.drawable.ic_music_note)
                    .error(R.drawable.ic_music_note)
                    .into(holder.albumCover);
                Log.d("SongAdapter", "Used embedded art for " + song.getTitle());
                loaded = true;
            }
        } catch (Exception e) {
            Log.w("SongAdapter", "Error reading embedded art for " + song.getTitle(), e);
        } finally {
            try { mmr.release(); } catch (Exception ignored) {}
        }

        if (!loaded) {
            if (song.getAlbumId() > 0) {
                Glide.with(holder.itemView.getContext())
                    .load(song.getAlbumArtUri())
                    .placeholder(R.drawable.ic_music_note)
                    .error(R.drawable.ic_music_note)
                    .into(holder.albumCover);
                Log.d("SongAdapter", "Used MediaStore art for " + song.getTitle() + " albumId=" + song.getAlbumId());
            } else {
                holder.albumCover.setImageResource(R.drawable.ic_music_note);
                Log.d("SongAdapter", "No art found for " + song.getTitle() + ", using placeholder");
            }
        }
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }
}
