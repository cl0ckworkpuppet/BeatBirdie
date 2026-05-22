package com.example.it391_project_beatbirdie_for_android;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CategoryDetailActivity extends AppCompatActivity implements PlaybackHandler.PlaybackListener {

    private RecyclerView rvSongs;
    private SongAdapter adapter;
    private List<Song> songList = new ArrayList<>();
    private String categoryType;
    private String categoryValue;

    private View nowPlayingBar;
    private TextView nowPlayingTitle, nowPlayingArtist;
    private ImageView nowPlayingCover;
    private ImageButton playPause;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_category_detail);

        categoryType = getIntent().getStringExtra("category_type");
        categoryValue = getIntent().getStringExtra("category_value");

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.categoryDetailRoot), (v, insets) -> {
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

        TextView tvHeaderName = findViewById(R.id.tvCategoryDetailName);
        ImageView ivHeaderThumb = findViewById(R.id.ivCategoryDetailThumb);
        tvHeaderName.setText(categoryValue);

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

        rvSongs = findViewById(R.id.rvCategorySongs);
        rvSongs.setLayoutManager(new LinearLayoutManager(this));
        
        loadSongs();
        
        // Update header thumbnail with the first song's art
        if (!songList.isEmpty()) {
            Glide.with(this)
                .load(songList.get(0).getCoverModel())
                .placeholder(R.drawable.ic_music_note)
                .error(Glide.with(this)
                    .load(songList.get(0).getAlbumArtUri())
                    .error(R.drawable.ic_music_note))
                .into(ivHeaderThumb);
        }
    }

    private void loadSongs() {
        songList.clear();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = "";
        String[] selectionArgs = new String[]{categoryValue};

        if (LibraryCategoryFragment.TYPE_ALBUMS.equals(categoryType)) {
            selection = MediaStore.Audio.Media.ALBUM + "=?";
        } else if (LibraryCategoryFragment.TYPE_ARTISTS.equals(categoryType)) {
            selection = MediaStore.Audio.Media.ARTIST + "=?";
        } else if (LibraryCategoryFragment.TYPE_GENRES.equals(categoryType)) {
            // Genre filtering is tricky. A simple way is to find the genre ID first.
            // For now, let's stick to a simpler approximation or just leave it for now if complex.
            // But user asked for it, so let's try to find songs by genre name.
            loadSongsByGenre(categoryValue);
            sortSongs();
            setupAdapter();
            return;
        }

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.TRACK
        };

        try (Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    Song song = Song.getByPath(this, cursor.getString(4));
                    if (song != null) {
                        songList.add(song);
                    }
                }
            }
        }

        sortSongs();
        setupAdapter();
    }

    private void loadSongsByGenre(String genreName) {
        // 1. Get Genre ID from Name
        long genreId = -1;
        Uri genreUri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI;
        try (Cursor c = getContentResolver().query(genreUri, new String[]{MediaStore.Audio.Genres._ID}, MediaStore.Audio.Genres.NAME + "=?", new String[]{genreName}, null)) {
            if (c != null && c.moveToFirst()) {
                genreId = c.getLong(0);
            }
        }

        if (genreId != -1) {
            Uri membersUri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId);
            try (Cursor c = getContentResolver().query(membersUri, new String[]{MediaStore.Audio.Media.DATA}, null, null, null)) {
                if (c != null) {
                    while (c.moveToNext()) {
                        Song song = Song.getByPath(this, c.getString(0));
                        if (song != null) songList.add(song);
                    }
                }
            }
        }
    }

    private void sortSongs() {
        if (LibraryCategoryFragment.TYPE_ALBUMS.equals(categoryType)) {
            Collections.sort(songList, (s1, s2) -> {
                int res = Integer.compare(s1.getTrackNumber(), s2.getTrackNumber());
                if (res == 0) {
                    res = s1.getTitle().compareToIgnoreCase(s2.getTitle());
                }
                return res;
            });
        } else {
            // Artists and Genres: Alphabetical by title
            Collections.sort(songList, (s1, s2) -> s1.getTitle().compareToIgnoreCase(s2.getTitle()));
        }
    }

    private void setupAdapter() {
        adapter = new SongAdapter(songList, position -> {
            PlaybackHandler.playSong(this, songList, position, -2); // -2 or something unique for dynamic category
            updateNowPlayingBar();
        }, position -> {
            // Optional: show options for the song
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
}
