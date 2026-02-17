package com.example.it391_project_beatbirdie_for_android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {
    private List<Song> songs;

    public SongAdapter(List<Song> songs) {
        this.songs = songs;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, artist;

        public ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.songTitle);
            artist = view.findViewById(R.id.songArtist);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.song_box, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.title.setText(song.title);
        holder.artist.setText(song.artist);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }
}
