package com.example.it391_project_beatbirdie_for_android;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.widget.Toolbar;

public class NowPlayingActivity extends AppCompatActivity {

    private ImageButton playPause;
    private TextView songTitle;
    private TextView artistAlbum;

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void updateUI() {
        Song currentSong = PlaybackHandler.getCurrentSong();
        if (currentSong != null) {
            songTitle.setText(currentSong.getTitle());
            String details = currentSong.getArtist() + " - " + currentSong.getAlbum();
            artistAlbum.setText(details);
        }
        updatePlayPauseIcon();
    }

    private void updatePlayPauseIcon() {
        if (PlaybackHandler.isPlaying()) {
            playPause.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            playPause.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_now_playing);
        
        songTitle = findViewById(R.id.songTitle);
        artistAlbum = findViewById(R.id.artistAlbum);
        playPause = findViewById(R.id.playpause);
        ImageButton skipButton = findViewById(R.id.skip);
        ImageButton rewindButton = findViewById(R.id.rewind);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        playPause.setOnClickListener(v -> {
            PlaybackHandler.toggle();
            updatePlayPauseIcon();
        });

        skipButton.setOnClickListener(v -> {
            PlaybackHandler.next(this);
            updateUI();
        });

        rewindButton.setOnClickListener(v -> {
            PlaybackHandler.previous(this);
            updateUI();
        });
        
        updateUI();
    }
}
