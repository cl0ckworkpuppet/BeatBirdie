package com.example.it391_project_beatbirdie_for_android;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
            Uri coverUri = findEarliestSongWithCover(paths);
            if (coverUri != null) {
                Glide.with(this)
                    .load(new AudioCoverModel(coverUri))
                    .placeholder(R.drawable.ic_music_note)
                    .error(R.drawable.ic_music_note)
                    .into(ivHeaderThumb);
            } else {
                ivHeaderThumb.setImageResource(R.drawable.ic_music_note);
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

        loadPlaylistSongs();
    }

    private Uri findEarliestSongWithCover(List<String> paths) {
        if (paths == null || paths.isEmpty()) return null;

        android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
        try {
            for (String path : paths) {
                try {
                    retriever.setDataSource(path);
                    byte[] art = retriever.getEmbeddedPicture();
                    if (art != null) {
                        return getSongUriByPath(path);
                    }
                } catch (Exception ignored) {
                }
            }
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {}
        }
        return null;
    }

    private Uri getSongUriByPath(String path) {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Audio.Media._ID};
        String selection = MediaStore.Audio.Media.DATA + "=?";
        String[] selectionArgs = {path};
        try (Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor.getLong(0));
            }
        }
        return null;
    }

    private void loadPlaylistSongs() {
        List<String> paths = db.playlistDao().getSongPathsForPlaylist(playlistId);
        playlistSongs.clear();
        
        for (String path : paths) {
            Song song = getSongByPath(path);
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
                .load(new AudioCoverModel(currentSong.getUri()))
                .placeholder(R.drawable.ic_music_note)
                .error(Glide.with(this).load(currentSong.getAlbumArtUri()).error(R.drawable.ic_music_note))
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

    private Song getSongByPath(String path) {
        if (path != null && (path.toLowerCase().endsWith(".mp2") || path.toLowerCase().endsWith(".wma"))) return null;

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION
        };
        String selection = MediaStore.Audio.Media.DATA + "=?";
        String[] selectionArgs = {path};

        try (Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(0);
                String title = cursor.getString(1);
                String artist = cursor.getString(2);
                String album = cursor.getString(3);
                long albumId = cursor.getLong(4);
                int duration = cursor.getInt(5);
                Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                return new Song(title, artist, album, contentUri, path, albumId, duration);
            }
        }
        return null;
    }

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
