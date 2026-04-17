package com.example.it391_project_beatbirdie_for_android;

import android.media.MediaMetadataRetriever;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.ViewHolder> {

    private List<Song> queue;
    private final OnQueueActionListener listener;

    public interface OnQueueActionListener {
        void onMoveToFront(int position);
        void onRemove(int position);
    }

    public QueueAdapter(List<Song> queue, OnQueueActionListener listener) {
        this.queue = queue;
        this.listener = listener;
    }

    public void updateData(List<Song> newQueue) {
        this.queue = newQueue;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.queue_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = queue.get(position);
        
        holder.songTitle.setText(song.getTitle());
        String details = song.getArtist() + " • " + song.getAlbum();
        holder.songDetails.setText(details);

        // Load album cover
        boolean loaded = false;
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(holder.itemView.getContext(), song.getUri());
            byte[] art = mmr.getEmbeddedPicture();
            if (art != null && art.length > 0) {
                Glide.with(holder.itemView.getContext())
                    .asBitmap()
                    .load(art)
                    .placeholder(R.drawable.ic_music_note)
                    .into(holder.queueAlbumCover);
                loaded = true;
            }
        } catch (Exception e) {
            Log.w("QueueAdapter", "Failed to load embedded art", e);
        } finally {
            try { mmr.release(); } catch (Exception ignored) {}
        }

        if (!loaded) {
            Glide.with(holder.itemView.getContext())
                .load(song.getAlbumArtUri())
                .placeholder(R.drawable.ic_music_note)
                .into(holder.queueAlbumCover);
        }

        holder.btnMoveFront.setVisibility(View.VISIBLE);
        holder.btnRemove.setVisibility(View.VISIBLE);

        holder.btnMoveFront.setOnClickListener(v -> {
            if (listener != null) listener.onMoveToFront(holder.getAdapterPosition());
        });

        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) listener.onRemove(holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return queue.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView songTitle, songDetails;
        ImageButton btnMoveFront, btnRemove;
        ImageView queueAlbumCover;

        public ViewHolder(View view) {
            super(view);
            songTitle = view.findViewById(R.id.songTitle);
            songDetails = view.findViewById(R.id.songDetails);
            btnMoveFront = view.findViewById(R.id.btnMoveFront);
            btnRemove = view.findViewById(R.id.btnRemove);
            queueAlbumCover = view.findViewById(R.id.queueAlbumCover);
        }
    }
}
