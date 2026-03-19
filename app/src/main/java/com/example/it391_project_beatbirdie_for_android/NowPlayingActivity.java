package com.example.it391_project_beatbirdie_for_android;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.widget.Toolbar;

public class NowPlayingActivity extends AppCompatActivity implements PlaybackHandler.PlaybackListener {

    private ImageButton playPause;
    private TextView songTitle;
    private TextView artistAlbum;
    private SeekBar scrubberBar;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateSeekBar;

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onSongChanged() {
        runOnUiThread(this::updateUI);
    }

    private void updateUI() {
        Song currentSong = PlaybackHandler.getCurrentSong();
        if (currentSong != null) {
            songTitle.setText(currentSong.getTitle());
            String details = currentSong.getArtist() + " - " + currentSong.getAlbum();
            artistAlbum.setText(details);

            scrubberBar.setMax(PlaybackHandler.getDuration());
            scrubberBar.setProgress(PlaybackHandler.getCurrentPosition());
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
        PlaybackHandler.addListener(this);
        updateUI();
        // Start the periodic update
        handler.post(updateSeekBar);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PlaybackHandler.removeListener(this);
        handler.removeCallbacks(updateSeekBar);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_now_playing);
        
        songTitle = findViewById(R.id.songTitle);
        artistAlbum = findViewById(R.id.artistAlbum);
        playPause = findViewById(R.id.playpause);
        scrubberBar = findViewById(R.id.scrubberBar);
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

        // Add listener for user scrubbing
        scrubberBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    PlaybackHandler.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Initialize the Runnable for updating the SeekBar
        updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (PlaybackHandler.isPlaying()) {
                    scrubberBar.setProgress(PlaybackHandler.getCurrentPosition());
                }
                handler.postDelayed(this, 1000); // Update every 1 second
            }
        };
        
        updateUI();
    }
}
