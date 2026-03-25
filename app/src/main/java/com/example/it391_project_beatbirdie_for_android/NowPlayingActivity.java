package com.example.it391_project_beatbirdie_for_android;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import com.bumptech.glide.Glide;
import java.util.List;

public class NowPlayingActivity extends AppCompatActivity implements PlaybackHandler.PlaybackListener {

    private ImageButton playPause;
    private TextView songTitle;
    private TextView artistAlbum;
    private ImageView albumCover;
    private SeekBar scrubberBar;
    private CheckBox repeatCheckbox;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateSeekBar;
    private SeekBar volumeSeekBar;
    private AudioManager audioManager;

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
            
            Glide.with(this)
                .load(currentSong.getAlbumArtUri())
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(albumCover);
            
            // Sync SeekBar with song duration and current position
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

    private void applyKeepScreenOn() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean keepOn = prefs.getBoolean("lock_screen_on", false);
        if (keepOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyKeepScreenOn();
        PlaybackHandler.addListener(this);
        updateUI();
        // Start the periodic update
        handler.post(updateSeekBar);
        if (repeatCheckbox != null) {
            repeatCheckbox.setChecked(PlaybackHandler.isLooping());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        PlaybackHandler.removeListener(this);
        // Stop the periodic update to save resources
        handler.removeCallbacks(updateSeekBar);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_now_playing);

        songTitle = findViewById(R.id.songTitle);
        artistAlbum = findViewById(R.id.artistAlbum);
        albumCover = findViewById(R.id.albumCover);
        playPause = findViewById(R.id.playpause);
        scrubberBar = findViewById(R.id.scrubberBar);
        repeatCheckbox = findViewById(R.id.repeatCheckbox);
        ImageButton skipButton = findViewById(R.id.skip);
        ImageButton rewindButton = findViewById(R.id.rewind);
        Spinner shuffleSpinner = findViewById(R.id.shuffleSpinner);
        Button btnViewQueue = findViewById(R.id.btnViewQueue);
        SeekBar volumeSeekBar = findViewById(R.id.volumeSeekBar);
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volumeSeekBar.setMax(maxVolume);
        volumeSeekBar.setProgress(currentVolume);
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

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

        // --- Shuffle Spinner Setup ---
        String[] algorithms = {"Play in Order", "Fisher-Yates", "True Random, No Repeats", "Fair Play"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, algorithms);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        shuffleSpinner.setAdapter(spinnerAdapter);

        // Sync spinner with current PlaybackHandler state
        for (int i = 0; i < algorithms.length; i++) {
            if (algorithms[i].equals(PlaybackHandler.currentAlg())) {
                shuffleSpinner.setSelection(i);
                break;
            }
        }

        shuffleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PlaybackHandler.setAlg(algorithms[position]);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });


        // repeat checkbox (if checked, song loops)
        repeatCheckbox.setChecked(PlaybackHandler.isLooping());
        repeatCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PlaybackHandler.setLooping(isChecked);
        });

        // --- View Queue Setup ---
        btnViewQueue.setOnClickListener(v -> {
            List<String> queue = PlaybackHandler.getQueueTitles();
            if (queue.isEmpty()) return;

            StringBuilder sb = new StringBuilder();
            for (String title : queue) {
                sb.append(title).append("\n");
            }

            new AlertDialog.Builder(this)
                    .setTitle("Current Playback Order (" + PlaybackHandler.currentAlg() + ")")
                    .setMessage(sb.toString())
                    .setPositiveButton("Close", null)
                    .show();
        });

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
