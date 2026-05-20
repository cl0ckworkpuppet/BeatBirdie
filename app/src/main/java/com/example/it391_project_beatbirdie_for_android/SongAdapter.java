package com.example.it391_project_beatbirdie_for_android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import java.util.ArrayList;
import java.util.List;
import me.zhanghai.android.fastscroll.PopupTextProvider;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> implements PopupTextProvider {

    private List<Song> songs;
    private List<Song> songsFull; // Full list for filtering
    private OnSongClickListener listener;
    private OnSongLongClickListener longClickListener;
    private String sortType = "title";

    public void setSortType(String sortType) {
        this.sortType = sortType;
    }

    public interface OnSongClickListener {
        void onSongClick(int position);
    }

    public interface OnSongLongClickListener {
        void onSongLongClick(int position);
    }

    public SongAdapter(List<Song> songs, OnSongClickListener listener, OnSongLongClickListener longClickListener) {
        this.songs = songs;
        this.songsFull = new ArrayList<>(songs); // Initialize full list
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    public void updateList(List<Song> newList) {
        this.songs = newList;
        this.songsFull = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, artist;
        ImageView albumCover;

        public ViewHolder(View view, final OnSongClickListener listener, final OnSongLongClickListener longClickListener) {
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

            view.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        longClickListener.onSongLongClick(position);
                        return true;
                    }
                }
                return false;
            });
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.song_box, parent, false);
        return new ViewHolder(view, listener, longClickListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.title.setText(song.getTitle());
        String subtitle = song.getArtist() + " - " + song.getAlbum();
        holder.artist.setText(subtitle);
        
        // Robust loading using custom AudioCoverModel.
        // This prioritizes embedded art via MediaMetadataRetriever (cached by Glide).
        // If embedded art fails, it falls back to the MediaStore album art URI.
        Glide.with(holder.itemView.getContext())
            .load(new AudioCoverModel(song.getUri()))
            .override(160, 160)
            .placeholder(R.drawable.ic_music_note)
            .error(Glide.with(holder.itemView.getContext())
                .load(song.getAlbumArtUri())
                .override(160, 160)
                .error(R.drawable.ic_music_note))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(holder.albumCover);
    }

    @NonNull
    @Override
    public String getPopupText(View view, int position) {
        String textToCompare;
        if ("artist".equalsIgnoreCase(sortType)) {
            textToCompare = songs.get(position).getArtist();
        } else if ("album".equalsIgnoreCase(sortType)) {
            textToCompare = songs.get(position).getAlbum();
        } else {
            textToCompare = songs.get(position).getTitle();
        }

        if (textToCompare == null || textToCompare.trim().isEmpty()) {
            return "&";
        }
        
        char c = textToCompare.trim().charAt(0);
        if (Character.isLetter(c)) {
            return String.valueOf(Character.toUpperCase(c));
        } else if (Character.isDigit(c)) {
            return "#";
        } else {
            return "&";
        }
    }

    public List<Song> getSongs() {
        return songs;
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public void filter(String query) {
        List<Song> filteredList = new ArrayList<>();
        if (query == null || query.isEmpty()) {
            filteredList.addAll(songsFull);
        } else {
            String filterPattern = query.toLowerCase().trim();
            for (Song song : songsFull) {
                if (song.getTitle().toLowerCase().contains(filterPattern) ||
                    song.getArtist().toLowerCase().contains(filterPattern) ||
                    song.getAlbum().toLowerCase().contains(filterPattern)) {
                    filteredList.add(song);
                }
            }
        }
        songs = filteredList;
        notifyDataSetChanged();
    }
}
