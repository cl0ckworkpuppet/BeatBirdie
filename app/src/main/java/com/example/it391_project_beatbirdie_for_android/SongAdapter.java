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

    public String getSortType() {
        return sortType;
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
        holder.artist.setText(song.getSubtitle());
        
        // Clear the ImageView immediately to prevent "ghost" images from recycled views
        holder.albumCover.setImageResource(R.drawable.ic_music_note);

        // PERFORMANCE: Use nested fallbacks with placeholders at every level to prevent "empty space"
        // We use .dontAnimate() to prevent the "disappearing" effect during transitions.
        Glide.with(holder.itemView.getContext())
            .load(song.getCoverModel())
            .placeholder(R.drawable.ic_music_note)
            .error(Glide.with(holder.itemView.getContext())
                .load(song.getAlbumArtUri())
                .placeholder(R.drawable.ic_music_note)
                .error(R.drawable.ic_music_note)
                .fallback(R.drawable.ic_music_note)
                .dontAnimate()
                .diskCacheStrategy(DiskCacheStrategy.ALL))
            .fallback(R.drawable.ic_music_note)
            .dontAnimate()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(holder.albumCover);
    }

    @NonNull
    @Override
    public String getPopupText(View view, int position) {
        if (position < 0 || position >= songs.size()) return "";
        
        Song song = songs.get(position);
        String textToCompare;
        if ("artist".equalsIgnoreCase(sortType)) {
            textToCompare = song.getArtist();
        } else if ("album".equalsIgnoreCase(sortType)) {
            textToCompare = song.getAlbum();
        } else {
            textToCompare = song.getTitle();
        }

        if (textToCompare == null || textToCompare.isEmpty()) {
            return "?";
        }
        
        // Simplified character extraction to keep UI thread responsive during fast scroll
        char c = textToCompare.charAt(0);
        if (Character.isDigit(c)) return "#";
        return String.valueOf(Character.toUpperCase(c));
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
