package com.example.it391_project_beatbirdie_for_android;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class PlaylistDetailActivity extends AppCompatActivity implements PlaybackHandler.PlaybackListener {

    private RecyclerView rvSongs;
    private SongAdapter adapter;
    private List<Song> playlistSongs = new ArrayList<>();
    private int playlistId;
    private String playlistName;
    private AppDatabase db;

    private View nowPlayingBar;
    private TextView nowPlayingTitle, nowPlayingArtist;
    private ImageView nowPlayingCover;
    private ImageButton playPause;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_playlist_detail);

        playlistId = getIntent().getIntExtra("playlist_id", -1);
        playlistName = getIntent().getStringExtra("playlist_name");

        db = AppDatabase.getInstance(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.playlistDetailRoot), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Header Setup
        TextView tvHeaderName = findViewById(R.id.tvPlaylistDetailName);
        ImageView ivHeaderThumb = findViewById(R.id.ivPlaylistDetailThumb);
        tvHeaderName.setText(playlistName);

        Playlist playlist = db.playlistDao().getPlaylistById(playlistId);
        if (playlist != null && playlist.getCustomThumbnailPath() != null) {
            Glide.with(this).load(playlist.getCustomThumbnailPath()).into(ivHeaderThumb);
        } else {
            List<String> paths = db.playlistDao().getSongPathsForPlaylist(playlistId);
            Object coverObj = Playlist.findEarliestSongWithCover(this, paths);
            if (coverObj != null) {
                Glide.with(this)
                    .load(coverObj)
                    .placeholder(R.drawable.queue_music_24px)
                    .error(R.drawable.queue_music_24px)
                    .into(ivHeaderThumb);
            } else {
                ivHeaderThumb.setImageResource(R.drawable.queue_music_24px);
            }
        }

        // Now Playing Bar Setup
        nowPlayingBar = findViewById(R.id.nowPlayingBar);
        nowPlayingTitle = findViewById(R.id.nowPlayingTitle);
        nowPlayingArtist = findViewById(R.id.nowPlayingArtist);
        nowPlayingCover = findViewById(R.id.nowPlayingCover);
        playPause = findViewById(R.id.btnPlayPause);

        nowPlayingBar.setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, NowPlayingActivity.class));
        });

        playPause.setOnClickListener(v -> {
            PlaybackHandler.toggle();
            updateNowPlayingBar();
        });

        rvSongs = findViewById(R.id.rvPlaylistSongs);
        rvSongs.setLayoutManager(new LinearLayoutManager(this));
        rvSongs.setHasFixedSize(true);
        rvSongs.setItemViewCacheSize(20);
        
        // Boost the recycled view pool to prevent inflation hangs during fast scroll
        rvSongs.getRecycledViewPool().setMaxRecycledViews(0, 50);

        loadPlaylistSongs();
    }

    private void loadPlaylistSongs() {
        List<String> paths = db.playlistDao().getSongPathsForPlaylist(playlistId);
        playlistSongs.clear();
        for (String path : paths) {
            Song song = Song.getByPath(this, path);
            if (song != null) {
                playlistSongs.add(song);
            }
        }

        adapter = new SongAdapter(playlistSongs, position -> {
            PlaybackHandler.playSong(this, playlistSongs, position, playlistId);
            updateNowPlayingBar();
        }, position -> {
            showRemoveOption(playlistSongs.get(position));
        });
        rvSongs.setAdapter(adapter);
    }

    private void updateNowPlayingBar() {
        Song currentSong = PlaybackHandler.getCurrentSong();
        if (currentSong != null) {
            nowPlayingBar.setVisibility(View.VISIBLE);
            nowPlayingTitle.setText(currentSong.getTitle());
            nowPlayingArtist.setText(currentSong.getArtist());
            
            Glide.with(this)
                .load(currentSong.getCoverModel())
                .placeholder(R.drawable.ic_music_note)
                .error(Glide.with(this)
                    .load(currentSong.getAlbumArtUri())
                    .error(R.drawable.ic_music_note))
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .into(nowPlayingCover);
            
            if (PlaybackHandler.isPlaying()) {
                playPause.setImageResource(android.R.drawable.ic_media_pause);
            } else {
                playPause.setImageResource(android.R.drawable.ic_media_play);
            }
        } else {
            nowPlayingBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        PlaybackHandler.addListener(this);
        updateNowPlayingBar();
    }

    @Override
    protected void onPause() {
        super.onPause();
        PlaybackHandler.removeListener(this);
    }

    @Override
    public void onSongChanged() { runOnUiThread(this::updateNowPlayingBar); }
    @Override
    public void onQueueModified() { }
    @Override
    public void onError(String message) { }

    private void showRemoveOption(Song song) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Remove from Playlist")
                .setMessage("Remove \"" + song.getTitle() + "\" from " + playlistName + "?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    db.playlistDao().removeSongFromPlaylist(new PlaylistSong(playlistId, song.getPath()));
                    db.playlistDao().updateSongCount(playlistId);
                    loadPlaylistSongs();
                    PlaybackHandler.updatePlaylist(playlistSongs, playlistId);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
